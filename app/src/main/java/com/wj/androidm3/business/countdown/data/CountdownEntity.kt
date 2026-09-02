package com.wj.androidm3.business.countdown.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "countdown_tasks")
data class CountdownEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val durationMs: Long,
    val status: CountdownStatus,
    val createdAtEpochMs: Long,
    val endAtEpochMs: Long? = null,
    val remainingAtPauseMs: Long = durationMs,
    val name: String? = null,
    val screenshotPath: String? = null,
    @ColumnInfo(defaultValue = "0")
    val preEndAlertSent: Boolean = false
) {
    val displayName: String
        get() = name?.trim()?.takeIf { it.isNotEmpty() } ?: "倒计时 #$id"
}
