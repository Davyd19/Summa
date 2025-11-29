package com.app.summa.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.summa.data.model.KnowledgeNote
import com.app.summa.data.model.NoteLink
import com.app.summa.data.repository.KnowledgeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KnowledgeUiState(
    val inboxNotes: List<KnowledgeNote> = emptyList(),
    val permanentNotes: List<KnowledgeNote> = emptyList(),
    val allLinks: List<NoteLink> = emptyList(),

    val selectedNote: KnowledgeNote? = null,
    val forwardLinks: List<KnowledgeNote> = emptyList(),
    val backlinks: List<KnowledgeNote> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val allLinkedNotes: List<KnowledgeNote>
        get() = (forwardLinks + backlinks).distinctBy { it.id }
}

@HiltViewModel
class KnowledgeViewModel @Inject constructor(
    private val repository: KnowledgeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(KnowledgeUiState())
    val uiState: StateFlow<KnowledgeUiState> = _uiState.asStateFlow()

    private val _searchResults = MutableStateFlow<List<KnowledgeNote>>(emptyList())
    val searchResults: StateFlow<List<KnowledgeNote>> = _searchResults.asStateFlow()

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
            // PERBAIKAN: Menggunakan repository.getAllLinks() yang asli
            combine(
                repository.getInboxNotes(),
                repository.getPermanentNotes(),
                repository.getAllLinks()
            ) { inbox, permanent, links ->
                _uiState.update {
                    it.copy(
                        inboxNotes = inbox,
                        permanentNotes = permanent,
                        allLinks = links,
                        isLoading = false
                    )
                }
            }
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .collect()
        }
    }

    private fun loadNote(id: Long) {
        viewModelScope.launch {
            repository.getNoteById(id).collectLatest { note ->
                _uiState.update { it.copy(selectedNote = note) }
            }
        }
        viewModelScope.launch {
            repository.getForwardLinks(id).collectLatest { links ->
                _uiState.update { it.copy(forwardLinks = links) }
            }
        }
        viewModelScope.launch {
            repository.getBacklinks(id).collectLatest { links ->
                _uiState.update { it.copy(backlinks = links) }
            }
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
                        && it.id != _uiState.value.selectedNote?.id
            }
        }
    }

    fun addLink(targetNote: KnowledgeNote) {
        val currentNoteId = _uiState.value.selectedNote?.id ?: return
        viewModelScope.launch {
            repository.addLink(currentNoteId, targetNote.id)
        }
    }

    fun removeLink(targetNote: KnowledgeNote) {
        val currentNoteId = _uiState.value.selectedNote?.id ?: return
        viewModelScope.launch {
            repository.removeLink(currentNoteId, targetNote.id)
        }
    }

    fun saveNote(title: String, content: String) {
        viewModelScope.launch {
            val note = _uiState.value.selectedNote
            val currentTime = System.currentTimeMillis()

            if (note != null) {
                repository.updateNote(note.copy(title = title, content = content, updatedAt = currentTime))
            } else {
                repository.saveNote(
                    KnowledgeNote(
                        title = title,
                        content = content,
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
            _uiState.value.selectedNote?.let { repository.deleteNote(it) }
        }
    }

    fun convertToPermanent() {
        viewModelScope.launch {
            _uiState.value.selectedNote?.let { repository.convertToPermanent(it) }
        }
    }
}