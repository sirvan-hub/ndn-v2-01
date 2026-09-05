package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entities.AuditLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE entityId = :entityId ORDER BY timestamp DESC")
    fun getLogsByEntityId(entityId: String): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE actorId = :actorId ORDER BY timestamp DESC")
    fun getLogsByActorId(actorId: String): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE id = :id LIMIT 1")
    suspend fun getLogByIdDirect(id: String): AuditLogEntity?

    @Query("SELECT * FROM audit_logs WHERE transactionId = :transactionId LIMIT 1")
    suspend fun getLogByTransactionId(transactionId: String): AuditLogEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLog(log: AuditLogEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLogs(logs: List<AuditLogEntity>)
}
