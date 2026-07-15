package com.example.layoutpreview.scroll

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Synchronisiert zwei LazyListState-Instanzen anhand des relativen
 * Scrollfortschritts (0f = oben, 1f = unten), damit unterschiedlich
 * lange/hohe Listen (Compose vs. XML/RecyclerView-Pendant) trotzdem
 * "im Gleichschritt" scrollen.
 *
 * Funktioniert auch für verschachtelte LazyColumns, da jede Ebene
 * ihren eigenen Controller bekommen kann (z. B. äußere Liste + innere
 * horizontale Karussells).
 */
@Stable
class ScrollSyncController(
    val composeListState: LazyListState,
    private val scope: CoroutineScope
) {
    /** Fortschritt 0f..1f, wird von außen (XML-Seite) gesetzt oder von Compose berechnet */
    var progress by mutableFloatStateOf(0f)
        private set

    /** true während einer programmatischen Sync-Bewegung, um Feedback-Loops zu vermeiden */
    private var isSyncing = false

    /** Callback, den die XML-Seite (z. B. RecyclerView/NestedScrollView) registriert */
    var onProgressChangedForXml: ((Float) -> Unit)? = null

    /** Muss in einem LaunchedEffect beobachtet werden, um Compose-Scroll -> progress zu berechnen */
    fun currentComposeProgress(): Float {
        val info = composeListState.layoutInfo
        val totalItems = info.totalItemsCount
        if (totalItems == 0) return 0f
        val visible = info.visibleItemsInfo
        if (visible.isEmpty()) return 0f

        val firstIndex = composeListState.firstVisibleItemIndex
        val firstOffset = composeListState.firstVisibleItemScrollOffset
        val avgItemHeight = visible.sumOf { it.size } / visible.size.coerceAtLeast(1)
        val maxScrollItems = (totalItems - visible.size).coerceAtLeast(1)

        val scrolledItems = firstIndex + (firstOffset.toFloat() / avgItemHeight.coerceAtLeast(1))
        return (scrolledItems / maxScrollItems).coerceIn(0f, 1f)
    }

    /** Wird von einer LaunchedEffect-Schleife aufgerufen, wenn sich die Compose-Liste bewegt */
    fun reportComposeScrolled() {
        if (isSyncing) return
        val p = currentComposeProgress()
        progress = p
        onProgressChangedForXml?.invoke(p)
    }

    /** Wird aufgerufen, wenn die XML-Seite gescrollt wurde (z. B. via NestedScrollConnection Bridge) */
    fun reportXmlScrolled(newProgress: Float) {
        progress = newProgress.coerceIn(0f, 1f)
        syncComposeTo(progress)
    }

    private fun syncComposeTo(targetProgress: Float) {
        val info = composeListState.layoutInfo
        val totalItems = info.totalItemsCount
        val visible = info.visibleItemsInfo
        if (totalItems == 0 || visible.isEmpty()) return

        val avgItemHeight = visible.sumOf { it.size } / visible.size.coerceAtLeast(1)
        val maxScrollItems = (totalItems - visible.size).coerceAtLeast(1)
        val targetScrolledItems = targetProgress * maxScrollItems
        val targetIndex = targetScrolledItems.toInt().coerceIn(0, totalItems - 1)
        val targetOffsetPx = ((targetScrolledItems - targetIndex) * avgItemHeight).roundToInt()

        isSyncing = true
        scope.launch {
            composeListState.scrollToItem(targetIndex, targetOffsetPx)
            isSyncing = false
        }
    }
}

@Composable
fun rememberScrollSyncController(
    listState: LazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
): ScrollSyncController {
    val scope = rememberCoroutineScope()
    val controller = remember(listState) { ScrollSyncController(listState, scope) }

    // Beobachtet Compose-Scroll-Bewegungen und meldet sie an die XML-Seite
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { controller.reportComposeScrolled() }
    }
    return controller
}