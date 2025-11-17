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

    // PENAMBAHAN: Query untuk mengambil satu task
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Long): Task?

    // PENAMBAHAN: Query untuk menyelesaikan task
    @Query("UPDATE tasks SET isCompleted = 1, completedAt = :completedAt WHERE id = :taskId")
    suspend fun completeTask(taskId: Long, completedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)
}