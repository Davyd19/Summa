package com.app.summa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.summa.data.model.Account
import com.app.summa.data.model.Transaction
import com.app.summa.data.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
        loadAccounts()
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            combine(
                accountRepository.getAllAccounts(),
                accountRepository.getTotalNetWorth()
            ) { accounts, netWorth ->
                MoneyUiState(
                    accounts = accounts,
                    totalNetWorth = netWorth ?: 0.0,
                    isLoading = false
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun addAccount(
        name: String,
        type: com.app.summa.data.model.AccountType,
        balance: Double,
        isInvestment: Boolean = false
    ) {
        viewModelScope.launch {
            val newAccount = Account(
                name = name,
                type = type,
                balance = balance,
                isInvestment = isInvestment
            )
            accountRepository.insertAccount(newAccount)
        }
    }

    fun updateAccount(account: Account) {
        viewModelScope.launch {
            accountRepository.updateAccount(account)
        }
    }

    fun transferMoney(fromAccountId: Long, toAccountId: Long, amount: Double) {
        viewModelScope.launch {
            accountRepository.transferBetweenAccounts(fromAccountId, toAccountId, amount)

            // Check if transfer is to investment account
            val toAccount = _uiState.value.accounts.find { it.id == toAccountId }
            if (toAccount?.isInvestment == true) {
                _uiState.value = _uiState.value.copy(showRewardAnimation = true)
            }
        }
    }

    fun dismissRewardAnimation() {
        _uiState.value = _uiState.value.copy(showRewardAnimation = false)
    }

    fun addTransaction(
        accountId: Long,
        type: com.app.summa.data.model.TransactionType,
        amount: Double,
        category: String = "",
        note: String = ""
    ) {
        viewModelScope.launch {
            val transaction = Transaction(
                accountId = accountId,
                type = type,
                amount = amount,
                category = category,
                note = note,
                date = java.time.LocalDate.now().toString()
            )
            accountRepository.insertTransaction(transaction)
        }
    }
}