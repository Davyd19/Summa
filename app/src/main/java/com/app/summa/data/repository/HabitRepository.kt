package com.app.summa.data.repository

import com.app.summa.data.local.HabitDao
import com.app.summa.data.model.Habit
import com.app.summa.data.model.HabitLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

interface HabitRepository {
    fun getAllHabits(): Flow<List<Habit>>
    fun getHabitById(id: Long): Flow<Habit?>
    // PENAMBAHAN: Flow untuk log harian
    fun getLogsForDate(date: LocalDate): Flow<List<HabitLog>>
    suspend fun insertHabit(habit: Habit): Long
    suspend fun updateHabit(habit: Habit)
    suspend fun deleteHabit(habit: Habit)
    fun getHabitLogs(habitId: Long): Flow<List<HabitLog>>
    // PERBAIKAN: Mengubah nama fungsi agar lebih jelas
    suspend fun updateHabitCount(habit: Habit, newCount: Int, date: LocalDate = LocalDate.now())
}

class HabitRepositoryImpl @Inject constructor(
    private val habitDao: HabitDao
) : HabitRepository {

    override fun getAllHabits(): Flow<List<Habit>> {
        return habitDao.getAllHabits()
    }

    override fun getHabitById(id: Long): Flow<Habit?> {
        return habitDao.getHabitById(id)
    }

    // PENAMBAHAN: Implementasi flow untuk log harian
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

    // IMPLEMENTASI: Ini adalah logika inti Summa
    override suspend fun updateHabitCount(habit: Habit, newCount: Int, date: LocalDate) {
        // Pindah ke I/O dispatcher untuk operasi database
        withContext(Dispatchers.IO) {
            val dateString = date.toString()
            val existingLog = habitDao.getHabitLogByDate(habit.id, dateString)

            var countDifference = newCount
            if (existingLog != null) {
                // Hitung selisihnya
                countDifference = newCount - existingLog.count
                // Update log yang ada
                habitDao.updateHabitLog(existingLog.copy(count = newCount))
            } else {
                // Buat log baru
                habitDao.insertHabitLog(
                    HabitLog(
                        habitId = habit.id,
                        date = dateString,
                        count = newCount
                    )
                )
            }

            // Hitung ulang streak dan total sum
            val (newCurrentStreak, newPerfectStreak) = calculateStreaks(habit.id, habit.targetCount)

            // Update Habit utama dengan data baru
            habitDao.updateHabit(
                habit.copy(
                    // Tambahkan selisihnya ke totalSum
                    totalSum = (habit.totalSum + countDifference).coerceAtLeast(0),
                    currentStreak = newCurrentStreak,
                    perfectStreak = newPerfectStreak
                )
            )
        }
    }

    // IMPLEMENTASI: Logika perhitungan Streak Fleksibel
    private suspend fun calculateStreaks(habitId: Long, targetCount: Int): Pair<Int, Int> {
        val allLogs = habitDao.getAllLogsForHabit(habitId).associateBy { LocalDate.parse(it.date) }
        var currentDate = LocalDate.now()

        var currentStreak = 0
        var perfectStreak = 0

        // 1. Hitung Streak Konsistensi (🔥) (count > 0)
        // Cek hari ini
        if (allLogs[currentDate]?.count ?: 0 > 0) {
            currentStreak = 1
            var checkDate = currentDate.minusDays(1)
            // Cek hari-hari sebelumnya
            while (allLogs.containsKey(checkDate) && allLogs[checkDate]!!.count > 0) {
                currentStreak++
                checkDate = checkDate.minusDays(1)
            }
        } else {
            // Jika hari ini 0, cek kemarin
            var checkDate = currentDate.minusDays(1)
            while (allLogs.containsKey(checkDate) && allLogs[checkDate]!!.count > 0) {
                currentStreak++
                checkDate = checkDate.minusDays(1)
            }
        }


        // 2. Hitung Streak Sempurna (👑) (count >= targetCount)
        // Cek hari ini
        if (allLogs[currentDate]?.count ?: 0 >= targetCount) {
            perfectStreak = 1
            var checkDate = currentDate.minusDays(1)
            // Cek hari-hari sebelumnya
            while (allLogs.containsKey(checkDate) && allLogs[checkDate]!!.count >= targetCount) {
                perfectStreak++
                checkDate = checkDate.minusDays(1)
            }
        } else {
            // Jika hari ini tidak sempurna, cek kemarin
            var checkDate = currentDate.minusDays(1)
            while (allLogs.containsKey(checkDate) && allLogs[checkDate]!!.count >= targetCount) {
                perfectStreak++
                checkDate = checkDate.minusDays(1)
            }
        }

        return Pair(currentStreak, perfectStreak)
    }
}