package com.scto.mobile.ide.extension.api
import android.util.Log
import com.scto.mobile.ide.extension.ExtensionId

fun ExtensionId.logDebug(msg: String) {
    Log.d(this, msg)
}

fun ExtensionId.logInfo(msg: String) {
    Log.i(this, msg)
}

fun ExtensionId.logWarn(msg: String) {
    Log.w(this, msg)
}

fun ExtensionId.logError(msg: String) {
    Log.e(this, msg)
}