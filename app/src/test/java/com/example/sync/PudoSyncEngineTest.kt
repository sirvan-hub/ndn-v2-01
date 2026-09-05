package com.example.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.database.PudoDatabase
import com.example.data.local.entities.*
import com.example.data.model.AccountApprovalStatus
import com.example.data.model.ParcelSize
import com.example.data.model.ParcelStatus
import com.example.data.model.RegistrationSource
import com.example.data.remote.RemoteSyncDataSource
import com.example.data.remote.dto.*
import com.example.data.sync.PudoSyncEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class PudoSyncEngineTest {

    private lateinit var database: PudoDatabase
    private lateinit var mockRemoteDataSource: FakeRemoteSyncDataSource
    private lateinit var syncEngine: PudoSyncEngine

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = PudoDatabase.createInMemory(context)
        mockRemoteDataSource = FakeRemoteSyncDataSource()
        syncEngine = PudoSyncEngine(database, mockRemoteDataSource)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testSuccessfulHubQueueDispatchMarksQueueSynced() = runBlocking {
        // Insert a local hub
        val hubEntity = HubEntity(
            id = "hub-test-1",
            name = "Tehran Hub 1",
            managerName = "Ali",
            phone = "09120001111",
            updatedAt = 1000L
        )
        database.hubDao().insertHub(hubEntity)

        // Insert pending SyncQueue item
        val queueItem = SyncQueueEntity(
            id = "queue-1",
            entityType = "HUB",
            entityId = hubEntity.id,
            action = "CREATE",
            payloadJson = "{\"id\":\"${hubEntity.id}\",\"name\":\"${hubEntity.name}\",\"manager_name\":\"${hubEntity.managerName}\",\"phone\":\"${hubEntity.phone}\"}",
            createdAt = 100L
        )
        database.syncQueueDao().insertSyncItem(queueItem)

        // Process queue
        val result = syncEngine.processPendingSyncQueue(batchSize = 10)

        assertEquals(1, result.processedCount)
        assertEquals(1, result.successCount)
        assertEquals(0, result.failureCount)

        // Verify remote received upsert
        assertEquals(1, mockRemoteDataSource.upsertedHubs.size)
        assertEquals("hub-test-1", mockRemoteDataSource.upsertedHubs[0].id)

        // Verify queue item is marked synced in Room
        val pendingItems = database.syncQueueDao().getPendingSyncItemsDirect()
        assertTrue(pendingItems.isEmpty())
    }

    @Test
    fun testFailedHubDispatchRemainsPendingAndIncrementsRetryCount() = runBlocking {
        mockRemoteDataSource.shouldFailNextOperation = true

        val queueItem = SyncQueueEntity(
            id = "queue-fail-1",
            entityType = "HUB",
            entityId = "hub-test-2",
            action = "CREATE",
            payloadJson = "{\"id\":\"hub-test-2\",\"name\":\"Fail Hub\",\"manager_name\":\"Reza\",\"phone\":\"0912\"}",
            createdAt = 100L,
            retryCount = 0
        )
        database.syncQueueDao().insertSyncItem(queueItem)

        val result = syncEngine.processPendingSyncQueue(batchSize = 10)

        assertEquals(1, result.processedCount)
        assertEquals(0, result.successCount)
        assertEquals(1, result.failureCount)

        // Verify item is NOT removed from pending queue and retryCount incremented
        val pendingItems = database.syncQueueDao().getPendingSyncItemsDirect()
        assertEquals(1, pendingItems.size)
        assertEquals(1, pendingItems[0].retryCount)
        assertFalse(pendingItems[0].isSynced)
    }

    @Test
    fun testSuccessfulParcelDispatchMarksQueueSynced() = runBlocking {
        val parcelEntity = ParcelEntity(
            id = "pcl-test-1",
            trackingNumber = "TRK-PCL-01",
            senderName = "Sender A",
            recipientName = "Recipient B",
            recipientPhone = "09121234567",
            recipientPostalCode = "1234567890",
            status = ParcelStatus.OUT_FOR_DELIVERY.name,
            size = ParcelSize.MEDIUM.name,
            registrationSource = RegistrationSource.CUSTOMER_REQUEST.name,
            updatedAt = 2000L
        )
        database.parcelDao().insertParcel(parcelEntity)

        val queueItem = SyncQueueEntity(
            id = "queue-parcel-1",
            entityType = "PARCEL",
            entityId = parcelEntity.id,
            action = "CREATE",
            payloadJson = "{\"id\":\"${parcelEntity.id}\",\"tracking_number\":\"${parcelEntity.trackingNumber}\",\"sender_name\":\"Sender A\",\"recipient_name\":\"Recipient B\",\"recipient_phone\":\"09121234567\",\"recipient_postal_code\":\"1234567890\",\"status\":\"OUT_FOR_DELIVERY\"}",
            createdAt = 200L
        )
        database.syncQueueDao().insertSyncItem(queueItem)

        val result = syncEngine.processPendingSyncQueue(batchSize = 10)
        assertEquals(1, result.successCount)

        assertEquals(1, mockRemoteDataSource.upsertedParcels.size)
        assertEquals("pcl-test-1", mockRemoteDataSource.upsertedParcels[0].id)

        val pending = database.syncQueueDao().getPendingSyncItemsDirect()
        assertTrue(pending.isEmpty())
    }

    @Test
    fun testQueueFifoOrderingPreserved() = runBlocking {
        val item1 = SyncQueueEntity(
            id = "q-1",
            entityType = "HUB",
            entityId = "hub-1",
            action = "CREATE",
            payloadJson = "{\"id\":\"hub-1\",\"name\":\"H1\",\"manager_name\":\"M1\",\"phone\":\"01\"}",
            createdAt = 100L
        )
        val item2 = SyncQueueEntity(
            id = "q-2",
            entityType = "HUB",
            entityId = "hub-2",
            action = "CREATE",
            payloadJson = "{\"id\":\"hub-2\",\"name\":\"H2\",\"manager_name\":\"M2\",\"phone\":\"02\"}",
            createdAt = 200L
        )
        val item3 = SyncQueueEntity(
            id = "q-3",
            entityType = "HUB",
            entityId = "hub-3",
            action = "CREATE",
            payloadJson = "{\"id\":\"hub-3\",\"name\":\"H3\",\"manager_name\":\"M3\",\"phone\":\"03\"}",
            createdAt = 300L
        )

        database.syncQueueDao().insertSyncItems(listOf(item3, item1, item2))

        val result = syncEngine.processPendingSyncQueue(batchSize = 10)
        assertEquals(3, result.successCount)

        // Verify FIFO order of remote dispatch
        assertEquals("hub-1", mockRemoteDataSource.upsertedHubs[0].id)
        assertEquals("hub-2", mockRemoteDataSource.upsertedHubs[1].id)
        assertEquals("hub-3", mockRemoteDataSource.upsertedHubs[2].id)
    }

    @Test
    fun testDuplicateAuditLogDispatchIsIdempotent() = runBlocking {
        val logEntity = AuditLogEntity(
            id = "audit-1",
            eventType = "PARCEL_TRANSITION",
            actorId = "usr-1",
            actorRole = "COURIER",
            entityId = "pcl-1",
            oldState = "OUT_FOR_DELIVERY",
            newState = "STORED_AT_HUB",
            transactionId = "tx-1",
            timestamp = 5000L
        )
        database.auditLogDao().insertLog(logEntity)

        val queueItem1 = SyncQueueEntity(
            id = "q-audit-1",
            entityType = "AUDIT_LOG",
            entityId = logEntity.id,
            action = "CREATE",
            payloadJson = "{\"id\":\"audit-1\",\"event_type\":\"PARCEL_TRANSITION\",\"actor_id\":\"usr-1\",\"actor_role\":\"COURIER\",\"entity_id\":\"pcl-1\",\"new_state\":\"STORED_AT_HUB\"}",
            createdAt = 500L
        )
        val queueItem2 = SyncQueueEntity(
            id = "q-audit-2",
            entityType = "AUDIT_LOG",
            entityId = logEntity.id,
            action = "CREATE",
            payloadJson = "{\"id\":\"audit-1\",\"event_type\":\"PARCEL_TRANSITION\",\"actor_id\":\"usr-1\",\"actor_role\":\"COURIER\",\"entity_id\":\"pcl-1\",\"new_state\":\"STORED_AT_HUB\"}",
            createdAt = 600L
        )

        database.syncQueueDao().insertSyncItems(listOf(queueItem1, queueItem2))

        val result = syncEngine.processPendingSyncQueue(batchSize = 10)
        assertEquals(2, result.successCount)
        assertEquals(2, mockRemoteDataSource.pushedAuditLogs.size)
        assertEquals("audit-1", mockRemoteDataSource.pushedAuditLogs[0].id)
        assertEquals("audit-1", mockRemoteDataSource.pushedAuditLogs[1].id)
    }

    @Test
    fun testLocalParcelRemainsVisibleOfflineInRoomFlow() = runBlocking {
        val localParcel = ParcelEntity(
            id = "pcl-offline-1",
            trackingNumber = "TRK-OFFLINE",
            senderName = "Offline Sender",
            recipientName = "Offline Recipient",
            recipientPhone = "09121111111",
            recipientPostalCode = "1111111111",
            status = ParcelStatus.OUT_FOR_DELIVERY.name,
            size = ParcelSize.MEDIUM.name,
            registrationSource = RegistrationSource.CUSTOMER_REQUEST.name,
            updatedAt = 1000L
        )
        database.parcelDao().insertParcel(localParcel)

        // Flow emits immediately even before remote sync is called
        val parcels = database.parcelDao().getAllParcels().first()
        assertEquals(1, parcels.size)
        assertEquals("pcl-offline-1", parcels[0].id)
    }

    @Test
    fun testInboundDeltaSyncMergesValidParcels() = runBlocking {
        // Prepare remote parcel
        val remoteParcel = SupabaseParcelDto(
            id = "pcl-remote-1",
            trackingNumber = "TRK-REMOTE-1",
            senderName = "Remote Sender",
            recipientName = "Remote Recipient",
            recipientPhone = "09122222222",
            recipientPostalCode = "2222222222",
            status = "OUT_FOR_DELIVERY",
            size = "MEDIUM",
            registrationSource = "CUSTOMER_REQUEST",
            updatedAt = 5000L
        )
        mockRemoteDataSource.remoteParcelsToReturn.add(remoteParcel)

        val inboundResult = syncEngine.performInboundDeltaSync()

        assertEquals(1, inboundResult.parcelsSynced)
        val localParcels = database.parcelDao().getAllParcels().first()
        assertEquals(1, localParcels.size)
        assertEquals("pcl-remote-1", localParcels[0].id)
    }

    @Test
    fun testInboundDeltaSyncRejectsStaleParcelStatusRegression() = runBlocking {
        // Local parcel has advanced to STORED_AT_HUB
        val localParcel = ParcelEntity(
            id = "pcl-conflict-1",
            trackingNumber = "TRK-CONFLICT",
            senderName = "Sender",
            recipientName = "Recipient",
            recipientPhone = "09123333333",
            recipientPostalCode = "3333333333",
            status = ParcelStatus.STORED_AT_HUB.name,
            size = ParcelSize.MEDIUM.name,
            registrationSource = RegistrationSource.CUSTOMER_REQUEST.name,
            updatedAt = 6000L
        )
        database.parcelDao().insertParcel(localParcel)

        // Remote has stale OUT_FOR_DELIVERY with older timestamp
        val staleRemoteParcel = SupabaseParcelDto(
            id = "pcl-conflict-1",
            trackingNumber = "TRK-CONFLICT",
            senderName = "Sender",
            recipientName = "Recipient",
            recipientPhone = "09123333333",
            recipientPostalCode = "3333333333",
            status = ParcelStatus.OUT_FOR_DELIVERY.name,
            size = "MEDIUM",
            registrationSource = "COURIER_INITIATED",
            updatedAt = 4000L
        )
        mockRemoteDataSource.remoteParcelsToReturn.add(staleRemoteParcel)

        val inboundResult = syncEngine.performInboundDeltaSync()

        // Should not apply stale regression
        assertEquals(0, inboundResult.parcelsSynced)
        val loaded = database.parcelDao().getParcelByIdDirect("pcl-conflict-1")
        assertEquals(ParcelStatus.STORED_AT_HUB.name, loaded?.status)
    }

    @Test
    fun testInboundDeltaSyncMergesHubsAndUsersByTimestampConflictRules() = runBlocking {
        val localUser = UserEntity(
            id = "usr-1",
            username = "local_user",
            fullName = "Old Name",
            phone = "09124444444",
            role = "CUSTOMER",
            approvalStatus = "APPROVED",
            updatedAt = 2000L
        )
        database.userDao().insertUser(localUser)

        val remoteUser = SupabaseUserDto(
            id = "usr-1",
            username = "local_user",
            fullName = "New Updated Name",
            phone = "09124444444",
            role = "CUSTOMER",
            approvalStatus = "APPROVED",
            updatedAt = 3000L
        )
        mockRemoteDataSource.remoteUsersToReturn.add(remoteUser)

        val inboundResult = syncEngine.performInboundDeltaSync()
        assertEquals(1, inboundResult.usersSynced)

        val updatedUser = database.userDao().getUserById("usr-1")
        assertEquals("New Updated Name", updatedUser?.fullName)
        assertEquals(3000L, updatedUser?.updatedAt)
    }

    @Test
    fun testInboundDeltaSyncUpdatesSyncCheckpointsPersistently() = runBlocking {
        mockRemoteDataSource.remoteHubsToReturn.add(
            SupabaseHubDto(
                id = "hub-chk-1",
                name = "Hub Checkpoint Test",
                managerName = "Manager",
                phone = "09125555555",
                updatedAt = 7500L
            )
        )

        syncEngine.performInboundDeltaSync()

        val savedCheckpoint = database.syncCheckpointDao().getCheckpoint(PudoSyncEngine.CHECKPOINT_HUBS)
        assertEquals(7500L, savedCheckpoint)
    }
}

