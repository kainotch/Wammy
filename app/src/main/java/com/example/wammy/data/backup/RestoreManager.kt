// Created by Notch
package com.example.wammy.data.backup

import android.content.Context
import android.net.Uri
import com.example.wammy.data.local.*
import com.example.wammy.data.backup.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.room.withTransaction
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.decodeFromByteArray
import okio.buffer
import okio.source

data class RestoreOptions(
    val library: Boolean = true,
    val categories: Boolean = true
)

class RestoreManager(private val context: Context, private val database: WammyDatabase) {

    @kotlinx.serialization.ExperimentalSerializationApi
    private val protoBuf = ProtoBuf

    suspend fun parseBackup(uri: Uri): Backup? = withContext(Dispatchers.IO) {
        try {
            val byteArray = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                java.util.zip.GZIPInputStream(inputStream).use { gzipStream ->
                    gzipStream.source().buffer().use { source ->
                        source.readByteArray()
                    }
                }
            } ?: return@withContext null

            protoBuf.decodeFromByteArray<Backup>(byteArray)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun restoreBackup(backup: Backup, options: RestoreOptions): Boolean = withContext(Dispatchers.IO) {
        try {
            val folderMap = mutableMapOf<Long, Long>()
            
            database.withTransaction {
                // 1. Restore Folders (Categories)
                if (options.categories) {
                    for (backupCategory in backup.backupCategories) {
                        val existingFolder = database.backupDao().getAllFoldersSync().find { it.name == backupCategory.name }
                        val newFolderId = if (existingFolder != null) {
                            existingFolder.id
                        } else {
                            database.folderDao().insertFolder(FolderEntity(
                                name = backupCategory.name,
                                sortOrder = backupCategory.order.toInt()
                            ))
                        }
                        folderMap[backupCategory.id] = newFolderId
                    }
                }

                // 2. Restore Manga
                if (options.library) {
                    for (backupManga in backup.backupManga) {
                        var manga = database.mangaDao().getMangaByUrlAndSource(backupManga.url, backupManga.source)
                        val newMangaId = if (manga != null) {
                            database.mangaDao().insertManga(manga.copy(favorite = backupManga.favorite))
                            manga.id
                        } else {
                            val sourceObj = backup.backupSources.find { it.sourceId == backupManga.source }
                            val extName = sourceObj?.name ?: "Unknown Source"
                            
                            val newManga = MangaEntity(
                                aniListId = null,
                                titleRomaji = backupManga.title,
                                coverImageUrl = backupManga.thumbnail_url,
                                description = backupManga.description,
                                sourceId = backupManga.source,
                                sourceUrl = backupManga.url,
                                author = backupManga.author,
                                artist = backupManga.artist,
                                status = reverseMapStatus(backupManga.status),
                                sourceName = extName,
                                genre = backupManga.genre.joinToString(", "),
                                favorite = backupManga.favorite,
                                readCompleted = false,
                                downloaded = false
                            )
                            database.mangaDao().insertManga(newManga)
                        }

                        for (backupChapter in backupManga.chapters) {
                            val newChapter = ChapterEntity(
                                mangaId = newMangaId,
                                sourceUrl = backupChapter.url,
                                name = backupChapter.name,
                                chapterNumber = backupChapter.chapterNumber,
                                dateUpload = backupChapter.dateUpload,
                                read = backupChapter.read,
                                lastPageRead = backupChapter.lastPageRead.toInt()
                            )
                            database.chapterDao().insertChapters(listOf(newChapter))
                        }

                        for (backupTrack in backupManga.tracking) {
                            val newTrack = TrackEntity(
                                mangaId = newMangaId,
                                syncId = backupTrack.syncId,
                                remoteId = backupTrack.mediaId,
                                title = backupTrack.title,
                                lastChapterRead = backupTrack.lastChapterRead,
                                totalChapters = backupTrack.totalChapters,
                                score = backupTrack.score,
                                status = backupTrack.status,
                                trackingUrl = backupTrack.trackingUrl
                            )
                            database.trackDao().insertTrack(newTrack)
                        }

                        if (options.categories) {
                            for (categoryId in backupManga.categories) {
                                val newFolderId = folderMap[categoryId]
                                if (newFolderId != null) {
                                    database.folderDao().insertMangaFolderCrossRef(MangaFolderCrossRef(newMangaId, newFolderId))
                                }
                            }
                        }

                        for (history in backupManga.history) {
                            val backupChapter = backupManga.chapters.find { it.url == history.url }
                            if (backupChapter != null) {
                                val historyEntity = HistoryEntity(
                                    mangaSourceUrl = backupManga.url,
                                    mangaTitle = backupManga.title,
                                    mangaCoverUrl = backupManga.thumbnail_url ?: "",
                                    chapterSourceUrl = backupChapter.url,
                                    chapterName = backupChapter.name,
                                    lastPageRead = backupChapter.lastPageRead.toInt(),
                                    totalPages = 0,
                                    lastReadTimestamp = history.lastRead
                                )
                                database.historyDao().insertHistory(historyEntity)
                            }
                        }
                    }
                }
            } // end of withTransaction
            
            // 3. Install missing extensions
            if (options.library) {
                try {
                    val uniqueSources = backup.backupManga.map { it.source }.distinct()
                    if (uniqueSources.isNotEmpty()) {
                        val repoUrl = "https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.json"
                        val repo = com.example.wammy.AppContainer.extensionApi.getExtensions(repoUrl)
                        val allExtensions = repo.extensionList.extensions
                        
                        for (sourceId in uniqueSources) {
                            val sourceIdStr = sourceId.toString()
                            val extensionToInstall = allExtensions.find { ext ->
                                ext.sources.any { it.id == sourceIdStr }
                            }
                            if (extensionToInstall != null) {
                                com.example.wammy.AppContainer.extensionManager.downloadAndInstallExtension(extensionToInstall)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    private fun reverseMapStatus(status: Int): String {
        return when (status) {
            1 -> "Ongoing"
            2 -> "Completed"
            3 -> "Licensed"
            4 -> "Cancelled"
            5 -> "On Hiatus"
            else -> "Unknown"
        }
    }
}
