package com.app.summa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.summa.data.model.Habit as HabitModel
import com.app.summa.data.model.HabitLog as HabitLogModel
import com.app.summa.data.repository.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HabitUiState(
    val habits: List<HabitModel> = emptyList(),
    val selectedHabit: HabitModel? = null,
    val habitLogs: List<HabitLogModel> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class HabitViewModel @Inject constructor(
    private val habitRepository: HabitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitUiState())
    val uiState: StateFlow<HabitUiState> = _uiState.asStateFlow()

    init {
        loadHabits()
    }

    private fun loadHabits() {
        viewModelScope.launch {
            habitRepository.getAllHabits()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message,
                        isLoading = false
                    )
                }
                .collect { habits ->
                    _uiState.value = _uiState.value.copy(
                        habits = habits,
                        isLoading = false,
                        error = null
                    )
                }
        }
    }

    fun selectHabit(habit: HabitModel) {
        viewModelScope.launch {
            habitRepository.getHabitLogs(habit.id)
                .collect { logs ->
                    _uiState.value = _uiState.value.copy(
                        selectedHabit = habit,
                        habitLogs = logs
                    )
                }
        }
    }

    fun incrementHabit(habitId: Long, currentCount: Int) {
        viewModelScope.launch {
            habitRepository.logHabitCompletion(
                habitId = habitId,
                count = currentCount + 1
            )
        }
    }

    fun decrementHabit(habitId: Long, currentCount: Int) {
        if (currentCount > 0) {
            viewModelScope.launch {
                habitRepository.logHabitCompletion(
                    habitId = habitId,
                    count = currentCount - 1
                )
            }
        }
    }

    fun addHabit(name: String, icon: String, targetCount: Int) {
        viewModelScope.launch {
            val newHabit = HabitModel(
                name = name,
                icon = icon,
                targetCount = targetCount
            )
            habitRepository.insertHabit(newHabit)
        }
    }

    fun updateHabit(habit: HabitModel) {
        viewModelScope.launch {
            habitRepository.updateHabit(habit)
        }
    }

    fun deleteHabit(habit: HabitModel) {
        viewModelScope.launch {
            habitRepository.deleteHabit(habit)
        }
    }
}
