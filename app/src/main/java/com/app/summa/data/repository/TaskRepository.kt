package com.app.summa.data.repository

import com.app.summa.data.local.TaskDao
import com.app.summa.data.model.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

interface TaskRepository {
    fun getActiveTasks(): Flow<List<Task>>
    fun getTasksByDate(date: LocalDate): Flow<List<Task>>
    fun getTasksForDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Task>>
    suspend fun insertTask(task: Task): Long
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(task: Task)
    suspend fun completeTask(taskId: Long)

    // Fungsi pintar untuk memindahkan aspirasi yang tertunda
    suspend fun processDailyWrapUp()
}

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao
) : TaskRepository {

    override fun getActiveTasks(): Flow<List<Task>> {
        return taskDao.getActiveTasks()
    }

    override fun getTasksByDate(date: LocalDate): Flow<List<Task>> {
        return taskDao.getTasksByDate(date.toString())
    }

    override fun getTasksForDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Task>> {
        return taskDao.getTasksForDateRange(startDate.toString(), endDate.toString())
    }

    override suspend fun insertTask(task: Task): Long {
        return taskDao.insertTask(task)
    }

    override suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
    }

    override suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
    }

    override suspend fun completeTask(taskId: Long) {
        taskDao.completeTask(taskId, System.currentTimeMillis())
    }

    // --- IMPLEMENTASI PLANNER CERDAS (REAL APP OPTIMIZED) ---
    override suspend fun processDailyWrapUp() {
        withContext(Dispatchers.IO) {
            val today = LocalDate.now().toString()

            // 1. Ambil tugas aspirasi yang terlewat menggunakan query spesifik (Sangat Efisien)
            val overdueAspirationTasks = taskDao.getOverdueAspirationTasks(today)

            // 2. Pindahkan tanggalnya ke hari ini secara otomatis
            // Dalam aplikasi skala besar, ini bisa dilakukan dengan @Query UPDATE batch,
            // tapi loop updateTask ini sudah cukup baik untuk penggunaan personal (ratusan task).
            overdueAspirationTasks.forEach { task ->
                taskDao.updateTask(task.copy(scheduledDate = today))
            }
        }
    }
}