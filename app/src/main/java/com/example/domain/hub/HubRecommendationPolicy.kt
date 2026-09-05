package com.example.domain.hub

import com.example.model.HubItem

/**
 * Request criteria for Hub recommendation.
 * Allows future expansion (e.g. courier GPS trajectory, traffic, detour)
 * without rewriting the Hub selection UI or Parcel models.
 */
data class HubRecommendationRequest(
    val packagePostalCode: String? = null,
    val packageSize: String? = null,
    val availableHubs: List<HubItem> = emptyList(),
    val preferredHubId: String? = null
)

/**
 * Domain policy boundary for Hub Recommendation.
 * Enables transitioning from Basic capacity/status filtering to
 * Route-Aware Hub Recommendation in Phase 2B without breaking architecture.
 */
interface HubRecommendationPolicy {
    fun recommendHubs(request: HubRecommendationRequest): List<HubItem>
}

/**
 * Basic implementation: filters open hubs and sorts by available remaining capacity.
 */
class BasicHubRecommendationPolicy : HubRecommendationPolicy {
    override fun recommendHubs(request: HubRecommendationRequest): List<HubItem> {
        return request.availableHubs
            .filter { it.isOpen }
            .sortedByDescending { it.maxCapacity - it.currentPackagesCount }
    }
}
