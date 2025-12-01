package com.app.summa.data.model

/**
 * Event yang dipancarkan ketika identitas naik level.
 * Ditangkap oleh MainViewModel untuk memicu animasi UI.
 */
data class LevelUpEvent(
    val identityName: String,
    val newLevel: Int,
    val previousLevel: Int
)