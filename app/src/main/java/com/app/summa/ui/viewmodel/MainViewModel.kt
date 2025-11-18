package com.app.summa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * ViewModel global yang menampung state seluruh aplikasi,
 * seperti Mode Kontekstual saat ini.
 * Di-inject di level Activity.
 */
data class MainUiState(
    val currentMode: String = "Normal" // Mode default
)

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    /**
     * Mengatur Mode Kontekstual untuk seluruh aplikasi.
     * Ini akan mengubah UI secara dinamis (misal: BottomNav).
     */
    fun setMode(mode: String) {
        _uiState.update { it.copy(currentMode = mode) }
    }
}