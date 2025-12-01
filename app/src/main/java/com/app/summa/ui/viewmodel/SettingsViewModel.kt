package com.app.summa.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.summa.data.repository.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.InputStreamReader
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun exportData(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading("Mengekspor data...")
            try {
                val jsonString = backupRepository.createBackupJson()

                context.contentResolver.openFileDescriptor(uri, "w")?.use { pfd ->
                    FileOutputStream(pfd.fileDescriptor).use { output ->
                        output.write(jsonString.toByteArray())
                    }
                }
                _uiState.value = SettingsUiState.Success("Backup berhasil disimpan!")
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error("Gagal backup: ${e.localizedMessage}")
            }
        }
    }

    fun importData(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading("Mengembalikan data...")
            try {
                val stringBuilder = StringBuilder()
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        var line: String? = reader.readLine()
                        while (line != null) {
                            stringBuilder.append(line)
                            line = reader.readLine()
                        }
                    }
                }

                backupRepository.restoreBackupFromJson(stringBuilder.toString())
                _uiState.value = SettingsUiState.Success("Data berhasil dipulihkan! Restart aplikasi disarankan.")
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error("Gagal restore: File rusak atau tidak valid.")
            }
        }
    }

    fun resetState() {
        _uiState.value = SettingsUiState.Idle
    }
}

sealed class SettingsUiState {
    object Idle : SettingsUiState()
    data class Loading(val message: String) : SettingsUiState()
    data class Success(val message: String) : SettingsUiState()
    data class Error(val message: String) : SettingsUiState()
}