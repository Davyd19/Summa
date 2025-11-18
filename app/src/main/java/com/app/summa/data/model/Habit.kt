package com.app.summa.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val icon: String = "🎯",
    val targetCount: Int = 3,
    val totalSum: Int = 0,
    val currentStreak: Int = 0,
    val perfectStreak: Int = 0,
    // --- PENAMBAHAN ---
    // Kolom ini akan menghubungkan kebiasaan ke identitas
    // Contoh: Habit "Lari Pagi" terhubung ke Identity "Orang Sehat"
    val relatedIdentityId: Long? = null,
    // -------------------
    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    val createdAt: Long? = null
)

@Entity(tableName = "habit_logs")
data class HabitLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val habitId: Long,
    val date: String, // Format: yyyy-MM-dd
    val count: Int,
    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    val timestamp: Long? = null
)