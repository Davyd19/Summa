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

data class VoteSuggestion(
    val identity: Identity,
    val reason: String, // "Karena kamu menyelesaikan Lari Pagi"
    val points: Int = 10
)

data class ReflectionUiState(
    val summary: DailySummary? = null,
    val identities: List<Identity> = emptyList(),
    val suggestions: List<VoteSuggestion> = emptyList(), // SMART SUGGESTIONS
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
                // Regenerate suggestions when identities load
                loadSummaryData()
            }
        }
    }

    private fun loadSummaryData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val today = LocalDate.now()
            val todayHabitLogs = habitRepository.getLogsForDate(today).first()
            val todayTasks = taskRepository.getTasksByDate(today).first()
            val allHabits = habitRepository.getAllHabits().first()
            val allIdentities = _uiState.value.identities

            val completedHabits = allHabits.filter { habit ->
                val log = todayHabitLogs.find { it.habitId == habit.id }
                (log?.count ?: 0) >= habit.targetCount && habit.targetCount > 0
            }
            val completedTasks = todayTasks.filter { it.isCompleted }

            // --- LOGIKA SMART SUGGESTION ---
            val suggestions = mutableListOf<VoteSuggestion>()

            // 1. Suggestion dari Habit yang terhubung Identitas
            completedHabits.forEach { habit ->
                if (habit.relatedIdentityId != null) {
                    val identity = allIdentities.find { it.id == habit.relatedIdentityId }
                    if (identity != null) {
                        suggestions.add(
                            VoteSuggestion(
                                identity = identity,
                                reason = "Menyelesaikan habit ${habit.icon} ${habit.name}"
                            )
                        )
                    }
                }
            }

            // 2. Suggestion Generik untuk Task
            if (completedTasks.isNotEmpty()) {
                // Cari identitas "Produktif" atau ambil yang pertama sebagai fallback
                val productiveIdentity = allIdentities.find { it.name.contains("Produktif", ignoreCase = true) }
                    ?: allIdentities.firstOrNull()

                if (productiveIdentity != null) {
                    suggestions.add(
                        VoteSuggestion(
                            identity = productiveIdentity,
                            reason = "Menyelesaikan ${completedTasks.size} tugas hari ini",
                            points = completedTasks.size * 2
                        )
                    )
                }
            }

            _uiState.update {
                it.copy(
                    summary = DailySummary(completedHabits, completedTasks),
                    suggestions = suggestions,
                    isLoading = false
                )
            }
        }
    }

    fun addVote(identity: Identity, points: Int, note: String) {
        viewModelScope.launch {
            identityRepository.addVoteToIdentity(identity.id, points, note)
            // Hapus suggestion setelah divote agar tidak duplikat (opsional UX)
            _uiState.update { state ->
                state.copy(suggestions = state.suggestions.filter { it.identity.id != identity.id })
            }
        }
    }

    fun saveReflection(text: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(reflectionText = text) }
        }
    }
}