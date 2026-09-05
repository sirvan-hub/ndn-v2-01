package com.example.domain.financial

import com.example.data.model.*
import java.util.UUID

/**
 * PUDO-NDN Domain Financial & Settlement Engine.
 *
 * Enforces strict separation of concerns:
 * LAYER 1: SERVICE PRICE CALCULATION
 *   - Calculated from the postal distribution fee printed on the parcel (PrintedPostalFee).
 *   - Model A:
 *       < 12 hours: ServiceFee = 20% × PrintedPostalFee
 *       <= 24 hours: ServiceFee = 40% × PrintedPostalFee
 *       > 24 hours: +50% × PrintedPostalFee per additional 24-hour block (up to 7 days max lifecycle).
 *
 * LAYER 2: SERVICE REVENUE DISTRIBUTION
 *   - Applied AFTER the ServiceFee is calculated:
 *       30% -> Courier / Delivery Agent
 *       30% -> Local PUDO Hub
 *       40% -> PUDO-NDN Network
 *
 * SETTLEMENT ELIGIBILITY:
 *   - Confirmed settlement eligibility requires a verified custody event
 *     (TRANSFERRED_TO_HUB, STORED_AT_HUB, or DELIVERED_TO_CUSTOMER).
 *   - Initial or pending states (OUT_FOR_DELIVERY, DELIVERY_ATTEMPTED, ELIGIBLE_FOR_HUB,
 *     HUB_SELECTED, HANDOVER_IN_PROGRESS, AWAITING_HUB_CONFIRMATION) are strictly ESTIMATED/PENDING.
 *
 * FINANCIAL IMMUTABILITY:
 *   - Every generated settlement snapshot preserves the complete rate, duration,
 *     percentage, and share breakdown as an immutable record.
 */
object PudoSettlementEngine {

    /**
     * LAYER 1: Calculate Service Fee from Printed Postal Fee based on delivery/holding duration.
     *
     * @param printedPostalFee The base postal distribution fee printed on the parcel (IRR).
     * @param durationHours The elapsed time in hours between parcel handover/pickup and delivery.
     * @param tariff The active versioned business rule/tariff configuration.
     * @return The calculated PUDO Service Fee in IRR.
     */
    fun calculateServiceFee(
        printedPostalFee: Long,
        durationHours: Double,
        tariff: SettlementTariffVersion
    ): Long {
        return tariff.calculateServiceFee(printedPostalFee, durationHours)
    }

    /**
     * LAYER 2: Distribute the calculated Service Fee among PUDO stakeholders.
     *
     * @param serviceFee The PUDO service fee calculated from Layer 1.
     * @param tariff The active versioned tariff specifying distribution percentages.
     * @return PudoRevenueDistribution containing courier, hub, and network share amounts.
     */
    fun distributeRevenue(
        serviceFee: Long,
        tariff: SettlementTariffVersion
    ): PudoRevenueDistribution {
        return tariff.calculateDistribution(serviceFee)
    }

    /**
     * Full 2-Layer Financial Evaluation of a parcel.
     */
    fun evaluateParcelFinancials(
        printedPostalFee: Long,
        durationHours: Double,
        tariff: SettlementTariffVersion
    ): PudoFinancialEvaluation {
        return tariff.evaluateFinancials(printedPostalFee, durationHours)
    }

    /**
     * Custody confirmation validation for settlement eligibility.
     *
     * A parcel that has only been registered or assigned to a hub must NOT generate
     * a confirmed courier/hub settlement. Eligibility begins only after the custody
     * transfer has been confirmed.
     */
    fun isEligibleForConfirmedSettlement(status: ParcelStatus): Boolean {
        return when (status) {
            ParcelStatus.TRANSFERRED_TO_HUB,
            ParcelStatus.STORED_AT_HUB,
            ParcelStatus.DELIVERED_TO_CUSTOMER -> true
            ParcelStatus.OUT_FOR_DELIVERY,
            ParcelStatus.DELIVERY_ATTEMPTED,
            ParcelStatus.ELIGIBLE_FOR_HUB,
            ParcelStatus.HUB_SELECTED,
            ParcelStatus.HANDOVER_IN_PROGRESS,
            ParcelStatus.AWAITING_HUB_CONFIRMATION,
            ParcelStatus.RETURNED_TO_SENDER,
            ParcelStatus.REJECTED -> false
        }
    }

