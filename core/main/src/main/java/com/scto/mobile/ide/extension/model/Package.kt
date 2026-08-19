package com.scto.mobile.ide.extension.model





import com.scto.mobile.ide.common.PackageType
import io.github.z4kn4fein.semver.toVersionOrNull
import java.util.Date
import kotlinx.serialization.Serializable











@Serializable
data class PackageCache(
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val size: Long? = null,
)

data class Review(val rating: Int, val text: String, val author: String, val date: Date, val authorResponse: String?)

interface Package {
    val id: String
    val type: PackageType
    val name: String
    val version: String
    val author: PackageAuthor
    val description: String?
    val tags: List<String>
    val repository: String?
    val license: String?
    val dependencies: List<String>
    val recommendations: List<String>
    val hasSettings: Boolean
    val iconUrl: String
    val readmeUrl: String
    val changelogUrl: String
    val minAppVersion: Int?
    val supportedArchitectures: List<String>?
    val downloads: Int?
    val rating: Float?
    val size: Long?
    val createdAt: Long?
    val updatedAt: Long?

    suspend fun getReviews(): List<Review>
}

interface UpdatablePackage : Package {
    val newVersion: String

    fun hasUpdate(): Boolean {
        val installedVersion = version.toVersionOrNull() ?: return false
        val storeVersion = newVersion.toVersionOrNull() ?: return false
        return installedVersion < storeVersion
    }
}
