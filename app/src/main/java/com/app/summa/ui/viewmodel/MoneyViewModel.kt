package com.app.summa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.summa.data.model.Account
import com.app.summa.data.model.Transaction
import com.app.summa.data.model.TransactionType
import com.app.summa.data.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class MoneyUiState(
    val accounts: List<Account> = emptyList(),
    val totalNetWorth: Double = 0.0,
    val recentTransactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val showRewardAnimation: Boolean = false
)

@HiltViewModel
class MoneyViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MoneyUiState())
    val uiState: StateFlow<MoneyUiState> = _uiState.asStateFlow()

    init {
        loadMoneyData()
    }

    // PERBAIKAN: Gabungkan semua flow yang dibutuhkan
    private fun loadMoneyData() {
        viewModelScope.launch {
            combine(
                accountRepository.getAllAccounts(),
                accountRepository.getTotalNetWorth(),
                accountRepository.getRecentTransactions()
            ) { accounts, netWorth, transactions ->
                MoneyUiState(
                    accounts = accounts,
                    totalNetWorth = netWorth ?: 0.0,
                    recentTransactions = transactions,
                    isLoading = false
                )
            }.catch { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
                .collect { newState ->
                    _uiState.value = newState
                }
        }
    }

    fun addAccount(
        name: String,
        type: com.app.summa.data.model.AccountType,
        balance: Double,
        isInvestment: Boolean = false,
        color: String
    ) {
        viewModelScope.launch {
            val newAccount = Account(
                name = name,
                type = type,
                balance = balance,
                isInvestment = isInvestment,
                color = color,
                updatedAt = System.currentTimeMillis()
            )
            accountRepository.insertAccount(newAccount)
        }
    }

    fun updateAccount(account: Account) {
        viewModelScope.launch {
            accountRepository.updateAccount(account)
        }
    }

    fun transferMoney(fromAccount: Account, toAccount: Account, amount: Double) {
        viewModelScope.launch {
            val success = accountRepository.transferBetweenAccounts(fromAccount.id, toAccount.id, amount)

            if (success) {
                // IMPLEMENTASI: Cek Ganjaran Langsung
                if (toAccount.isInvestment) {
                    _uiState.update { it.copy(showRewardAnimation = true) }
                }
            } else {
                // TODO: Tampilkan error (misal: "Saldo tidak cukup")
                _uiState.update { it.copy(error = "Transfer gagal. Periksa saldo Anda.") }
            }
        }
    }

    fun dismissRewardAnimation() {
        _uiState.update { it.copy(showRewardAnimation = false) }
    }

    fun addTransaction(
        accountId: Long,
        type: com.app.summa.data.model.TransactionType,
        amount: Double,
        category: String = "",
        note: String = ""
    ) {
        viewModelScope.launch {
            // Pastikan amount positif/negatif sesuai tipe
            val finalAmount = when(type) {
                TransactionType.INCOME -> amount
                TransactionType.EXPENSE -> -amount
                TransactionType.TRANSFER -> 0.0 // Transfer harus pakai transferMoney
            }

            if (type == TransactionType.TRANSFER) return@launch // Paksa gunakan fungsi transfer

            val transaction = Transaction(
                accountId = accountId,
                type = type,
                amount = finalAmount,
                category = category,
                note = note,
                date = LocalDate.now().toString(),
                timestamp = System.currentTimeMillis()
            )
            accountRepository.insertTransaction(transaction)
        }
    }

    // Helper untuk membersihkan error
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}