package com.app.summa.data.repository

import com.app.summa.data.local.SummaDatabase
import com.app.summa.data.model.BackupData
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface BackupRepository {
    suspend fun createBackupJson(): String
    suspend fun restoreBackupFromJson(jsonString: String)
}

class BackupRepositoryImpl @Inject constructor(
    private val database: SummaDatabase
) : BackupRepository {

    private val gson = Gson()

    override suspend fun createBackupJson(): String = withContext(Dispatchers.IO) {
        // 1. Ambil semua data dari database
        val habits = database.habitDao().getAllHabitsSync() // Perlu method sync atau first() dari flow
        val habitLogs = database.habitDao().getAllHabitLogsSync()
        val tasks = database.taskDao().getAllTasksSync()
        val accounts = database.accountDao().getAllAccountsSync()
        val transactions = database.accountDao().getAllTransactionsSync()
        val identities = database.identityDao().getAllIdentitiesSync()
        val focusSessions = database.focusSessionDao().getAllSessionsSync()
        val notes = database.knowledgeDao().getAllNotesSync()
        val links = database.noteLinkDao().getAllLinksSync()

        // 2. Bungkus dalam object BackupData
        val backupData = BackupData(
            habits = habits,
            habitLogs = habitLogs, // Masukkan ke field yang benar
            tasks = tasks,
            accounts = accounts,
            transactions = transactions,
            identities = identities,
            focusSessions = focusSessions,
            knowledgeNotes = notes,
            noteLinks = links
        )

        // 3. Convert ke JSON String
        return@withContext gson.toJson(backupData)
    }

    override suspend fun restoreBackupFromJson(jsonString: String) = withContext(Dispatchers.IO) {
        // 1. Parse JSON ke Object
        val backupData = gson.fromJson(jsonString, BackupData::class.java)

        // 2. Jalankan dalam transaksi database agar aman (Atomic)
        database.runInTransaction {
            // A. Hapus data lama (Nuke)
            database.clearAllTables()

            // B. Masukkan data baru (Restore)
            if (backupData.identities.isNotEmpty()) database.identityDao().insertAll(backupData.identities)
            if (backupData.habits.isNotEmpty()) database.habitDao().insertHabits(backupData.habits)
            if (backupData.habitLogs.isNotEmpty()) database.habitDao().insertHabitLogs(backupData.habitLogs)
            if (backupData.tasks.isNotEmpty()) database.taskDao().insertTasks(backupData.tasks)
            if (backupData.accounts.isNotEmpty()) database.accountDao().insertAccounts(backupData.accounts)
            if (backupData.transactions.isNotEmpty()) database.accountDao().insertTransactions(backupData.transactions)
            if (backupData.focusSessions.isNotEmpty()) database.focusSessionDao().insertSessions(backupData.focusSessions)
            if (backupData.knowledgeNotes.isNotEmpty()) database.knowledgeDao().insertNotes(backupData.knowledgeNotes)
            if (backupData.noteLinks.isNotEmpty()) database.noteLinkDao().insertLinks(backupData.noteLinks)
        }
    }
}