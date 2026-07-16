package com.bloodpressure.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bloodpressure.app.data.MeasurementPeriod
import com.bloodpressure.app.recognition.BloodPressurePhotoRecognizer
import com.bloodpressure.app.recognition.RomSunDigitClassifier
import com.bloodpressure.app.ui.theme.*
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloodPressureInputScreen(
    onSave: (systolic: Int, diastolic: Int, heartRate: Int, date: LocalDate, period: MeasurementPeriod, medicationTaken: Boolean) -> Unit,
    onBack: () -> Unit,
    startWithCamera: Boolean = false
) {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

    var systolic by remember { mutableStateOf("") }
    var diastolic by remember { mutableStateOf("") }
    var heartRate by remember { mutableStateOf("") }
    var selectedPeriod by remember {
        mutableStateOf(if (now.time.hour < 12) MeasurementPeriod.MORNING else MeasurementPeriod.EVENING)
    }
    var medicationTaken by remember { mutableStateOf(false) }
    var isRecognizing by remember { mutableStateOf(false) }
    var recognitionMessage by remember { mutableStateOf<String?>(null) }
    var showCamera by remember(startWithCamera) { mutableStateOf(startWithCamera) }
    val recognitionScope = rememberCoroutineScope()
    val context = LocalContext.current
    val digitClassifier = remember {
        runCatching { RomSunDigitClassifier(context.applicationContext) }.getOrNull()
    }
    DisposableEffect(digitClassifier) {
        onDispose { digitClassifier?.close() }
    }

    var selectedDate by remember { mutableStateOf(now.date) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    if (showCamera) {
        BloodPressureCameraScreen(
            onCaptured = { frames ->
                showCamera = false
                isRecognizing = true
                recognitionScope.launch {
                    try {
                        val (visibleFrameCount, result) = withContext(Dispatchers.Default) {
                            val visibleFrames = frames.filter(BloodPressurePhotoRecognizer::hasVisibleDisplay)
                            val recognized = digitClassifier?.let { classifier ->
                                val readings = visibleFrames.map { frame ->
                                    BloodPressurePhotoRecognizer.recognizeScreen(frame, classifier)
                                }
                                BloodPressurePhotoRecognizer.consensus(readings)
                            }
                            visibleFrames.size to recognized
                        }
                        if (result != null) {
                            systolic = result.systolic.toString()
                            diastolic = result.diastolic.toString()
                            heartRate = result.heartRate.toString()
                            recognitionMessage = "已识别读数，请核对后保存"
                        } else if (visibleFrameCount < 5) {
                            recognitionMessage = "未检测到发光数字，请先让血压计停留在读数画面再拍摄"
                        } else {
                            recognitionMessage = "暂未识别到完整读数，请让发光屏幕贴合取景框重试"
                        }
                    } catch (_: Throwable) {
                        recognitionMessage = "识别处理失败，请重新拍摄或手动输入"
                    } finally {
                        frames.forEach { if (!it.isRecycled) it.recycle() }
                        isRecognizing = false
                    }
                }
            },
            onBack = {
                if (startWithCamera) onBack() else showCamera = false
            }
        )
        return
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = {
                    Text("记录血压", fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .background(AppBackground)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ====== 日期 + 时段 同一行 ======
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 左侧：日期
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showDatePicker = true }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Accent.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = Accent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("日期", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                "${selectedDate.monthNumber}/${selectedDate.dayOfMonth}",
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary,
                                fontSize = 15.sp
                            )
                        }
                    }

                    // 分隔线
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(SurfaceVariant)
                    )

                    // 右侧：时段
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text("时段", fontSize = 11.sp, color = TextSecondary)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                PeriodChipSmall(
                                    label = "早晨",
                                    selected = selectedPeriod == MeasurementPeriod.MORNING,
                                    onClick = { selectedPeriod = MeasurementPeriod.MORNING }
                                )
                                PeriodChipSmall(
                                    label = "晚上",
                                    selected = selectedPeriod == MeasurementPeriod.EVENING,
                                    onClick = { selectedPeriod = MeasurementPeriod.EVENING }
                                )
                            }
                        }
                    }
                }
            }

            // ====== 血压数据输入卡片 ======
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Primary.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("血压数据", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }

                    if (startWithCamera) {
                        OutlinedButton(
                            onClick = {
                                recognitionMessage = null
                                showCamera = true
                            },
                            enabled = !isRecognizing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
                        ) {
                            if (isRecognizing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("正在识别…")
                            } else {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("重新拍照识别")
                            }
                        }

                        Text(
                            "请正对血压计，让发光显示屏边缘贴合取景框",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    recognitionMessage?.let { message ->
                        Text(
                            message,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (message.startsWith("已识别")) Primary else DangerRed,
                            fontSize = 12.sp
                        )
                    }

                    // 收缩压
                    OutlinedTextField(
                        value = systolic,
                        onValueChange = { if (it.length <= 3) systolic = it.filter { c -> c.isDigit() } },
                        label = { Text("收缩压 (mmHg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = SurfaceVariant,
                            focusedLabelColor = Primary
                        )
                    )

                    // 舒张压
                    OutlinedTextField(
                        value = diastolic,
                        onValueChange = { if (it.length <= 3) diastolic = it.filter { c -> c.isDigit() } },
                        label = { Text("舒张压 (mmHg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = SurfaceVariant,
                            focusedLabelColor = Primary
                        )
                    )

                    // 心率
                    OutlinedTextField(
                        value = heartRate,
                        onValueChange = { if (it.length <= 3) heartRate = it.filter { c -> c.isDigit() } },
                        label = { Text("心率 (次/分)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = SurfaceVariant,
                            focusedLabelColor = Primary
                        )
                    )

                    // 是否已服药
                    if (selectedPeriod == MeasurementPeriod.MORNING) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = medicationTaken,
                                onCheckedChange = { medicationTaken = it },
                                colors = CheckboxDefaults.colors(checkedColor = Primary)
                            )
                            Text("本次测量前已服药", color = TextPrimary, fontSize = 14.sp)
                        }
                    }

                    // 输入提示
                    if (systolic.isNotEmpty() && diastolic.isNotEmpty() && heartRate.isNotEmpty()) {
                        val isValid = systolic.toIntOrNull()?.let { it in 60..250 } == true &&
                                diastolic.toIntOrNull()?.let { it in 40..150 } == true &&
                                heartRate.toIntOrNull()?.let { it in 30..200 } == true &&
                                (systolic.toIntOrNull() ?: 0) > (diastolic.toIntOrNull() ?: Int.MAX_VALUE)
                        if (!isValid) {
                            Text(
                                "请输入合理范围：收缩压60-250，舒张压40-150，心率30-200",
                                style = MaterialTheme.typography.bodySmall,
                                color = DangerRed,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // ====== 保存按钮 ======
            val isValid = systolic.toIntOrNull()?.let { it in 60..250 } == true &&
                    diastolic.toIntOrNull()?.let { it in 40..150 } == true &&
                    heartRate.toIntOrNull()?.let { it in 30..200 } == true &&
                    (systolic.toIntOrNull() ?: 0) > (diastolic.toIntOrNull() ?: Int.MAX_VALUE)

            Button(
                onClick = {
                    onSave(
                        systolic.toInt(),
                        diastolic.toInt(),
                        heartRate.toInt(),
                        selectedDate,
                        selectedPeriod,
                        medicationTaken
                    )
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    disabledContainerColor = Primary.copy(alpha = 0.4f)
                )
            ) {
                Text("保存记录", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // 日期选择弹窗
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            dismissButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val epochDay = millis / 86400000L
                            val localDate = LocalDate.fromEpochDays(epochDay.toInt())
                            selectedDate = localDate
                        }
                        showDatePicker = false
                    }
                ) { Text("确定", color = Primary) }
            },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun PeriodChipSmall(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(40.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(6.dp),
        color = if (selected) Primary else SurfaceVariant
    ) {
        Box(
            modifier = Modifier.padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                color = if (selected) Color.White else TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}
