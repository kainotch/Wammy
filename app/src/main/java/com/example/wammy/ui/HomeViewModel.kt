// Created by Notch
package com.example.wammy.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wammy.AppContainer
import com.example.wammy.data.local.MangaEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.FilterList
import com.example.wammy.extension.awaitFirst

data class BrowseSourceItem(
    val id: Long,
    val name: String,
    val iconUrl: String?,
    val lang: String = ""
)

class HomeViewModel : ViewModel() {

    private val _latestManga = MutableStateFlow<List<MangaEntity>>(emptyList())
    val latestManga: StateFlow<List<MangaEntity>> = _latestManga.asStateFlow()

    private val _bigThreeManga = MutableStateFlow<List<MangaEntity>>(emptyList())
    val bigThreeManga: StateFlow<List<MangaEntity>> = _bigThreeManga.asStateFlow()

    private val _seinenManga = MutableStateFlow<List<MangaEntity>>(emptyList())
    val seinenManga: StateFlow<List<MangaEntity>> = _seinenManga.asStateFlow()

    private val _searchResults = MutableStateFlow<List<MangaEntity>>(emptyList())
    val searchResults: StateFlow<List<MangaEntity>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    

    private val _searchManga = MutableStateFlow(true)
    val searchManga: StateFlow<Boolean> = _searchManga.asStateFlow()
    
    private val _searchNovel = MutableStateFlow(false)
    val searchNovel: StateFlow<Boolean> = _searchNovel.asStateFlow()

    fun toggleSearchType(manga: Boolean, novel: Boolean) {
        if (!manga && !novel) return
        _searchManga.value = manga
        _searchNovel.value = novel
        if (_searchQuery.value.length > 2) {
            performSearch(_searchQuery.value)
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val prefs = com.example.wammy.AppContainer.appContext.getSharedPreferences("pinned_sources", android.content.Context.MODE_PRIVATE)

    private val _pinnedMangaSources = kotlinx.coroutines.flow.MutableStateFlow(prefs.getStringSet("manga", emptySet()) ?: emptySet())
    val pinnedMangaSources: kotlinx.coroutines.flow.StateFlow<Set<String>> = _pinnedMangaSources.asStateFlow()

    private val _pinnedNovelSources = kotlinx.coroutines.flow.MutableStateFlow(prefs.getStringSet("novel", emptySet()) ?: emptySet())
    val pinnedNovelSources: kotlinx.coroutines.flow.StateFlow<Set<String>> = _pinnedNovelSources.asStateFlow()

    private val _libraryNovelMode = kotlinx.coroutines.flow.MutableStateFlow(prefs.getBoolean("library_novel_mode", false))
    val libraryNovelMode: kotlinx.coroutines.flow.StateFlow<Boolean> = _libraryNovelMode.asStateFlow()

    fun toggleLibraryMode() {
        _libraryNovelMode.value = !_libraryNovelMode.value
        prefs.edit().putBoolean("library_novel_mode", _libraryNovelMode.value).apply()
    }

    fun toggleMangaPin(sourceId: Long) {
        val current = _pinnedMangaSources.value.toMutableSet()
        val idStr = sourceId.toString()
        if (current.contains(idStr)) current.remove(idStr) else current.add(idStr)
        prefs.edit().putStringSet("manga", current).apply()
        _pinnedMangaSources.value = current
    }

    fun toggleNovelPin(pkg: String) {
        val current = _pinnedNovelSources.value.toMutableSet()
        if (current.contains(pkg)) current.remove(pkg) else current.add(pkg)
        prefs.edit().putStringSet("novel", current).apply()
        _pinnedNovelSources.value = current
    }

    private val _availableSources = kotlinx.coroutines.flow.MutableStateFlow<List<BrowseSourceItem>>(emptyList())
    val availableSources: kotlinx.coroutines.flow.StateFlow<List<BrowseSourceItem>> = _availableSources.asStateFlow()

    fun refreshAvailableSources() {
        val sources = mutableListOf<BrowseSourceItem>()
        // Always add MangaDex as a built-in source
        sources.add(BrowseSourceItem(
            id = AppContainer.mangaDexSource.id,
            name = AppContainer.mangaDexSource.name,
            iconUrl = "https://mangadex.org/favicon.ico",
            lang = "all"
        ))
        // Add all installed extension sources
        AppContainer.extensionManager.activeSources.forEach { source ->
            val pkgName = AppContainer.extensionManager.getPackageNameForSource(source)
            var iconUrl: String? = null
            if (pkgName != null) {
                // Try local extracted icon first
                val iconFile = java.io.File(AppContainer.appContext.cacheDir, "${pkgName}_icon.png")
                if (iconFile.exists()) {
                    iconUrl = "file://" + iconFile.absolutePath
                }
            }
            // Use the CDN icon URL from the extension index if local not available
            if (iconUrl == null && pkgName != null) {
                val ext = AppContainer.extensionManager.getExtensionInfo(pkgName)
                if (ext != null) {
                    iconUrl = ext.iconUrl
                }
            }
            sources.add(BrowseSourceItem(
                id = source.id,
                name = source.name,
                iconUrl = iconUrl,
                lang = source.lang
            ))
        }
        _availableSources.value = sources
    }

    private var searchJob: Job? = null

    init {
        fetchLatest()
        fetchBigThree()
        fetchSeinen()
        refreshAvailableSources()
    }

    fun refreshHome() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            
            // Re-fetch all categories concurrently
            val j1 = fetchLatest()
            val j2 = fetchBigThree()
            val j3 = fetchSeinen()
            
            j1.join()
            j2.join()
            j3.join()
            
            _isRefreshing.value = false
        }
    }

