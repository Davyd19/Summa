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
    val completedTasks: List<Task>,
    // FIELD BARU: Statistik untuk penilaian
    val totalCommitments: Int,
    val completedCommitments: Int,
    val dailyScore: Int, // 0 - 100
    val dailyGrade: String // S, A, B, C
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

            // Logika Waktu: Sebelum jam 04:00 pagi dianggap hari "Kemarin"
            val now = LocalTime.now()
            val effectiveDate = if (now.isBefore(LocalTime.of(4, 0))) {
                LocalDate.now().minusDays(1)
            } else {
                LocalDate.now()
            }

            // Ambil data
            val dailyHabitLogs = habitRepository.getLogsForDate(effectiveDate).first()
            val dailyTasks = taskRepository.getTasksByDate(effectiveDate).first()
            val allHabits = habitRepository.getAllHabits().first()
            val allIdentities = _uiState.value.identities

            // 1. Filter Habit Selesai
            val completedHabits = allHabits.filter { habit ->
                val log = dailyHabitLogs.find { it.habitId == habit.id }
                (log?.count ?: 0) >= habit.targetCount && habit.targetCount > 0
            }

            // 2. Filter Tugas Selesai
            val completedTasks = dailyTasks.filter { it.isCompleted }

            // 3. Hitung Statistik Komitmen (Untuk Scoring)
            val commitments = dailyTasks.filter { it.isCommitment }
            val totalCommitments = commitments.size
            val completedCommitments = commitments.count { it.isCompleted }

            // 4. Kalkulasi Skor (Bobot: Komitmen 70%, Habit 30%)
            val commitmentScore = if (totalCommitments > 0) {
                (completedCommitments.toFloat() / totalCommitments) * 70
            } else {
                70f // Jika tidak ada komitmen, anggap full points (bonus hari santai)
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

            // 5. Generate Suggestions untuk Voting Identitas
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
                // Cari identitas "Produktif" atau default ke yang levelnya paling tinggi
                val productiveIdentity = allIdentities.find {
                    it.name.contains("Produktif", ignoreCase = true) ||
                            it.name.contains("Kerja", ignoreCase = true)
                } ?: allIdentities.maxByOrNull { it.progress }

                if (productiveIdentity != null) {
                    suggestions.add(
                        VoteSuggestion(
                            identity = productiveIdentity,
                            reason = "Menyelesaikan ${completedTasks.size} tugas hari ini",
                            points = completedTasks.size * 2 // 2 poin per tugas
                        )
                    )
                }
            }

            _uiState.update {
                it.copy(
                    summary = DailySummary(
                        completedHabits = completedHabits,
                        completedTasks = completedTasks,
                        totalCommitments = totalCommitments,
                        completedCommitments = completedCommitments,
                        dailyScore = totalScore,
                        dailyGrade = grade
                    ),
                    suggestions = suggestions,
                    isLoading = false
                )
            }
        }
    }

    fun addVote(identity: Identity, points: Int, note: String) {
        viewModelScope.launch {
            identityRepository.addVoteToIdentity(identity.id, points, note)
            // Hapus saran setelah divote
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

            // Simpan catatan refleksi meskipun kosong (sebagai tanda hari itu ditutup)
            val now = LocalTime.now()
            val effectiveDate = if (now.isBefore(LocalTime.of(4, 0))) {
                LocalDate.now().minusDays(1)
            } else {
                LocalDate.now()
            }

            val dateStr = effectiveDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id", "ID")))

            // Format isi jurnal lebih rapi dengan ringkasan otomatis
            val summary = _uiState.value.summary
            val autoSummary = """
                |
                |--- Ringkasan Sistem ---
                |Skor: ${summary?.dailyScore ?: 0} (Grade ${summary?.dailyGrade ?: "-"})
                |Komitmen: ${summary?.completedCommitments}/${summary?.totalCommitments}
                |Kebiasaan: ${summary?.completedHabits?.size ?: 0} Selesai
            """.trimMargin()

            val finalContent = if (text.isNotBlank()) "$text$autoSummary" else "Tidak ada catatan manual.$autoSummary"

            val newNote = KnowledgeNote(
                title = "Refleksi: $dateStr",
                content = finalContent,
                tags = "#jurnal, #refleksi, #grade_${summary?.dailyGrade ?: "N"}",
                isPermanent = true, // Langsung ke Pustaka
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            knowledgeRepository.saveNote(newNote)

            _uiState.update { it.copy(reflectionText = "") }
        }
    }
}