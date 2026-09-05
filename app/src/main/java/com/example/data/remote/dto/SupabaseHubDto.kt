package com.example.data.remote.dto

import com.example.data.local.entities.HubEntity
import com.example.model.HubItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseHubDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("type") val type: String = "store",
    @SerialName("type_name") val typeName: String = "مرکز توزیع محلی",
    @SerialName("manager_name") val managerName: String,
    @SerialName("phone") val phone: String,
    @SerialName("license_number") val licenseNumber: String = "",
    @SerialName("address") val address: String = "تهران",
    @SerialName("rating") val rating: Float = 4.8f,
    @SerialName("review_count") val reviewCount: Int = 120,
    @SerialName("working_hours") val workingHours: String = "۰۸:۰۰ - ۲۲:۰۰",
    @SerialName("is_open") val isOpen: Boolean = true,
    @SerialName("current_packages_count") val currentPackagesCount: Int = 0,
    @SerialName("max_capacity") val maxCapacity: Int = 50,
    @SerialName("lat") val lat: Double = 35.7924,
    @SerialName("lng") val lng: Double = 51.3789,
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("updated_at") val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): HubItem = HubItem(
        id = id,
        name = name,
        type = type,
        typeName = typeName,
        managerName = managerName,
        phone = phone,
        licenseNumber = licenseNumber,
        address = address,
        rating = rating,
        reviewCount = reviewCount,
        workingHours = workingHours,
        isOpen = isOpen,
        currentPackagesCount = currentPackagesCount,
        maxCapacity = maxCapacity,
        lat = lat,
        lng = lng
    )

    fun toEntity(): HubEntity = HubEntity(
        id = id,
        name = name,
        type = type,
        typeName = typeName,
        managerName = managerName,
        phone = phone,
        licenseNumber = licenseNumber,
        address = address,
        rating = rating,
        reviewCount = reviewCount,
        workingHours = workingHours,
        isOpen = isOpen,
        currentPackagesCount = currentPackagesCount,
        maxCapacity = maxCapacity,
        lat = lat,
        lng = lng,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(hub: HubItem, createdAt: Long = System.currentTimeMillis(), updatedAt: Long = System.currentTimeMillis()): SupabaseHubDto = SupabaseHubDto(
            id = hub.id,
            name = hub.name,
            type = hub.type,
            typeName = hub.typeName,
            managerName = hub.managerName,
            phone = hub.phone,
            licenseNumber = hub.licenseNumber,
            address = hub.address,
            rating = hub.rating,
            reviewCount = hub.reviewCount,
            workingHours = hub.workingHours,
            isOpen = hub.isOpen,
            currentPackagesCount = hub.currentPackagesCount,
            maxCapacity = hub.maxCapacity,
            lat = hub.lat,
            lng = hub.lng,
            createdAt = createdAt,
            updatedAt = updatedAt
        )

        fun fromEntity(entity: HubEntity): SupabaseHubDto = SupabaseHubDto(
            id = entity.id,
            name = entity.name,
            type = entity.type,
            typeName = entity.typeName,
            managerName = entity.managerName,
            phone = entity.phone,
            licenseNumber = entity.licenseNumber,
            address = entity.address,
            rating = entity.rating,
            reviewCount = entity.reviewCount,
            workingHours = entity.workingHours,
            isOpen = entity.isOpen,
            currentPackagesCount = entity.currentPackagesCount,
            maxCapacity = entity.maxCapacity,
            lat = entity.lat,
            lng = entity.lng,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
}
