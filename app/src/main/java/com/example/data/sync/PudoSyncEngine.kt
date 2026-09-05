package com.example.data.sync

import androidx.room.withTransaction
import com.example.data.local.database.PudoDatabase
import com.example.data.local.entities.SyncCheckpointEntity
import com.example.data.local.entities.SyncQueueEntity
import com.example.data.model.ParcelStatus
import com.example.data.remote.RemoteSyncDataSource
import com.example.data.remote.SupabaseRemoteSyncDataSourceImpl
import com.example.data.remote.dto.*
import com.example.domain.statemachine.ParcelStateMachine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

data class SyncEngineResult(
    val processedCount: Int = 0,
    val successCount: Int = 0,
    val failureCount: Int = 0,
    val errors: List<String> = emptyList()
)

data class InboundSyncResult(
    val usersSynced: Int = 0,
    val hubsSynced: Int = 0,
    val parcelsSynced: Int = 0,
    val auditLogsSynced: Int = 0,
    val transactionsSynced: Int = 0,
    val errors: List<String> = emptyList()
)

data class FullSyncResult(
    val outbound: SyncEngineResult,
    val inbound: InboundSyncResult,
    val isSuccess: Boolean = outbound.failureCount == 0 && inbound.errors.isEmpty()
)

/**
 * Authoritative Synchronization Engine.
 * 
 * ARCHITECTURAL MANDATES:
 * - Read pending SyncQueueEntity items FIFO.
 * - Outbound dispatch to RemoteSyncDataSource.
 * - Mark queue item synced only upon confirmed remote success.
 * - Increment retry count on failure with exponential backoff; never drop unsynced items.
 * - Inbound delta merge with ParcelStateMachine validation (stale remote status rejected).
 * - Terminal states (DELIVERED_TO_CUSTOMER, RETURNED_TO_SENDER, REJECTED) are irreversible.
 * - Mutex concurrency guard prevents overlapping sync cycles.
 * - Checkpoints advanced ONLY after successful Room transaction merge.
 * - UI updates automatically through Room Flow subscriptions.
 */
