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
    fun getLogsForDate(date: LocalDate): Flow<List<HabitLog>>
    suspend fun insertHabit(habit: Habit): Long
    suspend fun updateHabit(habit: Habit)
    suspend fun deleteHabit(habit: Habit)
    fun getHabitLogs(habitId: Long): Flow<List<HabitLog>>
    suspend fun updateHabitCount(habit: Habit, newCount: Int, date: LocalDate = LocalDate.now())
}

// --- PERUBAHAN IMPLEMENTASI ---
class HabitRepositoryImpl @Inject constructor(
    private val habitDao: HabitDao,
    // --- PENAMBAHAN ---
    // Kita butuh IdentityRepository untuk voting otomatis
    private val identityRepository: IdentityRepository
    // -------------------
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

            // --- LOGIKA VOTING OTOMATIS ---
            val isTargetMet = habit.targetCount > 0 && newCount >= habit.targetCount
            val wasTargetMetBefore = existingLog != null && existingLog.count >= habit.targetCount

            // Jika target baru saja tercapai (sebelumnya belum) dan ada ID identitas
            if (isTargetMet && !wasTargetMetBefore && habit.relatedIdentityId != null && date.isEqual(LocalDate.now())) {
                identityRepository.addVoteToIdentity(
                    identityId = habit.relatedIdentityId,
                    points = 5, // Anda bisa sesuaikan poinnya
                    note = "Otomatis dari penyelesaian kebiasaan: ${habit.name}" // Jurnal otomatis!
                )
            }
            // --------------------------------

            val (newCurrentStreak, newPerfectStreak) = calculateStreaks(habit.id, habit.targetCount)

            habitDao.updateHabit(
                habit.copy(
                    totalSum = (habit.totalSum + countDifference).coerceAtLeast(0),
                    currentStreak = newCurrentStreak,
                    perfectStreak = newPerfectStreak
                )
            )
        }
    }

    private suspend fun calculateStreaks(habitId: Long, targetCount: Int): Pair<Int, Int> {
        val allLogs = habitDao.getAllLogsForHabit(habitId).associateBy { LocalDate.parse(it.date) }
        var currentDate = LocalDate.now()

        var currentStreak = 0
        var perfectStreak = 0

        if (allLogs[currentDate]?.count ?: 0 > 0) {
            currentStreak = 1
            var checkDate = currentDate.minusDays(1)
            while (allLogs.containsKey(checkDate) && allLogs[checkDate]!!.count > 0) {
                currentStreak++
                checkDate = checkDate.minusDays(1)
            }
        } else {
            var checkDate = currentDate.minusDays(1)
            while (allLogs.containsKey(checkDate) && allLogs[checkDate]!!.count > 0) {
                currentStreak++
                checkDate = checkDate.minusDays(1)
            }
        }

        if (allLogs[currentDate]?.count ?: 0 >= targetCount) {
            perfectStreak = 1
            var checkDate = currentDate.minusDays(1)
            while (allLogs.containsKey(checkDate) && allLogs[checkDate]!!.count >= targetCount) {
                perfectStreak++
                checkDate = checkDate.minusDays(1)
            }
        } else {
            var checkDate = currentDate.minusDays(1)
            while (allLogs.containsKey(checkDate) && allLogs[checkDate]!!.count >= targetCount) {
                perfectStreak++
                checkDate = checkDate.minusDays(1)
            }
        }

        return Pair(currentStreak, perfectStreak)
    }
}