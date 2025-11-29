package com.app.summa.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity untuk menyimpan riwayat sesi fokus.
 * Sekarang mendukung Task maupun Habit.
 */
@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    // Diubah jadi nullable agar bisa kosong jika ini sesi Habit
    val taskId: Long? = null,
    // Field baru untuk sesi Habit
    val habitId: Long? = null,
    val startTime: Long,
    val endTime: Long,
    val paperclipsCollected: Int,
    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    val createdAt: Long? = null
)