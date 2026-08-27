// Created by Notch
package com.example.wammy.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wammy.AppContainer
import com.example.wammy.data.local.ChapterEntity
import com.example.wammy.data.local.MangaEntity
import com.example.wammy.extension.awaitFirst
import eu.kanade.tachiyomi.source.model.SChapter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import java.io.File

enum class PageState {
    QUEUE, LOAD_PAGE, DOWNLOAD_IMAGE, READY, ERROR
}

data class ReaderPage(
    val index: Int,
    val url: String = "",
    val imageUrl: String? = null,
    val headers: Map<String, String>? = null,
    val state: PageState = PageState.QUEUE,
    val progress: Int = 0,
    val sourcePage: eu.kanade.tachiyomi.source.model.Page? = null
)

enum class ReadingMode {
    WEBTOON, LTR, RTL
}

class ReaderViewModel : ViewModel() {
    private val historyMutex = kotlinx.coroutines.sync.Mutex()
    
    private val _pages = MutableStateFlow<List<ReaderPage>>(emptyList())
    val pages: StateFlow<List<ReaderPage>> = _pages.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _chapterName = MutableStateFlow("")
    val chapterName: StateFlow<String> = _chapterName.asStateFlow()
    
    private val _readingMode = MutableStateFlow(ReadingMode.RTL)
    val readingMode: StateFlow<ReadingMode> = _readingMode.asStateFlow()
    private val _colorFilter = MutableStateFlow(ColorFilterMode.NONE)
    val colorFilter: StateFlow<ColorFilterMode> = _colorFilter.asStateFlow()

    private val _webtoonSidePadding = MutableStateFlow(0)
    val webtoonSidePadding: StateFlow<Int> = _webtoonSidePadding.asStateFlow()

    private val _initialPage = MutableStateFlow(0)
    val initialPage: StateFlow<Int> = _initialPage.asStateFlow()

    private var chapters: List<ChapterEntity> = emptyList()
    private var currentChapterIndex: Int = -1
    private var sourceId: Long = 1L
    private var activeManga: MangaEntity? = null

    // In a descending list (Ch 100 = 0, Ch 99 = 1), the "Next" chronological chapter is index - 1
    val hasNextChapter: Boolean get() = currentChapterIndex > 0
    val hasPrevChapter: Boolean get() = currentChapterIndex < chapters.size - 1

    fun initReader(chaptersList: List<ChapterEntity>, startIndex: Int, source: Long, manga: MangaEntity) {
        chapters = chaptersList
        currentChapterIndex = startIndex
        sourceId = source
        activeManga = manga
        loadCurrentChapter()
    }

