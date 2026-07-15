package com.example.layoutpreview

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * Synchronisiert zwei LazyListStates (Compose-Seite <-> XML-Seite via RecyclerView/ScrollView)
 * bidirektional, sodass beim Scrollen der einen Liste die andere im selben
 * relativen Verhältnis mitscrollt.
 */
@Stable
class ScrollSyncState {

    /** State für die Compose-LazyColumn */
    val composeListState = LazyListState()

    /** State für die XML-Seite (wird per NestedScrollConnection an RecyclerView/ScrollView angebunden) */
    var xmlScrollY by mutableIntStateOf(0)
    var xmlMaxScrollY by mutableIntStateOf(1) // vermeidet Division durch 0

    /** Aktiviert/deaktiviert die Synchronisation zur Laufzeit (Toolbar-Toggle) */
    var enabled by mutableStateOf(true)

    /** Verhindert Endlos-Rückkopplung zwischen den beiden Quellen */
    private var isSyncingFromCompose = false
    private var isSyncingFromXml = false

    /** Callback, den die XML-Seite (View-Welt) aufruft, wenn sie programmatisch scrollen soll */
    var onRequestXmlScrollTo: ((fraction: Float) -> Unit)? = null

    /**
     * Muss einmalig im Compose-Baum aufgerufen werden (z. B. in LayoutPreviewScreen),
     * um die Synchronisations-Loops zu starten.
     */
    @Composable
    fun AttachSync() {
        // Compose -> XML: relative Scroll-Fraktion berechnen und an View-Seite senden
        LaunchedEffect(composeListState, enabled) {
            snapshotFlow {
                val layoutInfo = composeListState.layoutInfo
                val totalItems = layoutInfo.totalItemsCount
                if (totalItems == 0) return@snapshotFlow 0f
                val firstVisible = composeListState.firstVisibleItemIndex
                val offset = composeListState.firstVisibleItemScrollOffset
                val avgItemSize = layoutInfo.visibleItemsInfo
                    .firstOrNull()?.size?.toFloat() ?: 1f
                val approxScrolled = firstVisible * avgItemSize + offset
                val approxTotal = totalItems * avgItemSize
                if (approxTotal <= 0f) 0f else (approxScrolled / approxTotal).coerceIn(0f, 1f)
            }
                .distinctUntilChanged()
                .filter { enabled && !isSyncingFromXml }
                .collectLatest { fraction ->
                    isSyncingFromCompose = true
                    onRequestXmlScrollTo?.invoke(fraction)
                    isSyncingFromCompose = false
                }
        }

        // XML -> Compose: normierte Fraktion aus xmlScrollY berechnen und Liste scrollen
        LaunchedEffect(xmlScrollY, xmlMaxScrollY, enabled) {
            if (!enabled || isSyncingFromCompose) return@LaunchedEffect
            val fraction = if (xmlMaxScrollY <= 0) 0f
                            else (xmlScrollY.toFloat() / xmlMaxScrollY).coerceIn(0f, 1f)

            val layoutInfo = composeListState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems == 0) return@LaunchedEffect

            isSyncingFromXml = true
            val targetIndex = (fraction * totalItems).toInt().coerceIn(0, totalItems - 1)
            composeListState.scrollToItem(targetIndex)
            isSyncingFromXml = false
        }
    }

    /** Von der View-Seite (NestedScrollConnection / ScrollListener) bei jedem Scroll-Event aufrufen */
    fun reportXmlScroll(scrollY: Int, maxScrollY: Int) {
        xmlMaxScrollY = maxOf(maxScrollY, 1)
        xmlScrollY = scrollY
    }
}

@Composable
fun rememberScrollSyncState(): ScrollSyncState = remember { ScrollSyncState() }