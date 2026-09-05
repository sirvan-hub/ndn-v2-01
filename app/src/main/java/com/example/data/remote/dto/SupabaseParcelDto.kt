package com.example.data.remote.dto

import com.example.data.local.entities.ParcelEntity
import com.example.data.model.Parcel
import com.example.data.model.ParcelSize
import com.example.data.model.ParcelStatus
import com.example.data.model.RegistrationSource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseParcelDto(
    @SerialName("id") val id: String,
    @SerialName("tracking_number") val trackingNumber: String,
    @SerialName("sender_name") val senderName: String,
    @SerialName("recipient_name") val recipientName: String,
    @SerialName("recipient_phone") val recipientPhone: String,
    @SerialName("recipient_postal_code") val recipientPostalCode: String,
    @SerialName("recipient_address") val recipientAddress: String = "",
    @SerialName("status") val status: String,
    @SerialName("size") val size: String = "MEDIUM",
    @SerialName("hub_id") val hubId: String? = null,
    @SerialName("hub_name") val hubName: String? = null,
    @SerialName("courier_id") val courierId: String? = null,
    @SerialName("courier_name") val courierName: String? = null,
    @SerialName("registration_source") val registrationSource: String = "CUSTOMER_REQUEST",
    @SerialName("base_fee") val baseFee: Long = 25000L,
    @SerialName("is_settled") val isSettled: Boolean = false,
    @SerialName("handover_otp") val handoverOtp: String? = null,
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("updated_at") val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Parcel = Parcel(
        id = id,
        trackingNumber = trackingNumber,
        senderName = senderName,
        recipientName = recipientName,
        recipientPhone = recipientPhone,
        recipientPostalCode = recipientPostalCode,
        recipientAddress = recipientAddress,
        status = try {
            ParcelStatus.valueOf(status)
        } catch (_: Exception) {
            ParcelStatus.OUT_FOR_DELIVERY
        },
        size = try {
            ParcelSize.valueOf(size)
        } catch (_: Exception) {
            ParcelSize.MEDIUM
        },
        assignedHubId = hubId,
        assignedHubName = hubName,
        assignedCourierId = courierId,
        assignedCourierName = courierName,
        registrationSource = try {
            RegistrationSource.valueOf(registrationSource)
        } catch (_: Exception) {
            RegistrationSource.CUSTOMER_REQUEST
        },
        baseFee = baseFee,
        isSettled = isSettled,
        handoverOtp = handoverOtp,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun toEntity(): ParcelEntity = ParcelEntity(
        id = id,
        trackingNumber = trackingNumber,
        senderName = senderName,
        recipientName = recipientName,
        recipientPhone = recipientPhone,
        recipientPostalCode = recipientPostalCode,
        recipientAddress = recipientAddress,
        status = status,
        size = size,
        hubId = hubId,
        hubName = hubName,
        courierId = courierId,
        courierName = courierName,
        registrationSource = registrationSource,
        baseFee = baseFee,
        isSettled = isSettled,
        handoverOtp = handoverOtp,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(parcel: Parcel): SupabaseParcelDto = SupabaseParcelDto(
            id = parcel.id,
            trackingNumber = parcel.trackingNumber,
            senderName = parcel.senderName,
            recipientName = parcel.recipientName,
            recipientPhone = parcel.recipientPhone,
            recipientPostalCode = parcel.recipientPostalCode,
            recipientAddress = parcel.recipientAddress,
            status = parcel.status.name,
            size = parcel.size.name,
            hubId = parcel.assignedHubId,
            hubName = parcel.assignedHubName,
            courierId = parcel.assignedCourierId,
            courierName = parcel.assignedCourierName,
            registrationSource = parcel.registrationSource.name,
            baseFee = parcel.baseFee,
            isSettled = parcel.isSettled,
            handoverOtp = parcel.handoverOtp,
            createdAt = parcel.createdAt,
            updatedAt = parcel.updatedAt
        )

        fun fromEntity(entity: ParcelEntity): SupabaseParcelDto = SupabaseParcelDto(
            id = entity.id,
            trackingNumber = entity.trackingNumber,
            senderName = entity.senderName,
            recipientName = entity.recipientName,
            recipientPhone = entity.recipientPhone,
            recipientPostalCode = entity.recipientPostalCode,
            recipientAddress = entity.recipientAddress,
            status = entity.status,
            size = entity.size,
            hubId = entity.hubId,
            hubName = entity.hubName,
            courierId = entity.courierId,
            courierName = entity.courierName,
            registrationSource = entity.registrationSource,
            baseFee = entity.baseFee,
            isSettled = entity.isSettled,
            handoverOtp = entity.handoverOtp,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
}
