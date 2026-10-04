package eu.kanade.tachiyomi.data.sync

import androidx.annotation.Keep

/**
 * Represents a user's favorite manga/novel entry stored in Firestore.
 * This is a denormalized snapshot of the manga metadata at the time
 * the user added it to their Top 5.
 */
@Keep
data class FavoriteManga(
    val title: String = "",
    val author: String = "",
    val description: String = "",
    val thumbnailUrl: String = "",
    val genres: List<String> = emptyList(),
    val sourceName: String = "",
    val sourceId: Long = 0L,
    val mangaUrl: String = "",
    val status: Long = 0L,
    val order: Int = 0,
)
