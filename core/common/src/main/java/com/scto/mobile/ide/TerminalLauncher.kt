package com.scto.mobile.ide

import android.app.Activity

object TerminalLauncher {
    var handler: ((Activity, Boolean, String, Array<String>, String, Boolean, String?, Array<String>?) -> Unit)? = null
    
    fun launch(activity: Activity, sandbox: Boolean = false, exe: String, args: Array<String>, id: String, terminatePreviousSession: Boolean = false, workingDir: String? = null, env: Array<String>? = null) {
        handler?.invoke(activity, sandbox, exe, args, id, terminatePreviousSession, workingDir, env)
    }
}
