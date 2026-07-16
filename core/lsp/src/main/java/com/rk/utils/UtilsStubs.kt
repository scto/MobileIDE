package com.rk.utils
import android.app.Activity
import android.content.Context
import java.io.File
fun logInfo(msg: Any?) {}
fun logError(msg: Any?) {}
fun dialog(activity: Activity, title: String, msg: String) {}
fun toast(msg: String?) {}
fun getTempDir(): File = File("/data/data/com.scto.mobile.ide/cache")
fun isDarkTheme(context: Context): Boolean = true
