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
    suspend fun processDailyWrapUp()
}

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val identityRepository: IdentityRepository
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
        withContext(Dispatchers.IO) {
            val task = taskDao.getTaskById(taskId)
            taskDao.completeTask(taskId, System.currentTimeMillis())

            if (task != null && task.relatedIdentityId != null) {
                val points = if (task.isCommitment) 10 else 5
                identityRepository.addVoteToIdentity(
                    identityId = task.relatedIdentityId,
                    points = points,
                    note = "Menelesaikan tugas: ${task.title}"
                )
            }
        }
    }

    // UPDATE: Logika Wrap-Up Harian
    override suspend fun processDailyWrapUp() {
        withContext(Dispatchers.IO) {
            val today = LocalDate.now().toString()

            // 1. Tangani Aspirasi (Pindah otomatis, tanpa penalti)
            val overdueAspirations = taskDao.getOverdueAspirationTasks(today)
            overdueAspirations.forEach { task ->
                taskDao.updateTask(task.copy(scheduledDate = today))
            }

            // 2. Tangani Komitmen (Tidak dipindah, tapi diberi penalti)
            val overdueCommitments = taskDao.getOverdueCommitmentTasks(today)
            overdueCommitments.forEach { task ->
                // Jika tugas ini belum pernah diproses penaltinya (opsional: bisa tambah field flag di DB)
                // Untuk sekarang, kita asumsi sistem ini jalan sekali sehari.

                if (task.relatedIdentityId != null) {
                    // HUKUMAN: Kurangi 5 poin karena melanggar komitmen
                    identityRepository.addVoteToIdentity(
                        identityId = task.relatedIdentityId,
                        points = -5,
                        note = "Terlewat komitmen: ${task.title}"
                    )
                }

                // Pindahkan ke hari ini agar user tetap melihatnya (sebagai hutang)
                taskDao.updateTask(task.copy(scheduledDate = today))
            }
        }
    }
}