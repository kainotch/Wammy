// Created by Notch
package com.example.wammy.track.mal

import android.content.Context
import com.example.wammy.data.local.TrackEntity
import com.example.wammy.track.TrackService
import com.example.wammy.track.TrackSearchItem

class MalTracker(private val context: Context, override val id: Int) : TrackService {
    override val name: String = "MyAnimeList"
    
    private val prefs = context.getSharedPreferences("mal_auth", Context.MODE_PRIVATE)
    
    fun getAuthUrl(): String {
        val clientId = "mal_client_placeholder"
        return "https://myanimelist.net/v1/oauth2/authorize?response_type=token&client_id=$clientId"
    }

    override fun isLogged(): Boolean {
        return prefs.getString("access_token", null) != null
    }

    fun saveToken(token: String) {
        prefs.edit().putString("access_token", token).apply()
    }

    override fun logout() {
        prefs.edit().remove("access_token").apply()
    }

    override suspend fun search(query: String): List<TrackSearchItem> {
        return listOf(
            TrackSearchItem(1, "$query (MAL)", "", "", "https://myanimelist.net/manga/1")
        )
    }

    override suspend fun update(track: TrackEntity, isCompleted: Boolean): TrackEntity {
        return track
    }

    override suspend fun bind(track: TrackEntity): TrackEntity {
        return track
    }
}