    private fun loadCurrentChapter() {
        if (currentChapterIndex !in chapters.indices) return
        val chapter = chapters[currentChapterIndex]
        val manga = activeManga ?: return
        _chapterName.value = chapter.name
        _initialPage.value = chapter.lastPageRead
        
        chapterLoadJob?.cancel()
        chapterLoadJob = viewModelScope.launch {
            _isLoading.value = true
            _pages.value = emptyList()
            try {
                // Check local storage first (Downloads)
                val context = AppContainer.appContext
                val storageDir = com.example.wammy.util.DownloadManager.getChapterDir(context, manga, chapter)
                if (storageDir.exists() && storageDir.listFiles()?.isNotEmpty() == true) {
                    val localPages = storageDir.listFiles()!!.sortedBy { 
                        it.nameWithoutExtension.removePrefix("page_").toIntOrNull() ?: 0 
                    }.mapIndexed { index, file ->
                        ReaderPage(
                            index = index,
                            url = "file://${file.absolutePath}",
                            state = PageState.READY
                        )
                    }
                    
                    if (localPages.isNotEmpty()) {
                        _pages.value = localPages
                        return@launch // Skip network
                    }
                }

                // If not downloaded, fetch from network
                if (sourceId == 1L) {
                    val pageUrls = AppContainer.mangaDexSource.fetchPageList(chapter.sourceUrl)
                    _pages.value = pageUrls.mapIndexed { index, url ->
                        ReaderPage(index = index, url = url, state = PageState.QUEUE)
                    }
                } else {
                    val source = AppContainer.extensionManager.activeSources.find { it.id == sourceId }
                    if (source != null) {
                        val sChapter = SChapter.create().apply { url = chapter.sourceUrl }
                        val pageList = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { source.getPageList(sChapter) }
                        val headersMap = (source as? eu.kanade.tachiyomi.source.online.HttpSource)?.headersBuilder()?.build()?.toMultimap()?.mapValues { it.value.firstOrNull() ?: "" }
                        
                        _pages.value = pageList.mapIndexedNotNull { index, page ->
                            val finalUrl = page.imageUrl ?: page.url
                            if (finalUrl.isNotEmpty()) {
                                ReaderPage(index = index, url = finalUrl, headers = headersMap, state = PageState.QUEUE, sourcePage = page)
                            } else null
                        }
                    }
                }
                
                // Immediately kick off the state machine for the first few pages
                startPagePreloader()
                
                // Immediately save history so that if the user exits without scrolling, the chapter is recorded
                saveHistory(_initialPage.value)

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private var chapterLoadJob: kotlinx.coroutines.Job? = null
    private var pageLoaderJob: kotlinx.coroutines.Job? = null
    
    private val defaultClient by lazy { eu.kanade.tachiyomi.network.NetworkHelper.client }

    private fun startPagePreloader() {
        pageLoaderJob?.cancel()
        pageLoaderJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val source = AppContainer.extensionManager.activeSources.find { it.id == sourceId } as? eu.kanade.tachiyomi.source.online.HttpSource
            val client = source?.client ?: defaultClient
            
            val chapter = chapters[currentChapterIndex]
            val manga = activeManga ?: return@launch
            
            val cacheDir = File(AppContainer.appContext.cacheDir, "reader_cache/${manga.sourceUrl.hashCode()}_${chapter.sourceUrl.hashCode()}")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            // Iterate and download
            while (true) {
                val currentList = _pages.value
                // Find the first page that is queued or error
                val targetPage = currentList.firstOrNull { it.state == PageState.QUEUE } ?: currentList.firstOrNull { it.state == PageState.ERROR } ?: break
                
                // Update state to LOADING
                updatePageState(targetPage.index, PageState.DOWNLOAD_IMAGE)

                try {
                    val finalFile = File(cacheDir, "page_${targetPage.index}.jpg")
                    
                    if (finalFile.exists() && finalFile.length() > 0) {
                        _pages.update { list ->
                            list.map { if (it.index == targetPage.index) it.copy(state = PageState.READY, url = "file://${finalFile.absolutePath}") else it }
                        }
                        continue
                    }

                    if (targetPage.sourcePage != null && targetPage.sourcePage.imageUrl == null) {
                        try {
                            var parsed = source?.getImageUrl(targetPage.sourcePage)
                            if (parsed != null && !parsed.startsWith("http")) {
                                parsed = (source?.baseUrl ?: "") + if (parsed.startsWith("/")) parsed else "/$parsed"
                            }
                            targetPage.sourcePage.imageUrl = parsed
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    if (targetPage.sourcePage != null && targetPage.sourcePage.imageUrl != null) {
                        var imgUrl = targetPage.sourcePage.imageUrl!!
                        if (!imgUrl.startsWith("http")) {
                            imgUrl = (source?.baseUrl ?: "") + if (imgUrl.startsWith("/")) imgUrl else "/$imgUrl"
                            targetPage.sourcePage.imageUrl = imgUrl
                        }
                    }

                    val finalImageUrlToDownload = targetPage.sourcePage?.imageUrl ?: targetPage.url

                    android.util.Log.d("WammyReader", "Attempting to download page ${targetPage.index} with URL: ${targetPage.sourcePage?.imageUrl ?: finalImageUrlToDownload}")
                    val response = if (source != null && targetPage.sourcePage != null && targetPage.sourcePage.imageUrl != null) {
                        try {
                            source.getImage(targetPage.sourcePage)
                        } catch (e: Exception) {
                            val reqUrl = if (finalImageUrlToDownload.startsWith("http")) finalImageUrlToDownload else (source.baseUrl) + if (finalImageUrlToDownload.startsWith("/")) finalImageUrlToDownload else "/$finalImageUrlToDownload"
                            val builder = okhttp3.Request.Builder().url(reqUrl)
                            targetPage.headers?.forEach { (k, v) -> builder.addHeader(k, v) }
                            client.newCall(builder.build()).execute()
                        }
                    } else {
                        val reqUrl = if (finalImageUrlToDownload.startsWith("http")) finalImageUrlToDownload else (source?.baseUrl ?: "") + if (finalImageUrlToDownload.startsWith("/")) finalImageUrlToDownload else "/${finalImageUrlToDownload}"
                        val builder = okhttp3.Request.Builder().url(reqUrl)
                        targetPage.headers?.forEach { (k, v) -> builder.addHeader(k, v) }
                        client.newCall(builder.build()).execute()
                    }
                    if (response.isSuccessful) {
                        val tmpFile = File(cacheDir, "page_${targetPage.index}.tmp")
                        response.body?.byteStream()?.use { input ->
                            tmpFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        tmpFile.renameTo(finalFile)
                        _pages.update { list ->
                            list.map { if (it.index == targetPage.index) it.copy(state = PageState.READY, url = "file://${finalFile.absolutePath}") else it }
                        }
                    } else {
                        android.util.Log.e("WammyReader", "Network response was not successful: ${response.code} for URL: ${response.request.url}")
                        response.close()
                        if (targetPage.sourcePage != null) targetPage.sourcePage.imageUrl = null
                        updatePageState(targetPage.index, PageState.ERROR)
                        kotlinx.coroutines.delay(2000) // wait before retry
                    }
                } catch (e: Exception) {
                    android.util.Log.e("WammyReader", "Exception in pageLoaderJob for index ${targetPage.index}", e)
                    if (targetPage.sourcePage != null) targetPage.sourcePage.imageUrl = null
                    updatePageState(targetPage.index, PageState.ERROR)
                    kotlinx.coroutines.delay(2000) // wait before retry
                }
            }
        }
    }

    fun updateProgress(pageIndex: Int) {
        saveHistory(pageIndex)
    }

    private fun saveHistory(pageIndex: Int = 0) {
        val manga = activeManga ?: return
        if (currentChapterIndex !in chapters.indices) return
        val chapter = chapters[currentChapterIndex]
        val totalPages = _pages.value.size
        if (totalPages == 0) return
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            historyMutex.withLock {
                val existingHistory = AppContainer.database.historyDao().getHistoryByManga(manga.sourceUrl)
                
                val isChapterCompleted = pageIndex >= totalPages - 1
                
                val updatedChapter = chapter.copy(
                    read = chapter.read || isChapterCompleted,
                    lastPageRead = pageIndex
                )
                AppContainer.database.chapterDao().updateChapter(updatedChapter)
                chapters = chapters.toMutableList().apply { set(currentChapterIndex, updatedChapter) }

                // If this is the chronologically last chapter (index 0) and completed
                val isCompleted = (currentChapterIndex == 0) && isChapterCompleted
                if (isChapterCompleted) {
                    val data = androidx.work.Data.Builder().putLong("mangaId", manga.id).putFloat("chapterNumber", chapter.chapterNumber).putBoolean("isCompleted", isCompleted).build()
                    val req = androidx.work.OneTimeWorkRequestBuilder<com.example.wammy.track.TrackSyncWorker>().setInputData(data).build()
                    androidx.work.WorkManager.getInstance(AppContainer.appContext).enqueue(req)
                }
                val existing = AppContainer.database.mangaDao().getMangaByUrl(manga.sourceUrl)
                if (existing != null && isCompleted && !existing.readCompleted) {
                    AppContainer.database.mangaDao().insertManga(existing.copy(readCompleted = true))
                }

                val history = com.example.wammy.data.local.HistoryEntity(
                    id = existingHistory?.id ?: 0L,
                    mangaSourceUrl = manga.sourceUrl,
                    mangaTitle = manga.titleRomaji,
                    mangaCoverUrl = manga.coverImageUrl ?: "",
                    chapterName = chapter.name,
                    chapterSourceUrl = chapter.sourceUrl,
                    lastPageRead = pageIndex,
                    totalPages = totalPages,
                    lastReadTimestamp = System.currentTimeMillis()
                )
                AppContainer.database.historyDao().insertHistory(history)
            }
        }
    }

    fun loadNextChapter() {
        if (hasNextChapter) {
            currentChapterIndex-- // Descending order
            loadCurrentChapter()
        }
    }
    
    fun loadPrevChapter() {
        if (hasPrevChapter) {
            currentChapterIndex++ // Descending order
            loadCurrentChapter()
        }
    }
    
    fun setColorFilter(mode: ColorFilterMode) {
        _colorFilter.value = mode
    }

    fun setWebtoonSidePadding(padding: Int) {
        _webtoonSidePadding.value = padding
    }

    fun setReadingMode(mode: ReadingMode) {
        _readingMode.value = mode
    }
    
    fun updatePageState(index: Int, newState: PageState) {
        _pages.update { currentPages ->
            currentPages.map { if (it.index == index) it.copy(state = newState) else it }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up the temporary reader cache to prevent 120MB+ storage bloat
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val cacheDir = java.io.File(AppContainer.appContext.cacheDir, "reader_cache")
                if (cacheDir.exists()) {
                    cacheDir.deleteRecursively()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}