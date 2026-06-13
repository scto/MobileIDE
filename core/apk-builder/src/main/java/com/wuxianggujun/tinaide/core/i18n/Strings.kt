package com.wuxianggujun.tinaide.core.i18n

import android.content.Context
import com.wuxianggujun.tinaide.core.apkbuilder.R

object Strings {
    val apk_builder_terminal_executable_missing = R.string.apk_builder_terminal_executable_missing
    val apk_builder_step_prepare_template = R.string.apk_builder_step_prepare_template
    val apk_builder_template_missing = R.string.apk_builder_template_missing
    val apk_builder_step_inject_terminal_payload = R.string.apk_builder_step_inject_terminal_payload
    val apk_builder_step_inject_so = R.string.apk_builder_step_inject_so
    val apk_builder_step_align = R.string.apk_builder_step_align
    val apk_builder_step_sign = R.string.apk_builder_step_sign
    val apk_builder_debug_keystore_unavailable = R.string.apk_builder_debug_keystore_unavailable
    val apk_builder_step_completed = R.string.apk_builder_step_completed
    val apk_builder_failed = R.string.apk_builder_failed
}

fun Int.strOr(context: Context?, vararg formatArgs: Any?): String {
    if (context == null) return "String resource ID: $this"
    return try {
        context.getString(this, *formatArgs)
    } catch (e: Exception) {
        "String resource ID: $this"
    }
}
