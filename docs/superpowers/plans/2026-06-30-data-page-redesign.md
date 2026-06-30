# Data Page Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the data page around blood-pressure status and period comparison while preserving all records, date filters, heart-rate chart, and fullscreen navigation.

**Architecture:** Move classification and aggregation into a small pure-Kotlin `DataInsights` module so the calculations can be unit tested without Compose. Keep screen state and rendering in `DataScreen.kt`, and add a page-specific candlestick chart without changing the existing fullscreen `LineChart`.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, kotlinx-datetime, JUnit 4, Gradle.

---

### Task 1: Add Tested Blood-Pressure Insight Calculations

**Files:**
- Modify: `sources/app/build.gradle.kts`
- Create: `sources/app/src/main/java/com/bloodpressure/app/ui/DataInsights.kt`
- Create: `sources/app/src/test/java/com/bloodpressure/app/ui/DataInsightsTest.kt`

- [ ] **Step 1: Add the JUnit dependency**

Add this dependency to `sources/app/build.gradle.kts`:

```kotlin
testImplementation("junit:junit:4.13.2")
```

- [ ] **Step 2: Write failing classification and daily-status tests**

Create `DataInsightsTest.kt` with records made from real `BloodPressureRecord` objects. Cover:

```kotlin
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

@Test
fun classify_uses_existing_three_level_thresholds() {
    assertEquals(BpLevel.NORMAL, classifyBp(119, 79))
    assertEquals(BpLevel.ELEVATED, classifyBp(120, 80))
    assertEquals(BpLevel.ELEVATED, classifyBp(139, 89))
    assertEquals(BpLevel.HIGH, classifyBp(140, 80))
    assertEquals(BpLevel.HIGH, classifyBp(120, 90))
}

@Test
fun dailyStatus_uses_most_severe_record() {
    val records = listOf(
        record(118, 76, "2026-06-20", "08:00"),
        record(145, 92, "2026-06-20", "20:00")
    )
    assertEquals(BpLevel.HIGH, buildDailyStatuses(records).single().level)
}
```

- [ ] **Step 3: Run tests and verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.bloodpressure.app.ui.DataInsightsTest"
```

Expected: compilation fails because `BpLevel`, `classifyBp`, and `buildDailyStatuses` do not exist.

- [ ] **Step 4: Implement the minimal classification API**

Create `DataInsights.kt` with:

```kotlin
enum class BpLevel { NORMAL, ELEVATED, HIGH }

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
```

- [ ] **Step 5: Run tests and verify GREEN**

Run the same `:app:testDebugUnitTest` command.

Expected: all classification and daily-status tests pass.

- [ ] **Step 6: Write failing summary and comparison tests**

Add tests for:

```kotlin
@Test
fun summary_counts_records_not_days() {
    val summary = requireNotNull(summarizeRecords(
        listOf(
            record(118, 76, "2026-06-20", "08:00"),
            record(125, 82, "2026-06-20", "20:00"),
            record(145, 92, "2026-06-21", "08:00")
        )
    ))
    assertEquals(33, summary.normalPercent)
    assertEquals(1, summary.normalCount)
    assertEquals(1, summary.elevatedCount)
    assertEquals(1, summary.highCount)
}

