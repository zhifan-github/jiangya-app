package com.bloodpressure.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bloodpressure.app.data.Medication
import com.bloodpressure.app.data.MedicationRecord
import com.bloodpressure.app.ui.theme.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationCheckInScreen(
    medications: List<Medication>,
    todayRecords: List<MedicationRecord>,
    onTakeMedication: (medicationId: Long, taken: Boolean) -> Unit,
    onAddMedication: (name: String, dosage: String) -> Unit,
    onUpdateMedication: (medicationId: Long, newName: String, newDosage: String) -> Unit,
    onDeleteMedication: (medicationId: Long) -> Unit,
    onBack: () -> Unit
) {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    // 本地状态：记录每个药物的打卡状态（用于立即刷新UI）
    var localTakenStates by remember { mutableStateOf<Map<Long, Boolean>>(emptyMap()) }

    // 初始化：从todayRecords中读取初始状态
    LaunchedEffect(todayRecords) {
        val states = todayRecords.associate { it.medicationId to it.taken }
        localTakenStates = states
    }

    // 编辑对话框状态
    var editingMedication by remember { mutableStateOf<Medication?>(null) }
    // 新增药品对话框状态
    var showAddDialog by remember { mutableStateOf(false) }
    // 删除确认对话框
    var deletingMedication by remember { mutableStateOf<Medication?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = { Text("用药打卡", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { showAddDialog = true }) {
                        Text("+ 新增", color = Primary, fontWeight = FontWeight.Medium)
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 日期和提示
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        "今日用药 ${today.monthNumber}月${today.dayOfMonth}日",
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "点击圆圈打卡，长按卡片可编辑",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            if (medications.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        "暂无药物，点击右上角\"新增\"添加",
                        modifier = Modifier.padding(18.dp),
                        color = TextSecondary
                    )
                }
            }

            medications.forEach { medication ->
                // 优先使用本地状态，如果没有则使用数据库记录
                val isTaken = localTakenStates[medication.id] ?: (todayRecords.find { it.medicationId == medication.id }?.taken == true)
                
                MedicationCard(
                    medication = medication,
                    isTaken = isTaken,
                    takenAt = if (isTaken) todayRecords.find { it.medicationId == medication.id }?.takenAt else null,
                    onToggle = {
                        // 先更新本地状态，立即刷新UI
                        localTakenStates = localTakenStates.toMutableMap().apply {
                            put(medication.id, !isTaken)
                        }
                        // 然后调用回调，更新数据库
                        onTakeMedication(medication.id, !isTaken)
                    },
                    onEdit = { editingMedication = medication },
                    onDelete = { deletingMedication = medication }
                )
            }

            // 忘药提示（使用本地状态，实时更新）
            val unTakenMeds = medications.filter { med ->
                localTakenStates[med.id] != true
            }
            if (unTakenMeds.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = WarningOrange.copy(alpha = 0.1f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "还有 ${unTakenMeds.size} 种药未服用，记得及时补服哦！",
                            color = WarningOrange,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // 编辑药品对话框
    editingMedication?.let { med ->
        EditMedicationDialog(
            medication = med,
            onConfirm = { newName, newDosage ->
                onUpdateMedication(med.id, newName, newDosage)
                editingMedication = null
            },
            onDelete = {
                editingMedication = null
                deletingMedication = med
            },
            onDismiss = { editingMedication = null }
        )
    }

    // 新增药品对话框
    if (showAddDialog) {
        AddMedicationDialog(
            onConfirm = { name, dosage ->
                onAddMedication(name, dosage)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    // 删除确认对话框
    deletingMedication?.let { med ->
        AlertDialog(
            onDismissRequest = { deletingMedication = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除\"${med.name}\"吗？删除后历史打卡记录仍保留。") },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDeleteMedication(med.id)
                        deletingMedication = null
                    }
                ) { Text("删除", color = DangerRed) }
            },
            confirmButton = {
                TextButton(onClick = { deletingMedication = null }) { Text("取消") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MedicationCard(
    medication: Medication,
    isTaken: Boolean,
    takenAt: Long?,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isTaken)
                Color(0xFFF0FFF0)       // 浅绿底
            else
                SurfaceWhite
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { showMenu = true }
                    )
            ) {
                Text(
                    medication.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "剂量: ${medication.dosage}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                if (isTaken) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "已打卡",
                        style = MaterialTheme.typography.bodySmall,
                        color = SuccessGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 长按菜单
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("编辑") },
                    onClick = {
                        showMenu = false
                        onEdit()
                    }
                )
                DropdownMenuItem(
                    text = { Text("删除", color = DangerRed) },
                    onClick = {
                        showMenu = false
                        onDelete()
                    }
                )
            }

            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (isTaken) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = if (isTaken) "已服用" else "未服用",
                    tint = if (isTaken) SuccessGreen else TextHint,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

@Composable
private fun EditMedicationDialog(
    medication: Medication,
    onConfirm: (name: String, dosage: String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(medication.name) }
    var dosage by remember { mutableStateOf(medication.dosage) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑药品") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("药品名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        focusedLabelColor = Primary
                    )
                )
                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text("剂量（如 5mg）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        focusedLabelColor = Primary
                    )
                )
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) {
                    Text("删除", color = DangerRed)
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = { onConfirm(name.trim(), dosage.trim()) },
                    enabled = name.isNotBlank() && dosage.isNotBlank()
                ) { Text("保存", color = Primary) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun AddMedicationDialog(
    onConfirm: (name: String, dosage: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增药品") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("药品名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        focusedLabelColor = Primary
                    )
                )
                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text("剂量（如 5mg）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        focusedLabelColor = Primary
                    )
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), dosage.trim()) },
                enabled = name.isNotBlank() && dosage.isNotBlank()
            ) { Text("添加", color = Primary) }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
