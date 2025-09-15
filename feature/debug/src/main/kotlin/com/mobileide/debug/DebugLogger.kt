package com.mobileide.debug

import com.mobileide.debug.data.LogRepository

import timber.log.Timber
import javax.inject.Inject

class DebugLogger @Inject constructor(private val logRepository: LogRepository) : Timber.DebugTree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        logRepository.addLog(priority, tag, message)
        super.log(priority, tag, message, t)
    }
}