    /**
     * Aggregates courier earnings over a settlement period (e.g. Weekly)
     * separating confirmed payouts from pending/estimated amounts.
     *
     * BUGFIX: each parcel's ServiceFee (Layer 1) MUST be calculated from that parcel's own
     * elapsed duration (createdAt -> updatedAt, i.e. registration to its current/last status
     * change), not a single duration applied uniformly to every parcel in the batch. Model A is
     * explicitly duration-tiered per parcel (see class doc); collapsing the whole courier's
     * parcel list onto one duration value would make every parcel bill at the same tier
     * regardless of how long it actually took, which is financially wrong.
     */
    fun calculateCourierSettlementSummary(
        courierId: String,
        parcels: List<Parcel>,
        tariff: SettlementTariffVersion,
        periodStartDate: Long,
        periodEndDate: Long,
        settlementPeriod: String = "WEEKLY",
        nowMillis: Long = System.currentTimeMillis()
    ): CourierSettlementSnapshot {
        val courierParcels = parcels.filter { it.assignedCourierId == courierId }

        var totalPrintedPostalFee = 0L
        var totalCalculatedServiceFee = 0L
        var confirmedCourierAmount = 0L
        var pendingCourierAmount = 0L
        var confirmedParcelsCount = 0
        var totalDurationHours = 0.0

        for (parcel in courierParcels) {
            // Elapsed time for a settled parcel is registration -> last recorded status change
            // (updatedAt is advanced on every status transition, including the terminal one).
            // For a parcel still in flight, elapsed time is registration -> now.
            val referenceEndMillis = if (isEligibleForConfirmedSettlement(parcel.status)) {
                parcel.updatedAt
            } else {
                nowMillis
            }
            val parcelDurationHours = ((referenceEndMillis - parcel.createdAt).coerceAtLeast(0L)) / 3_600_000.0

            val evaluation = evaluateParcelFinancials(parcel.baseFee, parcelDurationHours, tariff)
            val courierShare = evaluation.distribution.courierShareAmount

            totalPrintedPostalFee += parcel.baseFee
            totalCalculatedServiceFee += evaluation.calculatedServiceFee
            totalDurationHours += parcelDurationHours

            if (isEligibleForConfirmedSettlement(parcel.status)) {
                confirmedCourierAmount += courierShare
                confirmedParcelsCount++
            } else {
                pendingCourierAmount += courierShare
            }
        }

        val averageDurationHours = if (courierParcels.isNotEmpty()) totalDurationHours / courierParcels.size else 0.0

        val snapshotId = "snap-courier-${courierId}-${periodStartDate}-${periodEndDate}"
        val distribution = tariff.calculateDistribution(totalCalculatedServiceFee)

        return CourierSettlementSnapshot(
            snapshotId = snapshotId,
            courierId = courierId,
            hubId = null,
            periodStartDate = periodStartDate,
            periodEndDate = periodEndDate,
            settlementPeriod = settlementPeriod,
            totalParcelsTransferred = confirmedParcelsCount,
            printedPostalFee = totalPrintedPostalFee,
            servicePricingRuleVersion = tariff.id,
            // Representative (average) duration across the batch, for reporting only.
            // Each parcel's own share was computed with its own actual duration above.
            deliveryDurationHours = averageDurationHours,
            calculatedServiceFee = totalCalculatedServiceFee,
            courierSharePercentage = tariff.courierSharePercentage,
            hubSharePercentage = tariff.hubSharePercentage,
            networkSharePercentage = tariff.networkSharePercentage,
            courierShareAmount = distribution.courierShareAmount,
            hubShareAmount = distribution.hubShareAmount,
            networkShareAmount = distribution.networkShareAmount,
            confirmedAmount = confirmedCourierAmount,
            pendingAmount = pendingCourierAmount,
            tariffVersionId = tariff.id,
            snapshotCreatedAt = System.currentTimeMillis(),
            isPaid = false,
            idempotencyKey = snapshotId
        )
    }
}
