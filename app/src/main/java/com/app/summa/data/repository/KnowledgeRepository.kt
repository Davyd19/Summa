package com.app.summa.data.repository

import com.app.summa.data.local.KnowledgeDao
import com.app.summa.data.local.NoteLinkDao
import com.app.summa.data.model.KnowledgeNote
import com.app.summa.data.model.NoteLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
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
    fun getAllLinks(): Flow<List<NoteLink>>
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
        return withContext(Dispatchers.IO) {
            val id = dao.insertNote(note)
            // JALANKAN SMART PARSING & SYNC SETELAH MENYIMPAN
            parseAndSyncLinks(id, note.content)
            id
        }
    }

    override suspend fun updateNote(note: KnowledgeNote) {
        withContext(Dispatchers.IO) {
            dao.updateNote(note)
            // JALANKAN SMART PARSING & SYNC SETELAH UPDATE
            parseAndSyncLinks(note.id, note.content)
        }
    }

    // --- LOGIKA SMART PARSING & SYNC (DIPERBAIKI) ---
    // Mencari pola [[Judul]], membersihkan link lama, dan membuat link baru
    private suspend fun parseAndSyncLinks(sourceId: Long, content: String) {
        // 1. Ekstrak semua judul target dari konten saat ini
        val regex = Regex("\\[\\[(.*?)\\]\\]")
        val foundTitles = regex.findAll(content).map { it.groupValues[1].trim() }.toSet()

        // 2. Hapus semua link keluar (outgoing) lama dari catatan ini
        // Ini penting agar jika kita menghapus [[Link]] dari teks, link di DB juga hilang.
        // "Text is Truth" - Database hanya refleksi dari teks.
        linkDao.clearLinksForNote(sourceId)

        // 3. Masukkan kembali link berdasarkan temuan teks terbaru
        foundTitles.forEach { targetTitle ->
            if (targetTitle.isNotBlank()) {
                // Cari catatan target berdasarkan judul
                val targetNote = dao.getNoteByTitle(targetTitle)

                // Jika ditemukan dan bukan link ke diri sendiri
                if (targetNote != null && targetNote.id != sourceId) {
                    linkDao.insertLink(
                        NoteLink(
                            sourceNoteId = sourceId,
                            targetNoteId = targetNote.id
                        )
                    )
                }
            }
        }
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