/**
 * In-memory Mock implementation of RemoteSyncDataSource for unit testing.
 */
class FakeRemoteSyncDataSource : RemoteSyncDataSource {

    val upsertedUsers = mutableListOf<SupabaseUserDto>()
    val upsertedHubs = mutableListOf<SupabaseHubDto>()
    val upsertedParcels = mutableListOf<SupabaseParcelDto>()
    val pushedAuditLogs = mutableListOf<SupabaseAuditLogDto>()
    val pushedTransactions = mutableListOf<SupabaseRegistrationTransactionDto>()

    val remoteUsersToReturn = mutableListOf<SupabaseUserDto>()
    val remoteHubsToReturn = mutableListOf<SupabaseHubDto>()
    val remoteParcelsToReturn = mutableListOf<SupabaseParcelDto>()
    val remoteAuditLogsToReturn = mutableListOf<SupabaseAuditLogDto>()
    val remoteTransactionsToReturn = mutableListOf<SupabaseRegistrationTransactionDto>()

    var shouldFailNextOperation: Boolean = false

    override suspend fun upsertUser(userDto: SupabaseUserDto): Result<SupabaseUserDto> {
        if (shouldFailNextOperation) return Result.failure(IOException("Simulated network timeout"))
        upsertedUsers.add(userDto)
        return Result.success(userDto)
    }

