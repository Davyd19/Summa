package com.app.summa.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.summa.data.model.FocusSession
import com.app.summa.data.model.Identity
import com.app.summa.data.model.Task
import com.app.summa.data.repository.FocusRepository
import com.app.summa.data.repository.IdentityRepository
import com.app.summa.data.repository.TaskRepository
import com.app.summa.util.NotificationScheduler // Import Scheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class PlannerUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val tasksForDay: List<Task> = emptyList(),
    val tasksForWeek: List<Task> = emptyList(),
    val tasksForMonth: List<Task> = emptyList(),
    val availableIdentities: List<Identity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val initialTaskTitle: String? = null,
    val initialTaskContent: String? = null
)

@HiltViewModel
class PlannerViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val focusRepository: FocusRepository,
    private val identityRepository: IdentityRepository,
    private val savedStateHandle: SavedStateHandle,
    // INJECT SCHEDULER
    private val notificationScheduler: NotificationScheduler
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
        loadIdentities()
    }

    private fun loadIdentities() {
        viewModelScope.launch {
            identityRepository.getAllIdentities().collect { identities ->
                _uiState.update { it.copy(availableIdentities = identities) }
            }
        }
    }

    private fun loadAllTasksForDate(date: LocalDate) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val firstDayOfWeek = date.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
            val lastDayOfWeek = firstDayOfWeek.plusDays(6)
            val firstDayOfMonth = date.with(TemporalAdjusters.firstDayOfMonth())
            val lastDayOfMonth = date.with(TemporalAdjusters.lastDayOfMonth())

            combine(
                taskRepository.getTasksByDate(date),
                taskRepository.getTasksForDateRange(firstDayOfWeek, lastDayOfWeek),
                taskRepository.getTasksForDateRange(firstDayOfMonth, lastDayOfMonth)
            ) { dayTasks, weekTasks, monthTasks ->
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
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }.collect()
        }
    }

    fun clearInitialTask() {
        _uiState.update { it.copy(initialTaskTitle = null, initialTaskContent = null) }
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
        loadAllTasksForDate(date)
    }

    fun addTask(
        title: String,
        description: String = "",
        scheduledTime: String? = null,
        isCommitment: Boolean = true,
        twoMinuteAction: String = "",
        relatedIdentityId: Long? = null
    ) {
        viewModelScope.launch {
            val dateStr = _uiState.value.selectedDate.toString()
            val newTask = Task(
                title = title,
                description = description,
                twoMinuteAction = twoMinuteAction,
                isCommitment = isCommitment,
                relatedIdentityId = relatedIdentityId,
                scheduledDate = dateStr,
                scheduledTime = scheduledTime
            )
            val newId = taskRepository.insertTask(newTask)

            // JADWALKAN NOTIFIKASI
            if (scheduledTime != null && scheduledTime.isNotBlank()) {
                notificationScheduler.scheduleTaskReminder(newId, title, dateStr, scheduledTime)
            }
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            taskRepository.updateTask(task)
            // UPDATE JADWAL (Cancel lama, set baru jika ada waktu)
            notificationScheduler.cancelTaskReminder(task.id)
            if (task.scheduledDate != null && task.scheduledTime != null) {
                notificationScheduler.scheduleTaskReminder(task.id, task.title, task.scheduledDate, task.scheduledTime)
            }
        }
    }

    // Drag & Drop Time Update
    fun moveTaskToTime(task: Task, newHour: Int) {
        viewModelScope.launch {
            val newTimeStr = String.format("%02d:00", newHour)
            if (task.scheduledTime == newTimeStr) return@launch

            val updatedTask = task.copy(scheduledTime = newTimeStr)
            taskRepository.updateTask(updatedTask)

            // UPDATE NOTIFIKASI
            notificationScheduler.cancelTaskReminder(task.id)
            if (task.scheduledDate != null) {
                notificationScheduler.scheduleTaskReminder(task.id, task.title, task.scheduledDate, newTimeStr)
            }
        }
    }

    fun completeTask(taskId: Long) {
        viewModelScope.launch {
            taskRepository.completeTask(taskId)
            // Hapus notifikasi jika tugas selesai (agar tidak bunyi jika belum waktunya)
            notificationScheduler.cancelTaskReminder(taskId)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskRepository.deleteTask(task)
            // Hapus notifikasi
            notificationScheduler.cancelTaskReminder(task.id)
        }
    }

    fun saveFocusSession(taskId: Long, paperclips: Int, startTime: Long) {
        viewModelScope.launch {
            val endTime = System.currentTimeMillis()
            val session = FocusSession(
                taskId = taskId,
                habitId = null,
                startTime = startTime,
                endTime = endTime,
                paperclipsCollected = paperclips,
                createdAt = endTime
            )
            focusRepository.saveSession(session)
        }
    }
}