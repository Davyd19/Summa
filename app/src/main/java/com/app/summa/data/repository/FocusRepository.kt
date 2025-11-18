package com.app.summa.data.repository

import com.app.summa.data.local.FocusSessionDao
import com.app.summa.data.model.FocusSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface FocusRepository {
    suspend fun saveSession(session: FocusSession)
    fun getTotalPaperclips(): Flow<Int>
}

class FocusRepositoryImpl @Inject constructor(
    private val dao: FocusSessionDao
) : FocusRepository {

    override suspend fun saveSession(session: FocusSession) {
        dao.insertFocusSession(session)
    }

    override fun getTotalPaperclips(): Flow<Int> {
        // Mengembalikan 0 jika null (belum ada sesi)
        return dao.getTotalPaperclips().map { it ?: 0 }
    }
}