    override suspend fun upsertHub(hubDto: SupabaseHubDto): Result<SupabaseHubDto> {
        if (shouldFailNextOperation) return Result.failure(IOException("Simulated network timeout"))
        upsertedHubs.add(hubDto)
        return Result.success(hubDto)
    }

    override suspend fun upsertParcel(parcelDto: SupabaseParcelDto): Result<SupabaseParcelDto> {
        if (shouldFailNextOperation) return Result.failure(IOException("Simulated network timeout"))
        upsertedParcels.add(parcelDto)
        return Result.success(parcelDto)
    }

    override suspend fun pushAuditLog(auditLogDto: SupabaseAuditLogDto): Result<SupabaseAuditLogDto> {
        if (shouldFailNextOperation) return Result.failure(IOException("Simulated network timeout"))
        pushedAuditLogs.add(auditLogDto)
        return Result.success(auditLogDto)
    }

    override suspend fun pushRegistrationTransaction(txDto: SupabaseRegistrationTransactionDto): Result<SupabaseRegistrationTransactionDto> {
        if (shouldFailNextOperation) return Result.failure(IOException("Simulated network timeout"))
        pushedTransactions.add(txDto)
        return Result.success(txDto)
    }

    override suspend fun deleteUser(userId: String): Result<Boolean> {
        if (shouldFailNextOperation) return Result.failure(IOException("Simulated network timeout"))
        return Result.success(true)
    }

