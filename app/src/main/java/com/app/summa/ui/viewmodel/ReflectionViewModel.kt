package com.app.summa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.summa.data.model.Habit
import com.app.summa.data.model.Identity
import com.app.summa.data.model.Task
import com.app.summa.data.repository.HabitRepository
import com.app.summa.data.repository.IdentityRepository
import com.app.summa.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DailySummary(
    val completedHabits: List<Habit>,
    val completedTasks: List<Task>
)

data class ReflectionUiState(
    val summary: DailySummary? = null,
    val identities: List<Identity> = emptyList(),
    val reflectionText: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class ReflectionViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val taskRepository: TaskRepository,
    private val identityRepository: IdentityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReflectionUiState())
    val uiState: StateFlow<ReflectionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            identityRepository.getAllIdentities().collect { identities ->
                _uiState.update { it.copy(identities = identities) }
            }
        }
        loadSummaryData()
    }

    private fun loadSummaryData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val today = LocalDate.now()
            val todayHabitLogs = habitRepository.getLogsForDate(today).first()
            val todayTasks = taskRepository.getTasksByDate(today).first()
            val allHabits = habitRepository.getAllHabits().first()

            val completedHabits = allHabits.filter { habit ->
                val log = todayHabitLogs.find { it.habitId == habit.id }
                (log?.count ?: 0) >= habit.targetCount && habit.targetCount > 0
            }
            val completedTasks = todayTasks.filter { it.isCompleted }

            _uiState.update {
                it.copy(
                    summary = DailySummary(completedHabits, completedTasks),
                    isLoading = false
                )
            }
        }
    }

    fun addVote(identity: Identity, points: Int, note: String) {
        viewModelScope.launch {
            // --- PERUBAHAN ---
            // Sekarang kita teruskan 'note' ke repository
            identityRepository.addVoteToIdentity(identity.id, points, note)
            // -----------------
        }
    }

    fun saveReflection(text: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(reflectionText = text) }
        }
    }
}