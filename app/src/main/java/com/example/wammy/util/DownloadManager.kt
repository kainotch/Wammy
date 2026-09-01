// Created by Notch
package com.example.wammy.util

import androidx.documentfile.provider.DocumentFile
import android.net.Uri
import com.example.wammy.util.StorageHelper
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.wammy.AppContainer
import com.example.wammy.data.local.ChapterEntity
import com.example.wammy.data.local.MangaEntity
import com.example.wammy.extension.awaitFirst
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class DownloadJob(
    val manga: MangaEntity,
    val totalChapters: Int,
    val downloadedChapters: Int,
    val currentChapterName: String = "",
    val isDownloading: Boolean = true,
    val queuedChapterUrls: List<String> = emptyList()
)

object DownloadManager {

    // Single source of truth - keyed by manga sourceUrl
    private val _downloads = MutableStateFlow<Map<String, DownloadJob>>(emptyMap())
    private val _downloadsList = MutableStateFlow<List<DownloadJob>>(emptyList())
    val downloads: StateFlow<List<DownloadJob>> = _downloadsList.asStateFlow()
    
    private fun updateDownloads(newMap: Map<String, DownloadJob>) {
        _downloads.value = newMap
        _downloadsList.value = newMap.values.toList()
    }

    private val jobs = ConcurrentHashMap<String, Job>()
    private val mangaMutex = Mutex()

