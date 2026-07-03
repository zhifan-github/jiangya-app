package com.bloodpressure.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bloodpressure.app.data.BloodPressureRecord
import com.bloodpressure.app.data.MeasurementPeriod
import com.bloodpressure.app.ui.theme.TextPrimary
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

private val DataBackground = Color(0xFFFAF8F2)
private val DataCard = Color(0xFFFFFDF9)
private val DataBorder = Color(0xFFE8E3D9)
private val DataText = Color(0xFF263A36)
private val DataMuted = Color(0xFF727B77)
private val DataTeal = Color(0xFF006F64)
private val DataNormal = Color(0xFF7FAE45)
private val DataElevated = Color(0xFFC4A43E)
private val DataHigh = Color(0xFFC75146)
private val DataNeutral = Color(0xFFEFEDE8)
private val DataMetric = Color(0xFFF3F0E9)

// Retained for the shared fullscreen and heart-rate line chart.
private val VintageGrid = DataBorder
private val VintageMuted = DataMuted
private val VintageSys = DataHigh
private val VintageDia = DataNormal
private val VintageHr = Color(0xFFB59432)
private val VintageRef130 = Color(0xFFD4886B)
private val VintageRef90 = Color(0xFF8FBC8F)

enum class TimeRange(val label: String, val days: Int) {
    WEEK("近7天", 7),
    TWO_WEEKS("近14天", 14),
    MONTH("近30天", 30),
    CUSTOM("自定义", -1),
    ALL("全部", Int.MAX_VALUE)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(
    allRecords: List<BloodPressureRecord>,
    onNavigateToHistory: () -> Unit,
    onOpenFullscreen: (chartType: String, records: List<BloodPressureRecord>) -> Unit = { _, _ -> }
) {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    var selectedRange by remember { mutableStateOf(TimeRange.WEEK) }
    var customStartDate by remember { mutableStateOf(today.minus(DatePeriod(days = 6))) }
    var customEndDate by remember { mutableStateOf(today) }
    var showCustomDatePicker by remember { mutableStateOf(false) }
    var customDatePickerStep by remember { mutableIntStateOf(0) }

    val fromDate = when (selectedRange) {
        TimeRange.WEEK -> today.minus(DatePeriod(days = 6))
        TimeRange.TWO_WEEKS -> today.minus(DatePeriod(days = 13))
        TimeRange.MONTH -> today.minus(DatePeriod(days = 29))
        TimeRange.CUSTOM -> customStartDate
        TimeRange.ALL -> allRecords.minOfOrNull { it.date } ?: today
    }
    val toDate = if (selectedRange == TimeRange.CUSTOM) customEndDate else today

    val filteredRecords = remember(selectedRange, allRecords, customStartDate, customEndDate) {
        sortTrendRecords(allRecords.filter { it.date in fromDate..toDate })
    }
    val previousRecords = remember(selectedRange, allRecords, customStartDate, customEndDate) {
        if (selectedRange == TimeRange.ALL) {
            emptyList()
        } else {
            val periodDays = (toDate.toEpochDays() - fromDate.toEpochDays() + 1).coerceAtLeast(1)
            val previousTo = fromDate.minus(DatePeriod(days = 1))
            val previousFrom = previousTo.minus(DatePeriod(days = periodDays - 1))
            allRecords.filter { it.date in previousFrom..previousTo }
        }
    }
    val summary = remember(filteredRecords) { summarizeRecords(filteredRecords) }
    val comparison = remember(filteredRecords, previousRecords) {
        comparePeriods(filteredRecords, previousRecords)
    }

    Scaffold(contentWindowInsets = WindowInsets(0.dp)) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(DataBackground)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("数据", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = DataText)

            TimeRangeSelector(
                selectedRange = selectedRange,
                onSelect = { range ->
                    selectedRange = range
                    if (range == TimeRange.CUSTOM) {
                        showCustomDatePicker = true
                        customDatePickerStep = 0
                    }
                }
            )

            if (selectedRange == TimeRange.CUSTOM) {
                CustomRangeRow(
                    startDate = customStartDate,
                    endDate = customEndDate,
                    onStartClick = {
                        customDatePickerStep = 0
                        showCustomDatePicker = true
                    },
                    onEndClick = {
                        customDatePickerStep = 1
                        showCustomDatePicker = true
                    }
                )
            }

            if (summary == null) {
                EmptyDataCard()
            } else {
                SummaryStrip(summary)
                StatusCalendar(fromDate, toDate, filteredRecords)
                ComparisonCard(selectedRange, summary, comparison)
                BloodPressureTrendCard(filteredRecords) { onOpenFullscreen("bp", allRecords) }
                HeartRateTrendCard(filteredRecords) { onOpenFullscreen("hr", allRecords) }
            }

            Spacer(modifier = Modifier.height(78.dp))
        }
    }

    if (showCustomDatePicker) {
        val initialDate = if (customDatePickerStep == 0) customStartDate else customEndDate
        val datePickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = initialDate.toEpochDays() * 86_400_000L
        )
        DatePickerDialog(
            onDismissRequest = { showCustomDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedDate = datePickerState.selectedDateMillis?.let { millis ->
                            LocalDate.fromEpochDays((millis / 86_400_000L).toInt())
                        }
                        if (customDatePickerStep == 0) {
                            selectedDate?.let { customStartDate = it }
                            customDatePickerStep = 1
                        } else {
                            selectedDate?.let { customEndDate = it }
                            if (customStartDate > customEndDate) {
                                val swap = customStartDate
                                customStartDate = customEndDate
                                customEndDate = swap
                            }
                            showCustomDatePicker = false
                        }
                    }
                ) {
                    Text(if (customDatePickerStep == 0) "下一步" else "确定", color = DataTeal)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDatePicker = false }) {
                    Text("取消", color = DataMuted)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun TimeRangeSelector(selectedRange: TimeRange, onSelect: (TimeRange) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DataNeutral, RoundedCornerShape(10.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        TimeRange.entries.forEach { range ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .background(
                        if (selectedRange == range) DataTeal else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelect(range) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    range.label,
                    color = if (selectedRange == range) Color.White else DataMuted,
                    fontSize = 11.sp,
                    fontWeight = if (selectedRange == range) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun CustomRangeRow(
    startDate: LocalDate,
    endDate: LocalDate,
    onStartClick: () -> Unit,
    onEndClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DateButton(startDate.toString(), Modifier.weight(1f), onStartClick)
        Text("至", color = DataMuted, fontSize = 12.sp)
        DateButton(endDate.toString(), Modifier.weight(1f), onEndClick)
    }
}

@Composable
private fun DateButton(text: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(44.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = DataCard,
        border = BorderStroke(1.dp, DataBorder)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = DataText, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SummaryStrip(summary: BpSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DataCard),
        border = BorderStroke(1.dp, DataBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(78.dp)) {
            SummaryCell(
                Modifier.weight(1.08f),
                "正常记录占比",
                summary.normalPercent.toString() + "%",
                DataTeal,
                Color.White
            )
            SummaryCell(
                Modifier.weight(1f),
                "平均血压",
                summary.averageSystolic.toString() + "/" + summary.averageDiastolic,
                DataCard,
                DataText
            )
            SummaryCell(
                Modifier.weight(0.86f),
                "平均心率",
                summary.averageHeartRate.toString(),
                DataCard,
                DataText,
                "bpm"
            )
        }
    }
}

@Composable
private fun SummaryCell(
    modifier: Modifier,
    label: String,
    value: String,
    background: Color,
    contentColor: Color,
    unit: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(background)
            .padding(horizontal = 10.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 10.sp, color = contentColor.copy(alpha = 0.72f))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = contentColor)
            unit?.let {
                Spacer(modifier = Modifier.width(3.dp))
                Text(it, fontSize = 8.sp, color = contentColor.copy(alpha = 0.68f))
            }
        }
    }
}

@Composable
private fun StatusCalendar(fromDate: LocalDate, toDate: LocalDate, records: List<BloodPressureRecord>) {
    val statusByDate = remember(records) { buildDailyStatuses(records).associateBy { it.date } }
    val dates = remember(fromDate, toDate) {
        (fromDate.toEpochDays()..toDate.toEpochDays()).map { LocalDate.fromEpochDays(it) }
    }
    val weeks = remember(dates) { dates.chunked(7) }

    DataCardContainer {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("血压状态日历", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DataText)
            Text(
                statusByDate.values.count { it.level == BpLevel.NORMAL }.toString() + " 天正常",
                fontSize = 11.sp,
                color = DataMuted
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val weekWidth = maxWidth
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(weeks) { _, week ->
                    Row(
                        modifier = Modifier.width(weekWidth),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        week.forEach { date ->
                            StatusDay(date, statusByDate[date], Modifier.weight(1f))
                        }
                        repeat(7 - week.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }
        }
        if (weeks.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("左右滑动查看其他日期", fontSize = 10.sp, color = DataMuted)
        }
    }
}

@Composable
private fun StatusDay(date: LocalDate, status: DailyBpStatus?, modifier: Modifier) {
    val levelColor = status?.level?.toColor()
    val weekLabels = listOf("一", "二", "三", "四", "五", "六", "日")
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(weekLabels[date.dayOfWeek.ordinal], fontSize = 9.sp, color = DataMuted)
        Spacer(modifier = Modifier.height(5.dp))
        Surface(
            modifier = Modifier.size(38.dp),
            shape = RoundedCornerShape(9.dp),
            color = levelColor?.copy(alpha = 0.20f) ?: DataNeutral
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    date.monthNumber.toString() + "/" + date.dayOfMonth,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = levelColor ?: DataMuted.copy(alpha = 0.65f)
                )
            }
        }
    }
}

