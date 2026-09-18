package eu.kanade.tachiyomi.ui.discover

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.viewModelScope
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.jsplugin.JsPluginManager
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.isNovelSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.LogPriority
import mihon.core.viewmodel.StateViewModel
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

data class DiscoverState(
    val isLoading: Boolean = true,
    val isNovel: Boolean = false,
    val sources: List<CatalogueSource> = emptyList(),
)

class DiscoverViewModel(
    val authManager: eu.kanade.tachiyomi.data.auth.AuthManager = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val jsPluginManager: JsPluginManager = Injekt.get(),
    private val preferences: BasePreferences = Injekt.get(),
    private val networkToLocalManga: NetworkToLocalManga = Injekt.get(),
    private val updateManga: eu.kanade.domain.manga.interactor.UpdateManga = Injekt.get()
) : StateViewModel<DiscoverState>(DiscoverState()) {

    val popularCache = mutableStateMapOf<Long, List<Manga>>()
    val latestCache = mutableStateMapOf<Long, List<Manga>>()

    init {
        viewModelScope.launch {
            combine(
                sourceManager.sources,
                jsPluginManager.jsSources,
                preferences.homeTabIsNovel.changes()
            ) { apkSources, jsSources, isNovel ->
                val allSources = (apkSources + jsSources).filterIsInstance<CatalogueSource>().distinctBy { it.id }
                val filtered = allSources.filter { 
                    it.isNovelSource() == isNovel && 
                    it.id != tachiyomi.source.local.LocalSource.ID && 
                    it.id != tachiyomi.source.local.LocalNovelSource.ID 
                }
                filtered to isNovel
            }.collectLatest { (sources, isNovel) ->
                mutableState.update { 
                    it.copy(isLoading = false, sources = sources, isNovel = isNovel)
                }
                // Pre-fetch the first 3 sources to speed up the initial banner load
                viewModelScope.launch {
                    sources.take(3).forEach { source ->
                        if (!popularCache.containsKey(source.id)) {
                            launch {
                                popularCache[source.id] = loadSourcePopular(source)
                            }
                        }
                    }
                }
            }
        }
    }

    fun toggleNovel(isNovel: Boolean) {
        preferences.homeTabIsNovel.set(isNovel)
    }

    suspend fun loadSourcePopular(source: CatalogueSource): List<Manga> {
        return withContext(Dispatchers.IO) {
            try {
                val page = source.getPopularManga(1)
                page.mangas.map { sManga ->
                    Manga.create().copy(
                        source = source.id,
                        url = sManga.url,
                        title = sManga.title,
                        thumbnailUrl = sManga.thumbnail_url,
                        initialized = sManga.initialized,
                        isNovel = source.isNovelSource()
                    )
                }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Failed to load popular manga for source ${source.name}" }
                emptyList()
            }
        }
    }

    suspend fun loadSourceLatest(source: CatalogueSource): List<Manga> {
        return withContext(Dispatchers.IO) {
            try {
                val page = source.getLatestUpdates(1)
                page.mangas.map { sManga ->
                    Manga.create().copy(
                        source = source.id,
                        url = sManga.url,
                        title = sManga.title,
                        thumbnailUrl = sManga.thumbnail_url,
                        initialized = sManga.initialized,
                        isNovel = source.isNovelSource()
                    )
                }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Failed to load latest manga for source ${source.name}" }
                emptyList()
            }
        }
    }

    suspend fun getNetworkToLocalManga(manga: Manga): Manga {
        return withContext(Dispatchers.IO) {
            networkToLocalManga(manga)
        }
    }

    fun toggleFavorite(manga: Manga, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val localManga = networkToLocalManga(manga)
            val newFavorite = !localManga.favorite
            updateManga.awaitUpdateFavorite(localManga.id, newFavorite)
            withContext(Dispatchers.Main) {
                onResult(newFavorite)
            }
        }
    }
}
