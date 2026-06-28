package com.bloodpressure.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.datetime.LocalDate

@Dao
interface TrainingDao {
    @Insert
    suspend fun insert(record: TrainingRecord): Long

    @Query("SELECT * FROM training_records ORDER BY date DESC, startTime DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<TrainingRecord>

    @Query("SELECT * FROM training_records WHERE date BETWEEN :from AND :to ORDER BY date DESC")
    suspend fun getByDateRange(from: LocalDate, to: LocalDate): List<TrainingRecord>

    @Query("SELECT COUNT(*) FROM training_records WHERE date BETWEEN :from AND :to AND completed = 1")
    suspend fun getCompletedCount(from: LocalDate, to: LocalDate): Int

    @Query("SELECT COUNT(*) FROM training_records WHERE completed = 1")
    suspend fun getTotalCompleted(): Int

    @Query("SELECT SUM(totalDuration) FROM training_records WHERE completed = 1")
    suspend fun getTotalDuration(): Int?

    @Query("DELETE FROM training_records WHERE id = :id")
    suspend fun deleteById(id: Long)
}
