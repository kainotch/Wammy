// Created by Notch
package com.example.wammy.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ForegroundInfo
import com.example.wammy.AppContainer
import com.example.wammy.util.NotificationUtils
import com.example.wammy.data.local.ChapterEntity
import com.example.wammy.extension.awaitFirst
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LibraryUpdateWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            NotificationUtils.createChannels(context)
            
            val libraryManga = AppContainer.database.mangaDao().getLibraryMangaList()
            if (libraryManga.isEmpty()) {
                return@withContext Result.success()
            }

            var newChaptersCount = 0

            for ((index, manga) in libraryManga.withIndex()) {
                // Update Foreground Notification
                val progress = index + 1
                val notification = NotificationUtils.getProgressNotificationBuilder(
                    context, 
                    libraryManga.size, 
                    progress, 
                    manga.titleRomaji
                ).build()
                
                // Use setForeground to keep it running
                setForeground(ForegroundInfo(NotificationUtils.LIBRARY_UPDATE_NOTIFICATION_ID, notification))

                try {
                    val source = AppContainer.extensionManager.activeSources.find { it.id == manga.sourceId }
                    if (source == null && manga.sourceId != 1L) continue

                    val sManga = SManga.create().apply { url = manga.sourceUrl }
                    val fetchedChapters = if (manga.sourceId == 1L) {
                        // For MangaDexSource, we need to map the entities back to SChapter if needed,
                        // or just use the local method directly. Wait, MangaDexSource fetchChapterList returns List<ChapterEntity>!
                        // Let's just handle MangaDex Source fetch separately if it doesn't return SChapter.
                        // Actually, let's look up AppContainer.mangaDexSource.fetchChapterList signature
                        emptyList<SChapter>() // Placeholder, we will fix this next
                    } else {
                        source?.fetchChapterList(sManga)?.awaitFirst() ?: emptyList()
                    }

                    if (manga.sourceId != 1L) {
                        val localChapters = AppContainer.database.chapterDao().getChaptersForMangaList(manga.id)
                        val localChapterUrls = localChapters.map { it.sourceUrl }.toSet()

                        val newChapters = fetchedChapters.filter { !localChapterUrls.contains(it.url) }
                        
                        if (newChapters.isNotEmpty()) {
                            newChaptersCount += newChapters.size
                            
                            val entities = newChapters.mapIndexed { i, sChap ->
                                ChapterEntity(
                                    mangaId = manga.id,
                                    sourceUrl = sChap.url,
                                    name = sChap.name,
                                    dateUpload = sChap.date_upload,
                                    chapterNumber = sChap.chapter_number,
                                )
                            }
                            
                            val finalEntities = entities.map { 
                                if (it.chapterNumber == -1f) {
                                    val parsed = com.example.wammy.util.ChapterRecognition.parseChapterNumber(manga.titleRomaji, it.name)
                                    it.copy(chapterNumber = if (parsed == -1f) 0f else parsed)
                                } else it
                            }

                            AppContainer.database.chapterDao().insertChapters(finalEntities)
                        }
                    } else {
                        // Custom MangaDex logic
                        val mdChapters = AppContainer.mangaDexSource.fetchChapters(manga.sourceUrl)
                        val localChapters = AppContainer.database.chapterDao().getChaptersForMangaList(manga.id)
                        val localChapterUrls = localChapters.map { it.sourceUrl }.toSet()
                        
                        val newChapters = mdChapters.filter { !localChapterUrls.contains(it.sourceUrl) }
                        if (newChapters.isNotEmpty()) {
                            newChaptersCount += newChapters.size
                            // Override mangaId to ensure foreign key matches
                            val finalEntities = newChapters.map { it.copy(mangaId = manga.id) }
                            AppContainer.database.chapterDao().insertChapters(finalEntities)
                        }
                    }

                } catch (e: Exception) {
                    Log.e("LibraryUpdate", "Failed to update ${manga.titleRomaji}", e)
                }
            }

            // Show summary notification
            NotificationUtils.showSummaryNotification(context, newChaptersCount)
            
            Result.success()
        } catch (e: Exception) {
            Log.e("LibraryUpdateWorker", "Worker failed", e)
            Result.failure()
        }
    }
}
