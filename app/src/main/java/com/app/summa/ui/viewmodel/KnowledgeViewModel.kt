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
    // List catatan yang terhubung dengan catatan yang sedang dipilih
    val linkedNotes: List<KnowledgeNote> = emptyList(),
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

    // State untuk pencarian saat linking
    private val _searchResults = MutableStateFlow<List<KnowledgeNote>>(emptyList())
    val searchResults: StateFlow<List<KnowledgeNote>> = _searchResults.asStateFlow()

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
                .collect()
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
                    // Jika ada linked IDs, muat detail catatan-catatan tersebut
                    if (note != null && note.linkedNoteIds.isNotEmpty()) {
                        loadLinkedNotes(note.linkedNoteIds)
                    }
                }
        }
    }

    private fun loadLinkedNotes(idsString: String) {
        viewModelScope.launch {
            val ids = idsString.split(",").mapNotNull { it.trim().toLongOrNull() }
            // Di real app, buat query WHERE id IN (:ids).
            // Di sini kita filter manual dari flow yang ada untuk simplifikasi
            val allNotes = repository.getPermanentNotes().first() + repository.getInboxNotes().first()
            val linked = allNotes.filter { it.id in ids }
            _uiState.update { it.copy(linkedNotes = linked) }
        }
    }

    fun searchNotesForLinking(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                _searchResults.value = emptyList()
                return@launch
            }
            val allNotes = repository.getPermanentNotes().first() + repository.getInboxNotes().first()
            _searchResults.value = allNotes.filter {
                (it.title.contains(query, ignoreCase = true) || it.content.contains(query, ignoreCase = true))
                        && it.id != _uiState.value.selectedNote?.id // Jangan link ke diri sendiri
            }
        }
    }

    fun addLink(targetNote: KnowledgeNote) {
        val currentNote = _uiState.value.selectedNote ?: return
        val currentIds = currentNote.linkedNoteIds.split(",").filter { it.isNotBlank() }.toMutableList()

        if (!currentIds.contains(targetNote.id.toString())) {
            currentIds.add(targetNote.id.toString())
            val newIdsString = currentIds.joinToString(",")

            saveNote(
                title = currentNote.title,
                content = currentNote.content,
                linkedNoteIds = newIdsString // Update IDs
            )
        }
    }

    fun removeLink(noteToRemove: KnowledgeNote) {
        val currentNote = _uiState.value.selectedNote ?: return
        val currentIds = currentNote.linkedNoteIds.split(",").filter { it.isNotBlank() }.toMutableList()

        if (currentIds.remove(noteToRemove.id.toString())) {
            val newIdsString = currentIds.joinToString(",")
            saveNote(
                title = currentNote.title,
                content = currentNote.content,
                linkedNoteIds = newIdsString
            )
        }
    }

    fun saveNote(title: String, content: String, linkedNoteIds: String? = null) {
        viewModelScope.launch {
            val note = _uiState.value.selectedNote
            val currentTime = System.currentTimeMillis()

            if (note != null) {
                repository.updateNote(
                    note.copy(
                        title = title,
                        content = content,
                        linkedNoteIds = linkedNoteIds ?: note.linkedNoteIds, // Pertahankan link lama jika null
                        updatedAt = currentTime
                    )
                )
            } else {
                repository.saveNote(
                    KnowledgeNote(
                        title = title,
                        content = content,
                        linkedNoteIds = linkedNoteIds ?: "",
                        isPermanent = false,
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