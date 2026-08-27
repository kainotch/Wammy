// Created by Notch
package com.example.wammy.source

import com.example.wammy.data.remote.mangadex.MangaDexApi
import com.example.wammy.data.local.ChapterEntity
import com.example.wammy.data.local.MangaEntity

class MangaDexSource(private val api: MangaDexApi) : Source {
    override val id: Long = 1
    override val name: String = "MangaDex"

    override suspend fun fetchLatest(page: Int): List<MangaEntity> {
        val limit = 20
        val offset = (page - 1) * limit
        val response = api.getMangaList(limit = limit, offset = offset)
        
        return response.data.map { manga ->
            val title = manga.attributes.title.values.firstOrNull() ?: "Unknown Title"
            val desc = manga.attributes.description["en"] ?: ""
            val statusStr = manga.attributes.status?.replaceFirstChar { it.uppercase() } ?: "Unknown"
            
            val coverRel = manga.relationships?.find { it.type == "cover_art" }
            val coverFileName = coverRel?.attributes?.fileName
            val coverUrl = if (coverFileName != null) "https://uploads.mangadex.org/covers/${manga.id}/$coverFileName" else null
            
            val authorRel = manga.relationships?.find { it.type == "author" }
            val artistRel = manga.relationships?.find { it.type == "artist" }
            
            val genreStr = manga.attributes.tags?.mapNotNull { it.attributes?.name?.get("en") }?.joinToString(" • ")

            MangaEntity(
                aniListId = null,
                titleRomaji = title,
                coverImageUrl = coverUrl,
                description = desc,
                sourceId = this.id,
                sourceUrl = manga.id,
                author = authorRel?.attributes?.name,
                artist = artistRel?.attributes?.name,
                status = statusStr,
                sourceName = this.name,
                genre = genreStr
            )
        }
    }

    override suspend fun fetchSearchManga(query: String, page: Int): List<MangaEntity> {
        val limit = 20
        val offset = (page - 1) * limit
        val response = api.getMangaList(limit = limit, offset = offset, title = query)
        
        return response.data.map { manga ->
            val title = manga.attributes.title.values.firstOrNull() ?: "Unknown Title"
            val desc = manga.attributes.description["en"] ?: ""
            val statusStr = manga.attributes.status?.replaceFirstChar { it.uppercase() } ?: "Unknown"
            
            val coverRel = manga.relationships?.find { it.type == "cover_art" }
            val coverFileName = coverRel?.attributes?.fileName
            val coverUrl = if (coverFileName != null) "https://uploads.mangadex.org/covers/${manga.id}/$coverFileName" else null
            
            val authorRel = manga.relationships?.find { it.type == "author" }
            val artistRel = manga.relationships?.find { it.type == "artist" }
            
            val genreStr = manga.attributes.tags?.mapNotNull { it.attributes?.name?.get("en") }?.joinToString(" • ")

            MangaEntity(
                aniListId = null,
                titleRomaji = title,
                coverImageUrl = coverUrl,
                description = desc,
                sourceId = this.id,
                sourceUrl = manga.id,
                author = authorRel?.attributes?.name,
                artist = artistRel?.attributes?.name,
                status = statusStr,
                sourceName = this.name,
                genre = genreStr
            )
        }
    }

    override suspend fun fetchChapters(mangaUrl: String): List<ChapterEntity> {
        val response = api.getMangaFeed(mangaId = mangaUrl)
        
        return response.data.map { chapter ->
            val title = chapter.attributes.title ?: "Chapter ${chapter.attributes.chapter}"
            val chapterNum = chapter.attributes.chapter?.toFloatOrNull() ?: 0f
            
            ChapterEntity(
                mangaId = 0L, 
                sourceUrl = chapter.id,
                name = title,
                chapterNumber = chapterNum,
                dateUpload = System.currentTimeMillis()
            )
        }
    }

    override suspend fun fetchPageList(chapterUrl: String): List<String> {
        val response = api.getAtHomeServer(chapterId = chapterUrl)
        val baseUrl = response.baseUrl
        val hash = response.chapter.hash
        
        return response.chapter.data.map { filename ->
            "$baseUrl/data/$hash/$filename"
        }
    }
}
