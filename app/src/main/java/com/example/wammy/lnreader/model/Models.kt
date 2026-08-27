package com.example.wammy.lnreader.model

import kotlinx.serialization.Serializable

data class LNNovel(val url: String, val title: String, val coverUrl: String? = null)
data class LNChapter(val url: String, val title: String, val timestamp: Long = 0L)

@Serializable
data class IReaderExtensionMeta(
    val pkg: String,
    val apk: String,
    val name: String,
    val id: Long,
    val lang: String,
    val code: Int = 1,
    val version: String,
    val description: String = "",
    val nsfw: Boolean = false,
    val sourceDir: String = "main",
    val iconUrl: String = ""
)

data class ChapterItem(
    val name: String,
    val path: String,
    val releaseTime: String? = null,
    val chapterNumber: Double? = null,
    val page: String? = null
)

data class SourceNovel(
    val path: String,
    val name: String? = null,
    val cover: String? = null,
    val genres: String? = null,
    val summary: String? = null,
    val author: String? = null,
    val artist: String? = null,
    val status: String? = null,
    val chapters: List<ChapterItem>? = null
)
