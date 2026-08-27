package com.example.wammy.data.local

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StatisticsDao {
    @Query("SELECT COUNT(*) FROM manga WHERE favorite = 1")
    fun getInLibraryCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM manga WHERE readCompleted = 1")
    fun getCompletedCount(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT mangaSourceUrl) FROM history")
    fun getStartedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM chapter WHERE mangaId IN (SELECT id FROM manga WHERE favorite = 1)")
    fun getTotalChaptersCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM chapter WHERE read = 1")
    fun getReadChaptersCount(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT mangaId) FROM manga_sync")
    fun getTrackedEntriesCount(): Flow<Int>

    @Query("SELECT AVG(score) FROM manga_sync WHERE score > 0")
    fun getMeanScore(): Flow<Float?>

    @Query("SELECT COUNT(DISTINCT syncId) FROM manga_sync")
    fun getUsedTrackersCount(): Flow<Int>
}
