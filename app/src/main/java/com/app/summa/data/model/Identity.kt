package com.app.summa.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "identities") // Pastikan nama tabel ini sesuai dengan query di DAO Anda
data class Identity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val description: String? = null
)