class PudoSyncEngine(
    private val database: PudoDatabase,
    private val remoteDataSource: RemoteSyncDataSource,
    private val json: Json = SupabaseRemoteSyncDataSourceImpl.defaultJson
) {
    private val syncQueueDao = database.syncQueueDao()
    private val userDao = database.userDao()
    private val hubDao = database.hubDao()
    private val parcelDao = database.parcelDao()
    private val auditLogDao = database.auditLogDao()
    private val registrationTransactionDao = database.registrationTransactionDao()
    private val syncCheckpointDao = database.syncCheckpointDao()

    private val syncMutex = Mutex()

    /**
     * Executes a full synchronization cycle: processes outbound queue, then pulls inbound deltas.
     * Guarded by Mutex to prevent overlapping synchronization executions.
     */
    suspend fun synchronize(batchSize: Int = 20): FullSyncResult = syncMutex.withLock {
        withContext(Dispatchers.IO) {
            val outboundResult = processPendingSyncQueueInternal(batchSize)
            val inboundResult = performInboundDeltaSyncInternal()
            FullSyncResult(outbound = outboundResult, inbound = inboundResult)
        }
    }

    /**
     * Reads pending SyncQueue items FIFO in small batches and dispatches them to Supabase.
     */
    suspend fun processPendingSyncQueue(batchSize: Int = 20): SyncEngineResult = syncMutex.withLock {
        withContext(Dispatchers.IO) {
            processPendingSyncQueueInternal(batchSize)
        }
    }

    private val dependencyOrderComparator = Comparator<SyncQueueEntity> { a, b ->
        val priorityA = getEntityPriority(a.entityType)
        val priorityB = getEntityPriority(b.entityType)
        if (priorityA != priorityB) {
            priorityA.compareTo(priorityB)
        } else {
            val actionA = getActionPriority(a.action)
            val actionB = getActionPriority(b.action)
            if (actionA != actionB) {
                actionA.compareTo(actionB)
            } else {
                a.createdAt.compareTo(b.createdAt)
            }
        }
    }

    fun getEntityPriority(entityType: String): Int = when (entityType.uppercase()) {
        "USER" -> 1
        "HUB" -> 1
        "PARCEL" -> 2
        "REGISTRATION_TRANSACTION" -> 3
        "AUDIT_LOG" -> 4
        else -> 5
    }

    fun getActionPriority(action: String): Int = when (action.uppercase()) {
        "CREATE" -> 1
        "UPDATE" -> 2
        "DELETE" -> 3
        else -> 4
    }

    fun classifyFailure(throwable: Throwable?, retryCount: Int): String {
        if (retryCount >= MAX_RETRIES) {
            return FAILURE_MAX_RETRIES_EXCEEDED
        }
        if (throwable == null) return FAILURE_TRANSIENT_NETWORK
        val msg = throwable.message.orEmpty()
        return when {
            throwable is java.io.IOException ||
            throwable is java.net.SocketTimeoutException ||
            throwable is java.net.UnknownHostException ||
            msg.contains("500") || msg.contains("502") || msg.contains("503") || msg.contains("504") ||
            msg.contains("timeout", ignoreCase = true) || msg.contains("network", ignoreCase = true) ->
                FAILURE_TRANSIENT_NETWORK

            msg.contains("401") || msg.contains("403") || msg.contains("unauthorized", ignoreCase = true) || msg.contains("forbidden", ignoreCase = true) ->
                FAILURE_AUTHENTICATION_ERROR

            throwable is kotlinx.serialization.SerializationException ||
            throwable is IllegalArgumentException ||
            msg.contains("400") || msg.contains("422") || msg.contains("validation", ignoreCase = true) ->
                FAILURE_PAYLOAD_VALIDATION_ERROR

            throwable is IllegalStateException && msg.contains("not found", ignoreCase = true) ->
                FAILURE_PERMANENT_SCHEMA_ERROR

            else -> FAILURE_TRANSIENT_NETWORK
        }
    }

    private suspend fun processPendingSyncQueueInternal(batchSize: Int): SyncEngineResult {
        val rawPending = syncQueueDao.getPendingSyncBatch(batchSize)
        val sortedPending = rawPending.sortedWith(dependencyOrderComparator)
        var successCount = 0
        var failureCount = 0
        val errors = mutableListOf<String>()

        for (item in sortedPending) {
            if (item.retryCount >= MAX_RETRIES || item.failureClassification == FAILURE_PERMANENT_SCHEMA_ERROR) {
                errors.add("Item ${item.id} skipped: max retries or permanent error (${item.failureClassification})")
                failureCount++
                continue
            }

            val result = dispatchQueueItem(item)
            val now = System.currentTimeMillis()
            if (result.isSuccess) {
                syncQueueDao.markAsSynced(item.id, now)
                successCount++
            } else {
                val throwable = result.exceptionOrNull()
                val errorMsg = throwable?.message ?: "Unknown sync failure"
                val nextRetry = item.retryCount + 1
                val classification = classifyFailure(throwable, nextRetry)

                errors.add("Item ${item.id} (${item.entityType}:${item.action}) failed [$classification]: $errorMsg")
                syncQueueDao.updateFailureDiagnostics(
                    id = item.id,
                    retryCount = nextRetry,
                    lastAttemptAt = now,
                    lastError = errorMsg,
                    failureClassification = classification
                )
                failureCount++
            }
        }

        return SyncEngineResult(
            processedCount = sortedPending.size,
            successCount = successCount,
            failureCount = failureCount,
            errors = errors
        )
    }

    private suspend fun dispatchQueueItem(item: SyncQueueEntity): Result<Any> {
        return when (item.entityType.uppercase()) {
            "USER" -> dispatchUser(item)
            "HUB" -> dispatchHub(item)
            "PARCEL" -> dispatchParcel(item)
            "AUDIT_LOG" -> dispatchAuditLog(item)
            "REGISTRATION_TRANSACTION" -> dispatchRegistrationTransaction(item)
            else -> Result.failure(IllegalArgumentException("Unsupported entity type: ${item.entityType}"))
        }
    }

    private suspend fun dispatchUser(item: SyncQueueEntity): Result<Any> {
        if (item.action.equals("DELETE", ignoreCase = true)) {
            return remoteDataSource.deleteUser(item.entityId)
        }
        val localEntity = userDao.getUserById(item.entityId)
        val dto = if (localEntity != null) {
            SupabaseUserDto.fromEntity(localEntity)
        } else {
            try {
                json.decodeFromString<SupabaseUserDto>(item.payloadJson)
            } catch (e: Exception) {
                return Result.failure(IllegalStateException("Local user not found and payload invalid for ID: ${item.entityId}", e))
            }
        }
        return remoteDataSource.upsertUser(dto)
    }

    private suspend fun dispatchHub(item: SyncQueueEntity): Result<Any> {
        if (item.action.equals("DELETE", ignoreCase = true)) {
            return remoteDataSource.deleteHub(item.entityId)
        }
        val localEntity = hubDao.getHubByIdDirect(item.entityId)
        val dto = if (localEntity != null) {
            SupabaseHubDto.fromEntity(localEntity)
        } else {
            try {
                json.decodeFromString<SupabaseHubDto>(item.payloadJson)
            } catch (e: Exception) {
                return Result.failure(IllegalStateException("Local hub not found and payload invalid for ID: ${item.entityId}", e))
            }
        }
        return remoteDataSource.upsertHub(dto)
    }

    private suspend fun dispatchParcel(item: SyncQueueEntity): Result<Any> {
        if (item.action.equals("DELETE", ignoreCase = true)) {
            return remoteDataSource.deleteParcel(item.entityId)
        }
        val localEntity = parcelDao.getParcelByIdDirect(item.entityId)
        val dto = if (localEntity != null) {
            SupabaseParcelDto.fromEntity(localEntity)
        } else {
            try {
                json.decodeFromString<SupabaseParcelDto>(item.payloadJson)
            } catch (e: Exception) {
                return Result.failure(IllegalStateException("Local parcel not found and payload invalid for ID: ${item.entityId}", e))
            }
        }
        return remoteDataSource.upsertParcel(dto)
    }

    private suspend fun dispatchAuditLog(item: SyncQueueEntity): Result<Any> {
        val localEntity = auditLogDao.getLogByIdDirect(item.entityId)
        val dto = if (localEntity != null) {
            SupabaseAuditLogDto.fromEntity(localEntity)
        } else {
            try {
                json.decodeFromString<SupabaseAuditLogDto>(item.payloadJson)
            } catch (e: Exception) {
                return Result.failure(IllegalStateException("Local audit log not found and payload invalid for ID: ${item.entityId}", e))
            }
        }
        return remoteDataSource.pushAuditLog(dto)
    }

    private suspend fun dispatchRegistrationTransaction(item: SyncQueueEntity): Result<Any> {
        val localEntity = registrationTransactionDao.getTransactionById(item.entityId)
        val dto = if (localEntity != null) {
            SupabaseRegistrationTransactionDto.fromEntity(localEntity)
        } else {
            try {
                json.decodeFromString<SupabaseRegistrationTransactionDto>(item.payloadJson)
            } catch (e: Exception) {
                return Result.failure(IllegalStateException("Local transaction not found and payload invalid for ID: ${item.entityId}", e))
            }
        }
        return remoteDataSource.pushRegistrationTransaction(dto)
    }

    /**
     * Performs inbound delta fetch from Supabase and merges updates into Room using validation guards.
     */
    suspend fun performInboundDeltaSync(): InboundSyncResult = syncMutex.withLock {
        withContext(Dispatchers.IO) {
            performInboundDeltaSyncInternal()
        }
    }

    private suspend fun performInboundDeltaSyncInternal(): InboundSyncResult {
        val errors = mutableListOf<String>()
        var usersSynced = 0
        var hubsSynced = 0
        var parcelsSynced = 0
        var auditLogsSynced = 0
        var transactionsSynced = 0

        // 1. Sync Users
        val userCheckpoint = syncCheckpointDao.getCheckpoint(CHECKPOINT_USERS) ?: 0L
        remoteDataSource.fetchUsersUpdatedSince(userCheckpoint).fold(
            onSuccess = { remoteUsers ->
                try {
                    if (remoteUsers.isNotEmpty()) {
                        database.withTransaction {
                            var count = 0
                            var maxTimestamp = userCheckpoint
                            val existingUsers = userDao.getAllUsersDirect()
                            val existingById = existingUsers.associateBy { it.id }
                            val existingByPhone = existingUsers.associateBy { it.phone }
                            val existingByUsername = existingUsers.filter { it.username.isNotBlank() }.associateBy { it.username }
                            val existingByNationalId = existingUsers.filter { it.nationalId.isNotBlank() }.associateBy { it.nationalId }

                            for (remoteUser in remoteUsers) {
                                val current = existingById[remoteUser.id]
                                // Protected system accounts must not be overwritten or demoted
                                if (current != null && (current.role == "SYSTEM_ADMIN" || current.username.equals("reza", ignoreCase = true))) {
                                    if (remoteUser.updatedAt > maxTimestamp) maxTimestamp = remoteUser.updatedAt
                                    continue
                                }

                                val match = current
                                    ?: existingByPhone[remoteUser.phone]
                                    ?: (if (remoteUser.username.isNotBlank()) existingByUsername[remoteUser.username] else null)
                                    ?: (if (remoteUser.nationalId.isNotBlank()) existingByNationalId[remoteUser.nationalId] else null)

                                if (match == null) {
                                    userDao.insertUser(remoteUser.toEntity())
                                    count++
                                } else {
                                    if (match.role != "SYSTEM_ADMIN" && !match.username.equals("reza", ignoreCase = true) && remoteUser.updatedAt >= match.updatedAt) {
                                        // Preserve local password hash if remote is blank
                                        val entityToUpdate = remoteUser.toEntity().let { entity ->
                                            if (entity.passwordHash.isBlank() && match.passwordHash.isNotBlank()) {
                                                entity.copy(id = match.id, passwordHash = match.passwordHash)
                                            } else {
                                                entity.copy(id = match.id)
                                            }
                                        }
                                        userDao.updateUser(entityToUpdate)
                                        count++
                                    }
                                }
                                if (remoteUser.updatedAt > maxTimestamp) {
                                    maxTimestamp = remoteUser.updatedAt
                                }
                            }
                            if (maxTimestamp > userCheckpoint) {
                                syncCheckpointDao.saveCheckpoint(
                                    SyncCheckpointEntity(
                                        syncKey = CHECKPOINT_USERS,
                                        lastSyncedTimestamp = maxTimestamp,
                                        lastSyncedAt = System.currentTimeMillis(),
                                        recordsCount = count
                                    )
                                )
                            }
                            usersSynced = count
                        }
                    }
                } catch (e: Exception) {
                    errors.add("Inbound user Room merge failed: ${e.message}")
                }
            },
            onFailure = { errors.add("Inbound user sync failed: ${it.message}") }
        )

        // 2. Sync Hubs
        val hubCheckpoint = syncCheckpointDao.getCheckpoint(CHECKPOINT_HUBS) ?: 0L
        remoteDataSource.fetchHubsUpdatedSince(hubCheckpoint).fold(
            onSuccess = { remoteHubs ->
                try {
                    if (remoteHubs.isNotEmpty()) {
                        database.withTransaction {
                            var count = 0
                            var maxTimestamp = hubCheckpoint
                            for (remoteHub in remoteHubs) {
                                val local = hubDao.getHubByIdDirect(remoteHub.id)
                                if (local == null) {
                                    hubDao.insertHub(remoteHub.toEntity())
                                    count++
                                } else if (remoteHub.updatedAt >= local.updatedAt) {
                                    hubDao.updateHub(remoteHub.toEntity())
                                    count++
                                }
                                if (remoteHub.updatedAt > maxTimestamp) {
                                    maxTimestamp = remoteHub.updatedAt
                                }
                            }
                            if (maxTimestamp > hubCheckpoint) {
                                syncCheckpointDao.saveCheckpoint(
                                    SyncCheckpointEntity(
                                        syncKey = CHECKPOINT_HUBS,
                                        lastSyncedTimestamp = maxTimestamp,
                                        lastSyncedAt = System.currentTimeMillis(),
                                        recordsCount = count
                                    )
                                )
                            }
                            hubsSynced = count
                        }
                    }
                } catch (e: Exception) {
                    errors.add("Inbound hub Room merge failed: ${e.message}")
                }
            },
            onFailure = { errors.add("Inbound hub sync failed: ${it.message}") }
        )

        // 3. Sync Parcels with State Machine Guard
        val parcelCheckpoint = syncCheckpointDao.getCheckpoint(CHECKPOINT_PARCELS) ?: 0L
        remoteDataSource.fetchParcelsUpdatedSince(parcelCheckpoint).fold(
            onSuccess = { remoteParcels ->
                try {
                    if (remoteParcels.isNotEmpty()) {
                        database.withTransaction {
                            var count = 0
                            var maxTimestamp = parcelCheckpoint
                            for (remoteParcel in remoteParcels) {
                                val local = parcelDao.getParcelByIdDirect(remoteParcel.id)
                                if (shouldApplyRemoteParcel(local, remoteParcel)) {
                                    if (local == null) {
                                        parcelDao.insertParcel(remoteParcel.toEntity())
                                    } else {
                                        parcelDao.updateParcel(remoteParcel.toEntity())
                                    }
                                    count++
                                }
                                if (remoteParcel.updatedAt > maxTimestamp) {
                                    maxTimestamp = remoteParcel.updatedAt
                                }
                            }
                            if (maxTimestamp > parcelCheckpoint) {
                                syncCheckpointDao.saveCheckpoint(
                                    SyncCheckpointEntity(
                                        syncKey = CHECKPOINT_PARCELS,
                                        lastSyncedTimestamp = maxTimestamp,
                                        lastSyncedAt = System.currentTimeMillis(),
                                        recordsCount = count
                                    )
                                )
                            }
                            parcelsSynced = count
                        }
                    }
                } catch (e: Exception) {
                    errors.add("Inbound parcel Room merge failed: ${e.message}")
                }
            },
            onFailure = { errors.add("Inbound parcel sync failed: ${it.message}") }
        )

        // 4. Sync Audit Logs (Append-only & deduplicated)
        val auditCheckpoint = syncCheckpointDao.getCheckpoint(CHECKPOINT_AUDIT) ?: 0L
        remoteDataSource.fetchAuditLogsSince(auditCheckpoint).fold(
            onSuccess = { remoteLogs ->
                try {
                    if (remoteLogs.isNotEmpty()) {
                        database.withTransaction {
                            val entitiesToInsert = remoteLogs.map { it.toEntity() }
                            auditLogDao.insertLogs(entitiesToInsert)
                            auditLogsSynced = entitiesToInsert.size
                            val maxTimestamp = remoteLogs.maxOf { it.timestamp }
                            if (maxTimestamp > auditCheckpoint) {
                                syncCheckpointDao.saveCheckpoint(
                                    SyncCheckpointEntity(
                                        syncKey = CHECKPOINT_AUDIT,
                                        lastSyncedTimestamp = maxTimestamp,
                                        lastSyncedAt = System.currentTimeMillis(),
                                        recordsCount = entitiesToInsert.size
                                    )
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    errors.add("Inbound audit log Room merge failed: ${e.message}")
                }
            },
            onFailure = { errors.add("Inbound audit log sync failed: ${it.message}") }
        )

        // 5. Sync Registration Transactions (Immutable receipts)
        val txCheckpoint = syncCheckpointDao.getCheckpoint(CHECKPOINT_TRANSACTIONS) ?: 0L
        remoteDataSource.fetchRegistrationTransactionsSince(txCheckpoint).fold(
            onSuccess = { remoteTxs ->
                try {
                    if (remoteTxs.isNotEmpty()) {
                        database.withTransaction {
                            val entitiesToInsert = remoteTxs.map { it.toEntity() }
                            registrationTransactionDao.insertTransactions(entitiesToInsert)
                            transactionsSynced = entitiesToInsert.size
                            val maxTimestamp = remoteTxs.maxOf { it.timestamp }
                            if (maxTimestamp > txCheckpoint) {
                                syncCheckpointDao.saveCheckpoint(
                                    SyncCheckpointEntity(
                                        syncKey = CHECKPOINT_TRANSACTIONS,
                                        lastSyncedTimestamp = maxTimestamp,
                                        lastSyncedAt = System.currentTimeMillis(),
                                        recordsCount = entitiesToInsert.size
                                    )
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    errors.add("Inbound registration tx Room merge failed: ${e.message}")
                }
            },
            onFailure = { errors.add("Inbound registration tx sync failed: ${it.message}") }
        )

        return InboundSyncResult(
            usersSynced = usersSynced,
            hubsSynced = hubsSynced,
            parcelsSynced = parcelsSynced,
            auditLogsSynced = auditLogsSynced,
            transactionsSynced = transactionsSynced,
            errors = errors
        )
    }

    /**
     * Determines whether an inbound remote parcel update is valid to apply over local Room state.
     * Prevents stale remote state from overwriting advanced or terminal local parcel state.
     */
    fun shouldApplyRemoteParcel(
        local: com.example.data.local.entities.ParcelEntity?,
        remote: SupabaseParcelDto
    ): Boolean {
        if (local == null) return true

        val localStatus = try {
            ParcelStatus.valueOf(local.status)
        } catch (_: Exception) {
            ParcelStatus.OUT_FOR_DELIVERY
        }

        val remoteStatus = try {
            ParcelStatus.valueOf(remote.status)
        } catch (_: Exception) {
            ParcelStatus.OUT_FOR_DELIVERY
        }

        // Terminal local states (DELIVERED_TO_CUSTOMER, RETURNED_TO_SENDER, REJECTED) must never regress
        if (localStatus == ParcelStatus.DELIVERED_TO_CUSTOMER ||
            localStatus == ParcelStatus.RETURNED_TO_SENDER ||
            localStatus == ParcelStatus.REJECTED
        ) {
            return false
        }

        // If status is identical, apply if remote is newer or equal
        if (localStatus == remoteStatus) {
            return remote.updatedAt >= local.updatedAt
        }

        // If status differs, ensure forward state transition is valid per ParcelStateMachine
        val isValidTransition = ParcelStateMachine.canTransition(localStatus, remoteStatus)
        return isValidTransition && remote.updatedAt >= local.updatedAt
    }

    companion object {
        const val MAX_RETRIES = 5
        const val FAILURE_TRANSIENT_NETWORK = "TRANSIENT_NETWORK"
        const val FAILURE_AUTHENTICATION_ERROR = "AUTHENTICATION_ERROR"
        const val FAILURE_PAYLOAD_VALIDATION_ERROR = "PAYLOAD_VALIDATION_ERROR"
        const val FAILURE_PERMANENT_SCHEMA_ERROR = "PERMANENT_SCHEMA_ERROR"
        const val FAILURE_MAX_RETRIES_EXCEEDED = "MAX_RETRIES_EXCEEDED"

        const val CHECKPOINT_USERS = "users"
        const val CHECKPOINT_HUBS = "hubs"
        const val CHECKPOINT_PARCELS = "parcels"
        const val CHECKPOINT_AUDIT = "audit_logs"
        const val CHECKPOINT_TRANSACTIONS = "registration_transactions"
    }
}
