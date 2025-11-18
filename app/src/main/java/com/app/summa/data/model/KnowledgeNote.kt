package com.app.summa.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "knowledge_notes")
data class KnowledgeNote(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "",
    val content: String,
    val tags: String = "", // Comma-separated
    // --- PENAMBAHAN ---
    // Kolom ini adalah dasar untuk Zettelkasten.
    // Akan menyimpan ID catatan lain (misal: "1,5,23")
    val linkedNoteIds: String = "",
    // -------------------
    val isPermanent: Boolean = false, // false = Inbox, true = Pustaka
    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    val createdAt: Long? = null,
    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    val updatedAt: Long? = null
)