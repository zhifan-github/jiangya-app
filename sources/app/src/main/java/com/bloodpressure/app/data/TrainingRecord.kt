package com.bloodpressure.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate

@Entity(tableName = "training_records")
data class TrainingRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val startTime: Long, // epoch millis
    val totalDuration: Int, // seconds
    val completedGroups: Int,
    val totalGroups: Int,
    val groupDuration: Int, // seconds per group
    val restDuration: Int, // seconds of rest between groups
    val completed: Boolean,
    val notes: String? = null
)
