package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "parcels",
    indices = [
        Index(value = ["trackingNumber"], unique = true),
        Index(value = ["courierId"]),
        Index(value = ["hubId"]),
        Index(value = ["status"])
    ]
)
data class ParcelEntity(
    @PrimaryKey
    val id: String,
    val trackingNumber: String,
    val senderName: String,
    val recipientName: String,
    val recipientPhone: String,
    val recipientPostalCode: String,
    val recipientAddress: String = "",
    val status: String,
    val size: String,
    val hubId: String? = null,
    val hubName: String? = null,
    val courierId: String? = null,
    val courierName: String? = null,
    val registrationSource: String,
    val baseFee: Long = 25000L,
    val isSettled: Boolean = false,
    val handoverOtp: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
