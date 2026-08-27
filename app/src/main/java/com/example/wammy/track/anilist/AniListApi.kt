// Created by Notch
package com.example.wammy.track.anilist

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AniListApi {
    @POST("/")
    suspend fun query(
        @Header("Authorization") token: String,
        @Header("Accept") accept: String = "application/json",
        @Header("Content-Type") contentType: String = "application/json",
        @Body payload: Map<String, Any>
    ): ALResponse
}

data class ALResponse(
    val data: ALData? = null
)

data class ALData(
    val Page: ALPage? = null,
    val SaveMediaListEntry: ALMediaList? = null
)

data class ALPage(
    val media: List<ALMedia>? = null
)

data class ALMedia(
    val id: Long,
    val title: ALTitle,
    val coverImage: ALCoverImage,
    val description: String?,
    val siteUrl: String
)

data class ALTitle(
    val romaji: String,
    val english: String?
)

data class ALCoverImage(
    val large: String
)

data class ALMediaList(
    val id: Long,
    val status: String,
    val score: Float,
    val progress: Int,
    val mediaId: Long
)
