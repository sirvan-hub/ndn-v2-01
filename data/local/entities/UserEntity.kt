package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["phone"], unique = true),
        Index(value = ["nationalId"]),
        Index(value = ["username"]),
        Index(value = ["role"]),
        Index(value = ["approvalStatus"])
    ]
)
data class UserEntity(
    @PrimaryKey
    val id: String,
    val username: String = "",
    val passwordHash: String = "",
    val fullName: String,
    val phone: String,
    val nationalId: String = "",
    val email: String = "",
    val postalCode: String = "",
    val address: String = "",
    val role: String,
    val approvalStatus: String,
    val storeName: String = "",
    val guildType: String = "",
    val bankCardNumber: String = "",
    val vehicleType: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
