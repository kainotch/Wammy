// Created by Notch
package com.example.wammy.track.anilist

import android.content.Context
import android.content.SharedPreferences
import com.example.wammy.data.local.TrackEntity
import com.example.wammy.track.TrackSearchItem
import com.example.wammy.track.TrackService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AniListTracker(private val context: Context, override val id: Int) : TrackService {

    override val name: String = "AniList"

    private val prefs: SharedPreferences = context.getSharedPreferences("anilist_prefs", Context.MODE_PRIVATE)

    private val api: AniListApi = Retrofit.Builder()
        .baseUrl("https://graphql.anilist.co")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(AniListApi::class.java)

    // For real OAuth, we'd need a registered client ID. 
    // Mihon's AniList client ID is 3591. We will use a standard OAuth implicit flow pattern.
    fun getAuthUrl(): String {
        val clientId = "3591" // Mihon/Tachiyomi AniList ID placeholder
        return "https://anilist.co/api/v2/oauth/authorize?client_id=$clientId&response_type=token"
    }

    fun saveToken(token: String) {
        prefs.edit().putString("access_token", token).apply()
    }

    private fun getToken(): String? {
        val token = prefs.getString("access_token", null)
        return if (token != null) "Bearer $token" else null
    }

    override fun isLogged(): Boolean {
        return prefs.contains("access_token")
    }

    override fun logout() {
        prefs.edit().remove("access_token").apply()
    }

    override suspend fun search(query: String): List<TrackSearchItem> {
        val token = getToken() ?: throw Exception("Not authenticated with AniList")
        
        val graphql = """
            query Search(${'$'}query: String) {
                Page(perPage: 50) {
                    media(search: ${'$'}query, type: MANGA, format_in: [MANGA, ONE_SHOT]) {
                        id
                        title { romaji english }
                        coverImage { large }
                        description
                        siteUrl
                    }
                }
            }
        """.trimIndent()
        
        val payload = mapOf(
            "query" to graphql,
            "variables" to mapOf("query" to query)
        )
        
        val response = api.query(token = token, payload = payload)
        
        return response.data?.Page?.media?.map { media ->
            TrackSearchItem(
                remoteId = media.id,
                title = media.title.romaji,
                coverUrl = media.coverImage.large,
                summary = media.description ?: "",
                trackingUrl = media.siteUrl
            )
        } ?: emptyList()
    }

    override suspend fun update(track: TrackEntity, isCompleted: Boolean): TrackEntity {
        val token = getToken() ?: throw Exception("Not authenticated with AniList")
        
        val graphql = """
            mutation UpdateTrack(${'$'}mediaId: Int, ${'$'}progress: Int, ${'$'}status: MediaListStatus) {
                SaveMediaListEntry(mediaId: ${'$'}mediaId, progress: ${'$'}progress, status: ${'$'}status) {
                    id
                    status
                    progress
                    score
                }
            }
        """.trimIndent()

        // Map status: 0=Reading, 1=Completed, etc. For simplicity, just use CURRENT or COMPLETED
        val statusString = if (isCompleted) "COMPLETED" else "CURRENT"
        
        val payload = mapOf(
            "query" to graphql,
            "variables" to mapOf(
                "mediaId" to track.remoteId.toInt(),
                "progress" to track.lastChapterRead.toInt(),
                "status" to statusString
            )
        )

        api.query(token = token, payload = payload)
        
        return track.copy(status = if (isCompleted) 1 else 0)
    }

    override suspend fun bind(track: TrackEntity): TrackEntity {
        // Just calls update to create the initial entry on AniList
        return update(track)
    }
}
