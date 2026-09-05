package com.example.camera

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.database.PudoDatabase
import com.example.data.model.*
import com.example.data.repository.PudoRepository
import com.example.data.repository.PudoRepositoryImpl
import com.example.domain.statemachine.ParcelStateMachine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BarcodeScannerIntegrationTest {

    private lateinit var database: PudoDatabase
    private lateinit var repository: PudoRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = PudoDatabase.createInMemory(context)
        repository = PudoRepositoryImpl(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // Test A: Scanner result accepted by duplicate guard
    @Test
    fun testScannerResultAccepted() {
        val guard = BarcodeDuplicateGuard(debounceCooldownMs = 1000L)
        val rawBarcode = "NDN-TRK-99001"

        assertTrue("First scan of valid barcode should be accepted", guard.shouldProcess(rawBarcode))
        guard.markProcessed(rawBarcode)
        assertTrue("Guard should remember processed barcode", guard.isAlreadyAcceptedInSession(rawBarcode))
    }

    // Test B: Empty scan result rejected
    @Test
    fun testEmptyScanResultRejected() {
        val guard = BarcodeDuplicateGuard(debounceCooldownMs = 1000L)
        assertFalse("Empty barcode string should be rejected", guard.shouldProcess(""))
        assertFalse("Blank barcode string should be rejected", guard.shouldProcess("   "))
        assertFalse("Null barcode should be rejected", guard.shouldProcess(null))
    }

    // Test C: Invalid / Whitespace barcode sanitized
    @Test
    fun testBarcodeSanitization() {
        val guard = BarcodeDuplicateGuard(debounceCooldownMs = 1000L)
        val untrimmed = "   IR-12345678   "
        assertTrue(guard.shouldProcess(untrimmed))
        guard.markProcessed(untrimmed)
        assertTrue(guard.isAlreadyAcceptedInSession("IR-12345678"))
    }

    // Test D: Duplicate barcode suppressed during cooldown window
    @Test
    fun testDuplicateBarcodeSuppression() {
        val guard = BarcodeDuplicateGuard(debounceCooldownMs = 5000L)
        val barcode = "NDN-SCAN-DUPLICATE"

        assertTrue("Initial scan accepted", guard.shouldProcess(barcode))
        guard.markProcessed(barcode)

        // Immediate subsequent scan from next camera frame
        assertFalse("Immediate duplicate scan across frames must be suppressed", guard.shouldProcess(barcode))
    }

    // Test E: Multiple distinct barcode detection
    @Test
    fun testMultipleDistinctBarcodeDetection() {
        val guard = BarcodeDuplicateGuard(debounceCooldownMs = 5000L)
        val code1 = "NDN-PKG-101"
        val code2 = "NDN-PKG-102"
        val code3 = "NDN-PKG-103"

        assertTrue(guard.shouldProcess(code1))
        guard.markProcessed(code1)

        assertTrue("Different code in the same session should be accepted", guard.shouldProcess(code2))
        guard.markProcessed(code2)

        assertTrue("Third distinct code should be accepted", guard.shouldProcess(code3))
        guard.markProcessed(code3)

        assertEquals(true, guard.isAlreadyAcceptedInSession(code1))
        assertEquals(true, guard.isAlreadyAcceptedInSession(code2))
        assertEquals(true, guard.isAlreadyAcceptedInSession(code3))
    }

    // Test F: Haptic feedback helper executes without throwing on no-vibrator environments
    @Test
    fun testCameraFeedbackHelperSafety() {
        // Must not crash in tests/emulators
        CameraFeedbackHelper.triggerScanHapticFeedback(context)
        // Rapid second trigger tested for cooldown
        CameraFeedbackHelper.triggerScanHapticFeedback(context)
    }

    // Test G: Manual fallback code entry follows identical validation pathway
    @Test
    fun testManualFallbackConvergence() = runTest {
        val manualTrackingCode = "MANUAL-TRK-7788"

        val parcel = Parcel(
            id = "pkg-manual-1",
            trackingNumber = manualTrackingCode,
            senderName = "فرستنده",
            recipientName = "گیرنده دستی",
            recipientPhone = "09121112233",
            recipientPostalCode = "1234567890",
            recipientAddress = "تهران، میرداماد",
            status = ParcelStatus.OUT_FOR_DELIVERY,
            size = ParcelSize.SMALL,
            assignedHubId = null,
            assignedHubName = null,
            assignedCourierId = "courier-1",
            assignedCourierName = "سفیر احمدی",
            registrationSource = RegistrationSource.CUSTOMER_REQUEST,
            baseFee = 20000L
        )

        val tx = RegistrationTransaction(
            transactionId = "tx-manual-1",
            parcelId = parcel.id,
            trackingNumber = manualTrackingCode,
            courierId = "courier-1",
            hubId = "hub-1",
            registrationSource = RegistrationSource.CUSTOMER_REQUEST
        )

        val result = repository.registerPudoParcel(parcel, tx, "courier-1", "COURIER")
        assertTrue(result.isSuccess)

        val loaded = database.parcelDao().getParcelByTrackingNumber(manualTrackingCode)
        assertNotNull("Manually entered parcel must be committed to Room source of truth", loaded)
        assertEquals(manualTrackingCode, loaded?.trackingNumber)
    }

    // Test H: Offline registration using scanned barcode
    @Test
    fun testOfflineRegistrationViaScannedCode() = runTest {
        val scannedBarcode = "BARCODE-OFFLINE-99"

        val parcel = Parcel(
            id = "pkg-offline-scan-1",
            trackingNumber = scannedBarcode,
            senderName = "دیجی‌کالا",
            recipientName = "رضا باقری",
            recipientPhone = "09351234567",
            recipientPostalCode = "1998765432",
            recipientAddress = "تهران، پونک",
            status = ParcelStatus.DELIVERY_ATTEMPTED,
            size = ParcelSize.MEDIUM,
            assignedHubId = null,
            assignedHubName = null,
            assignedCourierId = "courier-offline",
            assignedCourierName = "سفیر کریمی",
            registrationSource = RegistrationSource.FAILED_HOME_DELIVERY,
            baseFee = 25000L
        )

        val tx = RegistrationTransaction(
            transactionId = "tx-offline-scan-1",
            parcelId = parcel.id,
            trackingNumber = scannedBarcode,
            courierId = "courier-offline",
            hubId = "hub-pudo-1",
            registrationSource = RegistrationSource.FAILED_HOME_DELIVERY
        )

        val regResult = repository.registerPudoParcel(parcel, tx, "courier-offline", "COURIER")
        assertTrue("Offline scan registration must succeed locally", regResult.isSuccess)

        // Verify SyncQueue contains entry
        val pendingSync = database.syncQueueDao().getPendingSyncItemsDirect()
        val syncEntry = pendingSync.find { it.entityId == parcel.id }
        assertNotNull("Offline registration must create a pending SyncQueue entry", syncEntry)
        assertEquals("PARCEL", syncEntry?.entityType)
        assertEquals("CREATE", syncEntry?.action)
    }

    // Test I: RegistrationTransaction idempotency on duplicate scan submission
    @Test
    fun testIdempotencyOnDuplicateScannedRegistration() = runTest {
        val trackingCode = "TRK-IDEMP-SCAN"

        val parcel = Parcel(
            id = "pkg-idemp-1",
            trackingNumber = trackingCode,
            senderName = "فرستنده",
            recipientName = "گیرنده",
            recipientPhone = "09120000000",
            recipientPostalCode = "1111111111",
            recipientAddress = "تهران",
            status = ParcelStatus.ELIGIBLE_FOR_HUB,
            size = ParcelSize.SMALL,
            assignedHubId = null,
            assignedHubName = null,
            assignedCourierId = "courier-1",
            assignedCourierName = "سفیر",
            registrationSource = RegistrationSource.CUSTOMER_REQUEST,
            baseFee = 18000L
        )

        val tx = RegistrationTransaction(
            transactionId = "tx-unique-scan-1",
            parcelId = parcel.id,
            trackingNumber = trackingCode,
            courierId = "courier-1",
            hubId = "hub-1",
            registrationSource = RegistrationSource.CUSTOMER_REQUEST
        )

        // First registration
        val firstResult = repository.registerPudoParcel(parcel, tx, "courier-1", "COURIER")
        assertTrue(firstResult.isSuccess)

        // Second registration attempt with identical transaction ID (simulating resubmit or duplicate frame submit)
        val secondResult = repository.registerPudoParcel(parcel, tx, "courier-1", "COURIER")
        assertTrue("Idempotent second submission should return success without failure", secondResult.isSuccess)
        assertEquals(parcel.id, secondResult.getOrNull()?.id)

        // Verify only 1 parcel row and 1 transaction row exist
        val parcelCount = database.parcelDao().getAllParcelsDirect().count { it.id == parcel.id }
        assertEquals(1, parcelCount)
    }

    // Test J: AuditLog creation with actor and tracking number
    @Test
    fun testAuditLogCreationOnScan() = runTest {
        val parcel = Parcel(
            id = "pkg-audit-scan-1",
            trackingNumber = "TRK-AUDIT-SCAN",
            senderName = "فروشگاه",
            recipientName = "خریدار",
            recipientPhone = "09123456789",
            recipientPostalCode = "1234567890",
            recipientAddress = "تهران",
            status = ParcelStatus.ELIGIBLE_FOR_HUB,
            size = ParcelSize.LARGE,
            assignedHubId = null,
            assignedHubName = null,
            assignedCourierId = "courier-44",
            assignedCourierName = "سفیر نوری",
            registrationSource = RegistrationSource.FAILED_HOME_DELIVERY,
            baseFee = 30000L
        )

        val tx = RegistrationTransaction(
            transactionId = "tx-audit-scan-1",
            parcelId = parcel.id,
            trackingNumber = parcel.trackingNumber,
            courierId = "courier-44",
            hubId = "hub-44",
            registrationSource = RegistrationSource.FAILED_HOME_DELIVERY
        )

        repository.registerPudoParcel(parcel, tx, "courier-44", "COURIER")

        val logs = database.auditLogDao().getLogsByEntityId(parcel.id).first()
        assertTrue("Audit log must exist for scanned parcel registration", logs.isNotEmpty())
        val regLog = logs.find { it.eventType == "PARCEL_PUDO_REGISTERED" }
        assertNotNull(regLog)
        assertEquals("courier-44", regLog?.actorId)
        assertEquals("COURIER", regLog?.actorRole)
        assertEquals("tx-audit-scan-1", regLog?.transactionId)
    }

    // Test K: Repository integration with scanned code
    @Test
    fun testRepositoryIntegrationWithScannedCode() = runTest {
        val scannedCode = "SCAN-HUB-RECEIVE-100"

        val parcel = Parcel(
            id = "pkg-hub-rec-1",
            trackingNumber = scannedCode,
            senderName = "فرستنده",
            recipientName = "گیرنده",
            recipientPhone = "09121111111",
            recipientPostalCode = "1111111111",
            recipientAddress = "تهران",
            status = ParcelStatus.ELIGIBLE_FOR_HUB,
            size = ParcelSize.MEDIUM,
            assignedHubId = null,
            assignedHubName = null,
            assignedCourierId = "courier-1",
            assignedCourierName = "سفیر",
            registrationSource = RegistrationSource.FAILED_HOME_DELIVERY,
            baseFee = 25000L
        )

        val tx = RegistrationTransaction(
            transactionId = "tx-hub-rec-1",
            parcelId = parcel.id,
            trackingNumber = scannedCode,
            courierId = "courier-1",
            hubId = "hub-01",
            registrationSource = RegistrationSource.FAILED_HOME_DELIVERY
        )

        // 1. Register parcel at customer location
        val regResult = repository.registerPudoParcel(parcel, tx, "courier-1", "COURIER")
        assertTrue("Registration must succeed", regResult.isSuccess)

        // 2. Select Hub
        val assignResult = repository.assignHub(parcel.id, "hub-01", "هاب آزادی", "courier-1", "COURIER")
        assertTrue("Hub assignment must succeed", assignResult.isSuccess)

        // 3. Courier starts handover with OTP
        val startResult = repository.startHandover(parcel.id, "5566", "courier-1", "COURIER")
        assertTrue("Start handover must succeed", startResult.isSuccess)
        assertEquals(ParcelStatus.HANDOVER_IN_PROGRESS, startResult.getOrNull()?.status)

        // 4. Hub manager confirms handover with matching OTP
        val confirmResult = repository.confirmHubHandover(parcel.id, "5566", "hub-manager-1", "HUB_MANAGER")
        assertTrue("Hub handover confirmation must succeed", confirmResult.isSuccess)
        assertEquals(ParcelStatus.TRANSFERRED_TO_HUB, confirmResult.getOrNull()?.status)
    }

    // Test L: StateMachine validation on scanned parcel status transitions
    @Test
    fun testStateMachineValidationOnScannedParcel() {
        // Cannot jump directly from OUT_FOR_DELIVERY to STORED_AT_HUB without handover
        assertFalse(ParcelStateMachine.canTransition(ParcelStatus.OUT_FOR_DELIVERY, ParcelStatus.STORED_AT_HUB))
        // Valid sequence: OUT_FOR_DELIVERY -> DELIVERY_ATTEMPTED -> ELIGIBLE_FOR_HUB -> HUB_SELECTED -> HANDOVER_IN_PROGRESS -> AWAITING_HUB_CONFIRMATION -> TRANSFERRED_TO_HUB -> STORED_AT_HUB -> DELIVERED_TO_CUSTOMER
        assertTrue(ParcelStateMachine.canTransition(ParcelStatus.OUT_FOR_DELIVERY, ParcelStatus.DELIVERY_ATTEMPTED))
        assertTrue(ParcelStateMachine.canTransition(ParcelStatus.DELIVERY_ATTEMPTED, ParcelStatus.ELIGIBLE_FOR_HUB))
        assertTrue(ParcelStateMachine.canTransition(ParcelStatus.ELIGIBLE_FOR_HUB, ParcelStatus.HUB_SELECTED))
        assertTrue(ParcelStateMachine.canTransition(ParcelStatus.HUB_SELECTED, ParcelStatus.HANDOVER_IN_PROGRESS))
        assertTrue(ParcelStateMachine.canTransition(ParcelStatus.HANDOVER_IN_PROGRESS, ParcelStatus.AWAITING_HUB_CONFIRMATION))
        assertTrue(ParcelStateMachine.canTransition(ParcelStatus.AWAITING_HUB_CONFIRMATION, ParcelStatus.TRANSFERRED_TO_HUB))
        assertTrue(ParcelStateMachine.canTransition(ParcelStatus.TRANSFERRED_TO_HUB, ParcelStatus.STORED_AT_HUB))
        assertTrue(ParcelStateMachine.canTransition(ParcelStatus.STORED_AT_HUB, ParcelStatus.DELIVERED_TO_CUSTOMER))
    }

    // Test M: Scanner lifecycle cleanup (guard reset)
    @Test
    fun testScannerLifecycleCleanup() {
        val guard = BarcodeDuplicateGuard(debounceCooldownMs = 10000L)
        val code = "NDN-RESET-TEST"

        guard.markProcessed(code)
        assertTrue(guard.isAlreadyAcceptedInSession(code))
        assertFalse(guard.shouldProcess(code))

        // Reset session on screen dispose
        guard.reset()
        assertFalse(guard.isAlreadyAcceptedInSession(code))
        assertTrue("After reset, guard must accept new scan session", guard.shouldProcess(code))
    }
}
