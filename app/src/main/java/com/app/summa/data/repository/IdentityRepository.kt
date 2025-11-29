package com.app.summa.data.repository

import com.app.summa.data.local.IdentityDao
import com.app.summa.data.model.Identity
import com.app.summa.data.model.KnowledgeNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface IdentityRepository {
    fun getAllIdentities(): Flow<List<Identity>>
    suspend fun insertIdentity(identity: Identity): Long
    suspend fun updateIdentity(identity: Identity)
    suspend fun deleteIdentity(identity: Identity)
    suspend fun addVoteToIdentity(identityId: Long, points: Int, note: String)

    // FUNGSI BARU: Mengambil bukti (catatan) untuk identitas spesifik
    fun getIdentityEvidence(identityName: String): Flow<List<KnowledgeNote>>
}

class IdentityRepositoryImpl @Inject constructor(
    private val identityDao: IdentityDao,
    private val knowledgeRepository: KnowledgeRepository
) : IdentityRepository {

    override fun getAllIdentities(): Flow<List<Identity>> {
        return identityDao.getAllIdentities()
    }

    override suspend fun insertIdentity(identity: Identity): Long {
        return identityDao.insertIdentity(identity)
    }

    override suspend fun updateIdentity(identity: Identity) {
        identityDao.updateIdentity(identity)
    }

    override suspend fun deleteIdentity(identity: Identity) {
        // TODO: Implement delete logic
    }

    override suspend fun addVoteToIdentity(identityId: Long, points: Int, note: String) {
        withContext(Dispatchers.IO) {
            val identity = identityDao.getIdentityById(identityId)
            if (identity != null) {
                // 1. Update progress identitas
                val newProgress = identity.progress + points
                identityDao.updateIdentity(identity.copy(progress = newProgress))

                // 2. Simpan 'note' sebagai Jurnal Mikro ke Pustaka
                // Format judul: "Bukti: [Nama Identitas]" agar mudah difilter
                val currentTime = System.currentTimeMillis()
                val microJournalNote = KnowledgeNote(
                    title = "Bukti: ${identity.name}",
                    content = if (note.isNotBlank()) note else "Melakukan aktivitas identitas",
                    // Tagging otomatis: #identitas dan #nama_identitas (lowercase, spasi jadi underscore)
                    tags = "#identitas, #${identity.name.lowercase().replace(" ", "_")}",
                    isPermanent = true,
                    createdAt = currentTime,
                    updatedAt = currentTime
                )
                knowledgeRepository.saveNote(microJournalNote)
            }
        }
    }

    // IMPLEMENTASI BARU: Filter catatan berdasarkan tag identitas
    override fun getIdentityEvidence(identityName: String): Flow<List<KnowledgeNote>> {
        val tagToFind = "#${identityName.lowercase().replace(" ", "_")}"
        // Kita ambil dari permanent notes karena bukti disimpan sebagai permanent
        return knowledgeRepository.getPermanentNotes().map { notes ->
            notes.filter { note ->
                note.tags.contains(tagToFind, ignoreCase = true) ||
                        note.tags.contains("#identitas", ignoreCase = true) && note.title.contains(identityName, ignoreCase = true)
            }.sortedByDescending { it.createdAt } // Urutkan dari yang terbaru
        }
    }
}