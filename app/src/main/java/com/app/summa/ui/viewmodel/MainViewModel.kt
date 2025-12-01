package com.app.summa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.summa.data.model.DailyWrapUpResult
import com.app.summa.data.model.LevelUpEvent
import com.app.summa.data.repository.IdentityRepository
import com.app.summa.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val currentMode: String = "Normal",
    val morningBriefing: DailyWrapUpResult? = null,
    // STATE BARU: Menyimpan event level up aktif
    val levelUpEvent: LevelUpEvent? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val identityRepository: IdentityRepository // Inject IdentityRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        runDailySystemCheck()
        observeLevelUpEvents() // Mulai dengarkan event
    }

    private fun observeLevelUpEvents() {
        viewModelScope.launch {
            identityRepository.levelUpEvents.collect { event ->
                // Saat event diterima, update state untuk memunculkan dialog
                _uiState.update { it.copy(levelUpEvent = event) }
            }
        }
    }

    private fun runDailySystemCheck() {
        viewModelScope.launch {
            try {
                val result = taskRepository.processDailyWrapUp()
                if (result != null) {
                    _uiState.update { it.copy(morningBriefing = result) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setMode(mode: String) {
        _uiState.update { it.copy(currentMode = mode) }
    }

    fun dismissBriefing() {
        _uiState.update { it.copy(morningBriefing = null) }
    }

    // Fungsi menutup dialog Level Up
    fun dismissLevelUp() {
        _uiState.update { it.copy(levelUpEvent = null) }
    }
}