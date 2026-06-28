package com.bloodpressure.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

@Entity(tableName = "blood_pressure_records")
data class BloodPressureRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val systolic: Int,          // 收缩压
    val diastolic: Int,         // 舒张压
    val heartRate: Int,         // 心率
    val date: LocalDate,        // 日期
    val time: LocalTime,        // 时间
    val period: MeasurementPeriod, // 早/晚
    val medicationTaken: Boolean = false, // 本次测量时是否已服药
    val note: String = "",      // 备注
    val createdAt: Long = System.currentTimeMillis()
)

enum class MeasurementPeriod {
    MORNING, EVENING
}

class Converters {
    @TypeConverter
    fun fromPeriod(period: MeasurementPeriod): String = period.name

    @TypeConverter
    fun toPeriod(name: String): MeasurementPeriod = MeasurementPeriod.valueOf(name)

    @TypeConverter
    fun fromDate(date: LocalDate): String = date.toString()

    @TypeConverter
    fun toDate(str: String): LocalDate = LocalDate.parse(str)

    @TypeConverter
    fun fromTime(time: LocalTime): String = time.toString()

    @TypeConverter
    fun toTime(str: String): LocalTime = LocalTime.parse(str)
}
