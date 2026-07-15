package com.example.layoutpreview

import android.content.Context
import androidx.annotation.LayoutRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Analog zu [SyncedScrollViewHost], aber für RecyclerView-basierte XML-Layouts.
 * Nutzt die Layout-Manager-Metriken für die Fraktions-Berechnung.
 */
@Composable
fun SyncedRecyclerHost(
    @LayoutRes layoutRes: Int,
    syncState: ScrollSyncState,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context: Context ->
            val root = android.view.LayoutInflater.from(context)
                .inflate(layoutRes, null, false)

            val recycler = findRecyclerView(root)
            recycler?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                    val lm = rv.layoutManager as? LinearLayoutManager ?: return
                    val total = lm.itemCount
                    if (total == 0) return
                    val first = lm.findFirstVisibleItemPosition().coerceAtLeast(0)
                    syncState.reportXmlScroll(first, total)
                }
            })

            syncState.onRequestXmlScrollTo = { fraction ->
                recycler?.let { rv ->
                    val lm = rv.layoutManager as? LinearLayoutManager
                    val total = lm?.itemCount ?: 0
                    val target = (fraction * total).toInt().coerceIn(0, maxOf(total - 1, 0))
                    lm?.scrollToPosition(target)
                }
            }

            root
        }
    )
}

private fun findRecyclerView(view: android.view.View): RecyclerView? {
    if (view is RecyclerView) return view
    if (view is android.view.ViewGroup) {
        for (i in 0 until view.childCount) {
            findRecyclerView(view.getChildAt(i))?.let { return it }
        }
    }
    return null
}