package com.scto.mobile.ide.features.terminal.service

import android.app.Activity

interface SessionBinderProvider {
    val sessionBinder: SessionService.SessionBinder?
}

val Activity.sessionBinder: SessionService.SessionBinder?
    get() = (this as? SessionBinderProvider)?.sessionBinder
