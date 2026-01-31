package com.app.summa.data.repository

import com.app.summa.data.local.AccountDao
import com.app.summa.data.model.Account
import com.app.summa.data.model.Transaction
import com.app.summa.data.model.TransactionType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class AccountRepositoryTest {

    private val accountDao: AccountDao = mockk(relaxed = true)
    private val accountRepository = AccountRepositoryImpl(accountDao)

    @Test
    fun `insertTransaction calls insertTransactionWithBalanceUpdate`() = runTest {
        val transaction = Transaction(
            accountId = 1,
            type = TransactionType.INCOME,
            amount = 100.0,
            category = "Test",
            note = "Note",
            date = LocalDate.now().toString(),
            timestamp = System.currentTimeMillis()
        )

        coEvery { accountDao.insertTransactionWithBalanceUpdate(transaction) } returns 1L

        val result = accountRepository.insertTransaction(transaction)

        assertEquals(1L, result)
        coVerify(exactly = 1) { accountDao.insertTransactionWithBalanceUpdate(transaction) }

        // Verify that individual calls are NOT made (they are inside the DAO method now)
        coVerify(exactly = 0) { accountDao.updateAccountBalance(any(), any(), any()) }
        coVerify(exactly = 0) { accountDao.insertTransaction(any()) }
    }
}