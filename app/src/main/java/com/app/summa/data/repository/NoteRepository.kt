package com.app.summa.data.repository

import com.app.summa.data.local.NoteDao
import com.app.summa.data.model.Note
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface NoteRepository {
    fun getAllNotes(): Flow<List<Note>>
    suspend fun insertNote(note: Note): Long
    suspend fun updateNote(note: Note)
    suspend fun deleteNote(note: Note)
}

class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao
) : NoteRepository {

    override fun getAllNotes(): Flow<List<Note>> {
        return noteDao.getAllNotes()
    }

    override suspend fun insertNote(note: Note): Long {
        return noteDao.insertNote(note)
    }

    override suspend fun updateNote(note: Note) {
        noteDao.updateNote(note)
    }

    override suspend fun deleteNote(note: Note) {
        noteDao.deleteNote(note)
    }
}