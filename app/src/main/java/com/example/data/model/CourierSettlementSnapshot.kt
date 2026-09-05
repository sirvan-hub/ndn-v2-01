package com.example.data.model

/**
 * Immutable Historical Snapshot of Confirmed PUDO Courier/Hub Settlement.
 *
 * FINANCIAL IMMUTABILITY:
 * Retains all original pricing rule versions, postal fees, durations, and calculated shares.
 * Past snapshots must never change when future tariffs or revenue distributions change.
 */
data class CourierSettlementSnapshot(
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
