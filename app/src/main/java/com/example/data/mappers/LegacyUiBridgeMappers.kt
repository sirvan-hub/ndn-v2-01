package com.example.data.mappers

import com.example.data.model.Parcel
import com.example.data.model.ParcelSize
import com.example.data.model.ParcelStatus
import com.example.data.model.RegistrationSource
import com.example.data.model.User
import com.example.model.PackageHistoryEntry
import com.example.model.PackageItem
import com.example.model.PackageSize as UiPackageSize
import com.example.model.PackageStatus as UiPackageStatus
import com.example.model.RegistrationInitiator
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.model.AccountApprovalStatus as UiApprovalStatus
import com.example.data.model.AccountApprovalStatus as DomainApprovalStatus

fun PackageItem.toDomainParcel(): Parcel {
    val domainStatus = when (status) {
        UiPackageStatus.PENDING_CUSTOMER_APPROVAL -> ParcelStatus.ELIGIBLE_FOR_HUB
        UiPackageStatus.PENDING_COURIER_VERIFICATION -> ParcelStatus.OUT_FOR_DELIVERY
        UiPackageStatus.IN_TRANSIT -> ParcelStatus.HANDOVER_IN_PROGRESS
        UiPackageStatus.AT_HUB -> ParcelStatus.STORED_AT_HUB
        UiPackageStatus.DELIVERED -> ParcelStatus.DELIVERED_TO_CUSTOMER
        UiPackageStatus.REJECTED -> ParcelStatus.REJECTED
    }

    val domainSize = when (size) {
        UiPackageSize.SMALL -> ParcelSize.SMALL
        UiPackageSize.MEDIUM -> ParcelSize.MEDIUM
        UiPackageSize.LARGE -> ParcelSize.LARGE
    }

    val source = if (registrationInitiator == RegistrationInitiator.CUSTOMER_INITIATED) {
        RegistrationSource.CUSTOMER_REQUEST
    } else {
        RegistrationSource.FAILED_HOME_DELIVERY
    }

    return Parcel(
        id = id,
        trackingNumber = trackingCode,
        senderName = sender,
        recipientName = receiver,
        recipientPhone = receiverPhone,
        recipientPostalCode = "1998765432",
        recipientAddress = "تهران",
        status = domainStatus,
        size = domainSize,
        assignedHubId = hubId,
        assignedHubName = hubName,
        assignedCourierId = courierId,
        assignedCourierName = courierName,
        registrationSource = source,
        baseFee = baseFee,
        isSettled = isPaid,
        handoverOtp = if (status == UiPackageStatus.IN_TRANSIT) "1234" else null,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}

fun Parcel.toLegacyPackageItem(): PackageItem {
    val legacyStatus = when (status) {
        ParcelStatus.OUT_FOR_DELIVERY -> UiPackageStatus.PENDING_COURIER_VERIFICATION
        ParcelStatus.DELIVERY_ATTEMPTED -> UiPackageStatus.PENDING_COURIER_VERIFICATION
        ParcelStatus.ELIGIBLE_FOR_HUB -> UiPackageStatus.PENDING_CUSTOMER_APPROVAL
        ParcelStatus.HUB_SELECTED -> UiPackageStatus.PENDING_CUSTOMER_APPROVAL
        ParcelStatus.HANDOVER_IN_PROGRESS -> UiPackageStatus.IN_TRANSIT
        ParcelStatus.AWAITING_HUB_CONFIRMATION -> UiPackageStatus.IN_TRANSIT
        ParcelStatus.TRANSFERRED_TO_HUB -> UiPackageStatus.AT_HUB
        ParcelStatus.STORED_AT_HUB -> UiPackageStatus.AT_HUB
        ParcelStatus.DELIVERED_TO_CUSTOMER -> UiPackageStatus.DELIVERED
        ParcelStatus.RETURNED_TO_SENDER -> UiPackageStatus.REJECTED
        ParcelStatus.REJECTED -> UiPackageStatus.REJECTED
    }

    val legacySize = when (size) {
        ParcelSize.SMALL -> UiPackageSize.SMALL
        ParcelSize.MEDIUM -> UiPackageSize.MEDIUM
        ParcelSize.LARGE -> UiPackageSize.LARGE
        ParcelSize.HEAVY -> UiPackageSize.LARGE
    }

    val initiator = if (registrationSource == RegistrationSource.CUSTOMER_REQUEST) {
        RegistrationInitiator.CUSTOMER_INITIATED
    } else {
        RegistrationInitiator.COURIER_INITIATED
    }

    return PackageItem(
        id = id,
        trackingCode = trackingNumber,
        title = "مرسوله ${size.labelFa}",
        sender = senderName,
        receiver = recipientName,
        receiverPhone = recipientPhone,
        hubId = assignedHubId ?: "",
        hubName = assignedHubName ?: "هاب منتخب",
        hubAddress = "تهران، منطقه پستی",
        status = legacyStatus,
        statusText = status.titleFa,
        dimensions = "30x20x15 cm",
        weight = "${size.defaultBaseFee / 10000} kg",
        size = legacySize,
        baseFee = baseFee,
        totalFee = baseFee,
        isPaid = isSettled,
        courierId = assignedCourierId ?: "",
        courierName = assignedCourierName ?: "سفیر توزیع",
        courierPhone = "09120000000",
        registrationInitiator = initiator,
        slaHoursRemaining = 48,
        history = listOf(
            PackageHistoryEntry(
                status = status.titleFa,
                timestamp = "ثبت شده در سیستم",
                description = "وضعیت: ${status.name}"
            )
        )
    )
}

fun UserProfile.toDomainUser(): User {
    val domainRole = when (role) {
        UserRole.CUSTOMER -> "CUSTOMER"
        UserRole.COURIER -> "COURIER"
        UserRole.HUB_MANAGER -> "HUB_MANAGER"
        UserRole.ADMIN -> "ADMIN"
    }

    val domainApproval = when (approvalStatus) {
        UiApprovalStatus.APPROVED -> DomainApprovalStatus.APPROVED
        UiApprovalStatus.PENDING -> DomainApprovalStatus.PENDING
        UiApprovalStatus.REJECTED -> DomainApprovalStatus.REJECTED
    }

    return User(
        id = id,
        username = username,
        passwordHash = if (password.isNotBlank()) com.example.util.IdentityNormalizer.hashPassword(password) else "",
        fullName = fullName,
        phone = phone,
        nationalId = nationalId,
        email = email,
        postalCode = postalCode,
        address = address,
        role = domainRole,
        approvalStatus = domainApproval,
        storeName = storeName,
        guildType = guildType,
        bankCardNumber = bankCardNumber,
        vehicleType = "",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}

fun User.toLegacyUserProfile(): UserProfile {
    val legacyRole = when (role) {
        "COURIER" -> UserRole.COURIER
        "HUB_MANAGER" -> UserRole.HUB_MANAGER
        "ADMIN", "SYSTEM_ADMIN" -> UserRole.ADMIN
        else -> UserRole.CUSTOMER
    }

    val legacyApproval = when (approvalStatus) {
        DomainApprovalStatus.APPROVED -> UiApprovalStatus.APPROVED
        DomainApprovalStatus.PENDING -> UiApprovalStatus.PENDING
        DomainApprovalStatus.REJECTED -> UiApprovalStatus.REJECTED
    }

    return UserProfile(
        id = id,
        role = legacyRole,
        fullName = fullName,
        username = username,
        phone = phone,
        nationalId = nationalId,
        email = email,
        postalCode = postalCode,
        address = address,
        bankCardNumber = bankCardNumber,
        storeName = storeName,
        guildType = guildType,
        approvalStatus = legacyApproval
    )
}
