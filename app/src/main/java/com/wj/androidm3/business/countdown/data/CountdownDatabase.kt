package com.wj.androidm3.business.countdown.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [CountdownEntity::class], version = 3, exportSchema = false)
@TypeConverters(CountdownConverters::class)
abstract class CountdownDatabase : RoomDatabase() {
    abstract fun countdownDao(): CountdownDao

    companion object {
        @Volatile
        private var instance: CountdownDatabase? = null

        fun getInstance(context: Context): CountdownDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CountdownDatabase::class.java,
                    "countdown_assistant.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE countdown_tasks ADD COLUMN name TEXT")
                db.execSQL("ALTER TABLE countdown_tasks ADD COLUMN screenshotPath TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE countdown_tasks ADD COLUMN preEndAlertSent INTEGER NOT NULL DEFAULT 0"
                )
            }
        }
    }
}
