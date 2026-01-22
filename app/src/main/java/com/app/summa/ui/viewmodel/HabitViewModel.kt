package com.app.summa.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.summa.data.model.FocusSession
import com.app.summa.data.model.Habit
import com.app.summa.data.model.HabitItem
import com.app.summa.data.model.HabitLog
import com.app.summa.data.model.Identity
import com.app.summa.data.repository.FocusRepository
import com.app.summa.data.repository.HabitRepository
import com.app.summa.data.repository.IdentityRepository
import com.app.summa.util.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HabitUiState(
    val habits: List<HabitItem> = emptyList(),
    val todayHabits: List<HabitItem> = emptyList(),
    val habitLogs: List<HabitLog> = emptyList(),
    val availableIdentities: List<Identity> = emptyList(),
    val selectedHabit: HabitItem? = null,
    val showRewardAnimation: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class HabitViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val identityRepository: IdentityRepository,
    private val focusRepository: FocusRepository,
    private val notificationScheduler: NotificationScheduler, // Inject Scheduler
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitUiState(isLoading = true))
    val uiState: StateFlow<HabitUiState> = _uiState.asStateFlow()

    init {
        // Load data awal
        viewModelScope.launch {
            combine(
                habitRepository.getAllHabits(),
                habitRepository.getLogsForDate(LocalDate.now()),
                identityRepository.getAllIdentities()
            ) { habits, logs, identities ->
                Triple(habits, logs, identities)
            }.collect { (habits, logs, identities) ->
                val habitItems = habits.map { habit ->
                    mapToHabitItem(habit, logs)
                }

                // Update state list utama
                _uiState.update {
                    it.copy(
                        habits = habitItems,
                        todayHabits = habitItems, // Bisa difilter jika ada logika jadwal
                        availableIdentities = identities,
                        isLoading = false
                    )
                }

                // Update detail habit jika sedang dibuka
                val currentSelectedId = _uiState.value.selectedHabit?.id
                if (currentSelectedId != null) {
                    val updatedSelected = habitItems.find { it.id == currentSelectedId }
                    if (updatedSelected != null) {
                        _uiState.update { it.copy(selectedHabit = updatedSelected) }
                        // Load logs historis untuk detail
                        loadHabitLogs(currentSelectedId)
                    }
                }
            }
        }

        // Cek argumen navigasi jika membuka detail
        val habitId = savedStateHandle.get<Long>("habitId")
        if (habitId != null) {
            selectHabit(habitId)
        }
    }

    private fun mapToHabitItem(habit: Habit, logs: List<HabitLog>): HabitItem {
        val todayLog = logs.find { it.habitId == habit.id }
        return HabitItem(
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

    fun addHabit(
        name: String,
        icon: String,
        targetCount: Int,
        relatedIdentityId: Long?,
        cue: String,
        reminderTime: String
    ) {
        viewModelScope.launch {
            val newHabit = Habit(
                name = name,
                icon = icon,
                targetCount = targetCount,
                relatedIdentityId = relatedIdentityId,
                cue = cue,
                reminderTime = reminderTime,
                createdAt = System.currentTimeMillis()
            )
            val id = habitRepository.insertHabit(newHabit)
            
            if (reminderTime.isNotBlank()) {
                 val habitWithId = newHabit.copy(id = id)
                 notificationScheduler.scheduleHabitReminder(habitWithId)
            }
        }
    }

    fun selectHabit(habitId: Long) {
        val habit = _uiState.value.habits.find { h -> h.id == habitId }
        if (habit != null) {
             _uiState.update { it.copy(selectedHabit = habit) }
             loadHabitLogs(habitId)
        }
    }

    private fun loadHabitLogs(habitId: Long) {
        viewModelScope.launch {
            habitRepository.getHabitLogs(habitId).collect { logs ->
                _uiState.update { it.copy(habitLogs = logs) }
            }
        }
    }

    fun updateHabitDetails(habit: Habit, name: String, icon: String, target: Int, identityId: Long?, cue: String, reminder: String) {
        viewModelScope.launch {
            val updatedHabit = habit.copy(
                name = name,
                icon = icon,
                targetCount = target,
                relatedIdentityId = identityId,
                cue = cue,
                reminderTime = reminder
            )
            habitRepository.updateHabit(updatedHabit)

            // PERBAIKAN: Cancel alarm lama dan set yang baru
            notificationScheduler.cancelHabitReminder(habit)
            notificationScheduler.scheduleHabitReminder(updatedHabit)
        }
    }

    fun incrementHabit(habitItem: HabitItem) {
        viewModelScope.launch {
            val newCount = habitItem.currentCount + 1
            habitRepository.updateHabitCount(habitItem.originalModel, newCount)

            // Trigger animasi jika target tercapai atau terlampaui
            if (newCount >= habitItem.targetCount) {
                _uiState.update { it.copy(showRewardAnimation = true) }
            }
        }
    }

    fun decrementHabit(habitItem: HabitItem) {
        if (habitItem.currentCount > 0) {
            viewModelScope.launch {
                habitRepository.updateHabitCount(habitItem.originalModel, habitItem.currentCount - 1)
            }
        }
    }

    fun saveFocusSession(habitId: Long, clips: Int, startTime: Long) {
        viewModelScope.launch {
            val session = FocusSession(
                habitId = habitId,
                startTime = startTime,
                endTime = System.currentTimeMillis(),
                paperclipsCollected = clips,
                createdAt = System.currentTimeMillis()
            )
            focusRepository.saveSession(session)
        }
    }

    fun dismissRewardAnimation() {
        _uiState.update { it.copy(showRewardAnimation = false) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedHabit = null, habitLogs = emptyList()) }
    }
}