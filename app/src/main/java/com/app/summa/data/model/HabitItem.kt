package com.app.summa.data.model // SEBELUMNYA MUNGKIN: com.app.summa.ui.model

import com.app.summa.data.model.Habit

// Ini adalah model UI yang kita gunakan
data class HabitItem(
    val id: Long,
    val name: String,
    val icon: String,
    val currentCount: Int,
    val targetCount: Int,
    val totalSum: Int,
    val currentStreak: Int,
    val perfectStreak: Int,
    val originalModel: Habit
)