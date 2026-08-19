package com.scto.mobile.ide.resources





import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.blankj.utilcode.util.StringUtils












fun getString(@StringRes id: Int): String {
    return StringUtils.getString(id)
}

fun getString(@StringRes id: Int, vararg formatArgs: Any): String {
    return StringUtils.getString(id, *formatArgs)
}
