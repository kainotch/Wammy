// Created by Notch
package com.example.wammy.track

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.wammy.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.firstOrNull

class TrackSyncWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val mangaId = inputData.getLong("mangaId", -1L)
            val chapterNumber = inputData.getFloat("chapterNumber", -1f)
            val isCompleted = inputData.getBoolean("isCompleted", false)

            if (mangaId == -1L || chapterNumber == -1f) return@withContext Result.failure()

            val tracks = AppContainer.database.trackDao().getTracksForManga(mangaId).firstOrNull()
            
            if (tracks.isNullOrEmpty()) return@withContext Result.success()

            for (track in tracks) {
                if (track.lastChapterRead >= chapterNumber) continue // Already ahead
                
                val service = AppContainer.trackManager.getService(track.syncId) ?: continue
                if (!service.isLogged()) continue

                try {
                    val updatedTrack = track.copy(lastChapterRead = chapterNumber)
                    val syncedTrack = service.update(updatedTrack, isCompleted)
                    AppContainer.database.trackDao().updateTrack(syncedTrack)
                } catch (e: Exception) {
                    Log.e("TrackSync", "Failed to sync to ${service.name}", e)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
