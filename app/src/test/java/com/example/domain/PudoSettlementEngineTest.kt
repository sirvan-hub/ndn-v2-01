package com.example.domain

import com.example.data.model.*
import com.example.domain.financial.PricingContext
import com.example.domain.financial.PudoSettlementEngine
import com.example.domain.financial.StandardModelAPricingPolicy
import com.example.domain.hub.BasicHubRecommendationPolicy
import com.example.domain.hub.HubRecommendationRequest
import com.example.model.HubItem
import com.example.camera.ScanContext
import org.junit.Assert.*
import org.junit.Test

class PudoSettlementEngineTest {

    private val standardTariffModelA = SettlementTariffVersion(
        id = "tariff-model-a-v1.0",
        versionCode = 1,
        versionName = "تعرفه مصوب PUDO-NDN مدل A",
        modelType = "MODEL_A",
        tier1HoursThreshold = 12.0,
        tier1RatePercentage = 0.20,
        tier2HoursThreshold = 24.0,
        tier2RatePercentage = 0.40,
        additional24hRatePercentage = 0.50,
        maxLifecycleHours = 168.0, // 7 days
        courierSharePercentage = 0.30,
        hubSharePercentage = 0.30,
        networkSharePercentage = 0.40,
        baseFee = 25000L
    )

    // =========================================================================
    // LAYER 1: NUMERICAL BOUNDARY VERIFICATION (Printed Postal Fee = 100,000 IRR)
    // =========================================================================

    private val basePrintedPostalFee = 100000L

    @Test
    fun testBoundary_0h() {
        // 0h: Tier 1 (< 12h) -> 20% -> 20,000 IRR
        val fee = PudoSettlementEngine.calculateServiceFee(basePrintedPostalFee, 0.0, standardTariffModelA)
        assertEquals(20000L, fee)
    }

    @Test
    fun testBoundary_1h() {
        // 1h: Tier 1 (< 12h) -> 20% -> 20,000 IRR
        val fee = PudoSettlementEngine.calculateServiceFee(basePrintedPostalFee, 1.0, standardTariffModelA)
        assertEquals(20000L, fee)
    }

    @Test
    fun testBoundary_11_99h() {
        // 11.99h: Tier 1 (< 12h) -> 20% -> 20,000 IRR
        val fee = PudoSettlementEngine.calculateServiceFee(basePrintedPostalFee, 11.99, standardTariffModelA)
        assertEquals(20000L, fee)
    }

    @Test
    fun testBoundary_12h() {
        // 12h: Tier 2 (<= 24h) -> 40% -> 40,000 IRR
        val fee = PudoSettlementEngine.calculateServiceFee(basePrintedPostalFee, 12.0, standardTariffModelA)
        assertEquals(40000L, fee)
    }

    @Test
    fun testBoundary_12_01h() {
        // 12.01h: Tier 2 (<= 24h) -> 40% -> 40,000 IRR
        val fee = PudoSettlementEngine.calculateServiceFee(basePrintedPostalFee, 12.01, standardTariffModelA)
        assertEquals(40000L, fee)
    }

    @Test
    fun testBoundary_18h_Canonical() {
        // 18h: Canonical example -> 40% -> 40,000 IRR
        val fee = PudoSettlementEngine.calculateServiceFee(basePrintedPostalFee, 18.0, standardTariffModelA)
        assertEquals(40000L, fee)
    }

    @Test
    fun testBoundary_23_99h() {
        // 23.99h: Tier 2 (<= 24h) -> 40% -> 40,000 IRR
        val fee = PudoSettlementEngine.calculateServiceFee(basePrintedPostalFee, 23.99, standardTariffModelA)
        assertEquals(40000L, fee)
    }

    @Test
    fun testBoundary_24h() {
        // 24h: Tier 2 (<= 24h) -> 40% -> 40,000 IRR
        val fee = PudoSettlementEngine.calculateServiceFee(basePrintedPostalFee, 24.0, standardTariffModelA)
        assertEquals(40000L, fee)
    }

    @Test
    fun testBoundary_24_01h() {
        // 24.01h: > 24h (1 additional 24h period) -> 40% + 50% = 90% -> 90,000 IRR
        val fee = PudoSettlementEngine.calculateServiceFee(basePrintedPostalFee, 24.01, standardTariffModelA)
        assertEquals(90000L, fee)
    }

