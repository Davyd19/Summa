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

    override fun getAllHabits(): Flow<List<Habit>> = habitDao.getAllHabits()
    override fun getHabitById(id: Long): Flow<Habit?> = habitDao.getHabitById(id)
    override fun getLogsForDate(date: LocalDate): Flow<List<HabitLog>> = habitDao.getLogsForDate(date.toString())
    override suspend fun insertHabit(habit: Habit): Long = habitDao.insertHabit(habit)
    override suspend fun updateHabit(habit: Habit) = habitDao.updateHabit(habit)
    override suspend fun deleteHabit(habit: Habit) = habitDao.deleteHabit(habit)
    override fun getHabitLogs(habitId: Long): Flow<List<HabitLog>> = habitDao.getHabitLogs(habitId)

    override suspend fun updateHabitCount(habit: Habit, newCount: Int, date: LocalDate) {
        withContext(Dispatchers.IO) {
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

            // --- PERBAIKAN: GAMIFIKASI ADVANCED ---
            if (habit.relatedIdentityId != null && date.isEqual(LocalDate.now())) {
                val target = habit.targetCount

                // 1. Reward Mencapai Target (Standard)
                if (target > 0 && newCount >= target && oldCount < target) {
                    identityRepository.addVoteToIdentity(
                        identityId = habit.relatedIdentityId,
                        points = 5,
                        note = "Target tercapai: ${habit.name}"
                    )
                }

                // 2. Reward Over-achievement (Bonus XP)
                // Jika user melakukan LEBIH dari target (misal target 5, skrg 6, 7, dst)
                if (target > 0 && newCount > target && newCount > oldCount) {
                    // Beri bonus 2 poin untuk setiap repetisi ekstra
                    val bonusPoints = (newCount - oldCount) * 2
                    identityRepository.addVoteToIdentity(
                        identityId = habit.relatedIdentityId,
                        points = bonusPoints,
                        note = "OVER-ACHIEVEMENT! 👑 Ekstra usaha di ${habit.name}"
                    )
                }
            }

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
        val allLogs = habitDao.getAllLogsForHabit(habitId).sortedByDescending { LocalDate.parse(it.date) }
        if (allLogs.isEmpty()) return Pair(0, 0)

        val today = LocalDate.now()
        var perfectStreak = 0
        var checkDatePerfect = today

        // 1. Perfect Streak Logic
        val todayLogPerfect = allLogs.find { it.date == checkDatePerfect.toString() }
        if ((todayLogPerfect?.count ?: 0) >= targetCount) {
            perfectStreak++
            checkDatePerfect = checkDatePerfect.minusDays(1)
            while (true) {
                val log = allLogs.find { it.date == checkDatePerfect.toString() }
                if (log != null && log.count >= targetCount) {
                    perfectStreak++
                    checkDatePerfect = checkDatePerfect.minusDays(1)
                } else break
            }
        } else {
            checkDatePerfect = today.minusDays(1)
            while (true) {
                val log = allLogs.find { it.date == checkDatePerfect.toString() }
                if (log != null && log.count >= targetCount) {
                    perfectStreak++
                    checkDatePerfect = checkDatePerfect.minusDays(1)
                } else break
            }
        }

        // 2. Flexible Streak Logic (Don't Miss Twice)
        var flexibleStreak = 0
        val activeLogs = allLogs.filter { it.count > 0 }.map { LocalDate.parse(it.date) }

        if (activeLogs.isNotEmpty()) {
            val mostRecentActive = activeLogs[0]
            val daysSinceLastActive = ChronoUnit.DAYS.between(mostRecentActive, today)

            if (daysSinceLastActive <= 1) {
                flexibleStreak = 1
                var currentDateRef = mostRecentActive
                for (i in 1 until activeLogs.size) {
                    val prevDate = activeLogs[i]
                    val gap = ChronoUnit.DAYS.between(prevDate, currentDateRef)
                    if (gap <= 2) { // Gap <= 2 means max 1 day missed
                        flexibleStreak++
                        currentDateRef = prevDate
                    } else break
                }
            }
        }

        return Pair(flexibleStreak, perfectStreak)
    }
}