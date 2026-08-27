// Created by Notch
package com.example.wammy.data.remote.anilist

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Headers

interface AniListApi {
    @Headers("Content-Type: application/json", "Accept: application/json")
    @POST("/")
    suspend fun query(@Body body: GraphQLRequest): GraphQLResponse
}

data class GraphQLRequest(
    val query: String,
    val variables: Map<String, Any>? = null
)

data class GraphQLResponse(
    val data: AniListData?
)

data class AniListData(
    val Page: AniListPage?
)

data class AniListPage(
    val media: List<AniListMedia>?
)

data class AniListMedia(
    val id: Int,
    val title: AniListTitle?,
    val coverImage: AniListCoverImage?,
    val description: String?,
    val episodes: Int?,
    val chapters: Int?
)

data class AniListTitle(
    val romaji: String?,
    val english: String?,
    val native: String?
)

data class AniListCoverImage(
    val large: String?
)
