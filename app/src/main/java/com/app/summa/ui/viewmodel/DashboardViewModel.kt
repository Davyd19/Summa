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
    val morningBriefing: DailyWrapUpResult? = null
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
    // Menggunakan combine memastikan UI hanya recompose jika salah satu data berubah.
    // Menggunakan stateIn dengan WhileSubscribed(5000) mencegah query berulang saat config change.

    val uiState: StateFlow<DashboardUiState> = combine(
        _currentMode,
        habitRepository.getLogsForDate(LocalDate.now()),
        habitRepository.getAllHabits(),
        taskRepository.getActiveTasks(),
        identityRepository.getAllIdentities(),
        accountRepository.getTotalNetWorth(),
        focusRepository.getTotalPaperclips(),
        _levelUpEvent,
        _morningBriefing
    ) { inputs ->
        // Destructuring array inputs agar rapi
        val mode = inputs[0] as String
        val todayLogs = inputs[1] as List<HabitLog>
        val allHabits = inputs[2] as List<Habit>
        val tasks = inputs[3] as List<Task>
        val identities = inputs[4] as List<Identity>
        val netWorth = inputs[5] as Double? ?: 0.0
        val paperclips = inputs[6] as Int
        val lvlEvent = inputs[7] as LevelUpEvent?
        val briefing = inputs[8] as DailyWrapUpResult?

        // 1. Hitung Progress Habit Hari Ini
        // Transform Habit Entity ke UI Model (HabitItem)
        val todayHabitItems = allHabits.map { habit ->
            val log = todayLogs.find { it.habitId == habit.id }
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
        val totalPoints = identities.sumOf { it.progress }
        val activeTasks = tasks.count { !it.isCompleted }
        val completedHabits = todayHabitItems.count { it.currentCount >= it.targetCount && it.targetCount > 0 }

        // 3. Tentukan Next Task (Tugas Prioritas)
        val nextTask = tasks
            .filter { it.scheduledTime != null }
            .sortedBy { it.scheduledTime }
            .firstOrNull()

        DashboardUiState(
            greeting = getGreetingMessage(),
            currentMode = mode,
            summaPoints = totalPoints,
            todayProgress = progress,
            activeTasks = activeTasks,
            completedHabits = completedHabits,
            totalPaperclips = paperclips,
            totalNetWorth = netWorth,
            nextTask = nextTask,
            todayHabits = todayHabitItems,
            levelUpEvent = lvlEvent,
            morningBriefing = briefing
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