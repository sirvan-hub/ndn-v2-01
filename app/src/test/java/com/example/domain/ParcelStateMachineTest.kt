package com.example.domain

import com.example.data.model.AccountApprovalStatus
import com.example.data.model.AuditLog
import com.example.data.model.Parcel
import com.example.data.model.ParcelSize
import com.example.data.model.ParcelStatus
import com.example.data.model.RegistrationSource
import com.example.data.model.RegistrationTransaction
import com.example.domain.statemachine.ParcelStateMachine
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class ParcelStateMachineTest {

    @Test
    fun testValidPrimaryOperationalFlow() {
        // OUT_FOR_DELIVERY -> DELIVERY_ATTEMPTED -> ELIGIBLE_FOR_HUB -> HUB_SELECTED -> HANDOVER_IN_PROGRESS -> AWAITING_HUB_CONFIRMATION -> TRANSFERRED_TO_HUB -> STORED_AT_HUB -> DELIVERED_TO_CUSTOMER
        assertTrue(ParcelStateMachine.canTransition(ParcelStatus.OUT_FOR_DELIVERY, ParcelStatus.DELIVERY_ATTEMPTED))
        assertTrue(ParcelStateMachine.canTransition(ParcelStatus.DELIVERY_ATTEMPTED, ParcelStatus.ELIGIBLE_FOR_HUB))
        assertTrue(ParcelStateMachine.canTransition(ParcelStatus.ELIGIBLE_FOR_HUB, ParcelStatus.HUB_SELECTED))
        assertTrue(ParcelStateMachine.canTransition(ParcelStatus.HUB_SELECTED, ParcelStatus.HANDOVER_IN_PROGRESS))
        assertTrue(ParcelStateMachine.canTransition(ParcelStatus.HANDOVER_IN_PROGRESS, ParcelStatus.AWAITING_HUB_CONFIRMATION))
        assertTrue(ParcelStateMachine.canTransition(ParcelStatus.AWAITING_HUB_CONFIRMATION, ParcelStatus.TRANSFERRED_TO_HUB))
        assertTrue(ParcelStateMachine.canTransition(ParcelStatus.TRANSFERRED_TO_HUB, ParcelStatus.STORED_AT_HUB))
        assertTrue(ParcelStateMachine.canTransition(ParcelStatus.STORED_AT_HUB, ParcelStatus.DELIVERED_TO_CUSTOMER))
    }

    @Test
    fun testDirectCustomerRequestToHubEligibility() {
        // Route registration on customer request
        assertTrue(ParcelStateMachine.canTransition(ParcelStatus.OUT_FOR_DELIVERY, ParcelStatus.ELIGIBLE_FOR_HUB))
    }

    @Test
    fun testIllegalTransitionsAreRejected() {
        // Direct jump from OUT_FOR_DELIVERY to STORED_AT_HUB (bypassing handover)
        assertFalse(ParcelStateMachine.canTransition(ParcelStatus.OUT_FOR_DELIVERY, ParcelStatus.STORED_AT_HUB))

        // Direct jump from OUT_FOR_DELIVERY to TRANSFERRED_TO_HUB (bypassing hub selection & confirmation)
        assertFalse(ParcelStateMachine.canTransition(ParcelStatus.OUT_FOR_DELIVERY, ParcelStatus.TRANSFERRED_TO_HUB))

        // Transition backwards from terminal DELIVERED_TO_CUSTOMER
        assertFalse(ParcelStateMachine.canTransition(ParcelStatus.DELIVERED_TO_CUSTOMER, ParcelStatus.OUT_FOR_DELIVERY))

        // Same-state transition must be rejected (idempotency guard)
        assertFalse(ParcelStateMachine.canTransition(ParcelStatus.OUT_FOR_DELIVERY, ParcelStatus.OUT_FOR_DELIVERY))
        assertFalse(ParcelStateMachine.canTransition(ParcelStatus.STORED_AT_HUB, ParcelStatus.STORED_AT_HUB))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testValidateTransitionThrowsOnIllegalMove() {
        ParcelStateMachine.validateTransition(ParcelStatus.OUT_FOR_DELIVERY, ParcelStatus.STORED_AT_HUB)
    }

    @Test
    fun testSettlementEligibilityRule() {
        // In-transit / Registration states MUST NOT be settlement eligible
        assertFalse(ParcelStateMachine.isSettlementEligible(ParcelStatus.OUT_FOR_DELIVERY))
        assertFalse(ParcelStateMachine.isSettlementEligible(ParcelStatus.DELIVERY_ATTEMPTED))
        assertFalse(ParcelStateMachine.isSettlementEligible(ParcelStatus.ELIGIBLE_FOR_HUB))
        assertFalse(ParcelStateMachine.isSettlementEligible(ParcelStatus.HUB_SELECTED))
        assertFalse(ParcelStateMachine.isSettlementEligible(ParcelStatus.HANDOVER_IN_PROGRESS))
        assertFalse(ParcelStateMachine.isSettlementEligible(ParcelStatus.AWAITING_HUB_CONFIRMATION))
        assertFalse(ParcelStateMachine.isSettlementEligible(ParcelStatus.REJECTED))
        assertFalse(ParcelStateMachine.isSettlementEligible(ParcelStatus.RETURNED_TO_SENDER))

        // Only verified handover and hub custody states are settlement eligible
        assertTrue(ParcelStateMachine.isSettlementEligible(ParcelStatus.TRANSFERRED_TO_HUB))
        assertTrue(ParcelStateMachine.isSettlementEligible(ParcelStatus.STORED_AT_HUB))
        assertTrue(ParcelStateMachine.isSettlementEligible(ParcelStatus.DELIVERED_TO_CUSTOMER))
    }

    @Test
    fun testRegistrationSources() {
        // Customer request and failed delivery are primary on-route workflows
        assertTrue(RegistrationSource.CUSTOMER_REQUEST.isPrimaryWorkflow)
        assertTrue(RegistrationSource.FAILED_HOME_DELIVERY.isPrimaryWorkflow)

        // Bulk end-of-shift is strictly an exception/recovery mechanism
        assertFalse(RegistrationSource.END_OF_SHIFT_RECOVERY.isPrimaryWorkflow)
    }

    @Test
    fun testRegistrationTransactionIntegrity() {
        val txId = UUID.randomUUID().toString()
        val parcelId = "parcel-${UUID.randomUUID()}"
        val tx = RegistrationTransaction(
            transactionId = txId,
            parcelId = parcelId,
            trackingNumber = "IR-987654321",
            courierId = "courier-01",
            hubId = "hub-saadat-abad",
            registrationSource = RegistrationSource.CUSTOMER_REQUEST
        )

        assertEquals(txId, tx.transactionId)
        assertEquals("IR-987654321", tx.trackingNumber)
        assertEquals(RegistrationSource.CUSTOMER_REQUEST, tx.registrationSource)
    }

    @Test
    fun testAuditLogCreation() {
        val auditLog = AuditLog(
            id = "audit-1",
            eventType = "PARCEL_STATUS_TRANSITION",
            actorId = "courier-01",
            actorRole = "COURIER",
            entityId = "parcel-101",
            oldState = ParcelStatus.AWAITING_HUB_CONFIRMATION.name,
            newState = ParcelStatus.TRANSFERRED_TO_HUB.name,
            transactionId = "tx-12345"
        )

        assertEquals("PARCEL_STATUS_TRANSITION", auditLog.eventType)
        assertEquals("COURIER", auditLog.actorRole)
        assertEquals("AWAITING_HUB_CONFIRMATION", auditLog.oldState)
        assertEquals("TRANSFERRED_TO_HUB", auditLog.newState)
        assertNotNull(auditLog.timestamp)
    }

    @Test
    fun testAccountApprovalStatusValues() {
        assertEquals(3, AccountApprovalStatus.values().size)
        assertTrue(AccountApprovalStatus.values().contains(AccountApprovalStatus.PENDING))
        assertTrue(AccountApprovalStatus.values().contains(AccountApprovalStatus.APPROVED))
        assertTrue(AccountApprovalStatus.values().contains(AccountApprovalStatus.REJECTED))
    }
}
