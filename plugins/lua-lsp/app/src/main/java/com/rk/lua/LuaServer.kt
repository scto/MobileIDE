package com.rk.lua

import android.app.Activity
import android.content.Context
import android.os.Build
import com.rk.file.BuiltinFileType
import com.rk.icons.Icon
import com.rk.lsp.LspConnectionConfig
import com.rk.lsp.LspServer
import java.io.File

class LuaServer(val lspPath: String) : LspServer() {
    override val id: String = "rk.lua"
    override val languageName: String = "Lua"
    override val serverName: String = "emmy-ls"
    override val supportedExtensions: List<String> = listOf("lua")
    override val icon: Icon? = BuiltinFileType.LUA.icon

    override suspend fun isInstalled(context: Context): Boolean {
        return true
    }


    override fun install(activity: Activity) {}

    override fun uninstall(activity: Activity) {}

    override suspend fun isUpdatable(context: Context): Boolean {
        return false
    }

    override fun update(activity: Activity) {}


    override fun getConnectionConfig(): LspConnectionConfig {

        val linker32 = File("/system/bin/linker")
        val linker64 = File("/system/bin/linker64")

        val linker = if (linker64.exists()){
            linker64.absolutePath
        }else{
            linker32.absolutePath
        }



        return LspConnectionConfig.Process(arrayOf(linker,lspPath))
    }
}