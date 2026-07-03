package com.bloodpressure.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bloodpressure.app.data.AppDatabase
import com.bloodpressure.app.data.BloodPressureRecord
import com.bloodpressure.app.data.MeasurementPeriod
import com.bloodpressure.app.data.Medication
import com.bloodpressure.app.data.MedicationRecord
import com.bloodpressure.app.data.TrainingPlan
import com.bloodpressure.app.data.TrainingPlanPrefs
import com.bloodpressure.app.data.TrainingRecord
import com.bloodpressure.app.ui.HomeScreen
import com.bloodpressure.app.ui.BloodPressureInputScreen
import com.bloodpressure.app.ui.MedicationCheckInScreen
import com.bloodpressure.app.ui.HistoryScreen
import com.bloodpressure.app.ui.SettingsScreen
import com.bloodpressure.app.ui.DataScreen
import com.bloodpressure.app.ui.TimeRange
import com.bloodpressure.app.ui.SportsScreen
import com.bloodpressure.app.ui.CustomTrainingPlanScreen
import com.bloodpressure.app.ui.TrainingPlanSelectionScreen
import com.bloodpressure.app.ui.TrainingScreen
import com.bloodpressure.app.ui.LineChart
import com.bloodpressure.app.ui.theme.BloodPressureAppTheme
import com.bloodpressure.app.ui.theme.*
import com.bloodpressure.app.reminder.ReminderPreferences
import com.bloodpressure.app.reminder.ReminderScheduler
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.minus

private data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String,
    val customIcon: (@Composable (Color) -> Unit)? = null
)

