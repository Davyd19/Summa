package com.app.summa.data.model

import com.google.gson.annotations.SerializedName

/**
 * Representasi snapshot dari seluruh database Summa.
 * Digunakan untuk export ke JSON dan import kembali.
 */
data class BackupData(
    @SerializedName("version") val version: Int = 1,
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis(),

    @SerializedName("habits") val habits: List<Habit>,
    @SerializedName("habit_logs") val habitLogs: List<HabitLog>,
    @SerializedName("tasks") val tasks: List<Task>,
    @SerializedName("accounts") val accounts: List<Account>,
    @SerializedName("transactions") val transactions: List<Transaction>,
    @SerializedName("identities") val identities: List<Identity>,
    @SerializedName("focus_sessions") val focusSessions: List<FocusSession>,
    @SerializedName("knowledge_notes") val knowledgeNotes: List<KnowledgeNote>,
    @SerializedName("note_links") val noteLinks: List<NoteLink>
)