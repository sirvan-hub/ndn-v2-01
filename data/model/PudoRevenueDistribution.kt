package com.example.data.model

/**
 * PUDO-NDN Layer 2 Financial Model: Revenue Distribution breakdown
 * Applied AFTER the PUDO Service Fee has been calculated from the Printed Postal Fee.
 */
data class PudoRevenueDistribution(
    val calculatedServiceFee: Long,
    val courierSharePercentage: Double = 0.30,
    val hubSharePercentage: Double = 0.30,
    val networkSharePercentage: Double = 0.40,
    val courierShareAmount: Long,
    val hubShareAmount: Long,
    val networkShareAmount: Long
)

/**
 * Complete 2-Layer Financial Evaluation for a parcel.
 */
data class PudoFinancialEvaluation(
    val printedPostalFee: Long,
    val deliveryDurationHours: Double,
    val tariffVersionId: String,
    val tariffVersionName: String,
    val calculatedServiceFee: Long,
    val distribution: PudoRevenueDistribution
)
