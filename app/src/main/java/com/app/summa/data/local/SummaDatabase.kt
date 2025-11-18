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
        KnowledgeNote::class,
        Identity::class,
        // --- PENAMBAHAN ---
        FocusSession::class
        // -------------------
    ],
    // --- PERUBAHAN ---
    // Versi dinaikkan ke 11 (sebelumnya 10)
    version = 11,
    // -----------------
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SummaDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun taskDao(): TaskDao
    abstract fun accountDao(): AccountDao
    abstract fun knowledgeDao(): KnowledgeDao
    abstract fun identityDao(): IdentityDao
    // --- PENAMBAHAN ---
    abstract fun focusSessionDao(): FocusSessionDao
    // -------------------
}