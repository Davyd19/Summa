package com.app.summa.ui.model

import com.app.summa.data.model.Habit

// Ini adalah model UI yang kita pindahkan dari HabitsScreen
// agar bisa digunakan bersama oleh DashboardViewModel dan HabitsViewModel
data class HabitItem(
    val id: Long,
    val name: String,
    val icon: String,
    val currentCount: Int,
    val targetCount: Int,
    val totalSum: Int,
    val currentStreak: Int,
    val perfectStreak: Int,
    val originalModel: Habit // Referensi ke model database
)