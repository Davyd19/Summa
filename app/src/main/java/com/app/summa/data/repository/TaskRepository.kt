package com.app.summa.data.repository

import com.app.summa.data.local.TaskDao
import com.app.summa.data.model.DailyWrapUpResult
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

    // UPDATE: Sekarang mengembalikan DailyWrapUpResult? (Null jika tidak ada kejadian penting)
    suspend fun processDailyWrapUp(): DailyWrapUpResult?
}

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val identityRepository: IdentityRepository,
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

    /**
     * Logika Inti "Sistem Operasi":
     * Mengecek tugas kemarin yang belum selesai.
     * - Aspirasi: Digeser ke hari ini (Rollover).
     * - Komitmen: Dihukum (XP Penalti) & Digeser (Nagging).
     * Mengembalikan laporan untuk ditampilkan di UI.
     */
    override suspend fun processDailyWrapUp(): DailyWrapUpResult? {
        return withContext(Dispatchers.IO) {
            val today = LocalDate.now().toString()

            // Cek tugas yang jadwalnya < hari ini DAN belum selesai
            // (Diasumsikan query DAO getOverdue... sudah memfilter scheduledDate < today)
            val overdueAspirations = taskDao.getOverdueAspirationTasks(today)
            val overdueCommitments = taskDao.getOverdueCommitmentTasks(today)

            // Jika tidak ada tunggakan, tidak perlu briefing
            if (overdueAspirations.isEmpty() && overdueCommitments.isEmpty()) {
                return@withContext null
            }

            // 1. Proses Rollover Aspirasi (Tanpa Penalti)
            overdueAspirations.forEach { task ->
                // Geser tanggal ke hari ini
                taskDao.updateTask(task.copy(scheduledDate = today))
            }

            // 2. Proses Hukuman Komitmen (Penalti XP)
            var totalPenalty = 0
            overdueCommitments.forEach { task ->
                if (task.relatedIdentityId != null) {
                    val penalty = -5
                    totalPenalty += 5 // Kita track angka positif untuk display di UI

                    // Kurangi XP Identitas
                    identityRepository.addVoteToIdentity(
                        identityId = task.relatedIdentityId,
                        points = penalty,
                        note = "Terlewat komitmen: ${task.title}"
                    )
                }
                // Tetap geser tugas agar "menghantui" pengguna (Nagging)
                taskDao.updateTask(task.copy(scheduledDate = today))
            }

            // 3. Kembalikan Hasil Laporan
            return@withContext DailyWrapUpResult(
                processedDate = LocalDate.now().minusDays(1).toString(),
                rolledOverAspirations = overdueAspirations,
                missedCommitments = overdueCommitments,
                totalPenalty = totalPenalty
            )
        }
    }
}