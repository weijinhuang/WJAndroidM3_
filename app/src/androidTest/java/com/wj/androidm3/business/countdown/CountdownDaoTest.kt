package com.wj.androidm3.business.countdown

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wj.androidm3.business.countdown.data.CountdownDatabase
import com.wj.androidm3.business.countdown.data.CountdownEntity
import com.wj.androidm3.business.countdown.data.CountdownStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CountdownDaoTest {
    private lateinit var database: CountdownDatabase

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CountdownDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun insertsOrdersCompletesAndDeletesTasks() = runBlocking {
        val dao = database.countdownDao()
        val laterId = dao.insert(
            task(createdAt = 20L, endAt = 2_000L).copy(
                name = "泡茶",
                screenshotPath = "/cache/countdown_screenshots/test.png"
            )
        )
        val earlierId = dao.insert(task(createdAt = 10L, endAt = 500L))

        assertEquals(listOf(laterId, earlierId), dao.getAll().map { it.id })
        assertEquals("泡茶", dao.getById(laterId)?.displayName)
        assertEquals("/cache/countdown_screenshots/test.png", dao.getById(laterId)?.screenshotPath)
        val completed = dao.reconcileExpired(1_000L)
        assertEquals(listOf(earlierId), completed.map { it.id })
        assertEquals(CountdownStatus.COMPLETED, dao.getById(earlierId)?.status)
        assertEquals(emptyList<CountdownEntity>(), dao.reconcileExpired(1_000L))

        dao.deleteById(laterId)
        assertNotNull(dao.getById(earlierId))
        assertEquals(1, dao.getAll().size)
    }

    @Test
    fun preEndAlertIsCollectedOnceAtFiftySecondBoundary() = runBlocking {
        val dao = database.countdownDao()
        val taskId = dao.insert(task(createdAt = 1L, endAt = 60_000L).copy(durationMs = 60_000L))

        assertEquals(emptyList<CountdownEntity>(), dao.collectPreEndAlerts(nowEpochMs = 9_999L))
        assertEquals(listOf(taskId), dao.collectPreEndAlerts(nowEpochMs = 10_000L).map { it.id })
        assertEquals(true, dao.getById(taskId)?.preEndAlertSent)
        assertEquals(emptyList<CountdownEntity>(), dao.collectPreEndAlerts(nowEpochMs = 11_000L))
    }

    @Test
    fun openingListCleanupDeletesExpiredAndCompletedButKeepsActiveTasks() = runBlocking {
        val dao = database.countdownDao()
        val expiredId = dao.insert(task(createdAt = 1L, endAt = 900L))
        val completedId = dao.insert(
            task(createdAt = 2L, endAt = 900L).copy(
                status = CountdownStatus.COMPLETED,
                endAtEpochMs = null,
                remainingAtPauseMs = 0L
            )
        )
        val runningId = dao.insert(task(createdAt = 3L, endAt = 2_000L))
        val pausedId = dao.insert(
            task(createdAt = 4L, endAt = 2_000L).copy(
                status = CountdownStatus.PAUSED,
                endAtEpochMs = null,
                remainingAtPauseMs = 500L
            )
        )

        val deleted = dao.deleteExpiredAndCompleted(nowEpochMs = 1_000L)

        assertEquals(setOf(expiredId, completedId), deleted.map { it.id }.toSet())
        assertEquals(setOf(runningId, pausedId), dao.getAll().map { it.id }.toSet())
    }

    private fun task(createdAt: Long, endAt: Long) = CountdownEntity(
        durationMs = 2_000L,
        status = CountdownStatus.RUNNING,
        createdAtEpochMs = createdAt,
        endAtEpochMs = endAt,
        remainingAtPauseMs = 2_000L
    )
}
