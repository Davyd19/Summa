package com.app.summa.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.app.summa.data.model.FocusSession
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
    @Insert
    suspend fun insertFocusSession(session: FocusSession): Long

    // Mengambil sesi untuk tugas tertentu
    @Query("SELECT * FROM focus_sessions WHERE taskId = :taskId ORDER BY startTime DESC")
    fun getSessionsForTask(taskId: Long): Flow<List<FocusSession>>

    // Menghitung total klip yang dikumpulkan (untuk gamifikasi global)
    @Query("SELECT SUM(paperclipsCollected) FROM focus_sessions")
    fun getTotalPaperclips(): Flow<Int?>
}