package com.app.summa.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
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