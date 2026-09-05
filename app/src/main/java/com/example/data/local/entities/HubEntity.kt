package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hubs")
data class HubEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: String = "store",
    val typeName: String = "مرکز توزیع محلی",
    val managerName: String,
    val phone: String,
    val licenseNumber: String = "",
    val address: String = "تهران",
    val rating: Float = 4.8f,
    val reviewCount: Int = 120,
    val workingHours: String = "۰۸:۰۰ - ۲۲:۰۰",
    val isOpen: Boolean = true,
    val currentPackagesCount: Int = 0,
    val maxCapacity: Int = 50,
    val lat: Double = 35.7924,
    val lng: Double = 51.3789,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
