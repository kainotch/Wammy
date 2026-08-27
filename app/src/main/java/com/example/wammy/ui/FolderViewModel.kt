// Created by Notch
package com.example.wammy.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wammy.AppContainer
import com.example.wammy.data.local.FolderEntity
import com.example.wammy.data.local.MangaEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FolderViewModel(private val folderId: Long) : ViewModel() {

    private val _folder = MutableStateFlow<FolderEntity?>(null)
    val folder: StateFlow<FolderEntity?> = _folder.asStateFlow()

    private val _mangas = MutableStateFlow<List<MangaEntity>>(emptyList())
    val mangas: StateFlow<List<MangaEntity>> = _mangas.asStateFlow()

    init {
        viewModelScope.launch {
            _folder.value = AppContainer.database.folderDao().getFolderById(folderId)
        }
        viewModelScope.launch {
            AppContainer.database.folderDao().getMangaForFolder(folderId).collect {
                _mangas.value = it
            }
        }
    }

    
    fun updateFolder(context: android.content.Context, name: String, coverImageUri: String?, isPinned: Boolean) {
        val currentFolder = _folder.value ?: return
        
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
        
        viewModelScope.launch {
            val updated = currentFolder.copy(
                name = name,
                coverImageUri = coverImageUri,
                isPinned = isPinned
            )
            AppContainer.database.folderDao().updateFolder(updated)
            _folder.value = updated
        }
    }

    fun removeMangaFromFolder(manga: MangaEntity) {
        viewModelScope.launch {
            AppContainer.database.folderDao().deleteMangaFolderCrossRef(manga.id, folderId)
        }
    }

    class Factory(private val folderId: Long) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FolderViewModel(folderId) as T
        }
    }
}
