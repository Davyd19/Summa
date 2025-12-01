package com.app.summa.data.model

/**
 * Model data untuk menyimpan hasil pemrosesan sistem harian.
 * Digunakan untuk menampilkan "Morning Briefing" kepada pengguna.
 */
data class DailyWrapUpResult(
    val processedDate: String,        // Tanggal yang diproses (biasanya "kemarin")
    val rolledOverAspirations: List<Task>, // Daftar tugas aspirasi yang otomatis digeser
    val missedCommitments: List<Task>,     // Daftar komitmen yang gagal diselesaikan
    val totalPenalty: Int             // Total XP yang dikurangi dari identitas
)