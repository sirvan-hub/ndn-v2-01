package com.example.data.local.mappers

import com.example.data.local.entities.*
import com.example.data.model.*
import com.example.model.HubItem

fun ParcelEntity.toDomain(): Parcel {
    return Parcel(
        id = id,
        trackingNumber = trackingNumber,
        senderName = senderName,
        recipientName = recipientName,
        recipientPhone = recipientPhone,
        recipientPostalCode = recipientPostalCode,
        recipientAddress = recipientAddress,
        status = try { ParcelStatus.valueOf(status) } catch (e: Exception) { ParcelStatus.OUT_FOR_DELIVERY },
        size = try { ParcelSize.valueOf(size) } catch (e: Exception) { ParcelSize.MEDIUM },
        assignedHubId = hubId,
        assignedHubName = hubName,
        assignedCourierId = courierId,
        assignedCourierName = courierName,
        registrationSource = try { RegistrationSource.valueOf(registrationSource) } catch (e: Exception) { RegistrationSource.CUSTOMER_REQUEST },
        baseFee = baseFee,
        isSettled = isSettled,
        handoverOtp = handoverOtp,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Parcel.toEntity(): ParcelEntity {
    return ParcelEntity(
        id = id,
        trackingNumber = trackingNumber,
        senderName = senderName,
        recipientName = recipientName,
        recipientPhone = recipientPhone,
        recipientPostalCode = recipientPostalCode,
        recipientAddress = recipientAddress,
        status = status.name,
        size = size.name,
        hubId = assignedHubId,
        hubName = assignedHubName,
        courierId = assignedCourierId,
        courierName = assignedCourierName,
        registrationSource = registrationSource.name,
        baseFee = baseFee,
        isSettled = isSettled,
        handoverOtp = handoverOtp,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun UserEntity.toDomain(): User {
    return User(
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
        approvalStatus = try { AccountApprovalStatus.valueOf(approvalStatus) } catch (e: Exception) { AccountApprovalStatus.PENDING },
        storeName = storeName,
        guildType = guildType,
        bankCardNumber = bankCardNumber,
        vehicleType = vehicleType,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun User.toEntity(): UserEntity {
    return UserEntity(
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
        approvalStatus = approvalStatus.name,
        storeName = storeName,
        guildType = guildType,
        bankCardNumber = bankCardNumber,
        vehicleType = vehicleType,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun AuditLogEntity.toDomain(): AuditLog {
    return AuditLog(
        id = id,
        eventType = eventType,
        actorId = actorId,
        actorRole = actorRole,
        entityId = entityId,
        oldState = oldState,
        newState = newState,
        transactionId = transactionId,
        timestamp = timestamp,
        metadataJson = metadataJson
    )
}

fun AuditLog.toEntity(): AuditLogEntity {
    return AuditLogEntity(
        id = id,
        eventType = eventType,
        actorId = actorId,
        actorRole = actorRole,
        entityId = entityId,
        oldState = oldState,
        newState = newState,
        transactionId = transactionId,
        timestamp = timestamp,
        metadataJson = metadataJson
    )
}

fun RegistrationTransactionEntity.toDomain(): RegistrationTransaction {
    return RegistrationTransaction(
        transactionId = transactionId,
        parcelId = parcelId,
        trackingNumber = trackingNumber,
        courierId = courierId,
        hubId = hubId,
        registrationSource = try { RegistrationSource.valueOf(registrationSource) } catch (e: Exception) { RegistrationSource.CUSTOMER_REQUEST },
        timestamp = timestamp,
        clientGeneratedId = clientGeneratedId
    )
}

fun RegistrationTransaction.toEntity(): RegistrationTransactionEntity {
    return RegistrationTransactionEntity(
        transactionId = transactionId,
        parcelId = parcelId,
        trackingNumber = trackingNumber,
        courierId = courierId,
        hubId = hubId,
        registrationSource = registrationSource.name,
        timestamp = timestamp,
        clientGeneratedId = clientGeneratedId
    )
}

fun SyncQueueEntity.toDomain(): SyncQueueItem {
    return SyncQueueItem(
        id = id,
        entityType = entityType,
        entityId = entityId,
        action = action,
        payloadJson = payloadJson,
        createdAt = createdAt,
        isSynced = isSynced,
        syncedAt = syncedAt,
        retryCount = retryCount
    )
}

fun SyncQueueItem.toEntity(): SyncQueueEntity {
    return SyncQueueEntity(
        id = id,
        entityType = entityType,
        entityId = entityId,
        action = action,
        payloadJson = payloadJson,
        createdAt = createdAt,
        isSynced = isSynced,
        syncedAt = syncedAt,
        retryCount = retryCount
    )
}

fun SettlementTariffVersionEntity.toDomain(): SettlementTariffVersion {
    return SettlementTariffVersion(
        id = id,
        versionCode = versionCode,
        versionName = versionName,
        modelType = modelType,
        tier1HoursThreshold = tier1HoursThreshold,
        tier1RatePercentage = tier1RatePercentage,
        tier2HoursThreshold = tier2HoursThreshold,
        tier2RatePercentage = tier2RatePercentage,
        additional24hRatePercentage = additional24hRatePercentage,
        maxLifecycleHours = maxLifecycleHours,
        courierSharePercentage = courierSharePercentage,
        hubSharePercentage = hubSharePercentage,
        networkSharePercentage = networkSharePercentage,
        baseFee = baseFee,
        effectiveFrom = effectiveFrom,
        isActive = isActive
    )
}

fun SettlementTariffVersion.toEntity(): SettlementTariffVersionEntity {
    return SettlementTariffVersionEntity(
        id = id,
        versionCode = versionCode,
        versionName = versionName,
        modelType = modelType,
        tier1HoursThreshold = tier1HoursThreshold,
        tier1RatePercentage = tier1RatePercentage,
        tier2HoursThreshold = tier2HoursThreshold,
        tier2RatePercentage = tier2RatePercentage,
        additional24hRatePercentage = additional24hRatePercentage,
        maxLifecycleHours = maxLifecycleHours,
        courierSharePercentage = courierSharePercentage,
        hubSharePercentage = hubSharePercentage,
        networkSharePercentage = networkSharePercentage,
        baseFee = baseFee,
        effectiveFrom = effectiveFrom,
        isActive = isActive
    )
}

fun CourierSettlementSnapshotEntity.toDomain(): CourierSettlementSnapshot {
    return CourierSettlementSnapshot(
        snapshotId = snapshotId,
        courierId = courierId,
        hubId = hubId,
        periodStartDate = periodStartDate,
        periodEndDate = periodEndDate,
        settlementPeriod = settlementPeriod,
        totalParcelsTransferred = totalParcelsTransferred,
        printedPostalFee = printedPostalFee,
        servicePricingRuleVersion = servicePricingRuleVersion,
        deliveryDurationHours = deliveryDurationHours,
        calculatedServiceFee = calculatedServiceFee,
        courierSharePercentage = courierSharePercentage,
        hubSharePercentage = hubSharePercentage,
        networkSharePercentage = networkSharePercentage,
        courierShareAmount = courierShareAmount,
        hubShareAmount = hubShareAmount,
        networkShareAmount = networkShareAmount,
        confirmedAmount = confirmedAmount,
        pendingAmount = pendingAmount,
        tariffVersionId = tariffVersionId,
        snapshotCreatedAt = snapshotCreatedAt,
        isPaid = isPaid,
        idempotencyKey = idempotencyKey
    )
}

fun CourierSettlementSnapshot.toEntity(): CourierSettlementSnapshotEntity {
    return CourierSettlementSnapshotEntity(
        snapshotId = snapshotId,
        courierId = courierId,
        hubId = hubId,
        periodStartDate = periodStartDate,
        periodEndDate = periodEndDate,
        settlementPeriod = settlementPeriod,
        totalParcelsTransferred = totalParcelsTransferred,
        printedPostalFee = printedPostalFee,
        servicePricingRuleVersion = servicePricingRuleVersion,
        deliveryDurationHours = deliveryDurationHours,
        calculatedServiceFee = calculatedServiceFee,
        courierSharePercentage = courierSharePercentage,
        hubSharePercentage = hubSharePercentage,
        networkSharePercentage = networkSharePercentage,
        courierShareAmount = courierShareAmount,
        hubShareAmount = hubShareAmount,
        networkShareAmount = networkShareAmount,
        confirmedAmount = confirmedAmount,
        pendingAmount = pendingAmount,
        tariffVersionId = tariffVersionId,
        snapshotCreatedAt = snapshotCreatedAt,
        isPaid = isPaid,
        idempotencyKey = idempotencyKey
    )
}

fun MobileChangeRequestEntity.toDomain(): MobileChangeRequest {
    return MobileChangeRequest(
        id = id,
        userId = userId,
        userFullName = userFullName,
        currentPhone = currentPhone,
        requestedPhone = requestedPhone,
        nationalId = nationalId,
        status = try { MobileChangeStatus.valueOf(status) } catch (e: Exception) { MobileChangeStatus.PENDING_ADMIN_APPROVAL },
        requestedAt = requestedAt,
        reviewedAt = reviewedAt,
        reviewedBy = reviewedBy,
        reviewNotes = reviewNotes
    )
}

fun MobileChangeRequest.toEntity(): MobileChangeRequestEntity {
    return MobileChangeRequestEntity(
        id = id,
        userId = userId,
        userFullName = userFullName,
        currentPhone = currentPhone,
        requestedPhone = requestedPhone,
        nationalId = nationalId,
        status = status.name,
        requestedAt = requestedAt,
        reviewedAt = reviewedAt,
        reviewedBy = reviewedBy,
        reviewNotes = reviewNotes
    )
}

fun HubEntity.toDomain(): HubItem {
    return HubItem(
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
}

fun HubItem.toEntity(createdAt: Long = System.currentTimeMillis(), updatedAt: Long = System.currentTimeMillis()): HubEntity {
    return HubEntity(
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
}

