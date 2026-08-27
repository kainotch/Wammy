// Created by Notch
package com.example.wammy.source

import com.example.wammy.data.local.ChapterEntity
import com.example.wammy.data.local.MangaEntity

interface Source {
    val id: Long
    val name: String
    
    suspend fun fetchLatest(page: Int): List<MangaEntity>
    
    suspend fun fetchSearchManga(query: String, page: Int): List<MangaEntity>
    
    suspend fun fetchChapters(mangaUrl: String): List<ChapterEntity>
    
    suspend fun fetchPageList(chapterUrl: String): List<String>
}
