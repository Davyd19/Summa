package com.app.summa.data.repository

import com.app.summa.data.local.AccountDao
import com.app.summa.data.model.Account
import com.app.summa.data.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface AccountRepository {
    fun getAllAccounts(): Flow<List<Account>>
    fun getTotalNetWorth(): Flow<Double?>
    fun getRecentTransactions(): Flow<List<Transaction>>
    suspend fun insertAccount(account: Account): Long
    suspend fun updateAccount(account: Account)
    fun getAccountTransactions(accountId: Long): Flow<List<Transaction>>
    suspend fun insertTransaction(transaction: Transaction): Long
    suspend fun transferBetweenAccounts(fromAccountId: Long, toAccountId: Long, amount: Double): Boolean
}

class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao
) : AccountRepository {

    override fun getAllAccounts(): Flow<List<Account>> {
        return accountDao.getAllAccounts()
    }

    override fun getTotalNetWorth(): Flow<Double?> {
        return accountDao.getTotalNetWorth()
    }

    override fun getRecentTransactions(): Flow<List<Transaction>> {
        return accountDao.getRecentTransactions()
    }

    override suspend fun insertAccount(account: Account): Long {
        return accountDao.insertAccount(account)
    }

    override suspend fun updateAccount(account: Account) {
        accountDao.updateAccount(account)
    }

    override fun getAccountTransactions(accountId: Long): Flow<List<Transaction>> {
        return accountDao.getAccountTransactions(accountId)
    }

    // PERBAIKAN: Memperbaiki logika 'insertTransaction' agar mengembalikan Long
    override suspend fun insertTransaction(transaction: Transaction): Long {
        return withContext(Dispatchers.IO) {
            val account = accountDao.getAccountById(transaction.accountId)
            if (account == null) {
                return@withContext 0L // Mengembalikan 0 jika akun tidak ditemukan
            }

            val newBalance = account.balance + transaction.amount // amount bisa positif/negatif
            accountDao.updateAccountBalance(account.id, newBalance, System.currentTimeMillis())
            // Ini akan menjadi nilai balik dari 'withContext'
            accountDao.insertTransaction(transaction)
        }
    }

    override suspend fun transferBetweenAccounts(
        fromAccountId: Long,
        toAccountId: Long,
        amount: Double
    ): Boolean {
        return withContext(Dispatchers.IO) {
            val fromAccount = accountDao.getAccountById(fromAccountId)
            val toAccount = accountDao.getAccountById(toAccountId)

            if (fromAccount == null || toAccount == null) {
                return@withContext false // Akun tidak ditemukan
            }

            if (fromAccount.balance < amount) {
                return@withContext false // Saldo tidak cukup
            }

            try {
                // Panggil fungsi @Transaction dari DAO
                accountDao.performTransfer(
                    fromAccountId = fromAccountId,
                    toAccountId = toAccountId,
                    amount = amount,
                    fromAccountBalance = fromAccount.balance,
                    toAccountBalance = toAccount.balance
                )
                return@withContext true // Transfer berhasil
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext false // Transfer gagal
            }
        }
    }
}