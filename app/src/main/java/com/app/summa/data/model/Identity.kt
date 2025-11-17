package com.app.summa.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "identities")
data class Identity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val progress: Int = 0,
    val relatedHabitIds: String = "", // Comma-separated IDs
    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    val createdAt: Long? = null
)