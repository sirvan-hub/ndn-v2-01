package com.example.data.model

data class RegistrationTransaction(
    val transactionId: String,
    val parcelId: String,
    val trackingNumber: String,
    val courierId: String,
    val hubId: String?,
    val registrationSource: RegistrationSource,
    val timestamp: Long = System.currentTimeMillis(),
    val clientGeneratedId: String = transactionId
)
