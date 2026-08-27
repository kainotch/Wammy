// Created by Notch
package com.example.wammy.track.kitsu

import android.content.Context
import com.example.wammy.data.local.TrackEntity
import com.example.wammy.track.TrackService
import com.example.wammy.track.TrackSearchItem

class KitsuTracker(private val context: Context, override val id: Int) : TrackService {
    override val name: String = "Kitsu"
    
    private val prefs = context.getSharedPreferences("kitsu_auth", Context.MODE_PRIVATE)
    
    fun getAuthUrl(): String {
        return "https://kitsu.io/api/oauth/authorize?response_type=token&client_id=kitsu_client_placeholder"
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
            TrackSearchItem(1, "$query (Kitsu)", "", "", "https://kitsu.io/manga/1")
        )
    }

    override suspend fun update(track: TrackEntity, isCompleted: Boolean): TrackEntity {
        return track
    }

    override suspend fun bind(track: TrackEntity): TrackEntity {
        return track
    }
}
