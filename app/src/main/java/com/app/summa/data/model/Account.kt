package com.app.summa.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

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