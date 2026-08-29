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
    private var currentPageIndex: Int = 0
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
        _readingMode.value = ReadingMode.values()[manga.readingMode.coerceIn(0, ReadingMode.values().size - 1)]
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
            
            val cacheDir = java.io.File(AppContainer.appContext.cacheDir, "reader_cache/${manga.sourceUrl.hashCode()}_${chapter.sourceUrl.hashCode()}")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val claimMutex = kotlinx.coroutines.sync.Mutex()

            // Launch a worker pool of 3 concurrent downloaders to mimic Mihon/Tachiyomi
            var lastMdRefreshTime = 0L
            val workers = List(2) {
                launch {
                    while (true) {
                        var targetPage: com.example.wammy.ui.ReaderPage? = null
                        
                        claimMutex.withLock {
                            val currentList = _pages.value
                            // Mihon behavior: Prioritize pages closest to the user's current view (currentPageIndex)
                            // We sort QUEUE pages by their absolute distance to currentPageIndex, then fall back to ERROR pages.
                            targetPage = currentList
                                .filter { it.state == PageState.QUEUE || it.state == PageState.ERROR }
                                .minByOrNull { kotlin.math.abs(it.index - currentPageIndex) }
                            if (targetPage != null) {
                                _pages.update { list ->
                                    list.map { if (it.index == targetPage!!.index) it.copy(state = PageState.DOWNLOAD_IMAGE) else it }
                                }
                            }
                        }

                        if (targetPage == null) {
                            kotlinx.coroutines.delay(500)
                            // check if all pages are READY
                            if (_pages.value.all { it.state == PageState.READY }) break
                            continue
                        }

                        try {
                            val finalFile = java.io.File(cacheDir, "page_${targetPage!!.index}.jpg")
                            
                            if (finalFile.exists() && finalFile.length() > 0) {
                                _pages.update { list ->
                                    list.map { if (it.index == targetPage!!.index) it.copy(state = PageState.READY, url = "file://${finalFile.absolutePath}") else it }
                                }
                                continue
                            }

                            if (targetPage!!.sourcePage != null && targetPage!!.sourcePage!!.imageUrl == null) {
                                var parsed = source?.getImageUrl(targetPage!!.sourcePage!!)
                                if (parsed != null && !parsed.startsWith("http")) {
                                    parsed = (source?.baseUrl ?: "") + if (parsed.startsWith("/")) parsed else "/$parsed"
                                }
                                targetPage!!.sourcePage!!.imageUrl = parsed
                            }

                            var response: okhttp3.Response? = null
                            try {
                                if (source != null && targetPage!!.sourcePage != null) {
                                    // 1. Mihon strictly relies on source.getImage() and relies on its interceptors to handle URLs and errors.
                                    android.util.Log.d("WammyReader", "Attempting to download page ${targetPage!!.index} via extension source.getImage")
                                    response = source.getImage(targetPage!!.sourcePage!!)
                                } else {
                                    // 2. Built-in source fallback
                                    val finalImageUrlToDownload = targetPage!!.url
                                    val reqUrl = if (finalImageUrlToDownload.startsWith("http")) finalImageUrlToDownload else "https://mangadex.org" + if (finalImageUrlToDownload.startsWith("/")) finalImageUrlToDownload else "/$finalImageUrlToDownload"
                                    android.util.Log.d("WammyReader", "Attempting to download page ${targetPage!!.index} via built-in request: $reqUrl")
                                    val builder = okhttp3.Request.Builder().url(reqUrl)
                                    targetPage!!.headers?.forEach { (k, v) -> builder.addHeader(k, v) }
                                    response = client.newCall(builder.build()).execute()
                                }
                                
                                if (response != null && response.isSuccessful) {
                                    val tmpFile = java.io.File(cacheDir, "page_${targetPage!!.index}.tmp")
                                    response.body?.byteStream()?.use { input ->
                                        tmpFile.outputStream().use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                    tmpFile.renameTo(finalFile)
                                    _pages.update { list ->
                                        list.map { if (it.index == targetPage!!.index) it.copy(state = PageState.READY, url = "file://${finalFile.absolutePath}") else it }
                                    }
                                } else {
                                    // If response is not successful, we handle it as a network error
                                    throw Exception("HTTP error ${response?.code}")
                                }
                            } finally {
                                response?.close()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("WammyReader", "Exception in pageLoaderJob for index ${targetPage!!.index}", e)
                            
                            val isExpiredNodeError = e.message?.contains("HTTP error 404") == true || e.message?.contains("HTTP error 403") == true
                            if (isExpiredNodeError) {
                                // Node Expired! Refresh URLs safely for both built-in and extensions!
                                claimMutex.withLock {
                                    val now = System.currentTimeMillis()
                                    if (now - lastMdRefreshTime > 10000) { // Only refresh once per 10 seconds
                                        lastMdRefreshTime = now
                                        try {
                                            android.util.Log.d("WammyReader", "Refreshing Chapter Page URLs due to 404...")
                                            if (sourceId == -1L) {
                                                val newUrls = AppContainer.mangaDexSource.fetchPageList(chapter.sourceUrl)
                                                _pages.update { list ->
                                                    list.map { page ->
                                                        if (page.index < newUrls.size && !page.url.startsWith("file://")) {
                                                            page.copy(url = newUrls[page.index], state = if (page.state == PageState.ERROR) PageState.QUEUE else page.state)
                                                        } else page
                                                    }
                                                }
                                            } else if (source != null) {
                                                val sChapter = eu.kanade.tachiyomi.source.model.SChapter.create().apply { url = chapter.sourceUrl }
                                                val newSourcePages = source.getPageList(sChapter)
                                                _pages.update { list ->
                                                    list.map { page ->
                                                        if (page.index < newSourcePages.size && !page.url.startsWith("file://")) {
                                                            val newSourcePage = newSourcePages[page.index]
                                                            page.copy(url = newSourcePage.url, sourcePage = newSourcePage, state = if (page.state == PageState.ERROR) PageState.QUEUE else page.state)
                                                        } else page
                                                    }
                                                }
                                            }
                                        } catch (e2: Exception) {
                                            android.util.Log.e("WammyReader", "Failed to refresh Chapter URLs", e2)
                                        }
                                    }
                                }
                            } else {
                                // Mihon behavior: clear the cached imageUrl so it forces a re-fetch on the next retry
                                if (targetPage!!.sourcePage != null) {
                                    targetPage!!.sourcePage!!.imageUrl = null
                                }
                            }
                            
                            updatePageState(targetPage!!.index, PageState.ERROR)
                            kotlinx.coroutines.delay(2000) // wait before retry
                        }
                    }
                }
            }
            
            workers.forEach { it.join() }
        }
    }

    fun updateProgress(pageIndex: Int) {
        currentPageIndex = pageIndex
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
        val manga = activeManga ?: return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val existing = AppContainer.database.mangaDao().getMangaByUrl(manga.sourceUrl)
            if (existing != null) {
                AppContainer.database.mangaDao().insertManga(existing.copy(readingMode = mode.ordinal))
                activeManga = existing.copy(readingMode = mode.ordinal)
            } else {
                val newManga = manga.copy(readingMode = mode.ordinal, id = 0)
                AppContainer.database.mangaDao().insertManga(newManga)
                activeManga = newManga
            }
        }
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