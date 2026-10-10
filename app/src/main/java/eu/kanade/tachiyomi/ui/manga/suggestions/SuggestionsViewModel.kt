package eu.kanade.tachiyomi.ui.manga.suggestions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import tachiyomi.domain.source.service.SourceManager
import eu.kanade.tachiyomi.source.CatalogueSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import logcat.LogPriority
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import mihon.domain.manga.model.toDomainManga
import kotlinx.coroutines.launch

class SuggestionsViewModel(
    private val mangaId: Long
) : ViewModel() {

    private val getManga: GetManga = Injekt.get()
    private val sourceManager: SourceManager = Injekt.get()

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        viewModelScope.launchIO {
            try {
                val manga = getManga.await(mangaId) ?: return@launchIO
                val source = sourceManager.get(manga.source) as? CatalogueSource ?: return@launchIO

                _state.update { it.copy(isFetching = true) }

                val regexWhitespace = Regex("\\s+")
                val regexSpecialCharacters = Regex("([^a-zA-Z0-9 ]|\\s-|-\\s|\\s\\.|\\.\\s)")
                val regexNumberOnly = Regex("^\\d+$")

                fun String.stripKeywordForRelatedMangas(): List<String> {
                    return replace(regexSpecialCharacters, " ")
                        .split(regexWhitespace)
                        .map { it.replace(regexNumberOnly, "").lowercase() }
                        .filter { it.length > 1 }
                }

                val words = HashSet<String>()
                words.add(manga.title)
                manga.title.stripKeywordForRelatedMangas()
                    .filterNot { word -> words.any { it.lowercase() == word } }
                    .onEach { words.add(it) }

                if (words.isEmpty()) {
                    _state.update { it.copy(isFetching = false) }
                    return@launchIO
                }

                val filterList = eu.kanade.tachiyomi.source.model.FilterList()
                val allResults = java.util.concurrent.ConcurrentLinkedQueue<eu.kanade.tachiyomi.source.model.SManga>()

                kotlinx.coroutines.coroutineScope {
                    words.forEach { keyword ->
                        launch {
                            try {
                                val results = source.getSearchManga(1, keyword, filterList).mangas
                                allResults.addAll(results)
                            } catch (e: Exception) {
                                logcat(LogPriority.ERROR, e) { "Failed related manga search: ${e.message}" }
                            }
                        }
                    }
                }

                val networkToLocalManga = Injekt.get<tachiyomi.domain.manga.interactor.NetworkToLocalManga>()
                val domainMangas = allResults.map {
                    it.toDomainManga(source.id, false) // Defaulting to false for isNovel fallback
                }

                val savedMangas = if (domainMangas.isNotEmpty()) {
                    networkToLocalManga(domainMangas.toList())
                } else {
                    emptyList()
                }

                val filteredMangas = savedMangas.filter { it.id != manga.id }.distinctBy { it.url }

                _state.update {
                    it.copy(
                        mangas = filteredMangas,
                        isFetching = false
                    )
                }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Failed to fetch suggestions: $e" }
                _state.update { it.copy(isFetching = false) }
            }
        }
    }

    data class State(
        val isFetching: Boolean = false,
        val mangas: List<Manga> = emptyList()
    )
}
