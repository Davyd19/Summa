package com.app.summa.data.local

import androidx.room.*
import com.app.summa.data.model.Habit
import com.app.summa.data.model.HabitLog
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY createdAt DESC")
    fun getAllHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits")
    fun getAllHabitsSync(): List<Habit> // Untuk Backup

    @Query("SELECT * FROM habit_logs")
    fun getAllHabitLogsSync(): List<HabitLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertHabits(habits: List<Habit>) // Untuk Restore

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertHabitLogs(logs: List<HabitLog>)

    @Query("SELECT * FROM habits WHERE id = :id")
    fun getHabitById(id: Long): Flow<Habit?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit): Long

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId ORDER BY date DESC")
    fun getHabitLogs(habitId: Long): Flow<List<HabitLog>>

    // PENAMBAHAN: Kita butuh flow untuk log harian agar UI update otomatis
    @Query("SELECT * FROM habit_logs WHERE date = :date")
    fun getLogsForDate(date: String): Flow<List<HabitLog>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND date = :date")
    suspend fun getHabitLogByDate(habitId: Long, date: String): HabitLog?

    // PENAMBAHAN: Query untuk mengambil semua log untuk perhitungan streak
    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId ORDER BY date DESC")
    suspend fun getAllLogsForHabit(habitId: Long): List<HabitLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabitLog(log: HabitLog)

    @Update
    suspend fun updateHabitLog(log: HabitLog)
}