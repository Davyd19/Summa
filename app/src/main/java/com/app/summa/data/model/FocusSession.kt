package com.app.summa.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity untuk menyimpan riwayat sesi fokus (Paperclip Mode).
 * Menyimpan durasi dan jumlah klip yang berhasil dipindahkan.
 */
@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long, // Terhubung ke tugas tertentu
    val startTime: Long,
    val endTime: Long,
    val paperclipsCollected: Int,
    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    val createdAt: Long? = null
)