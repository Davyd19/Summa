package com.app.summa.data.repository

import com.app.summa.data.local.TaskDao
import com.app.summa.data.model.Task
import com.app.summa.util.NotificationScheduler
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
    private val identityRepository: IdentityRepository,
    // PERBAIKAN: Inject NotificationScheduler untuk notifikasi hukuman
    private val notificationScheduler: NotificationScheduler
) : TaskRepository {

    override fun getActiveTasks(): Flow<List<Task>> = taskDao.getActiveTasks()
    override fun getTasksByDate(date: LocalDate): Flow<List<Task>> = taskDao.getTasksByDate(date.toString())
    override fun getTasksForDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Task>> = taskDao.getTasksForDateRange(startDate.toString(), endDate.toString())
    override suspend fun insertTask(task: Task): Long = taskDao.insertTask(task)
    override suspend fun updateTask(task: Task) = taskDao.updateTask(task)
    override suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)

    override suspend fun completeTask(taskId: Long) {
        withContext(Dispatchers.IO) {
            val task = taskDao.getTaskById(taskId) ?: return@withContext
            taskDao.completeTask(taskId, System.currentTimeMillis())

            if (task.relatedIdentityId != null) {
                val points = if (task.isCommitment) 10 else 5
                identityRepository.addVoteToIdentity(
                    identityId = task.relatedIdentityId,
                    points = points,
                    note = "Menelesaikan tugas: ${task.title}"
                )
            }
        }
    }

    override suspend fun processDailyWrapUp() {
        withContext(Dispatchers.IO) {
            val today = LocalDate.now().toString()

            // 1. Rollover Aspirasi (Tanpa Penalti)
            val overdueAspirations = taskDao.getOverdueAspirationTasks(today)
            overdueAspirations.forEach { task ->
                taskDao.updateTask(task.copy(scheduledDate = today))
            }

            // 2. Hukuman Komitmen (Penalti XP)
            val overdueCommitments = taskDao.getOverdueCommitmentTasks(today)
            var totalPenalty = 0

            overdueCommitments.forEach { task ->
                if (task.relatedIdentityId != null) {
                    val penalty = -5
                    totalPenalty += 5 // Track positive value for display

                    identityRepository.addVoteToIdentity(
                        identityId = task.relatedIdentityId,
                        points = penalty,
                        note = "Terlewat komitmen: ${task.title}"
                    )
                }
                // Pindahkan tugas agar tetap terlihat (Nagging)
                taskDao.updateTask(task.copy(scheduledDate = today))
            }

            // 3. Notifikasi Pengguna jika ada penalti
            if (totalPenalty > 0) {
                notificationScheduler.showImmediateNotification(
                    title = "Komitmen Terlewat ⚠️",
                    message = "Anda kehilangan $totalPenalty poin identitas hari ini."
                )
            }
        }
    }
}