@Composable
private fun ComparisonCard(selectedRange: TimeRange, summary: BpSummary, comparison: PeriodComparison?) {
    DataCardContainer {
        Text(
            if (selectedRange == TimeRange.ALL) "记录状态分布" else "与上一周期相比",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = DataText
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (selectedRange == TimeRange.ALL) {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                MetricBlock("正常", summary.normalCount.toString(), DataNormal, Modifier.weight(1f))
                MetricBlock("偏高", summary.elevatedCount.toString(), DataElevated, Modifier.weight(1f))
                MetricBlock("高", summary.highCount.toString(), DataHigh, Modifier.weight(1f))
            }
        } else if (comparison == null) {
            Box(modifier = Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.CenterStart) {
                Text("暂无对比数据", fontSize = 13.sp, color = DataMuted)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                MetricBlock(
                    "收缩压均值",
                    comparison.systolicChange.toChangeLabel(),
                    comparison.systolicChange.toChangeColor(),
                    Modifier.weight(1f)
                )
                MetricBlock(
                    "舒张压均值",
                    comparison.diastolicChange.toChangeLabel(),
                    comparison.diastolicChange.toChangeColor(),
                    Modifier.weight(1f)
                )
                MetricBlock(
                    "偏高/高记录",
                    comparison.elevatedAndHighCount.toString(),
                    if (comparison.elevatedAndHighCount == 0) DataNormal else DataHigh,
                    Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricBlock(label: String, value: String, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .background(DataMetric, RoundedCornerShape(7.dp))
            .padding(horizontal = 9.dp, vertical = 9.dp)
    ) {
        Text(label, fontSize = 9.sp, color = DataMuted, maxLines = 1)
        Spacer(modifier = Modifier.height(5.dp))
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun BloodPressureTrendCard(records: List<BloodPressureRecord>, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DataCard),
        border = BorderStroke(1.dp, DataBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("血压趋势", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DataText)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    DataLegendItem(DataNormal, "正常")
                    DataLegendItem(DataElevated, "偏高")
                    DataLegendItem(DataHigh, "高")
                }
            }
            Spacer(modifier = Modifier.height(9.dp))
            DataBpCandleChart(records, Modifier.fillMaxWidth().height(210.dp))
        }
    }
}

@Composable
private fun DataBpCandleChart(records: List<BloodPressureRecord>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (records.isEmpty()) return@Canvas
        val padLeft = 31.dp.toPx()
        val padRight = 9.dp.toPx()
        val padTop = 16.dp.toPx()
        val padBottom = 44.dp.toPx()
        val chartWidth = size.width - padLeft - padRight
        val chartHeight = size.height - padTop - padBottom
        val rawMin = records.minOf { it.diastolic }
        val rawMax = records.maxOf { it.systolic }
        val minValue = min(60f, floor(rawMin / 20f) * 20f)
        val maxValue = max(160f, ceil(rawMax / 20f) * 20f)
        val valueRange = (maxValue - minValue).coerceAtLeast(1f)
        val axisPaint = android.graphics.Paint().apply {
            color = DataMuted.copy(alpha = 0.75f).toArgb()
            textSize = 8.sp.toPx()
            isAntiAlias = true
        }
        val labelPaint = android.graphics.Paint().apply {
            color = DataMuted.copy(alpha = 0.82f).toArgb()
            textSize = 7.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        val valuePaint = android.graphics.Paint().apply {
            color = DataText.toArgb()
            textSize = 8.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
        }

        repeat(5) { index ->
            val value = minValue + valueRange * index / 4f
            val y = padTop + chartHeight * (maxValue - value) / valueRange
            drawLine(DataBorder, Offset(padLeft, y), Offset(size.width - padRight, y), 1f)
            drawContext.canvas.nativeCanvas.drawText(value.toInt().toString(), 1f, y + 3.dp.toPx(), axisPaint)
        }

        listOf(140f to DataHigh, 120f to DataTeal, 90f to DataElevated, 80f to DataNormal)
            .forEach { (value, color) ->
                if (value in minValue..maxValue) {
                    val y = padTop + chartHeight * (maxValue - value) / valueRange
                    drawLine(
                        color.copy(alpha = 0.36f),
                        Offset(padLeft, y),
                        Offset(size.width - padRight, y),
                        1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))
                    )
                }
            }

        val stepX = chartWidth / records.size
        val candleWidth = min(7.dp.toPx(), stepX * 0.30f).coerceAtLeast(2.dp.toPx())
        val labelInterval = max(1, ceil(records.size / 6f).toInt())
        records.forEachIndexed { index, record ->
            val x = padLeft + stepX * (index + 0.5f)
            val systolicY = padTop + chartHeight * (maxValue - record.systolic) / valueRange
            val diastolicY = padTop + chartHeight * (maxValue - record.diastolic) / valueRange
            val color = classifyBp(record.systolic, record.diastolic).toColor()
            drawLine(color, Offset(x, systolicY), Offset(x, diastolicY), candleWidth, cap = StrokeCap.Round)
            drawLine(
                Color.White.copy(alpha = 0.40f),
                Offset(x - candleWidth * 0.18f, systolicY + candleWidth * 0.45f),
                Offset(x - candleWidth * 0.18f, diastolicY - candleWidth * 0.45f),
                max(1f, candleWidth * 0.18f),
                cap = StrokeCap.Round
            )

            drawContext.canvas.nativeCanvas.drawText(
                record.systolic.toString(),
                x,
                systolicY - 4.dp.toPx(),
                valuePaint
            )
            drawContext.canvas.nativeCanvas.drawText(
                record.diastolic.toString(),
                x,
                diastolicY + 10.dp.toPx(),
                valuePaint
            )
            if (index % labelInterval == 0 || index == records.lastIndex) {
                val dateLabel = String.format(Locale.US, "%02d/%02d", record.date.monthNumber, record.date.dayOfMonth)
                val periodLabel = if (record.period == MeasurementPeriod.MORNING) "早" else "晚"
                drawContext.canvas.nativeCanvas.drawText(dateLabel, x, padTop + chartHeight + 24.dp.toPx(), labelPaint)
                drawContext.canvas.nativeCanvas.drawText(periodLabel, x, padTop + chartHeight + 36.dp.toPx(), labelPaint)
            }
        }
    }
}

