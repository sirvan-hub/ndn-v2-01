package com.example.data.remote.dto

import com.example.data.local.entities.RegistrationTransactionEntity
import com.example.data.model.RegistrationSource
import com.example.data.model.RegistrationTransaction
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseRegistrationTransactionDto(
    @SerialName("transaction_id") val transactionId: String,
    @SerialName("parcel_id") val parcelId: String,
    @SerialName("tracking_number") val trackingNumber: String,
    @SerialName("courier_id") val courierId: String,
    @SerialName("hub_id") val hubId: String? = null,
    @SerialName("registration_source") val registrationSource: String = "CUSTOMER_REQUEST",
    @SerialName("timestamp") val timestamp: Long = System.currentTimeMillis(),
    @SerialName("client_generated_id") val clientGeneratedId: String = transactionId
) {
    fun toDomain(): RegistrationTransaction = RegistrationTransaction(
        transactionId = transactionId,
        parcelId = parcelId,
        trackingNumber = trackingNumber,
        courierId = courierId,
        hubId = hubId,
        registrationSource = try {
            RegistrationSource.valueOf(registrationSource)
        } catch (_: Exception) {
            RegistrationSource.CUSTOMER_REQUEST
        },
        timestamp = timestamp,
        clientGeneratedId = clientGeneratedId
    )

    fun toEntity(): RegistrationTransactionEntity = RegistrationTransactionEntity(
        transactionId = transactionId,
        parcelId = parcelId,
        trackingNumber = trackingNumber,
        courierId = courierId,
        hubId = hubId,
        registrationSource = registrationSource,
        timestamp = timestamp,
        clientGeneratedId = clientGeneratedId
    )

    companion object {
        fun fromDomain(tx: RegistrationTransaction): SupabaseRegistrationTransactionDto = SupabaseRegistrationTransactionDto(
            transactionId = tx.transactionId,
            parcelId = tx.parcelId,
            trackingNumber = tx.trackingNumber,
            courierId = tx.courierId,
            hubId = tx.hubId,
            registrationSource = tx.registrationSource.name,
            timestamp = tx.timestamp,
            clientGeneratedId = tx.clientGeneratedId
        )

        fun fromEntity(entity: RegistrationTransactionEntity): SupabaseRegistrationTransactionDto = SupabaseRegistrationTransactionDto(
            transactionId = entity.transactionId,
            parcelId = entity.parcelId,
            trackingNumber = entity.trackingNumber,
            courierId = entity.courierId,
            hubId = entity.hubId,
            registrationSource = entity.registrationSource,
            timestamp = entity.timestamp,
            clientGeneratedId = entity.clientGeneratedId
        )
    }
}
