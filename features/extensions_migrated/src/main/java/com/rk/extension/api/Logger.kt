package com.scto.mobile.ide.features.extensions.api

import android.util.Log
import com.scto.mobile.ide.features.extensions.ExtensionId
import com.scto.mobile.ide.resources.getString
import com.scto.mobile.ide.resources.strings
import com.scto.mobile.ide.settings.debugOptions.LogCollector

fun ExtensionId.logDebug(msg: String) {
    Log.d(this, msg)
    LogCollector.reportDebug(msg, this)
}

fun ExtensionId.logInfo(msg: String) {
    Log.i(this, msg)
    LogCollector.reportInfo(msg, this)
}

fun ExtensionId.logWarn(msg: String) {
    Log.w(this, msg)
    LogCollector.reportWarn(msg, this)
}

fun ExtensionId.logError(msg: String) {
    Log.e(this, msg)
    LogCollector.reportError(msg, this)
}

fun ExtensionId.logError(throwable: Throwable, msg: String = strings.unknown_error.getString()) {
    Log.e(this, msg, throwable)
    LogCollector.reportError("$msg: \n${throwable.stackTraceToString()}", this)
}
