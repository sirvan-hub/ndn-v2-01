package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entities.SyncCheckpointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncCheckpointDao {

    @Query("SELECT lastSyncedTimestamp FROM sync_checkpoints WHERE syncKey = :syncKey LIMIT 1")
    suspend fun getCheckpoint(syncKey: String): Long?

    @Query("SELECT * FROM sync_checkpoints WHERE syncKey = :syncKey LIMIT 1")
    suspend fun getCheckpointEntity(syncKey: String): SyncCheckpointEntity?

    @Query("SELECT * FROM sync_checkpoints")
    fun getAllCheckpoints(): Flow<List<SyncCheckpointEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCheckpoint(checkpoint: SyncCheckpointEntity)

    @Query("DELETE FROM sync_checkpoints WHERE syncKey = :syncKey")
    suspend fun clearCheckpoint(syncKey: String)

    @Query("DELETE FROM sync_checkpoints")
    suspend fun clearAllCheckpoints()
}
