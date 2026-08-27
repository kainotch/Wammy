// Created by Notch
package com.example.wammy.data.local

import androidx.room.Entity

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlin.jvm.JvmSuppressWildcards
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "relation", primaryKeys = ["mangaId", "relatedMangaId"])
data class RelationEntity(
    val mangaId: Long,
    val relatedMangaId: Long,
    val relationType: String
)

@Dao
@JvmSuppressWildcards
interface MangaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertManga(manga: MangaEntity): Long

    @Query("DELETE FROM manga WHERE id = :id")
    suspend fun deleteMangaById(id: Long): Int

    @Query("UPDATE manga SET downloaded = 0 WHERE id = :id")
    suspend fun clearDownloadedFlag(id: Long): Int

    @Query("SELECT * FROM manga WHERE id = :id")
    suspend fun getMangaById(id: Long): MangaEntity?

    @Query("SELECT * FROM manga WHERE sourceUrl = :sourceUrl LIMIT 1")
    suspend fun getMangaByUrl(sourceUrl: String): MangaEntity?

    @Query("SELECT * FROM manga WHERE sourceUrl = :sourceUrl AND sourceId = :sourceId LIMIT 1")
    suspend fun getMangaByUrlAndSource(sourceUrl: String, sourceId: Long): MangaEntity?

    @Query("SELECT * FROM manga")
    fun getAllManga(): Flow<@JvmSuppressWildcards List<MangaEntity>>
    
    @Query("SELECT * FROM manga WHERE favorite = 1 OR downloaded = 1 OR readCompleted = 1")
    fun getLibraryManga(): Flow<@JvmSuppressWildcards List<MangaEntity>>
    @Query("SELECT * FROM manga WHERE favorite = 1 OR downloaded = 1 OR readCompleted = 1")
    suspend fun getLibraryMangaList(): @JvmSuppressWildcards List<MangaEntity>

    // Recursive CTE to fetch entire franchise tree starting from a specific manga ID
    @Query("""
        WITH RECURSIVE FranchiseTree(mangaId, relatedMangaId, relationType, depth) AS (
            SELECT mangaId, relatedMangaId, relationType, 1
            FROM relation WHERE mangaId = :startMangaId
            UNION
            SELECT r.mangaId, r.relatedMangaId, r.relationType, ft.depth + 1
            FROM relation r
            INNER JOIN FranchiseTree ft ON r.mangaId = ft.relatedMangaId
            WHERE ft.depth < 10
        )
        SELECT m.* FROM manga m 
        INNER JOIN FranchiseTree ft ON m.id = ft.relatedMangaId
    """)
    suspend fun getFranchiseTree(startMangaId: Long): @JvmSuppressWildcards List<MangaEntity>
}

@Dao
@JvmSuppressWildcards
interface ChapterDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertChapters(chapters: @JvmSuppressWildcards List<ChapterEntity>): @JvmSuppressWildcards List<Long>

    @Update
    suspend fun updateChapter(chapter: ChapterEntity): Int

    @Query("SELECT * FROM chapter WHERE mangaId = :mangaId ORDER BY chapterNumber DESC, dateUpload DESC")
    fun getChaptersForManga(mangaId: Long): Flow<@JvmSuppressWildcards List<ChapterEntity>>
    @Query("SELECT * FROM chapter WHERE mangaId = :mangaId ORDER BY chapterNumber DESC, dateUpload DESC")
    suspend fun getChaptersForMangaList(mangaId: Long): @JvmSuppressWildcards List<ChapterEntity>

    @Query("DELETE FROM chapter WHERE mangaId = :mangaId")
    suspend fun deleteChaptersForManga(mangaId: Long): Int
    @Query("SELECT * FROM chapter WHERE mangaId = :mangaId")
    suspend fun getChaptersForMangaSync(mangaId: Long): @JvmSuppressWildcards List<ChapterEntity>
}

