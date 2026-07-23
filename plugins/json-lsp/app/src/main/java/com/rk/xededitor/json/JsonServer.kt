package com.rk.xededitor.json

import android.app.Activity
import android.content.Context
import com.scto.mobile.ide.core.common.files.BuiltinFileType
import com.scto.mobile.ide.core.common.icons.Icon
import com.rk.lsp.LspConnectionConfig
import com.rk.lsp.LspServer
import java.io.File

class JsonServer(val lspPath: String) : LspServer() {
    override val id: String = "rk.json"
    override val languageName: String = "Json"
    override val serverName: String = "light-json"
    override val supportedExtensions: List<String> = listOf("json")
    override val icon: Icon? = BuiltinFileType.JSON.icon

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