@Test
fun comparePeriods_returns_null_without_previous_records() {
    assertNull(comparePeriods(listOf(record(120, 80)), emptyList()))
}
```

Also cover signed average systolic and diastolic changes when both periods have records.

- [ ] **Step 7: Run tests and verify RED**

Expected: compilation fails because `summarizeRecords` and `comparePeriods` do not exist.

- [ ] **Step 8: Implement summaries and period comparison**

Add:

```kotlin
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
```

`summarizeRecords` returns `null` for an empty list, rounds averages to integers, and calculates `normalPercent` from record count. `comparePeriods` returns `null` when either list is empty and otherwise subtracts previous averages from current averages.

- [ ] **Step 9: Run the complete unit-test class**

Expected: all `DataInsightsTest` tests pass.

- [ ] **Step 10: Commit the calculation layer**

```powershell
git add sources/app/build.gradle.kts sources/app/src/main/java/com/bloodpressure/app/ui/DataInsights.kt sources/app/src/test/java/com/bloodpressure/app/ui/DataInsightsTest.kt
git commit -m "feat: add tested blood pressure insights"
```

### Task 2: Rebuild the Data Page Structure

**Files:**
- Modify: `sources/app/src/main/java/com/bloodpressure/app/ui/DataScreen.kt`

- [ ] **Step 1: Define page-local design tokens**

Use the approved palette:

```kotlin
private val DataBackground = Color(0xFFFAF8F2)
private val DataCard = Color(0xFFFFFDF9)
private val DataBorder = Color(0xFFE8E3D9)
private val DataTeal = Color(0xFF006F64)
private val DataNormal = Color(0xFF7FAE45)
private val DataElevated = Color(0xFFC4A43E)
private val DataHigh = Color(0xFFC75146)
```

- [ ] **Step 2: Correct range calculation without changing stored data**

For `ALL`, use the earliest date present in `allRecords` instead of subtracting `Int.MAX_VALUE`. For finite/custom ranges, retain current filtering. Derive the previous period immediately before the current period using the same inclusive day count.

- [ ] **Step 3: Replace the page hierarchy**

Render, in order:

```text
数据
时间范围分段选择
正常记录占比 / 平均血压 / 平均心率
血压状态日历
与上一周期相比（全部范围时显示状态数量）
血压趋势
心率趋势
```

Use `13-14sp` body text, `15-16sp` section titles, `22-26sp` key values, 14dp-or-smaller card radii, and 44dp minimum interactive height.

- [ ] **Step 4: Implement the status summary strip**

Use one continuous three-column surface. The first cell uses `DataTeal` with white text; the other cells use `DataCard`. Show `--` when the current period is empty rather than fabricated zero values.

- [ ] **Step 5: Implement the status calendar**

Generate every date in the selected interval. Dates with no records use a neutral surface; recorded dates use the most severe daily `BpLevel`. Display 7 dates per week and place week groups in a `LazyRow` for ranges longer than seven days.

- [ ] **Step 6: Implement comparison and empty states**

Finite/custom ranges show average systolic change, average diastolic change, and elevated/high record count. If the previous period is empty, show `暂无对比数据`. `ALL` shows normal/elevated/high totals instead. If the current range is empty, keep the title and range selector and show only `所选时间范围内无数据`.

- [ ] **Step 7: Preserve navigation behavior**

Keep `onOpenFullscreen("bp", allRecords)` and `onOpenFullscreen("hr", allRecords)` unchanged. Do not touch the database, DAO, record model, or save callbacks.

- [ ] **Step 8: Build after the structural rewrite**

Run:

```powershell
.\gradlew.bat :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 9: Commit the page structure**

```powershell
git add sources/app/src/main/java/com/bloodpressure/app/ui/DataScreen.kt
git commit -m "feat: redesign data insights layout"
```

### Task 3: Add the Approved Candlestick Trend Visualization

**Files:**
- Modify: `sources/app/src/main/java/com/bloodpressure/app/ui/DataScreen.kt`

- [ ] **Step 1: Add `DataBpCandleChart`**

Draw one vertical candle per record from systolic to diastolic. Use `classifyBp` for candle color. The x-axis shows actual record date and time, with adaptive label intervals for longer ranges.

- [ ] **Step 2: Add chart references and legend**

Place `正常 / 偏高 / 高` in the card header’s upper-right corner. Draw neutral grid lines and dashed reference lines at 140, 120, 90, and 80, matching the home chart’s semantics.

- [ ] **Step 3: Keep the heart-rate chart and fullscreen chart intact**

Only replace the inline blood-pressure chart. Continue using `LineChart(showHr = true)` for heart rate and retain `LineChart` for existing fullscreen rendering.

- [ ] **Step 4: Build and run all unit tests**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Expected: unit tests pass and build completes successfully.

- [ ] **Step 5: Commit the chart**

```powershell
git add sources/app/src/main/java/com/bloodpressure/app/ui/DataScreen.kt
git commit -m "feat: add data page candlestick chart"
```

### Task 4: Device Verification

**Files:**
- No source-file changes expected.

- [ ] **Step 1: Install the debug build**

```powershell
.\gradlew.bat :app:installDebug
```

Expected: `Installed on 1 device` and `BUILD SUCCESSFUL`.

- [ ] **Step 2: Verify the approved requirements**

On the connected Xiaomi 14 Ultra, verify:

```text
No overlapping title, summary values, date labels, or legend at approximately 411dp width.
All five time ranges and custom dates remain selectable.
Daily colors match the most severe record for each day.
No-data and no-previous-period states contain no fake values.
Blood-pressure and heart-rate cards still open fullscreen.
Existing blood-pressure record count and contents are unchanged.
```

- [ ] **Step 3: Review the final diff**

Run:

```powershell
git status --short
git diff HEAD~3 -- sources/app/build.gradle.kts sources/app/src/main/java/com/bloodpressure/app/ui/DataInsights.kt sources/app/src/main/java/com/bloodpressure/app/ui/DataScreen.kt sources/app/src/test/java/com/bloodpressure/app/ui/DataInsightsTest.kt
```

Expected: only the planned files contain product changes.