    @Test
    fun testBoundary_25h() {
        // 25h: 1 additional period -> 90% -> 90,000 IRR
        val fee = PudoSettlementEngine.calculateServiceFee(basePrintedPostalFee, 25.0, standardTariffModelA)
        assertEquals(90000L, fee)
    }

    @Test
    fun testBoundary_30h() {
        // 30h: 1 additional period -> 90% -> 90,000 IRR
        val fee = PudoSettlementEngine.calculateServiceFee(basePrintedPostalFee, 30.0, standardTariffModelA)
        assertEquals(90000L, fee)
    }

    @Test
    fun testBoundary_36h() {
        // 36h: 1 additional period -> 90% -> 90,000 IRR
        val fee = PudoSettlementEngine.calculateServiceFee(basePrintedPostalFee, 36.0, standardTariffModelA)
        assertEquals(90000L, fee)
    }

    @Test
    fun testBoundary_47_99h() {
        // 47.99h: 1 additional period -> 90% -> 90,000 IRR
        val fee = PudoSettlementEngine.calculateServiceFee(basePrintedPostalFee, 47.99, standardTariffModelA)
        assertEquals(90000L, fee)
    }

    @Test
    fun testBoundary_48h() {
        // 48h: 1 additional period (excess 24h / 24 = 1.0) -> 90% -> 90,000 IRR
        val fee = PudoSettlementEngine.calculateServiceFee(basePrintedPostalFee, 48.0, standardTariffModelA)
        assertEquals(90000L, fee)
    }

    @Test
    fun testBoundary_48_01h() {
        // 48.01h: 2 additional periods (excess 24.01h / 24 ceil = 2) -> 40% + 100% = 140% -> 140,000 IRR
        val fee = PudoSettlementEngine.calculateServiceFee(basePrintedPostalFee, 48.01, standardTariffModelA)
        assertEquals(140000L, fee)
    }

    @Test
    fun testBoundary_52h() {
        // 52h: 2 additional periods -> 140% -> 140,000 IRR
        val fee = PudoSettlementEngine.calculateServiceFee(basePrintedPostalFee, 52.0, standardTariffModelA)
        assertEquals(140000L, fee)
    }

    @Test
    fun testBoundary_60h() {
        // 60h: 2 additional periods -> 140% -> 140,000 IRR
        val fee = PudoSettlementEngine.calculateServiceFee(basePrintedPostalFee, 60.0, standardTariffModelA)
        assertEquals(140000L, fee)
    }

    @Test
    fun testBoundary_71_99h() {
        // 71.99h: 2 additional periods -> 140% -> 140,000 IRR
        val fee = PudoSettlementEngine.calculateServiceFee(basePrintedPostalFee, 71.99, standardTariffModelA)
        assertEquals(140000L, fee)
    }

    @Test
    fun testBoundary_72h() {
        // 72h: 2 additional periods -> 140% -> 140,000 IRR
        val fee = PudoSettlementEngine.calculateServiceFee(basePrintedPostalFee, 72.0, standardTariffModelA)
        assertEquals(140000L, fee)
    }

    @Test
    fun testBoundary_96h() {
        // 96h: 3 additional periods -> 40% + 150% = 190% -> 190,000 IRR
        val fee = PudoSettlementEngine.calculateServiceFee(basePrintedPostalFee, 96.0, standardTariffModelA)
        assertEquals(190000L, fee)
    }

    @Test
    fun testBoundary_120h() {
        // 120h: 4 additional periods -> 40% + 200% = 240% -> 240,000 IRR
        val fee = PudoSettlementEngine.calculateServiceFee(basePrintedPostalFee, 120.0, standardTariffModelA)
        assertEquals(240000L, fee)
    }

    @Test
    fun testBoundary_144h() {
        // 144h: 5 additional periods -> 40% + 250% = 290% -> 290,000 IRR
        val fee = PudoSettlementEngine.calculateServiceFee(basePrintedPostalFee, 144.0, standardTariffModelA)
        assertEquals(290000L, fee)
    }

    @Test
    fun testBoundary_167_99h() {
        // 167.99h: 6 additional periods -> 40% + 300% = 340% -> 340,000 IRR
        val fee = PudoSettlementEngine.calculateServiceFee(basePrintedPostalFee, 167.99, standardTariffModelA)
        assertEquals(340000L, fee)
    }