@Composable
private fun HeartRateTrendCard(records: List<BloodPressureRecord>, onClick: () -> Unit) {
    val averageHeartRate = summarizeRecords(records)?.averageHeartRate
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DataCard),
        border = BorderStroke(1.dp, DataBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("心率趋势", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DataText)
                Text(
                    averageHeartRate?.let { "平均 " + it + " bpm" } ?: "--",
                    fontSize = 11.sp,
                    color = DataMuted
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (records.size >= 2) {
                LineChart(records, true, modifier = Modifier.fillMaxWidth().height(154.dp))
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(76.dp), contentAlignment = Alignment.Center) {
                    Text("至少需要 2 条记录显示趋势", fontSize = 12.sp, color = DataMuted)
                }
            }
        }
    }
}

@Composable
private fun DataLegendItem(color: Color, label: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.13f)) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
            Spacer(modifier = Modifier.width(3.dp))
            Text(label, fontSize = 9.sp, color = DataText)
        }
    }
}

@Composable
private fun DataCardContainer(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DataCard),
        border = BorderStroke(1.dp, DataBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(13.dp), content = content)
    }
}

@Composable
private fun EmptyDataCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DataCard),
        border = BorderStroke(1.dp, DataBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
            Text("所选时间范围内无数据", fontSize = 14.sp, color = DataMuted)
        }
    }
}

