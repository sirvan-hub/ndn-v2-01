package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "courier_settlement_snapshots",
    indices = [
        Index(value = ["courierId"]),
        Index(value = ["hubId"]),
        Index(value = ["periodEndDate"]),
        Index(value = ["isPaid"]),
        Index(value = ["idempotencyKey"], unique = true)
    ]
)
data class CourierSettlementSnapshotEntity(
    @PrimaryKey
    val snapshotId: String,
    val courierId: String,
    val hubId: String? = null,
    val periodStartDate: Long,
    val periodEndDate: Long,
    val settlementPeriod: String = "WEEKLY",
    val totalParcelsTransferred: Int = 0,
    val printedPostalFee: Long = 0L,
    val servicePricingRuleVersion: String = "tariff-model-a-v1.0",
    val deliveryDurationHours: Double = 0.0,
    val calculatedServiceFee: Long = 0L,
    val courierSharePercentage: Double = 0.30,
    val hubSharePercentage: Double = 0.30,
    val networkSharePercentage: Double = 0.40,
    val courierShareAmount: Long = 0L,
    val hubShareAmount: Long = 0L,
    val networkShareAmount: Long = 0L,
    val confirmedAmount: Long = 0L,
    val pendingAmount: Long = 0L,
    val tariffVersionId: String = servicePricingRuleVersion,
    val snapshotCreatedAt: Long = System.currentTimeMillis(),
    val isPaid: Boolean = false,
    val idempotencyKey: String = snapshotId
)
