package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.database.PudoDatabase
import com.example.data.local.entities.AuditLogEntity
import com.example.data.local.entities.SyncQueueEntity
import com.example.data.local.mappers.*
import com.example.data.model.*
import com.example.domain.statemachine.ParcelStateMachine
import com.example.model.HubItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class PudoRepositoryImpl(
    private val database: PudoDatabase
) : PudoRepository {

    private val parcelDao = database.parcelDao()
    private val hubDao = database.hubDao()
    private val userDao = database.userDao()

    private suspend fun requireAdminActor(actorId: String): com.example.data.local.entities.UserEntity {
        val actor = userDao.getUserById(actorId)
        require(actor != null && (actor.role == "ADMIN" || actor.role == "SYSTEM_ADMIN")) {
            "Unauthorized: actor is not a verified admin."
        }
        return actor
    }
    private val auditLogDao = database.auditLogDao()
    private val registrationTransactionDao = database.registrationTransactionDao()
    private val syncQueueDao = database.syncQueueDao()
    private val tariffDao = database.tariffDao()
    private val settlementDao = database.courierSettlementSnapshotDao()

    // ==========================================
    // Hub Operations & Room Persistence
    // ==========================================
    override fun getAllHubs(): Flow<List<HubItem>> {
        return hubDao.getAllHubs().map { list -> list.map { it.toDomain() } }
    }

    override fun getHubById(id: String): Flow<HubItem?> {
        return hubDao.getHubById(id).map { it?.toDomain() }
    }

    override suspend fun insertHub(hub: HubItem, actorId: String): Result<HubItem> {
        return try {
            val actor = requireAdminActor(actorId)
            val now = System.currentTimeMillis()
            val entity = hub.toEntity(createdAt = now, updatedAt = now)
            val auditLog = AuditLogEntity(
                id = "audit-${UUID.randomUUID()}",
                eventType = "HUB_CREATED",
                actorId = actor.id,
                actorRole = actor.role,
                entityId = hub.id,
                oldState = "NONE",
                newState = "ACTIVE",
                transactionId = null,
                metadataJson = "{\"hubName\":\"${hub.name}\",\"manager\":\"${hub.managerName}\"}",
                timestamp = now
            )
            val syncItem = SyncQueueEntity(
                id = "sync-${UUID.randomUUID()}",
                entityType = "HUB",
                entityId = hub.id,
                action = "CREATE",
                payloadJson = "{\"id\":\"${hub.id}\",\"name\":\"${hub.name}\",\"isOpen\":${hub.isOpen},\"updatedAt\":$now}",
                createdAt = now
            )
            database.withTransaction {
                hubDao.insertHub(entity)
                auditLogDao.insertLog(auditLog)
                syncQueueDao.insertSyncItem(syncItem)
            }
            Result.success(hub)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateHub(hub: HubItem, actorId: String): Result<HubItem> {
        return try {
            val actor = requireAdminActor(actorId)
            val existing = hubDao.getHubByIdDirect(hub.id)
            val now = System.currentTimeMillis()
            val entity = hub.toEntity(createdAt = existing?.createdAt ?: now, updatedAt = now)
            val auditLog = AuditLogEntity(
                id = "audit-${UUID.randomUUID()}",
                eventType = "HUB_UPDATED",
                actorId = actor.id,
                actorRole = actor.role,
                entityId = hub.id,
                oldState = if (existing?.isOpen == true) "OPEN" else "CLOSED",
                newState = if (hub.isOpen) "OPEN" else "CLOSED",
                transactionId = null,
                metadataJson = "{\"hubName\":\"${hub.name}\",\"isOpen\":${hub.isOpen}}",
                timestamp = now
            )
            val syncItem = SyncQueueEntity(
                id = "sync-${UUID.randomUUID()}",
                entityType = "HUB",
                entityId = hub.id,
                action = "UPDATE",
                payloadJson = "{\"id\":\"${hub.id}\",\"name\":\"${hub.name}\",\"isOpen\":${hub.isOpen},\"updatedAt\":$now}",
                createdAt = now
            )
            database.withTransaction {
                hubDao.updateHub(entity)
                auditLogDao.insertLog(auditLog)
                syncQueueDao.insertSyncItem(syncItem)
            }
            Result.success(hub)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteHub(id: String, actorId: String): Result<Unit> {
        return try {
            val actor = requireAdminActor(actorId)
            val now = System.currentTimeMillis()
            val auditLog = AuditLogEntity(
                id = "audit-${UUID.randomUUID()}",
                eventType = "HUB_DELETED",
                actorId = actor.id,
                actorRole = actor.role,
                entityId = id,
                oldState = "ACTIVE",
                newState = "DELETED",
                transactionId = null,
                metadataJson = "{\"hubId\":\"$id\"}",
                timestamp = now
            )
            val syncItem = SyncQueueEntity(
                id = "sync-${UUID.randomUUID()}",
                entityType = "HUB",
                entityId = id,
                action = "DELETE",
                payloadJson = "{\"id\":\"$id\"}",
                createdAt = now
            )
            database.withTransaction {
                hubDao.deleteHubById(id)
                auditLogDao.insertLog(auditLog)
                syncQueueDao.insertSyncItem(syncItem)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun seedHubsIfEmpty(seedHubs: List<HubItem>) {
        if (seedHubs.isEmpty()) return
        val count = hubDao.getHubCount()
        if (count == 0) {
            val entities = seedHubs.map { it.toEntity() }
            hubDao.insertHubs(entities)
        }
    }

    override fun getAllParcels(): Flow<List<Parcel>> {
        return parcelDao.getAllParcels().map { list -> list.map { it.toDomain() } }
    }

    override fun getParcelById(id: String): Flow<Parcel?> {
        return parcelDao.getParcelById(id).map { it?.toDomain() }
    }

    override fun getCourierParcels(courierId: String): Flow<List<Parcel>> {
        return parcelDao.getParcelsByCourier(courierId).map { list -> list.map { it.toDomain() } }
    }

    override fun getHubParcels(hubId: String): Flow<List<Parcel>> {
        return parcelDao.getParcelsByHub(hubId).map { list -> list.map { it.toDomain() } }
    }

    override fun getParcelsByStatus(status: ParcelStatus): Flow<List<Parcel>> {
        return parcelDao.getParcelsByStatus(status.name).map { list -> list.map { it.toDomain() } }
    }

    override fun getParcelsByRecipientPhone(phone: String): Flow<List<Parcel>> {
        return parcelDao.getParcelsByRecipientPhone(phone).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun registerPudoParcel(
        parcel: Parcel,
        transaction: RegistrationTransaction,
        actorId: String,
        actorRole: String
    ): Result<Parcel> {
        return try {
            // 1. Idempotency Check: if transactionId already registered, return existing parcel without re-inserting
            val existingTx = registrationTransactionDao.getTransactionById(transaction.transactionId)
            if (existingTx != null) {
                val existingParcel = parcelDao.getParcelByIdDirect(existingTx.parcelId)
                if (existingParcel != null) {
                    return Result.success(existingParcel.toDomain())
                }
            }

            // 2. Validate Registration Source
            require(ParcelStateMachine.canRegisterFromRoute(transaction.registrationSource)) {
                "Invalid registration source for route registration: ${transaction.registrationSource}"
            }

            // 3. Validate state transition
            val initialStatus = ParcelStatus.OUT_FOR_DELIVERY
            val targetStatus = parcel.status
            if (initialStatus != targetStatus) {
                ParcelStateMachine.validateTransition(initialStatus, targetStatus)
            }

            val auditLog = AuditLog(
                id = "audit-${UUID.randomUUID()}",
                eventType = "PARCEL_PUDO_REGISTERED",
                actorId = actorId,
                actorRole = actorRole,
                entityId = parcel.id,
                oldState = initialStatus.name,
                newState = targetStatus.name,
                transactionId = transaction.transactionId,
                metadataJson = "{\"tracking\":\"${parcel.trackingNumber}\",\"source\":\"${transaction.registrationSource}\"}"
            )

            val syncItem = SyncQueueItem(
                id = "sync-${UUID.randomUUID()}",
                entityType = "PARCEL",
                entityId = parcel.id,
                action = "CREATE",
                payloadJson = "{\"id\":\"${parcel.id}\",\"tracking\":\"${parcel.trackingNumber}\"}"
            )

            // 4. Atomic Execution across Room entities
            database.withTransaction {
                parcelDao.insertParcel(parcel.toEntity())
                registrationTransactionDao.insertTransaction(transaction.toEntity())
                auditLogDao.insertLog(auditLog.toEntity())
                syncQueueDao.insertSyncItem(syncItem.toEntity())
            }

            Result.success(parcel)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun registerFailedDelivery(
        parcel: Parcel,
        transaction: RegistrationTransaction,
        actorId: String,
        actorRole: String
    ): Result<Parcel> {
        val updatedParcel = parcel.copy(
            status = ParcelStatus.DELIVERY_ATTEMPTED,
            registrationSource = RegistrationSource.FAILED_HOME_DELIVERY,
            updatedAt = System.currentTimeMillis()
        )
        return registerPudoParcel(updatedParcel, transaction, actorId, actorRole)
    }

    override suspend fun assignHub(
        parcelId: String,
        hubId: String,
        hubName: String,
        actorId: String,
        actorRole: String
    ): Result<Parcel> {
        return try {
            val currentEntity = parcelDao.getParcelByIdDirect(parcelId)
                ?: parcelDao.getParcelByTrackingNumber(parcelId)
                ?: return Result.failure(IllegalArgumentException("Parcel with id or tracking $parcelId not found"))

            val currentDomain = currentEntity.toDomain()
            val targetStatus = ParcelStatus.HUB_SELECTED
            ParcelStateMachine.validateTransition(currentDomain.status, targetStatus)

            val updatedDomain = currentDomain.copy(
                assignedHubId = hubId,
                assignedHubName = hubName,
                status = targetStatus,
                updatedAt = System.currentTimeMillis()
            )

            val auditLog = AuditLog(
                id = "audit-${UUID.randomUUID()}",
                eventType = "PARCEL_HUB_ASSIGNED",
                actorId = actorId,
                actorRole = actorRole,
                entityId = currentEntity.id,
                oldState = currentDomain.status.name,
                newState = targetStatus.name,
                transactionId = null,
                metadataJson = "{\"hubId\":\"$hubId\",\"hubName\":\"$hubName\"}"
            )

            val syncItem = SyncQueueItem(
                id = "sync-${UUID.randomUUID()}",
                entityType = "PARCEL",
                entityId = currentEntity.id,
                action = "UPDATE",
                payloadJson = "{\"status\":\"${targetStatus.name}\",\"hubId\":\"$hubId\"}"
            )

            database.withTransaction {
                parcelDao.updateParcel(updatedDomain.toEntity())
                auditLogDao.insertLog(auditLog.toEntity())
                syncQueueDao.insertSyncItem(syncItem.toEntity())
            }

            Result.success(updatedDomain)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun startHandover(
        parcelId: String,
        otp: String,
        actorId: String,
        actorRole: String
    ): Result<Parcel> {
        return try {
            val currentEntity = parcelDao.getParcelByIdDirect(parcelId)
                ?: parcelDao.getParcelByTrackingNumber(parcelId)
                ?: return Result.failure(IllegalArgumentException("Parcel with id or tracking $parcelId not found"))

            val currentDomain = currentEntity.toDomain()
            val targetStatus = ParcelStatus.HANDOVER_IN_PROGRESS
            ParcelStateMachine.validateTransition(currentDomain.status, targetStatus)

            val updatedDomain = currentDomain.copy(
                status = targetStatus,
                handoverOtp = otp,
                updatedAt = System.currentTimeMillis()
            )

            val auditLog = AuditLog(
                id = "audit-${UUID.randomUUID()}",
                eventType = "HANDOVER_STARTED",
                actorId = actorId,
                actorRole = actorRole,
                entityId = currentEntity.id,
                oldState = currentDomain.status.name,
                newState = targetStatus.name,
                transactionId = null
            )

            val syncItem = SyncQueueItem(
                id = "sync-${UUID.randomUUID()}",
                entityType = "PARCEL",
                entityId = currentEntity.id,
                action = "UPDATE"
            )

            database.withTransaction {
                parcelDao.updateParcel(updatedDomain.toEntity())
                auditLogDao.insertLog(auditLog.toEntity())
                syncQueueDao.insertSyncItem(syncItem.toEntity())
            }

            Result.success(updatedDomain)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun confirmHubHandover(
        parcelId: String,
        inputOtp: String?,
        actorId: String,
        actorRole: String
    ): Result<Parcel> {
        return try {
            val currentEntity = parcelDao.getParcelByIdDirect(parcelId)
                ?: parcelDao.getParcelByTrackingNumber(parcelId)
                ?: return Result.failure(IllegalArgumentException("Parcel with id or tracking $parcelId not found"))

            val currentDomain = currentEntity.toDomain()

            // If OTP verification required
            if (!currentDomain.handoverOtp.isNullOrBlank() && inputOtp != null) {
                require(currentDomain.handoverOtp == inputOtp.trim()) {
                    "Handover OTP mismatch for parcel ${currentEntity.id}"
                }
            }

            // We advance to TRANSFERRED_TO_HUB directly when Hub confirms
            val finalTargetStatus = ParcelStatus.TRANSFERRED_TO_HUB
            ParcelStateMachine.validateTransition(currentDomain.status, finalTargetStatus)

            val updatedDomain = currentDomain.copy(
                status = finalTargetStatus,
                isSettled = false,
                updatedAt = System.currentTimeMillis()
            )

            val auditLog = AuditLog(
                id = "audit-${UUID.randomUUID()}",
                eventType = "HANDOVER_CONFIRMED_AT_HUB",
                actorId = actorId,
                actorRole = actorRole,
                entityId = currentEntity.id,
                oldState = currentDomain.status.name,
                newState = finalTargetStatus.name,
                transactionId = null,
                metadataJson = "{\"settlementEligible\":${ParcelStateMachine.isSettlementEligible(finalTargetStatus)}}"
            )

            val syncItem = SyncQueueItem(
                id = "sync-${UUID.randomUUID()}",
                entityType = "PARCEL",
                entityId = currentEntity.id,
                action = "UPDATE"
            )

            database.withTransaction {
                parcelDao.updateParcel(updatedDomain.toEntity())
                auditLogDao.insertLog(auditLog.toEntity())
                syncQueueDao.insertSyncItem(syncItem.toEntity())
            }

            Result.success(updatedDomain)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun receivePackageAtHub(
        parcelId: String,
        actorId: String,
        actorRole: String
    ): Result<Parcel> {
        return transitionParcelStatus(
            parcelId = parcelId,
            targetStatus = ParcelStatus.STORED_AT_HUB,
            actorId = actorId,
            actorRole = actorRole
        )
    }

    override suspend fun deliverToCustomer(
        parcelId: String,
        actorId: String,
        actorRole: String
    ): Result<Parcel> {
        return transitionParcelStatus(
            parcelId = parcelId,
            targetStatus = ParcelStatus.DELIVERED_TO_CUSTOMER,
            actorId = actorId,
            actorRole = actorRole
        )
    }

    override suspend fun payPackageFee(
        parcelId: String,
        actorId: String,
        actorRole: String
    ): Result<Parcel> {
        return try {
            val currentEntity = parcelDao.getParcelByIdDirect(parcelId)
                ?: parcelDao.getParcelByTrackingNumber(parcelId)
                ?: return Result.failure(IllegalArgumentException("Parcel with id or tracking $parcelId not found"))

            val currentDomain = currentEntity.toDomain()
            val now = System.currentTimeMillis()
            val updatedDomain = currentDomain.copy(
                isSettled = true,
                updatedAt = now
            )

            val auditLog = AuditLogEntity(
                id = "audit-${UUID.randomUUID()}",
                eventType = "PARCEL_FEE_PAID",
                actorId = actorId,
                actorRole = actorRole,
                entityId = currentEntity.id,
                oldState = if (currentDomain.isSettled) "PAID" else "UNPAID",
                newState = "PAID",
                transactionId = null,
                metadataJson = "{\"baseFee\":${currentDomain.baseFee},\"trackingNumber\":\"${currentDomain.trackingNumber}\"}",
                timestamp = now
            )

            val syncItem = SyncQueueEntity(
                id = "sync-${UUID.randomUUID()}",
                entityType = "PARCEL",
                entityId = currentEntity.id,
                action = "UPDATE",
                payloadJson = "{\"id\":\"${currentEntity.id}\",\"isSettled\":true}",
                createdAt = now
            )

            database.withTransaction {
                parcelDao.updateParcel(updatedDomain.toEntity())
                auditLogDao.insertLog(auditLog)
                syncQueueDao.insertSyncItem(syncItem)
            }

            Result.success(updatedDomain)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun returnToSender(
        parcelId: String,
        reason: String,
        actorId: String,
        actorRole: String
    ): Result<Parcel> {
        return transitionParcelStatus(
            parcelId = parcelId,
            targetStatus = ParcelStatus.RETURNED_TO_SENDER,
            actorId = actorId,
            actorRole = actorRole,
            reason = reason
        )
    }

    override suspend fun transitionParcelStatus(
        parcelId: String,
        targetStatus: ParcelStatus,
        actorId: String,
        actorRole: String,
        reason: String?
    ): Result<Parcel> {
        return try {
            val currentEntity = parcelDao.getParcelByIdDirect(parcelId)
                ?: parcelDao.getParcelByTrackingNumber(parcelId)
                ?: return Result.failure(IllegalArgumentException("Parcel with id or tracking $parcelId not found"))

            val currentDomain = currentEntity.toDomain()
            ParcelStateMachine.validateTransition(currentDomain.status, targetStatus)

            val updatedDomain = currentDomain.copy(
                status = targetStatus,
                isSettled = if (targetStatus == ParcelStatus.DELIVERED_TO_CUSTOMER) true else currentDomain.isSettled,
                updatedAt = System.currentTimeMillis()
            )

            val auditLog = AuditLog(
                id = "audit-${UUID.randomUUID()}",
                eventType = when (targetStatus) {
                    ParcelStatus.STORED_AT_HUB -> "PARCEL_RECEIVED_AT_HUB"
                    ParcelStatus.DELIVERED_TO_CUSTOMER -> "PARCEL_DELIVERED_TO_CUSTOMER"
                    ParcelStatus.HANDOVER_IN_PROGRESS -> "PARCEL_WITH_COURIER"
                    ParcelStatus.RETURNED_TO_SENDER -> "PARCEL_RETURNED_TO_SENDER"
                    ParcelStatus.REJECTED -> "PARCEL_CANCELLED"
                    else -> "PARCEL_STATUS_TRANSITION"
                },
                actorId = actorId,
                actorRole = actorRole,
                entityId = currentEntity.id,
                oldState = currentDomain.status.name,
                newState = targetStatus.name,
                transactionId = null,
                metadataJson = if (reason != null) "{\"reason\":\"$reason\"}" else null
            )

            val syncItem = SyncQueueItem(
                id = "sync-${UUID.randomUUID()}",
                entityType = "PARCEL",
                entityId = currentEntity.id,
                action = "UPDATE"
            )

            database.withTransaction {
                parcelDao.updateParcel(updatedDomain.toEntity())
                auditLogDao.insertLog(auditLog.toEntity())
                syncQueueDao.insertSyncItem(syncItem.toEntity())
            }

            Result.success(updatedDomain)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getActiveTariff(): Flow<SettlementTariffVersion?> {
        return tariffDao.getActiveTariff().map { it?.toDomain() }
    }

    override fun getAllTariffs(): Flow<List<SettlementTariffVersion>> {
        return tariffDao.getAllTariffVersions().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun saveTariff(tariff: SettlementTariffVersion) {
        tariffDao.insertTariff(tariff.toEntity())
    }

    override suspend fun createSettlementSnapshot(snapshot: CourierSettlementSnapshot): Result<CourierSettlementSnapshot> {
        return try {
            // Check idempotency
            val existing = settlementDao.getSnapshotByIdempotencyKey(snapshot.idempotencyKey)
            if (existing != null) {
                return Result.success(existing.toDomain())
            }

            val auditLog = AuditLog(
                id = "audit-${UUID.randomUUID()}",
                eventType = "SETTLEMENT_SNAPSHOT_CREATED",
                actorId = snapshot.courierId,
                actorRole = "SYSTEM",
                entityId = snapshot.snapshotId,
                oldState = null,
                newState = "CREATED",
                transactionId = snapshot.snapshotId,
                metadataJson = "{\"confirmedAmount\":${snapshot.confirmedAmount},\"parcels\":${snapshot.totalParcelsTransferred}}"
            )

            database.withTransaction {
                settlementDao.insertSnapshot(snapshot.toEntity())
                auditLogDao.insertLog(auditLog.toEntity())
            }

            Result.success(snapshot)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCourierSettlementSnapshots(courierId: String): Flow<List<CourierSettlementSnapshot>> {
        return settlementDao.getSnapshotsByCourier(courierId).map { list -> list.map { it.toDomain() } }
    }

    override fun getAllAuditLogs(): Flow<List<AuditLog>> {
        return auditLogDao.getAllLogs().map { list -> list.map { it.toDomain() } }
    }

    override fun getAuditLogsForEntity(entityId: String): Flow<List<AuditLog>> {
        return auditLogDao.getLogsByEntityId(entityId).map { list -> list.map { it.toDomain() } }
    }

    override fun getPendingSyncQueueItems(): Flow<List<SyncQueueItem>> {
        return syncQueueDao.getPendingSyncItems().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun seedParcelsIfEmpty(seedParcels: List<Parcel>) {
        val existing = parcelDao.getAllParcelsDirect()
        if (existing.isEmpty() && seedParcels.isNotEmpty()) {
            parcelDao.insertParcels(seedParcels.map { it.toEntity() })
        }
    }

    override suspend fun seedTariffIfEmpty(defaultTariff: SettlementTariffVersion) {
        val active = tariffDao.getActiveTariffDirect()
        if (active == null) {
            tariffDao.insertTariff(defaultTariff.toEntity())
        }
    }

    override suspend fun syncAndMergeParcels(remoteParcels: List<Parcel>): Result<Int> {
        return try {
            var mergedCount = 0
            database.withTransaction {
                val existingParcels = parcelDao.getAllParcelsDirect().associateBy { it.id }
                for (remote in remoteParcels) {
                    val existing = existingParcels[remote.id]
                    if (existing == null) {
                        parcelDao.insertParcel(remote.toEntity())
                        mergedCount++
                    } else {
                        val existingStatus = try {
                            ParcelStatus.valueOf(existing.status)
                        } catch (_: Exception) {
                            ParcelStatus.OUT_FOR_DELIVERY
                        }

                        val isTerminal = existingStatus == ParcelStatus.DELIVERED_TO_CUSTOMER ||
                                existingStatus == ParcelStatus.RETURNED_TO_SENDER ||
                                existingStatus == ParcelStatus.REJECTED

                        if (!isTerminal) {
                            val canApply = if (existingStatus == remote.status) {
                                remote.updatedAt >= existing.updatedAt
                            } else {
                                ParcelStateMachine.canTransition(existingStatus, remote.status) &&
                                        remote.updatedAt >= existing.updatedAt
                            }

                            if (canApply) {
                                parcelDao.updateParcel(remote.toEntity())
                                mergedCount++
                            }
                        }
                    }
                }
            }
            Result.success(mergedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
