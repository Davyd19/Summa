package com.app.summa.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val icon: String = "🎯",
    val targetCount: Int = 3,
    val totalSum: Int = 0,
    val currentStreak: Int = 0,
    val perfectStreak: Int = 0,
    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    val createdAt: Long? = null
)

@Entity(tableName = "habit_logs")
data class HabitLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val habitId: Long,
    val date: String, // Format: yyyy-MM-dd
    val count: Int,
    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    val timestamp: Long? = null
)

// File: data/model/Task.kt
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val twoMinuteAction: String = "",
    val isCommitment: Boolean = true,
    val scheduledDate: String?, // yyyy-MM-dd
    val scheduledTime: String?, // HH:mm
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    val createdAt: Long? = null
)

// File: data/model/Account.kt
@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val balance: Double,
    val icon: String = "💰",
    val color: String = "#4CAF50",
    val isInvestment: Boolean = false,
    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    val updatedAt: Long? = null
)

enum class AccountType {
    BANK, CASH, INVESTMENT, EWALLET
}

@Entity(tableName = "transactions")
data class Transaction(
    // PERBAIKAN: Menambahkan @PrimaryKey yang hilang
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountId: Long,
    val type: TransactionType,
    val amount: Double,
    val category: String = "",
    val note: String = "",
    val date: String, // yyyy-MM-dd
    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    val timestamp: Long? = null
)

enum class TransactionType {
    INCOME, EXPENSE, TRANSFER
}

// File: data/model/Note.kt
@Entity(tableName = "notes")
data class Note(
    // PERBAIKAN: Menambahkan @PrimaryKey yang hilang
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "",
    val content: String,
    val tags: String = "", // Comma-separated
    val isArchived: Boolean = false,
    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    val createdAt: Long? = null,
    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    val updatedAt: Long? = null
)

// File: data/model/Identity.kt
@Entity(tableName = "identities")
data class Identity(
    // PERBAIKAN: Menambahkan @PrimaryKey yang hilang
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val progress: Int = 0,
    val relatedHabitIds: String = "", // Comma-separated IDs
    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    val createdAt: Long? = null
)