// 首页图标
@Composable
private fun HomeLineIcon(tint: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val strokeW = 1.8f
        val roof = Path().apply {
            moveTo(w * 0.16f, h * 0.48f)
            lineTo(w * 0.50f, h * 0.18f)
            lineTo(w * 0.84f, h * 0.48f)
        }
        drawPath(roof, color = tint, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
        val body = Path().apply {
            moveTo(w * 0.26f, h * 0.46f)
            lineTo(w * 0.26f, h * 0.82f)
            lineTo(w * 0.42f, h * 0.82f)
            lineTo(w * 0.42f, h * 0.62f)
            lineTo(w * 0.58f, h * 0.62f)
            lineTo(w * 0.58f, h * 0.82f)
            lineTo(w * 0.74f, h * 0.82f)
            lineTo(w * 0.74f, h * 0.46f)
        }
        drawPath(body, color = tint, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

// 数据图标
@Composable
private fun BarChartIcon(tint: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val strokeW = 1.8f
        drawLine(
            color = tint.copy(alpha = 0.45f),
            start = Offset(w * 0.16f, h * 0.84f),
            end = Offset(w * 0.86f, h * 0.84f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        val barWidth = w * 0.12f
        val bars = listOf(
            Triple(w * 0.26f, h * 0.58f, h * 0.26f),
            Triple(w * 0.46f, h * 0.42f, h * 0.42f),
            Triple(w * 0.66f, h * 0.26f, h * 0.58f)
        )
        bars.forEach { (x, y, height) ->
            drawRoundRect(
                color = tint,
                topLeft = Offset(x, y),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(3f, 3f)
            )
        }
    }
}

// 运动训练图标
@Composable
private fun GripTrainingIcon(tint: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val strokeW = 1.8f
        drawCircle(
            color = tint,
            radius = w * 0.14f,
            center = Offset(w * 0.5f, h * 0.23f),
            style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawLine(tint, Offset(w * 0.5f, h * 0.38f), Offset(w * 0.5f, h * 0.66f), strokeW, cap = StrokeCap.Round)
        val arms = Path().apply {
            moveTo(w * 0.26f, h * 0.43f)
            quadraticBezierTo(w * 0.38f, h * 0.34f, w * 0.5f, h * 0.43f)
            quadraticBezierTo(w * 0.62f, h * 0.34f, w * 0.74f, h * 0.43f)
        }
        drawPath(arms, color = tint, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(tint, Offset(w * 0.5f, h * 0.66f), Offset(w * 0.34f, h * 0.86f), strokeW, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.5f, h * 0.66f), Offset(w * 0.66f, h * 0.86f), strokeW, cap = StrokeCap.Round)
        drawOval(
            color = tint.copy(alpha = 0.65f),
            topLeft = Offset(w * 0.2f, h * 0.08f),
            size = Size(w * 0.6f, h * 0.26f),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )
    }
}
class MainActivity : ComponentActivity() {

    private val db by lazy { AppDatabase.getDatabase(this) }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            BloodPressureAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation(
                        db = db,
                        lifecycleScope = lifecycleScope,
                        onExitApp = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun MainNavigation(
    db: AppDatabase,
    lifecycleScope: kotlinx.coroutines.CoroutineScope,
    onExitApp: () -> Unit
) {
    var currentScreen by remember { mutableStateOf("home") }
    var selectedTab by remember { mutableStateOf(0) }

    var todayRecords by remember { mutableStateOf<List<BloodPressureRecord>>(emptyList()) }
    var todayMedRecords by remember { mutableStateOf<List<MedicationRecord>>(emptyList()) }
    var medications by remember { mutableStateOf<List<Medication>>(emptyList()) }
    var allBpRecords by remember { mutableStateOf<List<BloodPressureRecord>>(emptyList()) }
    var allMedRecords by remember { mutableStateOf<List<MedicationRecord>>(emptyList()) }
    var dataRecords by remember { mutableStateOf<List<BloodPressureRecord>>(emptyList()) }
    var allRecordsForHome by remember { mutableStateOf<List<BloodPressureRecord>>(emptyList()) }

    var showExitDialog by remember { mutableStateOf(false) }
    var showSaveSuccess by remember { mutableStateOf(false) }
    var medRefreshTrigger by remember { mutableStateOf(0) }
    var sportsRefreshTrigger by remember { mutableStateOf(0) }
    var bpSavedTrigger by remember { mutableStateOf(0) }
    var showFullscreenChart by remember { mutableStateOf(false) }
    var fullscreenChartType by remember { mutableStateOf("bp") }
    var fullscreenRecords by remember { mutableStateOf<List<BloodPressureRecord>>(emptyList()) }

    // Training plan - load once
    val context = LocalContext.current
    var showPlanSelection by remember { mutableStateOf(false) }
    var showCustomPlan by remember { mutableStateOf(false) }
    var currentTrainingPlan by remember {
        mutableStateOf(
            com.bloodpressure.app.data.TrainingPlanPrefs.loadPlan(context)
        )
    }

    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    // Tab screens and sub-screens
    val tabScreens = listOf("home", "data", "sports")
    val subScreens = listOf("input_bp", "medication", "history", "settings", "training", "plan_selection", "custom_plan")

    // Helper to check if current screen is a tab
    val isTabScreen = currentScreen in tabScreens

    // Sync selectedTab with currentScreen
    LaunchedEffect(currentScreen) {
        selectedTab = when (currentScreen) {
            "home" -> 0
            "data" -> 1
            "sports" -> 2
            else -> selectedTab // keep current tab when on sub-screens (bottom bar not shown)
        }
    }

    // Bottom navigation items
    val navItems = listOf(
        BottomNavItem("首页", Icons.Default.Home, "home", customIcon = { HomeLineIcon(it) }),
        BottomNavItem("数据", Icons.Default.Home, "data", customIcon = { BarChartIcon(it) }),
        BottomNavItem("运动", Icons.Default.Home, "sports", customIcon = { GripTrainingIcon(it) })
    )

    BackHandler {
        when {
            showPlanSelection -> { showPlanSelection = false }
            showCustomPlan -> { showCustomPlan = false }
            currentScreen in subScreens -> currentScreen = "home"
            currentScreen == "home" -> showExitDialog = true
            currentScreen == "data" -> currentScreen = "home"
            currentScreen == "sports" -> currentScreen = "home"
        }
    }

    LaunchedEffect(Unit, medRefreshTrigger) {
        medications = db.medicationDao().getActiveMedications()
        if (medications.isEmpty()) {
            db.medicationDao().insert(Medication(name = "降压片A", dosage = "5mg"))
            db.medicationDao().insert(Medication(name = "降压片B", dosage = "10mg"))
            medications = db.medicationDao().getActiveMedications()
        }
    }

    LaunchedEffect(currentScreen, medRefreshTrigger, bpSavedTrigger) {
        when {
            currentScreen == "home" || currentScreen == "medication" -> {
                todayRecords = db.bloodPressureDao().getByDate(today)
                todayMedRecords = db.medicationRecordDao().getByDate(today)
                val fromDate = LocalDate.fromEpochDays(today.toEpochDays() - 7)
                allRecordsForHome = db.bloodPressureDao().getByDateRange(fromDate, today)
            }
            currentScreen == "history" -> {
                allBpRecords = db.bloodPressureDao().getRecent(30)
                allMedRecords = db.medicationRecordDao().getRecent(30)
            }
            currentScreen == "data" -> {
                dataRecords = db.bloodPressureDao().getAll()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        bottomBar = {
            if (isTabScreen) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    color = SurfaceWhite,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        navItems.forEachIndexed { index, item ->
                            val selected = selectedTab == index
                            val bgColor = if (selected) Primary.copy(alpha = 0.10f) else Color.Transparent
                            val contentColor = if (selected) Primary else TextSecondary

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(bgColor)
                                    .clickable {
                                        selectedTab = index
                                        currentScreen = item.route
                                    }
                                    .padding(vertical = 5.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(26.dp)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (selected) Primary else Color.Transparent)
                                )
                                Spacer(modifier = Modifier.height(5.dp))
                                if (item.customIcon != null) {
                                    item.customIcon.invoke(contentColor)
                                } else {
                                    Icon(
                                        item.icon,
                                        contentDescription = item.label,
                                        tint = contentColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    item.label,
                                    color = contentColor,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
    CurrentScreenContent(
        currentScreen = currentScreen,
        todayRecords = todayRecords,
        todayMedRecords = todayMedRecords,
        medications = medications,
        allBpRecords = allBpRecords,
        allMedRecords = allMedRecords,
        dataRecords = dataRecords,
        allRecordsForHome = allRecordsForHome,
        showSaveSuccess = showSaveSuccess,
        onSaveSuccessConsumed = { showSaveSuccess = false },
                onAddBloodPressure = { currentScreen = "input_bp" },
                onAddMedication = { currentScreen = "medication" },
                onNavigateToHistory = { currentScreen = "history" },
                onNavigateToSettings = { currentScreen = "settings" },
                onStartTraining = { currentScreen = "training" },
                onBackToSports = { currentScreen = "sports" },
                onMedicationChanged = { medRefreshTrigger++ },
                onSportsChanged = { sportsRefreshTrigger++ },
                onOpenFullscreen = { type, records ->
                    fullscreenChartType = type
                    fullscreenRecords = records
                    showFullscreenChart = true
                },
                sportsRefreshTrigger = sportsRefreshTrigger,
                currentTrainingPlan = currentTrainingPlan,
                onShowPlanSelection = { currentScreen = "plan_selection" },
                onShowCustomPlan = { currentScreen = "custom_plan" },
                onBackToPlanSelection = { currentScreen = "plan_selection" },
                onTrainingPlanSelected = { plan ->
                    currentTrainingPlan = plan
                    lifecycleScope.launch {
                        TrainingPlanPrefs.savePlan(context, plan)
                    }
                    currentScreen = "sports"
                },
                onSaveBloodPressure = { systolic: Int, diastolic: Int, heartRate: Int, date: LocalDate, period: MeasurementPeriod, medicationTaken: Boolean ->
                    lifecycleScope.launch { val activity = this
                        db.bloodPressureDao().insert(
                            BloodPressureRecord(
                                systolic = systolic,
                                diastolic = diastolic,
                                heartRate = heartRate,
                                date = date,
                                time = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time,
                                period = period,
                                medicationTaken = medicationTaken
                            )
                        )
                        showSaveSuccess = true
                        bpSavedTrigger++
                        currentScreen = "home"
                    }
                },
                onBack = { currentScreen = "home" },
                db = db,
                lifecycleScope = lifecycleScope,
                today = today
            )
        }
    }

    // 退出确认对话框
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("退出应用") },
            text = { Text("确定要退出降压吧吗？") },
            dismissButton = {
                TextButton(onClick = { onExitApp() }) {
                    Text("退出")
                }
            },
            confirmButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 全屏图表覆盖层 — 在 Scaffold 上方渲染
    if (showFullscreenChart) {
        val ctxActivity = LocalContext.current as? android.app.Activity
        var fsRange by remember { mutableStateOf<TimeRange>(TimeRange.WEEK) }

        // 过滤全屏显示的数据
        val fsFromDate = if (fsRange.days > 0) {
            LocalDate.fromEpochDays(today.toEpochDays() - fsRange.days)
        } else {
            fullscreenRecords.minOfOrNull { it.date } ?: today
        }
        val fsRecords = remember(fsRange, fullscreenRecords) {
            fullscreenRecords.filter { it.date >= fsFromDate }.sortedWith(compareBy({ it.date }, { it.period }))
        }

        // 统计数据
        val avgSys = if (fsRecords.isNotEmpty()) fsRecords.map { it.systolic }.average().toInt() else 0
        val avgDia = if (fsRecords.isNotEmpty()) fsRecords.map { it.diastolic }.average().toInt() else 0
        val avgHr = if (fsRecords.isNotEmpty()) fsRecords.map { it.heartRate }.average().toInt() else 0
        val maxSys = if (fsRecords.isNotEmpty()) fsRecords.maxOf { it.systolic } else 0
        val minDia = if (fsRecords.isNotEmpty()) fsRecords.minOf { it.diastolic } else 0

        DisposableEffect(Unit) {
            val w = ctxActivity?.window ?: return@DisposableEffect onDispose {}
            val ic = androidx.core.view.WindowInsetsControllerCompat(w, w.decorView)
            ic.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            ic.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            ctxActivity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            onDispose {
                ic.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                ctxActivity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
        BackHandler { showFullscreenChart = false }

        Box(Modifier.fillMaxSize().background(Color(0xFF1A1A2E))) {
            Column(Modifier.fillMaxSize()) {
                // 顶部栏 — 返回 + 标题 + 时间切换
                Row(
                    Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showFullscreenChart = false }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Close, "返回", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (fullscreenChartType == "bp") "血压趋势" else "心率趋势",
                        color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val ranges = listOf(TimeRange.WEEK, TimeRange.TWO_WEEKS, TimeRange.MONTH, TimeRange.ALL)
                        ranges.forEach { r ->
                            val selected = fsRange == r
                            Surface(
                                modifier = Modifier.clickable { fsRange = r },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) Color(0xFFC75B39).copy(alpha = 0.25f) else Color.Transparent
                            ) {
                                Text(r.label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    fontSize = 13.sp,
                                    color = if (selected) Color(0xFFC75B39) else Color(0xFF888888),
                                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal)
                            }
                        }
                    }
                }

                // 图表区 - 使用 Box 包裹以确保 proper layout
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    LineChart(
                        records = fsRecords,
                        showHr = fullscreenChartType == "hr",
                        isDarkBg = true,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 底部统计栏
                Row(
                    Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (fullscreenChartType == "bp") {
                        Text("均值 ${avgSys}/${avgDia}", color = Color(0xFFAAAAAA), fontSize = 13.sp)
                        Text("最高 $maxSys", color = Color(0xFFAAAAAA), fontSize = 13.sp)
                        Text("最低 $minDia", color = Color(0xFFAAAAAA), fontSize = 13.sp)
                    }
                    Text("心率 ${avgHr}bpm", color = Color(0xFFAAAAAA), fontSize = 13.sp)
                }
            }
        }
    }
}
}

@Composable
private fun CurrentScreenContent(
    currentScreen: String,
    todayRecords: List<BloodPressureRecord>,
    todayMedRecords: List<MedicationRecord>,
    medications: List<Medication>,
    allBpRecords: List<BloodPressureRecord>,
    allMedRecords: List<MedicationRecord>,
    dataRecords: List<BloodPressureRecord>,
    allRecordsForHome: List<BloodPressureRecord>,
    showSaveSuccess: Boolean,
    onSaveSuccessConsumed: () -> Unit,
    onAddBloodPressure: () -> Unit,
    onAddMedication: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onStartTraining: () -> Unit,
    onBackToSports: () -> Unit,
    onMedicationChanged: () -> Unit,
    onSportsChanged: () -> Unit,
    onOpenFullscreen: (chartType: String, records: List<BloodPressureRecord>) -> Unit,
    sportsRefreshTrigger: Int,
    currentTrainingPlan: TrainingPlan,
    onShowPlanSelection: () -> Unit,
    onShowCustomPlan: () -> Unit,
    onBackToPlanSelection: () -> Unit,
    onTrainingPlanSelected: (TrainingPlan) -> Unit,
    onSaveBloodPressure: (systolic: Int, diastolic: Int, heartRate: Int, date: LocalDate, period: MeasurementPeriod, medicationTaken: Boolean) -> Unit,
    onBack: () -> Unit,
    db: AppDatabase,
    lifecycleScope: kotlinx.coroutines.CoroutineScope,
    today: LocalDate
) {
    when (currentScreen) {
        "home" -> HomeScreen(
            todayRecords = todayRecords,
            todayMedRecords = todayMedRecords,
            medications = medications,
            recentRecords = allRecordsForHome, // 传递60天数据，在HomeScreen中根据时间范围过滤
            showSaveSuccess = showSaveSuccess,
            onSaveSuccessConsumed = onSaveSuccessConsumed,
            onAddBloodPressure = onAddBloodPressure,
            onAddMedication = onAddMedication,
            onNavigateToHistory = onNavigateToHistory,
            onNavigateToSettings = onNavigateToSettings
        )

        "data" -> DataScreen(
            allRecords = dataRecords,
            onNavigateToHistory = onNavigateToHistory,
            onOpenFullscreen = onOpenFullscreen
        )

        "sports" -> {
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val startOfWeek = today.minus(kotlinx.datetime.DatePeriod(days = today.dayOfWeek.ordinal - 1))
            val trainingDao = db.trainingDao()
            var recentRecords by remember { mutableStateOf<List<TrainingRecord>>(emptyList()) }
            var completedThisWeek by remember { mutableStateOf(0) }
            var totalCompleted by remember { mutableStateOf(0) }
            var totalMinutes by remember { mutableStateOf(0) }

            LaunchedEffect(sportsRefreshTrigger) {
                recentRecords = trainingDao.getRecent(10)
                completedThisWeek = trainingDao.getCompletedCount(startOfWeek, today)
                totalCompleted = trainingDao.getTotalCompleted()
                totalMinutes = (trainingDao.getTotalDuration() ?: 0) / 60
            }

            SportsScreen(
                records = recentRecords,
                completedThisWeek = completedThisWeek,
                weeklyGoal = 3,
                totalCompleted = totalCompleted,
                totalMinutes = totalMinutes,
                currentPlan = currentTrainingPlan,
                onStartTraining = onStartTraining,
                onShowPlanSelection = onShowPlanSelection
            )
        }

        "training" -> {
            // 跟踪是否已完成全部训练（用于区分保存完成/未完成记录）
            var trainingFinished by remember { mutableStateOf(false) }

            TrainingScreen(
                groupCount = currentTrainingPlan.groupCount,
                groupDuration = currentTrainingPlan.groupDuration,
                restDuration = currentTrainingPlan.restDuration,
                onFinish = { completedGroups, totalSeconds ->
                    trainingFinished = true
                    val dao = db.trainingDao()
                    lifecycleScope.launch { val activity = this
                        val nowTs = Clock.System.now().toEpochMilliseconds()
                        val startTs = nowTs - totalSeconds * 1000L
                        dao.insert(
                            TrainingRecord(
                                date = today,
                                startTime = startTs,
                                totalDuration = totalSeconds,
                                completedGroups = completedGroups,
                                totalGroups = currentTrainingPlan.groupCount,
                                groupDuration = currentTrainingPlan.groupDuration,
                                restDuration = currentTrainingPlan.restDuration,
                                completed = completedGroups >= currentTrainingPlan.groupCount
                            )
                        )
                        onSportsChanged()
                    }
                },
                onExit = { onBackToSports() },
                onEndTraining = { completedGroups, totalSeconds ->
                    if (!trainingFinished) {
                        lifecycleScope.launch { val activity = this
                            val nowTs = Clock.System.now().toEpochMilliseconds()
                            val startTs = nowTs - totalSeconds * 1000L
                            db.trainingDao().insert(
                                TrainingRecord(
                                    date = today,
                                    startTime = startTs,
                                    totalDuration = totalSeconds,
                                    completedGroups = completedGroups,
                                    totalGroups = currentTrainingPlan.groupCount,
                                    groupDuration = currentTrainingPlan.groupDuration,
                                    restDuration = currentTrainingPlan.restDuration,
                                    completed = false
                                )
                            )
                            onSportsChanged()
                        }
                    }
                }
            )
        }

        // 训练方案选择
        "plan_selection" -> {
            TrainingPlanSelectionScreen(
                currentPlan = currentTrainingPlan,
                onPlanSelected = onTrainingPlanSelected,
                onCustomPlan = onShowCustomPlan,
                onBack = onBackToSports
            )
        }

        // 自定义训练方案
        "custom_plan" -> {
            CustomTrainingPlanScreen(
                initialPlan = currentTrainingPlan,
                onSave = onTrainingPlanSelected,
                onBack = onBackToPlanSelection
            )
        }

        "input_bp" -> BloodPressureInputScreen(
            onSave = onSaveBloodPressure,
            onBack = onBack
        )

        "medication" -> MedicationCheckInScreen(
            medications = medications,
            todayRecords = todayMedRecords,
            onTakeMedication = { medicationId, taken ->
                lifecycleScope.launch { val activity = this
                    val existing = db.medicationRecordDao().getByMedicationAndDate(medicationId, today)
                    if (existing == null) {
                        val med = medications.find { it.id == medicationId }
                        db.medicationRecordDao().insert(
                            MedicationRecord(
                                medicationId = medicationId,
                                date = today,
                                taken = taken,
                                dosage = med?.dosage ?: "",
                                takenAt = if (taken) System.currentTimeMillis() else null
                            )
                        )
                    } else {
                        db.medicationRecordDao().updateTaken(existing.id, taken, if (taken) System.currentTimeMillis() else null)
                    }
                    onMedicationChanged()
                }
            },
            onAddMedication = { name, dosage ->
                lifecycleScope.launch { val activity = this
                    db.medicationDao().insert(Medication(name = name, dosage = dosage))
                    onMedicationChanged()
                }
            },
            onUpdateMedication = { medicationId, newName, newDosage ->
                lifecycleScope.launch { val activity = this
                    db.medicationDao().updateNameAndDosage(medicationId, newName, newDosage)
                    onMedicationChanged()
                }
            },
            onDeleteMedication = { medicationId ->
                lifecycleScope.launch { val activity = this
                    db.medicationDao().deleteById(medicationId)
                    onMedicationChanged()
                }
            },
            onBack = onBack
        )

        "history" -> HistoryScreen(
            bpRecords = allBpRecords,
            medRecords = allMedRecords,
            medications = medications,
            onDeleteBpRecord = { record ->
                lifecycleScope.launch { val activity = this
                    db.bloodPressureDao().delete(record)
                }
            },
            onUpdateBpRecord = { id, systolic, diastolic, heartRate, date, period ->
                lifecycleScope.launch { val activity = this
                    db.bloodPressureDao().updateRecord(id, systolic, diastolic, heartRate, date, period)
                }
            },
            onDeleteMedRecord = { record ->
                lifecycleScope.launch { val activity = this
                    db.medicationRecordDao().delete(record)
                }
            },
            onUpdateMedRecord = { id, medicationId, date, taken, dosage ->
                lifecycleScope.launch { val activity = this
                    db.medicationRecordDao().updateMedRecord(id, medicationId, date, taken, dosage)
                }
            },
            onBack = onBack
        )

        "settings" -> {
            val context = LocalContext.current
            val morningBp = ReminderPreferences.getMorningBpTime(context)
            val morningMed = ReminderPreferences.getMorningMedTime(context)
            val eveningBp = ReminderPreferences.getEveningBpTime(context)

            SettingsScreen(
                morningBpHour = morningBp.first,
                morningBpMinute = morningBp.second,
                morningMedHour = morningMed.first,
                morningMedMinute = morningMed.second,
                eveningBpHour = eveningBp.first,
                eveningBpMinute = eveningBp.second,
                onSaveReminderTimes = { mbpH, mbpM, mmedH, mmedM, ebpH, ebpM ->
                    ReminderPreferences.setMorningBpTime(context, mbpH, mbpM)
                    ReminderPreferences.setMorningMedTime(context, mmedH, mmedM)
                    ReminderPreferences.setEveningBpTime(context, ebpH, ebpM)
                    ReminderScheduler.cancelAll(context)
                    ReminderScheduler.scheduleAll(context)
                },
                onBack = onBack
            )
        }
    }
}
