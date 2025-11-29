package com.app.summa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.summa.data.model.Habit
import com.app.summa.data.model.Identity
import com.app.summa.data.model.KnowledgeNote
import com.app.summa.data.model.Task
import com.app.summa.data.repository.HabitRepository
import com.app.summa.data.repository.IdentityRepository
import com.app.summa.data.repository.KnowledgeRepository
import com.app.summa.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class DailySummary(
    val completedHabits: List<Habit>,
    val completedTasks: List<Task>
)

data class VoteSuggestion(
    val identity: Identity,
    val reason: String,
    val points: Int = 10
)

data class ReflectionUiState(
    val summary: DailySummary? = null,
    val identities: List<Identity> = emptyList(),
    val suggestions: List<VoteSuggestion> = emptyList(),
    val reflectionText: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class ReflectionViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val taskRepository: TaskRepository,
    private val identityRepository: IdentityRepository,
    private val knowledgeRepository: KnowledgeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReflectionUiState())
    val uiState: StateFlow<ReflectionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            identityRepository.getAllIdentities().collect { identities ->
                _uiState.update { it.copy(identities = identities) }
                loadSummaryData()
            }
        }
    }

    private fun loadSummaryData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // PERBAIKAN LOGIKA WAKTU:
            // Jika sekarang sebelum jam 04:00 pagi, anggap ini refleksi untuk "Kemarin".
            val now = LocalTime.now()
            val effectiveDate = if (now.isBefore(LocalTime.of(4, 0))) {
                LocalDate.now().minusDays(1)
            } else {
                LocalDate.now()
            }

            // Ambil data berdasarkan effectiveDate
            val dailyHabitLogs = habitRepository.getLogsForDate(effectiveDate).first()
            val dailyTasks = taskRepository.getTasksByDate(effectiveDate).first()
            val allHabits = habitRepository.getAllHabits().first()
            val allIdentities = _uiState.value.identities

            val completedHabits = allHabits.filter { habit ->
                val log = dailyHabitLogs.find { it.habitId == habit.id }
                (log?.count ?: 0) >= habit.targetCount && habit.targetCount > 0
            }
            val completedTasks = dailyTasks.filter { it.isCompleted }

            val suggestions = mutableListOf<VoteSuggestion>()

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

            if (completedTasks.isNotEmpty()) {
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
            _uiState.update { state ->
                state.copy(suggestions = state.suggestions.filter { it.identity.id != identity.id })
            }
        }
    }

    fun updateReflectionText(text: String) {
        _uiState.update { it.copy(reflectionText = text) }
    }

    fun completeReflection() {
        viewModelScope.launch {
            val text = _uiState.value.reflectionText
            if (text.isNotBlank()) {
                // Gunakan tanggal efektif juga untuk judul catatan
                val now = LocalTime.now()
                val effectiveDate = if (now.isBefore(LocalTime.of(4, 0))) {
                    LocalDate.now().minusDays(1)
                } else {
                    LocalDate.now()
                }

                val dateStr = effectiveDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id", "ID")))
                val newNote = KnowledgeNote(
                    title = "Refleksi Harian: $dateStr",
                    content = text,
                    tags = "#jurnal, #refleksi",
                    isPermanent = true,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                knowledgeRepository.saveNote(newNote)
            }
            _uiState.update { it.copy(reflectionText = "") }
        }
    }
}