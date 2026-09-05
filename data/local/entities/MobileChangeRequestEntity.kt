package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "mobile_change_requests",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["requestedPhone"]),
        Index(value = ["status"])
    ]
)
data class MobileChangeRequestEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val userFullName: String,
    val currentPhone: String,
    val requestedPhone: String,
    val nationalId: String,
    val status: String, // PENDING_ADMIN_APPROVAL, APPROVED, REJECTED
    val requestedAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long? = null,
    val reviewedBy: String? = null,
    val reviewNotes: String? = null
)
