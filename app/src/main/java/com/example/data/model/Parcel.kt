package com.example.data.model

data class Parcel(
    val id: String,
    val trackingNumber: String,
    val senderName: String,
    val recipientName: String,
    val recipientPhone: String,
    val recipientPostalCode: String,
    val recipientAddress: String = "",
    val status: ParcelStatus = ParcelStatus.OUT_FOR_DELIVERY,
    val size: ParcelSize = ParcelSize.MEDIUM,
    val assignedHubId: String? = null,
    val assignedHubName: String? = null,
    val assignedCourierId: String? = null,
    val assignedCourierName: String? = null,
    val registrationSource: RegistrationSource = RegistrationSource.CUSTOMER_REQUEST,
    val baseFee: Long = 25000L,
    val isSettled: Boolean = false,
    val handoverOtp: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
