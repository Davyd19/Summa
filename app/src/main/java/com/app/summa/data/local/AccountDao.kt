package com.app.summa.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.app.summa.data.model.Account
import com.app.summa.data.model.Transaction

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY updatedAt DESC")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT SUM(balance) FROM accounts")
    fun getTotalNetWorth(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account): Long

    @Update
    suspend fun updateAccount(account: Account)

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY timestamp DESC LIMIT 50")
    fun getAccountTransactions(accountId: Long): Flow<List<Transaction>>

    @Insert
    suspend fun insertTransaction(transaction: Transaction): Long
}