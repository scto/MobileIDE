


//DO NOT UPDATE PACKAGE NAME OTHERWISE EXTENSIONS WILL BREAK
package com.rk.extension



interface ExtensionSettings {
    fun getString(key: String, default: String): String?

    fun getBoolean(key: String, default: Boolean): Boolean

    fun getInt(key: String, default: Int): Int

    fun putString(key: String, value: String)

    fun putBoolean(key: String, value: Boolean)

    fun putInt(key: String, value: Int)
}

class SharedPrefExtensionSettings(private val id: String) : ExtensionSettings {
    override fun getString(key: String, default: String) = default

    override fun getBoolean(key: String, default: Boolean) = default

    override fun getInt(key: String, default: Int) = default

    override fun putString(key: String, value: String) {}

    override fun putBoolean(key: String, value: Boolean) {}

    override fun putInt(key: String, value: Int) {}
}
