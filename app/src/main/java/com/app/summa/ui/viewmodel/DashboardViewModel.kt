package com.app.summa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.summa.data.model.*
import com.app.summa.data.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class DashboardUiState(
    val greeting: String = "",
    val currentMode: String = "Normal", // Normal, Fokus, Pagi
    val summaPoints: Int = 0,
    val todayProgress: Float = 0f,
    val activeTasks: Int = 0,
    val completedHabits: Int = 0,
    val totalPaperclips: Int = 0,
    val totalNetWorth: Double = 0.0,
    val nextTask: Task? = null,
    val todayHabits: List<HabitItem> = emptyList(),
    val levelUpEvent: LevelUpEvent? = null,
    val morningBriefing: DailyWrapUpResult? = null,
    val accounts: List<Account> = emptyList()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository,
    private val identityRepository: IdentityRepository,
    private val accountRepository: AccountRepository,
    private val focusRepository: FocusRepository
) : ViewModel() {

    // Internal State
    private val _currentMode = MutableStateFlow("Normal")
    private val _levelUpEvent = MutableStateFlow<LevelUpEvent?>(null)
    private val _morningBriefing = MutableStateFlow<DailyWrapUpResult?>(null)

    // --- OPTIMASI FLOW: MENGGABUNGKAN SEMUA SUMBER DATA ---
    // Menggunakan combine berjenjang untuk menghindari unchecked cast dan limit argumen combine (max 5)

    private data class HabitData(
        val logs: List<HabitLog>,
        val habits: List<Habit>,
        val tasks: List<Task>,
        val identities: List<Identity>
    )

    private data class FinancialData(
        val netWorth: Double,
        val paperclips: Int,
        val accounts: List<Account>
    )

    private data class UiStateData(
        val mode: String,
        val levelUp: LevelUpEvent?,
        val briefing: DailyWrapUpResult?
    )

    private val habitDataFlow = combine(
        habitRepository.getLogsForDate(LocalDate.now()),
        habitRepository.getAllHabits(),
        taskRepository.getActiveTasks(),
        identityRepository.getAllIdentities()
    ) { logs, habits, tasks, identities ->
        HabitData(logs, habits, tasks, identities)
    }

    private val financialDataFlow = combine(
        accountRepository.getTotalNetWorth(),
        focusRepository.getTotalPaperclips(),
        accountRepository.getAllAccounts()
    ) { netWorth, paperclips, accounts ->
        FinancialData(netWorth ?: 0.0, paperclips, accounts)
    }

    private val uiStateFlow = combine(
        _currentMode,
        _levelUpEvent,
        _morningBriefing
    ) { mode, levelUp, briefing ->
        UiStateData(mode, levelUp, briefing)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        habitDataFlow,
        financialDataFlow,
        uiStateFlow
    ) { habitData, financialData, uiState ->

        // 1. Hitung Progress Habit Hari Ini
        // Transform Habit Entity ke UI Model (HabitItem)
        val todayHabitItems = habitData.habits.map { habit ->
            val log = habitData.logs.find { it.habitId == habit.id }
            val currentCount = log?.count ?: 0
            HabitItem(
                id = habit.id,
                name = habit.name,
                icon = habit.icon,
                currentCount = currentCount,
                targetCount = habit.targetCount,
                totalSum = habit.totalSum,
                currentStreak = habit.currentStreak,
                perfectStreak = habit.perfectStreak,
                originalModel = habit
            )
        }

        // Hitung persentase penyelesaian
        val totalTargets = todayHabitItems.sumOf { it.targetCount }
        val currentProgressSum = todayHabitItems.sumOf { it.currentCount.coerceAtMost(it.targetCount) }
        val progress = if (totalTargets > 0) currentProgressSum.toFloat() / totalTargets else 0f

        // 2. Hitung Total Poin (Summa Points) dari Identitas
        val totalPoints = habitData.identities.sumOf { it.progress }
        val activeTasks = habitData.tasks.count { !it.isCompleted }
        val completedHabits = todayHabitItems.count { it.currentCount >= it.targetCount && it.targetCount > 0 }

        // 3. Tentukan Next Task (Tugas Prioritas)
        val nextTask = habitData.tasks
            .filter { it.scheduledTime != null }
            .sortedBy { it.scheduledTime }
            .firstOrNull()

        DashboardUiState(
            greeting = getGreetingMessage(),
            currentMode = uiState.mode,
            summaPoints = totalPoints,
            todayProgress = progress,
            activeTasks = activeTasks,
            completedHabits = completedHabits,
            totalPaperclips = financialData.paperclips,
            totalNetWorth = financialData.netWorth,
            nextTask = nextTask,
            todayHabits = todayHabitItems,
            levelUpEvent = uiState.levelUp,
            morningBriefing = uiState.briefing,
            accounts = financialData.accounts
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000), // Cache data selama 5 detik setelah UI inactive
        initialValue = DashboardUiState()
    )

    init {
        // Mendengarkan Event Global
        viewModelScope.launch {
            identityRepository.levelUpEvents.collect { event ->
                _levelUpEvent.value = event
            }
        }

        // Cek Morning Briefing saat ViewModel dibuat
        checkMorningBriefing()
    }

    fun setMode(mode: String) {
        _currentMode.value = mode
    }

    fun dismissLevelUp() {
        _levelUpEvent.value = null
    }

    fun dismissBriefing() {
        _morningBriefing.value = null
    }

    private fun checkMorningBriefing() {
        viewModelScope.launch {
            val result = taskRepository.processDailyWrapUp()
            if (result != null) {
                _morningBriefing.value = result
            }
        }
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

    private fun getGreetingMessage(): String {
        val hour = LocalTime.now().hour
        return when (hour) {
            in 5..11 -> "Selamat Pagi"
            in 12..15 -> "Selamat Siang"
            in 16..18 -> "Selamat Sore"
            else -> "Selamat Malam"
        }
    }
}
