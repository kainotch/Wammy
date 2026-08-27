// Created by Notch
package com.example.wammy.ui
import kotlinx.coroutines.isActive
import okio.sink
import okio.buffer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wammy.AppContainer
import com.example.wammy.data.local.ChapterEntity
import com.example.wammy.data.local.MangaEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import com.example.wammy.util.NetworkUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import eu.kanade.tachiyomi.source.model.SChapter
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import eu.kanade.tachiyomi.source.model.SManga
import com.example.wammy.extension.awaitFirst
class DetailsViewModel : ViewModel() {
    private val okHttpClient = okhttp3.OkHttpClient()
    private val mangaMutex = kotlinx.coroutines.sync.Mutex()

    private val _isLoadingChapters = MutableStateFlow(true)
    val isLoadingChapters: StateFlow<Boolean> = _isLoadingChapters.asStateFlow()

    private val _chapterLoadError = MutableStateFlow<String?>(null)
    val chapterLoadError: StateFlow<String?> = _chapterLoadError.asStateFlow()

    private val _manga = MutableStateFlow<MangaEntity?>(null)
    val manga: StateFlow<MangaEntity?> = _manga.asStateFlow()

    private val _chapters = MutableStateFlow<List<ChapterEntity>>(emptyList())
    val chapters: StateFlow<List<ChapterEntity>> = _chapters.asStateFlow()

    fun getMangaUrl(manga: MangaEntity): String? {
        if (manga.sourceId == 1L) {
            return "https://mangadex.org/title/${manga.sourceUrl}"
        }
        val source = AppContainer.extensionManager.activeSources.find { it.id == manga.sourceId }
        if (source is eu.kanade.tachiyomi.source.online.HttpSource) {
            try {
                val sManga = SManga.create().apply {
                    url = manga.sourceUrl
                    title = manga.titleRomaji
                }
                return source.mangaDetailsRequest(sManga).url.toString()
            } catch (e: Exception) {
                return null
            }
        }
        return null
    }

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _isDownloaded = MutableStateFlow(false)
    val isDownloaded: StateFlow<Boolean> = _isDownloaded.asStateFlow()

    private val _downloadingChapters = MutableStateFlow<Set<String>>(emptySet())
    val downloadingChapters: StateFlow<Set<String>> = _downloadingChapters.asStateFlow()

    private val _downloadedChapters = MutableStateFlow<Set<String>>(emptySet())
    val downloadedChapters: StateFlow<Set<String>> = _downloadedChapters.asStateFlow()

    private val _tracks = kotlinx.coroutines.flow.MutableStateFlow<List<com.example.wammy.data.local.TrackEntity>>(emptyList())
    val tracks: kotlinx.coroutines.flow.StateFlow<List<com.example.wammy.data.local.TrackEntity>> = _tracks.asStateFlow()

    private val _allFolders = kotlinx.coroutines.flow.MutableStateFlow<List<com.example.wammy.data.local.FolderEntity>>(emptyList())
    val allFolders: kotlinx.coroutines.flow.StateFlow<List<com.example.wammy.data.local.FolderEntity>> = _allFolders.asStateFlow()

    private val _mangaFolders = kotlinx.coroutines.flow.MutableStateFlow<List<Long>>(emptyList())
    val mangaFolders: kotlinx.coroutines.flow.StateFlow<List<Long>> = _mangaFolders.asStateFlow()
    
    init {
        viewModelScope.launch {
            AppContainer.database.folderDao().getAllFolders().collect { folders ->
                _allFolders.value = folders
            }
        }
        // Watch DownloadManager — whenever a chapter finishes for the current manga,
        // re-scan the filesystem so tick marks appear in real time.
        viewModelScope.launch {
            while (isActive) {
                if (_manga.value != null) {
                    checkDownloadedChapters(com.example.wammy.AppContainer.appContext)
                }
                kotlinx.coroutines.delay(1000)
            }
        }
    }






