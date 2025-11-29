package com.app.summa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FocusUiState(
    val timeRemaining: Int = 0,
    val isRunning: Boolean = false,
    val paperclipsMoved: Int = 0,
    val paperclipsLeft: Int = 10,
    val startTime: Long = 0L,
    val targetClips: Int = 10
)

@HiltViewModel
class FocusViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(FocusUiState())
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun initializeSession(initialTarget: Int) {
        if (_uiState.value.startTime == 0L) { // Hanya init jika belum berjalan
            _uiState.update {
                it.copy(
                    targetClips = initialTarget,
                    paperclipsLeft = initialTarget,
                    timeRemaining = 0, // Timer naik (elapsed time)
                    startTime = System.currentTimeMillis()
                )
            }
        }
    }

    fun startTimer() {
        if (_uiState.value.isRunning) return

        _uiState.update { it.copy(isRunning = true) }

        timerJob = viewModelScope.launch {
            while (_uiState.value.isRunning) {
                delay(1000)
                _uiState.update { it.copy(timeRemaining = it.timeRemaining + 1) }
            }
        }
    }

    fun pauseTimer() {
        _uiState.update { it.copy(isRunning = false) }
        timerJob?.cancel()
    }

    fun movePaperclip() {
        _uiState.update {
            it.copy(
                paperclipsLeft = (it.paperclipsLeft - 1).coerceAtLeast(0),
                paperclipsMoved = it.paperclipsMoved + 1
            )
        }

        // Auto-start timer on first clip move
        if (!_uiState.value.isRunning && _uiState.value.paperclipsMoved > 0) {
            startTimer()
        }
    }

    // Reset state saat sesi selesai/batal
    fun reset() {
        timerJob?.cancel()
        _uiState.value = FocusUiState()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}