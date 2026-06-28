package com.bloodpressure.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bloodpressure.app.data.TrainingPlan
import com.bloodpressure.app.data.TrainingRecord
import com.bloodpressure.app.ui.theme.*

@Composable
fun SportsScreen(
    records: List<TrainingRecord>,
    completedThisWeek: Int,
    weeklyGoal: Int,
    totalCompleted: Int,
    totalMinutes: Int,
    currentPlan: TrainingPlan,
    onStartTraining: () -> Unit,
    onShowPlanSelection: () -> Unit
) {
    val remaining = (weeklyGoal - completedThisWeek).coerceAtLeast(0)
    val progress = if (weeklyGoal > 0) (completedThisWeek.toFloat() / weeklyGoal).coerceIn(0f, 1f) else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ===== 本周统计卡片 =====
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 环形进度
                Box(
                    modifier = Modifier.size(72.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 5.dp,
                        trackColor = SecondaryLight,
                        color = Primary,
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$completedThisWeek",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                        Text(
                            "/ ${weeklyGoal}次",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                Column {
                    Text(
                        "本周训练",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Text(
                        if (remaining > 0) "还差 $remaining 次完成本周目标" else "本周目标已完成",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFE8F5E9)
                        ) {
                            Text(
                                "累计 $totalCompleted 次",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = BpNormal
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SecondaryLight
                        ) {
                            Text(
                                "总时长 ${totalMinutes}分钟",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Primary
                            )
                        }
                    }
                }
            }
        }

        // ===== 开始训练按钮 ======
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onShowPlanSelection() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "当前方案",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        currentPlan.name,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
                Text(
                    "${currentPlan.groupCount}组",
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onStartTraining() },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(Primary, PrimaryDark)),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        currentPlan.name,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "开始训练",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("${currentPlan.groupCount}组", fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f))
                        Text("|", fontSize = 13.sp, color = Color.White.copy(alpha = 0.3f))
                        Text("${currentPlan.groupDuration / 60}分钟/组", fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f))
                        Text("|", fontSize = 13.sp, color = Color.White.copy(alpha = 0.3f))
                        Text("休息${currentPlan.restDuration}秒", fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f))
                    }
                }
            }
        }

        // ===== 最近记录 ======
        if (records.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "最近训练",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
            }

            records.take(5).forEach { record ->
                TrainingRecordCard(record)
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun TrainingRecordCard(record: TrainingRecord) {
    val completed = record.completed
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (completed) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (completed) "✓" else "—",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (completed) BpNormal else WarningOrange
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (completed) "完成训练" else "中途退出",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    "${record.date.monthNumber}月${record.date.dayOfMonth}日 · ${record.completedGroups}/${record.totalGroups}组 · ${record.totalDuration / 60}分钟",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}
