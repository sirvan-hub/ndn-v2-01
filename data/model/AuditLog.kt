package com.example.data.model

data class AuditLog(
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
