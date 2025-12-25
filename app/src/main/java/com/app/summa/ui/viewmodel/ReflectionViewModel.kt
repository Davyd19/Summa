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
    val completedHabits: List<Habit> = emptyList(),
    val completedTasks: List<Task> = emptyList(),
    val totalCommitments: Int = 0,
    val completedCommitments: Int = 0,
    val dailyScore: Int = 0,
    val dailyGrade: String = "-"
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

    // Input teks manual dari user
    private val _reflectionText = MutableStateFlow("")

    // Logika Waktu: Sebelum jam 04:00 pagi dianggap hari "Kemarin"
    private val effectiveDate: LocalDate
        get() {
            val now = LocalTime.now()
            return if (now.isBefore(LocalTime.of(4, 0))) {
                LocalDate.now().minusDays(1)
            } else {
                LocalDate.now()
            }
        }

    // --- STATE UTAMA (REAKTIF) ---
    // Menggabungkan semua sumber data. UI akan otomatis update jika DB berubah.
    val uiState: StateFlow<ReflectionUiState> = combine(
        identityRepository.getAllIdentities(),
        habitRepository.getLogsForDate(effectiveDate),
        taskRepository.getTasksByDate(effectiveDate),
        habitRepository.getAllHabits(),
        _reflectionText
    ) { identities, dailyLogs, dailyTasks, allHabits, text ->

        // 1. Kalkulasi Data Summary
        val completedHabits = allHabits.filter { habit ->
            val log = dailyLogs.find { it.habitId == habit.id }
            (log?.count ?: 0) >= habit.targetCount && habit.targetCount > 0
        }

        val completedTasks = dailyTasks.filter { it.isCompleted }
        val commitments = dailyTasks.filter { it.isCommitment }
        val totalCommitments = commitments.size
        val completedCommitments = commitments.count { it.isCompleted }

        // 2. Scoring System
        val commitmentScore = if (totalCommitments > 0) {
            (completedCommitments.toFloat() / totalCommitments) * 70
        } else {
            70f // Bonus jika tidak ada komitmen berat
        }

        val habitScore = if (allHabits.isNotEmpty()) {
            (completedHabits.size.toFloat() / allHabits.size) * 30
        } else {
            30f
        }

        val totalScore = (commitmentScore + habitScore).toInt().coerceIn(0, 100)
        val grade = when {
            totalScore >= 90 -> "S"
            totalScore >= 80 -> "A"
            totalScore >= 60 -> "B"
            totalScore >= 40 -> "C"
            else -> "D"
        }

        // 3. Generate Suggestions
        val suggestions = mutableListOf<VoteSuggestion>()
        completedHabits.forEach { habit ->
            if (habit.relatedIdentityId != null) {
                val identity = identities.find { it.id == habit.relatedIdentityId }
                if (identity != null) {
                    suggestions.add(VoteSuggestion(identity, "Menyelesaikan habit ${habit.icon}"))
                }
            }
        }

        // Return State Baru
        ReflectionUiState(
            summary = DailySummary(
                completedHabits = completedHabits,
                completedTasks = completedTasks,
                totalCommitments = totalCommitments,
                completedCommitments = completedCommitments,
                dailyScore = totalScore,
                dailyGrade = grade
            ),
            identities = identities,
            suggestions = suggestions,
            reflectionText = text,
            isLoading = false // Data sudah siap
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReflectionUiState(isLoading = true)
    )

    fun addVote(identity: Identity, points: Int, note: String) {
        viewModelScope.launch {
            identityRepository.addVoteToIdentity(identity.id, points, note)
        }
    }

    fun updateReflectionText(text: String) {
        _reflectionText.value = text
    }

    fun completeReflection() {
        viewModelScope.launch {
            val currentState = uiState.value
            val summary = currentState.summary
            val text = currentState.reflectionText
            val dateStr = effectiveDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id", "ID")))

            val autoSummary = """
                |
                |--- Ringkasan Sistem ---
                |Skor: ${summary?.dailyScore ?: 0} (Grade ${summary?.dailyGrade ?: "-"})
                |Komitmen: ${summary?.completedCommitments}/${summary?.totalCommitments}
                |Kebiasaan: ${summary?.completedHabits?.size ?: 0} Selesai
            """.trimMargin()

            val finalContent = if (text.isNotBlank()) "$text$autoSummary" else "Refleksi selesai.$autoSummary"

            val newNote = KnowledgeNote(
                title = "Refleksi: $dateStr",
                content = finalContent,
                tags = "#jurnal, #refleksi, #grade_${summary?.dailyGrade ?: "N"}",
                isPermanent = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            knowledgeRepository.saveNote(newNote)

            // Reset text
            _reflectionText.value = ""
        }
    }
}