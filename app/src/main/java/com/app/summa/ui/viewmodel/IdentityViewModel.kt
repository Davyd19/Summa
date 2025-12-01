package com.app.summa.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.summa.data.model.Identity
import com.app.summa.data.model.KnowledgeNote
import com.app.summa.data.repository.IdentityRepository
import com.app.summa.data.repository.KnowledgeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IdentityUiState(
    val identities: List<Identity> = emptyList(),
    // Log umum (semua aktivitas #identitas) untuk halaman depan
    val recentActivityLogs: List<KnowledgeNote> = emptyList(),
    val totalLevel: Int = 0,

    // STATE BARU: Detail Identitas Terpilih
    val selectedIdentity: Identity? = null,
    val selectedIdentityLogs: List<KnowledgeNote> = emptyList(),
    val xpToNextLevel: Int = 0,

    val isLoading: Boolean = true
)

@HiltViewModel
class IdentityViewModel @Inject constructor(
    private val identityRepository: IdentityRepository,
    private val knowledgeRepository: KnowledgeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(IdentityUiState())
    val uiState: StateFlow<IdentityUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                identityRepository.getAllIdentities(),
                knowledgeRepository.getPermanentNotes()
            ) { identities, notes ->
                // 1. Hitung Total Level
                val totalXp = identities.sumOf { it.progress }
                val totalLevel = totalXp / 100

                // 2. Filter Global Activity Log
                val logs = notes.filter {
                    it.tags.contains("#identitas", ignoreCase = true)
                }.sortedByDescending { it.createdAt }
                    .take(20)

                // 3. Update Selected Identity jika ada (Real-time update saat detail dibuka)
                val currentSelected = _uiState.value.selectedIdentity
                val updatedSelected = if (currentSelected != null) {
                    identities.find { it.id == currentSelected.id }
                } else null

                // Update logs untuk selected identity juga
                val updatedSelectedLogs = if (updatedSelected != null) {
                    filterLogsForIdentity(notes, updatedSelected.name)
                } else _uiState.value.selectedIdentityLogs

                _uiState.value.copy(
                    identities = identities,
                    recentActivityLogs = logs,
                    totalLevel = totalLevel,
                    selectedIdentity = updatedSelected,
                    selectedIdentityLogs = updatedSelectedLogs,
                    xpToNextLevel = if(updatedSelected != null) 100 - (updatedSelected.progress % 100) else 0,
                    isLoading = false
                )
            }.catch { e ->
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false) }
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun selectIdentity(identity: Identity) {
        viewModelScope.launch {
            // Ambil semua notes dulu (atau optimalkan dengan query DB spesifik nanti)
            val allNotes = knowledgeRepository.getPermanentNotes().first()
            val logs = filterLogsForIdentity(allNotes, identity.name)

            _uiState.update {
                it.copy(
                    selectedIdentity = identity,
                    selectedIdentityLogs = logs,
                    xpToNextLevel = 100 - (identity.progress % 100)
                )
            }
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedIdentity = null, selectedIdentityLogs = emptyList()) }
    }

    private fun filterLogsForIdentity(allNotes: List<KnowledgeNote>, identityName: String): List<KnowledgeNote> {
        val tagToFind = identityName.lowercase().replace(" ", "_")
        return allNotes.filter { note ->
            note.tags.contains(tagToFind, ignoreCase = true) ||
                    (note.tags.contains("#identitas", ignoreCase = true) && note.content.contains(identityName, ignoreCase = true)) ||
                    note.title.contains(identityName, ignoreCase = true)
        }.sortedByDescending { it.createdAt }
    }

    fun addIdentity(name: String, description: String) {
        viewModelScope.launch {
            val newIdentity = Identity(
                name = name,
                description = description,
                createdAt = System.currentTimeMillis(),
                progress = 0
            )
            identityRepository.insertIdentity(newIdentity)
        }
    }
}