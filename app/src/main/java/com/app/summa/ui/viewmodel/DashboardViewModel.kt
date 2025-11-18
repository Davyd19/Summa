package com.app.summa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.summa.data.model.HabitLog
import com.app.summa.data.model.Task
import com.app.summa.data.repository.AccountRepository
import com.app.summa.data.repository.HabitRepository
import com.app.summa.data.repository.TaskRepository
import com.app.summa.ui.model.HabitItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class DashboardUiState(
    val greeting: String = "",
    val todayProgress: Float = 0f,
    val summaPoints: Int = 0,
    val nextTask: Task? = null,
    val todayHabits: List<HabitItem> = emptyList(),
    val totalNetWorth: Double = 0.0,
    val isLoading: Boolean = true
    // --- PERUBAHAN ---
    // 'currentMode' DIHAPUS dari state lokal ini.
    // Sekarang dipegang oleh MainViewModel.
    // -------------------
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val taskRepository: TaskRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val today = LocalDate.now()
    private val todayLogs = habitRepository.getLogsForDate(today)
    private val todayTasks = taskRepository.getTasksByDate(today)

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            combine(
                habitRepository.getAllHabits(),
                todayLogs,
                todayTasks,
                accountRepository.getTotalNetWorth()
            ) { habits, logs, tasks, netWorth ->

                val habitItems = habits.map { habit ->
                    val todayLog = logs.find { it.habitId == habit.id }
                    HabitItem(
                        id = habit.id,
                        name = habit.name,
                        icon = habit.icon,
                        currentCount = todayLog?.count ?: 0,
                        targetCount = habit.targetCount,
                        totalSum = habit.totalSum,
                        currentStreak = habit.currentStreak,
                        perfectStreak = habit.perfectStreak,
                        originalModel = habit
                    )
                }

                val completedHabits = habitItems.count {
                    it.targetCount > 0 && it.currentCount >= it.targetCount
                }
                val totalTargetHabits = habitItems.count { it.targetCount > 0 }
                val progress = if (totalTargetHabits > 0) {
                    completedHabits.toFloat() / totalTargetHabits
                } else 0f

                val nextTask = tasks.filter {
                    !it.isCompleted &&
                            try {
                                LocalTime.parse(it.scheduledTime ?: "00:00").isAfter(LocalTime.now())
                            } catch (e: Exception) {
                                true
                            }
                }.minByOrNull {
                    it.scheduledTime ?: "23:59"
                }

                val summaPoints = habitItems.sumOf { it.totalSum }

                DashboardUiState(
                    greeting = getGreeting(),
                    todayProgress = progress,
                    summaPoints = summaPoints,
                    nextTask = nextTask,
                    todayHabits = habitItems,
                    totalNetWorth = netWorth ?: 0.0,
                    isLoading = false
                    // --- PERUBAHAN ---
                    // 'currentMode' DIHAPUS dari update state ini
                    // -------------------
                )
            }.catch { e ->
                _uiState.update { it.copy(isLoading = false) }
                e.printStackTrace()
            }
                .collect { newState ->
                    _uiState.value = newState
                }
        }
    }

    // --- PERUBAHAN ---
    // 'setMode' DIHAPUS. Fungsi ini sekarang ditangani oleh MainViewModel.
    // -------------------

    private fun getGreeting(): String {
        val hour = LocalTime.now().hour
        return when (hour) {
            in 5..11 -> "Selamat Pagi"
            in 12..14 -> "Selamat Siang"
            in 15..18 -> "Selamat Sore"
            else -> "Selamat Malam"
        }
    }
}