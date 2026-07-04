package com.bloodpressure.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingTimerTest {
    @Test
    fun pause_keepsCurrentPositionAndMarksTimerPaused() {
        val state = TrainingTimerState(
            phase = TrainingPhase.REST,
            currentGroup = 2,
            secondsLeft = 18,
            totalElapsed = 203,
            completedGroups = 2,
            isPaused = false
        )

        val paused = pauseTrainingTimer(state)

        assertTrue(paused.isPaused)
        assertEquals(state.copy(isPaused = true), paused)
    }
    @Test
    fun tick_doesNotAdvanceWhilePaused() {
        val state = TrainingTimerState(
            phase = TrainingPhase.GRIP,
            currentGroup = 1,
            secondsLeft = 30,
            totalElapsed = 12,
            completedGroups = 0,
            isPaused = true
        )

        assertEquals(
            state,
            tickTrainingTimer(state, groupCount = 4, groupDuration = 120, restDuration = 60)
        )
    }

    @Test
    fun tick_advancesAtMostOnePhaseWhenCountdownHasEnded() {
        val state = TrainingTimerState(
            phase = TrainingPhase.GRIP,
            currentGroup = 1,
            secondsLeft = 0,
            totalElapsed = 120,
            completedGroups = 0,
            isPaused = false
        )

        val next = tickTrainingTimer(
            state,
            groupCount = 4,
            groupDuration = 120,
            restDuration = 0
        )

        assertEquals(TrainingPhase.REST, next.phase)
        assertEquals(1, next.currentGroup)
        assertEquals(0, next.secondsLeft)
        assertEquals(1, next.completedGroups)
    }

    @Test
    fun newTimer_alwaysStartsFromFirstGripPhase() {
        assertEquals(
            TrainingTimerState(
                phase = TrainingPhase.GRIP,
                currentGroup = 1,
                secondsLeft = 90,
                totalElapsed = 0,
                completedGroups = 0,
                isPaused = false
            ),
            newTrainingTimerState(groupDuration = 90)
        )
    }
}
