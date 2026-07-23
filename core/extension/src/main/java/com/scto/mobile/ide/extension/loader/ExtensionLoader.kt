package com.scto.mobile.ide.extension.loader

import android.app.Application
import androidx.core.content.pm.PackageInfoCompat
import com.scto.mobile.ide.extension.ExtensionAPI
import com.scto.mobile.ide.extension.ExtensionContext
import com.scto.mobile.ide.extension.LocalExtension
import com.scto.mobile.ide.extension.apkFile
import com.scto.mobile.ide.extension.manager.ExtensionManager
import com.scto.mobile.ide.extension.manager.LoadedExtension
import com.scto.mobile.ide.extension.extensionManager

import android.os.Looper
import dalvik.system.PathClassLoader
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.reflect.InvocationTargetException
import kotlin.collections.iterator

/**
 * Loads a locally installed extension.
 *
 * This function performs compatibility checks, instantiates the extension's main class,
 * initializes the extension lifecycle, and caches the result.
 *
 * @param application The main Android [Application] instance.
 * @param initialInstallation True if this is the first time the extension is installed/loaded.
 * @return A [Result] enclosing the loaded [com.scto.mobile.ide.extension.ExtensionAPI] instance, or a failure exception.
 */
fun LocalExtension.load(
    application: Application,
    initialInstallation: Boolean = false
): Result<ExtensionAPI> {
    if ((Looper.myLooper() == Looper.getMainLooper())) {
        return Result.failure(
            IllegalStateException(
                "Attempted to load extension '${manifest.name}' on the main thread. Extension loading must be performed on a background thread."
            )
        )
    }

    return runCatching {
        // 1. Verify compatibility with the current app version
        verifyCompatibility(application)

        // 2. Create the class loader to load the extension's code
        val classLoader = createClassLoader(application)

        // 3. Load the extension's main class and verify it implements ExtensionAPI
        val mainClass = loadMainClass(classLoader)

        // 4. Set up the coroutine scope and instantiate the API class
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineName("Extension: $id"))
        val instance = instantiateAPI(mainClass, application, scope)

        // 5. Invoke lifecycle callback methods
        if (initialInstallation) {
            instance.onInstalled()
        }
        instance.onExtensionLoaded()

        // 6. Cache the loaded extension in the manager
        extensionManager.loadedExtensions[this] = LoadedExtension(instance, scope)
        instance
    }
}

/**
 * Verifies if the extension is compatible with the running version of the editor.
 * Throws an [IllegalStateException] if the app version does not satisfy the extension's requirements.
 */
private fun LocalExtension.verifyCompatibility(application: Application) {
    val minAppVersion = manifest.minAppVersion
    val maxAppVersion = manifest.maxAppVersion

    val xedVersionCode = PackageInfoCompat.getLongVersionCode(
        application.packageManager.getPackageInfo(application.packageName, 0)
    )

    val isBelowMin = minAppVersion != null && xedVersionCode < minAppVersion
    val isAboveMax = maxAppVersion != null && xedVersionCode > maxAppVersion

    if (isBelowMin || isAboveMax) {
        throw IllegalStateException(
            "Extension '${manifest.name}' (${manifest.version}) is not compatible with this version of Xed-Editor (min: $minAppVersion, max: $maxAppVersion, Xed-Editor: $xedVersionCode)"
        )
    }
}

/**
 * Creates a class loader specifically configured for this extension's APK/package file.
 * Uses a child-first delegation strategy so extension-specific libraries take precedence.
 */
private fun LocalExtension.createClassLoader(application: Application): ClassLoader {
    return try {
        PathClassLoader(apkFile.absolutePath, application.classLoader)
    } catch (err: Exception) {
        throw IllegalStateException(
            "Failed to create ClassLoader for extension '${manifest.name}'. Details: ${err.message}",
            err
        )
    }
}



/**
 * Loads the main entry point class of the extension and asserts that it implements [ExtensionAPI].
 */
private fun LocalExtension.loadMainClass(classLoader: ClassLoader): Class<*> {
    val mainClass = try {
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
 * Instantiates the extension's main [ExtensionAPI] class by calling its public constructor
 * that accepts an [com.scto.mobile.ide.extension.ExtensionContext].
 */
private fun LocalExtension.instantiateAPI(
    mainClassInstance: Class<*>,
    application: Application,
    scope: CoroutineScope
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



/**
 * Scans all local extensions and loads any that are not disabled.
 * If an extension fails to load, it is marked as disabled and a crash screen is shown.
 */
suspend fun ExtensionManager.loadAllExtensions() =
    withContext(Dispatchers.IO) {
        for ((_, extension) in localExtensions) {
            if (isExtensionDisabled(extension.id)) {
                continue
            }
            launch(Dispatchers.IO) {
                extension.load(this@loadAllExtensions.context).onFailure { error ->
                    setExtensionDisabled(extension.id, true)
                    withContext(Dispatchers.Main) {
                        android.util.Log.e("ExtensionLoader", "Crash loading extension ${extension.id}", error)
                    }
                }
            }
        }
    }
