package com.app.summa.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.summa.data.model.KnowledgeNote
import com.app.summa.data.repository.KnowledgeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KnowledgeUiState(
    val inboxNotes: List<KnowledgeNote> = emptyList(),
    val permanentNotes: List<KnowledgeNote> = emptyList(),
    val selectedNote: KnowledgeNote? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class KnowledgeViewModel @Inject constructor(
    private val repository: KnowledgeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(KnowledgeUiState())
    val uiState: StateFlow<KnowledgeUiState> = _uiState.asStateFlow()

    private val noteId: Long = savedStateHandle.get<Long>("noteId") ?: 0L

    init {
        if (noteId > 0) {
            loadNote(noteId)
        } else {
            loadAllNotes()
        }
    }

    private fun loadAllNotes() {
        viewModelScope.launch {
            repository.getInboxNotes()
                .combine(repository.getPermanentNotes()) { inbox, permanent ->
                    _uiState.update {
                        it.copy(
                            inboxNotes = inbox,
                            permanentNotes = permanent,
                            isLoading = false
                        )
                    }
                }
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect() // Mulai flow
        }
    }

    private fun loadNote(id: Long) {
        viewModelScope.launch {
            repository.getNoteById(id)
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { note ->
                    _uiState.update { it.copy(isLoading = false, selectedNote = note) }
                }
        }
    }

    fun saveNote(title: String, content: String) {
        viewModelScope.launch {
            val note = _uiState.value.selectedNote
            val currentTime = System.currentTimeMillis()

            if (note != null) {
                // Update catatan yang ada
                repository.updateNote(
                    note.copy(
                        title = title,
                        content = content,
                        updatedAt = currentTime
                    )
                )
            } else {
                // Buat catatan baru (selalu masuk ke Inbox dulu)
                repository.saveNote(
                    KnowledgeNote(
                        title = title,
                        content = content,
                        isPermanent = false, // Baru selalu di Inbox
                        createdAt = currentTime,
                        updatedAt = currentTime
                    )
                )
            }
        }
    }

    fun deleteNote() {
        viewModelScope.launch {
            _uiState.value.selectedNote?.let {
                repository.deleteNote(it)
            }
        }
    }

    fun convertToPermanent() {
        viewModelScope.launch {
            _uiState.value.selectedNote?.let {
                repository.convertToPermanent(it)
            }
        }
    }
}