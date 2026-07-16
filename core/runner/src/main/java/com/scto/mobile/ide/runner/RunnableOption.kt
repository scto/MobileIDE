package com.scto.mobile.ide.runner

import android.app.Activity
import android.content.Context
import com.scto.mobile.ide.core.common.icons.Icon

interface RunnableOption {
    val label: String

    fun getIcon(context: Context): Icon?

    fun run(activity: Activity)
}
