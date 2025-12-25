package com.app.summa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.summa.data.model.KnowledgeNote
import com.app.summa.data.model.NoteLink
import com.app.summa.data.repository.KnowledgeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KnowledgeUiState(
    val inboxNotes: List<KnowledgeNote> = emptyList(),
    val permanentNotes: List<KnowledgeNote> = emptyList(),
    val selectedNote: KnowledgeNote? = null,
    val forwardLinks: List<KnowledgeNote> = emptyList(), // Catatan yang dilink OLEH note ini
    val backlinks: List<KnowledgeNote> = emptyList(),    // Catatan yang melink KE note ini
    val allLinks: List<NoteLink> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class KnowledgeViewModel @Inject constructor(
    private val repository: KnowledgeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(KnowledgeUiState(isLoading = true))
    val uiState: StateFlow<KnowledgeUiState> = _uiState.asStateFlow()

    // State terpisah untuk pencarian (agar tidak mengganggu state utama UI)
    private val _searchResults = MutableStateFlow<List<KnowledgeNote>>(emptyList())
    val searchResults: StateFlow<List<KnowledgeNote>> = _searchResults.asStateFlow()

    // State KHUSUS untuk Autocomplete [[...]]
    private val _linkSuggestions = MutableStateFlow<List<KnowledgeNote>>(emptyList())
    val linkSuggestions: StateFlow<List<KnowledgeNote>> = _linkSuggestions.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                repository.getInboxNotes(),
                repository.getPermanentNotes(),
                repository.getAllLinks()
            ) { inbox, permanent, links ->
                Triple(inbox, permanent, links)
            }.collect { (inbox, permanent, links) ->
                _uiState.update {
                    it.copy(
                        inboxNotes = inbox,
                        permanentNotes = permanent,
                        allLinks = links,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun loadNoteDetail(noteId: Long) {
        if (noteId == 0L) {
            _uiState.update { it.copy(selectedNote = null, forwardLinks = emptyList(), backlinks = emptyList()) }
            return
        }

        viewModelScope.launch {
            repository.getNoteById(noteId).collect { note ->
                if (note != null) {
                    // Load links secara paralel
                    launch {
                        repository.getForwardLinks(noteId).collect { forwards ->
                            _uiState.update { it.copy(forwardLinks = forwards) }
                        }
                    }
                    launch {
                        repository.getBacklinks(noteId).collect { backs ->
                            _uiState.update { it.copy(backlinks = backs) }
                        }
                    }
                    _uiState.update { it.copy(selectedNote = note) }
                }
            }
        }
    }

    fun saveNote(title: String, content: String) {
        viewModelScope.launch {
            val currentNote = _uiState.value.selectedNote
            val newNote = currentNote?.copy(
                title = title,
                content = content,
                updatedAt = System.currentTimeMillis()
            ) ?: KnowledgeNote(
                title = title,
                content = content,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            if (currentNote == null) {
                val newId = repository.saveNote(newNote)
                loadNoteDetail(newId) // Reload agar links terupdate
            } else {
                repository.updateNote(newNote)
            }
        }
    }

    fun deleteNote() {
        _uiState.value.selectedNote?.let {
            viewModelScope.launch { repository.deleteNote(it) }
        }
    }

    fun convertToPermanent() {
        _uiState.value.selectedNote?.let {
            viewModelScope.launch { repository.convertToPermanent(it) }
        }
    }

    fun promoteNote(note: KnowledgeNote) {
        viewModelScope.launch { repository.convertToPermanent(note) }
    }

    // --- LOGIKA PENCARIAN LINK ---

    // 1. Pencarian Manual (Dialog)
    fun searchNotesForLinking(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                _searchResults.value = emptyList()
                return@launch
            }
            val allNotes = _uiState.value.inboxNotes + _uiState.value.permanentNotes
            _searchResults.value = allNotes.filter {
                it.title.contains(query, ignoreCase = true) && it.id != _uiState.value.selectedNote?.id
            }
        }
    }

    // 2. Pencarian Autocomplete (Saat ketik [[ )
    fun searchForAutocomplete(query: String) {
        viewModelScope.launch {
            val allNotes = _uiState.value.inboxNotes + _uiState.value.permanentNotes
            // Filter: Judul mengandung query, dan bukan note yang sedang diedit
            _linkSuggestions.value = allNotes.filter {
                it.title.contains(query, ignoreCase = true) && it.id != _uiState.value.selectedNote?.id
            }.take(5) // Limit 5 saran saja agar tidak menuhin layar
        }
    }

    fun clearLinkSuggestions() {
        _linkSuggestions.value = emptyList()
    }

    // Menambah link manual (jika lewat dialog)
    fun addLink(targetNote: KnowledgeNote) {
        val sourceId = _uiState.value.selectedNote?.id ?: return
        viewModelScope.launch {
            repository.addLink(sourceId, targetNote.id)
        }
    }

    fun removeLink(targetNote: KnowledgeNote) {
        val sourceId = _uiState.value.selectedNote?.id ?: return
        viewModelScope.launch {
            repository.removeLink(sourceId, targetNote.id)
        }
    }
}