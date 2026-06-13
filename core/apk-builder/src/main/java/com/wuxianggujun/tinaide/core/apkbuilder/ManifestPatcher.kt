package com.wuxianggujun.tinaide.core.apkbuilder

import android.Manifest
import com.reandroid.app.AndroidManifest
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock
import com.reandroid.arsc.chunk.xml.ResXmlElement
import com.reandroid.arsc.value.ValueType
import java.io.ByteArrayInputStream
import timber.log.Timber

/**
 * Patches the template AndroidManifest.xml as binary AXML.
 *
 * Besides package/app metadata, it rewrites the exported permission set so the
 * final APK only declares the permissions selected during export.
 */
object ManifestPatcher {

    private const val TAG = "ManifestPatcher"

    private val permissionAttributeOverrides = mapOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE to PermissionAttributeOverride(maxSdkVersion = 29)
    )

    fun patch(axml: ByteArray, config: ApkBuildConfig): ByteArray {
        val manifest = AndroidManifestBlock.load(ByteArrayInputStream(axml))
        val requestedPermissions = normalizeRequestedPermissions(config.requestedPermissions)

        manifest.setPackageName(config.packageName)
        manifest.setVersionCode(config.versionCode)
        manifest.setVersionName(config.versionName)
        patchApplicationLabel(manifest, config.appName)
        patchUsesPermissions(manifest, requestedPermissions)
        manifest.refreshFull()

        val patched = manifest.getBytes()
        Timber.tag(TAG).d(
            "Manifest patched: package=%s, appName=%s, permissions=%s",
            config.packageName,
            config.appName,
            requestedPermissions.joinToString(",")
        )
        return patched
    }

    private fun patchApplicationLabel(manifest: AndroidManifestBlock, appName: String) {
        val applicationElement = manifest.getOrCreateApplicationElement()
        applicationElement
            .getOrCreateAndroidAttribute(AndroidManifest.NAME_label, AndroidManifest.ID_label)
            .setValueAsString(appName)
    }

    private fun patchUsesPermissions(
        manifest: AndroidManifestBlock,
        requestedPermissions: List<String>
    ) {
        val manifestElement = manifest.getManifestElement() ?: return
        manifestElement.removeElementsIf { element -> element.equalsName(AndroidManifest.TAG_uses_permission) }

        requestedPermissions.forEach { permissionName ->
            val permissionElement = manifest.addUsesPermission(permissionName) ?: return@forEach
            applyPermissionAttributes(permissionElement, permissionName)
        }
    }

    private fun applyPermissionAttributes(permissionElement: ResXmlElement, permissionName: String) {
        val override = permissionAttributeOverrides[permissionName] ?: return
        override.maxSdkVersion?.let { maxSdkVersion ->
            permissionElement
                .getOrCreateAndroidAttribute(AndroidManifest.NAME_maxSdkVersion, AndroidManifest.ID_maxSdkVersion)
                .setTypeAndData(ValueType.DEC, maxSdkVersion)
        }
    }

    private fun normalizeRequestedPermissions(permissions: List<String>): List<String> {
        return permissions
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .toList()
    }

    private data class PermissionAttributeOverride(
        val maxSdkVersion: Int? = null
    )
}
