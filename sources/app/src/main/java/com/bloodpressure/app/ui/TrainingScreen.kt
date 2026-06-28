package com.bloodpressure.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bloodpressure.app.ui.theme.*
import kotlinx.coroutines.delay

enum class TrainingPhase { GRIP, REST, FINISHED }

@Composable
fun TrainingScreen(
    groupCount: Int = 4,
    groupDuration: Int = 120, // seconds
    restDuration: Int = 60, // seconds
    onFinish: (completedGroups: Int, totalSeconds: Int) -> Unit,
    onExit: () -> Unit,
    onEndTraining: (completedGroups: Int, totalSeconds: Int) -> Unit = { _, _ -> }
) {
    var phase by remember { mutableStateOf(TrainingPhase.GRIP) }
    var currentGroup by remember { mutableStateOf(1) }
    var secondsLeft by remember { mutableStateOf(groupDuration) }
    var totalElapsed by remember { mutableStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    var completedGroups by remember { mutableStateOf(0) }

    // Countdown timer — single coroutine avoids LaunchedEffect restart race conditions
    LaunchedEffect(Unit) {
        // 初始设置
        val initialDuration = when (phase) {
            TrainingPhase.GRIP -> groupDuration
            TrainingPhase.REST -> restDuration
            else -> 0
        }
        secondsLeft = initialDuration

        while (true) {
            if (isPaused) { delay(200L); continue }
            if (phase == TrainingPhase.FINISHED) break

            while (secondsLeft > 0) {
                delay(1000L)
                if (isPaused) break
                secondsLeft--
                totalElapsed++
            }

            if (isPaused) continue

            when (phase) {
                TrainingPhase.GRIP -> {
                    completedGroups = currentGroup
                    if (currentGroup >= groupCount) {
                        phase = TrainingPhase.FINISHED
                        onFinish(completedGroups, totalElapsed)
                    } else {
                        phase = TrainingPhase.REST
                        secondsLeft = restDuration
                    }
                }
                TrainingPhase.REST -> {
                    currentGroup++
                    phase = TrainingPhase.GRIP
                    secondsLeft = groupDuration
                }
                TrainingPhase.FINISHED -> {}
            }
        }
    }

    // Format seconds to MM:SS
    val minutes = secondsLeft / 60
    val seconds = secondsLeft % 60
    val timeStr = "%d:%02d".format(minutes, seconds)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Title
            Text(
                "握力环训练",
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Group progress
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(groupCount) { i ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (i < completedGroups || (i == currentGroup - 1 && phase != TrainingPhase.FINISHED))
                                    Primary
                                else
                                    Color.White.copy(alpha = 0.2f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "第 $currentGroup 组 / 共 $groupCount 组",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(60.dp))

            if (phase == TrainingPhase.FINISHED) {
                // Finished state
                Text("训练完成", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))
                Text("共完成 $groupCount 组",
                    fontSize = 16.sp, color = Color.White.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(8.dp))
                Text("总用时 ${totalElapsed / 60} 分钟",
                    fontSize = 14.sp, color = Color.White.copy(alpha = 0.5f))
            } else {
                // Phase label
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (phase == TrainingPhase.GRIP)
                        Primary.copy(alpha = 0.2f)
                    else
                        WarningOrange.copy(alpha = 0.2f)
                ) {
                    Text(
                        if (phase == TrainingPhase.GRIP) "持续握紧" else "放松休息",
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (phase == TrainingPhase.GRIP) Primary else WarningOrange
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Big countdown
                Text(
                    timeStr,
                    fontSize = 96.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    if (phase == TrainingPhase.GRIP) "剩余 ${secondsLeft}秒"
                    else "休息 ${secondsLeft}秒",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )

                // Pause indicator
                if (isPaused) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "已暂停",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = WarningOrange
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Circle animation
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .border(
                            3.dp,
                            if (phase == TrainingPhase.GRIP) Primary else WarningOrange.copy(alpha = 0.5f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .border(
                                2.dp,
                                (if (phase == TrainingPhase.GRIP) Primary else WarningOrange)
                                    .copy(alpha = 0.3f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (phase == TrainingPhase.GRIP) "✊" else "✋",
                            fontSize = 36.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom controls
            if (phase != TrainingPhase.FINISHED) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Pause/Resume
                    Button(
                        onClick = { isPaused = !isPaused },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.1f),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            if (isPaused) "继续" else "暂停",
                            fontSize = 15.sp
                        )
                    }

                    // Skip group
                    Button(
                        onClick = {
                            secondsLeft = 0
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.1f),
                            contentColor = Color.White.copy(alpha = 0.5f)
                        )
                    ) {
                        Text("跳过", fontSize = 15.sp)
                    }
                }

                // End button
                TextButton(
                    onClick = {
                        onEndTraining(completedGroups, totalElapsed)
                        onExit()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Text(
                        "结束训练",
                        color = DangerRed.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            } else {
                // Finished - back button
                Button(
                    onClick = { onExit() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        contentColor = Color.White
                    )
                ) {
                    Text("返回", fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
