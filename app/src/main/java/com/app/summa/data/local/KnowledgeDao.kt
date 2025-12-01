package com.app.summa.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.app.summa.data.model.KnowledgeNote
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgeDao {
    // Query untuk "Inbox" (Catatan Cepat)
    @Query("SELECT * FROM knowledge_notes WHERE isPermanent = 0 ORDER BY updatedAt DESC")
    fun getInboxNotes(): Flow<List<KnowledgeNote>>

    @Query("SELECT * FROM knowledge_notes")
    fun getAllNotesSync(): List<KnowledgeNote> // Untuk Backup

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertNotes(tasks: List<KnowledgeNote>) // Untuk Restore

    // Query untuk "Pustaka" (Catatan Permanen)
    @Query("SELECT * FROM knowledge_notes WHERE isPermanent = 1 ORDER BY updatedAt DESC")
    fun getPermanentNotes(): Flow<List<KnowledgeNote>>

    // Query untuk mengambil satu catatan
    @Query("SELECT * FROM knowledge_notes WHERE id = :id")
    fun getNoteById(id: Long): Flow<KnowledgeNote?> // Ubah ke Flow

    // QUERY BARU: Mencari catatan berdasarkan judul persis (Case Insensitive untuk kenyamanan)
    @Query("SELECT * FROM knowledge_notes WHERE title LIKE :title LIMIT 1")
    suspend fun getNoteByTitle(title: String): KnowledgeNote?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: KnowledgeNote): Long

    @Update
    suspend fun updateNote(note: KnowledgeNote)

    @Delete
    suspend fun deleteNote(note: KnowledgeNote)
}