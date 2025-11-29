package com.app.summa.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val icon: String = "🎯",
    val targetCount: Int = 1,
    val totalSum: Int = 0,
    val currentStreak: Int = 0,
    val perfectStreak: Int = 0,
    val relatedIdentityId: Long? = null,

    // --- KOLOM BARU UNTUK INPUT LENGKAP ---
    val description: String = "", // Alasan/Why
    val frequency: String = "DAILY", // "DAILY", "WEEKLY", "SPECIFIC"
    val specificDays: String = "", // "MON,WED,FRI" jika frequency SPECIFIC
    val reminderTime: String = "", // "08:00"
    val cue: String = "", // "Setelah saya minum kopi pagi..." (Atomic Habits Concept)
    // ---------------------------------------

    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    val createdAt: Long? = null
)

@Entity(tableName = "habit_logs")
data class HabitLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val habitId: Long,
    val date: String,
    val count: Int,
    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    val timestamp: Long? = null
)