private fun BpLevel.toColor(): Color = when (this) {
    BpLevel.NORMAL -> DataNormal
    BpLevel.ELEVATED -> DataElevated
    BpLevel.HIGH -> DataHigh
}

private fun Int.toChangeLabel(): String = when {
    this < 0 -> "↓ " + abs(this)
    this > 0 -> "↑ " + this
    else -> "持平"
}

private fun Int.toChangeColor(): Color = when {
    this < 0 -> DataTeal
    this > 0 -> DataHigh
    else -> DataMuted
}

@Composable
internal fun LineChart(
    records: List<BloodPressureRecord>,
    showHr: Boolean,
    isDarkBg: Boolean = false,
    modifier: Modifier = Modifier,
    paddingLeftPx: Float? = null,
    paddingRightPx: Float? = null,
    paddingTopPx: Float? = null,
    paddingBottomPx: Float? = null
) {
    if (records.size < 2) return

    val lineColor = if (showHr) VintageHr else VintageSys
    val lineColor2 = if (showHr) Color.Transparent else VintageDia
    val gridColor = if (isDarkBg) Color(0xFF33334D) else VintageGrid
    val textColor = if (isDarkBg) Color(0xFFB0ADBE) else VintageMuted

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val density = density
        val paddingLeft = paddingLeftPx ?: with(density) { 48.dp.toPx() }
        val paddingRight = paddingRightPx ?: with(density) { 24.dp.toPx() }
        val paddingTop = paddingTopPx ?: with(density) { 24.dp.toPx() }
        val paddingBottom = paddingBottomPx ?: with(density) { 48.dp.toPx() }
        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        // Y range
        val minVal: Float
        val maxVal: Float
        if (showHr) {
            val hrVals = records.map { it.heartRate.toFloat() }
            minVal = ((hrVals.minOrNull() ?: 60f) - 10f).coerceAtLeast(40f)
            maxVal = ((hrVals.maxOrNull() ?: 100f) + 10f).coerceAtLeast(80f)
        } else {
            val allVals = records.flatMap { listOf(it.systolic.toFloat(), it.diastolic.toFloat()) }
            minVal = ((allVals.minOrNull() ?: 80f) - 10f).coerceAtLeast(40f)
            maxVal = ((allVals.maxOrNull() ?: 140f) + 10f).coerceAtLeast(100f)
        }
        val range = maxVal - minVal

        // Y-axis grid lines
        val ySteps = 4
        for (i in 0..ySteps) {
            val valY = minVal + range * i / ySteps
            val y = paddingTop + chartHeight * (1 - i.toFloat() / ySteps)
            drawLine(gridColor, Offset(paddingLeft, y), Offset(width - paddingRight, y), 0.5f)
            drawContext.canvas.nativeCanvas.drawText(
                "${valY.toInt()}", 2f, y + 5f,
                android.graphics.Paint().apply {
                    color = textColor.hashCode()
                    textSize = 22f
                }
            )
        }

        // Reference lines for BP chart
        if (!showHr) {
            val ref130Y = paddingTop + chartHeight * (maxVal - 130f) / range
            val ref90Y = paddingTop + chartHeight * (maxVal - 90f) / range
            if (130f in minVal..maxVal) {
                drawLine(VintageRef130, Offset(paddingLeft, ref130Y), Offset(width - paddingRight, ref130Y), 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))
                drawContext.canvas.nativeCanvas.drawText("130", width - paddingRight + 4f, ref130Y - 4f,
                    android.graphics.Paint().apply { color = VintageRef130.hashCode(); textSize = 18f })
            }
            if (90f in minVal..maxVal) {
                drawLine(VintageRef90, Offset(paddingLeft, ref90Y), Offset(width - paddingRight, ref90Y), 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))
                drawContext.canvas.nativeCanvas.drawText("90", width - paddingRight + 4f, ref90Y - 4f,
                    android.graphics.Paint().apply { color = VintageRef90.hashCode(); textSize = 18f })
            }
        }

        val stepX = if (records.size > 1) chartWidth / (records.size - 1) else chartWidth

        // Draw line 1 (systolic/HR) — straight angled line
        val path1 = Path()
        records.forEachIndexed { i, r ->
            val x = paddingLeft + i * stepX
            val val1 = if (showHr) r.heartRate.toFloat() else r.systolic.toFloat()
            val y = paddingTop + chartHeight * (maxVal - val1) / range
            if (i == 0) path1.moveTo(x, y)
            else path1.lineTo(x, y)
        }
        drawPath(path1, lineColor, style = Stroke(width = 2.5f))

        // Draw line 2 (diastolic) for BP chart
        if (!showHr) {
            val path2 = Path()
            records.forEachIndexed { i, r ->
                val x = paddingLeft + i * stepX
                val y = paddingTop + chartHeight * (maxVal - r.diastolic.toFloat()) / range
                if (i == 0) path2.moveTo(x, y)
                else path2.lineTo(x, y)
            }
            drawPath(path2, lineColor2, style = Stroke(width = 2.5f))
        }

        // Draw data dots and value labels
        records.forEachIndexed { i, r ->
            val x = paddingLeft + i * stepX
            val val1 = if (showHr) r.heartRate.toFloat() else r.systolic.toFloat()
            val y1 = paddingTop + chartHeight * (maxVal - val1) / range

            // Dot — filled circle with white ring
            drawCircle(Color.White, 5f, Offset(x, y1))
            drawCircle(lineColor, 3.5f, Offset(x, y1))

            // Value label
            drawContext.canvas.nativeCanvas.drawText("${val1.toInt()}", x - 10f, y1 - 10f,
                android.graphics.Paint().apply {
                    color = lineColor.hashCode()
                    textSize = 22f
                    isFakeBoldText = true
                })

            if (!showHr) {
                val y2 = paddingTop + chartHeight * (maxVal - r.diastolic.toFloat()) / range
                drawCircle(Color.White, 5f, Offset(x, y2))
                drawCircle(lineColor2, 3.5f, Offset(x, y2))
                drawContext.canvas.nativeCanvas.drawText("${r.diastolic}", x - 8f, y2 + 18f,
                    android.graphics.Paint().apply {
                        color = lineColor2.hashCode()
                        textSize = 22f
                        isFakeBoldText = true
                    })
            }
        }

        // X-axis labels — show date+period for all records when feasible
        val labelY = paddingTop + chartHeight + 16f  // 16px below chart area
        val labelInterval = when {
            records.size <= 14 -> 1       // ≤14条：每条都标
            records.size <= 28 -> 2       // 15-28条：隔一条
            records.size <= 60 -> records.size / 8  // 29-60条：分8段
            else -> records.size / 12     // >60条：分12段
        }
        records.forEachIndexed { i, r ->
            if (i % labelInterval == 0 || i == records.size - 1) {
                val x = paddingLeft + i * stepX
                val periodLabel = when (r.period) {
                    MeasurementPeriod.MORNING -> "早"
                    MeasurementPeriod.EVENING -> "晚"
                }
                val dateStr = "${r.date.monthNumber}/${r.date.dayOfMonth}$periodLabel"
                drawContext.canvas.nativeCanvas.drawText(dateStr, x - 14f, labelY,
                    android.graphics.Paint().apply {
                        color = textColor.hashCode()
                        textSize = 18f
                    })
            }
        }
    }
}

@Composable
private fun LegendItemDarkBg(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, color = Color(0xFFB0ADBE))
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, fontSize = 14.sp, color = TextPrimary)
    }
}
