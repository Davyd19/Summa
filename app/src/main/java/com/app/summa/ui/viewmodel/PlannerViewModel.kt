package com.app.summa.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.summa.data.model.Task
import com.app.summa.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class PlannerUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    // PERBAIKAN: Ganti nama 'tasks' menjadi 'tasksForDay'
    val tasksForDay: List<Task> = emptyList(),
    // PENAMBAHAN: State untuk data mingguan dan bulanan
    val tasksForWeek: List<Task> = emptyList(),
    val tasksForMonth: List<Task> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val initialTaskTitle: String? = null,
    val initialTaskContent: String? = null
)

@HiltViewModel
class PlannerViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlannerUiState())
    val uiState: StateFlow<PlannerUiState> = _uiState.asStateFlow()

    init {
        val noteTitle: String? = savedStateHandle.get<String>("noteTitle")
        val noteContent: String? = savedStateHandle.get<String>("noteContent")

        if (noteTitle != null || noteContent != null) {
            _uiState.update {
                it.copy(
                    initialTaskTitle = noteTitle?.let { URLDecoder.decode(it, "UTF-8") },
                    initialTaskContent = noteContent?.let { URLDecoder.decode(it, "UTF-8") }
                )
            }
        }

        loadAllTasksForDate(_uiState.value.selectedDate)
    }

    // PERBAIKAN: Ubah fungsi ini untuk memuat semua data yang relevan
    private fun loadAllTasksForDate(date: LocalDate) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // 1. Tentukan rentang tanggal Minggu (Senin - Minggu)
            // Asumsi Senin adalah hari pertama dalam seminggu
            val firstDayOfWeek = date.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
            val lastDayOfWeek = firstDayOfWeek.plusDays(6)

            // 2. Tentukan rentang tanggal Bulan
            val firstDayOfMonth = date.with(TemporalAdjusters.firstDayOfMonth())
            val lastDayOfMonth = date.with(TemporalAdjusters.lastDayOfMonth())

            // 3. Gabungkan 3 flow data menggunakan combine
            combine(
                taskRepository.getTasksByDate(date),
                taskRepository.getTasksForDateRange(firstDayOfWeek, lastDayOfWeek),
                taskRepository.getTasksForDateRange(firstDayOfMonth, lastDayOfMonth)
            ) { dayTasks, weekTasks, monthTasks ->
                // Update UI State dengan semua data baru
                _uiState.update {
                    it.copy(
                        tasksForDay = dayTasks,
                        tasksForWeek = weekTasks,
                        tasksForMonth = monthTasks,
                        isLoading = false,
                        error = null
                    )
                }
            }.catch { e ->
                _uiState.update {
                    it.copy(
                        error = e.message,
                        isLoading = false
                    )
                }
            }.collect() // Mulai mengumpulkan flow gabungan
        }
    }

    fun clearInitialTask() {
        _uiState.update { it.copy(initialTaskTitle = null, initialTaskContent = null) }
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
        // PERBAIKAN: Panggil fungsi yang memuat semua data
        loadAllTasksForDate(date)
    }

    fun addTask(
        title: String,
        description: String = "",
        twoMinuteAction: String = "",
        isCommitment: Boolean = true,
        scheduledTime: String? = null
    ) {
        viewModelScope.launch {
            val newTask = Task(
                title = title,
                description = description,
                twoMinuteAction = twoMinuteAction,
                isCommitment = isCommitment,
                scheduledDate = _uiState.value.selectedDate.toString(),
                scheduledTime = scheduledTime
            )
            taskRepository.insertTask(newTask)
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            taskRepository.updateTask(task)
        }
    }

    fun completeTask(taskId: Long) {
        viewModelScope.launch {
            taskRepository.completeTask(taskId)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskRepository.deleteTask(task)
        }
    }
}