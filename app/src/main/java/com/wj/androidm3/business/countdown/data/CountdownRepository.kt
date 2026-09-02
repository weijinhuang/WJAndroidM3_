package com.wj.androidm3.business.countdown.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class CountdownRepository private constructor(context: Context) {
    private val dao = CountdownDatabase.getInstance(context).countdownDao()
    private val screenshotStore = CountdownScreenshotStore(context)

    fun observeAll(): Flow<List<CountdownEntity>> = dao.observeAll()

    suspend fun getAll(): List<CountdownEntity> = dao.getAll()

    suspend fun hasRunningTasks(): Boolean {
        dao.reconcileExpired(System.currentTimeMillis())
        return dao.countByStatus(CountdownStatus.RUNNING) > 0
    }

    suspend fun createAndStart(
        seconds: Int,
        name: String? = null,
        screenshotPath: String? = null
    ): Long {
        require(seconds > 0) { "Countdown duration must be greater than zero" }
        val now = System.currentTimeMillis()
        val durationMs = seconds * 1000L
        return dao.insert(
            CountdownEntity(
                durationMs = durationMs,
                status = CountdownStatus.RUNNING,
                createdAtEpochMs = now,
                endAtEpochMs = now + durationMs,
                remainingAtPauseMs = durationMs,
                name = name?.trim()?.takeIf { it.isNotEmpty() },
                screenshotPath = screenshotPath
            )
        )
    }

    suspend fun start(id: Long) = resume(id)

    suspend fun pause(id: Long) {
        val task = dao.getById(id) ?: return
        if (task.status != CountdownStatus.RUNNING) return
        val remaining = CountdownTime.remainingMillis(task, System.currentTimeMillis())
        dao.update(
            task.copy(
                status = if (remaining > 0L) CountdownStatus.PAUSED else CountdownStatus.COMPLETED,
                endAtEpochMs = null,
                remainingAtPauseMs = remaining
            )
        )
    }

    suspend fun resume(id: Long) {
        val task = dao.getById(id) ?: return
        if (task.status != CountdownStatus.PAUSED && task.status != CountdownStatus.PENDING) return
        val remaining = when (task.status) {
            CountdownStatus.PENDING -> task.durationMs
            else -> task.remainingAtPauseMs
        }.coerceAtLeast(0L)
        if (remaining == 0L) {
            dao.update(task.copy(status = CountdownStatus.COMPLETED, endAtEpochMs = null, remainingAtPauseMs = 0L))
            return
        }
        dao.update(
            task.copy(
                status = CountdownStatus.RUNNING,
                endAtEpochMs = System.currentTimeMillis() + remaining,
                remainingAtPauseMs = remaining
            )
        )
    }

    suspend fun delete(id: Long) {
        val task = dao.getById(id) ?: return
        dao.deleteById(id)
        task.screenshotPath?.let(screenshotStore::deleteSafely)
    }

    suspend fun reconcileExpired(nowEpochMs: Long = System.currentTimeMillis()): List<CountdownEntity> =
        dao.reconcileExpired(nowEpochMs)

    suspend fun collectPreEndAlerts(nowEpochMs: Long = System.currentTimeMillis()): List<CountdownEntity> =
        dao.collectPreEndAlerts(nowEpochMs)

    suspend fun deleteExpiredAndCompleted(nowEpochMs: Long = System.currentTimeMillis()): Int {
        val deletedTasks = dao.deleteExpiredAndCompleted(nowEpochMs)
        deletedTasks.forEach { task ->
            task.screenshotPath?.let(screenshotStore::deleteSafely)
        }
        return deletedTasks.size
    }

    companion object {
        @Volatile
        private var instance: CountdownRepository? = null

        fun getInstance(context: Context): CountdownRepository =
            instance ?: synchronized(this) {
                instance ?: CountdownRepository(context.applicationContext).also { instance = it }
            }
    }
}
