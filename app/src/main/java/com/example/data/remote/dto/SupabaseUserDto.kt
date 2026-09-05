package com.example.data.remote.dto

import com.example.data.local.entities.UserEntity
import com.example.data.model.AccountApprovalStatus
import com.example.data.model.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseUserDto(
    @SerialName("id") val id: String,
    @SerialName("username") val username: String = "",
    @SerialName("password_hash") val passwordHash: String = "",
    @SerialName("full_name") val fullName: String,
    @SerialName("phone") val phone: String,
    @SerialName("national_id") val nationalId: String = "",
    @SerialName("email") val email: String = "",
    @SerialName("postal_code") val postalCode: String = "",
    @SerialName("address") val address: String = "",
    @SerialName("role") val role: String = "CUSTOMER",
    @SerialName("approval_status") val approvalStatus: String = "PENDING",
    @SerialName("store_name") val storeName: String = "",
    @SerialName("guild_type") val guildType: String = "",
    @SerialName("bank_card_number") val bankCardNumber: String = "",
    @SerialName("vehicle_type") val vehicleType: String = "",
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("updated_at") val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): User = User(
        id = id,
        username = username,
        passwordHash = passwordHash,
        fullName = fullName,
        phone = phone,
        nationalId = nationalId,
        email = email,
        postalCode = postalCode,
        address = address,
        role = role,
        approvalStatus = try {
            AccountApprovalStatus.valueOf(approvalStatus)
        } catch (_: Exception) {
            AccountApprovalStatus.PENDING
        },
        storeName = storeName,
        guildType = guildType,
        bankCardNumber = bankCardNumber,
        vehicleType = vehicleType,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun toEntity(): UserEntity = UserEntity(
        id = id,
        username = username,
        passwordHash = passwordHash,
        fullName = fullName,
        phone = phone,
        nationalId = nationalId,
        email = email,
        postalCode = postalCode,
        address = address,
        role = role,
        approvalStatus = approvalStatus,
        storeName = storeName,
        guildType = guildType,
        bankCardNumber = bankCardNumber,
        vehicleType = vehicleType,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(user: User): SupabaseUserDto = SupabaseUserDto(
            id = user.id,
            username = user.username,
            passwordHash = user.passwordHash,
            fullName = user.fullName,
            phone = user.phone,
            nationalId = user.nationalId,
            email = user.email,
            postalCode = user.postalCode,
            address = user.address,
            role = user.role,
            approvalStatus = user.approvalStatus.name,
            storeName = user.storeName,
            guildType = user.guildType,
            bankCardNumber = user.bankCardNumber,
            vehicleType = user.vehicleType,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt
        )

        fun fromEntity(entity: UserEntity): SupabaseUserDto = SupabaseUserDto(
            id = entity.id,
            username = entity.username,
            passwordHash = entity.passwordHash,
            fullName = entity.fullName,
            phone = entity.phone,
            nationalId = entity.nationalId,
            email = entity.email,
            postalCode = entity.postalCode,
            address = entity.address,
            role = entity.role,
            approvalStatus = entity.approvalStatus,
            storeName = entity.storeName,
            guildType = entity.guildType,
            bankCardNumber = entity.bankCardNumber,
            vehicleType = entity.vehicleType,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
}
