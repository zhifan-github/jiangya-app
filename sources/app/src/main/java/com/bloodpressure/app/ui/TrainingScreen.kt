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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bloodpressure.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

enum class TrainingPhase { GRIP, REST, FINISHED }

data class TrainingTimerState(
    val phase: TrainingPhase,
    val currentGroup: Int,
    val secondsLeft: Int,
    val totalElapsed: Int,
    val completedGroups: Int,
    val isPaused: Boolean
)

fun newTrainingTimerState(groupDuration: Int) = TrainingTimerState(
    phase = TrainingPhase.GRIP,
    currentGroup = 1,
    secondsLeft = groupDuration,
    totalElapsed = 0,
    completedGroups = 0,
    isPaused = false
)

fun pauseTrainingTimer(state: TrainingTimerState): TrainingTimerState =
    if (state.phase == TrainingPhase.FINISHED) state else state.copy(isPaused = true)

fun tickTrainingTimer(
    state: TrainingTimerState,
    groupCount: Int,
    groupDuration: Int,
    restDuration: Int
): TrainingTimerState {
    if (state.isPaused || state.phase == TrainingPhase.FINISHED) return state
    if (state.secondsLeft > 1) {
        return state.copy(
            secondsLeft = state.secondsLeft - 1,
            totalElapsed = state.totalElapsed + 1
        )
    }

    val elapsed = state.totalElapsed + if (state.secondsLeft == 1) 1 else 0
    return when (state.phase) {
        TrainingPhase.GRIP -> {
            if (state.currentGroup >= groupCount) {
                state.copy(
                    phase = TrainingPhase.FINISHED,
                    secondsLeft = 0,
                    totalElapsed = elapsed,
                    completedGroups = state.currentGroup
                )
            } else {
                state.copy(
                    phase = TrainingPhase.REST,
                    secondsLeft = restDuration,
                    totalElapsed = elapsed,
                    completedGroups = state.currentGroup
                )
            }
        }
        TrainingPhase.REST -> state.copy(
            phase = TrainingPhase.GRIP,
            currentGroup = state.currentGroup + 1,
            secondsLeft = groupDuration,
            totalElapsed = elapsed
        )
        TrainingPhase.FINISHED -> state
    }
}

@Composable
fun TrainingScreen(
    groupCount: Int = 4,
    groupDuration: Int = 120, // seconds
    restDuration: Int = 60, // seconds
    onFinish: (completedGroups: Int, totalSeconds: Int) -> Unit,
    onExit: () -> Unit,
    onEndTraining: (completedGroups: Int, totalSeconds: Int) -> Unit = { _, _ -> }
) {
    var timerState by remember(groupCount, groupDuration, restDuration) {
        mutableStateOf(newTrainingTimerState(groupDuration))
    }
    val phase = timerState.phase
    val currentGroup = timerState.currentGroup
    val secondsLeft = timerState.secondsLeft
    val totalElapsed = timerState.totalElapsed
    val isPaused = timerState.isPaused
    val completedGroups = timerState.completedGroups
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    val currentOnFinish by rememberUpdatedState(onFinish)
    var showSkipConfirmation by remember { mutableStateOf(false) }

    DisposableEffect(view) {
        val wasKeepingScreenOn = view.keepScreenOn
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = wasKeepingScreenOn }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                timerState = pauseTrainingTimer(timerState)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(groupCount, groupDuration, restDuration) {
        while (isActive) {
            delay(1000L)
            val previousState = timerState
            val nextState = tickTrainingTimer(
                state = previousState,
                groupCount = groupCount,
                groupDuration = groupDuration,
                restDuration = restDuration
            )
            if (nextState != previousState) {
                timerState = nextState
                if (
                    previousState.phase != TrainingPhase.FINISHED &&
                    nextState.phase == TrainingPhase.FINISHED
                ) {
                    currentOnFinish(nextState.completedGroups, nextState.totalElapsed)
                }
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
                        onClick = { timerState = timerState.copy(isPaused = !timerState.isPaused) },
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

                    // End the current phase early
                    Button(
                        onClick = { showSkipConfirmation = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.1f),
                            contentColor = Color.White.copy(alpha = 0.5f)
                        )
                    ) {
                        Text("提前结束", fontSize = 15.sp)
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

    if (showSkipConfirmation) {
        AlertDialog(
            onDismissRequest = { showSkipConfirmation = false },
            title = { Text("提前结束当前阶段？") },
            text = { Text("确认后将立即进入下一阶段。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSkipConfirmation = false
                        timerState = timerState.copy(secondsLeft = 0)
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSkipConfirmation = false }) {
                    Text("取消")
                }
            }
        )
    }
}
