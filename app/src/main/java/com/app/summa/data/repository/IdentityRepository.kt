package com.app.summa.data.repository

import com.app.summa.data.local.IdentityDao
import com.app.summa.data.model.Identity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

// INTERFACE BARU
interface IdentityRepository {
    fun getAllIdentities(): Flow<List<Identity>>
    suspend fun insertIdentity(identity: Identity): Long
    suspend fun updateIdentity(identity: Identity)
    suspend fun deleteIdentity(identity: Identity)
    // Fungsi inti untuk memberi "suara"
    suspend fun addVoteToIdentity(identityId: Long, points: Int)
}

// IMPLEMENTASI BARU
class IdentityRepositoryImpl @Inject constructor(
    private val identityDao: IdentityDao
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

    override suspend fun addVoteToIdentity(identityId: Long, points: Int) {
        withContext(Dispatchers.IO) {
            // PERBAIKAN: Mengambil data identitas yang ada
            val identity = identityDao.getIdentityById(identityId)
            if (identity != null) {
                // PERBAIKAN: Menghitung progres baru
                val newProgress = identity.progress + points
                // PERBAIKAN: Menyimpan identitas yang telah diperbarui
                identityDao.updateIdentity(identity.copy(progress = newProgress))
            }
            // TODO: Implementasikan penyimpanan 'note' sebagai Jurnal Mikro
            // Ini mungkin memerlukan injeksi KnowledgeRepository jika Anda ingin
            // menyimpannya sebagai KnowledgeNote baru.
        }
    }
}