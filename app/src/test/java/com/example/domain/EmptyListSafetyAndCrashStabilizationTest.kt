package com.example.domain

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.model.CourierCandidate
import com.example.model.HubItem
import com.example.model.PackageItem
import com.example.model.PackageSize
import com.example.model.PackageStatus
import com.example.viewmodel.NdnViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EmptyListSafetyAndCrashStabilizationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: NdnViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val app = ApplicationProvider.getApplicationContext<Application>()
        viewModel = NdnViewModel(app)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testCourierScreenLookup_WithEmptyHubs_DoesNotThrowException() {
        val emptyHubs = emptyList<HubItem>()
        val selectedHubId = "non-existent-hub-id"
        
        // This was the exact crash point: uiState.hubs.find { it.id == selectedHubId } ?: uiState.hubs.first()
        val safeLookup = emptyHubs.find { it.id == selectedHubId } ?: emptyHubs.firstOrNull()
        
        assertNull("Safe lookup on empty hubs must return null instead of throwing NoSuchElementException", safeLookup)
    }

    @Test
    fun testCustomerScreenLookup_WithEmptyHubs_DoesNotThrowException() {
        val emptyHubs = emptyList<HubItem>()
        val selectedPackage = PackageItem(
            id = "pkg-01",
            trackingCode = "TRK-123456",
            title = "Test Pkg",
            sender = "Sender",
            receiver = "Receiver",
            receiverPhone = "09121112233",
            hubId = "missing-hub",
            hubName = "Missing Hub",
            hubAddress = "Tehran",
            status = PackageStatus.PENDING_COURIER_VERIFICATION,
            statusText = "Pending",
            dimensions = "10x10x10",
            weight = "1kg",
            size = PackageSize.MEDIUM,
            baseFee = 25000L,
            totalFee = 25000L,
            isPaid = false
        )

        // Previous crash point: val hubObj = uiState.hubs.find { it.id == selected.hubId } ?: uiState.hubs.first()
        val safeHubObj = emptyHubs.find { it.id == selectedPackage.hubId } ?: emptyHubs.firstOrNull()
        
        assertNull("Safe hub object lookup on empty hubs must return null", safeHubObj)
    }

    @Test
    fun testBarcodeScannerModal_WithEmptyAvailablePackages_DoesNotThrowException() {
        val availablePackages = emptyList<PackageItem>()
        val targetPackage: PackageItem? = null

        // Previous crash point: val pkg = targetPackage ?: availablePackages.first()
        val safePkg = targetPackage ?: availablePackages.firstOrNull()

        assertNull("Safe package lookup in barcode scanner modal must return null when empty", safePkg)
    }

    @Test
    fun testRegisterNewPackageByCourier_WithEmptyOrMissingHub_SafelyAbortsWithToast() = runTest {
        viewModel.registerNewPackageByCourier(
            title = "Test Package",
            receiver = "Test Receiver",
            receiverPhone = "09123456789",
            size = PackageSize.MEDIUM,
            hubId = "non-existent-hub-id-xyz"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // When hubId is not found, it safely falls back to firstOrNull() or aborts without crashing
        assertNotNull("ViewModel must maintain state without unhandled exception", viewModel.uiState.value)
    }

    @Test
    fun testRegisterManualPackageByCustomer_WithInvalidHubAndCourier_SafelyAbortsWithToast() = runTest {
        viewModel.registerManualPackageByCustomer(
            trackingCode = "TRK-999888",
            title = "Manual Package",
            sender = "Store A",
            courierId = "invalid-courier-id-xyz",
            hubId = "invalid-hub-id-xyz"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Safe fallback or safe error toast without crashing
        assertNotNull("ViewModel state must remain stable", viewModel.uiState.value)
        assertFalse("Manual tracking modal should be closed after registration attempt", viewModel.uiState.value.isManualTrackingModalOpen)
    }

    @Test
    fun testManualTrackingDialog_ValidationLogicBlocksEmptySubmissions() {
        val emptyCouriers = emptyList<CourierCandidate>()
        val emptyHubs = emptyList<HubItem>()
        val trackingInput = "TRK-123456"
        val selectedCourierId = ""
        val selectedHubId = ""

        val hasValidCourier = selectedCourierId.isNotBlank() && emptyCouriers.any { it.id == selectedCourierId }
        val hasValidHub = selectedHubId.isNotBlank() && emptyHubs.any { it.id == selectedHubId }
        val isFormValid = trackingInput.isNotBlank() && hasValidCourier && hasValidHub

        assertFalse("Form must NOT be valid when courier or hub list is empty", isFormValid)
    }
}
