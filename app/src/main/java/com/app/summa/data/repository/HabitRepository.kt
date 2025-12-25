package com.app.summa.data.repository

import androidx.room.withTransaction
import com.app.summa.data.local.HabitDao
import com.app.summa.data.local.SummaDatabase
import com.app.summa.data.model.Habit
import com.app.summa.data.model.HabitLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

interface HabitRepository {
    fun getAllHabits(): Flow<List<Habit>>
    fun getHabitById(id: Long): Flow<Habit?>
    fun getLogsForDate(date: LocalDate): Flow<List<HabitLog>>
    suspend fun insertHabit(habit: Habit): Long
    suspend fun updateHabit(habit: Habit)
    suspend fun deleteHabit(habit: Habit)
    fun getHabitLogs(habitId: Long): Flow<List<HabitLog>>
    suspend fun updateHabitCount(habit: Habit, newCount: Int, date: LocalDate = LocalDate.now())
}

class HabitRepositoryImpl @Inject constructor(
    private val habitDao: HabitDao,
    private val identityRepository: IdentityRepository,
    private val database: SummaDatabase // Inject Database untuk Transaction
) : HabitRepository {

    override fun getAllHabits(): Flow<List<Habit>> = habitDao.getAllHabits()
    override fun getHabitById(id: Long): Flow<Habit?> = habitDao.getHabitById(id)
    override fun getLogsForDate(date: LocalDate): Flow<List<HabitLog>> = habitDao.getLogsForDate(date.toString())
    override suspend fun insertHabit(habit: Habit): Long = habitDao.insertHabit(habit)
    override suspend fun updateHabit(habit: Habit) = habitDao.updateHabit(habit)
    override suspend fun deleteHabit(habit: Habit) = habitDao.deleteHabit(habit)
    override fun getHabitLogs(habitId: Long): Flow<List<HabitLog>> = habitDao.getHabitLogs(habitId)

    override suspend fun updateHabitCount(habit: Habit, newCount: Int, date: LocalDate) {
        // PERBAIKAN: Gunakan withTransaction untuk Atomic Operation
        database.withTransaction {
            val dateString = date.toString()
            val existingLog = habitDao.getHabitLogByDate(habit.id, dateString)
            var countDifference = newCount
            val oldCount = existingLog?.count ?: 0

            if (existingLog != null) {
                countDifference = newCount - existingLog.count
                habitDao.updateHabitLog(existingLog.copy(count = newCount))
            } else {
                habitDao.insertHabitLog(HabitLog(habitId = habit.id, date = dateString, count = newCount))
            }

            // --- PERBAIKAN LOGIKA GAMIFIKASI (ANTI-CHEAT) ---
            if (habit.relatedIdentityId != null && date.isEqual(LocalDate.now())) {
                val target = habit.targetCount

                // Hanya proses reward jika count BERTAMBAH (cegah spam +/-)
                if (newCount > oldCount) {
                    // 1. Reward Mencapai Target (Hanya jika baru saja menyentuh/melewati target dari bawah)
                    if (target > 0 && newCount >= target && oldCount < target) {
                        identityRepository.addVoteToIdentity(
                            identityId = habit.relatedIdentityId,
                            points = 5,
                            note = "Target tercapai: ${habit.name}"
                        )
                    }

                    // 2. Reward Over-achievement
                    // Syarat diperketat: Harus di atas target DAN user memang menambah progress
                    if (target > 0 && newCount > target) {
                        val bonusPoints = (newCount - oldCount) * 2
                        identityRepository.addVoteToIdentity(
                            identityId = habit.relatedIdentityId,
                            points = bonusPoints,
                            note = "Ekstra usaha: ${habit.name}"
                        )
                    }
                }
            }

            // Hitung Streak (Optimasi: Bisa dipindah ke query SQL kedepannya)
            val (newCurrentStreak, newPerfectStreak) = calculateAdvancedStreaks(habit.id, habit.targetCount)

            habitDao.updateHabit(
                habit.copy(
                    totalSum = (habit.totalSum + countDifference).coerceAtLeast(0),
                    currentStreak = newCurrentStreak,
                    perfectStreak = newPerfectStreak
                )
            )
        }
    }

    private suspend fun calculateAdvancedStreaks(habitId: Long, targetCount: Int): Pair<Int, Int> {
        // PERBAIKAN PERFORMANCE: Limit log yang diambil jika user sudah lama pakai app
        // Saat ini masih logic manual, tapi aman dijalankan di background thread (via Repository scope)
        return withContext(Dispatchers.Default) {
            val allLogs = habitDao.getAllLogsForHabit(habitId).sortedByDescending { LocalDate.parse(it.date) }
            if (allLogs.isEmpty()) return@withContext Pair(0, 0)

            val today = LocalDate.now()
            var perfectStreak = 0
            var checkDatePerfect = today

            // PERBAIKAN: Tambahkan safety counter untuk mencegah infinite loop
            var safetyCounter = 0
            val maxIterations = 366 // Maksimal cek 1 tahun ke belakang

            // 1. Perfect Streak Logic
            val todayLogPerfect = allLogs.find { it.date == checkDatePerfect.toString() }
            if ((todayLogPerfect?.count ?: 0) >= targetCount) {
                perfectStreak++
                checkDatePerfect = checkDatePerfect.minusDays(1)

                while (safetyCounter < maxIterations) {
                    val currentDateStr = checkDatePerfect.toString()
                    val log = allLogs.find { it.date == currentDateStr }
                    if (log != null && log.count >= targetCount) {
                        perfectStreak++
                        checkDatePerfect = checkDatePerfect.minusDays(1)
                    } else {
                        break
                    }
                    safetyCounter++
                }
            } else {
                checkDatePerfect = today.minusDays(1)
                while (safetyCounter < maxIterations) {
                    val currentDateStr = checkDatePerfect.toString()
                    val log = allLogs.find { it.date == currentDateStr }
                    if (log != null && log.count >= targetCount) {
                        perfectStreak++
                        checkDatePerfect = checkDatePerfect.minusDays(1)
                    } else {
                        break
                    }
                    safetyCounter++
                }
            }

            // 2. Flexible Streak Logic
            var flexibleStreak = 0
            val activeLogs = allLogs.filter { it.count > 0 }.map { LocalDate.parse(it.date) }

            if (activeLogs.isNotEmpty()) {
                val mostRecentActive = activeLogs[0]
                val daysSinceLastActive = ChronoUnit.DAYS.between(mostRecentActive, today)

                // Jika aktif hari ini atau kemarin, streak masih hidup
                if (daysSinceLastActive <= 1) {
                    flexibleStreak = 1
                    var currentDateRef = mostRecentActive
                    // Loop aman karena dibatasi ukuran list activeLogs
                    for (i in 1 until activeLogs.size) {
                        val prevDate = activeLogs[i]
                        val gap = ChronoUnit.DAYS.between(prevDate, currentDateRef)
                        // Fleksibel: Boleh bolong 1 hari (gap <= 2 hari)
                        if (gap <= 2) {
                            flexibleStreak++
                            currentDateRef = prevDate
                        } else {
                            break
                        }
                    }
                }
            }

            Pair(flexibleStreak, perfectStreak)
        }
    }
}