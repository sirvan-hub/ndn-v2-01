package com.example.data.remote.dto

import com.example.data.local.entities.AuditLogEntity
import com.example.data.model.AuditLog
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseAuditLogDto(
    @SerialName("id") val id: String,
    @SerialName("event_type") val eventType: String,
    @SerialName("actor_id") val actorId: String,
    @SerialName("actor_role") val actorRole: String,
    @SerialName("entity_id") val entityId: String,
    @SerialName("old_state") val oldState: String? = null,
    @SerialName("new_state") val newState: String,
    @SerialName("transaction_id") val transactionId: String? = null,
    @SerialName("timestamp") val timestamp: Long = System.currentTimeMillis(),
    @SerialName("metadata_json") val metadataJson: String? = null
) {
    fun toDomain(): AuditLog = AuditLog(
        id = id,
        eventType = eventType,
        actorId = actorId,
        actorRole = actorRole,
        entityId = entityId,
        oldState = oldState,
        newState = newState,
        transactionId = transactionId,
        timestamp = timestamp,
        metadataJson = metadataJson
    )

    fun toEntity(): AuditLogEntity = AuditLogEntity(
        id = id,
        eventType = eventType,
        actorId = actorId,
        actorRole = actorRole,
        entityId = entityId,
        oldState = oldState,
        newState = newState,
        transactionId = transactionId,
        timestamp = timestamp,
        metadataJson = metadataJson
    )

    companion object {
        fun fromDomain(log: AuditLog): SupabaseAuditLogDto = SupabaseAuditLogDto(
            id = log.id,
            eventType = log.eventType,
            actorId = log.actorId,
            actorRole = log.actorRole,
            entityId = log.entityId,
            oldState = log.oldState,
            newState = log.newState,
            transactionId = log.transactionId,
            timestamp = log.timestamp,
            metadataJson = log.metadataJson
        )

        fun fromEntity(entity: AuditLogEntity): SupabaseAuditLogDto = SupabaseAuditLogDto(
            id = entity.id,
            eventType = entity.eventType,
            actorId = entity.actorId,
            actorRole = entity.actorRole,
            entityId = entity.entityId,
            oldState = entity.oldState,
            newState = entity.newState,
            transactionId = entity.transactionId,
            timestamp = entity.timestamp,
            metadataJson = entity.metadataJson
        )
    }
}