    @Test
    fun testBoundary_168h() {
        // 168h (7 days max lifecycle): 6 additional periods -> 340% -> 340,000 IRR
        val fee = PudoSettlementEngine.calculateServiceFee(basePrintedPostalFee, 168.0, standardTariffModelA)
        assertEquals(340000L, fee)
    }

    @Test
    fun testBoundary_Over168h_CappedAtMaxLifecycle() {
        // > 168h (e.g. 200h): Capped at 168.0h -> 340% -> 340,000 IRR
        val fee = PudoSettlementEngine.calculateServiceFee(basePrintedPostalFee, 200.0, standardTariffModelA)
        assertEquals(340000L, fee)
    }

    // =========================================================================
    // LAYER 2: REVENUE DISTRIBUTION & CONSERVATION TESTS
    // =========================================================================

    @Test
    fun testLayer2ServiceRevenueDistribution_CanonicalExample() {
        // Canonical: ServiceFee = 40,000 IRR -> Courier: 12,000, Hub: 12,000, Network: 16,000
        val calculatedServiceFee = 40000L
        val distribution = PudoSettlementEngine.distributeRevenue(calculatedServiceFee, standardTariffModelA)

        assertEquals(12000L, distribution.courierShareAmount)
        assertEquals(12000L, distribution.hubShareAmount)
        assertEquals(16000L, distribution.networkShareAmount)
        assertEquals(calculatedServiceFee, distribution.courierShareAmount + distribution.hubShareAmount + distribution.networkShareAmount)
    }

    @Test
    fun testLayer2RevenueConservation_OddServiceFeeValues() {
        // Test multiple values to ensure zero loss of Rials (courier + hub + network == serviceFee)
        val testFees = listOf(10000L, 25000L, 33333L, 40000L, 77777L, 100000L, 340000L, 1L)
        for (fee in testFees) {
            val dist = PudoSettlementEngine.distributeRevenue(fee, standardTariffModelA)
            assertEquals("Conservation failed for fee=$fee", fee, dist.courierShareAmount + dist.hubShareAmount + dist.networkShareAmount)
        }
    }

    @Test
    fun testFull2LayerFinancialEvaluation() {
        val printedPostalFee = 100000L
        val duration = 18.0
        val eval = PudoSettlementEngine.evaluateParcelFinancials(printedPostalFee, duration, standardTariffModelA)

        assertEquals(100000L, eval.printedPostalFee)
        assertEquals(18.0, eval.deliveryDurationHours, 0.001)
        assertEquals("tariff-model-a-v1.0", eval.tariffVersionId)
        assertEquals(40000L, eval.calculatedServiceFee)
        assertEquals(12000L, eval.distribution.courierShareAmount)
        assertEquals(12000L, eval.distribution.hubShareAmount)
        assertEquals(16000L, eval.distribution.networkShareAmount)
    }

    // =========================================================================
    // SETTLEMENT ELIGIBILITY & CUSTODY RULES
    // =========================================================================

    @Test
    fun testSettlementEligibilityConfirmedCustodyOnly() {
        // Confirmed custody states -> Eligible for confirmed settlement
        assertTrue(PudoSettlementEngine.isEligibleForConfirmedSettlement(ParcelStatus.TRANSFERRED_TO_HUB))
        assertTrue(PudoSettlementEngine.isEligibleForConfirmedSettlement(ParcelStatus.STORED_AT_HUB))
        assertTrue(PudoSettlementEngine.isEligibleForConfirmedSettlement(ParcelStatus.DELIVERED_TO_CUSTOMER))

        // Pre-handover / unconfirmed / assigned states -> NOT eligible (pending only)
        assertFalse(PudoSettlementEngine.isEligibleForConfirmedSettlement(ParcelStatus.OUT_FOR_DELIVERY))
        assertFalse(PudoSettlementEngine.isEligibleForConfirmedSettlement(ParcelStatus.DELIVERY_ATTEMPTED))
        assertFalse(PudoSettlementEngine.isEligibleForConfirmedSettlement(ParcelStatus.ELIGIBLE_FOR_HUB))
        assertFalse(PudoSettlementEngine.isEligibleForConfirmedSettlement(ParcelStatus.HUB_SELECTED))
        assertFalse(PudoSettlementEngine.isEligibleForConfirmedSettlement(ParcelStatus.HANDOVER_IN_PROGRESS))
        assertFalse(PudoSettlementEngine.isEligibleForConfirmedSettlement(ParcelStatus.AWAITING_HUB_CONFIRMATION))
        assertFalse(PudoSettlementEngine.isEligibleForConfirmedSettlement(ParcelStatus.RETURNED_TO_SENDER))
        assertFalse(PudoSettlementEngine.isEligibleForConfirmedSettlement(ParcelStatus.REJECTED))
    }

