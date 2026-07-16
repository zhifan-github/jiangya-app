@file:OptIn(ExperimentalFoundationApi::class)

package com.bloodpressure.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bloodpressure.app.data.BloodPressureRecord
import com.bloodpressure.app.data.MeasurementPeriod
import com.bloodpressure.app.data.Medication
import com.bloodpressure.app.data.MedicationRecord
import com.bloodpressure.app.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    bpRecords: List<BloodPressureRecord>,
    medRecords: List<MedicationRecord>,
    medications: List<Medication>,
    onDeleteBpRecord: (BloodPressureRecord) -> Unit,
    onUpdateBpRecord: (id: Long, systolic: Int, diastolic: Int, heartRate: Int, date: LocalDate, period: MeasurementPeriod) -> Unit,
    onDeleteMedRecord: (MedicationRecord) -> Unit,
    onUpdateMedRecord: (id: Long, medicationId: Long, date: LocalDate, taken: Boolean, dosage: String) -> Unit,
    onBack: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    var pendingDeleteBp by remember { mutableStateOf<BloodPressureRecord?>(null) }
    var pendingDeleteMed by remember { mutableStateOf<MedicationRecord?>(null) }

    var editingBpRecord by remember { mutableStateOf<BloodPressureRecord?>(null) }
    var editingMedRecord by remember { mutableStateOf<MedicationRecord?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = { Text("历史记录", fontWeight = FontWeight.SemiBold) },
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
        ) {
            val tabs = listOf("血压记录", "服药记录")
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = SurfaceWhite,
                contentColor = Primary,
                edgePadding = 16.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = {
                            Text(
                                title,
                                fontWeight = if (pagerState.currentPage == index) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (pagerState.currentPage == index) Primary else TextSecondary
                            )
                        }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> BloodPressureHistoryTab(
                        records = bpRecords,
                        onEdit = { record -> editingBpRecord = record },
                        onDelete = { record -> pendingDeleteBp = record }
                    )
                    1 -> MedicationHistoryTab(
                        records = medRecords.filter { it.taken },
                        medications = medications,
                        onEdit = { record -> editingMedRecord = record },
                        onDelete = { record -> pendingDeleteMed = record }
                    )
                }
            }
        }
    }

    pendingDeleteBp?.let { record ->
        AlertDialog(
            onDismissRequest = { pendingDeleteBp = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除这条血压记录吗？\n${record.date}  ${record.systolic}/${record.diastolic}") },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDeleteBpRecord(record)
                        pendingDeleteBp = null
                    }
                ) { Text("删除", color = DangerRed) }
            },
            confirmButton = {
                TextButton(onClick = { pendingDeleteBp = null }) { Text("取消") }
            }
        )
    }

    pendingDeleteMed?.let { record ->
        val medName = medications.find { it.id == record.medicationId }?.name ?: "未知药物"
        AlertDialog(
            onDismissRequest = { pendingDeleteMed = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除这条服药记录吗？\n${record.date}  $medName") },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDeleteMedRecord(record)
                        pendingDeleteMed = null
                    }
                ) { Text("删除", color = DangerRed) }
            },
            confirmButton = {
                TextButton(onClick = { pendingDeleteMed = null }) { Text("取消") }
            }
        )
    }

    editingBpRecord?.let { record ->
        EditBpRecordDialog(
            record = record,
            onConfirm = { id, systolic, diastolic, heartRate, date, period ->
                onUpdateBpRecord(id, systolic, diastolic, heartRate, date, period)
                editingBpRecord = null
            },
            onDismiss = { editingBpRecord = null }
        )
    }

    editingMedRecord?.let { record ->
        EditMedRecordDialog(
            record = record,
            medications = medications,
            onConfirm = { id, medicationId, date, taken, dosage ->
                onUpdateMedRecord(id, medicationId, date, taken, dosage)
                editingMedRecord = null
            },
            onDismiss = { editingMedRecord = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BloodPressureHistoryTab(
    records: List<BloodPressureRecord>,
    onEdit: (BloodPressureRecord) -> Unit,
    onDelete: (BloodPressureRecord) -> Unit
) {
    if (records.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    tint = TextHint,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("暂无血压记录", color = TextSecondary, fontSize = 15.sp)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground),
            contentPadding = PaddingValues(top = 4.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(records, key = { it.id }) { record ->
                BpRecordCard(
                    record = record,
                    onEdit = { onEdit(record) },
                    onDelete = { onDelete(record) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BpRecordCard(
    record: BloodPressureRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onEdit,
                onLongClick = onDelete
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    record.date.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    if (record.period == MeasurementPeriod.MORNING) "早晨" else "晚上",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }

            Text(
                "${record.systolic}/${record.diastolic}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = getBpColor(record.systolic, record.diastolic)
            )

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${record.heartRate} 次/分",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MedicationHistoryTab(
    records: List<MedicationRecord>,
    medications: List<Medication>,
    onEdit: (MedicationRecord) -> Unit,
    onDelete: (MedicationRecord) -> Unit
) {
    if (records.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Medication,
                    contentDescription = null,
                    tint = TextHint,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("暂无服药记录", color = TextSecondary, fontSize = 15.sp)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground),
            contentPadding = PaddingValues(top = 4.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(records, key = { it.id }) { record ->
                MedRecordCard(
                    record = record,
                    medication = medications.find { it.id == record.medicationId },
                    onEdit = { onEdit(record) },
                    onDelete = { onDelete(record) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MedRecordCard(
    record: MedicationRecord,
    medication: Medication?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onEdit,
                onLongClick = onDelete
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    record.date.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    medication?.name ?: "未知药物",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                if (record.dosage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        record.dosage,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun EditBpRecordDialog(
    record: BloodPressureRecord,
    onConfirm: (id: Long, systolic: Int, diastolic: Int, heartRate: Int, date: LocalDate, period: MeasurementPeriod) -> Unit,
    onDismiss: () -> Unit
) {
    var systolic by remember { mutableStateOf(record.systolic.toString()) }
    var diastolic by remember { mutableStateOf(record.diastolic.toString()) }
    var heartRate by remember { mutableStateOf(record.heartRate.toString()) }
    var selectedPeriod by remember { mutableStateOf(record.period) }

    // Date picker state
    var selectedDate by remember { mutableStateOf(record.date) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = record.date.toEpochDays().toLong() * 86400000L
    )

    val isValid = systolic.toIntOrNull()?.let { it in 60..250 } == true &&
            diastolic.toIntOrNull()?.let { it in 40..150 } == true &&
            heartRate.toIntOrNull()?.let { it in 30..200 } == true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑血压记录") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Date selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("日期: ", color = TextSecondary, fontSize = 14.sp)
                    Text(
                        "${selectedDate.year}-${selectedDate.monthNumber}-${selectedDate.dayOfMonth}",
                        fontWeight = FontWeight.Medium,
                        color = Primary,
                        fontSize = 14.sp
                    )
                }

                OutlinedTextField(
                    value = systolic,
                    onValueChange = { if (it.length <= 3) systolic = it.filter { c -> c.isDigit() } },
                    label = { Text("收缩压 (mmHg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        focusedLabelColor = Primary
                    )
                )
                OutlinedTextField(
                    value = diastolic,
                    onValueChange = { if (it.length <= 3) diastolic = it.filter { c -> c.isDigit() } },
                    label = { Text("舒张压 (mmHg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        focusedLabelColor = Primary
                    )
                )
                OutlinedTextField(
                    value = heartRate,
                    onValueChange = { if (it.length <= 3) heartRate = it.filter { c -> c.isDigit() } },
                    label = { Text("心率 (次/分)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        focusedLabelColor = Primary
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .combinedClickable(
                                onClick = { selectedPeriod = MeasurementPeriod.MORNING }
                            ),
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedPeriod == MeasurementPeriod.MORNING) Primary.copy(alpha = 0.12f) else SurfaceVariant
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "早晨",
                                fontWeight = if (selectedPeriod == MeasurementPeriod.MORNING) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selectedPeriod == MeasurementPeriod.MORNING) Primary else TextSecondary
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .combinedClickable(
                                onClick = { selectedPeriod = MeasurementPeriod.EVENING }
                            ),
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedPeriod == MeasurementPeriod.EVENING) Primary.copy(alpha = 0.12f) else SurfaceVariant
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "晚上",
                                fontWeight = if (selectedPeriod == MeasurementPeriod.EVENING) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selectedPeriod == MeasurementPeriod.EVENING) Primary else TextSecondary
                            )
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        record.id,
                        systolic.toInt(),
                        diastolic.toInt(),
                        heartRate.toInt(),
                        selectedDate,
                        selectedPeriod
                    )
                },
                enabled = isValid
            ) { Text("保存", color = if (isValid) Primary else TextHint) }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            dismissButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val epochDay = millis / 86400000L
                            selectedDate = LocalDate.fromEpochDays(epochDay.toInt())
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditMedRecordDialog(
    record: MedicationRecord,
    medications: List<Medication>,
    onConfirm: (id: Long, medicationId: Long, date: LocalDate, taken: Boolean, dosage: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMedicationId by remember { mutableStateOf(record.medicationId) }
    var dosage by remember { mutableStateOf(record.dosage) }
    var medMenuExpanded by remember { mutableStateOf(false) }

    // Date picker state
    var selectedDate by remember { mutableStateOf(record.date) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = record.date.toEpochDays().toLong() * 86400000L
    )

    val selectedMedName = medications.find { it.id == selectedMedicationId }?.name ?: "未知药物"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑服药记录") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Date selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("日期: ", color = TextSecondary, fontSize = 14.sp)
                    Text(
                        "${selectedDate.year}-${selectedDate.monthNumber}-${selectedDate.dayOfMonth}",
                        fontWeight = FontWeight.Medium,
                        color = Primary,
                        fontSize = 14.sp
                    )
                }

                // Medication selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    ExposedDropdownMenuBox(
                        expanded = medMenuExpanded,
                        onExpandedChange = { medMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedMedName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("药品名称") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = medMenuExpanded) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                focusedLabelColor = Primary
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = medMenuExpanded,
                            onDismissRequest = { medMenuExpanded = false }
                        ) {
                            medications.forEach { med ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                med.name,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                med.dosage,
                                                color = TextSecondary,
                                                fontSize = 12.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedMedicationId = med.id
                                        dosage = med.dosage
                                        medMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text("剂量") },
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
                onClick = {
                    onConfirm(record.id, selectedMedicationId, selectedDate, true, dosage.trim())
                }
            ) { Text("保存", color = Primary) }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            dismissButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val epochDay = millis / 86400000L
                            selectedDate = LocalDate.fromEpochDays(epochDay.toInt())
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

private fun getBpColor(systolic: Int, diastolic: Int): Color {
    return when {
        systolic < 120 && diastolic < 80 -> BpNormal
        systolic < 140 && diastolic < 90 -> BpElevated
        else -> BpHigh
    }
}
