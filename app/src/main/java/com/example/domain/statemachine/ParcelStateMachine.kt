package com.example.domain.statemachine

import com.example.data.model.ParcelStatus
import com.example.data.model.RegistrationSource

object ParcelStateMachine {

    private val validTransitions: Map<ParcelStatus, Set<ParcelStatus>> = mapOf(
        ParcelStatus.OUT_FOR_DELIVERY to setOf(
            ParcelStatus.DELIVERY_ATTEMPTED,
            ParcelStatus.ELIGIBLE_FOR_HUB,
            ParcelStatus.HUB_SELECTED,
            ParcelStatus.HANDOVER_IN_PROGRESS,
            ParcelStatus.DELIVERED_TO_CUSTOMER,
            ParcelStatus.RETURNED_TO_SENDER,
            ParcelStatus.REJECTED
        ),
        ParcelStatus.DELIVERY_ATTEMPTED to setOf(
            ParcelStatus.ELIGIBLE_FOR_HUB,
            ParcelStatus.HUB_SELECTED,
            ParcelStatus.RETURNED_TO_SENDER,
            ParcelStatus.REJECTED
        ),
        ParcelStatus.ELIGIBLE_FOR_HUB to setOf(
            ParcelStatus.HUB_SELECTED,
            ParcelStatus.HANDOVER_IN_PROGRESS,
            ParcelStatus.TRANSFERRED_TO_HUB,
            ParcelStatus.STORED_AT_HUB,
            ParcelStatus.REJECTED
        ),
        ParcelStatus.HUB_SELECTED to setOf(
            ParcelStatus.HANDOVER_IN_PROGRESS,
            ParcelStatus.TRANSFERRED_TO_HUB,
            ParcelStatus.STORED_AT_HUB,
            ParcelStatus.ELIGIBLE_FOR_HUB,
            ParcelStatus.REJECTED
        ),
        ParcelStatus.HANDOVER_IN_PROGRESS to setOf(
            ParcelStatus.AWAITING_HUB_CONFIRMATION,
            ParcelStatus.TRANSFERRED_TO_HUB,
            ParcelStatus.STORED_AT_HUB,
            ParcelStatus.HUB_SELECTED,
            ParcelStatus.REJECTED
        ),
        ParcelStatus.AWAITING_HUB_CONFIRMATION to setOf(
            ParcelStatus.TRANSFERRED_TO_HUB,
            ParcelStatus.STORED_AT_HUB,
            ParcelStatus.HANDOVER_IN_PROGRESS,
            ParcelStatus.REJECTED
        ),
        ParcelStatus.TRANSFERRED_TO_HUB to setOf(
            ParcelStatus.STORED_AT_HUB,
            ParcelStatus.DELIVERED_TO_CUSTOMER,
            ParcelStatus.RETURNED_TO_SENDER,
            ParcelStatus.REJECTED
        ),
        ParcelStatus.STORED_AT_HUB to setOf(
            ParcelStatus.DELIVERED_TO_CUSTOMER,
            ParcelStatus.RETURNED_TO_SENDER,
            ParcelStatus.REJECTED
        ),
        ParcelStatus.DELIVERED_TO_CUSTOMER to emptySet(),
        ParcelStatus.RETURNED_TO_SENDER to emptySet(),
        ParcelStatus.REJECTED to emptySet()
    )

    fun canTransition(current: ParcelStatus, next: ParcelStatus): Boolean {
        if (current == next) return false
        return validTransitions[current]?.contains(next) == true
    }

    fun validateTransition(current: ParcelStatus, next: ParcelStatus) {
        require(canTransition(current, next)) {
            "Illegal state transition from ${current.name} to ${next.name}. Transition guard violation."
        }
    }

    fun isSettlementEligible(status: ParcelStatus): Boolean {
        return status.isSettlementEligible
    }

    fun canRegisterFromRoute(source: RegistrationSource): Boolean {
        return source.isPrimaryWorkflow
    }
}
