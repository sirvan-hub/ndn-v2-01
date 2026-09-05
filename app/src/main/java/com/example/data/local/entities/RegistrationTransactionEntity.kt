package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "registration_transactions",
    indices = [
        Index(value = ["transactionId"], unique = true),
        Index(value = ["parcelId"]),
        Index(value = ["courierId"]),
        Index(value = ["trackingNumber"])
    ]
)
data class RegistrationTransactionEntity(
    @PrimaryKey
    val transactionId: String,
    val parcelId: String,
    val trackingNumber: String,
    val courierId: String,
    val hubId: String?,
    val registrationSource: String,
    val timestamp: Long = System.currentTimeMillis(),
    val clientGeneratedId: String = transactionId
)
