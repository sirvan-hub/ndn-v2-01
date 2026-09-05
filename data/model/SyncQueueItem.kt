package com.example.data.model

data class SyncQueueItem(
    val id: String,
    val entityType: String,
    val entityId: String,
    val action: String,
    val payloadJson: String = "{}",
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val syncedAt: Long? = null,
    val retryCount: Int = 0
)
