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
    val sessionStartTimeStamp: Long = 0L, // Waktu mulai sesi sebenarnya
    val targetClips: Int = 10
)

@HiltViewModel
class FocusViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(FocusUiState())
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun initializeSession(initialTarget: Int) {
        if (_uiState.value.startTime == 0L) {
            _uiState.update {
                it.copy(
                    targetClips = initialTarget,
                    paperclipsLeft = initialTarget,
                    timeRemaining = 0,
                    startTime = 0L
                )
            }
        }
    }

    fun startTimer() {
        if (_uiState.value.isRunning) return

        // Set start time stamp logic
        val currentTime = System.currentTimeMillis()
        // Jika melanjutkan pause, sesuaikan timestamp agar waktu tidak melompat
        val adjustedStartTime = if (_uiState.value.sessionStartTimeStamp == 0L) {
            currentTime
        } else {
            currentTime - (_uiState.value.timeRemaining * 1000L)
        }

        _uiState.update {
            it.copy(
                isRunning = true,
                sessionStartTimeStamp = adjustedStartTime
            )
        }

        timerJob = viewModelScope.launch {
            while (_uiState.value.isRunning) {
                // Update UI setiap 200ms agar responsif, tapi hitungan tetap akurat
                delay(200)

                val now = System.currentTimeMillis()
                val elapsedSeconds = (now - _uiState.value.sessionStartTimeStamp) / 1000

                _uiState.update { it.copy(timeRemaining = elapsedSeconds.toInt()) }
            }
        }
    }

    fun pauseTimer() {
        _uiState.update { it.copy(isRunning = false) }
        timerJob?.cancel()
    }

    fun movePaperclip() {
        _uiState.update {
            val newLeft = (it.paperclipsLeft - 1).coerceAtLeast(0)
            it.copy(
                paperclipsLeft = newLeft,
                paperclipsMoved = it.paperclipsMoved + 1
            )
        }

        val currentState = _uiState.value

        // Logika Baru: Auto-Start saat klip pertama dipindah
        if (!currentState.isRunning && currentState.paperclipsMoved > 0 && currentState.paperclipsLeft > 0) {
            startTimer()
        }

        // Logika Baru: Auto-Stop saat target selesai
        if (currentState.paperclipsLeft == 0) {
            pauseTimer()
        }
    }

    fun reset() {
        timerJob?.cancel()
        _uiState.value = FocusUiState()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}