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
import com.example.data.remote.SupabaseRemoteSyncDataSourceImpl
import com.example.data.remote.dto.*
import com.example.data.sync.PudoSyncEngine
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
class SupabaseInboundSyncVerificationTest {

    private lateinit var database: PudoDatabase
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun validConfig() = SupabaseConfig(
        url = "https://ndn-pudo-test.supabase.co",
        publishableKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test_anon_key_valid_length"
    )

    private fun createMockClient(
        routeResponses: Map<String, Pair<Int, String>> = emptyMap(),
        defaultStatusCode: Int = 200,
        defaultResponseBody: String = "[]",
        onRequestIntercepted: ((Request) -> Unit)? = null
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                onRequestIntercepted?.invoke(request)

                val urlString = request.url.toString()
                val matchedRoute = routeResponses.entries.firstOrNull { urlString.contains(it.key) }
                val (code, bodyStr) = matchedRoute?.value ?: Pair(defaultStatusCode, defaultResponseBody)

                if (code >= 400) {
                    Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(code)
                        .message("Error $code")
                        .body(bodyStr.toResponseBody(jsonMediaType))
                        .build()
                } else {
                    Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(code)
                        .message("Success $code")
                        .body(bodyStr.toResponseBody(jsonMediaType))
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
    // 1. INBOUND DELTA FETCH SENDS VALID QUERY PARAMS (updated_at=gte.<timestamp>)
    // =========================================================================
    @Test
    fun testInboundDeltaFetchSendsValidQueryParams() = runBlocking {
        database.syncCheckpointDao().saveCheckpoint(
            SyncCheckpointEntity(
                syncKey = PudoSyncEngine.CHECKPOINT_PARCELS,
                lastSyncedTimestamp = 12345000L
            )
        )

        val interceptedUrls = mutableListOf<String>()
        val okHttpClient = createMockClient(
            routeResponses = mapOf(
                "parcels" to Pair(200, "[]"),
                "users" to Pair(200, "[]"),
                "hubs" to Pair(200, "[]"),
                "audit_logs" to Pair(200, "[]"),
                "registration_transactions" to Pair(200, "[]")
            )
        ) { req ->
            interceptedUrls.add(req.url.toString())
        }

        val remoteDataSource = SupabaseRemoteSyncDataSourceImpl(
            config = validConfig(),
            client = okHttpClient
        )
        val syncEngine = PudoSyncEngine(database, remoteDataSource)

        syncEngine.performInboundDeltaSync()

        val parcelRequestUrl = interceptedUrls.firstOrNull { it.contains("/parcels") }
        assertNotNull(parcelRequestUrl)
        assertTrue(parcelRequestUrl!!.contains("updated_at=gte.12345000"))
        assertTrue(parcelRequestUrl.contains("select=*"))
        assertTrue(parcelRequestUrl.contains("order=updated_at.asc"))
    }

    // =========================================================================
    // 2. INBOUND PARCEL INSERTION PERSISTS TO ROOM AND EMITS VIA FLOW
    // =========================================================================
    @Test
    fun testInboundParcelInsertionPersistsToRoomAndEmitsViaFlow() = runBlocking {
        val remoteParcelJson = """
            [
              {
                "id": "pcl-inbound-01",
                "tracking_number": "TRK-IN-01",
                "sender_name": "Tehran Merchant",
                "recipient_name": "Maryam",
                "recipient_phone": "09121110000",
                "recipient_postal_code": "1999999999",
                "status": "OUT_FOR_DELIVERY",
                "size": "MEDIUM",
                "registration_source": "CUSTOMER_REQUEST",
                "base_fee": 25000,
                "is_settled": false,
                "updated_at": 50000
              }
            ]
        """.trimIndent()

        val okHttpClient = createMockClient(
            routeResponses = mapOf("parcels" to Pair(200, remoteParcelJson))
        )
        val remoteDataSource = SupabaseRemoteSyncDataSourceImpl(
            config = validConfig(),
            client = okHttpClient
        )
        val syncEngine = PudoSyncEngine(database, remoteDataSource)

        val result = syncEngine.performInboundDeltaSync()
        assertEquals(1, result.parcelsSynced)

        val parcelsFromFlow = database.parcelDao().getAllParcels().first()
        assertEquals(1, parcelsFromFlow.size)
        assertEquals("pcl-inbound-01", parcelsFromFlow[0].id)
        assertEquals("TRK-IN-01", parcelsFromFlow[0].trackingNumber)
        assertEquals(ParcelStatus.OUT_FOR_DELIVERY.name, parcelsFromFlow[0].status)
    }

    // =========================================================================
    // 3. INBOUND PARCEL UPDATE REPLACES OLDER LOCAL PARCEL
    // =========================================================================
    @Test
    fun testInboundParcelUpdateReplacesOlderLocalParcel() = runBlocking {
        val initialLocal = ParcelEntity(
            id = "pcl-update-01",
            trackingNumber = "TRK-UPD-01",
            senderName = "Old Sender",
            recipientName = "Old Recipient",
            recipientPhone = "09120000000",
            recipientPostalCode = "1000000000",
            status = ParcelStatus.OUT_FOR_DELIVERY.name,
            size = ParcelSize.SMALL.name,
            registrationSource = RegistrationSource.CUSTOMER_REQUEST.name,
            updatedAt = 1000L
        )
        database.parcelDao().insertParcel(initialLocal)

        val remoteUpdatedJson = """
            [
              {
                "id": "pcl-update-01",
                "tracking_number": "TRK-UPD-01",
                "sender_name": "Updated Sender",
                "recipient_name": "Updated Recipient",
                "recipient_phone": "09129999999",
                "recipient_postal_code": "1000000000",
                "status": "OUT_FOR_DELIVERY",
                "size": "LARGE",
                "registration_source": "CUSTOMER_REQUEST",
                "updated_at": 2000
              }
            ]
        """.trimIndent()

        val okHttpClient = createMockClient(
            routeResponses = mapOf("parcels" to Pair(200, remoteUpdatedJson))
        )
        val remoteDataSource = SupabaseRemoteSyncDataSourceImpl(
            config = validConfig(),
            client = okHttpClient
        )
        val syncEngine = PudoSyncEngine(database, remoteDataSource)

        val result = syncEngine.performInboundDeltaSync()
        assertEquals(1, result.parcelsSynced)

        val stored = database.parcelDao().getParcelByIdDirect("pcl-update-01")
        assertNotNull(stored)
        assertEquals("Updated Sender", stored!!.senderName)
        assertEquals("Updated Recipient", stored.recipientName)
        assertEquals(2000L, stored.updatedAt)
    }

    // =========================================================================
    // 4. INBOUND STALE PARCEL UPDATE (OLDER TIMESTAMP) IS IGNORED
    // =========================================================================
    @Test
    fun testInboundStaleParcelUpdateIsIgnored() = runBlocking {
        val currentLocal = ParcelEntity(
            id = "pcl-stale-01",
            trackingNumber = "TRK-STALE-01",
            senderName = "Local Fresh Sender",
            recipientName = "Local Fresh Recipient",
            recipientPhone = "09121111111",
            recipientPostalCode = "1111111111",
            status = ParcelStatus.OUT_FOR_DELIVERY.name,
            size = ParcelSize.MEDIUM.name,
            registrationSource = RegistrationSource.CUSTOMER_REQUEST.name,
            updatedAt = 5000L
        )
        database.parcelDao().insertParcel(currentLocal)

        // Remote payload with older timestamp 4000L
        val staleRemoteJson = """
            [
              {
                "id": "pcl-stale-01",
                "tracking_number": "TRK-STALE-01",
                "sender_name": "Remote Stale Sender",
                "recipient_name": "Remote Stale Recipient",
                "recipient_phone": "09120000000",
                "recipient_postal_code": "1111111111",
                "status": "OUT_FOR_DELIVERY",
                "size": "SMALL",
                "registration_source": "CUSTOMER_REQUEST",
                "updated_at": 4000
              }
            ]
        """.trimIndent()

        val okHttpClient = createMockClient(
            routeResponses = mapOf("parcels" to Pair(200, staleRemoteJson))
        )
        val remoteDataSource = SupabaseRemoteSyncDataSourceImpl(
            config = validConfig(),
            client = okHttpClient
        )
        val syncEngine = PudoSyncEngine(database, remoteDataSource)

        val result = syncEngine.performInboundDeltaSync()
        assertEquals(0, result.parcelsSynced)

        val stored = database.parcelDao().getParcelByIdDirect("pcl-stale-01")
        assertNotNull(stored)
        assertEquals("Local Fresh Sender", stored!!.senderName)
        assertEquals(5000L, stored.updatedAt)
    }

    // =========================================================================
    // 5. TERMINAL STATE DELIVERED_TO_CUSTOMER REJECTS REMOTE REGRESSION
    // =========================================================================
    @Test
    fun testTerminalStateDeliveredToCustomerRejectsRemoteRegression() = runBlocking {
        val deliveredLocal = ParcelEntity(
            id = "pcl-term-deliv",
            trackingNumber = "TRK-DELIV",
            senderName = "Sender",
            recipientName = "Recipient",
            recipientPhone = "09121234567",
            recipientPostalCode = "1234567890",
            status = ParcelStatus.DELIVERED_TO_CUSTOMER.name,
            size = ParcelSize.MEDIUM.name,
            registrationSource = RegistrationSource.CUSTOMER_REQUEST.name,
            updatedAt = 8000L
        )
        database.parcelDao().insertParcel(deliveredLocal)

        val regressingRemoteJson = """
            [
              {
                "id": "pcl-term-deliv",
                "tracking_number": "TRK-DELIV",
                "sender_name": "Sender",
                "recipient_name": "Recipient",
                "recipient_phone": "09121234567",
                "recipient_postal_code": "1234567890",
                "status": "STORED_AT_HUB",
                "size": "MEDIUM",
                "registration_source": "CUSTOMER_REQUEST",
                "updated_at": 9000
              }
            ]
        """.trimIndent()

        val okHttpClient = createMockClient(
            routeResponses = mapOf("parcels" to Pair(200, regressingRemoteJson))
        )
        val remoteDataSource = SupabaseRemoteSyncDataSourceImpl(
            config = validConfig(),
            client = okHttpClient
        )
        val syncEngine = PudoSyncEngine(database, remoteDataSource)

        val result = syncEngine.performInboundDeltaSync()
        assertEquals(0, result.parcelsSynced)

        val stored = database.parcelDao().getParcelByIdDirect("pcl-term-deliv")
        assertEquals(ParcelStatus.DELIVERED_TO_CUSTOMER.name, stored?.status)
    }

    // =========================================================================
    // 6. TERMINAL STATE RETURNED_TO_SENDER REJECTS REMOTE REGRESSION
    // =========================================================================
    @Test
    fun testTerminalStateReturnedToSenderRejectsRemoteRegression() = runBlocking {
        val returnedLocal = ParcelEntity(
            id = "pcl-term-ret",
            trackingNumber = "TRK-RET",
            senderName = "Sender",
            recipientName = "Recipient",
            recipientPhone = "09121234567",
            recipientPostalCode = "1234567890",
            status = ParcelStatus.RETURNED_TO_SENDER.name,
            size = ParcelSize.MEDIUM.name,
            registrationSource = RegistrationSource.CUSTOMER_REQUEST.name,
            updatedAt = 8000L
        )
        database.parcelDao().insertParcel(returnedLocal)

        val regressingRemoteJson = """
            [
              {
                "id": "pcl-term-ret",
                "tracking_number": "TRK-RET",
                "sender_name": "Sender",
                "recipient_name": "Recipient",
                "recipient_phone": "09121234567",
                "recipient_postal_code": "1234567890",
                "status": "OUT_FOR_DELIVERY",
                "size": "MEDIUM",
                "registration_source": "CUSTOMER_REQUEST",
                "updated_at": 9000
              }
            ]
        """.trimIndent()

        val okHttpClient = createMockClient(
            routeResponses = mapOf("parcels" to Pair(200, regressingRemoteJson))
        )
        val remoteDataSource = SupabaseRemoteSyncDataSourceImpl(
            config = validConfig(),
            client = okHttpClient
        )
        val syncEngine = PudoSyncEngine(database, remoteDataSource)

        val result = syncEngine.performInboundDeltaSync()
        assertEquals(0, result.parcelsSynced)

        val stored = database.parcelDao().getParcelByIdDirect("pcl-term-ret")
        assertEquals(ParcelStatus.RETURNED_TO_SENDER.name, stored?.status)
    }

    // =========================================================================
    // 7. TERMINAL STATE REJECTED REJECTS REMOTE REGRESSION
    // =========================================================================
    @Test
    fun testTerminalStateRejectedRejectsRemoteRegression() = runBlocking {
        val rejectedLocal = ParcelEntity(
            id = "pcl-term-rej",
            trackingNumber = "TRK-REJ",
            senderName = "Sender",
            recipientName = "Recipient",
            recipientPhone = "09121234567",
            recipientPostalCode = "1234567890",
            status = ParcelStatus.REJECTED.name,
            size = ParcelSize.MEDIUM.name,
            registrationSource = RegistrationSource.CUSTOMER_REQUEST.name,
            updatedAt = 8000L
        )
        database.parcelDao().insertParcel(rejectedLocal)

        val regressingRemoteJson = """
            [
              {
                "id": "pcl-term-rej",
                "tracking_number": "TRK-REJ",
                "sender_name": "Sender",
                "recipient_name": "Recipient",
                "recipient_phone": "09121234567",
                "recipient_postal_code": "1234567890",
                "status": "HUB_SELECTED",
                "size": "MEDIUM",
                "registration_source": "CUSTOMER_REQUEST",
                "updated_at": 9000
              }
            ]
        """.trimIndent()

        val okHttpClient = createMockClient(
            routeResponses = mapOf("parcels" to Pair(200, regressingRemoteJson))
        )
        val remoteDataSource = SupabaseRemoteSyncDataSourceImpl(
            config = validConfig(),
            client = okHttpClient
        )
        val syncEngine = PudoSyncEngine(database, remoteDataSource)

        val result = syncEngine.performInboundDeltaSync()
        assertEquals(0, result.parcelsSynced)

        val stored = database.parcelDao().getParcelByIdDirect("pcl-term-rej")
        assertEquals(ParcelStatus.REJECTED.name, stored?.status)
    }

    // =========================================================================
    // 8. VALID STATE TRANSITION (STORED_AT_HUB -> DELIVERED_TO_CUSTOMER) IS ACCEPTED
    // =========================================================================
    @Test
    fun testValidStateTransitionIsAccepted() = runBlocking {
        val storedLocal = ParcelEntity(
            id = "pcl-trans-valid",
            trackingNumber = "TRK-VALID-TRANS",
            senderName = "Sender",
            recipientName = "Recipient",
            recipientPhone = "09123334455",
            recipientPostalCode = "1234567890",
            status = ParcelStatus.STORED_AT_HUB.name,
            size = ParcelSize.MEDIUM.name,
            registrationSource = RegistrationSource.CUSTOMER_REQUEST.name,
            updatedAt = 2000L
        )
        database.parcelDao().insertParcel(storedLocal)

        val validTransitionRemoteJson = """
            [
              {
                "id": "pcl-trans-valid",
                "tracking_number": "TRK-VALID-TRANS",
                "sender_name": "Sender",
                "recipient_name": "Recipient",
                "recipient_phone": "09123334455",
                "recipient_postal_code": "1234567890",
                "status": "DELIVERED_TO_CUSTOMER",
                "size": "MEDIUM",
                "registration_source": "CUSTOMER_REQUEST",
                "updated_at": 3000
              }
            ]
        """.trimIndent()

        val okHttpClient = createMockClient(
            routeResponses = mapOf("parcels" to Pair(200, validTransitionRemoteJson))
        )
        val remoteDataSource = SupabaseRemoteSyncDataSourceImpl(
            config = validConfig(),
            client = okHttpClient
        )
        val syncEngine = PudoSyncEngine(database, remoteDataSource)

        val result = syncEngine.performInboundDeltaSync()
        assertEquals(1, result.parcelsSynced)

        val stored = database.parcelDao().getParcelByIdDirect("pcl-trans-valid")
        assertEquals(ParcelStatus.DELIVERED_TO_CUSTOMER.name, stored?.status)
        assertEquals(3000L, stored?.updatedAt)
    }

    // =========================================================================
    // 9. INVALID STATE TRANSITION (STORED_AT_HUB -> OUT_FOR_DELIVERY) IS REJECTED
    // =========================================================================
    @Test
    fun testInvalidStateTransitionIsRejected() = runBlocking {
        val storedLocal = ParcelEntity(
            id = "pcl-trans-invalid",
            trackingNumber = "TRK-INV-TRANS",
            senderName = "Sender",
            recipientName = "Recipient",
            recipientPhone = "09123334455",
            recipientPostalCode = "1234567890",
            status = ParcelStatus.STORED_AT_HUB.name,
            size = ParcelSize.MEDIUM.name,
            registrationSource = RegistrationSource.CUSTOMER_REQUEST.name,
            updatedAt = 3000L
        )
        database.parcelDao().insertParcel(storedLocal)

        // Invalid transition: Cannot transition from STORED_AT_HUB back to OUT_FOR_DELIVERY
        val invalidTransitionRemoteJson = """
            [
              {
                "id": "pcl-trans-invalid",
                "tracking_number": "TRK-INV-TRANS",
                "sender_name": "Sender",
                "recipient_name": "Recipient",
                "recipient_phone": "09123334455",
                "recipient_postal_code": "1234567890",
                "status": "OUT_FOR_DELIVERY",
                "size": "MEDIUM",
                "registration_source": "CUSTOMER_REQUEST",
                "updated_at": 4000
              }
            ]
        """.trimIndent()

        val okHttpClient = createMockClient(
            routeResponses = mapOf("parcels" to Pair(200, invalidTransitionRemoteJson))
        )
        val remoteDataSource = SupabaseRemoteSyncDataSourceImpl(
            config = validConfig(),
            client = okHttpClient
        )
        val syncEngine = PudoSyncEngine(database, remoteDataSource)

        val result = syncEngine.performInboundDeltaSync()
        assertEquals(0, result.parcelsSynced)

        val stored = database.parcelDao().getParcelByIdDirect("pcl-trans-invalid")
        assertEquals(ParcelStatus.STORED_AT_HUB.name, stored?.status)
    }

    // =========================================================================
    // 10. INBOUND HUB DELTA MERGE CORRECTLY UPDATES LOCAL HUB METADATA
    // =========================================================================
    @Test
    fun testInboundHubDeltaMergeCorrectlyUpdatesLocalHubMetadata() = runBlocking {
        val localHub = HubEntity(
            id = "hub-inbound-10",
            name = "Old Hub Name",
            managerName = "Old Manager",
            phone = "02111111111",
            updatedAt = 1000L
        )
        database.hubDao().insertHub(localHub)

        val remoteHubJson = """
            [
              {
                "id": "hub-inbound-10",
                "name": "Super PUDO Hub Saadat Abad",
                "manager_name": "Mr. Karimi",
                "phone": "02122222222",
                "rating": 4.9,
                "review_count": 120,
                "is_open": true,
                "updated_at": 2500
              }
            ]
        """.trimIndent()

        val okHttpClient = createMockClient(
            routeResponses = mapOf("hubs" to Pair(200, remoteHubJson))
        )
        val remoteDataSource = SupabaseRemoteSyncDataSourceImpl(
            config = validConfig(),
            client = okHttpClient
        )
        val syncEngine = PudoSyncEngine(database, remoteDataSource)

        val result = syncEngine.performInboundDeltaSync()
        assertEquals(1, result.hubsSynced)

        val storedHub = database.hubDao().getHubByIdDirect("hub-inbound-10")
        assertNotNull(storedHub)
        assertEquals("Super PUDO Hub Saadat Abad", storedHub!!.name)
        assertEquals("Mr. Karimi", storedHub.managerName)
        assertEquals("02122222222", storedHub.phone)
        assertEquals(4.9f, storedHub.rating, 0.01f)
        assertEquals(2500L, storedHub.updatedAt)
    }

    // =========================================================================
    // 11. INBOUND USER DELTA PRESERVES LOCAL PASSWORD HASH & PROTECTS SYSTEM ADMIN
    // =========================================================================
    @Test
    fun testInboundUserDeltaPreservesLocalPasswordHashAndProtectsSystemAdmin() = runBlocking {
        // 1. Seed System Admin
        val systemAdmin = UserEntity(
            id = "usr-admin-root",
            username = "reza",
            passwordHash = "argon2_secret_hash_reza",
            fullName = "Reza System Admin",
            phone = "09120000001",
            role = "SYSTEM_ADMIN",
            approvalStatus = AccountApprovalStatus.APPROVED.name,
            updatedAt = 1000L
        )
        // 2. Seed normal Courier with local password hash
        val courierUser = UserEntity(
            id = "usr-courier-11",
            username = "courier_ali",
            passwordHash = "argon2_secret_hash_ali",
            fullName = "Ali Courier",
            phone = "09121112233",
            role = "COURIER",
            approvalStatus = AccountApprovalStatus.APPROVED.name,
            updatedAt = 1000L
        )
        database.userDao().insertUsers(listOf(systemAdmin, courierUser))

        val remoteUsersJson = """
            [
              {
                "id": "usr-admin-root",
                "username": "reza",
                "password_hash": "",
                "full_name": "Attempted Remote Tamper",
                "phone": "09120000001",
                "role": "CUSTOMER",
                "approval_status": "PENDING",
                "updated_at": 9999
              },
              {
                "id": "usr-courier-11",
                "username": "courier_ali",
                "password_hash": "",
                "full_name": "Ali Courier (Updated Profile)",
                "phone": "09121112233",
                "role": "COURIER",
                "approval_status": "APPROVED",
                "updated_at": 2000
              }
            ]
        """.trimIndent()

        val okHttpClient = createMockClient(
            routeResponses = mapOf("users" to Pair(200, remoteUsersJson))
        )
        val remoteDataSource = SupabaseRemoteSyncDataSourceImpl(
            config = validConfig(),
            client = okHttpClient
        )
        val syncEngine = PudoSyncEngine(database, remoteDataSource)

        val result = syncEngine.performInboundDeltaSync()
        // Admin was ignored (protected), Courier was merged
        assertEquals(1, result.usersSynced)

        // Verify System Admin was untouched
        val adminStored = database.userDao().getUserById("usr-admin-root")
        assertEquals("reza", adminStored?.username)
        assertEquals("SYSTEM_ADMIN", adminStored?.role)
        assertEquals("Reza System Admin", adminStored?.fullName)
        assertEquals("argon2_secret_hash_reza", adminStored?.passwordHash)

        // Verify Courier profile updated but local password hash preserved
        val courierStored = database.userDao().getUserById("usr-courier-11")
        assertEquals("Ali Courier (Updated Profile)", courierStored?.fullName)
        assertEquals("argon2_secret_hash_ali", courierStored?.passwordHash)
        assertEquals(2000L, courierStored?.updatedAt)
    }

    // =========================================================================
    // 12. INBOUND AUDIT LOG DELTA IS APPEND-ONLY AND IDEMPOTENT
    // =========================================================================
    @Test
    fun testInboundAuditLogDeltaIsAppendOnlyAndIdempotent() = runBlocking {
        val existingLog = AuditLogEntity(
            id = "audit-exist-1",
            eventType = "PARCEL_CREATED",
            actorId = "usr-1",
            actorRole = "CUSTOMER",
            entityId = "pcl-1",
            oldState = null,
            newState = "OUT_FOR_DELIVERY",
            transactionId = "tx-1",
            timestamp = 1000L
        )
        database.auditLogDao().insertLog(existingLog)

        val remoteLogsJson = """
            [
              {
                "id": "audit-exist-1",
                "event_type": "PARCEL_CREATED",
                "actor_id": "usr-1",
                "actor_role": "CUSTOMER",
                "entity_id": "pcl-1",
                "new_state": "OUT_FOR_DELIVERY",
                "timestamp": 1000
              },
              {
                "id": "audit-new-2",
                "event_type": "PARCEL_RECEIVED_AT_HUB",
                "actor_id": "usr-2",
                "actor_role": "HUB_MANAGER",
                "entity_id": "pcl-1",
                "old_state": "OUT_FOR_DELIVERY",
                "new_state": "STORED_AT_HUB",
                "timestamp": 2000
              }
            ]
        """.trimIndent()

        val okHttpClient = createMockClient(
            routeResponses = mapOf("audit_logs" to Pair(200, remoteLogsJson))
        )
        val remoteDataSource = SupabaseRemoteSyncDataSourceImpl(
            config = validConfig(),
            client = okHttpClient
        )
        val syncEngine = PudoSyncEngine(database, remoteDataSource)

        val result = syncEngine.performInboundDeltaSync()
        assertEquals(2, result.auditLogsSynced)

        val allLogs = database.auditLogDao().getAllLogs().first()
        assertEquals(2, allLogs.size)
        assertTrue(allLogs.any { it.id == "audit-exist-1" })
        assertTrue(allLogs.any { it.id == "audit-new-2" })
    }

    // =========================================================================
    // 13. INBOUND REGISTRATION TRANSACTION DELTA IS IMMUTABLE AND DEDUPLICATED
    // =========================================================================
    @Test
    fun testInboundRegistrationTransactionDeltaIsImmutableAndDeduplicated() = runBlocking {
        val existingTx = RegistrationTransactionEntity(
            transactionId = "tx-exist-1",
            parcelId = "pcl-1",
            trackingNumber = "TRK-01",
            courierId = "usr-c1",
            hubId = "hub-h1",
            registrationSource = RegistrationSource.CUSTOMER_REQUEST.name,
            timestamp = 1500L
        )
        database.registrationTransactionDao().insertTransaction(existingTx)

        val remoteTxJson = """
            [
              {
                "transaction_id": "tx-exist-1",
                "parcel_id": "pcl-1",
                "tracking_number": "TRK-01",
                "courier_id": "usr-c1",
                "hub_id": "hub-h1",
                "registration_source": "CUSTOMER_REQUEST",
                "timestamp": 1500
              },
              {
                "transaction_id": "tx-new-2",
                "parcel_id": "pcl-2",
                "tracking_number": "TRK-02",
                "courier_id": "usr-c1",
                "hub_id": "hub-h1",
                "registration_source": "FAILED_HOME_DELIVERY",
                "timestamp": 2500
              }
            ]
        """.trimIndent()

        val okHttpClient = createMockClient(
            routeResponses = mapOf("registration_transactions" to Pair(200, remoteTxJson))
        )
        val remoteDataSource = SupabaseRemoteSyncDataSourceImpl(
            config = validConfig(),
            client = okHttpClient
        )
        val syncEngine = PudoSyncEngine(database, remoteDataSource)

        val result = syncEngine.performInboundDeltaSync()
        assertEquals(2, result.transactionsSynced)

        val allTxs = database.registrationTransactionDao().getAllTransactions().first()
        assertEquals(2, allTxs.size)
        assertTrue(allTxs.any { it.transactionId == "tx-exist-1" })
        assertTrue(allTxs.any { it.transactionId == "tx-new-2" })
    }

    // =========================================================================
    // 14. SYNC CHECKPOINTS ADVANCE ONLY AFTER SUCCESSFUL ROOM TRANSACTION COMMIT
    // =========================================================================
    @Test
    fun testSyncCheckpointsAdvanceOnlyAfterSuccessfulRoomTransactionCommit() = runBlocking {
        val initialHubCheckpoint = database.syncCheckpointDao().getCheckpoint(PudoSyncEngine.CHECKPOINT_HUBS) ?: 0L
        assertEquals(0L, initialHubCheckpoint)

        val remoteHubJson = """
            [
              {
                "id": "hub-chk-advance",
                "name": "Checkpoint Advancing Hub",
                "manager_name": "Manager",
                "phone": "02199990000",
                "updated_at": 7777000
              }
            ]
        """.trimIndent()

        val okHttpClient = createMockClient(
            routeResponses = mapOf("hubs" to Pair(200, remoteHubJson))
        )
        val remoteDataSource = SupabaseRemoteSyncDataSourceImpl(
            config = validConfig(),
            client = okHttpClient
        )
        val syncEngine = PudoSyncEngine(database, remoteDataSource)

        syncEngine.performInboundDeltaSync()

        val updatedCheckpoint = database.syncCheckpointDao().getCheckpoint(PudoSyncEngine.CHECKPOINT_HUBS)
        assertEquals(7777000L, updatedCheckpoint)

        val checkpointEntity = database.syncCheckpointDao().getCheckpointEntity(PudoSyncEngine.CHECKPOINT_HUBS)
        assertNotNull(checkpointEntity)
        assertEquals(1, checkpointEntity!!.recordsCount)
        assertTrue(checkpointEntity.lastSyncedAt > 0L)
    }

    // =========================================================================
    // 15. INBOUND SYNC FAILURE PRESERVES PREVIOUS CHECKPOINT WITHOUT ADVANCEMENT
    // =========================================================================
    @Test
    fun testInboundSyncFailurePreservesPreviousCheckpointWithoutAdvancement() = runBlocking {
        // Set existing checkpoint
        database.syncCheckpointDao().saveCheckpoint(
            SyncCheckpointEntity(
                syncKey = PudoSyncEngine.CHECKPOINT_PARCELS,
                lastSyncedTimestamp = 5000000L
            )
        )

        // Mock returns 500 Internal Server Error for parcels
        val okHttpClient = createMockClient(
            routeResponses = mapOf("parcels" to Pair(500, "Internal Server Error"))
        )
        val remoteDataSource = SupabaseRemoteSyncDataSourceImpl(
            config = validConfig(),
            client = okHttpClient
        )
        val syncEngine = PudoSyncEngine(database, remoteDataSource)

        val result = syncEngine.performInboundDeltaSync()
        assertTrue(result.errors.isNotEmpty())
        assertEquals(0, result.parcelsSynced)

        // Checkpoint must NOT advance on failure
        val checkpointAfterFailure = database.syncCheckpointDao().getCheckpoint(PudoSyncEngine.CHECKPOINT_PARCELS)
        assertEquals(5000000L, checkpointAfterFailure)
    }

    // =========================================================================
    // 16. CONCURRENT SYNC CALLS ARE SERIALIZED CLEANLY VIA MUTEX
    // =========================================================================
    @Test
    fun testConcurrentSyncCallsAreSerializedCleanlyViaMutex() = runBlocking {
        var callCount = 0
        val okHttpClient = createMockClient {
            callCount++
        }
        val remoteDataSource = SupabaseRemoteSyncDataSourceImpl(
            config = validConfig(),
            client = okHttpClient
        )
        val syncEngine = PudoSyncEngine(database, remoteDataSource)

        // Launch 5 concurrent synchronize calls
        val deferreds = (1..5).map {
            async {
                syncEngine.synchronize(batchSize = 10)
            }
        }

        val results = deferreds.awaitAll()

        // All 5 completed successfully without collision, deadlock, or exception
        assertEquals(5, results.size)
        assertTrue(results.all { it.isSuccess })
    }
}
