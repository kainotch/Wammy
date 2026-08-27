// Created by Notch
package com.example.wammy.data.remote.mangadex

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MangaDexApi {
    @GET("manga")
    suspend fun getMangaList(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("title") title: String? = null,
        @Query("availableTranslatedLanguage[]") languages: List<String> = listOf("en"),
        @Query("contentRating[]") contentRating: List<String> = listOf("safe", "suggestive"),
        @Query("includes[]") includes: List<String> = listOf("cover_art", "author", "artist")
    ): MangaResponse

    @GET("manga/{id}/feed")
    suspend fun getMangaFeed(
        @Path("id") mangaId: String,
        @Query("limit") limit: Int = 500,
        @Query("translatedLanguage[]") languages: List<String> = listOf("en")
    ): ChapterFeedResponse

    @GET("at-home/server/{chapterId}")
    suspend fun getAtHomeServer(
        @Path("chapterId") chapterId: String
    ): AtHomeResponse
}

// Data Classes for Retrofit Parsing
data class MangaResponse(
    val data: List<MangaDexManga>
)

data class MangaDexManga(
    val id: String,
    val attributes: MangaAttributes,
    val relationships: List<MangaRelationship>? = null
)

data class MangaAttributes(
    val title: Map<String, String>,
    val description: Map<String, String>,
    val status: String? = null,
    val tags: List<Tag>? = null
)

data class ChapterFeedResponse(
    val data: List<MangaDexChapter>
)

data class MangaDexChapter(
    val id: String,
    val attributes: ChapterAttributes
)

data class ChapterAttributes(
    val chapter: String?,
    val title: String?,
    val pages: Int
)

data class AtHomeResponse(
    val baseUrl: String,
    val chapter: AtHomeChapter
)

data class AtHomeChapter(
    val hash: String,
    val data: List<String>,
    val dataSaver: List<String>
)

data class MangaRelationship(
    val id: String,
    val type: String,
    val attributes: RelationshipAttributes? = null
)

data class RelationshipAttributes(
    val fileName: String? = null,
    val name: String? = null
)

data class Tag(val attributes: TagAttributes?)
data class TagAttributes(val name: Map<String, String>?)
