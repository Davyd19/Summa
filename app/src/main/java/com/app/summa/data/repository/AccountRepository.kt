package com.app.summa.data.repository

import com.app.summa.data.local.AccountDao
import com.app.summa.data.model.Account
import com.app.summa.data.model.Transaction
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface AccountRepository {
    fun getAllAccounts(): Flow<List<Account>>
    fun getTotalNetWorth(): Flow<Double?>
    suspend fun insertAccount(account: Account): Long
    suspend fun updateAccount(account: Account)
    fun getAccountTransactions(accountId: Long): Flow<List<Transaction>>
    suspend fun insertTransaction(transaction: Transaction): Long
    suspend fun transferBetweenAccounts(fromAccountId: Long, toAccountId: Long, amount: Double)
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

    override suspend fun insertAccount(account: Account): Long {
        return accountDao.insertAccount(account)
    }

    override suspend fun updateAccount(account: Account) {
        accountDao.updateAccount(account)
    }

    override fun getAccountTransactions(accountId: Long): Flow<List<Transaction>> {
        return accountDao.getAccountTransactions(accountId)
    }

    override suspend fun insertTransaction(transaction: Transaction): Long {
        return accountDao.insertTransaction(transaction)
    }

    override suspend fun transferBetweenAccounts(
        fromAccountId: Long,
        toAccountId: Long,
        amount: Double
    ) {
        // TODO: Implement transfer logic with proper transaction handling
    }
}