package com.app.summa.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.app.summa.data.model.*

@Database(
    entities = [
        Habit::class,
        HabitLog::class,
        Task::class,
        Account::class,
        Transaction::class,
        Note::class,
        Identity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SummaDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun taskDao(): TaskDao
    abstract fun accountDao(): AccountDao
    abstract fun noteDao(): NoteDao
    abstract fun identityDao(): IdentityDao
}