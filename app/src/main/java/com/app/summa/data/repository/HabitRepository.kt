package com.app.summa.data.repository

import com.app.summa.data.local.HabitDao
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
    private val identityRepository: IdentityRepository
) : HabitRepository {

    override fun getAllHabits(): Flow<List<Habit>> {
        return habitDao.getAllHabits()
    }

    override fun getHabitById(id: Long): Flow<Habit?> {
        return habitDao.getHabitById(id)
    }

    override fun getLogsForDate(date: LocalDate): Flow<List<HabitLog>> {
        return habitDao.getLogsForDate(date.toString())
    }

    override suspend fun insertHabit(habit: Habit): Long {
        return habitDao.insertHabit(habit)
    }

    override suspend fun updateHabit(habit: Habit) {
        habitDao.updateHabit(habit)
    }

    override suspend fun deleteHabit(habit: Habit) {
        habitDao.deleteHabit(habit)
    }

    override fun getHabitLogs(habitId: Long): Flow<List<HabitLog>> {
        return habitDao.getHabitLogs(habitId)
    }

    override suspend fun updateHabitCount(habit: Habit, newCount: Int, date: LocalDate) {
        withContext(Dispatchers.IO) {
            val dateString = date.toString()
            val existingLog = habitDao.getHabitLogByDate(habit.id, dateString)

            var countDifference = newCount
            if (existingLog != null) {
                countDifference = newCount - existingLog.count
                habitDao.updateHabitLog(existingLog.copy(count = newCount))
            } else {
                habitDao.insertHabitLog(
                    HabitLog(
                        habitId = habit.id,
                        date = dateString,
                        count = newCount
                    )
                )
            }

            // --- LOGIKA VOTING OTOMATIS (SAMA SEPERTI SEBELUMNYA) ---
            val isTargetMet = habit.targetCount > 0 && newCount >= habit.targetCount
            val wasTargetMetBefore = existingLog != null && existingLog.count >= habit.targetCount

            if (isTargetMet && !wasTargetMetBefore && habit.relatedIdentityId != null && date.isEqual(LocalDate.now())) {
                identityRepository.addVoteToIdentity(
                    identityId = habit.relatedIdentityId,
                    points = 5,
                    note = "Otomatis dari penyelesaian kebiasaan: ${habit.name}"
                )
            }
            // --------------------------------

            // HITUNG STREAK DENGAN LOGIKA BARU
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

    /**
     * LOGIKA STREAK FLEKSIBEL:
     * 1. Streak "Api" (Konsistensi): Mengizinkan bolong 1 hari (Aturan "Don't Miss Twice").
     * 2. Streak "Mahkota" (Sempurna): Harus berturut-turut tanpa celah.
     */
    private suspend fun calculateAdvancedStreaks(habitId: Long, targetCount: Int): Pair<Int, Int> {
        val allLogs = habitDao.getAllLogsForHabit(habitId)
            .sortedByDescending { LocalDate.parse(it.date) }

        if (allLogs.isEmpty()) return Pair(0, 0)

        val today = LocalDate.now()

        // --- 1. HITUNG PERFECT STREAK (STRICT) ---
        var perfectStreak = 0
        var checkDatePerfect = today

        // Cek hari ini dulu
        val todayLogPerfect = allLogs.find { it.date == checkDatePerfect.toString() }
        if ((todayLogPerfect?.count ?: 0) >= targetCount) {
            perfectStreak++
            checkDatePerfect = checkDatePerfect.minusDays(1)

            // Cek hari-hari sebelumnya secara ketat
            while (true) {
                val log = allLogs.find { it.date == checkDatePerfect.toString() }
                if (log != null && log.count >= targetCount) {
                    perfectStreak++
                    checkDatePerfect = checkDatePerfect.minusDays(1)
                } else {
                    break // Putus kalau ada bolong
                }
            }
        } else {
            // Jika hari ini belum selesai, cek kemarin
            checkDatePerfect = today.minusDays(1)
            while (true) {
                val log = allLogs.find { it.date == checkDatePerfect.toString() }
                if (log != null && log.count >= targetCount) {
                    perfectStreak++
                    checkDatePerfect = checkDatePerfect.minusDays(1)
                } else {
                    break
                }
            }
        }

        // --- 2. HITUNG FLEXIBLE STREAK (DON'T MISS TWICE) ---
        var flexibleStreak = 0

        // Kita gunakan list log yang sudah urut descending
        // Filter hanya hari yang ada aktivitas (count > 0)
        val activeLogs = allLogs.filter { it.count > 0 }
            .map { LocalDate.parse(it.date) }

        if (activeLogs.isNotEmpty()) {
            val mostRecentActive = activeLogs[0]
            val daysSinceLastActive = ChronoUnit.DAYS.between(mostRecentActive, today)

            // Jika terakhir aktif lebih dari 1 hari yang lalu (misal kemarin lusa), streak putus.
            // (Ingat: Gap 1 hari boleh. Jadi kalau terakhir aktif Kemarin (gap=1), masih aman.
            // Kalau Lusa (gap=2), streak putus).
            if (daysSinceLastActive <= 1) {
                flexibleStreak = 1
                var currentDateRef = mostRecentActive

                for (i in 1 until activeLogs.size) {
                    val prevDate = activeLogs[i]
                    val gap = ChronoUnit.DAYS.between(prevDate, currentDateRef)

                    if (gap <= 2) {
                        // Gap <= 2 artinya bolong maksimal 1 hari (Hari H - Hari H-2 = 2 hari)
                        // Contoh: Senin (aktif) -> Rabu (aktif). Gap = 2 hari. Selasa bolong.
                        // Karena aturan "Don't Miss Twice", ini masih nyambung.
                        flexibleStreak++
                        currentDateRef = prevDate
                    } else {
                        // Gap > 2 artinya bolong 2 hari atau lebih. Putus.
                        break
                    }
                }
            } else {
                flexibleStreak = 0
            }
        }

        return Pair(flexibleStreak, perfectStreak)
    }
}