@Dao
@JvmSuppressWildcards
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity): Long

    @Query("SELECT * FROM history WHERE mangaSourceUrl = :mangaUrl")
    suspend fun getHistoryByManga(mangaUrl: String): HistoryEntity?

    @Query("SELECT * FROM history GROUP BY mangaSourceUrl ORDER BY MAX(lastReadTimestamp) DESC")
    fun getAllHistory(): Flow<@JvmSuppressWildcards List<HistoryEntity>>
    
    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteHistory(id: Long): Int
    
    @Query("DELETE FROM history")
    suspend fun clearHistory(): Int
}

@androidx.room.Dao
@JvmSuppressWildcards
interface FolderDao {
    @androidx.room.Query("SELECT * FROM folder ORDER BY isPinned DESC, sortOrder ASC, createdAt DESC")
    fun getAllFolders(): kotlinx.coroutines.flow.Flow<@JvmSuppressWildcards List<FolderEntity>>

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @androidx.room.Query("DELETE FROM folder WHERE id = :id")
    suspend fun deleteFolder(id: Long): Int

    @androidx.room.Update
    suspend fun updateFolder(folder: FolderEntity): Int

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.IGNORE)
    suspend fun insertMangaFolderCrossRef(crossRef: MangaFolderCrossRef): Long

    @androidx.room.Query("DELETE FROM manga_folder_cross_ref WHERE mangaId = :mangaId AND folderId = :folderId")
    suspend fun deleteMangaFolderCrossRef(mangaId: Long, folderId: Long): Int

    @androidx.room.Query("SELECT folderId FROM manga_folder_cross_ref WHERE mangaId = :mangaId")
    fun getFoldersForManga(mangaId: Long): kotlinx.coroutines.flow.Flow<@JvmSuppressWildcards List<Long>>

    @androidx.room.Query("SELECT * FROM manga_folder_cross_ref")
    fun getAllMangaFolderCrossRefs(): kotlinx.coroutines.flow.Flow<@JvmSuppressWildcards List<MangaFolderCrossRef>>

    @androidx.room.Query("""
        SELECT m.* FROM manga m
        INNER JOIN manga_folder_cross_ref crossRef ON m.id = crossRef.mangaId
        WHERE crossRef.folderId = :folderId
    """)
    fun getMangaForFolder(folderId: Long): kotlinx.coroutines.flow.Flow<@JvmSuppressWildcards List<MangaEntity>>
    
    @androidx.room.Query("SELECT * FROM folder WHERE id = :id")
    suspend fun getFolderById(id: Long): FolderEntity?
}

@Dao
@JvmSuppressWildcards
interface TrackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackEntity): Long

    @Update
    suspend fun updateTrack(track: TrackEntity): Int

    @Query("SELECT * FROM manga_sync WHERE mangaId = :mangaId")
    fun getTracksForManga(mangaId: Long): Flow<@JvmSuppressWildcards List<TrackEntity>>

    @Query("DELETE FROM manga_sync WHERE mangaId = :mangaId AND syncId = :syncId")
    suspend fun deleteTrack(mangaId: Long, syncId: Int): Int
    @Query("SELECT * FROM manga_sync WHERE mangaId = :mangaId")
    suspend fun getTracksForMangaSync(mangaId: Long): @JvmSuppressWildcards List<TrackEntity>
}

@Dao
interface BackupDao {
    @Query("SELECT * FROM manga")
    suspend fun getAllMangaSync(): @JvmSuppressWildcards List<MangaEntity>
    @Query("SELECT * FROM folder")
    suspend fun getAllFoldersSync(): @JvmSuppressWildcards List<FolderEntity>
    @Query("SELECT * FROM manga_folder_cross_ref WHERE mangaId = :mangaId")
    suspend fun getFoldersForMangaSync(mangaId: Long): @JvmSuppressWildcards List<MangaFolderCrossRef>
}
