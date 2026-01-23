package com.app.summa.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
// Hapus import androidx.room.* dan alias Transaction agar tidak bingung

import kotlinx.coroutines.flow.Flow
import com.app.summa.data.model.Account
import com.app.summa.data.model.Transaction
import com.app.summa.data.model.TransactionType
import java.time.LocalDate

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY updatedAt DESC")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts")
    fun getAllAccountsSync(): List<Account> // Untuk Backup

    // PERBAIKAN 1: Query harus ke tabel 'transactions', bukan 'accounts'
    @Query("SELECT * FROM transactions")
    fun getAllTransactionsSync(): List<Transaction> // Untuk Backup

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAccounts(accounts: List<Account>) // Ubah nama param jadi accounts

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTransactions(transactions: List<Transaction>) // Untuk Restore

    @Query("SELECT SUM(balance) FROM accounts")
    fun getTotalNetWorth(): Flow<Double?>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT 20")
    fun getRecentTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :timestamp")
    fun getTransactionsAfter(timestamp: Long): Flow<List<Transaction>>

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

    // PERBAIKAN 2: Gunakan @androidx.room.Transaction secara eksplisit
    // Ini mencegah konflik dengan class model 'Transaction'
    @androidx.room.Transaction
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