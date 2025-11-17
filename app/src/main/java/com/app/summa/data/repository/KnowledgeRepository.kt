package com.app.summa.data.repository

import com.app.summa.data.local.KnowledgeDao
import com.app.summa.data.model.KnowledgeNote
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface KnowledgeRepository {
    fun getInboxNotes(): Flow<List<KnowledgeNote>>
    fun getPermanentNotes(): Flow<List<KnowledgeNote>>
    fun getNoteById(id: Long): Flow<KnowledgeNote?>
    suspend fun saveNote(note: KnowledgeNote): Long
    suspend fun updateNote(note: KnowledgeNote)
    suspend fun deleteNote(note: KnowledgeNote)
    suspend fun convertToPermanent(note: KnowledgeNote)
}

class KnowledgeRepositoryImpl @Inject constructor(
    private val dao: KnowledgeDao
) : KnowledgeRepository {

    override fun getInboxNotes(): Flow<List<KnowledgeNote>> {
        return dao.getInboxNotes()
    }

    override fun getPermanentNotes(): Flow<List<KnowledgeNote>> {
        return dao.getPermanentNotes()
    }

    override fun getNoteById(id: Long): Flow<KnowledgeNote?> {
        return dao.getNoteById(id)
    }

    override suspend fun saveNote(note: KnowledgeNote): Long {
        return dao.insertNote(note)
    }

    override suspend fun updateNote(note: KnowledgeNote) {
        dao.updateNote(note)
    }

    override suspend fun deleteNote(note: KnowledgeNote) {
        dao.deleteNote(note)
    }

    override suspend fun convertToPermanent(note: KnowledgeNote) {
        dao.updateNote(note.copy(isPermanent = true, updatedAt = System.currentTimeMillis()))
    }
}