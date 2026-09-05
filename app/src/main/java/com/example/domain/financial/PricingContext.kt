package com.example.domain.financial

import com.example.data.model.SettlementTariffVersion

/**
 * Minimal Future-Proofing Abstraction for Pricing Execution.
 * Decouples caller identity, organization context, and tariff parameters
 * from the pure calculation logic without implementing B2B features prematurely.
 */
data class PricingContext(
    val customerType: String = "STANDARD",
    val organizationId: String? = null,
    val contractId: String? = null,
    val tariffVersion: SettlementTariffVersion,
    val requestedAt: Long = System.currentTimeMillis()
)

/**
 * Extensible policy boundary for service fee pricing.
 */
interface PricingPolicy {
    fun calculateServiceFee(printedPostalFee: Long, durationHours: Double, context: PricingContext): Long
}

/**
 * Standard authoritative Model A implementation.
 */
class StandardModelAPricingPolicy : PricingPolicy {
    override fun calculateServiceFee(
        printedPostalFee: Long,
        durationHours: Double,
        context: PricingContext
    ): Long {
        return context.tariffVersion.calculateServiceFee(printedPostalFee, durationHours)
    }
}
