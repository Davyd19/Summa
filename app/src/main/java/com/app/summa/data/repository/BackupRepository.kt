package com.app.summa.data.repository

import com.app.summa.data.local.SummaDatabase
import com.app.summa.data.model.BackupData
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface BackupRepository {
    suspend fun createBackupJson(): String
    // Mengembalikan true jika sukses, false jika gagal/data korup
    suspend fun restoreBackupFromJson(jsonString: String): Boolean
    
    // Reset Total
    suspend fun clearAllData()
}

class BackupRepositoryImpl @Inject constructor(
    private val database: SummaDatabase
) : BackupRepository {

    private val gson = Gson()

    override suspend fun createBackupJson(): String = withContext(Dispatchers.IO) {
        // 1. Ambil snapshot data saat ini
        val habits = database.habitDao().getAllHabitsSync()
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
            version = 1, // Versi skema backup
            timestamp = System.currentTimeMillis(),
            habits = habits,
            habitLogs = habitLogs,
            tasks = tasks,
            accounts = accounts,
            transactions = transactions,
            identities = identities,
            focusSessions = focusSessions,
            knowledgeNotes = notes,
            noteLinks = links
        )

        // 3. Serialize ke JSON
        return@withContext gson.toJson(backupData)
    }

    override suspend fun restoreBackupFromJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. Validasi JSON Parsing
            if (jsonString.isBlank()) return@withContext false

            val backupData = try {
                gson.fromJson(jsonString, BackupData::class.java)
            } catch (e: JsonSyntaxException) {
                e.printStackTrace()
                return@withContext false
            }

            // 2. Validasi Integritas Data Minimal
            // Minimal harus object valid dan tidak null
            if (backupData == null) return@withContext false

            // 3. Eksekusi Restore dalam Transaksi Database (Atomic)
            // Jika ada error di tengah jalan, DB akan rollback otomatis ke kondisi semula
            database.runInTransaction {
                // A. Bersihkan tabel lama (Nuke)
                database.clearAllTables()

                // B. Masukkan data baru (Restore)
                // Menggunakan safe call (?.) untuk menangani kemungkinan list kosong/null dari JSON lama
                backupData.identities.let { if (it.isNotEmpty()) database.identityDao().insertAll(it) }
                backupData.habits.let { if (it.isNotEmpty()) database.habitDao().insertHabits(it) }
                backupData.habitLogs.let { if (it.isNotEmpty()) database.habitDao().insertHabitLogs(it) }
                backupData.tasks.let { if (it.isNotEmpty()) database.taskDao().insertTasks(it) }
                backupData.accounts.let { if (it.isNotEmpty()) database.accountDao().insertAccounts(it) }
                backupData.transactions.let { if (it.isNotEmpty()) database.accountDao().insertTransactions(it) }
                backupData.focusSessions.let { if (it.isNotEmpty()) database.focusSessionDao().insertSessions(it) }
                backupData.knowledgeNotes.let { if (it.isNotEmpty()) database.knowledgeDao().insertNotes(it) }
                backupData.noteLinks.let { if (it.isNotEmpty()) database.noteLinkDao().insertLinks(it) }
            }

            return@withContext true

        } catch (e: Exception) {
            e.printStackTrace()
            // Jika transaction gagal, Room akan melempar exception
            // Kita tangkap dan kembalikan false agar UI bisa memberi tahu user
            return@withContext false
        }
    }
            return@withContext false
        }
    }

    override suspend fun clearAllData() = withContext(Dispatchers.IO) {
        database.clearAllTables()
    }
}