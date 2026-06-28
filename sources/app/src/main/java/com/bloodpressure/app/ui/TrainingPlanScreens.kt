package com.bloodpressure.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bloodpressure.app.data.TrainingPlan
import com.bloodpressure.app.data.TrainingPlanPrefs
import com.bloodpressure.app.ui.theme.AppBackground
import com.bloodpressure.app.ui.theme.Primary
import com.bloodpressure.app.ui.theme.SurfaceVariant
import com.bloodpressure.app.ui.theme.SurfaceWhite
import com.bloodpressure.app.ui.theme.TextPrimary
import com.bloodpressure.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingPlanSelectionScreen(
    currentPlan: TrainingPlan,
    onPlanSelected: (TrainingPlan) -> Unit,
    onCustomPlan: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = { Text("训练方案", fontWeight = FontWeight.SemiBold) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TrainingPlanPrefs.presetPlans.forEach { plan ->
                TrainingPlanCard(
                    plan = plan,
                    selected = plan == currentPlan,
                    onClick = { onPlanSelected(plan) }
                )
            }

            TextButton(
                onClick = onCustomPlan,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("自定义训练方案", color = Primary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTrainingPlanScreen(
    initialPlan: TrainingPlan,
    onSave: (TrainingPlan) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf(initialPlan.name) }
    var groupCount by remember { mutableStateOf(initialPlan.groupCount) }
    var groupDurationMinutes by remember { mutableStateOf((initialPlan.groupDuration / 60).coerceAtLeast(1)) }
    var restDurationSeconds by remember { mutableStateOf(initialPlan.restDuration.coerceAtLeast(15)) }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = { Text("自定义方案", fontWeight = FontWeight.SemiBold) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(20) },
                        label = { Text("方案名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    PlanSlider(
                        title = "训练组数",
                        valueText = "${groupCount}组",
                        value = groupCount.toFloat(),
                        range = 1f..8f,
                        steps = 6,
                        onValueChange = { groupCount = it.toInt() }
                    )

                    PlanSlider(
                        title = "每组时长",
                        valueText = "${groupDurationMinutes}分钟",
                        value = groupDurationMinutes.toFloat(),
                        range = 1f..5f,
                        steps = 3,
                        onValueChange = { groupDurationMinutes = it.toInt() }
                    )

                    PlanSlider(
                        title = "休息时长",
                        valueText = "${restDurationSeconds}秒",
                        value = restDurationSeconds.toFloat(),
                        range = 15f..180f,
                        steps = 10,
                        onValueChange = { restDurationSeconds = it.toInt() }
                    )
                }
            }

            Button(
                onClick = {
                    onSave(
                        TrainingPlan(
                            name = name.ifBlank { "Custom plan" },
                            groupCount = groupCount,
                            groupDuration = groupDurationMinutes * 60,
                            restDuration = restDurationSeconds
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("保存方案", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TrainingPlanCard(
    plan: TrainingPlan,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 3.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(plan.name, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(plan.summary, fontSize = 13.sp, color = TextSecondary)
            }
            if (selected) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        "当前",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        color = Primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanSlider(
    title: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Medium)
            Box(
                modifier = Modifier
                    .background(SurfaceVariant, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(valueText, color = TextSecondary, fontSize = 13.sp)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps
        )
    }
}
