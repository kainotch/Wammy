package eu.kanade.tachiyomi.ui.manga

import androidx.compose.runtime.mutableStateMapOf

/**
 * In-memory cache for manga cover palette colors.
 * Uses Compose's mutableStateMapOf so the UI automatically recomposes
 * when a new color is added by MangaCoverFetcher.
 */
object PaletteCache {
    val vibrantCoverColorMap = mutableStateMapOf<Long, Int>()
}