// Created by Notch
package com.example.wammy.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wammy.AppContainer
import com.example.wammy.data.local.MangaEntity
import com.example.wammy.extension.awaitFirst

import com.example.wammy.source.MangaDexSource
import eu.kanade.tachiyomi.source.Source

import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.FilterList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class BrowseSourceViewModel : ViewModel() {
    private val _mangaList = MutableStateFlow<List<MangaEntity>>(emptyList())
    val mangaList: StateFlow<List<MangaEntity>> = _mangaList.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _sourceName = MutableStateFlow("")
    val sourceName: StateFlow<String> = _sourceName.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var currentTachiyomiSource: Source? = null
    private var isMangaDex = false
    private var currentSourceId: Long = -1L

    fun initSource(sourceId: Long) {
        // Always clear old data and reset state first
        _mangaList.value = emptyList()
        _isLoading.value = true
        _errorMessage.value = null
        currentTachiyomiSource = null

        currentSourceId = sourceId

        if (sourceId == AppContainer.mangaDexSource.id) {
            isMangaDex = true
            _sourceName.value = AppContainer.mangaDexSource.name
            fetchPopularManga()
        } else {
            isMangaDex = false
            val source = AppContainer.extensionManager.activeSources.find { it.id == sourceId }
            if (source != null) {
                currentTachiyomiSource = source
                _sourceName.value = source.name
                android.util.Log.d("BrowseSource", "initSource: Found source ${source.name} (id=$sourceId, class=${source.javaClass.name})")
                fetchPopularManga()
            } else {
                android.util.Log.e("BrowseSource", "initSource: No active source found for id=$sourceId. Active sources: ${AppContainer.extensionManager.activeSources.map { "${it.name}(${it.id})" }}")
                _errorMessage.value = "Source not found. Try reinstalling the extension."
                _isLoading.value = false
            }
        }
    }

    fun retry() {
        _errorMessage.value = null
        fetchPopularManga()
    }

    private fun fetchPopularManga() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                if (isMangaDex) {
                    _mangaList.value = AppContainer.mangaDexSource.fetchLatest(1)
                } else {
                    val source = currentTachiyomiSource
                    if (source == null) {
                        android.util.Log.e("BrowseSource", "fetchPopularManga: currentTachiyomiSource is null!")
                        _errorMessage.value = "Source not loaded"
                        return@launch
                    }
                    android.util.Log.d("BrowseSource", "fetchPopularManga: Calling fetchPopularManga(1) for ${source.name} (${source.javaClass.name})...")
                    
                    val page = try {
                        withTimeoutOrNull(30000) {
                            withContext(Dispatchers.IO) { source.getPopularManga(1) }
                        }
                    } catch (e: Throwable) {
                        android.util.Log.e("BrowseSource", "fetchPopularManga: Exception during fetch for ${source.name}: ${e.javaClass.simpleName}: ${e.message}", e)
                        _errorMessage.value = "${e.javaClass.simpleName}: ${e.message}"
                        _mangaList.value = emptyList()
                        return@launch
                    }
                    
                    if (page == null) {
                        android.util.Log.e("BrowseSource", "fetchPopularManga: Extension ${source.name} timed out after 30s.")
                        _errorMessage.value = "Timed out loading from ${source.name}. Tap to retry."
                        _mangaList.value = emptyList()
                        return@launch
                    }
                    android.util.Log.d("BrowseSource", "fetchPopularManga: Got ${page.mangas.size} manga from ${source.name}")
                    
                    if (page.mangas.isEmpty()) {
                        _errorMessage.value = "Source returned 0 results"
                    }
                    
                    val entities = page.mangas.map { sManga ->
                        MangaEntity(
                            aniListId = null,
                            titleRomaji = sManga.title,
                            coverImageUrl = sManga.thumbnail_url?.let { if (it.startsWith("/") && source is eu.kanade.tachiyomi.source.online.HttpSource) source.baseUrl + it else it } ?: "",
                            description = sManga.description ?: "Imported from ${source.name}",
                            sourceId = source.id,
                            sourceUrl = sManga.url,
                            author = sManga.author,
                            artist = sManga.artist,
                            status = when (sManga.status) {
                                SManga.ONGOING -> "Ongoing"
                                SManga.COMPLETED -> "Completed"
                                SManga.LICENSED -> "Licensed"
                                SManga.PUBLISHING_FINISHED -> "Finished"
                                SManga.CANCELLED -> "Cancelled"
                                SManga.ON_HIATUS -> "On Hiatus"
                                else -> "Unknown"
                            },
                            sourceName = source.name,
                            genre = sManga.genre
                        )
                    }
                    _mangaList.value = entities
                }
            } catch (e: Throwable) {
                android.util.Log.e("BrowseSource", "fetchPopularManga: Outer exception: ${e.javaClass.simpleName}: ${e.message}", e)
                _errorMessage.value = "${e.javaClass.simpleName}: ${e.message}"
                _mangaList.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchManga(query: String) {
        if (query.isEmpty()) {
            fetchPopularManga()
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                if (isMangaDex) {
                    _mangaList.value = AppContainer.mangaDexSource.fetchSearchManga(query, 1)
                } else {
                    val source = currentTachiyomiSource ?: return@launch
                    val page = try {
                        withTimeoutOrNull(30000) {
                            withContext(Dispatchers.IO) { source.getSearchManga(1, query, FilterList()) }
                        }
                    } catch (e: Throwable) {
                        _errorMessage.value = "Search error: ${e.message}"
                        _mangaList.value = emptyList()
                        return@launch
                    }
                    if (page == null) {
                        _errorMessage.value = "Search timed out"
                        _mangaList.value = emptyList()
                        return@launch
                    }
                    
                    val entities = page.mangas.map { sManga ->
                        MangaEntity(
                            aniListId = null,
                            titleRomaji = sManga.title,
                            coverImageUrl = sManga.thumbnail_url?.let { if (it.startsWith("/") && source is eu.kanade.tachiyomi.source.online.HttpSource) source.baseUrl + it else it } ?: "",
                            description = sManga.description ?: "Imported from ${source.name}",
                            sourceId = source.id,
                            sourceUrl = sManga.url,
                            author = sManga.author,
                            artist = sManga.artist,
                            status = when (sManga.status) {
                                SManga.ONGOING -> "Ongoing"
                                SManga.COMPLETED -> "Completed"
                                SManga.LICENSED -> "Licensed"
                                SManga.PUBLISHING_FINISHED -> "Finished"
                                SManga.CANCELLED -> "Cancelled"
                                SManga.ON_HIATUS -> "On Hiatus"
                                else -> "Unknown"
                            },
                            sourceName = source.name,
                            genre = sManga.genre
                        )
                    }
                    _mangaList.value = entities
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                _errorMessage.value = "${e.javaClass.simpleName}: ${e.message}"
                _mangaList.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
