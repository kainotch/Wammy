package eu.kanade.tachiyomi.ui.browse.source.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.State
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.domain.track.interactor.AddTracks
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.isNovelSource
import eu.kanade.tachiyomi.source.model.FilterList
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.logcat
import mihon.core.viewmodel.StateViewModel
import mihon.domain.manga.model.toDomainManga
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.core.common.preference.mapAsCheckboxState
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.SetMangaCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.chapter.interactor.SetMangaDefaultChapterFlags
import tachiyomi.domain.manga.interactor.GetDuplicateLibraryManga
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaWithChapterCount
import tachiyomi.domain.manga.model.toMangaUpdate
import tachiyomi.domain.source.interactor.GetRemoteManga
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SourceFeedViewModel(
    val sourceId: Long,
    sourceManager: SourceManager = Injekt.get(),
    private val getManga: GetManga = Injekt.get(),
    private val networkToLocalManga: NetworkToLocalManga = Injekt.get(),
    private val getDuplicateLibraryManga: GetDuplicateLibraryManga = Injekt.get(),
    private val getCategories: GetCategories = Injekt.get(),
    private val setMangaCategories: SetMangaCategories = Injekt.get(),
    private val setMangaDefaultChapterFlags: SetMangaDefaultChapterFlags = Injekt.get(),
    private val updateManga: UpdateManga = Injekt.get(),
    private val addTracks: AddTracks = Injekt.get(),
) : StateViewModel<SourceFeedViewModel.State>(State()) {

    companion object {
        val SOURCE_ID_KEY = CreationExtras.Key<Long>()

        val Factory = viewModelFactory {
            initializer {
                SourceFeedViewModel(
                    sourceId = get(SOURCE_ID_KEY)!!,
                )
            }
        }
    }

    val source = sourceManager.getOrStub(sourceId)

    init {
        loadFeed()
    }

    private fun loadFeed() {
        viewModelScope.launchIO {
            val catalogueSource = source as? CatalogueSource ?: return@launchIO

            // Fetch Latest and Browse concurrently
            val latestDeferred = if (catalogueSource.supportsLatest) {
                async {
                    try {
                        val page = catalogueSource.getLatestUpdates(1)
                        page.mangas.map { it.toDomainManga(sourceId, catalogueSource.isNovelSource()) }
                            .distinctBy { it.url }
                            .let { networkToLocalManga(it) }
                    } catch (e: Exception) {
                        logcat(LogPriority.ERROR) { "Failed to fetch latest from ${source.name}: ${e.message}" }
                        emptyList()
                    }
                }
            } else {
                null
            }

            val browseDeferred = async {
                try {
                    val page = catalogueSource.getPopularManga(1)
                    page.mangas.map { it.toDomainManga(sourceId, catalogueSource.isNovelSource()) }
                        .distinctBy { it.url }
                        .let { networkToLocalManga(it) }
                } catch (e: Exception) {
                    logcat(LogPriority.ERROR) { "Failed to fetch popular from ${source.name}: ${e.message}" }
                    emptyList()
                }
            }

            // Update state as each completes
            latestDeferred?.let { deferred ->
                launch {
                    val latest = deferred.await()
                    mutableState.update { it.copy(latestItems = latest) }
                }
            }

            launch {
                val browse = browseDeferred.await()
                mutableState.update { it.copy(browseItems = browse) }
            }
        }
    }

    fun refresh() {
        mutableState.update { it.copy(latestItems = null, browseItems = null) }
        loadFeed()
    }

    @Composable
    fun getManga(initialManga: Manga): androidx.compose.runtime.State<Manga> {
        return produceState(initialValue = initialManga) {
            getManga.subscribe(initialManga.url, initialManga.source)
                .collectLatest { manga ->
                    if (manga == null) return@collectLatest
                    value = manga
                }
        }
    }

    fun addFavorite(manga: Manga) {
        viewModelScope.launchIO {
            changeMangaFavorite(manga)
        }
    }

    private suspend fun changeMangaFavorite(manga: Manga) {
        val newManga = manga.copy(favorite = !manga.favorite, dateAdded = System.currentTimeMillis())
        updateManga.await(newManga.toMangaUpdate())
        if (newManga.favorite) {
            setMangaDefaultChapterFlags.await(newManga)
            addTracks.bindEnhancedTrackers(newManga, source)
        }
    }

    private suspend fun moveMangaToCategories(manga: Manga, categories: List<Category>) {
        setMangaCategories.await(manga.id, categories.map { it.id })
    }

    suspend fun getDuplicateLibraryManga(manga: Manga): List<MangaWithChapterCount> {
        return getDuplicateLibraryManga.invoke(manga)
    }

    fun getFilters(): FilterList {
        return (source as? CatalogueSource)?.getFilterList() ?: FilterList()
    }

    sealed interface Dialog {
        data class AddDuplicateManga(val manga: Manga, val duplicates: List<MangaWithChapterCount>) : Dialog
        data class RemoveManga(val manga: Manga) : Dialog
        data class ChangeMangaCategory(
            val manga: Manga,
            val initialSelection: List<tachiyomi.core.common.preference.CheckboxState.State<Category>>,
        ) : Dialog
    }

    fun toggleSelectionMode() {
        mutableState.update { it.copy(selectionMode = !it.selectionMode, selection = emptySet()) }
    }

    fun toggleSelection(manga: Manga) {
        mutableState.update { state ->
            val newSelection = state.selection.toMutableSet().apply {
                if (!add(manga)) remove(manga)
            }
            state.copy(
                selection = newSelection,
                selectionMode = newSelection.isNotEmpty() || state.selectionMode,
            )
        }
    }

    fun selectAll() {
        mutableState.update { state ->
            val allItems = (state.latestItems.orEmpty() + state.browseItems.orEmpty()).distinctBy { it.id }
            state.copy(selection = allItems.toSet())
        }
    }

    fun invertSelection() {
        mutableState.update { state ->
            val allItems = (state.latestItems.orEmpty() + state.browseItems.orEmpty()).distinctBy { it.id }
            state.copy(selection = allItems.filterNot { it in state.selection }.toSet())
        }
    }

    fun clearSelection() {
        mutableState.update { it.copy(selection = emptySet(), selectionMode = false) }
    }

    fun setDialog(dialog: Dialog?) {
        mutableState.update { it.copy(dialog = dialog) }
    }

    @Immutable
    data class State(
        val latestItems: List<Manga>? = null,
        val browseItems: List<Manga>? = null,
        val dialog: Dialog? = null,
        val selectionMode: Boolean = false,
        val selection: Set<Manga> = emptySet(),
    ) {
        val isLoading get() = latestItems == null && browseItems == null
    }
}
