package com.example.data.model

data class User(
    val id: String,
    val username: String = "",
    val passwordHash: String = "",
    val fullName: String,
    val phone: String,
    val nationalId: String = "",
    val email: String = "",
    val postalCode: String = "",
    val address: String = "",
    val role: String = "CUSTOMER", // CUSTOMER, COURIER, HUB_MANAGER, ADMIN
    val approvalStatus: AccountApprovalStatus = AccountApprovalStatus.PENDING,
    val storeName: String = "",
    val guildType: String = "",
    val bankCardNumber: String = "",
    val vehicleType: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