    @Test
    fun testWeeklySettlementSummarySeparatesConfirmedAndPending() {
        val courierId = "courier-alireza-01"

        // BUGFIX regression coverage: duration is now derived per-parcel from
        // (createdAt -> updatedAt/now), not a single shared parameter applied to every parcel.
        // All four parcels here are set up with an 18-hour elapsed duration so the expected
        // totals below match the original intent of this test (tier2RatePercentage = 40%).
        val registeredAt = 0L
        val eighteenHoursLaterMillis = 18L * 3_600_000L

        val parcel1 = Parcel(
            id = "p-1",
            trackingNumber = "TRK-001",
            senderName = "فرستنده ۱",
            recipientName = "گیرنده ۱",
            recipientPhone = "09121111111",
            recipientPostalCode = "1111111111",
            status = ParcelStatus.TRANSFERRED_TO_HUB,
            assignedCourierId = courierId,
            baseFee = 100000L,
            createdAt = registeredAt,
            updatedAt = eighteenHoursLaterMillis
        )
        val parcel2 = Parcel(
            id = "p-2",
            trackingNumber = "TRK-002",
            senderName = "فرستنده ۲",
            recipientName = "گیرنده ۲",
            recipientPhone = "09122222222",
            recipientPostalCode = "2222222222",
            status = ParcelStatus.DELIVERED_TO_CUSTOMER,
            assignedCourierId = courierId,
            baseFee = 100000L,
            createdAt = registeredAt,
            updatedAt = eighteenHoursLaterMillis
        )
        val parcel3 = Parcel(
            id = "p-3",
            trackingNumber = "TRK-003",
            senderName = "فرستنده ۳",
            recipientName = "گیرنده ۳",
            recipientPhone = "09123333333",
            recipientPostalCode = "3333333333",
            status = ParcelStatus.ELIGIBLE_FOR_HUB,
            assignedCourierId = courierId,
            baseFee = 100000L,
            createdAt = registeredAt,
            updatedAt = eighteenHoursLaterMillis
        )
        val parcel4 = Parcel(
            id = "p-4",
            trackingNumber = "TRK-004",
            senderName = "فرستنده ۴",
            recipientName = "گیرنده ۴",
            recipientPhone = "09124444444",
            recipientPostalCode = "4444444444",
            status = ParcelStatus.HANDOVER_IN_PROGRESS,
            assignedCourierId = courierId,
            baseFee = 100000L,
            createdAt = registeredAt,
            updatedAt = eighteenHoursLaterMillis
        )

        val allParcels = listOf(parcel1, parcel2, parcel3, parcel4)

        val snapshot = PudoSettlementEngine.calculateCourierSettlementSummary(
            courierId = courierId,
            parcels = allParcels,
            tariff = standardTariffModelA,
            periodStartDate = 1000L,
            periodEndDate = 7000L,
            settlementPeriod = "WEEKLY_2026_W34",
            // Pending parcels (3 & 4) measure elapsed time up to "now" — pin it to the same
            // 18-hour mark as the confirmed parcels' updatedAt so all four land in the same tier.
            nowMillis = eighteenHoursLaterMillis
        )

        assertEquals(2, snapshot.totalParcelsTransferred)
        assertEquals(400000L, snapshot.printedPostalFee)
        assertEquals(160000L, snapshot.calculatedServiceFee)
        assertEquals(24000L, snapshot.confirmedAmount)
        assertEquals(24000L, snapshot.pendingAmount)
        assertEquals("tariff-model-a-v1.0", snapshot.servicePricingRuleVersion)
        assertEquals(0.30, snapshot.courierSharePercentage, 0.001)
        assertEquals(0.30, snapshot.hubSharePercentage, 0.001)
        assertEquals(0.40, snapshot.networkSharePercentage, 0.001)
    }

    // =========================================================================
    // FINANCIAL IMMUTABILITY & VERSION INDEPENDENCE
    // =========================================================================

