package com.app.summa.data.local

import androidx.room.TypeConverter
import com.app.summa.data.model.AccountType
import com.app.summa.data.model.TransactionType
import java.lang.IllegalArgumentException

/**
 * TypeConverter untuk memberi tahu Room cara menyimpan dan membaca tipe Enum.
 * PERBAIKAN: Tipe parameter di 'from...' disesuaikan menjadi non-nullable
 * agar cocok dengan data model.
 */
class Converters {

    // --- Konverter untuk AccountType ---

    @TypeConverter
    fun fromAccountType(value: AccountType): String {
        // Mengubah Enum AccountType (non-null) menjadi String
        return value.name
    }

    @TypeConverter
    fun toAccountType(value: String): AccountType {
        // Mengubah String "BANK" kembali menjadi Enum AccountType.BANK
        return try {
            AccountType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            AccountType.CASH // Fallback jika String tidak valid
        }
    }

    // --- Konverter untuk TransactionType ---

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String {
        // Mengubah Enum TransactionType (non-null) menjadi String
        return value.name
    }

    @TypeConverter
    fun toTransactionType(value: String): TransactionType {
        // Mengubah String "EXPENSE" kembali menjadi Enum TransactionType.EXPENSE
        return try {
            TransactionType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            TransactionType.EXPENSE // Fallback jika String tidak valid
        }
    }
}