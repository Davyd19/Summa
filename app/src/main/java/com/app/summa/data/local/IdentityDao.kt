package com.app.summa.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.app.summa.data.model.Identity
import kotlinx.coroutines.flow.Flow

@Dao
interface IdentityDao {
    @Query("SELECT * FROM identities ORDER BY progress DESC")
    fun getAllIdentities(): Flow<List<Identity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIdentity(identity: Identity): Long

    @Update
    suspend fun updateIdentity(identity: Identity)
}