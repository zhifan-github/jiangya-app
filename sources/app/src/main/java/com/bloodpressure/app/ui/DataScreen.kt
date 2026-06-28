package com.bloodpressure.app.ui

import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.bloodpressure.app.data.BloodPressureRecord
import com.bloodpressure.app.data.MeasurementPeriod
import com.bloodpressure.app.ui.theme.*
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.minus

// 复古配色
private val VintageBg = Color(0xFFFDFAF3)
private val VintageGrid = Color(0xFFECE4D5)
private val VintageText = Color(0xFF5C4033)
private val VintageMuted = Color(0xFFA09080)
private val VintageSys = Color(0xFFC75B39)      // 铁锈红 收缩压
private val VintageDia = Color(0xFF6B8E4E)      // 鼠尾草绿 舒张压
private val VintageHr = Color(0xFFC4A43E)       // 古铜金 心率
private val VintageRef130 = Color(0xFFD4886B)   // 130参考线
private val VintageRef90 = Color(0xFF8FBC8F)    // 90参考线

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

    // 自定义时间范围
    var customStartDate by remember { mutableStateOf(today.minus(kotlinx.datetime.DatePeriod(days = 7))) }
    var customEndDate by remember { mutableStateOf(today) }
    var showCustomDatePicker by remember { mutableStateOf(false) }
    var customDatePickerStep by remember { mutableStateOf(0) } // 0: select start, 1: select end

    // Calculate date range
    val fromDate = if (selectedRange.days > 0) {
        LocalDate.fromEpochDays(today.toEpochDays() - selectedRange.days)
    } else {
        // CUSTOM mode
        customStartDate
    }

    val toDate = if (selectedRange.days > 0) {
        today
    } else {
        customEndDate
    }

    // Filter records by date range
    val filteredRecords = remember(selectedRange, allRecords, customStartDate, customEndDate) {
        allRecords.filter { it.date >= fromDate && it.date <= toDate }.sortedWith(compareBy({ it.date }, { it.period }))
    }

    Scaffold(contentWindowInsets = WindowInsets(0.dp)) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(AppBackground)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ====== 时间范围选择 ======
            TimeRangeSelector(
                selectedRange = selectedRange,
                onSelect = {
                    selectedRange = it
                    if (it == TimeRange.CUSTOM) {
                        showCustomDatePicker = true
                        customDatePickerStep = 0
                    }
                }
            )

            // 自定义时间范围显示
            if (selectedRange == TimeRange.CUSTOM) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "自定义范围",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.clickable {
                                showCustomDatePicker = true
                                customDatePickerStep = 0
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceVariant
                        ) {
                            Text(
                                "${customStartDate}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                        }
                        Text("至", fontSize = 13.sp, color = TextSecondary)
                        Surface(
                            modifier = Modifier.clickable {
                                showCustomDatePicker = true
                                customDatePickerStep = 1
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceVariant
                        ) {
                            Text(
                                "${customEndDate}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // ====== 平均血压卡片 ======
            AverageCardsSection(records = filteredRecords)

            // ====== 血压趋势图 ======
            if (filteredRecords.isNotEmpty()) {
                BpTrendChart(
                    records = filteredRecords,
                    onTap = {
                        onOpenFullscreen("bp", allRecords)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ====== 心率趋势图 ======
                HeartRateTrendChart(
                    records = filteredRecords,
                    onTap = {
                        onOpenFullscreen("hr", allRecords)
                    }
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("所选时间范围内无数据", color = TextSecondary, fontSize = 15.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // 自定义日期选择器
    if (showCustomDatePicker) {
        val initialDate = if (customDatePickerStep == 0) customStartDate else customEndDate
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialDate.toEpochDays() * 86400000L
        )

        DatePickerDialog(
            onDismissRequest = { showCustomDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (customDatePickerStep == 0) {
                            // 选择开始日期
                            datePickerState.selectedDateMillis?.let { millis ->
                                val epochDay = millis / 86400000L
                                customStartDate = LocalDate.fromEpochDays(epochDay.toInt())
                            }
                            customDatePickerStep = 1
                        } else {
                            // 选择结束日期
                            datePickerState.selectedDateMillis?.let { millis ->
                                val epochDay = millis / 86400000L
                                customEndDate = LocalDate.fromEpochDays(epochDay.toInt())
                            }
                            // 确保起始日期不晚于结束日期
                            if (customStartDate > customEndDate) {
                                val tmp = customStartDate
                                customStartDate = customEndDate
                                customEndDate = tmp
                            }
                            showCustomDatePicker = false
                        }
                    }
                ) { Text(if (customDatePickerStep == 0) "下一步" else "确定", color = Primary) }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun TimeRangeSelector(
    selectedRange: TimeRange,
    onSelect: (TimeRange) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TimeRange.values().forEach { range ->
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(range) },
                shape = RoundedCornerShape(10.dp),
                color = if (selectedRange == range) Primary else SurfaceWhite,
                shadowElevation = if (selectedRange == range) 2.dp else 0.dp
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        range.label,
                        fontWeight = FontWeight.Medium,
                        color = if (selectedRange == range) Color.White else TextPrimary,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AverageCardsSection(records: List<BloodPressureRecord>) {
    val avgSystolic = if (records.isNotEmpty()) records.map { it.systolic }.average().toInt() else 0
    val avgDiastolic = if (records.isNotEmpty()) records.map { it.diastolic }.average().toInt() else 0
    val avgHeartRate = if (records.isNotEmpty()) records.map { it.heartRate }.average().toInt() else 0

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            title = "平均收缩压",
            value = if (avgSystolic > 0) "$avgSystolic" else "--",
            unit = "mmHg",
            color = Primary
        )
        StatCard(
            modifier = Modifier.weight(1f),
            title = "平均舒张压",
            value = if (avgDiastolic > 0) "$avgDiastolic" else "--",
            unit = "mmHg",
            color = Accent
        )
        StatCard(
            modifier = Modifier.weight(1f),
            title = "平均心率",
            value = if (avgHeartRate > 0) "$avgHeartRate" else "--",
            unit = "bpm",
            color = Color(0xFFE57373)
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    unit: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = VintageBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                fontSize = 11.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    value,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                if (value != "--") {
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        unit,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BpTrendChart(
    records: List<BloodPressureRecord>,
    onTap: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = VintageBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = VintageSys,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "血压趋势",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = VintageText
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "点击放大",
                    fontSize = 12.sp,
                    color = VintageMuted
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Chart
            LineChart(
                records = records,
                showHr = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
        }
    }
}

@Composable
private fun HeartRateTrendChart(
    records: List<BloodPressureRecord>,
    onTap: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = VintageBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Timeline,
                    contentDescription = null,
                    tint = VintageHr,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "心率趋势",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = VintageText
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "点击放大",
                    fontSize = 12.sp,
                    color = VintageMuted
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            LineChart(
                records = records,
                showHr = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
        }
    }
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
