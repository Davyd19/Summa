package com.app.summa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.summa.data.model.Habit
import com.app.summa.data.model.Task
import com.app.summa.data.repository.HabitRepository
import com.app.summa.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DashboardUiState(
    val greeting: String = "",
    val todayProgress: Float = 0f,
    val summaPoints: Int = 0,
    val nextTask: Task? = null,
    val todayHabits: List<Habit> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            combine(
                habitRepository.getAllHabits(),
                taskRepository.getActiveTasks()
            ) { habits, tasks ->
                val completedHabits = habits.filter { it.totalSum > 0 }
                val progress = if (habits.isNotEmpty()) {
                    completedHabits.size.toFloat() / habits.size
                } else 0f

                DashboardUiState(
                    greeting = getGreeting(),
                    todayProgress = progress,
                    summaPoints = completedHabits.sumOf { it.totalSum },
                    nextTask = tasks.firstOrNull(),
                    todayHabits = habits,
                    isLoading = false
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    private fun getGreeting(): String {
        val hour = java.time.LocalTime.now().hour
        return when (hour) {
            in 5..11 -> "Selamat Pagi"
            in 12..14 -> "Selamat Siang"
            in 15..18 -> "Selamat Sore"
            else -> "Selamat Malam"
        }
    }
}