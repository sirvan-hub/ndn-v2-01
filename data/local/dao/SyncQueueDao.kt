package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {

    @Query("SELECT * FROM sync_queue WHERE isSynced = 0 ORDER BY createdAt ASC")
    fun getPendingSyncItems(): Flow<List<SyncQueueEntity>>

    @Query("SELECT * FROM sync_queue WHERE isSynced = 0 ORDER BY createdAt ASC")
    suspend fun getPendingSyncItemsDirect(): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue WHERE isSynced = 0 ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPendingSyncBatch(limit: Int): List<SyncQueueEntity>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE isSynced = 0")
    suspend fun getPendingCount(): Int

    @Query("SELECT * FROM sync_queue ORDER BY createdAt DESC LIMIT 100")
    fun getAllSyncItems(): Flow<List<SyncQueueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncItem(item: SyncQueueEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncItems(items: List<SyncQueueEntity>)

    @Update
    suspend fun updateSyncItem(item: SyncQueueEntity)

    @Query("UPDATE sync_queue SET retryCount = :retryCount WHERE id = :id")
    suspend fun updateRetryCount(id: String, retryCount: Int)

    @Query("""
        UPDATE sync_queue 
        SET retryCount = :retryCount, 
            lastAttemptAt = :lastAttemptAt, 
            lastError = :lastError, 
            failureClassification = :failureClassification 
        WHERE id = :id
    """)
    suspend fun updateFailureDiagnostics(
        id: String,
        retryCount: Int,
        lastAttemptAt: Long,
        lastError: String?,
        failureClassification: String?
    )

    @Query("UPDATE sync_queue SET isSynced = 1, syncedAt = :syncedAt WHERE id = :id")
    suspend fun markAsSynced(id: String, syncedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM sync_queue WHERE isSynced = 1 AND syncedAt < :olderThanTimestamp")
    suspend fun purgeSyncedItems(olderThanTimestamp: Long)
}
