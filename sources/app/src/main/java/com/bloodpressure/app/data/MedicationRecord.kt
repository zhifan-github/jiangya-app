package com.bloodpressure.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,           // 药名
    val dosage: String,         // 当前剂量，如 "5mg"
    val isActive: Boolean = true // 是否正在使用
)

@Entity(tableName = "medication_records")
data class MedicationRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,     // 关联药物ID
    val date: LocalDate,        // 日期
    val taken: Boolean,         // 是否已服用
    val dosage: String,         // 本次服用剂量
    val takenAt: Long? = null,  // 实际服用时间戳
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "dosage_changes")
data class DosageChange(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,      // 关联药物ID
    val date: LocalDate,        // 变更日期
    val oldDosage: String,      // 旧剂量
    val newDosage: String,       // 新剂量
    val note: String = "",      // 变更原因
    val createdAt: Long = System.currentTimeMillis()
)
