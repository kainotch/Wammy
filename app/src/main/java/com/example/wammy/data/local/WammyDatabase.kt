// Created by Notch
package com.example.wammy.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        MangaEntity::class,
        ChapterEntity::class,
        CategoryEntity::class,
        HistoryEntity::class,
        MangaCategoryCrossRef::class,
        RelationEntity::class,
        FolderEntity::class,
        MangaFolderCrossRef::class,
        TrackEntity::class
    ],
    version = 11,
    exportSchema = false
)
abstract class WammyDatabase : RoomDatabase() {
    abstract fun mangaDao(): MangaDao
    abstract fun chapterDao(): ChapterDao
    abstract fun historyDao(): HistoryDao
    abstract fun folderDao(): FolderDao
    abstract fun trackDao(): TrackDao
    abstract fun backupDao(): BackupDao
    abstract fun statisticsDao(): StatisticsDao
}
