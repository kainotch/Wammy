package eu.kanade.tachiyomi.ui.manga

/**
 * In-memory cache for manga cover palette colors.
 * Colors are extracted inside MangaCoverFetcher when covers are loaded,
 * so by the time the user taps a manga, the color is already available.
 */
object PaletteCache {
    val vibrantCoverColorMap = java.util.concurrent.ConcurrentHashMap<Long, Int>()
}