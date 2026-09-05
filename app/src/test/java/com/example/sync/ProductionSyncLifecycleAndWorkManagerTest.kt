package com.example.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.*
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.data.local.database.PudoDatabase
import com.example.data.local.entities.*
import com.example.data.model.AccountApprovalStatus
import com.example.data.model.ParcelSize
import com.example.data.model.ParcelStatus
import com.example.data.model.RegistrationSource
import com.example.data.remote.dto.*
import com.example.data.sync.PudoSyncEngine
import com.example.data.sync.PudoSyncScheduler
import com.example.data.sync.PudoSyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.util.concurrent.ExecutionException

@RunWith(RobolectricTestRunner::class)
class ProductionSyncLifecycleAndWorkManagerTest {

    private lateinit var context: Context
    private lateinit var database: PudoDatabase
    private lateinit var mockRemoteDataSource: FakeRemoteSyncDataSource
    private lateinit var syncEngine: PudoSyncEngine

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        
        // Initialize WorkManager test harness
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)

        database = PudoDatabase.createInMemory(context)
        mockRemoteDataSource = FakeRemoteSyncDataSource()
        syncEngine = PudoSyncEngine(database, mockRemoteDataSource)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // =========================================================================
    // 1. WorkManager Scheduling & Constraints Verification
    // =========================================================================
    @Test
    fun testWorkManagerSchedulingAndNetworkConstraint() {
        val constraints = PudoSyncScheduler.buildSyncConstraints()
        assertEquals(NetworkType.CONNECTED, constraints.requiredNetworkType)

        // Schedule one-time sync
        PudoSyncScheduler.scheduleOneTimeSync(context, replaceExisting = true)
        val workManager = WorkManager.getInstance(context)
        val workInfos = workManager.getWorkInfosForUniqueWork(PudoSyncScheduler.WORK_NAME_ONE_TIME).get()

        assertNotNull(workInfos)
        assertFalse(workInfos.isEmpty())
        val workInfo = workInfos[0]
        assertTrue(
            workInfo.state == WorkInfo.State.ENQUEUED ||
            workInfo.state == WorkInfo.State.RUNNING ||
            workInfo.state == WorkInfo.State.BLOCKED
        )
        assertTrue(workInfo.tags.contains(PudoSyncScheduler.TAG_SYNC))
    }

    // =========================================================================
    // 2. Unique Work Deduplication Policy
    // =========================================================================
    @Test
    fun testUniqueWorkDeduplicationPolicy() {
        val workManager = WorkManager.getInstance(context)

        // Enqueue once
        PudoSyncScheduler.scheduleOneTimeSync(context, replaceExisting = false)
        val firstInfos = workManager.getWorkInfosForUniqueWork(PudoSyncScheduler.WORK_NAME_ONE_TIME).get()
        assertEquals(1, firstInfos.size)
        val firstId = firstInfos[0].id

        // Enqueue again with KEEP policy
        PudoSyncScheduler.scheduleOneTimeSync(context, replaceExisting = false)
        val secondInfos = workManager.getWorkInfosForUniqueWork(PudoSyncScheduler.WORK_NAME_ONE_TIME).get()
        assertEquals(1, secondInfos.size)
        assertEquals(firstId, secondInfos[0].id)
    }

    // =========================================================================
    // 3. Worker Retry Behavior on Transient Remote Failure
    // =========================================================================
    @Test
    fun testWorkerRetryBehaviorOnTransientFailure() = runBlocking {
        mockRemoteDataSource.shouldFailNextOperation = true

        // Insert pending item
        database.syncQueueDao().insertSyncItem(
            SyncQueueEntity(
                id = "retry-queue-1",
                entityType = "HUB",
                entityId = "hub-retry-1",
                action = "CREATE",
                payloadJson = "{\"id\":\"hub-retry-1\",\"name\":\"Retry Hub\",\"manager_name\":\"M\",\"phone\":\"0912\"}",
                createdAt = 1000L
            )
        )

        val result = syncEngine.synchronize()

        // SyncEngine records failure and does not mark as synced
        assertEquals(1, result.outbound.failureCount)
        assertEquals(0, result.outbound.successCount)
        assertFalse(result.isSuccess)

        val pending = database.syncQueueDao().getPendingSyncItemsDirect()
        assertEquals(1, pending.size)
        assertEquals(1, pending[0].retryCount)
        assertEquals(PudoSyncEngine.FAILURE_TRANSIENT_NETWORK, pending[0].failureClassification)
    }

    // =========================================================================
    // 4. Worker Success on Healthy Sync
    // =========================================================================
    @Test
    fun testWorkerSuccessOnHealthySync() = runBlocking {
        // Insert local hub and pending queue
        val hub = HubEntity(
            id = "hub-worker-ok",
            name = "Worker OK Hub",
            managerName = "Manager",
            phone = "09128888888",
            updatedAt = 2000L
        )
        database.hubDao().insertHub(hub)
        database.syncQueueDao().insertSyncItem(
            SyncQueueEntity(
                id = "q-worker-ok",
                entityType = "HUB",
                entityId = hub.id,
                action = "CREATE",
                createdAt = 2000L
            )
        )

        val result = syncEngine.synchronize()

        assertTrue(result.isSuccess)
        assertEquals(1, result.outbound.successCount)
        assertEquals(0, result.outbound.failureCount)

        val pending = database.syncQueueDao().getPendingSyncItemsDirect()
        assertTrue(pending.isEmpty())
    }

    // =========================================================================
    // 5. Offline Queue Survival Across Simulated Process Death / Reload
    // =========================================================================
    @Test
    fun testOfflineQueueSurvivalAcrossSimulatedProcessRestart() = runBlocking {
        // User performs offline mutations
        val offlineParcel = ParcelEntity(
            id = "offline-pcl-1",
            trackingNumber = "TRK-OFFLINE-1",
            senderName = "Offline Sender",
            recipientName = "Offline Recipient",
            recipientPhone = "09121112233",
            recipientPostalCode = "1234567890",
            status = ParcelStatus.HUB_SELECTED.name,
            size = ParcelSize.MEDIUM.name,
            registrationSource = RegistrationSource.CUSTOMER_REQUEST.name,
            createdAt = 3000L,
            updatedAt = 3000L
        )
        database.parcelDao().insertParcel(offlineParcel)

        val queueItem = SyncQueueEntity(
            id = "q-offline-1",
            entityType = "PARCEL",
            entityId = offlineParcel.id,
            action = "CREATE",
            payloadJson = "{\"id\":\"offline-pcl-1\",\"tracking_number\":\"TRK-OFFLINE-1\"}",
            createdAt = 3000L,
            retryCount = 0
        )
        database.syncQueueDao().insertSyncItem(queueItem)

        // Verify pending queue count before reload
        assertEquals(1, database.syncQueueDao().getPendingCount())

        // Simulate engine re-initialization (e.g. process restart)
        val newEngine = PudoSyncEngine(database, mockRemoteDataSource)
        val pendingBeforeSync = database.syncQueueDao().getPendingSyncItemsDirect()
        assertEquals(1, pendingBeforeSync.size)
        assertEquals("q-offline-1", pendingBeforeSync[0].id)
        assertEquals("PARCEL", pendingBeforeSync[0].entityType)

        // Process after restart
        val syncResult = newEngine.synchronize()
        assertTrue(syncResult.isSuccess)
        assertEquals(1, syncResult.outbound.successCount)
        assertEquals(0, database.syncQueueDao().getPendingCount())
    }

    // =========================================================================
    // 6. Network Recovery Dispatches Accumulated Queue
    // =========================================================================
    @Test
    fun testNetworkRecoveryDispatchesAccumulatedQueue() = runBlocking {
        // Accumulate multiple entities while offline
        val user = UserEntity(
            id = "usr-acc-1",
            role = "COURIER",
            fullName = "Courier Acc",
            phone = "09123334455",
            nationalId = "0011223344",
            approvalStatus = AccountApprovalStatus.APPROVED.name,
            updatedAt = 4000L
        )
        database.userDao().insertUser(user)

        val hub = HubEntity(
            id = "hub-acc-1",
            name = "Hub Acc",
            managerName = "Manager Acc",
            phone = "09124445566",
            updatedAt = 4000L
        )
        database.hubDao().insertHub(hub)

        val parcel = ParcelEntity(
            id = "pcl-acc-1",
            trackingNumber = "TRK-ACC-1",
            senderName = "Sender Acc",
            recipientName = "Recipient Acc",
            recipientPhone = "09125556677",
            recipientPostalCode = "9988776655",
            status = ParcelStatus.HUB_SELECTED.name,
            size = ParcelSize.SMALL.name,
            registrationSource = RegistrationSource.FAILED_HOME_DELIVERY.name,
            courierId = user.id,
            hubId = hub.id,
            createdAt = 4000L,
            updatedAt = 4000L
        )
        database.parcelDao().insertParcel(parcel)

        val tx = RegistrationTransactionEntity(
            transactionId = "tx-acc-1",
            parcelId = parcel.id,
            trackingNumber = parcel.trackingNumber,
            courierId = user.id,
            hubId = hub.id,
            registrationSource = RegistrationSource.FAILED_HOME_DELIVERY.name,
            timestamp = 4000L
        )
        database.registrationTransactionDao().insertTransaction(tx)

        val log = AuditLogEntity(
            id = "log-acc-1",
            eventType = "REGISTER",
            actorId = user.id,
            actorRole = "COURIER",
            entityId = parcel.id,
            oldState = null,
            newState = ParcelStatus.HUB_SELECTED.name,
            transactionId = tx.transactionId,
            timestamp = 4000L
        )
        database.auditLogDao().insertLog(log)

        // Queue all items
        database.syncQueueDao().insertSyncItems(
            listOf(
                SyncQueueEntity(id = "q-user", entityType = "USER", entityId = user.id, action = "CREATE", createdAt = 4001L),
                SyncQueueEntity(id = "q-hub", entityType = "HUB", entityId = hub.id, action = "CREATE", createdAt = 4002L),
                SyncQueueEntity(id = "q-pcl", entityType = "PARCEL", entityId = parcel.id, action = "CREATE", createdAt = 4003L),
                SyncQueueEntity(id = "q-tx", entityType = "REGISTRATION_TRANSACTION", entityId = tx.transactionId, action = "CREATE", createdAt = 4004L),
                SyncQueueEntity(id = "q-log", entityType = "AUDIT_LOG", entityId = log.id, action = "CREATE", createdAt = 4005L)
            )
        )

        assertEquals(5, database.syncQueueDao().getPendingCount())

        // Network returns: execute synchronization
        val syncResult = syncEngine.synchronize(batchSize = 20)

        assertTrue(syncResult.isSuccess)
        assertEquals(5, syncResult.outbound.successCount)
        assertEquals(0, syncResult.outbound.failureCount)
        assertEquals(0, database.syncQueueDao().getPendingCount())

        // Verify remote received all 5 items
        assertEquals(1, mockRemoteDataSource.upsertedUsers.size)
        assertEquals(1, mockRemoteDataSource.upsertedHubs.size)
        assertEquals(1, mockRemoteDataSource.upsertedParcels.size)
        assertEquals(1, mockRemoteDataSource.pushedTransactions.size)
        assertEquals(1, mockRemoteDataSource.pushedAuditLogs.size)
    }

    // =========================================================================
    // 7. Dependency-Aware Ordering Verification
    // =========================================================================
    @Test
    fun testDependencyAwareOrderingHierarchy() {
        val userItem = SyncQueueEntity(id = "q-1", entityType = "USER", entityId = "u1", action = "CREATE", createdAt = 5000L)
        val hubItem = SyncQueueEntity(id = "q-2", entityType = "HUB", entityId = "h1", action = "CREATE", createdAt = 5000L)
        val parcelItem = SyncQueueEntity(id = "q-3", entityType = "PARCEL", entityId = "p1", action = "CREATE", createdAt = 4000L)
        val txItem = SyncQueueEntity(id = "q-4", entityType = "REGISTRATION_TRANSACTION", entityId = "t1", action = "CREATE", createdAt = 3000L)
        val logItem = SyncQueueEntity(id = "q-5", entityType = "AUDIT_LOG", entityId = "l1", action = "CREATE", createdAt = 2000L)

        // Raw list has AuditLog and Tx first by timestamp
        val rawList = listOf(logItem, txItem, parcelItem, hubItem, userItem)

        // Sort by dependency priorities
        val sorted = rawList.sortedWith(Comparator { a, b ->
            val prioA = syncEngine.getEntityPriority(a.entityType)
            val prioB = syncEngine.getEntityPriority(b.entityType)
            if (prioA != prioB) prioA.compareTo(prioB)
            else a.createdAt.compareTo(b.createdAt)
        })

        // Verify topological levels: Users/Hubs (level 1) -> Parcels (level 2) -> Transactions (level 3) -> AuditLogs (level 4)
        assertTrue(sorted[0].entityType == "USER" || sorted[0].entityType == "HUB")
        assertTrue(sorted[1].entityType == "USER" || sorted[1].entityType == "HUB")
        assertEquals("PARCEL", sorted[2].entityType)
        assertEquals("REGISTRATION_TRANSACTION", sorted[3].entityType)
        assertEquals("AUDIT_LOG", sorted[4].entityType)
    }

    // =========================================================================
    // 8. Concurrent Sync Trigger Safety (Mutex Protection)
    // =========================================================================
    @Test
    fun testConcurrentSyncTriggerSafetyMutex() = runBlocking {
        val hub = HubEntity(
            id = "hub-concurrent",
            name = "Concurrent Hub",
            managerName = "Mgr",
            phone = "09120009999",
            updatedAt = 6000L
        )
        database.hubDao().insertHub(hub)

        database.syncQueueDao().insertSyncItem(
            SyncQueueEntity(
                id = "q-concurrent",
                entityType = "HUB",
                entityId = hub.id,
                action = "CREATE",
                createdAt = 6000L
            )
        )

        // Launch 10 concurrent coroutines attempting to synchronize at the exact same moment
        val deferredResults = (1..10).map {
            async(Dispatchers.IO) {
                syncEngine.synchronize()
            }
        }

        val results = deferredResults.awaitAll()

        // Exactly one sync run will process the pending item, all others will find 0 pending items safely
        val totalOutboundProcessed = results.sumOf { it.outbound.processedCount }
        val totalSuccess = results.sumOf { it.outbound.successCount }

        assertEquals(1, totalOutboundProcessed)
        assertEquals(1, totalSuccess)
        assertEquals(1, mockRemoteDataSource.upsertedHubs.size)
        assertEquals(0, database.syncQueueDao().getPendingCount())
    }

    // =========================================================================
    // 9. Failure Classification & Bounded Retries
    // =========================================================================
    @Test
    fun testFailureClassificationAndDiagnostics() {
        // Network IO error
        val netClass = syncEngine.classifyFailure(IOException("Connection refused"), 1)
        assertEquals(PudoSyncEngine.FAILURE_TRANSIENT_NETWORK, netClass)

        // HTTP 503 error
        val serverErrClass = syncEngine.classifyFailure(Exception("HTTP 503 Service Unavailable"), 1)
        assertEquals(PudoSyncEngine.FAILURE_TRANSIENT_NETWORK, serverErrClass)

        // HTTP 401 Auth error
        val authClass = syncEngine.classifyFailure(Exception("HTTP 401 Unauthorized"), 1)
        assertEquals(PudoSyncEngine.FAILURE_AUTHENTICATION_ERROR, authClass)

        // HTTP 422 Validation error
        val valClass = syncEngine.classifyFailure(Exception("HTTP 422 Unprocessable Entity"), 1)
        assertEquals(PudoSyncEngine.FAILURE_PAYLOAD_VALIDATION_ERROR, valClass)

        // Permanent missing entity
        val schemaClass = syncEngine.classifyFailure(IllegalStateException("Local user not found"), 1)
        assertEquals(PudoSyncEngine.FAILURE_PERMANENT_SCHEMA_ERROR, schemaClass)

        // Exceeded retries
        val maxClass = syncEngine.classifyFailure(IOException("Timeout"), 5)
        assertEquals(PudoSyncEngine.FAILURE_MAX_RETRIES_EXCEEDED, maxClass)
    }

    // =========================================================================
    // 10. Checkpoint Atomicity on Inbound Sync
    // =========================================================================
    @Test
    fun testCheckpointAtomicityOnInboundFailure() = runBlocking {
        // Remote returns an update
        mockRemoteDataSource.remoteHubsToReturn.add(
            SupabaseHubDto(
                id = "hub-inbound-atomicity",
                name = "Atomicity Hub",
                managerName = "Mgr",
                phone = "09121111111",
                updatedAt = 9999L
            )
        )

        // Perform inbound sync successfully
        val res = syncEngine.performInboundDeltaSync()
        assertEquals(1, res.hubsSynced)

        val savedCheckpoint = database.syncCheckpointDao().getCheckpoint(PudoSyncEngine.CHECKPOINT_HUBS)
        assertEquals(9999L, savedCheckpoint)

        // Remote fails on next attempt
        mockRemoteDataSource.shouldFailNextOperation = true
        val failRes = syncEngine.performInboundDeltaSync()
        assertTrue(failRes.errors.isNotEmpty())
        assertEquals(0, failRes.hubsSynced)

        // Checkpoint must NOT advance on failure
        val checkpointAfterFail = database.syncCheckpointDao().getCheckpoint(PudoSyncEngine.CHECKPOINT_HUBS)
        assertEquals(9999L, checkpointAfterFail)
    }
}
