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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Multi-Device End-to-End Simulation Test.
 * 
 * Verifies real-world distributed sync behavior between multiple Android client devices
 * and a shared Supabase PostgREST cloud backend:
 * 
 * 1. Device A (Courier) creates local parcel -> pushes to Cloud.
 * 2. Device B (Hub Manager / Customer) polls inbound delta -> Room merges & Flow emits to UI.
 * 3. Offline conflict reconciliation:
 *    - Device A goes offline, attempts invalid backward transition or older timestamp edit.
 *    - Device B advances parcel to terminal state (DELIVERED_TO_CUSTOMER) -> pushes to Cloud.
 *    - Device A comes back online -> performs delta sync -> terminal state preserved, regression rejected.
 * 4. Multi-device user registration & remote approval propagation (Courier signup on Device A -> Admin approval on Device B).
 * 5. Multi-device distributed audit log and registration receipt convergence across all devices.
 */
@RunWith(RobolectricTestRunner::class)
class MultiDeviceSyncSimulationTest {

    private lateinit var deviceADatabase: PudoDatabase
    private lateinit var deviceBDatabase: PudoDatabase
    private lateinit var sharedCloudBackend: SharedInMemoryCloudBackend

    private lateinit var deviceASyncEngine: PudoSyncEngine
    private lateinit var deviceBSyncEngine: PudoSyncEngine

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        deviceADatabase = PudoDatabase.createInMemory(context)
        deviceBDatabase = PudoDatabase.createInMemory(context)

        sharedCloudBackend = SharedInMemoryCloudBackend()

