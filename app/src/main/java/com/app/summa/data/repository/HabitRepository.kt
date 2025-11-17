package com.app.summa.data.repository

import com.app.summa.data.local.HabitDao
import com.app.summa.data.model.Habit
import com.app.summa.data.model.HabitLog
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

interface HabitRepository {
    fun getAllHabits(): Flow<List<Habit>>
    fun getHabitById(id: Long): Flow<Habit?>
    suspend fun insertHabit(habit: Habit): Long
    suspend fun updateHabit(habit: Habit)
    suspend fun deleteHabit(habit: Habit)
    fun getHabitLogs(habitId: Long): Flow<List<HabitLog>>
    suspend fun logHabitCompletion(habitId: Long, count: Int, date: LocalDate = LocalDate.now())
    suspend fun calculateStreaks(habitId: Long): Pair<Int, Int> // current, perfect
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

    override suspend fun logHabitCompletion(habitId: Long, count: Int, date: LocalDate) {
        val dateString = date.toString()
        val existingLog = habitDao.getHabitLogByDate(habitId, dateString)

        if (existingLog != null) {
            habitDao.updateHabitLog(existingLog.copy(count = count))
        } else {
            habitDao.insertHabitLog(
                HabitLog(
                    habitId = habitId,
                    date = dateString,
                    count = count
                )
            )
        }
    }

    override suspend fun calculateStreaks(habitId: Long): Pair<Int, Int> {
        // TODO: Implement streak calculation logic
        return Pair(0, 0)
    }
}