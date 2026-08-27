// Created by Notch
package com.example.wammy.data.backup

import android.content.Context
import android.net.Uri
import com.example.wammy.data.local.WammyDatabase
import com.example.wammy.data.backup.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.encodeToByteArray
import okio.buffer
import okio.sink
import java.io.OutputStream

class BackupManager(private val context: Context, private val database: WammyDatabase) {

    suspend fun createBackup(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val mangas = database.backupDao().getAllMangaSync()
            val backupMangas = mutableListOf<BackupManga>()
            
            for (manga in mangas) {
                val chapters = database.chapterDao().getChaptersForMangaSync(manga.id)
                val tracks = database.trackDao().getTracksForMangaSync(manga.id)
                val folders = database.backupDao().getFoldersForMangaSync(manga.id)
                // History mapping might be complex, skipping for simplicity in clone
                
                val backupChapters = chapters.map {
                    BackupChapter(
                        url = it.sourceUrl,
                        name = it.name,
                        scanlator = null,
                        read = it.read,
                        bookmark = false,
                        lastPageRead = it.lastPageRead.toLong(),
                        chapterNumber = it.chapterNumber,
                        sourceOrder = 0L,
                        dateFetch = System.currentTimeMillis(),
                        dateUpload = it.dateUpload,
                        lastModifiedAt = System.currentTimeMillis(),
                        version = 1L,
                        memo = ByteArray(0)
                    )
                }

                val backupTracks = tracks.map {
                    BackupTracking(
                        syncId = it.syncId,
                        libraryId = it.remoteId,
                        mediaIdInt = it.remoteId.toInt(),
                        trackingUrl = it.trackingUrl,
                        title = it.title,
                        lastChapterRead = it.lastChapterRead,
                        totalChapters = it.totalChapters,
                        score = it.score,
                        status = it.status,
                        startedReadingDate = 0L,
                        finishedReadingDate = 0L,
                        private = false,
                        mediaId = it.remoteId
                    )
                }

                val backupManga = BackupManga(
                    source = manga.sourceId,
                    url = manga.sourceUrl,
                    title = manga.titleRomaji,
                    artist = manga.artist,
                    author = manga.author,
                    description = manga.description,
                    genre = manga.genre?.split(", ") ?: emptyList(),
                    status = mapStatus(manga.status),
                    thumbnail_url = manga.coverImageUrl,
                    dateAdded = System.currentTimeMillis(),
                    viewer = 0,
                    chapters = backupChapters,
                    categories = folders.map { it.folderId },
                    tracking = backupTracks,
                    favorite = manga.favorite,
                    chapterFlags = 0,
                    viewer_flags = 0,
                    history = emptyList(),
                    updateStrategy = 0,
                    lastModifiedAt = System.currentTimeMillis(),
                    favoriteModifiedAt = if (manga.favorite) System.currentTimeMillis() else null,
                    excludedScanlators = emptyList(),
                    version = 1L,
                    notes = "",
                    initialized = true,
                    memo = ByteArray(0)
                )
                backupMangas.add(backupManga)
            }

            val allFolders = database.backupDao().getAllFoldersSync()
            val backupCategories = allFolders.map {
                BackupCategory(
                    name = it.name,
                    order = it.sortOrder.toLong(),
                    id = it.id,
                    flags = 0L
                )
            }

            val backup = Backup(
                backupManga = backupMangas,
                backupCategories = backupCategories,
                backupSources = emptyList(),
                backupPreferences = emptyList(),
                backupExtensionStores = emptyList()
            )

            val byteArray = ProtoBuf.encodeToByteArray(backup)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                java.util.zip.GZIPOutputStream(outputStream).use { gzipStream ->
                    gzipStream.sink().buffer().use { sink ->
                        sink.write(byteArray)
                    }
                }
            }
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    private fun mapStatus(status: String?): Int {
        return when (status?.lowercase()) {
            "ongoing" -> 1
            "completed" -> 2
            "licensed" -> 3
            "publishing finished" -> 2
            "cancelled" -> 4
            "on hiatus" -> 5
            else -> 0
        }
    }
}
