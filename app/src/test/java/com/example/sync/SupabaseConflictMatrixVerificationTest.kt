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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * PHASE S1.4-E — AUTHORITATIVE CONFLICT MATRIX VERIFICATION TESTS
 * 
 * Verifies all 11 distributed conflict, race condition, clock skew, and reliability edge cases:
 * 1. Device A offline update vs Device B online update
 * 2. Same parcel updated simultaneously
 * 3. Terminal parcel state vs stale non-terminal state
 * 4. Hub update conflict
 * 5. User profile update conflict
 * 6. Duplicate audit log
 * 7. Duplicate registration transaction
 * 8. Sync retry after process restart
 * 9. Crash between remote success and local markAsSynced (Idempotent retry)
 * 10. Remote record received twice
 * 11. Clock skew between devices (Lifecycle regression prevention)
 */
@RunWith(RobolectricTestRunner::class)
class SupabaseConflictMatrixVerificationTest {

    private lateinit var database: PudoDatabase
    private lateinit var sharedCloud: FakeSupabaseCloudBackend
    private lateinit var syncEngine: PudoSyncEngine

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = PudoDatabase.createInMemory(context)
        sharedCloud = FakeSupabaseCloudBackend()
        syncEngine = PudoSyncEngine(database, sharedCloud)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // =========================================================================
    // CASE 1: Device A offline update vs Device B online update
    // =========================================================================
    @Test
    fun testCase1_deviceAOfflineUpdateVsDeviceBOnlineUpdate() = runBlocking {
        // Device A has parcel locally in HUB_SELECTED at t=1000
        val parcelA = ParcelEntity(
            id = "pcl-case1",
            trackingNumber = "TRK-CASE-1",
            senderName = "Sender A",
            recipientName = "Recipient A",
            recipientPhone = "09121111111",
            recipientPostalCode = "1111111111",
            status = ParcelStatus.HUB_SELECTED.name,
            size = ParcelSize.MEDIUM.name,
            registrationSource = RegistrationSource.CUSTOMER_REQUEST.name,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        database.parcelDao().insertParcel(parcelA)

        // Device B (online) advanced the parcel to STORED_AT_HUB at t=2000 and pushed to cloud
        val parcelBRemote = SupabaseParcelDto(
            id = "pcl-case1",
            trackingNumber = "TRK-CASE-1",
            senderName = "Sender A",
            recipientName = "Recipient A",
            recipientPhone = "09121111111",
            recipientPostalCode = "1111111111",
            status = ParcelStatus.STORED_AT_HUB.name,
            hubId = "hub-tehran-1",
            hubName = "هاب ونک",
            createdAt = 1000L,
            updatedAt = 2000L
        )
        sharedCloud.upsertParcel(parcelBRemote)

        // Device A comes online and syncs delta
        val syncResult = syncEngine.performInboundDeltaSync()
        assertEquals(1, syncResult.parcelsSynced)

        // Device A local state should converge to Device B's newer online update
        val resolved = database.parcelDao().getParcelByIdDirect("pcl-case1")
        assertNotNull(resolved)
        assertEquals(ParcelStatus.STORED_AT_HUB.name, resolved!!.status)
        assertEquals(2000L, resolved.updatedAt)
    }

    // =========================================================================
    // CASE 2: Same parcel updated simultaneously
    // =========================================================================
    @Test
    fun testCase2_sameParcelUpdatedSimultaneously() = runBlocking {
        val baseParcel = ParcelEntity(
            id = "pcl-case2",
            trackingNumber = "TRK-CASE-2",
            senderName = "Sender B",
            recipientName = "Recipient B",
            recipientPhone = "09122222222",
            recipientPostalCode = "2222222222",
            status = ParcelStatus.ELIGIBLE_FOR_HUB.name,
            size = ParcelSize.SMALL.name,
            registrationSource = RegistrationSource.CUSTOMER_REQUEST.name,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        database.parcelDao().insertParcel(baseParcel)

        // Simultaneous update 1: Device B sets HUB_SELECTED at t=1500
        val updateB = SupabaseParcelDto(
            id = "pcl-case2",
            trackingNumber = "TRK-CASE-2",
            senderName = "Sender B",
            recipientName = "Recipient B",
            recipientPhone = "09122222222",
            recipientPostalCode = "2222222222",
            status = ParcelStatus.HUB_SELECTED.name,
            hubId = "hub-2",
            updatedAt = 1500L
        )
        sharedCloud.upsertParcel(updateB)
        syncEngine.performInboundDeltaSync()

        // Simultaneous update 2: Device C sets STORED_AT_HUB at t=1800
        val updateC = SupabaseParcelDto(
            id = "pcl-case2",
            trackingNumber = "TRK-CASE-2",
            senderName = "Sender B",
            recipientName = "Recipient B",
            recipientPhone = "09122222222",
            recipientPostalCode = "2222222222",
            status = ParcelStatus.STORED_AT_HUB.name,
            hubId = "hub-2",
            updatedAt = 1800L
        )
        sharedCloud.upsertParcel(updateC)
        syncEngine.performInboundDeltaSync()

        val finalParcel = database.parcelDao().getParcelByIdDirect("pcl-case2")
        assertNotNull(finalParcel)
        assertEquals(ParcelStatus.STORED_AT_HUB.name, finalParcel!!.status)
        assertEquals(1800L, finalParcel.updatedAt)
    }

    // =========================================================================
    // CASE 3: Terminal parcel state vs stale non-terminal state
    // =========================================================================
    @Test
    fun testCase3_terminalParcelStateVsStaleNonTerminalState() = runBlocking {
        // Local parcel has reached terminal status DELIVERED_TO_CUSTOMER
        val terminalParcel = ParcelEntity(
            id = "pcl-case3",
            trackingNumber = "TRK-CASE-3",
            senderName = "Sender C",
            recipientName = "Recipient C",
            recipientPhone = "09123333333",
            recipientPostalCode = "3333333333",
            status = ParcelStatus.DELIVERED_TO_CUSTOMER.name,
            size = ParcelSize.LARGE.name,
            registrationSource = RegistrationSource.CUSTOMER_REQUEST.name,
            createdAt = 1000L,
            updatedAt = 5000L
        )
        database.parcelDao().insertParcel(terminalParcel)

        // Incoming remote delta attempts to revert to STORED_AT_HUB (even with a higher timestamp t=9000)
        val staleRemoteDto = SupabaseParcelDto(
            id = "pcl-case3",
            trackingNumber = "TRK-CASE-3",
            senderName = "Sender C",
            recipientName = "Recipient C",
            recipientPhone = "09123333333",
            recipientPostalCode = "3333333333",
            status = ParcelStatus.STORED_AT_HUB.name,
            createdAt = 1000L,
            updatedAt = 9000L
        )
        sharedCloud.upsertParcel(staleRemoteDto)

        val inboundResult = syncEngine.performInboundDeltaSync()
        assertEquals(0, inboundResult.parcelsSynced)

        val localAfterSync = database.parcelDao().getParcelByIdDirect("pcl-case3")
        assertNotNull(localAfterSync)
        assertEquals(ParcelStatus.DELIVERED_TO_CUSTOMER.name, localAfterSync!!.status)
        assertEquals(5000L, localAfterSync.updatedAt)
    }

    // =========================================================================
    // CASE 4: Hub update conflict
    // =========================================================================
    @Test
    fun testCase4_hubUpdateConflict() = runBlocking {
        // Local Hub updated at t=3000 to isOpen = false
        val localHub = HubEntity(
            id = "hub-case4",
            name = "هاب سعادت آباد",
            managerName = "مدیر هاب",
            phone = "02122000000",
            isOpen = false,
            createdAt = 1000L,
            updatedAt = 3000L
        )
        database.hubDao().insertHub(localHub)

        // Remote has older hub state from t=2000 with isOpen = true
        val staleRemoteHub = SupabaseHubDto(
            id = "hub-case4",
            name = "هاب سعادت آباد",
            managerName = "مدیر هاب",
            phone = "02122000000",
            isOpen = true,
            createdAt = 1000L,
            updatedAt = 2000L
        )
        sharedCloud.upsertHub(staleRemoteHub)

        syncEngine.performInboundDeltaSync()

        // Local newer state must be preserved
        val hubInDb = database.hubDao().getHubByIdDirect("hub-case4")
        assertNotNull(hubInDb)
        assertFalse(hubInDb!!.isOpen)
        assertEquals(3000L, hubInDb.updatedAt)
    }

    // =========================================================================
    // CASE 5: User profile update conflict
    // =========================================================================
    @Test
    fun testCase5_userProfileUpdateConflict() = runBlocking {
        // Local user has passwordHash and is APPROVED at t=4000
        val localUser = UserEntity(
            id = "usr-case5",
            username = "courier_ali",
            passwordHash = "argon2_local_secret_hash",
            fullName = "علی کریمی",
            phone = "09124445566",
            nationalId = "0055566677",
            role = "COURIER",
            approvalStatus = AccountApprovalStatus.APPROVED.name,
            createdAt = 1000L,
            updatedAt = 4000L
        )
        database.userDao().insertUser(localUser)

        // Remote delta arrives with empty password hash and newer timestamp t=5000
        val remoteUser = SupabaseUserDto(
            id = "usr-case5",
            username = "courier_ali",
            passwordHash = "", // remote postgrest query omitted secret password hash
            fullName = "علی کریمی (ویرایش شده)",
            phone = "09124445566",
            nationalId = "0055566677",
            role = "COURIER",
            approvalStatus = AccountApprovalStatus.APPROVED.name,
            createdAt = 1000L,
            updatedAt = 5000L
        )
        sharedCloud.upsertUser(remoteUser)

        val syncResult = syncEngine.performInboundDeltaSync()
        assertEquals(1, syncResult.usersSynced)

        val userInDb = database.userDao().getUserById("usr-case5")
        assertNotNull(userInDb)
        assertEquals("علی کریمی (ویرایش شده)", userInDb!!.fullName)
        // Verify local password hash is never blanked out by remote delta
        assertEquals("argon2_local_secret_hash", userInDb.passwordHash)
        assertEquals(5000L, userInDb.updatedAt)
    }

    // =========================================================================
    // CASE 6: Duplicate audit log
    // =========================================================================
    @Test
    fun testCase6_duplicateAuditLog() = runBlocking {
        val auditLogDto = SupabaseAuditLogDto(
            id = "audit-dup-101",
            eventType = "PARCEL_SCANNED",
            actorId = "usr-1",
            actorRole = "COURIER",
            entityId = "pcl-101",
            oldState = "OUT_FOR_DELIVERY",
            newState = "STORED_AT_HUB",
            timestamp = 1000L
        )
        sharedCloud.pushAuditLog(auditLogDto)

        // First sync
        val sync1 = syncEngine.performInboundDeltaSync()
        assertEquals(1, sync1.auditLogsSynced)

        // Second sync receives the same audit log
        val sync2 = syncEngine.performInboundDeltaSync()
        // Should not crash and logs list in Room must have exactly 1 record
        val allLogs = database.auditLogDao().getAllLogs().first()
        assertEquals(1, allLogs.size)
        assertEquals("audit-dup-101", allLogs[0].id)
    }

    // =========================================================================
    // CASE 7: Duplicate registration transaction
    // =========================================================================
    @Test
    fun testCase7_duplicateRegistrationTransaction() = runBlocking {
        val txDto = SupabaseRegistrationTransactionDto(
            transactionId = "tx-dup-202",
            parcelId = "pcl-202",
            trackingNumber = "TRK-DUP-202",
            courierId = "courier-1",
            hubId = "hub-1",
            registrationSource = RegistrationSource.CUSTOMER_REQUEST.name,
            timestamp = 2000L
        )
        sharedCloud.pushRegistrationTransaction(txDto)

        // First inbound sync
        val sync1 = syncEngine.performInboundDeltaSync()
        assertEquals(1, sync1.transactionsSynced)

        // Push duplicate transaction to cloud
        sharedCloud.pushRegistrationTransaction(txDto)
        val sync2 = syncEngine.performInboundDeltaSync()

        // Room must contain exactly 1 transaction receipt
        val allTxs = database.registrationTransactionDao().getAllTransactions().first()
        assertEquals(1, allTxs.size)
        assertEquals("tx-dup-202", allTxs[0].transactionId)
    }

    // =========================================================================
    // CASE 8: Sync retry after process restart
    // =========================================================================
    @Test
    fun testCase8_syncRetryAfterProcessRestart() = runBlocking {
        // Enqueue an outbound parcel item
        val parcel = ParcelEntity(
            id = "pcl-case8",
            trackingNumber = "TRK-CASE-8",
            senderName = "Sender 8",
            recipientName = "Recipient 8",
            recipientPhone = "09128888888",
            recipientPostalCode = "8888888888",
            status = ParcelStatus.OUT_FOR_DELIVERY.name,
            size = ParcelSize.MEDIUM.name,
            registrationSource = RegistrationSource.CUSTOMER_REQUEST.name,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        database.parcelDao().insertParcel(parcel)

        val queueItem = SyncQueueEntity(
            id = "q-case8",
            entityType = "PARCEL",
            entityId = parcel.id,
            action = "CREATE",
            payloadJson = "{\"id\":\"${parcel.id}\"}",
            createdAt = 1000L
        )
        database.syncQueueDao().insertSyncItem(queueItem)

        // Simulate network failure on initial sync attempt
        sharedCloud.shouldFailNextRequest = true
        val result1 = syncEngine.processPendingSyncQueue()
        assertEquals(1, result1.failureCount)
        assertEquals(0, result1.successCount)

        val queueAfterFailure = database.syncQueueDao().getPendingSyncItemsDirect()
        assertEquals(1, queueAfterFailure.size)
        assertEquals(1, queueAfterFailure[0].retryCount)

        // Simulate Process Restart: instantiate new PudoSyncEngine on the same persistent database
        val newEngineAfterRestart = PudoSyncEngine(database, sharedCloud)
        sharedCloud.shouldFailNextRequest = false

        // Run sync on restarted engine
        val result2 = newEngineAfterRestart.processPendingSyncQueue()
        assertEquals(1, result2.successCount)
        assertEquals(0, result2.failureCount)

        // Queue item should now be marked synced
        val pendingAfterSuccess = database.syncQueueDao().getPendingSyncItemsDirect()
        assertEquals(0, pendingAfterSuccess.size)
    }

    // =========================================================================
    // CASE 9: Crash between remote success and local markAsSynced (Idempotent retry)
    // =========================================================================
    @Test
    fun testCase9_crashBetweenRemoteSuccessAndLocalMarkAsSynced() = runBlocking {
        val parcel = ParcelEntity(
            id = "pcl-case9",
            trackingNumber = "TRK-CASE-9",
            senderName = "Sender 9",
            recipientName = "Recipient 9",
            recipientPhone = "09129999999",
            recipientPostalCode = "9999999999",
            status = ParcelStatus.STORED_AT_HUB.name,
            size = ParcelSize.SMALL.name,
            registrationSource = RegistrationSource.CUSTOMER_REQUEST.name,
            createdAt = 1000L,
            updatedAt = 2000L
        )
        database.parcelDao().insertParcel(parcel)

        val queueItem = SyncQueueEntity(
            id = "q-case9",
            entityType = "PARCEL",
            entityId = parcel.id,
            action = "UPDATE",
            payloadJson = "{\"id\":\"${parcel.id}\"}",
            createdAt = 2000L
        )
        database.syncQueueDao().insertSyncItem(queueItem)

        // Remote receives the record directly before simulated crash
        sharedCloud.upsertParcel(SupabaseParcelDto.fromEntity(parcel))
        assertTrue(sharedCloud.parcels.containsKey("pcl-case9"))

        // Retry occurs (the queue item was not marked synced before process was killed)
        val syncResult = syncEngine.processPendingSyncQueue()
        assertEquals(1, syncResult.successCount)

        // Verify remote record is intact and queue item is cleanly resolved
        assertEquals(0, database.syncQueueDao().getPendingSyncItemsDirect().size)
        assertEquals(ParcelStatus.STORED_AT_HUB.name, sharedCloud.parcels["pcl-case9"]?.status)
    }

    // =========================================================================
    // CASE 10: Remote record received twice
    // =========================================================================
    @Test
    fun testCase10_remoteRecordReceivedTwice() = runBlocking {
        val remoteParcel = SupabaseParcelDto(
            id = "pcl-case10",
            trackingNumber = "TRK-CASE-10",
            senderName = "Sender 10",
            recipientName = "Recipient 10",
            recipientPhone = "09120001122",
            recipientPostalCode = "1010101010",
            status = ParcelStatus.OUT_FOR_DELIVERY.name,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        sharedCloud.upsertParcel(remoteParcel)

        // Pull 1
        val res1 = syncEngine.performInboundDeltaSync()
        assertEquals(1, res1.parcelsSynced)

        // Pull 2 (identical data)
        val res2 = syncEngine.performInboundDeltaSync()
        // Checkpoint advanced, second pull is idempotent and clean
        val countInDb = database.parcelDao().getAllParcelsDirect().count { it.id == "pcl-case10" }
        assertEquals(1, countInDb)
    }

    // =========================================================================
    // CASE 11: Clock skew between devices (Lifecycle regression prevention)
    // =========================================================================
    @Test
    fun testCase11_clockSkewBetweenDevicesPreventsLifecycleRegression() = runBlocking {
        // Device A has clock running fast: t=10000, status = STORED_AT_HUB
        val localParcelA = ParcelEntity(
            id = "pcl-case11",
            trackingNumber = "TRK-CASE-11",
            senderName = "Sender 11",
            recipientName = "Recipient 11",
            recipientPhone = "09123334455",
            recipientPostalCode = "1111222233",
            status = ParcelStatus.STORED_AT_HUB.name,
            size = ParcelSize.MEDIUM.name,
            registrationSource = RegistrationSource.CUSTOMER_REQUEST.name,
            createdAt = 1000L,
            updatedAt = 10000L
        )
        database.parcelDao().insertParcel(localParcelA)

        // Device B has clock skewed behind or sends invalid backward transition:
        // Even if remote timestamp is t=12000, it sends OUT_FOR_DELIVERY
        val regressiveRemote = SupabaseParcelDto(
            id = "pcl-case11",
            trackingNumber = "TRK-CASE-11",
            senderName = "Sender 11",
            recipientName = "Recipient 11",
            recipientPhone = "09123334455",
            recipientPostalCode = "1111222233",
            status = ParcelStatus.OUT_FOR_DELIVERY.name,
            createdAt = 1000L,
            updatedAt = 12000L
        )

        // Guard must return false because STORED_AT_HUB -> OUT_FOR_DELIVERY is an illegal regression
        val canApply = syncEngine.shouldApplyRemoteParcel(localParcelA, regressiveRemote)
        assertFalse("Illegal status transition must be rejected despite clock skew", canApply)

        sharedCloud.upsertParcel(regressiveRemote)
        syncEngine.performInboundDeltaSync()

        // Local state remains STORED_AT_HUB
        val localAfter = database.parcelDao().getParcelByIdDirect("pcl-case11")
        assertNotNull(localAfter)
        assertEquals(ParcelStatus.STORED_AT_HUB.name, localAfter!!.status)
        assertEquals(10000L, localAfter.updatedAt)
    }

    /**
     * Fake Supabase PostgREST cloud backend for deterministic conflict testing.
     */
    private class FakeSupabaseCloudBackend : RemoteSyncDataSource {
        val users = ConcurrentHashMap<String, SupabaseUserDto>()
        val hubs = ConcurrentHashMap<String, SupabaseHubDto>()
        val parcels = ConcurrentHashMap<String, SupabaseParcelDto>()
        val auditLogs = CopyOnWriteArrayList<SupabaseAuditLogDto>()
        val registrationTransactions = CopyOnWriteArrayList<SupabaseRegistrationTransactionDto>()

        var shouldFailNextRequest = false

        override suspend fun upsertUser(userDto: SupabaseUserDto): Result<SupabaseUserDto> {
            if (shouldFailNextRequest) return Result.failure(IOException("Simulated network timeout"))
            users[userDto.id] = userDto
            return Result.success(userDto)
        }

        override suspend fun upsertHub(hubDto: SupabaseHubDto): Result<SupabaseHubDto> {
            if (shouldFailNextRequest) return Result.failure(IOException("Simulated network timeout"))
            hubs[hubDto.id] = hubDto
            return Result.success(hubDto)
        }

        override suspend fun upsertParcel(parcelDto: SupabaseParcelDto): Result<SupabaseParcelDto> {
            if (shouldFailNextRequest) return Result.failure(IOException("Simulated network timeout"))
            parcels[parcelDto.id] = parcelDto
            return Result.success(parcelDto)
        }

        override suspend fun pushAuditLog(auditLogDto: SupabaseAuditLogDto): Result<SupabaseAuditLogDto> {
            if (shouldFailNextRequest) return Result.failure(IOException("Simulated network timeout"))
            if (auditLogs.none { it.id == auditLogDto.id }) {
                auditLogs.add(auditLogDto)
            }
            return Result.success(auditLogDto)
        }

        override suspend fun pushRegistrationTransaction(txDto: SupabaseRegistrationTransactionDto): Result<SupabaseRegistrationTransactionDto> {
            if (shouldFailNextRequest) return Result.failure(IOException("Simulated network timeout"))
            if (registrationTransactions.none { it.transactionId == txDto.transactionId }) {
                registrationTransactions.add(txDto)
            }
            return Result.success(txDto)
        }

        override suspend fun deleteUser(userId: String): Result<Boolean> {
            if (shouldFailNextRequest) return Result.failure(IOException("Simulated network timeout"))
            users.remove(userId)
            return Result.success(true)
        }

        override suspend fun deleteHub(hubId: String): Result<Boolean> {
            if (shouldFailNextRequest) return Result.failure(IOException("Simulated network timeout"))
            hubs.remove(hubId)
            return Result.success(true)
        }

        override suspend fun deleteParcel(parcelId: String): Result<Boolean> {
            if (shouldFailNextRequest) return Result.failure(IOException("Simulated network timeout"))
            parcels.remove(parcelId)
            return Result.success(true)
        }

        override suspend fun fetchUsersUpdatedSince(timestamp: Long): Result<List<SupabaseUserDto>> {
            if (shouldFailNextRequest) return Result.failure(IOException("Simulated network timeout"))
            return Result.success(users.values.filter { it.updatedAt >= timestamp }.sortedBy { it.updatedAt })
        }

        override suspend fun fetchHubsUpdatedSince(timestamp: Long): Result<List<SupabaseHubDto>> {
            if (shouldFailNextRequest) return Result.failure(IOException("Simulated network timeout"))
            return Result.success(hubs.values.filter { it.updatedAt >= timestamp }.sortedBy { it.updatedAt })
        }

        override suspend fun fetchParcelsUpdatedSince(timestamp: Long): Result<List<SupabaseParcelDto>> {
            if (shouldFailNextRequest) return Result.failure(IOException("Simulated network timeout"))
            return Result.success(parcels.values.filter { it.updatedAt >= timestamp }.sortedBy { it.updatedAt })
        }

        override suspend fun fetchAuditLogsSince(timestamp: Long): Result<List<SupabaseAuditLogDto>> {
            if (shouldFailNextRequest) return Result.failure(IOException("Simulated network timeout"))
            return Result.success(auditLogs.filter { it.timestamp >= timestamp }.sortedBy { it.timestamp })
        }

        override suspend fun fetchRegistrationTransactionsSince(timestamp: Long): Result<List<SupabaseRegistrationTransactionDto>> {
            if (shouldFailNextRequest) return Result.failure(IOException("Simulated network timeout"))
            return Result.success(registrationTransactions.filter { it.timestamp >= timestamp }.sortedBy { it.timestamp })
        }
    }
}
