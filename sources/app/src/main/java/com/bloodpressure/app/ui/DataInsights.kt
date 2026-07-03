package com.bloodpressure.app.ui

import com.bloodpressure.app.data.BloodPressureRecord
import kotlinx.datetime.LocalDate
import kotlin.math.roundToInt

enum class BpLevel {
    NORMAL,
    ELEVATED,
    HIGH
}

data class DailyBpStatus(
    val date: LocalDate,
    val level: BpLevel,
    val recordCount: Int
)

fun classifyBp(systolic: Int, diastolic: Int): BpLevel = when {
    systolic < 120 && diastolic < 80 -> BpLevel.NORMAL
    systolic < 140 && diastolic < 90 -> BpLevel.ELEVATED
    else -> BpLevel.HIGH
}

fun sortTrendRecords(records: List<BloodPressureRecord>): List<BloodPressureRecord> =
    records.sortedWith(
        compareBy<BloodPressureRecord> { it.date }
            .thenBy { it.period.ordinal }
            .thenBy { it.id }
    )

fun buildDailyStatuses(records: List<BloodPressureRecord>): List<DailyBpStatus> =
    records.groupBy { it.date }
        .map { (date, dayRecords) ->
            DailyBpStatus(
                date = date,
                level = dayRecords.maxOf { classifyBp(it.systolic, it.diastolic) },
                recordCount = dayRecords.size
            )
        }
        .sortedBy { it.date }


data class BpSummary(
    val averageSystolic: Int,
    val averageDiastolic: Int,
    val averageHeartRate: Int,
    val normalPercent: Int,
    val normalCount: Int,
    val elevatedCount: Int,
    val highCount: Int
)

data class PeriodComparison(
    val systolicChange: Int,
    val diastolicChange: Int,
    val elevatedAndHighCount: Int
)

fun summarizeRecords(records: List<BloodPressureRecord>): BpSummary? {
    if (records.isEmpty()) return null

    val levels = records.map { classifyBp(it.systolic, it.diastolic) }
    val normalCount = levels.count { it == BpLevel.NORMAL }
    return BpSummary(
        averageSystolic = records.map { it.systolic }.average().roundToInt(),
        averageDiastolic = records.map { it.diastolic }.average().roundToInt(),
        averageHeartRate = records.map { it.heartRate }.average().roundToInt(),
        normalPercent = (normalCount * 100f / records.size).roundToInt(),
        normalCount = normalCount,
        elevatedCount = levels.count { it == BpLevel.ELEVATED },
        highCount = levels.count { it == BpLevel.HIGH }
    )
}

fun comparePeriods(
    current: List<BloodPressureRecord>,
    previous: List<BloodPressureRecord>
): PeriodComparison? {
    val currentSummary = summarizeRecords(current) ?: return null
    val previousSummary = summarizeRecords(previous) ?: return null
    return PeriodComparison(
        systolicChange = currentSummary.averageSystolic - previousSummary.averageSystolic,
        diastolicChange = currentSummary.averageDiastolic - previousSummary.averageDiastolic,
        elevatedAndHighCount = currentSummary.elevatedCount + currentSummary.highCount
    )
}
