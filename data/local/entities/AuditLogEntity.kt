package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audit_logs",
    indices = [
        Index(value = ["entityId"]),
        Index(value = ["actorId"]),
        Index(value = ["timestamp"]),
        Index(value = ["transactionId"])
    ]
)
data class AuditLogEntity(
    @PrimaryKey
    val id: String,
    val eventType: String,
    val actorId: String,
    val actorRole: String,
    val entityId: String,
    val oldState: String?,
    val newState: String,
    val transactionId: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val metadataJson: String? = null
)
