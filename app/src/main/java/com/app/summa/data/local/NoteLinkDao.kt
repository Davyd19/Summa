package com.app.summa.data.local

import androidx.room.*
import com.app.summa.data.model.KnowledgeNote
import com.app.summa.data.model.NoteLink
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteLinkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLink(link: NoteLink)

    @Query("SELECT * FROM note_links")
    fun getAllLinksSync(): List<NoteLink> // Untuk Backup

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLinks(tasks: List<NoteLink>) // Untuk Restore

    @Query("DELETE FROM note_links WHERE sourceNoteId = :sourceId AND targetNoteId = :targetId")
    suspend fun deleteLink(sourceId: Long, targetId: Long)

    @Query("DELETE FROM note_links WHERE sourceNoteId = :sourceId")
    suspend fun clearLinksForNote(sourceId: Long)

    // QUERY BARU: Ambil semua link untuk Graph View global
    @Query("SELECT * FROM note_links")
    fun getAllLinks(): Flow<List<NoteLink>>

    // FORWARD LINKS: Catatan apa saja yang DIHUBUNGKAN OLEH catatan ini?
    @Transaction
    @Query("""
        SELECT n.* FROM knowledge_notes n
        INNER JOIN note_links l ON n.id = l.targetNoteId
        WHERE l.sourceNoteId = :noteId
    """)
    fun getForwardLinks(noteId: Long): Flow<List<KnowledgeNote>>

    // BACKLINKS: Catatan apa saja yang MENGHUBUNGKAN KE catatan ini? (Fitur Penting Zettelkasten)
    @Transaction
    @Query("""
        SELECT n.* FROM knowledge_notes n
        INNER JOIN note_links l ON n.id = l.sourceNoteId
        WHERE l.targetNoteId = :noteId
    """)
    fun getBacklinks(noteId: Long): Flow<List<KnowledgeNote>>
}