package com.example.layoutpreview

import android.content.Context
import android.widget.ScrollView
import androidx.annotation.LayoutRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Wie [XmlLayoutHost], aber zusätzlich an [ScrollSyncState] gekoppelt:
 * meldet Scroll-Events der View-Seite und kann programmatisch gescrollt werden.
 *
 * Voraussetzung: Das XML-Root-Layout ist (oder enthält) eine ScrollView/NestedScrollView.
 * Für RecyclerViews siehe [SyncedRecyclerHost] weiter unten.
 */
@Composable
fun SyncedScrollViewHost(
    @LayoutRes layoutRes: Int,
    syncState: ScrollSyncState,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context: Context ->
            val root = android.view.LayoutInflater.from(context)
                .inflate(layoutRes, null, false)

            val scrollView = findScrollView(root)
            scrollView?.setOnScrollChangeListener { v, _, scrollY, _, _ ->
                val maxScroll = (v.getChildAt(0)?.height ?: 0) - v.height
                syncState.reportXmlScroll(scrollY, maxOf(maxScroll, 1))
            }

            syncState.onRequestXmlScrollTo = { fraction ->
                scrollView?.let { sv ->
                    val maxScroll = (sv.getChildAt(0)?.height ?: 0) - sv.height
                    val target = (fraction * maxOf(maxScroll, 1)).toInt()
                    sv.scrollTo(0, target)
                }
            }

            root
        }
    )

    LaunchedEffect(Unit) { /* Sync-Loop wird zentral über syncState.AttachSync() gestartet */ }
}

private fun findScrollView(view: android.view.View): ScrollView? {
    if (view is ScrollView) return view
    if (view is android.view.ViewGroup) {
        for (i in 0 until view.childCount) {
            findScrollView(view.getChildAt(i))?.let { return it }
        }
    }
    return null
}