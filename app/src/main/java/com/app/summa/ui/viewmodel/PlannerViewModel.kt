package com.app.summa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.summa.data.model.DailyWrapUpResult
import com.app.summa.data.model.FocusSession
import com.app.summa.data.model.Identity
import com.app.summa.data.model.Task
import com.app.summa.data.repository.FocusRepository
import com.app.summa.data.repository.IdentityRepository
import com.app.summa.data.repository.TaskRepository
import com.app.summa.util.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class PlannerUiState(
    // Memuat tugas sebulan penuh untuk keperluan kalender
    val tasks: List<Task> = emptyList(),
    val identities: List<Identity> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val isLoading: Boolean = false,
    val morningBriefing: DailyWrapUpResult? = null
)

@HiltViewModel
class PlannerViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val identityRepository: IdentityRepository,
    private val focusRepository: FocusRepository,
    private val notificationScheduler: NotificationScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlannerUiState(isLoading = true))
    val uiState: StateFlow<PlannerUiState> = _uiState.asStateFlow()

    init {
        // Load data: Trigger ulang setiap kali bulan dari selectedDate berubah
        viewModelScope.launch {
            _uiState.map { it.selectedDate }
                .distinctUntilChanged { old, new ->
                    old.year == new.year && old.month == new.month
                } // Hanya refresh jika bulan berubah
                .flatMapLatest { date ->
                    val startOfMonth = date.with(TemporalAdjusters.firstDayOfMonth())
                    val endOfMonth = date.with(TemporalAdjusters.lastDayOfMonth())

                    combine(
                        taskRepository.getTasksForDateRange(startOfMonth, endOfMonth),
                        identityRepository.getAllIdentities()
                    ) { tasks, identities ->
                        Pair(tasks, identities)
                    }
                }.collect { (tasks, identities) ->
                    _uiState.update {
                        it.copy(
                            tasks = tasks,
                            identities = identities,
                            isLoading = false
                        )
                    }
                }
        }

        checkMorningBriefing()
    }

    fun selectDate(date: LocalDate) {
        // onDateSelected / selectDate alias
        _uiState.update { it.copy(selectedDate = date) }
    }

    // Alias untuk kompatibilitas
    fun onDateSelected(date: LocalDate) = selectDate(date)

    fun saveTask(
        id: Long,
        title: String,
        description: String,
        time: String,
        isCommitment: Boolean,
        identityId: Long?,
        twoMinuteAction: String
    ) {
        viewModelScope.launch {
            val currentDateStr = _uiState.value.selectedDate.toString()

            if (id == 0L) {
                val newTask = Task(
                    title = title,
                    description = description,
                    scheduledDate = currentDateStr,
                    scheduledTime = time.ifBlank { null },
                    isCommitment = isCommitment,
                    relatedIdentityId = identityId,
                    twoMinuteAction = twoMinuteAction,
                    createdAt = System.currentTimeMillis()
                )
                val newId = taskRepository.insertTask(newTask)
                notificationScheduler.scheduleTaskNotification(newTask.copy(id = newId))
            } else {
                val existingTask = _uiState.value.tasks.find { it.id == id } ?: return@launch
                val updatedTask = existingTask.copy(
                    title = title,
                    description = description,
                    scheduledDate = currentDateStr,
                    scheduledTime = time.ifBlank { null },
                    isCommitment = isCommitment,
                    relatedIdentityId = identityId,
                    twoMinuteAction = twoMinuteAction
                )

                taskRepository.updateTask(updatedTask)
                notificationScheduler.cancelTaskNotification(existingTask)
                notificationScheduler.scheduleTaskNotification(updatedTask)
            }
        }
    }

    // Alias agar sesuai dengan PlannerScreen.kt
    fun addTask(title: String, description: String, time: String, isCommitment: Boolean, twoMinuteAction: String, identityId: Long?) {
        saveTask(0L, title, description, time, isCommitment, identityId, twoMinuteAction)
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            if (task.isCompleted) {
                val reopenedTask = task.copy(isCompleted = false, completedAt = null)
                taskRepository.updateTask(reopenedTask)
                notificationScheduler.scheduleTaskNotification(reopenedTask)
            } else {
                taskRepository.completeTask(task.id)
                notificationScheduler.cancelTaskNotification(task)
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskRepository.deleteTask(task)
            notificationScheduler.cancelTaskNotification(task)
        }
    }

    // Fitur Drag & Drop Time Slot
    fun moveTaskToTime(task: Task, hour: Int) {
        viewModelScope.launch {
            // Format jam "08:00", "14:00"
            val newTime = LocalTime.of(hour, 0).format(DateTimeFormatter.ofPattern("HH:mm"))
            val updatedTask = task.copy(scheduledTime = newTime)

            taskRepository.updateTask(updatedTask)

            notificationScheduler.cancelTaskNotification(task)
            notificationScheduler.scheduleTaskNotification(updatedTask)
        }
    }

    fun saveFocusSession(taskId: Long, clips: Int, startTime: Long) {
        viewModelScope.launch {
            val session = FocusSession(
                taskId = taskId,
                startTime = startTime,
                endTime = System.currentTimeMillis(),
                paperclipsCollected = clips,
                createdAt = System.currentTimeMillis()
            )
            focusRepository.saveSession(session)
        }
    }

    fun completeTask(taskId: Long) {
        viewModelScope.launch {
            taskRepository.completeTask(taskId)
        }
    }

    private fun checkMorningBriefing() {
        viewModelScope.launch {
            val result = taskRepository.processDailyWrapUp()
            if (result != null) {
                _uiState.update { it.copy(morningBriefing = result) }
            }
        }
    }

    fun dismissBriefing() {
        _uiState.update { it.copy(morningBriefing = null) }
    }

    // Placeholder agar PlannerScreen tidak error (logic sudah dipindah ke Screen via LaunchedEffect)
    fun clearInitialTask() { }
}