        deviceASyncEngine = PudoSyncEngine(deviceADatabase, sharedCloudBackend)
        deviceBSyncEngine = PudoSyncEngine(deviceBDatabase, sharedCloudBackend)
    }

    @After
    fun tearDown() {
        deviceADatabase.close()
        deviceBDatabase.close()
    }

    // =========================================================================
    // 1. DEVICE A -> CLOUD -> DEVICE B END-TO-END PARCEL SYNC
    // =========================================================================
    @Test
    fun testDeviceAToCloudToDeviceBEndToEndSync() = runBlocking {
        // Step 1: Device A creates a parcel locally and enqueues sync
        val parcelOnDeviceA = ParcelEntity(
            id = "pcl-multidev-101",
            trackingNumber = "TRK-MULTI-101",
            senderName = "Tehran Bookseller",
            recipientName = "Saeed Rezaei",
            recipientPhone = "09129998877",
            recipientAddress = "Vanak Sq, Tehran",
            recipientPostalCode = "1999912345",
            status = ParcelStatus.OUT_FOR_DELIVERY.name,
            size = ParcelSize.MEDIUM.name,
            registrationSource = RegistrationSource.CUSTOMER_REQUEST.name,
            baseFee = 35000L,
            isSettled = false,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        deviceADatabase.parcelDao().insertParcel(parcelOnDeviceA)

        val queueItemA = SyncQueueEntity(
            id = "q-devA-101",
            entityType = "PARCEL",
            entityId = parcelOnDeviceA.id,
            action = "CREATE",
            payloadJson = "{\"id\":\"${parcelOnDeviceA.id}\",\"tracking_number\":\"${parcelOnDeviceA.trackingNumber}\",\"sender_name\":\"${parcelOnDeviceA.senderName}\",\"recipient_name\":\"${parcelOnDeviceA.recipientName}\",\"recipient_phone\":\"${parcelOnDeviceA.recipientPhone}\",\"recipient_address\":\"${parcelOnDeviceA.recipientAddress}\",\"recipient_postal_code\":\"${parcelOnDeviceA.recipientPostalCode}\",\"status\":\"OUT_FOR_DELIVERY\",\"size\":\"MEDIUM\",\"registration_source\":\"CUSTOMER_REQUEST\",\"base_fee\":35000,\"is_settled\":false,\"created_at\":1000,\"updated_at\":1000}",
            createdAt = 1000L
        )
        deviceADatabase.syncQueueDao().insertSyncItem(queueItemA)

        // Step 2: Device A performs full sync -> outbound push to Cloud
        val deviceASyncResult = deviceASyncEngine.synchronize()
        assertTrue(deviceASyncResult.isSuccess)
        assertEquals(1, deviceASyncResult.outbound.successCount)
        assertEquals(0, deviceADatabase.syncQueueDao().getPendingSyncItemsDirect().size)

        // Verify parcel is stored in Cloud backend
        assertTrue(sharedCloudBackend.parcels.containsKey("pcl-multidev-101"))

        // Step 3: Device B has empty local database initially
        val deviceBParcelsBefore = deviceBDatabase.parcelDao().getAllParcels().first()
        assertTrue(deviceBParcelsBefore.isEmpty())

        // Step 4: Device B performs inbound delta sync
        val deviceBInboundResult = deviceBSyncEngine.performInboundDeltaSync()
        assertEquals(1, deviceBInboundResult.parcelsSynced)

        // Step 5: Device B observes parcel in its Room database via Flow
        val deviceBParcelsAfter = deviceBDatabase.parcelDao().getAllParcels().first()
        assertEquals(1, deviceBParcelsAfter.size)
        val parcelOnDeviceB = deviceBParcelsAfter[0]
        assertEquals("pcl-multidev-101", parcelOnDeviceB.id)
        assertEquals("TRK-MULTI-101", parcelOnDeviceB.trackingNumber)
        assertEquals("Tehran Bookseller", parcelOnDeviceB.senderName)
        assertEquals("Saeed Rezaei", parcelOnDeviceB.recipientName)
        assertEquals(ParcelStatus.OUT_FOR_DELIVERY.name, parcelOnDeviceB.status)
    }

    // =========================================================================
    // 2. MULTI-DEVICE STATUS PROGRESSION & TERMINAL STATE CONFLICT RECONCILIATION
    // =========================================================================
    @Test
    fun testMultiDeviceStatusProgressionAndTerminalStateReconciliation() = runBlocking {
        // Initial setup: Both Device A and Device B have the parcel stored locally
        val initialParcel = ParcelEntity(
            id = "pcl-multidev-202",
            trackingNumber = "TRK-MULTI-202",
            senderName = "Digikala Center",
            recipientName = "Zahra Ahmadi",
            recipientPhone = "09125554433",
            recipientPostalCode = "1444455555",
            status = ParcelStatus.STORED_AT_HUB.name,
            size = ParcelSize.SMALL.name,
            registrationSource = RegistrationSource.CUSTOMER_REQUEST.name,
            updatedAt = 2000L
        )
        deviceADatabase.parcelDao().insertParcel(initialParcel)
        deviceBDatabase.parcelDao().insertParcel(initialParcel)

        // Device B delivers parcel to customer (Terminal state) and pushes to Cloud at time 5000L
        val deliveredParcelB = initialParcel.copy(
            status = ParcelStatus.DELIVERED_TO_CUSTOMER.name,
            updatedAt = 5000L
        )
        deviceBDatabase.parcelDao().updateParcel(deliveredParcelB)

        val queueItemB = SyncQueueEntity(
            id = "q-devB-202",
            entityType = "PARCEL",
            entityId = deliveredParcelB.id,
            action = "UPDATE",
            payloadJson = "{\"id\":\"${deliveredParcelB.id}\",\"tracking_number\":\"${deliveredParcelB.trackingNumber}\",\"sender_name\":\"${deliveredParcelB.senderName}\",\"recipient_name\":\"${deliveredParcelB.recipientName}\",\"recipient_phone\":\"${deliveredParcelB.recipientPhone}\",\"recipient_postal_code\":\"${deliveredParcelB.recipientPostalCode}\",\"status\":\"DELIVERED_TO_CUSTOMER\",\"size\":\"SMALL\",\"registration_source\":\"CUSTOMER_REQUEST\",\"updated_at\":5000}",
            createdAt = 5000L
        )
        deviceBDatabase.syncQueueDao().insertSyncItem(queueItemB)
        deviceBSyncEngine.processPendingSyncQueue()

        // Device A (which was offline) comes online and syncs inbound delta
        val syncResultA = deviceASyncEngine.performInboundDeltaSync()
        assertEquals(1, syncResultA.parcelsSynced)

        // Verify Device A now reflects DELIVERED_TO_CUSTOMER
        val parcelOnDeviceA = deviceADatabase.parcelDao().getParcelByIdDirect("pcl-multidev-202")
        assertNotNull(parcelOnDeviceA)
        assertEquals(ParcelStatus.DELIVERED_TO_CUSTOMER.name, parcelOnDeviceA!!.status)
        assertEquals(5000L, parcelOnDeviceA.updatedAt)

        // Now simulate a stale/regressive remote push from an old cache or malicious attempt
        val staleParcelDto = SupabaseParcelDto(
            id = "pcl-multidev-202",
            trackingNumber = "TRK-MULTI-202",
            senderName = "Digikala Center",
            recipientName = "Zahra Ahmadi",
            recipientPhone = "09125554433",
            recipientPostalCode = "1444455555",
            status = ParcelStatus.STORED_AT_HUB.name,
            updatedAt = 6000L
        )
        assertFalse(deviceASyncEngine.shouldApplyRemoteParcel(parcelOnDeviceA, staleParcelDto))
    }

    // =========================================================================
    // 3. USER REGISTRATION ON DEVICE A -> ADMIN APPROVAL ON DEVICE B -> SYNC
    // =========================================================================
    @Test
    fun testUserRegistrationOnDeviceAAdminApprovalOnDeviceB() = runBlocking {
        // Device A registers a new Courier account
        val newCourier = UserEntity(
            id = "usr-courier-303",
            username = "courier_hassan",
            passwordHash = "local_secure_argon2_hash_deviceA",
            fullName = "Hassan Rostami",
            phone = "09127778899",
            nationalId = "0012345678",
            role = "COURIER",
            approvalStatus = AccountApprovalStatus.PENDING.name,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        deviceADatabase.userDao().insertUser(newCourier)

        val queueItemA = SyncQueueEntity(
            id = "q-user-303",
            entityType = "USER",
            entityId = newCourier.id,
            action = "CREATE",
            payloadJson = "{\"id\":\"${newCourier.id}\",\"username\":\"${newCourier.username}\",\"password_hash\":\"\",\"full_name\":\"${newCourier.fullName}\",\"phone\":\"${newCourier.phone}\",\"national_id\":\"${newCourier.nationalId}\",\"role\":\"COURIER\",\"approval_status\":\"PENDING\",\"created_at\":1000,\"updated_at\":1000}",
            createdAt = 1000L
        )
        deviceADatabase.syncQueueDao().insertSyncItem(queueItemA)

        // Device A pushes user to Cloud
        deviceASyncEngine.processPendingSyncQueue()

        // Device B (System Admin) syncs inbound delta
        val syncAdmin = deviceBSyncEngine.performInboundDeltaSync()
        assertEquals(1, syncAdmin.usersSynced)

        val userOnAdminDevice = deviceBDatabase.userDao().getUserById("usr-courier-303")
        assertNotNull(userOnAdminDevice)
        assertEquals(AccountApprovalStatus.PENDING.name, userOnAdminDevice!!.approvalStatus)

        // Device B approves Courier and pushes update to Cloud
        val approvedUser = userOnAdminDevice.copy(
            approvalStatus = AccountApprovalStatus.APPROVED.name,
            updatedAt = 2500L
        )
        deviceBDatabase.userDao().updateUser(approvedUser)

        val queueItemAdmin = SyncQueueEntity(
            id = "q-admin-approve-303",
            entityType = "USER",
            entityId = approvedUser.id,
            action = "UPDATE",
            payloadJson = "{\"id\":\"${approvedUser.id}\",\"username\":\"${approvedUser.username}\",\"password_hash\":\"\",\"full_name\":\"${approvedUser.fullName}\",\"phone\":\"${approvedUser.phone}\",\"national_id\":\"${approvedUser.nationalId}\",\"role\":\"COURIER\",\"approval_status\":\"APPROVED\",\"updated_at\":2500}",
            createdAt = 2500L
        )
        deviceBDatabase.syncQueueDao().insertSyncItem(queueItemAdmin)
        deviceBSyncEngine.processPendingSyncQueue()

        // Device A syncs inbound delta -> receives APPROVED status
        val syncCourierDevice = deviceASyncEngine.performInboundDeltaSync()
        assertEquals(1, syncCourierDevice.usersSynced)

        val courierOnDeviceA = deviceADatabase.userDao().getUserById("usr-courier-303")
        assertNotNull(courierOnDeviceA)
        assertEquals(AccountApprovalStatus.APPROVED.name, courierOnDeviceA!!.approvalStatus)
        // Verify local password hash on Device A is securely preserved
        assertEquals("local_secure_argon2_hash_deviceA", courierOnDeviceA.passwordHash)
        assertEquals(2500L, courierOnDeviceA.updatedAt)
    }

    // =========================================================================
    // 4. DISTRIBUTED AUDIT LOG & TRANSACTION RECEIPTS CONVERGENCE
    // =========================================================================
    @Test
    fun testDistributedAuditLogAndTransactionReceiptsConvergence() = runBlocking {
        // Device A records Log 1 & Tx 1
        val logA = AuditLogEntity(
            id = "audit-devA-401",
            eventType = "PARCEL_ACCEPTED",
            actorId = "usr-devA",
            actorRole = "COURIER",
            entityId = "pcl-401",
            oldState = "CREATED",
            newState = "STORED_AT_HUB",
            transactionId = "tx-devA-401",
            timestamp = 1000L
        )
        val txA = RegistrationTransactionEntity(
            transactionId = "tx-devA-401",
            parcelId = "pcl-401",
            trackingNumber = "TRK-401",
            courierId = "usr-devA",
            hubId = "hub-tehran-1",
            registrationSource = RegistrationSource.CUSTOMER_REQUEST.name,
            timestamp = 1000L
        )
        deviceADatabase.auditLogDao().insertLog(logA)
        deviceADatabase.registrationTransactionDao().insertTransaction(txA)

        val queueLogA = SyncQueueEntity(
            id = "q-logA",
            entityType = "AUDIT_LOG",
            entityId = logA.id,
            action = "CREATE",
            payloadJson = "{\"id\":\"${logA.id}\",\"event_type\":\"${logA.eventType}\",\"actor_id\":\"${logA.actorId}\",\"actor_role\":\"${logA.actorRole}\",\"entity_id\":\"${logA.entityId}\",\"old_state\":\"CREATED\",\"new_state\":\"STORED_AT_HUB\",\"transaction_id\":\"tx-devA-401\",\"timestamp\":1000}",
            createdAt = 1000L
        )
        val queueTxA = SyncQueueEntity(
            id = "q-txA",
            entityType = "REGISTRATION_TRANSACTION",
            entityId = txA.transactionId,
            action = "CREATE",
            payloadJson = "{\"transaction_id\":\"${txA.transactionId}\",\"parcel_id\":\"${txA.parcelId}\",\"tracking_number\":\"${txA.trackingNumber}\",\"courier_id\":\"${txA.courierId}\",\"hub_id\":\"${txA.hubId}\",\"registration_source\":\"CUSTOMER_REQUEST\",\"timestamp\":1000}",
            createdAt = 1000L
        )
        deviceADatabase.syncQueueDao().insertSyncItem(queueLogA)
        deviceADatabase.syncQueueDao().insertSyncItem(queueTxA)

        // Device A pushes to cloud
        deviceASyncEngine.processPendingSyncQueue()

        // Device B records Log 2 & Tx 2
        val logB = AuditLogEntity(
            id = "audit-devB-402",
            eventType = "PARCEL_DELIVERED",
            actorId = "usr-devB",
            actorRole = "HUB_MANAGER",
            entityId = "pcl-402",
            oldState = "STORED_AT_HUB",
            newState = "DELIVERED_TO_CUSTOMER",
            transactionId = "tx-devB-402",
            timestamp = 2000L
        )
        val txB = RegistrationTransactionEntity(
            transactionId = "tx-devB-402",
            parcelId = "pcl-402",
            trackingNumber = "TRK-402",
            courierId = "usr-devB",
            hubId = "hub-tehran-2",
            registrationSource = RegistrationSource.FAILED_HOME_DELIVERY.name,
            timestamp = 2000L
        )
        deviceBDatabase.auditLogDao().insertLog(logB)
        deviceBDatabase.registrationTransactionDao().insertTransaction(txB)

        val queueLogB = SyncQueueEntity(
            id = "q-logB",
            entityType = "AUDIT_LOG",
            entityId = logB.id,
            action = "CREATE",
            payloadJson = "{\"id\":\"${logB.id}\",\"event_type\":\"${logB.eventType}\",\"actor_id\":\"${logB.actorId}\",\"actor_role\":\"${logB.actorRole}\",\"entity_id\":\"${logB.entityId}\",\"old_state\":\"STORED_AT_HUB\",\"new_state\":\"DELIVERED_TO_CUSTOMER\",\"transaction_id\":\"tx-devB-402\",\"timestamp\":2000}",
            createdAt = 2000L
        )
        val queueTxB = SyncQueueEntity(
            id = "q-txB",
            entityType = "REGISTRATION_TRANSACTION",
            entityId = txB.transactionId,
            action = "CREATE",
            payloadJson = "{\"transaction_id\":\"${txB.transactionId}\",\"parcel_id\":\"${txB.parcelId}\",\"tracking_number\":\"${txB.trackingNumber}\",\"courier_id\":\"${txB.courierId}\",\"hub_id\":\"${txB.hubId}\",\"registration_source\":\"FAILED_HOME_DELIVERY\",\"timestamp\":2000}",
            createdAt = 2000L
        )
        deviceBDatabase.syncQueueDao().insertSyncItem(queueLogB)
        deviceBDatabase.syncQueueDao().insertSyncItem(queueTxB)

        // Device B pushes to cloud
        deviceBSyncEngine.processPendingSyncQueue()

        // Both devices perform inbound delta sync
        deviceASyncEngine.performInboundDeltaSync()
        deviceBSyncEngine.performInboundDeltaSync()

        // Verify both devices have converged to have BOTH audit logs and BOTH registration transactions
        val logsOnA = deviceADatabase.auditLogDao().getAllLogs().first()
        val logsOnB = deviceBDatabase.auditLogDao().getAllLogs().first()

        assertEquals(2, logsOnA.size)
        assertEquals(2, logsOnB.size)
        assertTrue(logsOnA.any { it.id == "audit-devA-401" } && logsOnA.any { it.id == "audit-devB-402" })
        assertTrue(logsOnB.any { it.id == "audit-devA-401" } && logsOnB.any { it.id == "audit-devB-402" })

        val txsOnA = deviceADatabase.registrationTransactionDao().getAllTransactions().first()
        val txsOnB = deviceBDatabase.registrationTransactionDao().getAllTransactions().first()

        assertEquals(2, txsOnA.size)
        assertEquals(2, txsOnB.size)
        assertTrue(txsOnA.any { it.transactionId == "tx-devA-401" } && txsOnA.any { it.transactionId == "tx-devB-402" })
        assertTrue(txsOnB.any { it.transactionId == "tx-devA-401" } && txsOnB.any { it.transactionId == "tx-devB-402" })
    }

    /**
     * In-memory cloud simulation implementing RemoteSyncDataSource,
     * acting as the authoritative Supabase PostgREST database.
     */
    private class SharedInMemoryCloudBackend : RemoteSyncDataSource {
        val users = ConcurrentHashMap<String, SupabaseUserDto>()
        val hubs = ConcurrentHashMap<String, SupabaseHubDto>()
        val parcels = ConcurrentHashMap<String, SupabaseParcelDto>()
        val auditLogs = CopyOnWriteArrayList<SupabaseAuditLogDto>()
        val registrationTransactions = CopyOnWriteArrayList<SupabaseRegistrationTransactionDto>()

        override suspend fun upsertUser(userDto: SupabaseUserDto): Result<SupabaseUserDto> {
            users[userDto.id] = userDto
            return Result.success(userDto)
        }

        override suspend fun upsertHub(hubDto: SupabaseHubDto): Result<SupabaseHubDto> {
            hubs[hubDto.id] = hubDto
            return Result.success(hubDto)
        }

        override suspend fun upsertParcel(parcelDto: SupabaseParcelDto): Result<SupabaseParcelDto> {
            parcels[parcelDto.id] = parcelDto
            return Result.success(parcelDto)
        }

        override suspend fun pushAuditLog(auditLogDto: SupabaseAuditLogDto): Result<SupabaseAuditLogDto> {
            if (auditLogs.none { it.id == auditLogDto.id }) {
                auditLogs.add(auditLogDto)
            }
            return Result.success(auditLogDto)
        }

        override suspend fun pushRegistrationTransaction(txDto: SupabaseRegistrationTransactionDto): Result<SupabaseRegistrationTransactionDto> {
            if (registrationTransactions.none { it.transactionId == txDto.transactionId }) {
                registrationTransactions.add(txDto)
            }
            return Result.success(txDto)
        }

        override suspend fun deleteUser(userId: String): Result<Boolean> {
            users.remove(userId)
            return Result.success(true)
        }

        override suspend fun deleteHub(hubId: String): Result<Boolean> {
            hubs.remove(hubId)
            return Result.success(true)
        }

        override suspend fun deleteParcel(parcelId: String): Result<Boolean> {
            parcels.remove(parcelId)
            return Result.success(true)
        }

        override suspend fun fetchUsersUpdatedSince(timestamp: Long): Result<List<SupabaseUserDto>> {
            val list = users.values.filter { it.updatedAt >= timestamp }.sortedBy { it.updatedAt }
            return Result.success(list)
        }

        override suspend fun fetchHubsUpdatedSince(timestamp: Long): Result<List<SupabaseHubDto>> {
            val list = hubs.values.filter { it.updatedAt >= timestamp }.sortedBy { it.updatedAt }
            return Result.success(list)
        }

        override suspend fun fetchParcelsUpdatedSince(timestamp: Long): Result<List<SupabaseParcelDto>> {
            val list = parcels.values.filter { it.updatedAt >= timestamp }.sortedBy { it.updatedAt }
            return Result.success(list)
        }

        override suspend fun fetchAuditLogsSince(timestamp: Long): Result<List<SupabaseAuditLogDto>> {
            val list = auditLogs.filter { it.timestamp >= timestamp }.sortedBy { it.timestamp }
            return Result.success(list)
        }

        override suspend fun fetchRegistrationTransactionsSince(timestamp: Long): Result<List<SupabaseRegistrationTransactionDto>> {
            val list = registrationTransactions.filter { it.timestamp >= timestamp }.sortedBy { it.timestamp }
            return Result.success(list)
        }
    }
}
