package com.wj.androidm3.business.countdown.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CountdownDao {
    @Query("SELECT * FROM countdown_tasks ORDER BY createdAtEpochMs DESC, id DESC")
    fun observeAll(): Flow<List<CountdownEntity>>

    @Query("SELECT * FROM countdown_tasks ORDER BY createdAtEpochMs DESC, id DESC")
    suspend fun getAll(): List<CountdownEntity>

    @Query("SELECT * FROM countdown_tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CountdownEntity?

    @Query("SELECT COUNT(*) FROM countdown_tasks WHERE status = :status")
    suspend fun countByStatus(status: CountdownStatus): Int

    @Insert
    suspend fun insert(entity: CountdownEntity): Long

    @Update
    suspend fun update(entity: CountdownEntity)

    @Query("DELETE FROM countdown_tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM countdown_tasks WHERE status = :status")
    suspend fun getByStatus(status: CountdownStatus): List<CountdownEntity>

    @Query("DELETE FROM countdown_tasks WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query(
        """SELECT * FROM countdown_tasks
            WHERE status = :runningStatus
              AND preEndAlertSent = 0
              AND endAtEpochMs > :nowEpochMs
              AND endAtEpochMs <= :alertAtOrBeforeEpochMs
            ORDER BY endAtEpochMs ASC, id ASC"""
    )
    suspend fun getTasksNeedingPreEndAlert(
        nowEpochMs: Long,
        alertAtOrBeforeEpochMs: Long,
        runningStatus: CountdownStatus = CountdownStatus.RUNNING
    ): List<CountdownEntity>

    @Query("UPDATE countdown_tasks SET preEndAlertSent = 1 WHERE id IN (:ids) AND preEndAlertSent = 0")
    suspend fun markPreEndAlertSent(ids: List<Long>)

    @Transaction
    suspend fun collectPreEndAlerts(
        nowEpochMs: Long,
        thresholdMs: Long = 50_000L
    ): List<CountdownEntity> {
        val tasks = getTasksNeedingPreEndAlert(nowEpochMs, nowEpochMs + thresholdMs)
        if (tasks.isNotEmpty()) {
            markPreEndAlertSent(tasks.map { it.id })
        }
        return tasks
    }

    @Query("SELECT * FROM countdown_tasks WHERE status = :runningStatus AND endAtEpochMs <= :nowEpochMs")
    suspend fun getExpired(
        nowEpochMs: Long,
        runningStatus: CountdownStatus = CountdownStatus.RUNNING
    ): List<CountdownEntity>

    @Query("UPDATE countdown_tasks SET status = :completedStatus, endAtEpochMs = NULL, remainingAtPauseMs = 0 WHERE id IN (:ids) AND status = :runningStatus")
    suspend fun markCompleted(
        ids: List<Long>,
        completedStatus: CountdownStatus = CountdownStatus.COMPLETED,
        runningStatus: CountdownStatus = CountdownStatus.RUNNING
    )

    @Transaction
    suspend fun reconcileExpired(nowEpochMs: Long): List<CountdownEntity> {
        val expired = getExpired(nowEpochMs)
        if (expired.isNotEmpty()) {
            markCompleted(expired.map { it.id })
        }
        return expired
    }

    @Transaction
    suspend fun deleteExpiredAndCompleted(nowEpochMs: Long): List<CountdownEntity> {
        reconcileExpired(nowEpochMs)
        val completed = getByStatus(CountdownStatus.COMPLETED)
        if (completed.isNotEmpty()) {
            deleteByIds(completed.map { it.id })
        }
        return completed
    }
}
