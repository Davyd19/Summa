package com.app.summa.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.app.summa.data.model.Identity
import com.app.summa.data.model.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface IdentityDao {
    @Query("SELECT * FROM identities ORDER BY progress DESC")
    fun getAllIdentities(): Flow<List<Identity>>

    @Query("SELECT * FROM identities")
    fun getAllIdentitiesSync(): List<Identity> // Untuk Backup

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(tasks: List<Identity>) // Untuk Restore

    // PENAMBAHAN: Query untuk mengambil satu identitas berdasarkan ID
    @Query("SELECT * FROM identities WHERE id = :id")
    suspend fun getIdentityById(id: Long): Identity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIdentity(identity: Identity): Long

    @Update
    suspend fun updateIdentity(identity: Identity)

    @Delete
    suspend fun deleteIdentity(identity: Identity)
}