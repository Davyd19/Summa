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

// Data class untuk ringkasan
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
        loadReviewData()
    }

    private fun loadReviewData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // 1. Ambil log dan task untuk hari ini
            val today = LocalDate.now()
            val todayHabitLogs = habitRepository.getLogsForDate(today).first()
            val todayTasks = taskRepository.getTasksByDate(today).first()
            val allHabits = habitRepository.getAllHabits().first()

            // 2. Filter yang selesai
            val completedHabits = allHabits.filter { habit ->
                val log = todayHabitLogs.find { it.habitId == habit.id }
                (log?.count ?: 0) >= habit.targetCount && habit.targetCount > 0
            }
            val completedTasks = todayTasks.filter { it.isCompleted }

            // 3. Ambil semua identitas
            val identities = identityRepository.getAllIdentities().first()

            _uiState.update {
                it.copy(
                    summary = DailySummary(completedHabits, completedTasks),
                    identities = identities,
                    isLoading = false
                )
            }
        }
    }

    fun addVote(identity: Identity, points: Int, note: String) {
        viewModelScope.launch {
            // TODO: Simpan 'note' sebagai Jurnal Mikro
            identityRepository.addVoteToIdentity(identity.id, points)
            // TODO: Update progress di 'identities' list
        }
    }

    fun saveReflection(text: String) {
        viewModelScope.launch {
            // TODO: Simpan jurnal umum
            _uiState.update { it.copy(reflectionText = text) }
        }
    }
}