package com.bloodpressure.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bloodpressure.app.data.BloodPressureRecord
import com.bloodpressure.app.data.Medication
import com.bloodpressure.app.data.MedicationRecord
import com.bloodpressure.app.ui.theme.*
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val HomeBg = Color(0xFFFAF8F2)
private val HomeCard = Color(0xFFFFFFFF)
private val HomeCardBorder = Color(0xFFEDE7DD)
private val HomeMutedPanel = Color(0xFFF7F3EA)
private val HeroTeal = Color(0xFF006F64)
private val HeroTealDark = Color(0xFF00584F)
private val HeroTealLight = Color(0xFF0E8A7E)
private val WarmRed = Color(0xFFD84A36)
private val MintCircle = Color(0xFFD2EEE8)
private val LeafCircle = Color(0xFFDDEDBB)
private val VintageBg = Color(0xFFFFFBF2)
private val VintageGrid = Color(0xFFE7E4DC)
private val VintageText = Color(0xFF1E2224)
private val VintageMuted = Color(0xFF747A7E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    todayRecords: List<BloodPressureRecord>,
    todayMedRecords: List<MedicationRecord>,
    medications: List<Medication>,
    recentRecords: List<BloodPressureRecord> = emptyList(),
    showSaveSuccess: Boolean = false,
    onSaveSuccessConsumed: () -> Unit = {},
    onAddBloodPressure: () -> Unit,
    onAddMedication: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit = {}
) {
    // Use the most recent record for the greeting card display
    val latestRecord = todayRecords.maxByOrNull { it.time }

    val snackbarHostState = remember { SnackbarHostState() }

    // 固定近7天，并按真实记录时间排序
    val filteredRecords = remember(recentRecords) {
        if (recentRecords.isEmpty()) return@remember emptyList<BloodPressureRecord>()
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val fromDate = LocalDate.fromEpochDays(today.toEpochDays() - 7)
        recentRecords
            .filter { it.date >= fromDate && it.date <= today }
            .sortedWith(compareBy<BloodPressureRecord> { it.date }.thenBy { it.time })
    }


    LaunchedEffect(showSaveSuccess) {
        if (showSaveSuccess) {
            onSaveSuccessConsumed()
            snackbarHostState.showSnackbar(
                message = "保存成功",
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(HomeBg)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 10.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ====== 顶部血压大卡片 ======
            BpGreetingCard(
                record = latestRecord,
                onClick = onAddBloodPressure,
                onNavigateToHistory = onNavigateToHistory,
                onNavigateToSettings = onNavigateToSettings
            )

            // ====== 今日用药 ======
            MedicationSection(
                medications = medications,
                todayMedRecords = todayMedRecords,
                onClick = onAddMedication
            )

            // ====== 快捷操作 ======
            QuickActionsRow(
                medications = medications,
                todayMedRecords = todayMedRecords,
                onAddBloodPressure = onAddBloodPressure,
                onAddMedication = onAddMedication
            )

            // ====== 血压趋势柱状图 ======
            if (filteredRecords.isNotEmpty()) {
                BpBarChart(
                    records = filteredRecords,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// ====== 顶部血压状态卡 ======
@Composable
private fun BpGreetingCard(
    record: BloodPressureRecord?,
    onClick: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit = {}
) {
    val statusLabel = record?.let { getBpHeroStatusLabel(it.systolic, it.diastolic) } ?: "待记录"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(listOf(HeroTealLight, HeroTealDark)),
                    RoundedCornerShape(18.dp)
                )
                .padding(16.dp)
        ) {
            Column {
                Text("最近一次血压", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "${record?.systolic ?: "--"}/${record?.diastolic ?: "--"}",
                        color = Color.White,
                        fontSize = 43.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("mmHg", color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(bottom = 7.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "◷ ${record?.time?.let { "${it.hour.toString().padStart(2, '0')}:${it.minute.toString().padStart(2, '0')}" } ?: "--:--"}",
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(13.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.30f)))
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(Color.White.copy(alpha = 0.13f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.FavoriteBorder, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(record?.heartRate?.toString() ?: "--", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("次/分", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
                            }
                            Text("心率", color = Color.White.copy(alpha = 0.88f), fontSize = 12.sp)
                        }
                    }
                    Surface(
                        modifier = Modifier.clickable { onNavigateToHistory() },
                        shape = RoundedCornerShape(24.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.36f))
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Description, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("查看历史记录", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            Box(modifier = Modifier.align(Alignment.TopEnd), contentAlignment = Alignment.TopEnd) {
                IconButton(onClick = onNavigateToSettings, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Outlined.Notifications, contentDescription = "提醒", tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(WarmRed).align(Alignment.TopEnd))
            }

            Surface(
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 6.dp, bottom = 64.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFFEAF3C9),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.55f))
            ) {
                Text(statusLabel, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp), color = Color(0xFF183F2B), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
// ====== 今日用药区域 ======
@Composable
private fun MedicationSection(medications: List<Medication>, todayMedRecords: List<MedicationRecord>, onClick: () -> Unit) {
    val activeMedications = medications.filter { it.isActive }
    val completedCount = activeMedications.count { med -> todayMedRecords.any { it.medicationId == med.id && it.taken } }
    val nextMedication = activeMedications.firstOrNull { med -> todayMedRecords.none { it.medicationId == med.id && it.taken } } ?: activeMedications.firstOrNull()

    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF8)), border = BorderStroke(1.dp, Color(0xFFF2D8CF)), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFFBE8DF)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.FavoriteBorder, contentDescription = null, tint = WarmRed, modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text("今日用药", color = WarmRed, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(nextMedication?.let { "${it.name} ${it.dosage}" } ?: "暂无用药", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(if (activeMedications.isEmpty()) "点击添加" else "已完成 $completedCount/${activeMedications.size}", color = Color(0xFF55595C), fontSize = 13.sp)
                }
            }
            Surface(shape = RoundedCornerShape(22.dp), color = Color.Transparent, border = BorderStroke(1.dp, WarmRed)) {
                Text("去打卡", modifier = Modifier.padding(horizontal = 17.dp, vertical = 8.dp), color = WarmRed, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ====== 快捷操作按钮行 ======
@Composable
private fun QuickActionsRow(medications: List<Medication>, todayMedRecords: List<MedicationRecord>, onAddBloodPressure: () -> Unit, onAddMedication: () -> Unit) {
    val activeMedications = medications.filter { it.isActive }
    val completedCount = activeMedications.count { med -> todayMedRecords.any { it.medicationId == med.id && it.taken } }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = HomeCard), border = BorderStroke(1.dp, HomeCardBorder), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            BigActionButton(modifier = Modifier.weight(1f), icon = Icons.Default.Add, title = "记录血压", subtitle = "快速录入", iconBg = MintCircle, iconColor = HeroTeal, onClick = onAddBloodPressure)
            Box(modifier = Modifier.width(1.dp).height(58.dp).background(HomeCardBorder))
            BigActionButton(modifier = Modifier.weight(1f), icon = Icons.Outlined.Description, title = "用药打卡", subtitle = if (activeMedications.isEmpty()) "今日 0/0" else "今日 $completedCount/${activeMedications.size}", iconBg = LeafCircle, iconColor = Color(0xFF5A8B2B), onClick = onAddMedication)
        }
    }
}

@Composable
private fun BigActionButton(modifier: Modifier = Modifier, icon: ImageVector, title: String, subtitle: String, iconBg: Color, iconColor: Color, onClick: () -> Unit) {
    Row(modifier = modifier.clickable { onClick() }.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(52.dp).clip(CircleShape).background(iconBg), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(26.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(3.dp))
            Text(subtitle, color = Color(0xFF565B60), fontSize = 13.sp)
        }
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(21.dp))
    }
}

// ====== 血压趋势柱状图 ======
@Composable
private fun BpBarChart(
    records: List<BloodPressureRecord>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = HomeCard),
        border = BorderStroke(1.dp, HomeCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "近7天血压趋势",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = VintageText
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(color = CandleNormal, label = "正常")
                    LegendItem(color = CandleElevated, label = "偏高")
                    LegendItem(color = CandleHigh, label = "高")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(216.dp)
            ) {
                if (records.isEmpty()) return@Canvas

                val width = size.width
                val height = size.height
                val padL = 34f
                val padR = 8f
                val padT = 12f
                val padB = 42f
                val cw = width - padL - padR
                val ch = height - padT - padB
                val maxV = 160f
                val minV = 0f
                val range = maxV - minV
                val axisValues = listOf(0, 40, 80, 120, 160)

                axisValues.forEach { value ->
                    val y = padT + ch * (maxV - value) / range
                    drawLine(
                        color = VintageGrid.copy(alpha = if (value == 0) 0.95f else 0.72f),
                        start = Offset(padL, y),
                        end = Offset(width - padR, y),
                        strokeWidth = if (value == 0) 1.1f else 0.8f
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        value.toString(),
                        0f,
                        y + 5f,
                        android.graphics.Paint().apply {
                            color = VintageMuted.toArgb()
                            textSize = 18f
                            isAntiAlias = true
                        }
                    )
                }

                listOf(
                    140f to CandleHigh,
                    120f to HeroTeal,
                    90f to CandleElevated,
                    80f to Color(0xFF7FAE45)
                ).forEach { (value, color) ->
                    val y = padT + ch * (maxV - value) / range
                    drawLine(
                        color = color.copy(alpha = if (value == 120f || value == 80f) 0.40f else 0.30f),
                        start = Offset(padL, y),
                        end = Offset(width - padR, y),
                        strokeWidth = if (value == 120f || value == 80f) 1.2f else 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 7f), 0f)
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        value.toInt().toString(),
                        width - padR - 24f,
                        y - 4f,
                        android.graphics.Paint().apply {
                            this.color = color.toArgb()
                            textSize = 14f
                            isFakeBoldText = true
                            isAntiAlias = true
                        }
                    )
                }

                val barCount = records.size
                val step = cw / barCount
                val barW = (step * 0.48f).coerceIn(16f, 34f)

                records.forEachIndexed { i, r ->
                    val cx = padL + step * i + step * 0.5f
                    val barColor = getBpColor(r.systolic, r.diastolic)
                    val sysTop = padT + ch * (maxV - r.systolic.coerceAtMost(160)) / range
                    val diaBot = padT + ch * (maxV - r.diastolic.coerceAtLeast(0)) / range
                    val candleH = (diaBot - sysTop).coerceAtLeast(4f)

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(cx - barW / 2, sysTop),
                        size = Size(barW, candleH),
                        cornerRadius = CornerRadius(7f, 7f)
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.10f),
                        topLeft = Offset(cx - barW / 2 + barW * 0.62f, sysTop + 2f),
                        size = Size(barW * 0.20f, (candleH - 4f).coerceAtLeast(2f)),
                        cornerRadius = CornerRadius(5f, 5f)
                    )

                    drawContext.canvas.nativeCanvas.drawText(
                        r.systolic.toString(),
                        cx - 13f,
                        sysTop - 7f,
                        android.graphics.Paint().apply {
                            color = VintageText.toArgb()
                            textSize = 18f
                            isFakeBoldText = true
                            isAntiAlias = true
                        }
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        r.diastolic.toString(),
                        cx - 12f,
                        diaBot + 18f,
                        android.graphics.Paint().apply {
                            color = Color.White.toArgb()
                            textSize = 18f
                            isFakeBoldText = true
                            isAntiAlias = true
                        }
                    )

                    val dateLabel = "${r.date.monthNumber}/${r.date.dayOfMonth}"
                    val timeLabel = "${r.time.hour.toString().padStart(2, '0')}:${r.time.minute.toString().padStart(2, '0')}"
                    drawContext.canvas.nativeCanvas.drawText(
                        dateLabel,
                        cx - 13f,
                        height - 17f,
                        android.graphics.Paint().apply {
                            color = VintageMuted.toArgb()
                            textSize = 16f
                            isAntiAlias = true
                        }
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        timeLabel,
                        cx - 18f,
                        height - 2f,
                        android.graphics.Paint().apply {
                            color = VintageMuted.copy(alpha = 0.78f).toArgb()
                            textSize = 13f
                            isAntiAlias = true
                        }
                    )
                }
            }
        }
    }
}
@Composable
private fun LegendItem(color: Color, label: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.14f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(label, fontSize = 12.sp, color = VintageText)
        }
    }
}
// 复古蜡烛图配色
private val CandleNormal = Color(0xFF7D9D5C)
private val CandleElevated = Color(0xFFC4A43E)
private val CandleHigh = Color(0xFFC75B39)

private fun getBpHeroStatusLabel(systolic: Int, diastolic: Int): String = when {
    systolic < 140 && diastolic < 90 -> "稳定"
    else -> "偏高"
}

private fun getBpColor(systolic: Int, diastolic: Int): Color = when {
    systolic < 120 && diastolic < 80 -> CandleNormal
    systolic < 140 && diastolic < 90 -> CandleElevated
    else -> CandleHigh
}
