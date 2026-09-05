package com.example.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.database.PudoDatabase
import com.example.data.local.entities.*
import com.example.data.model.AccountApprovalStatus
import com.example.data.model.ParcelSize
import com.example.data.model.ParcelStatus
import com.example.data.model.RegistrationSource
import com.example.data.remote.SupabaseConfig
import com.example.data.remote.SupabaseConnectivityVerifier
import com.example.data.remote.SupabaseRemoteSyncDataSourceImpl
import com.example.data.sync.PudoSyncEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class SupabaseOutboundPushVerificationTest {

    private lateinit var database: PudoDatabase
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun validConfig() = SupabaseConfig(
        url = "https://ndn-pudo-test.supabase.co",
        publishableKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test_anon_key_valid_length"
    )

    private fun createMockClient(
        statusCode: Int = 201,
        responseBody: String = "[]",
        onRequestIntercepted: ((Request) -> Unit)? = null
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                onRequestIntercepted?.invoke(request)

                if (statusCode >= 400) {
                    Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(statusCode)
                        .message("Error $statusCode")
                        .body(responseBody.toResponseBody(jsonMediaType))
                        .build()
                } else {
                    Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(statusCode)
                        .message("Success $statusCode")
                        .body(responseBody.toResponseBody(jsonMediaType))
                        .build()
                }
            }
            .build()
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = PudoDatabase.createInMemory(context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // =========================================================================
    // 1. SUCCESSFUL REMOTE PUSH MARKS SYNC QUEUE SYNCED (HTTP 201 Created)
    // =========================================================================
    @Test
    fun testSuccessfulRemotePushMarksSyncQueueSynced() = runBlocking {
        var interceptedRequest: Request? = null
        val okHttpClient = createMockClient(
            statusCode = 201,
            responseBody = """[{"id":"pcl-real-101","tracking_number":"TRK-PUDO-101","status":"OUT_FOR_DELIVERY"}]"""
        ) { req ->
            interceptedRequest = req
        }

        val remoteDataSource = SupabaseRemoteSyncDataSourceImpl(
            config = validConfig(),
            client = okHttpClient
        )
        val syncEngine = PudoSyncEngine(database, remoteDataSource)

        // 1. Write parcel locally to Room
        val localParcel = ParcelEntity(
            id = "pcl-real-101",
            trackingNumber = "TRK-PUDO-101",
            senderName = "Digikala Center",
            recipientName = "Ahmad Reza",
            recipientPhone = "09121112233",
            recipientPostalCode = "1983963111",
            status = ParcelStatus.OUT_FOR_DELIVERY.name,
            size = ParcelSize.MEDIUM.name,
            registrationSource = RegistrationSource.CUSTOMER_REQUEST.name,
            updatedAt = System.currentTimeMillis()
        )
        database.parcelDao().insertParcel(localParcel)

        // 2. Write Outbound SyncQueue item
        val queueItem = SyncQueueEntity(
            id = "queue-real-101",
            entityType = "PARCEL",
            entityId = localParcel.id,
            action = "CREATE",
            payloadJson = "{\"id\":\"${localParcel.id}\"}",
            createdAt = System.currentTimeMillis()
        )
        database.syncQueueDao().insertSyncItem(queueItem)

        // Verify initially pending
        val initialPending = database.syncQueueDao().getPendingSyncItemsDirect()
        assertEquals(1, initialPending.size)
        assertFalse(initialPending[0].isSynced)

        // 3. Execute Outbound Sync
        val syncResult = syncEngine.processPendingSyncQueue(batchSize = 10)

        // 4. Assert Success
        assertEquals(1, syncResult.processedCount)
        assertEquals(1, syncResult.successCount)
        assertEquals(0, syncResult.failureCount)

        // 5. Assert HTTP Request was sent to Supabase REST endpoint
        assertNotNull(interceptedRequest)
        assertEquals("POST", interceptedRequest!!.method)
        assertEquals("https://ndn-pudo-test.supabase.co/rest/v1/parcels", interceptedRequest!!.url.toString())
        assertEquals("resolution=merge-duplicates,return=representation", interceptedRequest!!.header("Prefer"))

        // 6. Assert Queue item in Room is marked synced with timestamp
        val pendingAfterSync = database.syncQueueDao().getPendingSyncItemsDirect()
        assertTrue(pendingAfterSync.isEmpty())

        val allQueueItems = database.syncQueueDao().getPendingSyncItems().first()
        assertTrue(allQueueItems.isEmpty())
    }

    // =========================================================================
    // 2. FAILED REMOTE PUSH LEAVES ITEM PENDING (HTTP 500 Server Error)
    // =========================================================================
    @Test
    fun testFailedRemotePushLeavesItemPending() = runBlocking {
        val okHttpClient = createMockClient(
            statusCode = 500,
            responseBody = "Internal server connection failed"
        )
        val remoteDataSource = SupabaseRemoteSyncDataSourceImpl(
            config = validConfig(),
            client = okHttpClient
        )
        val syncEngine = PudoSyncEngine(database, remoteDataSource)

        val localParcel = ParcelEntity(
            id = "pcl-fail-201",
            trackingNumber = "TRK-FAIL-201",
            senderName = "Sender",
            recipientName = "Recipient",
            recipientPhone = "09123334455",
            recipientPostalCode = "1111111111",
            status = ParcelStatus.OUT_FOR_DELIVERY.name,
            size = ParcelSize.SMALL.name,
            registrationSource = RegistrationSource.CUSTOMER_REQUEST.name,
            updatedAt = System.currentTimeMillis()
        )
        database.parcelDao().insertParcel(localParcel)

        val queueItem = SyncQueueEntity(
            id = "queue-fail-201",
            entityType = "PARCEL",
            entityId = localParcel.id,
            action = "CREATE",
            payloadJson = "{}",
            createdAt = System.currentTimeMillis(),
            retryCount = 0
        )
        database.syncQueueDao().insertSyncItem(queueItem)

        val syncResult = syncEngine.processPendingSyncQueue(batchSize = 10)

        assertEquals(1, syncResult.processedCount)
        assertEquals(0, syncResult.successCount)
        assertEquals(1, syncResult.failureCount)
        assertTrue(syncResult.errors.isNotEmpty())

        // Item remains pending
        val pendingItems = database.syncQueueDao().getPendingSyncItemsDirect()
        assertEquals(1, pendingItems.size)
        assertEquals("queue-fail-201", pendingItems[0].id)
        assertFalse(pendingItems[0].isSynced)
    }

    // =========================================================================
    // 3. RETRY COUNT INCREMENTS ACCURATELY ON CONSECUTIVE FAILURES
    // =========================================================================
    @Test
    fun testRetryCountIncrementsOnConsecutiveFailures() = runBlocking {
        val okHttpClient = createMockClient(
            statusCode = 503,
            responseBody = "Service Unavailable"
        )
        val remoteDataSource = SupabaseRemoteSyncDataSourceImpl(
            config = validConfig(),
            client = okHttpClient
        )
        val syncEngine = PudoSyncEngine(database, remoteDataSource)

        val localHub = HubEntity(
            id = "hub-retry-301",
            name = "Retry Hub",
            managerName = "Manager R",
            phone = "02199998888",
            updatedAt = System.currentTimeMillis()
        )
        database.hubDao().insertHub(localHub)

        val queueItem = SyncQueueEntity(
            id = "queue-retry-301",
            entityType = "HUB",
            entityId = localHub.id,
            action = "CREATE",
            payloadJson = "{}",
            createdAt = System.currentTimeMillis(),
            retryCount = 0
        )
        database.syncQueueDao().insertSyncItem(queueItem)

        // Pass 1: retryCount becomes 1
        syncEngine.processPendingSyncQueue(batchSize = 10)
        var pending = database.syncQueueDao().getPendingSyncItemsDirect()
        assertEquals(1, pending.size)
        assertEquals(1, pending[0].retryCount)

        // Pass 2: retryCount becomes 2
        syncEngine.processPendingSyncQueue(batchSize = 10)
        pending = database.syncQueueDao().getPendingSyncItemsDirect()
        assertEquals(1, pending.size)
        assertEquals(2, pending[0].retryCount)

        // Pass 3: retryCount becomes 3
        syncEngine.processPendingSyncQueue(batchSize = 10)
        pending = database.syncQueueDao().getPendingSyncItemsDirect()
        assertEquals(1, pending.size)
        assertEquals(3, pending[0].retryCount)
        assertFalse(pending[0].isSynced)
    }

    // =========================================================================
    // 4. DUPLICATE OUTBOUND DISPATCH REMAINS IDEMPOTENT (PostgREST merge-duplicates)
    // =========================================================================
    @Test
    fun testDuplicateOutboundDispatchIsIdempotentAcrossAllEntities() = runBlocking {
        val requestedUrls = mutableListOf<String>()
        val okHttpClient = createMockClient(
            statusCode = 200,
            responseBody = "[]"
        ) { req ->
            requestedUrls.add(req.url.toString())
        }

        val remoteDataSource = SupabaseRemoteSyncDataSourceImpl(
            config = validConfig(),
            client = okHttpClient
        )
        val syncEngine = PudoSyncEngine(database, remoteDataSource)

        // 1. User
        val user = UserEntity(
            id = "usr-idem-1",
            username = "idem_user",
            fullName = "Idem User",
            phone = "09120001122",
            role = "COURIER",
            approvalStatus = AccountApprovalStatus.APPROVED.name,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        database.userDao().insertUsers(listOf(user))

        // 2. Hub
        val hub = HubEntity(
            id = "hub-idem-1",
            name = "Idem Hub",
            managerName = "M",
            phone = "021",
            updatedAt = 1000L
        )
        database.hubDao().insertHub(hub)

        // 3. Parcel
        val parcel = ParcelEntity(
            id = "pcl-idem-1",
            trackingNumber = "TRK-IDEM-1",
            senderName = "S",
            recipientName = "R",
            recipientPhone = "0912",
            recipientPostalCode = "111",
            status = ParcelStatus.STORED_AT_HUB.name,
            size = ParcelSize.MEDIUM.name,
            registrationSource = RegistrationSource.CUSTOMER_REQUEST.name,
            updatedAt = 1000L
        )
        database.parcelDao().insertParcel(parcel)

        // 4. Audit Log
        val auditLog = AuditLogEntity(
            id = "audit-idem-1",
            eventType = "PARCEL_STORED",
            actorId = "usr-idem-1",
            actorRole = "COURIER",
            entityId = "pcl-idem-1",
            oldState = "OUT_FOR_DELIVERY",
            newState = "STORED_AT_HUB",
            transactionId = "tx-idem-1",
            timestamp = 1000L
        )
        database.auditLogDao().insertLog(auditLog)

        // 5. Registration Transaction
        val tx = RegistrationTransactionEntity(
            transactionId = "tx-idem-1",
            parcelId = "pcl-idem-1",
            trackingNumber = "TRK-IDEM-1",
            courierId = "usr-idem-1",
            hubId = "hub-idem-1",
            registrationSource = RegistrationSource.CUSTOMER_REQUEST.name,
            timestamp = 1000L
        )
        database.registrationTransactionDao().insertTransaction(tx)

        // Enqueue duplicates for all 5 entities
        val duplicateQueue = listOf(
            SyncQueueEntity("q-u1", "USER", user.id, "CREATE", "{}", 100L),
            SyncQueueEntity("q-u2", "USER", user.id, "CREATE", "{}", 101L),
            SyncQueueEntity("q-h1", "HUB", hub.id, "CREATE", "{}", 102L),
            SyncQueueEntity("q-h2", "HUB", hub.id, "CREATE", "{}", 103L),
            SyncQueueEntity("q-p1", "PARCEL", parcel.id, "CREATE", "{}", 104L),
            SyncQueueEntity("q-p2", "PARCEL", parcel.id, "CREATE", "{}", 105L),
            SyncQueueEntity("q-a1", "AUDIT_LOG", auditLog.id, "CREATE", "{}", 106L),
            SyncQueueEntity("q-a2", "AUDIT_LOG", auditLog.id, "CREATE", "{}", 107L),
            SyncQueueEntity("q-t1", "REGISTRATION_TRANSACTION", tx.transactionId, "CREATE", "{}", 108L),
            SyncQueueEntity("q-t2", "REGISTRATION_TRANSACTION", tx.transactionId, "CREATE", "{}", 109L)
        )
        database.syncQueueDao().insertSyncItems(duplicateQueue)

        val result = syncEngine.processPendingSyncQueue(batchSize = 20)
        assertEquals(10, result.processedCount)
        assertEquals(10, result.successCount)
        assertEquals(0, result.failureCount)

        // Verify all 10 items are marked synced without error
        val pending = database.syncQueueDao().getPendingSyncItemsDirect()
        assertTrue(pending.isEmpty())
        assertEquals(10, requestedUrls.size)
    }

    // =========================================================================
    // 5. LOCAL ROOM DATA SURVIVES REMOTE FAILURE COMPLETELY
    // =========================================================================
    @Test
    fun testLocalRoomDataSurvivesRemoteFailureCompletely() = runBlocking {
        // Mock network throws IOException (complete network failure)
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor {
                throw IOException("Network unreachable / offline mode active")
            }
            .build()

        val remoteDataSource = SupabaseRemoteSyncDataSourceImpl(
            config = validConfig(),
            client = okHttpClient
        )
        val syncEngine = PudoSyncEngine(database, remoteDataSource)

        val localParcel = ParcelEntity(
            id = "pcl-survive-501",
            trackingNumber = "TRK-SURVIVE",
            senderName = "Offline Sender",
            recipientName = "Offline Recipient",
            recipientPhone = "09127778899",
            recipientPostalCode = "1234567890",
            status = ParcelStatus.STORED_AT_HUB.name,
            size = ParcelSize.LARGE.name,
            registrationSource = RegistrationSource.FAILED_HOME_DELIVERY.name,
            baseFee = 35000L,
            isSettled = false,
            updatedAt = 9000L
        )
        database.parcelDao().insertParcel(localParcel)

        val queueItem = SyncQueueEntity(
            id = "q-survive-501",
            entityType = "PARCEL",
            entityId = localParcel.id,
            action = "CREATE",
            payloadJson = "{}",
            createdAt = 9000L
        )
        database.syncQueueDao().insertSyncItem(queueItem)

        // Attempt sync during outage
        val syncResult = syncEngine.processPendingSyncQueue(batchSize = 10)
        assertEquals(1, syncResult.failureCount)

        // Verify Local Room Entity is 100% intact and undamaged
        val queriedParcel = database.parcelDao().getParcelByIdDirect("pcl-survive-501")
        assertNotNull(queriedParcel)
        assertEquals("TRK-SURVIVE", queriedParcel!!.trackingNumber)
        assertEquals(ParcelStatus.STORED_AT_HUB.name, queriedParcel.status)
        assertEquals(35000L, queriedParcel.baseFee)

        // Reactive Room Flow continues emitting immediately
        val flowParcels = database.parcelDao().getAllParcels().first()
        assertEquals(1, flowParcels.size)
        assertEquals("pcl-survive-501", flowParcels[0].id)
    }

    // =========================================================================
    // 6. CONFIGURATION ABSENCE FAILS SAFELY
    // =========================================================================
    @Test
    fun testConfigurationAbsenceFailsSafelyWithoutCrashing() = runBlocking {
        val placeholderConfig = SupabaseConfig(
            url = "https://placeholder-pudo-project.supabase.co",
            publishableKey = "placeholder-supabase-anon-key"
        )
        assertFalse(placeholderConfig.isConfigured())

        val remoteDataSource = SupabaseRemoteSyncDataSourceImpl(config = placeholderConfig)
        val syncEngine = PudoSyncEngine(database, remoteDataSource)

        val localHub = HubEntity(
            id = "hub-unconf-601",
            name = "Unconfigured Hub",
            managerName = "M",
            phone = "021",
            updatedAt = 1000L
        )
        database.hubDao().insertHub(localHub)

        val queueItem = SyncQueueEntity(
            id = "q-unconf-601",
            entityType = "HUB",
            entityId = localHub.id,
            action = "CREATE",
            payloadJson = "{}",
            createdAt = 1000L
        )
        database.syncQueueDao().insertSyncItem(queueItem)

        val syncResult = syncEngine.processPendingSyncQueue(batchSize = 10)

        assertEquals(1, syncResult.processedCount)
        assertEquals(0, syncResult.successCount)
        assertEquals(1, syncResult.failureCount)
        assertTrue(syncResult.errors[0].contains("Supabase is not configured"))

        // Item remains pending with retryCount incremented
        val pending = database.syncQueueDao().getPendingSyncItemsDirect()
        assertEquals(1, pending.size)
        assertEquals(1, pending[0].retryCount)
    }
}
