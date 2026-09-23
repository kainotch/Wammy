package tachiyomi.domain.manga.model

/**
 * Contains the required data for MangaCoverFetcher
 */
data class MangaCover(
    val mangaId: Long,
    val sourceId: Long,
    val isMangaFavorite: Boolean,
    val url: String?,
    val lastModified: Long,
) {
    // KMK -->
    /**
     * [vibrantCoverColor] is used to set the color theme in manga detail page.
     * It contains color for all mangas, both in library or browsing.
     *
     * It reads/saves to a hashmap in [MangaCover.vibrantCoverColorMap] for multiple mangas.
     */
    var vibrantCoverColor: Int?
        get() = vibrantCoverColorMap[mangaId]
        set(value) {
            vibrantCoverColorMap[mangaId] = value
        }

    companion object {
        /**
         * [vibrantCoverColorMap] store color generated while browsing library.
         * It always empty at beginning each time app starts, then add more color while browsing.
         */
        val vibrantCoverColorMap: HashMap<Long, Int?> = hashMapOf()
    }
    // KMK <--
}

fun Manga.asMangaCover(): MangaCover {
    return MangaCover(
        mangaId = id,
        sourceId = source,
        isMangaFavorite = favorite,
        url = thumbnailUrl,
        lastModified = coverLastModified,
    )
}
