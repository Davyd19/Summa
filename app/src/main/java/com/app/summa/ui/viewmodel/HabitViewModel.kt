package com.app.summa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.summa.data.model.Habit as HabitModel
import com.app.summa.data.model.HabitLog as HabitLogModel
import com.app.summa.data.model.Identity
import com.app.summa.data.repository.HabitRepository
import com.app.summa.data.repository.IdentityRepository
import com.app.summa.ui.model.HabitItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HabitUiState(
    val habits: List<HabitItem> = emptyList(),
    val selectedHabit: HabitItem? = null,
    val habitLogs: List<HabitLogModel> = emptyList(),
    // --- PENAMBAHAN ---
    // Daftar identitas untuk ditampilkan di Dropdown saat tambah habit
    val availableIdentities: List<Identity> = emptyList(),
    // -------------------
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class HabitViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    // --- PENAMBAHAN ---
    // Kita butuh repo ini untuk mengambil daftar pilihan identitas
    private val identityRepository: IdentityRepository
    // -------------------
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitUiState())
    val uiState: StateFlow<HabitUiState> = _uiState.asStateFlow()

    private val todayLogs = habitRepository.getLogsForDate(LocalDate.now())

    init {
        loadHabits()
        loadIdentities()
    }

    // --- PENAMBAHAN ---
    private fun loadIdentities() {
        viewModelScope.launch {
            identityRepository.getAllIdentities().collect { identities ->
                _uiState.update { it.copy(availableIdentities = identities) }
            }
        }
    }
    // -------------------

    private fun loadHabits() {
        viewModelScope.launch {
            habitRepository.getAllHabits().combine(todayLogs) { habits, logs ->
                habits.map { habit ->
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
            }
                .catch { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
                .collect { mappedHabits ->
                    _uiState.update { it.copy(habits = mappedHabits, isLoading = false, error = null) }
                }
        }
    }

    fun selectHabit(habitItem: HabitItem) {
        viewModelScope.launch {
            habitRepository.getHabitLogs(habitItem.id)
                .collect { logs ->
                    _uiState.update { it.copy(selectedHabit = habitItem, habitLogs = logs) }
                }
        }
    }

    fun incrementHabit(habitItem: HabitItem) {
        viewModelScope.launch {
            val newCount = habitItem.currentCount + 1
            habitRepository.updateHabitCount(
                habit = habitItem.originalModel,
                newCount = newCount
            )
        }
    }

    fun decrementHabit(habitItem: HabitItem) {
        if (habitItem.currentCount > 0) {
            viewModelScope.launch {
                val newCount = habitItem.currentCount - 1
                habitRepository.updateHabitCount(
                    habit = habitItem.originalModel,
                    newCount = newCount
                )
            }
        }
    }

    fun addHabit(name: String, icon: String, targetCount: Int, relatedIdentityId: Long? = null) {
        viewModelScope.launch {
            val newHabit = HabitModel(
                name = name,
                icon = icon,
                targetCount = targetCount,
                relatedIdentityId = relatedIdentityId,
                createdAt = System.currentTimeMillis()
            )
            habitRepository.insertHabit(newHabit)
        }
    }

    fun updateHabit(habitItem: HabitItem) {
        viewModelScope.launch {
            habitRepository.updateHabit(habitItem.originalModel)
        }
    }

    fun deleteHabit(habitItem: HabitItem) {
        viewModelScope.launch {
            habitRepository.deleteHabit(habitItem.originalModel)
        }
    }

    fun onBackFromDetail() {
        _uiState.update { it.copy(selectedHabit = null) }
    }
}