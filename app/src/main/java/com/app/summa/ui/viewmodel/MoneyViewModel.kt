package com.app.summa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.summa.data.model.Account
import com.app.summa.data.model.Transaction
import com.app.summa.data.model.TransactionType
import com.app.summa.data.repository.AccountRepository
import com.app.summa.data.repository.IdentityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class MoneyUiState(
    val accounts: List<Account> = emptyList(),
    val totalNetWorth: Double = 0.0,
    val recentTransactions: List<Transaction> = emptyList(),
    val monthlyGrowth: Double = 0.0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showRewardAnimation: Boolean = false
)

@HiltViewModel
class MoneyViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    // PERBAIKAN: Inject IdentityRepository untuk gamifikasi
    private val identityRepository: IdentityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MoneyUiState())
    val uiState: StateFlow<MoneyUiState> = _uiState.asStateFlow()

    init {
        loadMoneyData()
    }

    private fun loadMoneyData() {
        // Calculate start of month timestamp
        val startOfMonth = java.time.LocalDate.now().withDayOfMonth(1)
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        viewModelScope.launch {
            combine(
                accountRepository.getAllAccounts(),
                accountRepository.getTotalNetWorth(),
                accountRepository.getRecentTransactions(),
                accountRepository.getTransactionsAfter(startOfMonth)
            ) { accounts, netWorth, recentTransactions, monthlyTransactions ->
                val currentNetWorth = netWorth ?: 0.0
                
                // Calculate Net Flow for this month (Income - Expense)
                val income = monthlyTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                val expense = monthlyTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount } // Expense stored as negative usually? Check logic.
                // In DAO/Repo: insertTransaction logic for balance update: balance + amount.
                // But typically expense is negative number in DB or logic handles it.
                // In insertTransaction: amount = -amount for Expense.
                // So sumOf { it.amount } accounts for signs correctly.
                
                val netFlow = income + expense // (Income + (-Expense))
                
                // Growth = NetFlow / (StartBalance). StartBalance = Current - NetFlow
                val startBalance = currentNetWorth - netFlow
                
                val growthPercentage = if (startBalance != 0.0) {
                    (netFlow / startBalance) * 100
                } else if (netFlow > 0) {
                    100.0 // Infinite growth from 0
                } else {
                    0.0
                }

                MoneyUiState(
                    accounts = accounts,
                    totalNetWorth = currentNetWorth,
                    recentTransactions = recentTransactions,
                    monthlyGrowth = growthPercentage,
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
                // LOGIKA BARU: Cek Ganjaran Langsung + Identity XP
                if (toAccount.isInvestment) {
                    _uiState.update { it.copy(showRewardAnimation = true) }

                    // Cari identitas yang relevan untuk diberi XP
                    val identities = identityRepository.getAllIdentities().first()
                    // Prioritas: Nama mengandung "Invest/Kaya/Uang" -> Level Tertinggi -> Identitas Pertama
                    val targetIdentity = identities.find {
                        it.name.contains("Invest", ignoreCase = true) ||
                                it.name.contains("Kaya", ignoreCase = true) ||
                                it.name.contains("Keuangan", ignoreCase = true)
                    } ?: identities.maxByOrNull { it.progress }

                    if (targetIdentity != null) {
                        // Konversi Investasi ke XP (Contoh: Setiap 50.000 = 10 XP, Max 100 XP per transfer)
                        // Anda bisa sesuaikan rumus ini
                        val basePoints = 10
                        val bonusPoints = (amount / 50000).toInt().coerceIn(0, 90)
                        val totalPoints = basePoints + bonusPoints

                        identityRepository.addVoteToIdentity(
                            identityId = targetIdentity.id,
                            points = totalPoints,
                            note = "Investasi cerdas ke ${toAccount.name}"
                        )
                    }
                }
            } else {
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
            val finalAmount = when(type) {
                TransactionType.INCOME -> amount
                TransactionType.EXPENSE -> -amount
                TransactionType.TRANSFER -> 0.0
            }

            if (type == TransactionType.TRANSFER) return@launch

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

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}