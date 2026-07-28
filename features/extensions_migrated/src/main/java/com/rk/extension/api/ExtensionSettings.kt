// DO NOT UPDATE PACKAGE NAME OTHERWISE EXTENSIONS WILL BREAK
package com.scto.mobile.ide.features.extensions

import com.scto.mobile.ide.settings.CachedExtensionPreference
import com.scto.mobile.ide.settings.Preference
import kotlin.properties.ReadWriteProperty

interface ExtensionSettings {
    fun getString(key: String, default: String): String?

    fun getBoolean(key: String, default: Boolean): Boolean

    fun getInt(key: String, default: Int): Int

    fun putString(key: String, value: String)

    fun putBoolean(key: String, value: Boolean)

    fun putInt(key: String, value: Int)

    fun <T> delegate(key: String, defaultValue: T): ReadWriteProperty<Any?, T>
}

class SharedPrefExtensionSettings(private val id: String) : ExtensionSettings {
    override fun getString(key: String, default: String) = Preference.getString("$id.$key", default)

    override fun getBoolean(key: String, default: Boolean) = Preference.getBoolean("$id.$key", default)

    override fun getInt(key: String, default: Int) = Preference.getInt("$id.$key", default)

    override fun putString(key: String, value: String) = Preference.setString("$id.$key", value)

    override fun putBoolean(key: String, value: Boolean) = Preference.setBoolean("$id.$key", value)

    override fun putInt(key: String, value: Int) = Preference.setInt("$id.$key", value)

    override fun <T> delegate(
        key: String,
        defaultValue: T,
    ): ReadWriteProperty<Any?, T> {
        return CachedExtensionPreference(id, key, defaultValue)
    }
}
