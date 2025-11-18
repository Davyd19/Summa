package com.app.summa.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.app.summa.data.model.Task

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY scheduledDate, scheduledTime")
    fun getActiveTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE scheduledDate = :date ORDER BY scheduledTime")
    fun getTasksByDate(date: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE scheduledDate BETWEEN :startDate AND :endDate ORDER BY scheduledDate, scheduledTime")
    fun getTasksForDateRange(startDate: String, endDate: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Long): Task?

    // --- QUERY BARU UNTUK FITUR CERDAS ---
    // Mengambil tugas yang:
    // 1. Belum selesai (isCompleted = 0)
    // 2. Tanggalnya sebelum hari ini (scheduledDate < :today)
    // 3. Bukan komitmen (isCommitment = 0) -> Aspirasi
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND scheduledDate < :today AND isCommitment = 0")
    suspend fun getOverdueAspirationTasks(today: String): List<Task>
    // -------------------------------------

    @Query("UPDATE tasks SET isCompleted = 1, completedAt = :completedAt WHERE id = :taskId")
    suspend fun completeTask(taskId: Long, completedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)
}