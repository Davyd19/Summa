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
    // Log umum (semua aktivitas #identitas)
    val recentActivityLogs: List<KnowledgeNote> = emptyList(),
    val totalLevel: Int = 0,
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

                IdentityUiState(
                    identities = identities,
                    recentActivityLogs = logs,
                    totalLevel = totalLevel,
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

    // FUNGSI BARU: Tambah Identitas
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