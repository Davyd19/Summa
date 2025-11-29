package com.app.summa.data.repository

import com.app.summa.data.local.KnowledgeDao
import com.app.summa.data.local.NoteLinkDao
import com.app.summa.data.model.KnowledgeNote
import com.app.summa.data.model.NoteLink
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

    // FITUR BARU: Linking & Graph
    fun getAllLinks(): Flow<List<NoteLink>> // FUNGSI BARU
    fun getForwardLinks(noteId: Long): Flow<List<KnowledgeNote>>
    fun getBacklinks(noteId: Long): Flow<List<KnowledgeNote>>
    suspend fun addLink(sourceId: Long, targetId: Long)
    suspend fun removeLink(sourceId: Long, targetId: Long)
}

class KnowledgeRepositoryImpl @Inject constructor(
    private val dao: KnowledgeDao,
    private val linkDao: NoteLinkDao
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

    // IMPLEMENTASI LINKING
    override fun getAllLinks(): Flow<List<NoteLink>> {
        return linkDao.getAllLinks()
    }

    override fun getForwardLinks(noteId: Long): Flow<List<KnowledgeNote>> {
        return linkDao.getForwardLinks(noteId)
    }

    override fun getBacklinks(noteId: Long): Flow<List<KnowledgeNote>> {
        return linkDao.getBacklinks(noteId)
    }

    override suspend fun addLink(sourceId: Long, targetId: Long) {
        linkDao.insertLink(NoteLink(sourceNoteId = sourceId, targetNoteId = targetId))
    }

    override suspend fun removeLink(sourceId: Long, targetId: Long) {
        linkDao.deleteLink(sourceId, targetId)
    }
}