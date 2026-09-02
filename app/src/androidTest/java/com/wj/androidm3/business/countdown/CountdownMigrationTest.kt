package com.wj.androidm3.business.countdown

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wj.androidm3.business.countdown.data.CountdownDatabase
import com.wj.androidm3.business.countdown.data.CountdownStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CountdownMigrationTest {
    @Test
    fun migrationFromVersion1PreservesExistingCountdowns() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "countdown_migration_test.db"
        context.deleteDatabase(databaseName)

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """CREATE TABLE IF NOT EXISTS countdown_tasks (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                durationMs INTEGER NOT NULL,
                                status TEXT NOT NULL,
                                createdAtEpochMs INTEGER NOT NULL,
                                endAtEpochMs INTEGER,
                                remainingAtPauseMs INTEGER NOT NULL
                            )""".trimIndent()
                        )
                        db.execSQL(
                            "INSERT INTO countdown_tasks (durationMs, status, createdAtEpochMs, endAtEpochMs, remainingAtPauseMs) VALUES (60000, 'PAUSED', 1000, NULL, 30000)"
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        helper.writableDatabase
        helper.close()

        val database = Room.databaseBuilder(context, CountdownDatabase::class.java, databaseName)
            .addMigrations(CountdownDatabase.MIGRATION_1_2, CountdownDatabase.MIGRATION_2_3)
            .build()
        try {
            val task = database.countdownDao().getAll().single()
            assertEquals(CountdownStatus.PAUSED, task.status)
            assertEquals(30_000L, task.remainingAtPauseMs)
            assertEquals(null, task.name)
            assertEquals(null, task.screenshotPath)
            assertEquals(false, task.preEndAlertSent)
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }


    @Test
    fun migrationFromVersion2AddsPreEndAlertState() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "countdown_migration_v2_test.db"
        context.deleteDatabase(databaseName)

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """CREATE TABLE IF NOT EXISTS countdown_tasks (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                durationMs INTEGER NOT NULL,
                                status TEXT NOT NULL,
                                createdAtEpochMs INTEGER NOT NULL,
                                endAtEpochMs INTEGER,
                                remainingAtPauseMs INTEGER NOT NULL,
                                name TEXT,
                                screenshotPath TEXT
                            )""".trimIndent()
                        )
                        db.execSQL(
                            "INSERT INTO countdown_tasks (durationMs, status, createdAtEpochMs, endAtEpochMs, remainingAtPauseMs, name) VALUES (60000, 'RUNNING', 1000, 61000, 60000, '测试')"
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        helper.writableDatabase
        helper.close()

        val database = Room.databaseBuilder(context, CountdownDatabase::class.java, databaseName)
            .addMigrations(CountdownDatabase.MIGRATION_2_3)
            .build()
        try {
            assertEquals(false, database.countdownDao().getAll().single().preEndAlertSent)
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }
}
