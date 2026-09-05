package com.example.data.model

import kotlin.math.ceil

/**
 * PUDO-NDN Versioned Tariff & Business Rule Configuration.
 *
 * LAYER 1: SERVICE PRICE CALCULATION
 * Calculated from the postal distribution fee printed on the parcel (PrintedPostalFee).
 * Model A:
 *   - < 12 hours: ServiceFee = 20% × PrintedPostalFee
 *   - <= 24 hours: ServiceFee = 40% × PrintedPostalFee
 *   - > 24 hours: For each additional 24-hour period: +50% × PrintedPostalFee
 *   - Operational delivery lifecycle is limited to 7 days (168 hours).
 *
 * LAYER 2: SERVICE REVENUE DISTRIBUTION
 * Applied AFTER the ServiceFee is calculated:
 *   - 30% -> Courier / Delivery Agent
 *   - 30% -> Local PUDO Hub
 *   - 40% -> PUDO-NDN Network
 */
data class SettlementTariffVersion(
    val id: String,
    val versionCode: Int,
    val versionName: String,
    val modelType: String = "MODEL_A",
    val tier1HoursThreshold: Double = 12.0,
    val tier1RatePercentage: Double = 0.20, // 20%
    val tier2HoursThreshold: Double = 24.0,
    val tier2RatePercentage: Double = 0.40, // 40%
    val additional24hRatePercentage: Double = 0.50, // +50% per additional 24h
    val maxLifecycleHours: Double = 168.0, // 7 days max lifecycle
    val courierSharePercentage: Double = 0.30, // 30%
    val hubSharePercentage: Double = 0.30, // 30%
    val networkSharePercentage: Double = 0.40, // 40%
    val baseFee: Long = 25000L,
    val effectiveFrom: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
) {
    /**
     * Layer 1: Calculate Service Fee from Printed Postal Fee based on duration.
     */
    fun calculateServiceFee(printedPostalFee: Long, durationHours: Double): Long {
        if (printedPostalFee <= 0L) return 0L
        val boundedHours = durationHours.coerceIn(0.0, maxLifecycleHours)
        val rate = when {
            boundedHours < tier1HoursThreshold -> tier1RatePercentage
            boundedHours <= tier2HoursThreshold -> tier2RatePercentage
            else -> {
                val excessHours = boundedHours - tier2HoursThreshold
                val additionalPeriods = ceil(excessHours / 24.0).toInt()
                tier2RatePercentage + (additionalPeriods * additional24hRatePercentage)
            }
        }
        return (printedPostalFee * rate).toLong()
    }

    /**
     * Layer 2: Distribute calculated Service Fee across stakeholders.
     * Note: VAT status is NOT DEFINED. No VAT is calculated or deducted.
     */
    fun calculateDistribution(serviceFee: Long): PudoRevenueDistribution {
        val courierAmount = (serviceFee * courierSharePercentage).toLong()
        val hubAmount = (serviceFee * hubSharePercentage).toLong()
        val networkAmount = serviceFee - courierAmount - hubAmount
        return PudoRevenueDistribution(
            calculatedServiceFee = serviceFee,
            courierSharePercentage = courierSharePercentage,
            hubSharePercentage = hubSharePercentage,
            networkSharePercentage = networkSharePercentage,
            courierShareAmount = courierAmount,
            hubShareAmount = hubAmount,
            networkShareAmount = networkAmount
        )
    }

    /**
     * Full 2-Layer Financial Evaluation
     */
    fun evaluateFinancials(printedPostalFee: Long, durationHours: Double): PudoFinancialEvaluation {
        val serviceFee = calculateServiceFee(printedPostalFee, durationHours)
        val distribution = calculateDistribution(serviceFee)
        return PudoFinancialEvaluation(
            printedPostalFee = printedPostalFee,
            deliveryDurationHours = durationHours,
            tariffVersionId = id,
            tariffVersionName = versionName,
            calculatedServiceFee = serviceFee,
            distribution = distribution
        )
    }
}
