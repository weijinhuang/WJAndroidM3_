package com.wj.androidm3.business.countdown

import com.wj.androidm3.business.countdown.data.CountdownEntity
import com.wj.androidm3.business.countdown.data.CountdownStatus
import com.wj.androidm3.business.countdown.data.CountdownTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CountdownTimeTest {
    @Test
    fun runningCountdownUsesDeadlineAndRoundsUp() {
        val task = runningTask(id = 1, endAt = 61_001L)

        assertEquals(60_001L, CountdownTime.remainingMillis(task, 1_000L))
        assertEquals(61L, CountdownTime.remainingSeconds(task, 1_000L))
        assertEquals("01:01", CountdownTime.format(task, 1_000L))
        assertEquals("00:00", CountdownTime.format(task, 61_001L))
    }

    @Test
    fun pausedAndCompletedCountdownsUsePersistedState() {
        val paused = CountdownEntity(
            id = 2,
            durationMs = 60_000L,
            status = CountdownStatus.PAUSED,
            createdAtEpochMs = 0L,
            remainingAtPauseMs = 12_250L
        )
        val completed = paused.copy(id = 3, status = CountdownStatus.COMPLETED)

        assertEquals("00:13", CountdownTime.format(paused, 999_999L))
        assertEquals("00:00", CountdownTime.format(completed, 0L))
    }

    @Test
    fun shortestRunningIgnoresPausedCompletedAndExpiredTasks() {
        val tasks = listOf(
            runningTask(id = 1, endAt = 8_000L),
            runningTask(id = 2, endAt = 5_000L),
            runningTask(id = 3, endAt = 900L),
            runningTask(id = 4, endAt = 3_000L).copy(status = CountdownStatus.PAUSED)
        )

        assertEquals(2L, CountdownTime.shortestRunning(tasks, 1_000L)?.id)
        assertEquals(listOf(2L, 1L), CountdownTime.nearestRunning(tasks, 1_000L).map { it.id })
        assertNull(CountdownTime.shortestRunning(tasks, 9_000L))
        assertEquals(emptyList<CountdownEntity>(), CountdownTime.nearestRunning(tasks, 9_000L))
    }

    @Test
    fun sortsAllCountdownsByCurrentRemainingTimeAscending() {
        val tasks = listOf(
            runningTask(id = 1, endAt = 11_000L),
            runningTask(id = 2, endAt = 4_000L),
            runningTask(id = 3, endAt = 30_000L).copy(
                status = CountdownStatus.PAUSED,
                remainingAtPauseMs = 5_000L
            ),
            runningTask(id = 4, endAt = 30_000L).copy(status = CountdownStatus.COMPLETED)
        )

        assertEquals(
            listOf(4L, 2L, 3L, 1L),
            CountdownTime.sortedByRemaining(tasks, nowEpochMs = 1_000L).map { it.id }
        )
    }

    @Test
    fun remainingTimeSortUsesNewestTaskFirstWhenTimesMatch() {
        val older = runningTask(id = 1, endAt = 6_000L).copy(createdAtEpochMs = 10L)
        val newer = runningTask(id = 2, endAt = 6_000L).copy(createdAtEpochMs = 20L)

        assertEquals(
            listOf(2L, 1L),
            CountdownTime.sortedByRemaining(listOf(older, newer), nowEpochMs = 1_000L).map { it.id }
        )
    }

    @Test
    fun formatsMaximumSupportedDuration() {
        assertEquals("59:59", CountdownTime.formatSeconds(3_599L))
    }

    @Test
    fun customNameOverridesGeneratedName() {
        val task = runningTask(id = 7, endAt = 8_000L)

        assertEquals("倒计时 #7", task.displayName)
        assertEquals("煮咖啡", task.copy(name = "  煮咖啡  ").displayName)
        assertEquals("倒计时 #7", task.copy(name = "   ").displayName)
    }

    private fun runningTask(id: Long, endAt: Long) = CountdownEntity(
        id = id,
        durationMs = 60_000L,
        status = CountdownStatus.RUNNING,
        createdAtEpochMs = id,
        endAtEpochMs = endAt,
        remainingAtPauseMs = 60_000L
    )
}
