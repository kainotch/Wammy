// Created by Notch
package com.example.wammy.data.backup

import android.content.Context
import java.io.File

class CacheManager(private val context: Context) {

    fun clearChapterCache() {
        val coilCache = File(context.cacheDir, "image_cache")
        if (coilCache.exists()) {
            coilCache.deleteRecursively()
        }
        
        val tempDownloads = File(context.filesDir, "temp_chapters")
        if (tempDownloads.exists()) {
            tempDownloads.deleteRecursively()
        }
    }
}
