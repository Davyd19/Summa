package com.app.summa.data.local

import androidx.room.*
// PENAMBAHAN: Import yang hilang untuk anotasi @Transaction
import androidx.room.Transaction as RoomTransaction
import kotlinx.coroutines.flow.Flow
import com.app.summa.data.model.Account
import com.app.summa.data.model.Transaction
import com.app.summa.data.model.TransactionType
import java.time.LocalDate

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY updatedAt DESC")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT SUM(balance) FROM accounts")
    fun getTotalNetWorth(): Flow<Double?>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT 20")
    fun getRecentTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): Account?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account): Long

    @Update
    suspend fun updateAccount(account: Account)

    @Query("UPDATE accounts SET balance = :balance, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateAccountBalance(id: Long, balance: Double, timestamp: Long)

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY timestamp DESC LIMIT 50")
    fun getAccountTransactions(accountId: Long): Flow<List<Transaction>>

    @Insert
    suspend fun insertTransaction(transaction: Transaction): Long

    // PERBAIKAN: Menggunakan alias 'RoomTransaction' untuk anotasi
    @RoomTransaction
    suspend fun performTransfer(
        fromAccountId: Long,
        toAccountId: Long,
        amount: Double,
        fromAccountBalance: Double,
        toAccountBalance: Double
    ) {
        val time = System.currentTimeMillis()
        val date = LocalDate.now().toString()

        // 1. Buat transaksi pengeluaran
        insertTransaction(Transaction(
            accountId = fromAccountId,
            type = TransactionType.EXPENSE,
            amount = -amount, // Jumlah negatif
            category = "Transfer",
            note = "Transfer ke akun lain",
            date = date,
            timestamp = time
        ))

        // 2. Buat transaksi pemasukan
        insertTransaction(Transaction(
            accountId = toAccountId,
            type = TransactionType.INCOME,
            amount = amount, // Jumlah positif
            category = "Transfer",
            note = "Transfer dari akun lain",
            date = date,
            timestamp = time
        ))

        // 3. Update saldo akun asal
        updateAccountBalance(fromAccountId, fromAccountBalance - amount, time)

        // 4. Update saldo akun tujuan
        updateAccountBalance(toAccountId, toAccountBalance + amount, time)
    }
}