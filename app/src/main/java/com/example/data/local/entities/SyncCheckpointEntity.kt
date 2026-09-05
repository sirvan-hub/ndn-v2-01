package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_checkpoints")
data class SyncCheckpointEntity(
    @PrimaryKey
    val syncKey: String,
    val lastSyncedTimestamp: Long = 0L,
    val lastSyncedAt: Long = System.currentTimeMillis(),
    val recordsCount: Int = 0,
    val metadata: String = ""
)
