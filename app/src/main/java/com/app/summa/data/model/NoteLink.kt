package com.app.summa.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// TABEL BARU: Menyimpan hubungan antar catatan (Many-to-Many)
// Ini memungkinkan fitur "Backlink" (Catatan A merujuk ke Catatan B)
@Entity(
    tableName = "note_links",
    foreignKeys = [
        ForeignKey(
            entity = KnowledgeNote::class,
            parentColumns = ["id"],
            childColumns = ["sourceNoteId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = KnowledgeNote::class,
            parentColumns = ["id"],
            childColumns = ["targetNoteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sourceNoteId"), Index("targetNoteId")]
)
data class NoteLink(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceNoteId: Long, // Catatan ASAL
    val targetNoteId: Long, // Catatan TUJUAN
    val createdAt: Long = System.currentTimeMillis()
)