package com.app.summa.data.repository

import com.app.summa.data.local.IdentityDao
import com.app.summa.data.model.Identity
import com.app.summa.data.model.KnowledgeNote
import com.app.summa.data.model.LevelUpEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface IdentityRepository {
    fun getAllIdentities(): Flow<List<Identity>>
    suspend fun insertIdentity(identity: Identity): Long
    suspend fun updateIdentity(identity: Identity)
    suspend fun deleteIdentity(identity: Identity)

    // UPDATE: Kembalikan Boolean, tapi logic utamanya sekarang emit ke flow di bawah
    suspend fun addVoteToIdentity(identityId: Long, points: Int, note: String): Boolean

    fun getIdentityEvidence(identityName: String): Flow<List<KnowledgeNote>>

    // BARU: Flow global untuk mendengarkan event Level Up
    val levelUpEvents: SharedFlow<LevelUpEvent>
}

class IdentityRepositoryImpl @Inject constructor(
    private val identityDao: IdentityDao,
    private val knowledgeRepository: KnowledgeRepository
) : IdentityRepository {

    // SharedFlow dengan buffer agar event tidak hilang jika UI belum siap menerima sesaat
    private val _levelUpEvents = MutableSharedFlow<LevelUpEvent>(extraBufferCapacity = 1)
    override val levelUpEvents = _levelUpEvents.asSharedFlow()

    override fun getAllIdentities(): Flow<List<Identity>> = identityDao.getAllIdentities()
    override suspend fun insertIdentity(identity: Identity): Long = identityDao.insertIdentity(identity)
    override suspend fun updateIdentity(identity: Identity) = identityDao.updateIdentity(identity)
    override suspend fun deleteIdentity(identity: Identity) = identityDao.deleteIdentity(identity)

    override suspend fun addVoteToIdentity(identityId: Long, points: Int, note: String): Boolean {
        return withContext(Dispatchers.IO) {
            val identity = identityDao.getIdentityById(identityId) ?: return@withContext false

            val oldLevel = identity.progress / 100
            val newProgress = identity.progress + points
            val newLevel = newProgress / 100

            // 1. Update DB
            identityDao.updateIdentity(identity.copy(progress = newProgress))

            // 2. Catat Bukti (Micro Journal)
            val currentTime = System.currentTimeMillis()
            val microJournalNote = KnowledgeNote(
                title = "Bukti: ${identity.name}",
                content = if (note.isNotBlank()) note else "Melakukan aktivitas identitas",
                tags = "#identitas, #${identity.name.lowercase().replace(" ", "_")}",
                isPermanent = true,
                createdAt = currentTime,
                updatedAt = currentTime
            )
            knowledgeRepository.saveNote(microJournalNote)

            // 3. CEK LEVEL UP & EMIT EVENT
            if (newLevel > oldLevel) {
                _levelUpEvents.emit(
                    LevelUpEvent(
                        identityName = identity.name,
                        newLevel = newLevel,
                        previousLevel = oldLevel
                    )
                )
                return@withContext true
            }

            return@withContext false
        }
    }

    override fun getIdentityEvidence(identityName: String): Flow<List<KnowledgeNote>> {
        val tagToFind = "#${identityName.lowercase().replace(" ", "_")}"
        return knowledgeRepository.getPermanentNotes().map { notes ->
            notes.filter { note ->
                note.tags.contains(tagToFind, ignoreCase = true) ||
                        (note.tags.contains("#identitas", ignoreCase = true) && note.title.contains(identityName, ignoreCase = true))
            }.sortedByDescending { it.createdAt }
        }
    }
}