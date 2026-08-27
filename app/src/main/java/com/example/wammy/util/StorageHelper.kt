package com.example.wammy.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.wammy.AppContainer
import com.example.wammy.data.local.ChapterEntity
import com.example.wammy.data.local.MangaEntity
import kotlinx.coroutines.flow.firstOrNull
import java.io.File
import java.io.InputStream
import java.io.OutputStream

object StorageHelper {

    suspend fun getStorageUri(): String? {
        return AppContainer.storagePreferences.storageUri.value
    }

    private suspend fun getRootDocument(context: Context): DocumentFile? {
        val uriString = getStorageUri() ?: return null
        return try {
            DocumentFile.fromTreeUri(context, Uri.parse(uriString))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getMangaDocumentDir(context: Context, manga: MangaEntity): DocumentFile? {
        val root = getRootDocument(context) ?: return null
        
        val downloads = root.findFile("downloads") ?: root.createDirectory("downloads")
        val sourceDir = downloads?.findFile(DownloadManager.cleanFileName(manga.sourceName)) 
            ?: downloads?.createDirectory(DownloadManager.cleanFileName(manga.sourceName))
        return sourceDir?.findFile(DownloadManager.cleanFileName(manga.titleRomaji))
            ?: sourceDir?.createDirectory(DownloadManager.cleanFileName(manga.titleRomaji))
    }

    suspend fun getChapterDocumentDir(context: Context, manga: MangaEntity, chapter: ChapterEntity): DocumentFile? {
        val mangaDir = getMangaDocumentDir(context, manga) ?: return null
        return mangaDir.findFile(DownloadManager.cleanFileName(chapter.name))
            ?: mangaDir.createDirectory(DownloadManager.cleanFileName(chapter.name))
    }
}
