package com.wj.androidm3.business.countdown.data

import kotlin.math.ceil

object CountdownTime {
    fun remainingMillis(task: CountdownEntity, nowEpochMs: Long): Long = when (task.status) {
        CountdownStatus.RUNNING -> (task.endAtEpochMs?.minus(nowEpochMs) ?: 0L).coerceAtLeast(0L)
        CountdownStatus.PENDING -> task.durationMs.coerceAtLeast(0L)
        CountdownStatus.PAUSED -> task.remainingAtPauseMs.coerceAtLeast(0L)
        CountdownStatus.COMPLETED -> 0L
    }

    fun remainingSeconds(task: CountdownEntity, nowEpochMs: Long): Long =
        ceil(remainingMillis(task, nowEpochMs) / 1000.0).toLong()

    fun formatSeconds(totalSeconds: Long): String {
        val safeSeconds = totalSeconds.coerceAtLeast(0L)
        return "%02d:%02d".format(safeSeconds / 60L, safeSeconds % 60L)
    }

    fun format(task: CountdownEntity, nowEpochMs: Long): String =
        formatSeconds(remainingSeconds(task, nowEpochMs))

    fun shortestRunning(
        tasks: List<CountdownEntity>,
        nowEpochMs: Long
    ): CountdownEntity? = nearestRunning(tasks, nowEpochMs, 1).firstOrNull()

    fun nearestRunning(
        tasks: List<CountdownEntity>,
        nowEpochMs: Long,
        limit: Int = 2
    ): List<CountdownEntity> = tasks
        .asSequence()
        .filter { it.status == CountdownStatus.RUNNING && remainingMillis(it, nowEpochMs) > 0L }
        .sortedBy { it.endAtEpochMs ?: Long.MAX_VALUE }
        .take(limit.coerceAtLeast(0))
        .toList()

    fun sortedByRemaining(
        tasks: List<CountdownEntity>,
        nowEpochMs: Long
    ): List<CountdownEntity> = tasks.sortedWith(
        compareBy<CountdownEntity> { remainingMillis(it, nowEpochMs) }
            .thenByDescending { it.createdAtEpochMs }
            .thenByDescending { it.id }
    )
}
