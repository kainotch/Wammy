// Created by Notch
package com.example.wammy.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wammy.AppContainer
import com.example.wammy.data.local.MangaEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map

import kotlinx.coroutines.launch
class LibraryViewModel : ViewModel() {

    private val _searchQuery = kotlinx.coroutines.flow.MutableStateFlow("")
    val libraryManga: StateFlow<List<MangaEntity>> = kotlinx.coroutines.flow.combine(
        AppContainer.database.mangaDao().getLibraryManga(),
        _searchQuery
    ) { mangaList, query ->
        val filtered = mangaList.filter { !it.isNovel }
        if (query.isEmpty()) filtered else filtered.filter { it.titleRomaji.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    val libraryNovels: StateFlow<List<MangaEntity>> = kotlinx.coroutines.flow.combine(
        AppContainer.database.mangaDao().getLibraryManga(),
        _searchQuery
    ) { mangaList, query ->
        val filtered = mangaList.filter { it.isNovel }
        if (query.isEmpty()) filtered else filtered.filter { it.titleRomaji.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        val allLibraryItems: StateFlow<List<MangaEntity>> = kotlinx.coroutines.flow.combine(
        AppContainer.database.mangaDao().getLibraryManga(),
        _searchQuery
    ) { mangaList, query ->
        if (query.isEmpty()) emptyList() else mangaList.filter { it.titleRomaji.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val continueReading: StateFlow<List<com.example.wammy.data.local.HistoryEntity>> = AppContainer.database.historyDao().getAllHistory()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allManga: StateFlow<List<MangaEntity>> = AppContainer.database.mangaDao().getAllManga()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())


    data class LibraryStats(
        val entries: Int = 0,
        val favorites: Int = 0,
        val inProgress: Int = 0,
        val downloaded: Int = 0,
        val completed: Int = 0
    )

    val stats: StateFlow<LibraryStats> = kotlinx.coroutines.flow.combine(
        AppContainer.database.mangaDao().getLibraryManga(),
        continueReading
    ) { mangaList, historyList ->
        LibraryStats(
            entries = mangaList.size,
            favorites = mangaList.count { it.favorite },
            inProgress = historyList.size,
            downloaded = mangaList.count { it.downloaded },
            completed = mangaList.count { it.readCompleted }
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, LibraryStats())

    
    val currentQuery: StateFlow<String> = _searchQuery.stateIn(viewModelScope, SharingStarted.Lazily, "")

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
        
    val folders: StateFlow<List<com.example.wammy.data.local.FolderEntity>> = AppContainer.database.folderDao().getAllFolders()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val folderCounts: StateFlow<Map<Long, Int>> = AppContainer.database.folderDao().getAllMangaFolderCrossRefs()
        .map { crossRefs -> crossRefs.groupingBy { it.folderId }.eachCount() }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    
    fun updateFolder(context: android.content.Context, folderId: Long, name: String, coverImageUri: String?, isPinned: Boolean) {
        viewModelScope.launch {
            val currentFolder = AppContainer.database.folderDao().getFolderById(folderId) ?: return@launch
            if (coverImageUri != null && coverImageUri != currentFolder.coverImageUri) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        android.net.Uri.parse(coverImageUri),
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            val updated = currentFolder.copy(
                name = name,
                coverImageUri = coverImageUri,
                isPinned = isPinned
            )
            AppContainer.database.folderDao().updateFolder(updated)
        }
    }

    fun deleteFolder(folderId: Long) {
        viewModelScope.launch {
            AppContainer.database.folderDao().deleteFolder(folderId)
        }
    }

    fun toggleFolderPin(folderId: Long) {
        viewModelScope.launch {
            val folder = AppContainer.database.folderDao().getFolderById(folderId) ?: return@launch
            AppContainer.database.folderDao().updateFolder(folder.copy(isPinned = !folder.isPinned))
        }
    }

    /** Delete a downloaded manga's files + remove it from DB entirely. */
    fun deleteMangaFromLibrary(manga: MangaEntity) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            com.example.wammy.util.DownloadManager.cancelDownload(manga.sourceUrl)
            val dir = com.example.wammy.util.DownloadManager.getMangaDir(AppContainer.appContext, manga)
            if (dir.exists()) dir.deleteRecursively()
            val dbManga = AppContainer.database.mangaDao().getMangaByUrl(manga.sourceUrl)
            if (dbManga != null) {
                AppContainer.database.chapterDao().deleteChaptersForManga(dbManga.id)
                AppContainer.database.mangaDao().deleteMangaById(dbManga.id)
            }
        }
    }

    /** Toggle the downloaded manga's pin state — reuses isPinned on FolderEntity pattern via downloaded flag trick;
     *  here we just add/remove the book from a virtual "Pinned" system folder concept.
     *  For simplicity we store pin state directly on MangaEntity by marking readCompleted temporarily –
     *  Actually, we don't have a pin field on MangaEntity. Instead just mark favourite as a visual pin. */
    fun toggleMangaPin(manga: MangaEntity) {
        viewModelScope.launch {
            val dbManga = AppContainer.database.mangaDao().getMangaByUrl(manga.sourceUrl) ?: return@launch
            AppContainer.database.mangaDao().insertManga(dbManga.copy(favorite = !dbManga.favorite))
        }
    }

    fun createFolder(context: android.content.Context, name: String, coverImageUri: String?, isPinned: Boolean) {
        if (coverImageUri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    android.net.Uri.parse(coverImageUri),
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        viewModelScope.launch {
            val folder = com.example.wammy.data.local.FolderEntity(
                name = name,
                coverImageUri = coverImageUri,
                isPinned = isPinned
            )
            AppContainer.database.folderDao().insertFolder(folder)
        }
    }


}
