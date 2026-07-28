package com.scto.mobile.ide.features.extensions.loader

import android.app.Application
import androidx.core.content.pm.PackageInfoCompat
import com.scto.mobile.ide.DefaultScope
import com.scto.mobile.ide.crashhandler.CrashActivity
import com.scto.mobile.ide.events.Events
import com.scto.mobile.ide.features.extensions.ExtensionAPI
import com.scto.mobile.ide.features.extensions.ExtensionContext
import com.scto.mobile.ide.features.extensions.ExtensionEvent
import com.scto.mobile.ide.features.extensions.LocalExtension
import com.scto.mobile.ide.features.extensions.apkFile
import com.scto.mobile.ide.features.extensions.extensionManager
import com.scto.mobile.ide.features.extensions.manager.ExtensionManager
import com.scto.mobile.ide.features.extensions.manager.LoadedExtension
import com.scto.mobile.ide.file.FileObject
import com.scto.mobile.ide.file.copyToTempDir
import com.scto.mobile.ide.utils.application
import com.scto.mobile.ide.utils.isMainThread
import dalvik.system.PathClassLoader
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.reflect.InvocationTargetException

enum class LoadScenario {
    INSTALL,
    UPDATE,
    NONE,
}

/**
 * Loads a locally installed extension.
 *
 * This function performs compatibility checks, instantiates the extension's main class, initializes the extension
 * lifecycle, and caches the result.
 *
 * @param application The main Android [Application] instance.
 * @param loadScenario The reason why the extension is being loaded. Determines whether installation or update lifecycle
 *   callbacks are invoked before the extension is marked as loaded.
 * @return A [Result] enclosing the loaded [com.scto.mobile.ide.features.extensions.ExtensionAPI] instance, or a failure exception.
 */
suspend fun LocalExtension.load(
    application: Application,
    loadScenario: LoadScenario,
): Result<ExtensionAPI> {
    if (isMainThread()) {
        return Result.failure(
            IllegalStateException(
                "Attempted to load extension '${manifest.name}' on the main thread. Extension loading must be performed on a background thread."
            )
        )
    }

    return runCatching {
        verifyCompatibility(application)

        val classLoader = createClassLoader(application)
        val mainClass = loadMainClass(classLoader)

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineName("Extension: $id"))
        val instance = instantiateAPI(mainClass, application, scope)

        if (loadScenario == LoadScenario.INSTALL) {
            instance.onInstalled()
            extensionManager.invalidateSize(this)
        } else if (loadScenario == LoadScenario.UPDATE) {
            instance.afterUpdate()
            extensionManager.invalidateSize(this)
        }
        instance.onExtensionLoaded()

        extensionManager.loadedExtensions[this] = LoadedExtension(instance, scope)

        DefaultScope.launch {
            Events.publish(ExtensionEvent.Loaded(this@load))
        }

        instance
    }
}

/**
 * Verifies if the extension is compatible with the running version of the editor. Throws an [IllegalStateException] if
 * the app version does not satisfy the extension's requirements.
 */
private fun LocalExtension.verifyCompatibility(application: Application) {
    val mobileideVersionCode =
        PackageInfoCompat.getLongVersionCode(application.packageManager.getPackageInfo(application.packageName, 0))

    val minAppVersion = manifest.minAppVersion
    if (minAppVersion != null && mobileideVersionCode < minAppVersion) {
        throw IllegalStateException(
            "Extension '${manifest.name}' (${manifest.version}) is not compatible with this version of MobileIDE (min: $minAppVersion, current: $mobileideVersionCode)"
        )
    }
}

/**
 * Creates a class loader specifically configured for this extension's APK/package file. Uses a child-first delegation
 * strategy so extension-specific libraries take precedence.
 */
private fun LocalExtension.createClassLoader(application: Application): ClassLoader {
    return try {
        PathClassLoader(apkFile.absolutePath, application.classLoader)
    } catch (err: Exception) {
        throw IllegalStateException(
            "Failed to create ClassLoader for extension '${manifest.name}'. Details: ${err.message}",
            err,
        )
    }
}

/** Loads the main entry point class of the extension and asserts that it implements [ExtensionAPI]. */
private fun LocalExtension.loadMainClass(classLoader: ClassLoader): Class<*> {
    val mainClass =
        try {
            classLoader.loadClass(manifest.mainClass)
        } catch (err: Throwable) {
            throw err
        }

    if (!ExtensionAPI::class.java.isAssignableFrom(mainClass)) {
        throw IllegalStateException(
            "The main class '${manifest.mainClass}' of extension '${manifest.name}' does not implement the ExtensionAPI interface. Please ensure the main class correctly implements this interface."
        )
    }

    return mainClass
}

/**
 * Instantiates the extension's main [ExtensionAPI] class by calling its public constructor that accepts an
 * [com.scto.mobile.ide.features.extensions.ExtensionContext].
 */
private fun LocalExtension.instantiateAPI(
    mainClassInstance: Class<*>,
    application: Application,
    scope: CoroutineScope,
): ExtensionAPI {
    val extContext = ExtensionContext(extension = this, appContext = application, scope = scope)
    return try {
        val constructor = mainClassInstance.getDeclaredConstructor(ExtensionContext::class.java)
        (constructor.newInstance(extContext) as? ExtensionAPI)
            ?: throw IllegalStateException(
                "Failed to instantiate main class '${mainClassInstance.name}' for extension '${manifest.name}'. Ensure the class implements the ExtensionAPI interface and declares a public constructor accepting ExtensionContext."
            )
    } catch (err: Throwable) {
        // Unpack Java reflection wrapping to show the real root exception if available
        val realError = if (err is InvocationTargetException) err.cause ?: err else err
        throw realError
    }
}

/** Installs an extension directly from a file object by copying it to a temporary directory first. */
suspend fun ExtensionManager.installExtensionFromZip(fileObject: FileObject) = run {
    val file = fileObject.copyToTempDir()
    installExtensionFromZip(file).also { file.delete() }
}

/**
 * Scans all local extensions and loads any that are not disabled. If an extension fails to load, it is marked as
 * disabled and a crash screen is shown.
 */
suspend fun ExtensionManager.loadAllExtensions() =
    withContext(Dispatchers.IO) {
        for ((_, extension) in localExtensions) {
            if (isExtensionCrashed(extension)) {
                continue
            }
            launch(Dispatchers.IO) {
                extension.load(application!!, LoadScenario.NONE).onFailure { error ->
                    setExtensionCrashed(extension, true)
                    withContext(Dispatchers.Main) {
                        CrashActivity.start(
                            context = application!!,
                            extensionId = extension.id,
                            extensionName = extension.name,
                            extensionVersion = extension.version,
                            extensionAuthor = extension.author.toString(),
                            repository = extension.repository,
                            error = error,
                        )
                    }
                }
            }
        }
    }
