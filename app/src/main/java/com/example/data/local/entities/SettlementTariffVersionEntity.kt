package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "settlement_tariffs",
    indices = [
        Index(value = ["isActive"]),
        Index(value = ["versionCode"])
    ]
)
data class SettlementTariffVersionEntity(
    @PrimaryKey
    val id: String,
    val versionCode: Int,
    val versionName: String,
    val modelType: String = "MODEL_A",
    val tier1HoursThreshold: Double = 12.0,
    val tier1RatePercentage: Double = 0.20,
    val tier2HoursThreshold: Double = 24.0,
    val tier2RatePercentage: Double = 0.40,
    val additional24hRatePercentage: Double = 0.50,
    val maxLifecycleHours: Double = 168.0,
    val courierSharePercentage: Double = 0.30,
    val hubSharePercentage: Double = 0.30,
    val networkSharePercentage: Double = 0.40,
    val baseFee: Long = 25000L,
    val effectiveFrom: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
