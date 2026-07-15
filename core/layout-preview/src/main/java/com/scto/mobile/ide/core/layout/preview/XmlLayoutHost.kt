package com.scto.mobile.ide.core.layout.preview

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Hostet ein klassisches XML-Layout (R.layout.xxx) innerhalb von Compose,
 * z. B. um es 1:1 gegen ein Compose-Pendant zu vergleichen.
 */
@Composable
fun XmlLayoutHost(
    @LayoutRes layoutRes: Int,
    modifier: Modifier = Modifier,
    onViewInflated: ((android.view.View) -> Unit)? = null
) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context: Context ->
            LayoutInflater.from(context).inflate(layoutRes, null, false).also {
                it.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                onViewInflated?.invoke(it)
            }
        },
        update = { /* bei Bedarf: Bindings aktualisieren */ }
    )
}