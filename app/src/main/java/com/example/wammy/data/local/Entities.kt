// Created by Notch
package com.example.wammy.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Index
  import androidx.room.ForeignKey

@Entity(tableName = "manga", indices = [Index(value = ["aniListId"], unique = true)])
data class MangaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val aniListId: Int?,
    val titleRomaji: String,
    val coverImageUrl: String?,
    val description: String?,
    val sourceId: Long,
    val sourceUrl: String,
    val author: String? = null,
    val artist: String? = null,
    val status: String? = null,
    val sourceName: String = "Unknown",
    val genre: String? = null,
    val favorite: Boolean = false,
    val readCompleted: Boolean = false,
    val downloaded: Boolean = false,
    val isNovel: Boolean = false,
    val novelPkgName: String? = null,
    val novelApkFile: String? = null
)

@Entity(
    tableName = "chapter",
    indices = [Index(value = ["mangaId", "sourceUrl"], unique = true)]
)
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mangaId: Long,
    val sourceUrl: String,
    val name: String,
    val chapterNumber: Float,
    val dateUpload: Long,
    val read: Boolean = false,
    val lastPageRead: Int = 0
)

@Entity(tableName = "category")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val sortOrder: Int
)

@Entity(primaryKeys = ["mangaId", "categoryId"])
data class MangaCategoryCrossRef(
    val mangaId: Long,
    val categoryId: Long
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mangaSourceUrl: String,
    val mangaTitle: String,
    val mangaCoverUrl: String,
    val chapterSourceUrl: String,
    val chapterName: String,
    val lastPageRead: Int,
    val totalPages: Int,
    val lastReadTimestamp: Long
)

@Entity(tableName = "folder")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val coverImageUri: String? = null,
    val isPinned: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "manga_folder_cross_ref",
    primaryKeys = ["mangaId", "folderId"],
    indices = [Index(value = ["folderId"])]
)
data class MangaFolderCrossRef(
    val mangaId: Long,
    val folderId: Long
)

@Entity(
    tableName = "manga_sync",
    foreignKeys = [
        ForeignKey(
            entity = MangaEntity::class,
            parentColumns = ["id"],
            childColumns = ["mangaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("mangaId")]
)
data class TrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mangaId: Long,
    val syncId: Int,
    val remoteId: Long,
    val title: String,
    val lastChapterRead: Float = 0f,
    val totalChapters: Int = 0,
    val score: Float = 0f,
    val status: Int = 0,
    val trackingUrl: String = ""
)
