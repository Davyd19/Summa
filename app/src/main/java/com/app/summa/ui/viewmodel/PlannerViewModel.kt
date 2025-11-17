package com.app.summa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.summa.data.model.Task
import com.app.summa.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class PlannerUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class PlannerViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlannerUiState())
    val uiState: StateFlow<PlannerUiState> = _uiState.asStateFlow()

    init {
        loadTasks()
    }

    private fun loadTasks() {
        viewModelScope.launch {
            taskRepository.getTasksByDate(_uiState.value.selectedDate)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message,
                        isLoading = false
                    )
                }
                .collect { tasks ->
                    _uiState.value = _uiState.value.copy(
                        tasks = tasks,
                        isLoading = false,
                        error = null
                    )
                }
        }
    }

    fun selectDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
        loadTasks()
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