    fun cleanFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
    }

    fun getMangaDir(context: Context, manga: MangaEntity): File {
        return File(context.filesDir, "downloads/${cleanFileName(manga.sourceName)}/${cleanFileName(manga.titleRomaji)}")
    }

    fun getChapterDir(context: Context, manga: MangaEntity, chapter: ChapterEntity): File {
        return File(getMangaDir(context, manga), cleanFileName(chapter.name))
    }

    // Single long-lived OkHttpClient with sensible timeouts - never create inside a loop!
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val notificationManager: NotificationManager by lazy {
        AppContainer.appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "wammy_downloads", "Downloads", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Book chapter downloads" }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun isDownloading(mangaUrl: String) = _downloads.value.containsKey(mangaUrl)

    fun startDownload(manga: MangaEntity, chapters: List<ChapterEntity>) {
        if (chapters.isEmpty()) return
        if (jobs.containsKey(manga.sourceUrl)) return

        val notificationId = manga.sourceUrl.hashCode()

        _downloads.value = _downloads.value + (manga.sourceUrl to DownloadJob(
            manga = manga,
            totalChapters = chapters.size,
            downloadedChapters = 0,
            currentChapterName = chapters.firstOrNull()?.name ?: "",
            queuedChapterUrls = chapters.map { it.sourceUrl }
        ))

        val builder = NotificationCompat.Builder(AppContainer.appContext, "wammy_downloads")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(manga.titleRomaji)
            .setContentText("Starting download...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setProgress(chapters.size, 0, false)

        notificationManager.notify(notificationId, builder.build())

        val job = GlobalScope.launch(Dispatchers.IO) {
            var downloadedCount = 0
            try {
                for (chapter in chapters) {
                    if (!isActive) break

                    // Update notification and UI state
                    updateDownloads(_downloads.value.toMutableMap().also {
                        val job = it[manga.sourceUrl]!!
                        it[manga.sourceUrl] = job.copy(
                            downloadedChapters = downloadedCount,
                            currentChapterName = chapter.name
                        )
                    })
                    builder.setContentText("${chapter.name} ($downloadedCount/${chapters.size})")
                    builder.setProgress(chapters.size, downloadedCount, false)
                    notificationManager.notify(notificationId, builder.build())

                    // Make sure manga is saved in DB and get its real ID
                    val finalMangaId = mangaMutex.withLock {
                        val dbManga = AppContainer.database.mangaDao().getMangaByUrl(manga.sourceUrl)
                        if (dbManga != null) {
                            AppContainer.database.mangaDao().insertManga(dbManga.copy(downloaded = true))
                            dbManga.id
                        } else {
                            AppContainer.database.mangaDao().insertManga(manga.copy(id = 0L, downloaded = true))
                        }
                    }

                    // Fetch page image URLs from the source
                    val pages: List<String> = try {
                        if (manga.sourceId == 1L) {
                            AppContainer.mangaDexSource.fetchPageList(chapter.sourceUrl)
                        } else {
                            val source = AppContainer.extensionManager.activeSources
                                .find { it.id == manga.sourceId }
                            if (source != null) {
                                val sChapter = eu.kanade.tachiyomi.source.model.SChapter.create()
                                    .apply { url = chapter.sourceUrl }
                                val pageList = kotlinx.coroutines.withTimeoutOrNull(30000) {
                                    kotlinx.coroutines.runInterruptible(kotlinx.coroutines.Dispatchers.IO) {
                                        source.fetchPageList(sChapter).toBlocking().first()
                                    }
                                }
                                pageList?.mapNotNull { page -> page.imageUrl ?: page.url } ?: emptyList()
                            } else {
                                emptyList()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        emptyList()
                    }

                    if (pages.isNotEmpty()) {
                        // Get source-specific request headers
                        var headersMap: Map<String, String>? = null
                        if (manga.sourceId != 1L) {
                            val source = AppContainer.extensionManager.activeSources
                                .find { it.id == manga.sourceId }
                            val httpSource = source as? eu.kanade.tachiyomi.source.online.HttpSource
                            headersMap = httpSource?.headersBuilder()?.build()
                                ?.toMultimap()?.mapValues { it.value.first() }
                        }

                        val chapterDocumentDir = StorageHelper.getChapterDocumentDir(AppContainer.appContext, manga, chapter)
                        val chapterDir = if (chapterDocumentDir == null) {
                            val dir = getChapterDir(AppContainer.appContext, manga, chapter)
                            dir.mkdirs()
                            dir
                        } else null

                        pages.forEachIndexed { index, imageUrl ->
                            if (!isActive) throw kotlinx.coroutines.CancellationException("Download cancelled")
                            if (imageUrl.isBlank()) return@forEachIndexed
                            try {
                                val reqBuilder = Request.Builder().url(imageUrl)
                                headersMap?.forEach { (k, v) -> reqBuilder.addHeader(k, v) }
                                val response = okHttpClient.newCall(reqBuilder.build()).execute()
                                if (response.isSuccessful) {
                                    val body = response.body
                                    if (body != null) {
                                        if (chapterDocumentDir != null) {
                                            var fileDoc = chapterDocumentDir.findFile("page_$index.jpg")
                                            if (fileDoc == null) {
                                                fileDoc = chapterDocumentDir.createFile("image/jpeg", "page_$index.jpg")
                                            }
                                            if (fileDoc != null) {
                                                AppContainer.appContext.contentResolver.openOutputStream(fileDoc.uri)?.use { os ->
                                                    val sink = os.sink().buffer()
                                                    sink.writeAll(body.source())
                                                    sink.close()
                                                }
                                            }
                                        } else {
                                            val file = File(chapterDir!!, "page_$index.jpg")
                                            val sink = file.sink().buffer()
                                            sink.writeAll(body.source())
                                            sink.close()
                                        }
                                    }
                                }
                                response.close()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    if (pages.isNotEmpty()) {
                        // Save chapter record to DB only if we actually got pages
                        AppContainer.database.chapterDao().insertChapters(
                            listOf(chapter.copy(mangaId = finalMangaId))
                        )
                    } else {
                        // If pages is empty, we failed to fetch the page list. 
                        // Do not save it to DB so the user can try again later.
                        android.util.Log.e("DownloadManager", "Failed to fetch pages for chapter: ${chapter.name}, skipping DB insert")
                    }

                    downloadedCount++
                    
                    // Update state to remove this chapter from the queued list so the UI spinner stops
                    updateDownloads(_downloads.value.toMutableMap().also {
                        val job = it[manga.sourceUrl]
                        if (job != null) {
                            it[manga.sourceUrl] = job.copy(
                                downloadedChapters = downloadedCount,
                                queuedChapterUrls = job.queuedChapterUrls - chapter.sourceUrl
                            )
                        }
                    })

                    // Small gap between chapters to avoid rate-limiting
                    delay(300)
                }

                // All done
                builder.setContentText("Download complete! $downloadedCount chapters saved.")
                    .setProgress(0, 0, false)
                    .setOngoing(false)
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                notificationManager.notify(notificationId, builder.build())

            } catch (e: CancellationException) {
                builder.setContentText("Download cancelled")
                    .setProgress(0, 0, false)
                    .setOngoing(false)
                notificationManager.notify(notificationId, builder.build())
                delay(3000)
                notificationManager.cancel(notificationId)
            } catch (e: Exception) {
                e.printStackTrace()
                builder.setContentText("Download failed: ${e.localizedMessage}")
                    .setProgress(0, 0, false)
                    .setOngoing(false)
                notificationManager.notify(notificationId, builder.build())
            } finally {
                _downloads.value = _downloads.value - manga.sourceUrl
                jobs.remove(manga.sourceUrl)
            }
        }
        jobs[manga.sourceUrl] = job
    }

    fun cancelAllDownloads() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        okHttpClient.dispatcher.cancelAll()
        _downloads.value = emptyMap()
    }

    fun cancelDownload(mangaUrl: String) {
        jobs[mangaUrl]?.cancel()
        jobs.remove(mangaUrl)
        _downloads.value = _downloads.value - mangaUrl
    }
}
