package com.bloodpressure.app.ui

import com.bloodpressure.app.data.BloodPressureRecord
import com.bloodpressure.app.data.MeasurementPeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DataInsightsTest {
    @Test
    fun classify_usesExistingThreeLevelThresholds() {
        assertEquals(BpLevel.NORMAL, classifyBp(119, 79))
        assertEquals(BpLevel.ELEVATED, classifyBp(120, 80))
        assertEquals(BpLevel.ELEVATED, classifyBp(139, 89))
        assertEquals(BpLevel.HIGH, classifyBp(140, 80))
        assertEquals(BpLevel.HIGH, classifyBp(120, 90))
    }

    @Test
    fun dailyStatus_usesMostSevereRecord() {
        val records = listOf(
            record(118, 76, "2026-06-20", "08:00"),
            record(145, 92, "2026-06-20", "20:00")
        )

        assertEquals(BpLevel.HIGH, buildDailyStatuses(records).single().level)
    }

    @Test
    fun summary_countsRecordsNotDays() {
        val summary = requireNotNull(
            summarizeRecords(
                listOf(
                    record(118, 76, "2026-06-20", "08:00"),
                    record(125, 82, "2026-06-20", "20:00"),
                    record(145, 92, "2026-06-21", "08:00")
                )
            )
        )

        assertEquals(33, summary.normalPercent)
        assertEquals(1, summary.normalCount)
        assertEquals(1, summary.elevatedCount)
        assertEquals(1, summary.highCount)
    }

    @Test
    fun comparePeriods_returnsNullWithoutPreviousRecords() {
        assertNull(comparePeriods(listOf(record(120, 80)), emptyList()))
    }

    @Test
    fun comparePeriods_returnsSignedAverageChanges() {
        val comparison = requireNotNull(
            comparePeriods(
                current = listOf(
                    record(126, 82, heartRate = 72),
                    record(130, 84, heartRate = 74)
                ),
                previous = listOf(
                    record(132, 86, heartRate = 70),
                    record(134, 88, heartRate = 72)
                )
            )
        )

        assertEquals(-5, comparison.systolicChange)
        assertEquals(-4, comparison.diastolicChange)
        assertEquals(2, comparison.elevatedAndHighCount)
    }

    private fun record(
        systolic: Int,
        diastolic: Int,
        date: String = "2026-06-20",
        time: String = "08:00",
        heartRate: Int = 72
    ) = BloodPressureRecord(
        systolic = systolic,
        diastolic = diastolic,
        heartRate = heartRate,
        date = LocalDate.parse(date),
        time = LocalTime.parse(time),
        period = MeasurementPeriod.MORNING
    )
}
