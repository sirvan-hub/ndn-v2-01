package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sync_queue",
    indices = [
        Index(value = ["isSynced"]),
        Index(value = ["createdAt"]),
        Index(value = ["entityType", "entityId"])
    ]
)
data class SyncQueueEntity(
    @PrimaryKey
    val id: String,
    val entityType: String,
    val entityId: String,
    val action: String,
    val payloadJson: String = "{}",
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val syncedAt: Long? = null,
    val retryCount: Int = 0,
    val lastAttemptAt: Long? = null,
    val lastError: String? = null,
    val failureClassification: String? = null
)
