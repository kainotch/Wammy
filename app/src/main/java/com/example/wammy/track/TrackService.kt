// Created by Notch
package com.example.wammy.track

import com.example.wammy.data.local.TrackEntity

interface TrackService {
    val id: Int
    val name: String
    
    fun isLogged(): Boolean
    fun logout()
    
    suspend fun search(query: String): List<TrackSearchItem>
    suspend fun update(track: TrackEntity, isCompleted: Boolean = false): TrackEntity
    suspend fun bind(track: TrackEntity): TrackEntity
}

data class TrackSearchItem(
    val remoteId: Long,
    val title: String,
    val coverUrl: String,
    val summary: String,
    val trackingUrl: String
) {
    fun toTrackEntity(mangaId: Long, syncId: Int): TrackEntity {
        return TrackEntity(
            mangaId = mangaId,
            syncId = syncId,
            remoteId = remoteId,
            title = title,
            trackingUrl = trackingUrl
        )
    }
}
