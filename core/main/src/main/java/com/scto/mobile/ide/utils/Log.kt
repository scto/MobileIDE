package com.scto.mobile.ide.utils





import android.util.Log
import com.blankj.utilcode.util.StringUtils.getString
import com.scto.mobile.ide.settings.debugOptions.LogCollector











fun Any.logDebug(msg: String) {
    Log.d(this::class.java.simpleName, msg)
    LogCollector.reportDebug(msg)
}

fun Any.logInfo(msg: String) {
    Log.i(this::class.java.simpleName, msg)
    LogCollector.reportInfo(msg)
}

fun Any.logWarn(msg: String) {
    Log.w(this::class.java.simpleName, msg)
    LogCollector.reportWarn(msg)
}

fun Any.logError(msg: String) {
    Log.e(this::class.java.simpleName, msg)
    LogCollector.reportError(msg)
}

fun Any.logError(throwable: Throwable, msg: String = com.scto.mobile.ide.core.main.R.string.unknown_error.getString()) {
    Log.e(this::class.java.simpleName, msg, throwable)
    LogCollector.reportError("$msg: \n${throwable.stackTraceToString()}")
}