    override suspend fun deleteHub(hubId: String): Result<Boolean> {
        if (shouldFailNextOperation) return Result.failure(IOException("Simulated network timeout"))
        return Result.success(true)
    }

    override suspend fun deleteParcel(parcelId: String): Result<Boolean> {
        if (shouldFailNextOperation) return Result.failure(IOException("Simulated network timeout"))
        return Result.success(true)
    }

    override suspend fun fetchUsersUpdatedSince(timestamp: Long): Result<List<SupabaseUserDto>> {
        if (shouldFailNextOperation) return Result.failure(IOException("Simulated network timeout"))
        return Result.success(remoteUsersToReturn)
    }

    override suspend fun fetchHubsUpdatedSince(timestamp: Long): Result<List<SupabaseHubDto>> {
        if (shouldFailNextOperation) return Result.failure(IOException("Simulated network timeout"))
        return Result.success(remoteHubsToReturn)
    }

    override suspend fun fetchParcelsUpdatedSince(timestamp: Long): Result<List<SupabaseParcelDto>> {
        if (shouldFailNextOperation) return Result.failure(IOException("Simulated network timeout"))
        return Result.success(remoteParcelsToReturn)
    }

    override suspend fun fetchAuditLogsSince(timestamp: Long): Result<List<SupabaseAuditLogDto>> {
        if (shouldFailNextOperation) return Result.failure(IOException("Simulated network timeout"))
        return Result.success(remoteAuditLogsToReturn)
    }

    override suspend fun fetchRegistrationTransactionsSince(timestamp: Long): Result<List<SupabaseRegistrationTransactionDto>> {
        if (shouldFailNextOperation) return Result.failure(IOException("Simulated network timeout"))
        return Result.success(remoteTransactionsToReturn)
    }
}