    fun fetchMangaDetails(context: android.content.Context, manga: MangaEntity) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                if (!com.example.wammy.util.NetworkUtils.isOnline(context)) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "No internet connection", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                
                if (manga.sourceId == 1L) {
                    // MangaDex - currently unsupported for details fetching in Wammy? We skip for now or rely on chapters.
                    fetchChaptersForManga(manga)
                } else {
                    val source = AppContainer.extensionManager.activeSources.find { it.id == manga.sourceId }
                    if (source != null) {
                        val sManga = eu.kanade.tachiyomi.source.model.SManga.create().apply { url = manga.sourceUrl }
                        val networkDetails = source.getMangaDetails(sManga)
                        
                        // Update our local manga entity with the new details
                        val updatedManga = manga.copy(
                            titleRomaji = networkDetails.title ?: manga.titleRomaji,
                            author = networkDetails.author ?: manga.author,
                            description = networkDetails.description ?: manga.description,
                            genre = networkDetails.genre ?: manga.genre,
                            status = when (networkDetails.status) {
                                eu.kanade.tachiyomi.source.model.SManga.ONGOING -> "Ongoing"
                                eu.kanade.tachiyomi.source.model.SManga.COMPLETED -> "Completed"
                                eu.kanade.tachiyomi.source.model.SManga.LICENSED -> "Licensed"
                                eu.kanade.tachiyomi.source.model.SManga.PUBLISHING_FINISHED -> "Publishing Finished"
                                eu.kanade.tachiyomi.source.model.SManga.CANCELLED -> "Cancelled"
                                eu.kanade.tachiyomi.source.model.SManga.ON_HIATUS -> "On Hiatus"
                                else -> manga.status
                            },
                            coverImageUrl = networkDetails.thumbnail_url ?: manga.coverImageUrl
                        )
                        
                        // Save to DB and update UI
                        AppContainer.database.mangaDao().insertManga(updatedManga)
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            _manga.value = updatedManga
                            android.widget.Toast.makeText(context, "Refreshed", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                    
                    // Also refresh chapters
                    fetchChaptersForManga(manga)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Failed to refresh: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun loadManga(mangaId: String, currentList: List<MangaEntity>) {
        viewModelScope.launch {
            // Check DB first
            val dbManga = AppContainer.database.mangaDao().getMangaByUrl(mangaId)
            val foundManga = dbManga ?: currentList.find { it.sourceUrl == mangaId }
            
            
            _manga.value = foundManga
            _isFavorite.value = foundManga?.favorite ?: false
            _isDownloaded.value = foundManga?.downloaded ?: false
            

            if (foundManga != null) {
                fetchChaptersForManga(foundManga)
                
                // Observe folders for this manga
                viewModelScope.launch {
                    AppContainer.database.folderDao().getFoldersForManga(foundManga.id).collect { folderIds ->
                        _mangaFolders.value = folderIds
                    }
                }
                
                // Observe tracking metadata for this manga
                viewModelScope.launch {
                    AppContainer.database.trackDao().getTracksForManga(foundManga.id).collect { trackList ->
                        _tracks.value = trackList
                    }
                }
            }
        }
    }

    fun toggleFavorite() {
        val currentManga = _manga.value ?: return
        viewModelScope.launch {
            val newFavState = !_isFavorite.value
            val updatedManga = currentManga.copy(favorite = newFavState)
            
            // Wait, if it came from currentList, its ID is 0. 
            // We should just check if DB has it to keep ID.
            val dbManga = AppContainer.database.mangaDao().getMangaByUrl(currentManga.sourceUrl)
            val mangaToSave = if (dbManga != null) {
                dbManga.copy(favorite = newFavState)
            } else {
                updatedManga.copy(id = 0L)
            }
            
            AppContainer.database.mangaDao().insertManga(mangaToSave)
            _manga.value = mangaToSave
            _isFavorite.value = newFavState
        }
    }

    fun retryLoadChapters() {
        _manga.value?.let { fetchChaptersForManga(it) }
    }

private fun fetchChaptersForManga(manga: MangaEntity) {
        viewModelScope.launch {
            _isLoadingChapters.value = true
            _chapterLoadError.value = null
            try {
                // Always get the authoritative DB version first to get the correct ID
                val dbManga = AppContainer.database.mangaDao().getMangaByUrl(manga.sourceUrl)
                val realManga = dbManga ?: manga
                val realMangaId = dbManga?.id ?: manga.id

                // Load chapters from DB using the real ID
                if (realMangaId > 0L) {
                    viewModelScope.launch {
                        AppContainer.database.chapterDao().getChaptersForManga(realMangaId).collect { localChapters ->
                            if (localChapters.isNotEmpty()) {
                                _chapters.value = localChapters
                                checkDownloadedChapters(AppContainer.appContext)
                            }
                        }
                    }
                }

                // Always try network to get fresh chapter list
                if (NetworkUtils.isOnline(AppContainer.appContext)) {
                    val chaptersList: List<com.example.wammy.data.local.ChapterEntity> = try {
                        if (realManga.sourceId == 1L) {
                            AppContainer.mangaDexSource.fetchChapters(realManga.sourceUrl)
                        } else {
                            val source = AppContainer.extensionManager.activeSources.find { it.id == realManga.sourceId }
                            if (source != null) {
                                val sManga = eu.kanade.tachiyomi.source.model.SManga.create().apply { url = realManga.sourceUrl }
                                val sChapters = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { source.getChapterList(sManga) }
                                sChapters.mapIndexed { index, sCh ->
                                    val parsedNumber = if (sCh.chapter_number >= 0f) {
                                        sCh.chapter_number
                                    } else {
                                        com.example.wammy.util.ChapterRecognition.parseChapterNumber(realManga.titleRomaji, sCh.name ?: "")
                                    }
                                    
                                    com.example.wammy.data.local.ChapterEntity(
                                        mangaId = realMangaId,
                                        name = sCh.name ?: ("Chapter " + (index + 1)),
                                        sourceUrl = sCh.url,
                                        chapterNumber = if (parsedNumber >= 0f) parsedNumber else (sChapters.size - index).toFloat(),
                                        dateUpload = sCh.date_upload
                                    )
                                }
                            } else emptyList()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        emptyList()
                    }

                    if (chaptersList.isNotEmpty()) {
                        // Ensure manga is in DB before inserting chapters (foreign key)
                        val finalMangaId = if (realMangaId > 0L) {
                            realMangaId
                        } else {
                            val newId = AppContainer.database.mangaDao().insertManga(manga.copy(id = 0L))
                            _manga.value = manga.copy(id = newId)
                            newId
                        }

                        val chaptersToInsert = chaptersList.map { it.copy(mangaId = finalMangaId) }
                        AppContainer.database.chapterDao().insertChapters(chaptersToInsert)

                        // Re-read fresh from DB so IDs and read-state are accurate
                        val fresh = AppContainer.database.chapterDao().getChaptersForManga(finalMangaId).firstOrNull()
                        _chapters.value = fresh ?: chaptersToInsert
                        checkDownloadedChapters(AppContainer.appContext)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _chapterLoadError.value = e.message
            } finally {
                _isLoadingChapters.value = false
            }
        }
    }

    fun checkDownloadedChapters(context: android.content.Context) {
        val currentManga = _manga.value ?: return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val downloadedSet = mutableSetOf<String>()
            for (chapter in _chapters.value) {
                val safDir = com.example.wammy.util.StorageHelper.getChapterDocumentDir(com.example.wammy.AppContainer.appContext, currentManga, chapter)
                val isSafDownloaded = safDir != null && safDir.exists() && safDir.listFiles().isNotEmpty()
                
                val storageDir = com.example.wammy.util.DownloadManager.getChapterDir(com.example.wammy.AppContainer.appContext, currentManga, chapter)
                val isInternalDownloaded = storageDir.exists() && storageDir.listFiles()?.isNotEmpty() == true
                
                if (isSafDownloaded || isInternalDownloaded) {
                    downloadedSet.add(chapter.sourceUrl)
                }
            }
            if (_downloadedChapters.value != downloadedSet) {
                _downloadedChapters.value = downloadedSet
            }
        }
    }

    /** Delete all downloaded chapter files from disk + clear the downloaded flag.
     *  The book stays in the library (favourite / completed flags are kept). */
    fun deleteDownloads(onDone: () -> Unit = {}) {
        val currentManga = _manga.value ?: return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            // Cancel any active download first
            com.example.wammy.util.DownloadManager.cancelDownload(currentManga.sourceUrl)

            // Delete all downloaded files from disk
            val mangaDir = com.example.wammy.util.DownloadManager.getMangaDir(AppContainer.appContext, currentManga)
            if (mangaDir.exists()) mangaDir.deleteRecursively()

            // Clear the downloaded flag in DB but keep the record
            val dbManga = AppContainer.database.mangaDao().getMangaByUrl(currentManga.sourceUrl)
            if (dbManga != null) {
                AppContainer.database.mangaDao().clearDownloadedFlag(dbManga.id)
                AppContainer.database.chapterDao().deleteChaptersForManga(dbManga.id)
            }

            _downloadedChapters.value = emptySet()
            _isDownloaded.value = false
            _manga.value = currentManga.copy(downloaded = false)

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { onDone() }
        }
    }

    /** Completely remove the book from the library and delete all its downloaded files. */
    fun deleteFromLibrary(onDone: () -> Unit = {}) {
        val currentManga = _manga.value ?: return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            com.example.wammy.util.DownloadManager.cancelDownload(currentManga.sourceUrl)

            val mangaDir = com.example.wammy.util.DownloadManager.getMangaDir(AppContainer.appContext, currentManga)
            if (mangaDir.exists()) mangaDir.deleteRecursively()

            val dbManga = AppContainer.database.mangaDao().getMangaByUrl(currentManga.sourceUrl)
            if (dbManga != null) {
                AppContainer.database.chapterDao().deleteChaptersForManga(dbManga.id)
                AppContainer.database.mangaDao().deleteMangaById(dbManga.id)
            }

            _downloadedChapters.value = emptySet()
            _isDownloaded.value = false
            _isFavorite.value = false
            _chapters.value = emptyList()
            _manga.value = null

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { onDone() }
        }
    }

    fun downloadAllChapters(context: android.content.Context, chaptersToDownload: List<com.example.wammy.data.local.ChapterEntity>) {
        val safeManga = _manga.value ?: return
        val validChapters = chaptersToDownload.filter { 
            !_downloadedChapters.value.contains(it.sourceUrl) 
        }
        com.example.wammy.util.DownloadManager.startDownload(safeManga, validChapters)
    }

    fun downloadChapter(context: android.content.Context, chapter: com.example.wammy.data.local.ChapterEntity) {
        val safeManga = _manga.value ?: return
        if (_downloadedChapters.value.contains(chapter.sourceUrl)) return
        com.example.wammy.util.DownloadManager.startDownload(safeManga, listOf(chapter))
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

    fun toggleFolderForManga(folderId: Long) {
        val currentManga = _manga.value ?: return
        viewModelScope.launch {
            val finalMangaId = mangaMutex.withLock {
                val dbManga = AppContainer.database.mangaDao().getMangaByUrl(currentManga.sourceUrl)
                if (dbManga != null) {
                    dbManga.id
                } else {
                    val newId = AppContainer.database.mangaDao().insertManga(currentManga.copy(id = 0L))
                    _manga.value = currentManga.copy(id = newId)
                    newId
                }
            }
            

            val currentlyInFolder = _mangaFolders.value.contains(folderId)
            if (currentlyInFolder) {
                AppContainer.database.folderDao().deleteMangaFolderCrossRef(finalMangaId, folderId)
                _mangaFolders.value = _mangaFolders.value - folderId
            } else {
                AppContainer.database.folderDao().insertMangaFolderCrossRef(
                    com.example.wammy.data.local.MangaFolderCrossRef(mangaId = finalMangaId, folderId = folderId)
                )
                _mangaFolders.value = _mangaFolders.value + folderId
            }

        }
    }

    fun bindTracker(trackEntity: com.example.wammy.data.local.TrackEntity) {
        viewModelScope.launch {
            val service = AppContainer.trackManager.getService(trackEntity.syncId)
            if (service != null) {
                try {
                    val syncedTrack = service.bind(trackEntity)
                    AppContainer.database.trackDao().insertTrack(syncedTrack)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
