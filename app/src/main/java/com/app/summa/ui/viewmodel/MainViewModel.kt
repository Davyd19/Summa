package com.app.summa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.summa.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val currentMode: String = "Normal"
)

@HiltViewModel
class MainViewModel @Inject constructor(
    // INJECT TaskRepository
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        // JALANKAN LOGIKA HARIAN SAAT APP DIBUKA
        runDailySystemCheck()
    }

    private fun runDailySystemCheck() {
        viewModelScope.launch {
            try {
                // Otomatis pindahkan tugas "Aspirasi" yang tertunda ke hari ini
                // Tugas "Komitmen" dibiarkan tertinggal (sebagai konsekuensi/hutang)
                taskRepository.processDailyWrapUp()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setMode(mode: String) {
        _uiState.update { it.copy(currentMode = mode) }
    }
}