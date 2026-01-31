package com.app.summa.data.repository

import com.app.summa.data.local.TaskDao
import com.app.summa.data.model.DailyWrapUpResult
import com.app.summa.data.model.Task
import com.app.summa.util.NotificationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeParseException
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
            val todayDate = LocalDate.now()
            val todayString = todayDate.toString()

            // PERBAIKAN 1: Ambil semua tugas aktif dulu, lalu filter tanggal dengan LocalDate di memori
            // Mengandalkan string comparison di SQL bisa berisiko jika format tanggal tercampur
            // OPTIMISASI: Gunakan getActiveTasksSync untuk memfilter di SQL level, menghemat memori
            val activeTasks = taskDao.getActiveTasksSync()

            val overdueTasks = activeTasks.filter { task ->
                try {
                    val taskDate = LocalDate.parse(task.scheduledDate)
                    taskDate.isBefore(todayDate)
                } catch (e: DateTimeParseException) {
                    false // Abaikan jika format tanggal salah
                } catch (e: NullPointerException) {
                    false // Abaikan jika tanggal null
                }
            }

            val overdueAspirations = overdueTasks.filter { !it.isCommitment }
            val overdueCommitments = overdueTasks.filter { it.isCommitment }

            // Jika tidak ada tunggakan, tidak perlu briefing
            if (overdueAspirations.isEmpty() && overdueCommitments.isEmpty()) {
                return@withContext null
            }

            // 1. Proses Rollover Aspirasi (Tanpa Penalti)
            if (overdueAspirations.isNotEmpty()) {
                overdueAspirations.map { it.id }.chunked(500).forEach { batchIds ->
                    taskDao.updateTasksScheduledDate(batchIds, todayString)
                }
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
            }

            // Tetap geser tugas agar "menghantui" pengguna (Nagging)
            if (overdueCommitments.isNotEmpty()) {
                overdueCommitments.map { it.id }.chunked(500).forEach { batchIds ->
                    taskDao.updateTasksScheduledDate(batchIds, todayString)
                }
            }

            // 3. Kembalikan Hasil Laporan
            return@withContext DailyWrapUpResult(
                processedDate = todayDate.minusDays(1).toString(),
                rolledOverAspirations = overdueAspirations,
                missedCommitments = overdueCommitments,
                totalPenalty = totalPenalty
            )
        }
    }
}