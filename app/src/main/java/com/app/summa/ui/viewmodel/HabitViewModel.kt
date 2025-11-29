package com.app.summa.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.summa.data.model.FocusSession
import com.app.summa.data.model.Habit as HabitModel
import com.app.summa.data.model.HabitLog as HabitLogModel
import com.app.summa.data.model.Identity
import com.app.summa.data.repository.FocusRepository
import com.app.summa.data.repository.HabitRepository
import com.app.summa.data.repository.IdentityRepository
import com.app.summa.ui.model.HabitItem
import com.app.summa.util.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HabitUiState(
    val habits: List<HabitItem> = emptyList(),
    val selectedHabit: HabitItem? = null,
    val habitLogs: List<HabitLogModel> = emptyList(),
    val availableIdentities: List<Identity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val showRewardAnimation: Boolean = false
)

@HiltViewModel
class HabitViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val identityRepository: IdentityRepository,
    private val focusRepository: FocusRepository,
    private val savedStateHandle: SavedStateHandle,
    private val notificationScheduler: NotificationScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitUiState())
    val uiState: StateFlow<HabitUiState> = _uiState.asStateFlow()

    private val todayLogs = habitRepository.getLogsForDate(LocalDate.now())

    init {
        loadHabits()
        loadIdentities()

        val habitId = savedStateHandle.get<Long>("habitId")
        if (habitId != null && habitId != -1L) {
            viewModelScope.launch {
                habitRepository.getAllHabits().first().find { it.id == habitId }?.let { habit ->
                    habitRepository.getHabitLogs(habitId).collect { logs ->
                        val item = HabitItem(
                            id = habit.id,
                            name = habit.name,
                            icon = habit.icon,
                            currentCount = 0,
                            targetCount = habit.targetCount,
                            totalSum = habit.totalSum,
                            currentStreak = habit.currentStreak,
                            perfectStreak = habit.perfectStreak,
                            originalModel = habit
                        )
                        _uiState.update { it.copy(selectedHabit = item, habitLogs = logs) }
                    }
                }
            }
        }
    }

    private fun loadIdentities() {
        viewModelScope.launch {
            identityRepository.getAllIdentities().collect { identities ->
                _uiState.update { it.copy(availableIdentities = identities) }
            }
        }
    }

    private fun loadHabits() {
        viewModelScope.launch {
            habitRepository.getAllHabits().combine(todayLogs) { habits, logs ->
                habits.map { habit ->
                    val todayLog = logs.find { it.habitId == habit.id }
                    val item = HabitItem(
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
                    if (_uiState.value.selectedHabit?.id == habit.id) {
                        _uiState.update { it.copy(selectedHabit = item) }
                    }
                    item
                }
            }
                .catch { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
                .collect { mappedHabits ->
                    _uiState.update { it.copy(habits = mappedHabits, isLoading = false, error = null) }
                }
        }
    }

    fun saveFocusSession(habitId: Long, paperclips: Int, startTime: Long) {
        viewModelScope.launch {
            val endTime = System.currentTimeMillis()
            // Perbaiki argumen FocusSession constructor. Pastikan taskId null untuk habit session.
            val session = FocusSession(
                taskId = null,
                habitId = habitId,
                startTime = startTime,
                endTime = endTime,
                paperclipsCollected = paperclips,
                createdAt = endTime
            )
            focusRepository.saveSession(session)
        }
    }

    fun selectHabit(habitItem: HabitItem) {
        viewModelScope.launch {
            habitRepository.getHabitLogs(habitItem.id).collect { logs ->
                _uiState.update { it.copy(selectedHabit = habitItem, habitLogs = logs) }
            }
        }
    }

    fun incrementHabit(habitItem: HabitItem) {
        viewModelScope.launch {
            val newCount = habitItem.currentCount + 1
            if (habitItem.targetCount > 0 && newCount > habitItem.targetCount) {
                _uiState.update { it.copy(showRewardAnimation = true) }
            }
            habitRepository.updateHabitCount(habitItem.originalModel, newCount)
        }
    }

    fun dismissRewardAnimation() { _uiState.update { it.copy(showRewardAnimation = false) } }

    fun decrementHabit(habitItem: HabitItem) {
        if (habitItem.currentCount > 0) {
            viewModelScope.launch {
                val newCount = habitItem.currentCount - 1
                habitRepository.updateHabitCount(habitItem.originalModel, newCount)
            }
        }
    }

    fun addHabit(name: String, icon: String, targetCount: Int, relatedIdentityId: Long? = null, cue: String = "", reminder: String = "") {
        viewModelScope.launch {
            val newHabit = HabitModel(
                name = name,
                icon = icon,
                targetCount = targetCount,
                relatedIdentityId = relatedIdentityId,
                cue = cue,
                reminderTime = reminder,
                createdAt = System.currentTimeMillis()
            )
            // insertHabit mengembalikan Long
            val newId = habitRepository.insertHabit(newHabit)

            // JADWALKAN NOTIFIKASI
            if (reminder.isNotBlank()) {
                // Pastikan tipe data newId adalah Long, scheduleHabitReminder menerima Long
                notificationScheduler.scheduleHabitReminder(newId, name, reminder)
            }
        }
    }

    fun updateHabitDetails(
        originalHabit: HabitModel,
        name: String,
        icon: String,
        targetCount: Int,
        relatedIdentityId: Long?,
        cue: String,
        reminder: String
    ) {
        viewModelScope.launch {
            val updatedHabit = originalHabit.copy(
                name = name,
                icon = icon,
                targetCount = targetCount,
                relatedIdentityId = relatedIdentityId,
                cue = cue,
                reminderTime = reminder
            )
            habitRepository.updateHabit(updatedHabit)
            _uiState.update { it.copy(selectedHabit = it.selectedHabit?.copy(name = name, icon = icon, targetCount = targetCount, originalModel = updatedHabit)) }

            // UPDATE JADWAL NOTIFIKASI
            // updatedHabit.id adalah Long
            notificationScheduler.cancelHabitReminder(updatedHabit.id) // Cancel yang lama
            if (reminder.isNotBlank()) {
                notificationScheduler.scheduleHabitReminder(updatedHabit.id, name, reminder)
            }
        }
    }

    fun updateHabit(habitItem: HabitItem) { viewModelScope.launch { habitRepository.updateHabit(habitItem.originalModel) } }

    fun deleteHabit(habitItem: HabitItem) {
        viewModelScope.launch {
            habitRepository.deleteHabit(habitItem.originalModel)
            // HAPUS NOTIFIKASI
            // habitItem.id adalah Long
            notificationScheduler.cancelHabitReminder(habitItem.id)
        }
    }

    fun onBackFromDetail() { _uiState.update { it.copy(selectedHabit = null) } }
}