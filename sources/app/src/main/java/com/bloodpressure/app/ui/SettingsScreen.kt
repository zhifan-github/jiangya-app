package com.bloodpressure.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bloodpressure.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    morningBpHour: Int,
    morningBpMinute: Int,
    morningMedHour: Int,
    morningMedMinute: Int,
    eveningBpHour: Int,
    eveningBpMinute: Int,
    onSaveReminderTimes: (morningBpH: Int, morningBpM: Int, morningMedH: Int, morningMedM: Int, eveningBpH: Int, eveningBpM: Int) -> Unit,
    onBack: () -> Unit
) {
    var morningBpH by remember { mutableStateOf(morningBpHour) }
    var morningBpM by remember { mutableStateOf(morningBpMinute) }
    var morningMedH by remember { mutableStateOf(morningMedHour) }
    var morningMedM by remember { mutableStateOf(morningMedMinute) }
    var eveningBpH by remember { mutableStateOf(eveningBpHour) }
    var eveningBpM by remember { mutableStateOf(eveningBpMinute) }

    // 时间选择器状态
    var editingField by remember { mutableStateOf<String?>(null) }
    var tempHour by remember { mutableStateOf(0) }
    var tempMinute by remember { mutableStateOf(0) }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text("提醒设置", fontWeight = FontWeight.SemiBold) },
                windowInsets = WindowInsets(0.dp),
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
                .fillMaxSize()
                .background(AppBackground)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 提示
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Secondary.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = Secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "自定义每日提醒时间",
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "修改后立即生效，第二天按新时间提醒",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            // 早晨测血压
            ReminderTimeCard(
                title = "早晨测血压",
                subtitle = "起床后测量",
                hour = morningBpH,
                minute = morningBpM,
                color = Primary,
                onClick = {
                    tempHour = morningBpH
                    tempMinute = morningBpM
                    editingField = "morning_bp"
                }
            )

            // 早晨吃药
            ReminderTimeCard(
                title = "早晨吃药",
                subtitle = "测血压后服药",
                hour = morningMedH,
                minute = morningMedM,
                color = Secondary,
                onClick = {
                    tempHour = morningMedH
                    tempMinute = morningMedM
                    editingField = "morning_med"
                }
            )

            // 晚间测血压
            ReminderTimeCard(
                title = "晚间测血压",
                subtitle = "睡前测量",
                hour = eveningBpH,
                minute = eveningBpM,
                color = Accent,
                onClick = {
                    tempHour = eveningBpH
                    tempMinute = eveningBpM
                    editingField = "evening_bp"
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 保存按钮
            Button(
                onClick = {
                    onSaveReminderTimes(morningBpH, morningBpM, morningMedH, morningMedM, eveningBpH, eveningBpM)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("保存设置", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
    }

    // 时间选择对话框
    if (editingField != null) {
        TimePickerDialog(
            initialHour = tempHour,
            initialMinute = tempMinute,
            onConfirm = { h, m ->
                when (editingField) {
                    "morning_bp" -> { morningBpH = h; morningBpM = m }
                    "morning_med" -> { morningMedH = h; morningMedM = m }
                    "evening_bp" -> { eveningBpH = h; eveningBpM = m }
                }
                editingField = null
            },
            onDismiss = { editingField = null }
        )
    }
}

@Composable
private fun ReminderTimeCard(
    title: String,
    subtitle: String,
    hour: Int,
    minute: Int,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    subtitle,
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
            Text(
                String.format("%02d:%02d", hour, minute),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择时间") },
        text = {
            TimePicker(state = timePickerState)
        },
        dismissButton = {
            TextButton(onClick = {
                onConfirm(timePickerState.hour, timePickerState.minute)
            }) { Text("确定", color = Primary) }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
