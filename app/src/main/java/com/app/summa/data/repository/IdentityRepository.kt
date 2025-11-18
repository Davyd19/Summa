package com.app.summa.data.repository

import com.app.summa.data.local.IdentityDao
import com.app.summa.data.model.Identity
import com.app.summa.data.model.KnowledgeNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface IdentityRepository {
    fun getAllIdentities(): Flow<List<Identity>>
    suspend fun insertIdentity(identity: Identity): Long
    suspend fun updateIdentity(identity: Identity)
    suspend fun deleteIdentity(identity: Identity)

    // PERBAIKAN: Menambahkan parameter 'note' agar sesuai dengan ViewModel
    suspend fun addVoteToIdentity(identityId: Long, points: Int, note: String)
}

class IdentityRepositoryImpl @Inject constructor(
    private val identityDao: IdentityDao,
    // PERBAIKAN: Menambahkan KnowledgeRepository agar sesuai dengan RepositoryModule
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
        // TODO: Implement delete logic if needed
    }

    override suspend fun addVoteToIdentity(identityId: Long, points: Int, note: String) {
        withContext(Dispatchers.IO) {
            val identity = identityDao.getIdentityById(identityId)
            if (identity != null) {
                // 1. Update progress identitas
                val newProgress = identity.progress + points
                identityDao.updateIdentity(identity.copy(progress = newProgress))

                // 2. IMPLEMENTASI NYATA: Simpan 'note' sebagai Jurnal Mikro ke Pustaka
                if (note.isNotBlank()) {
                    val currentTime = System.currentTimeMillis()
                    val microJournalNote = KnowledgeNote(
                        title = "Jurnal Identitas: ${identity.name}",
                        content = note,
                        // Otomatis tag agar mudah dicari nanti
                        tags = "#refleksi, #identitas, #${identity.name.lowercase().replace(" ", "_")}",
                        isPermanent = true, // Langsung masuk pustaka (permanen)
                        createdAt = currentTime,
                        updatedAt = currentTime
                    )
                    // Simpan ke database knowledge
                    knowledgeRepository.saveNote(microJournalNote)
                }
            }
        }
    }
}