    private fun fetchBigThree(): Job {
        return viewModelScope.launch {
            try {
                val op = async { AppContainer.mangaDexSource.fetchSearchManga("One Piece", 1).firstOrNull() }
                val naruto = async { AppContainer.mangaDexSource.fetchSearchManga("Naruto Official Color", 1).firstOrNull() }
                val bleach = async { AppContainer.mangaDexSource.fetchSearchManga("Bleach", 1).firstOrNull() }
                
                _bigThreeManga.value = listOfNotNull(op.await(), naruto.await(), bleach.await())
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    fun fetchLatest(): Job {
        return viewModelScope.launch {
            _isLoading.value = true
            try {
                // Fetch from MangaDex
                val mdJob = async {
                    try {
                        AppContainer.mangaDexSource.fetchLatest(1)
                    } catch (e: Throwable) { emptyList() }
                }

                // Fetch from all installed Tachiyomi sources concurrently
                val extJobs = AppContainer.extensionManager.activeSources
                    .filter { _pinnedMangaSources.value.contains(it.id.toString()) }
                    .map { source ->
                    async {
                        try {
                            val page = kotlinx.coroutines.withTimeoutOrNull(5000) {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { source.getPopularManga(1) }
                            }
                            if (page == null) {
                                android.util.Log.e("HomeViewModel", "Extension \${source.name} timed out or hung.")
                                return@async emptyList()
                            }
                            page.mangas.map { sManga ->
                                MangaEntity(
                                    aniListId = null,
                                    titleRomaji = sManga.title,
                                    coverImageUrl = sManga.thumbnail_url?.let { if (it.startsWith("/") && source is eu.kanade.tachiyomi.source.online.HttpSource) source.baseUrl + it else it } ?: "",
                                    description = sManga.description ?: "",
                                    sourceId = source.id,
                                    sourceUrl = sManga.url,
                                    author = sManga.author,
                                    artist = sManga.artist,
                                    status = "Unknown",
                                    sourceName = source.name,
                                    genre = sManga.genre
                                )
                            }.take(10)
                        } catch (e: Throwable) {
                            emptyList()
                        }
                    }
                }

                val mdResults = mdJob.await()
                val extResults = extJobs.awaitAll().flatten()

                val combinedList = (mdResults.take(15) + extResults).shuffled()
                _latestManga.value = combinedList
            } catch (e: Throwable) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                e.printStackTrace()
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(AppContainer.appContext, "Search error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.length > 2) {
            performSearch(query)
        } else {
            _searchResults.value = emptyList()
        }
    }
    
    fun toggleSearch() {
        _isSearching.value = !_isSearching.value
        if (!_isSearching.value) {
            _searchQuery.value = ""
            _searchResults.value = emptyList()
        } else {
            refreshAvailableSources()
        }
    }

    fun forceSearch() { performSearch(_searchQuery.value, false) }

    private fun performSearch(query: String, debounce: Boolean = true) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (debounce) kotlinx.coroutines.delay(500) // Debounce search to prevent network spam
            _isLoading.value = true
            try {
                val dexSearch = if (_searchManga.value) async {
                    try {
                        kotlinx.coroutines.withTimeoutOrNull(30000) {
                            AppContainer.mangaDexSource.fetchSearchManga(query, 1)
                        } ?: emptyList()
                    } catch(e: Throwable) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        emptyList()
                    }
                } else async { emptyList<MangaEntity>() }
                
                val extensionSearches = if (_searchManga.value) AppContainer.extensionManager.activeSources.map { source ->
                    async {
                        try {
                            val mangasPage = kotlinx.coroutines.withTimeoutOrNull(30000) {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { source.fetchSearchManga(1, query, FilterList()).awaitFirst() }
                            }
                            if (mangasPage == null) return@async emptyList<MangaEntity>()
                            mangasPage.mangas.map { sManga ->
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
                                        eu.kanade.tachiyomi.source.model.SManga.ONGOING -> "Ongoing"
                                        eu.kanade.tachiyomi.source.model.SManga.COMPLETED -> "Completed"
                                        eu.kanade.tachiyomi.source.model.SManga.LICENSED -> "Licensed"
                                        eu.kanade.tachiyomi.source.model.SManga.PUBLISHING_FINISHED -> "Finished"
                                        eu.kanade.tachiyomi.source.model.SManga.CANCELLED -> "Cancelled"
                                        eu.kanade.tachiyomi.source.model.SManga.ON_HIATUS -> "On Hiatus"
                                        else -> "Unknown"
                                    },
                                    sourceName = source.name,
                                    genre = sManga.genre
                                )
                            }
                        } catch(e: Throwable) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
                            emptyList<MangaEntity>()
                        }
                    }
                } else emptyList()

                val novelSearches = if (_searchNovel.value) {
                    val repoService = com.example.wammy.lnreader.repo.IReaderRepoService(AppContainer.appContext)
                    val installedNovels = repoService.getInstalledExtensions()
                    val loader = com.example.wammy.lnreader.loader.LNReaderPluginLoader(AppContainer.appContext)
                    installedNovels.map { ext ->
                        async {
                            try {
                                val pluginFile = java.io.File(AppContainer.appContext.filesDir, "ir_extensions/${ext.apk}")
                                val plugin = com.example.wammy.lnreader.plugin.PluginRegistry.find(ext.pkg)
                                
                                val novels = kotlinx.coroutines.withTimeoutOrNull(30000) {
                                    if (plugin != null) {
                                        plugin.searchNovels(query, 1)
                                    } else {
                                        loader.searchNovels(pluginFile, query, 1)
                                    }
                                } ?: emptyList()
                                
                                novels.map { ln ->
                                    MangaEntity(
                                        aniListId = null,
                                        titleRomaji = ln.title,
                                        coverImageUrl = ln.coverUrl,
                                        description = "Imported from ${ext.name}",
                                        sourceId = ext.pkg.hashCode().toLong(),
                                        sourceUrl = ln.url,
                                        sourceName = ext.name,
                                        isNovel = true,
                                        novelPkgName = ext.pkg,
                                        novelApkFile = ext.apk
                                    )
                                }
                            } catch(e: Throwable) {
                                if (e is kotlinx.coroutines.CancellationException) throw e
                                android.util.Log.e("HomeViewModel", "Novel plugin ${ext.name} failed: ${e.message}")
                                emptyList<MangaEntity>()
                            }
                        }
                    }
                } else emptyList()
                
                val results = mutableListOf<MangaEntity>()
                results.addAll(dexSearch.await())
                extensionSearches.awaitAll().forEach { results.addAll(it) }
                novelSearches.awaitAll().forEach { results.addAll(it) }
                
                android.util.Log.d("HomeViewModel", "Search finished with ${results.size} results"); _searchResults.value = results
                if (results.isEmpty() && query.isNotEmpty()) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        android.widget.Toast.makeText(AppContainer.appContext, "No results found for '$query'", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Throwable) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                e.printStackTrace()
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(AppContainer.appContext, "Search error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun fetchSeinen(): Job {
        return viewModelScope.launch {
            try {
                val vagabond = async { AppContainer.mangaDexSource.fetchSearchManga("Vagabond", 1).firstOrNull() }
                val vinland = async { AppContainer.mangaDexSource.fetchSearchManga("Vinland Saga", 1).firstOrNull() }
                val climber = async { AppContainer.mangaDexSource.fetchSearchManga("Kokou no Hito", 1).firstOrNull() }
                val jojo = async { AppContainer.mangaDexSource.fetchSearchManga("JoJo's Bizarre Adventure", 1).firstOrNull() }
                
                _seinenManga.value = listOfNotNull(vagabond.await(), vinland.await(), climber.await(), jojo.await())
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
}