    @Test
    fun testHistoricalFinancialImmutability() {
        val originalSnapshot = CourierSettlementSnapshot(
            snapshotId = "snap-hist-001",
            courierId = "courier-hist",
            hubId = "hub-hist",
            periodStartDate = 1000L,
            periodEndDate = 8000L,
            settlementPeriod = "WEEKLY_2026_W33",
            totalParcelsTransferred = 5,
            printedPostalFee = 500000L,
            servicePricingRuleVersion = "tariff-model-a-v1.0",
            deliveryDurationHours = 18.0,
            calculatedServiceFee = 200000L,
            courierSharePercentage = 0.30,
            hubSharePercentage = 0.30,
            networkSharePercentage = 0.40,
            courierShareAmount = 60000L,
            hubShareAmount = 60000L,
            networkShareAmount = 80000L,
            confirmedAmount = 60000L,
            pendingAmount = 0L,
            isPaid = true
        )

        val futureTariff = SettlementTariffVersion(
            id = "tariff-model-b-v2.0",
            versionCode = 2,
            versionName = "تعرفه سال آینده",
            courierSharePercentage = 0.35,
            hubSharePercentage = 0.35,
            networkSharePercentage = 0.30
        )

        assertEquals("tariff-model-a-v1.0", originalSnapshot.servicePricingRuleVersion)
        assertEquals(500000L, originalSnapshot.printedPostalFee)
        assertEquals(200000L, originalSnapshot.calculatedServiceFee)
        assertEquals(60000L, originalSnapshot.courierShareAmount)
        assertEquals(0.30, originalSnapshot.courierSharePercentage, 0.001)
        assertEquals(0.30, originalSnapshot.hubSharePercentage, 0.001)
        assertEquals(0.40, originalSnapshot.networkSharePercentage, 0.001)
    }

    // =========================================================================
    // FUTURE-PROOFING EXTENSION POINTS TESTS
    // =========================================================================

    @Test
    fun testPricingContextAndPolicyAbstraction() {
        val policy = StandardModelAPricingPolicy()
        val context = PricingContext(
            customerType = "B2B_STANDARD",
            organizationId = "org-post-iran",
            contractId = "contract-2026-001",
            tariffVersion = standardTariffModelA
        )

        val fee18h = policy.calculateServiceFee(100000L, 18.0, context)
        assertEquals(40000L, fee18h)

        val fee30h = policy.calculateServiceFee(100000L, 30.0, context)
        assertEquals(90000L, fee30h)
    }

    @Test
    fun testBasicHubRecommendationPolicy() {
        val policy = BasicHubRecommendationPolicy()
        val hubs = listOf(
            HubItem(id = "hub-1", name = "هاب ۱", type = "supermarket", typeName = "سوپرمارکت", managerName = "مدیر ۱", phone = "09121111111", licenseNumber = "LIC-1", address = "خیابان ۱", maxCapacity = 100, currentPackagesCount = 90, isOpen = true),
            HubItem(id = "hub-2", name = "هاب ۲ (بسته)", type = "supermarket", typeName = "سوپرمارکت", managerName = "مدیر ۲", phone = "09122222222", licenseNumber = "LIC-2", address = "خیابان ۲", maxCapacity = 100, currentPackagesCount = 10, isOpen = false),
            HubItem(id = "hub-3", name = "هاب ۳ (خلوت)", type = "supermarket", typeName = "سوپرمارکت", managerName = "مدیر ۳", phone = "09123333333", licenseNumber = "LIC-3", address = "خیابان ۳", maxCapacity = 100, currentPackagesCount = 20, isOpen = true)
        )

        val recommended = policy.recommendHubs(HubRecommendationRequest(availableHubs = hubs))
        assertEquals(2, recommended.size)
        // hub-3 has remaining capacity 80, hub-1 has remaining capacity 10 -> hub-3 first
        assertEquals("hub-3", recommended[0].id)
        assertEquals("hub-1", recommended[1].id)
    }

    @Test
    fun testScanContextEnum() {
        val contexts = ScanContext.values()
        assertTrue(contexts.contains(ScanContext.PARCEL_REGISTRATION))
        assertTrue(contexts.contains(ScanContext.COURIER_HANDOVER))
        assertTrue(contexts.contains(ScanContext.HUB_RECEIPT))
        assertTrue(contexts.contains(ScanContext.CUSTOMER_COLLECTION))
    }
}
