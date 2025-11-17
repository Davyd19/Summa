package com.app.summa.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val twoMinuteAction: String = "",
    val isCommitment: Boolean = true,
    val scheduledDate: String?, // yyyy-MM-dd
    val scheduledTime: String?, // HH:mm
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    val createdAt: Long? = null
)