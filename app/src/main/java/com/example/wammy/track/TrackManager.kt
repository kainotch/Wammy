// Created by Notch
package com.example.wammy.track

import android.content.Context
import com.example.wammy.track.anilist.AniListTracker
import com.example.wammy.track.mal.MalTracker
import com.example.wammy.track.kitsu.KitsuTracker

class TrackManager(context: Context) {
    val aniList = AniListTracker(context, 1)
    val myAnimeList = MalTracker(context, 2)
    val kitsu = KitsuTracker(context, 3)

    val services: List<TrackService> = listOf(aniList, myAnimeList, kitsu)

    fun getService(id: Int): TrackService? {
        return services.find { it.id == id }
    }
}
