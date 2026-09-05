package com.example.remote

import com.example.data.model.AccountApprovalStatus
import com.example.data.model.ParcelSize
import com.example.data.model.ParcelStatus
import com.example.data.model.RegistrationSource
import com.example.data.remote.ConfigurationValidationReport
import com.example.data.remote.SupabaseConfig
import com.example.data.remote.SupabaseConnectivityVerifier
import com.example.data.remote.SupabaseRemoteSyncDataSourceImpl
import com.example.data.remote.dto.*
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class SupabaseRemoteSyncDataSourceTest {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun createMockClient(
        handler: (Request) -> Response
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                handler(chain.request())
            }
            .build()
    }

    private fun validConfig() = SupabaseConfig(
        url = "https://validproject.supabase.co",
        publishableKey = "eyJhGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.valid_anon_token_12345"
    )

    @Test
    fun testUpsertParcelSendsCorrectHeadersAndPostgRESTPayload() = runBlocking {
        var capturedRequest: Request? = null

        val client = createMockClient { request ->
            capturedRequest = request
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(201)
                .message("Created")
                .body("""[{"id":"pcl-1","tracking_number":"TRK-100","status":"OUT_FOR_DELIVERY"}]""".toResponseBody(jsonMediaType))
                .build()
        }

        val dataSource = SupabaseRemoteSyncDataSourceImpl(
            config = validConfig(),
            client = client
        )

        val parcelDto = SupabaseParcelDto(
            id = "pcl-1",
            trackingNumber = "TRK-100",
            senderName = "Sender A",
            recipientName = "Recipient B",
            recipientPhone = "09121234567",
            recipientPostalCode = "1234567890",
            status = ParcelStatus.OUT_FOR_DELIVERY.name,
            size = ParcelSize.MEDIUM.name,
            registrationSource = RegistrationSource.CUSTOMER_REQUEST.name,
            updatedAt = 1000L
        )

        val result = dataSource.upsertParcel(parcelDto)
        assertTrue(result.isSuccess)
        assertEquals("pcl-1", result.getOrNull()?.id)

        assertNotNull(capturedRequest)
        val req = capturedRequest!!
        assertEquals("POST", req.method)
        assertEquals("https://validproject.supabase.co/rest/v1/parcels", req.url.toString())
        assertEquals("eyJhGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.valid_anon_token_12345", req.header("apikey"))
        assertEquals("Bearer eyJhGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.valid_anon_token_12345", req.header("Authorization"))
        assertEquals("resolution=merge-duplicates,return=representation", req.header("Prefer"))
    }

    @Test
    fun testUpsertHubSendsCorrectRequest() = runBlocking {
        var capturedUrl: String? = null

        val client = createMockClient { request ->
            capturedUrl = request.url.toString()
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("""[{"id":"hub-1","name":"Hub One","is_open":true}]""".toResponseBody(jsonMediaType))
                .build()
        }

        val dataSource = SupabaseRemoteSyncDataSourceImpl(
            config = validConfig(),
            client = client
        )

        val hubDto = SupabaseHubDto(
            id = "hub-1",
            name = "Hub One",
            managerName = "Manager M",
            phone = "02111112222",
            isOpen = true,
            updatedAt = 2000L
        )

        val result = dataSource.upsertHub(hubDto)
        assertTrue(result.isSuccess)
        assertEquals("https://validproject.supabase.co/rest/v1/hubs", capturedUrl)
    }

    @Test
    fun testUpsertUserSendsCorrectRequest() = runBlocking {
        val client = createMockClient { request ->
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("[]".toResponseBody(jsonMediaType))
                .build()
        }

        val dataSource = SupabaseRemoteSyncDataSourceImpl(
            config = validConfig(),
            client = client
        )

        val userDto = SupabaseUserDto(
            id = "usr-1",
            username = "admin_user",
            fullName = "Admin",
            phone = "09120000000",
            role = "SYSTEM_ADMIN",
            approvalStatus = AccountApprovalStatus.APPROVED.name,
            updatedAt = 3000L
        )

        val result = dataSource.upsertUser(userDto)
        assertTrue(result.isSuccess)
        assertEquals("usr-1", result.getOrNull()?.id)
    }

    @Test
    fun testPushAuditLogAndRegistrationTransaction() = runBlocking {
        val client = createMockClient { request ->
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(201)
                .message("Created")
                .body("[]".toResponseBody(jsonMediaType))
                .build()
        }

        val dataSource = SupabaseRemoteSyncDataSourceImpl(
            config = validConfig(),
            client = client
        )

        val logDto = SupabaseAuditLogDto(
            id = "audit-1",
            eventType = "PARCEL_DELIVERED",
            actorId = "usr-1",
            actorRole = "HUB_MANAGER",
            entityId = "pcl-1",
            oldState = "HANDOVER_IN_PROGRESS",
            newState = "DELIVERED_TO_CUSTOMER",
            timestamp = 4000L
        )
        val logResult = dataSource.pushAuditLog(logDto)
        assertTrue(logResult.isSuccess)

        val txDto = SupabaseRegistrationTransactionDto(
            transactionId = "tx-100",
            parcelId = "pcl-1",
            trackingNumber = "TRK-100",
            courierId = "usr-2",
            hubId = "hub-1",
            registrationSource = "CUSTOMER_REQUEST",
            timestamp = 4000L
        )
        val txResult = dataSource.pushRegistrationTransaction(txDto)
        assertTrue(txResult.isSuccess)
    }

    @Test
    fun testDeleteParcelSendsCorrectDeleteRequest() = runBlocking {
        var capturedMethod: String? = null
        var capturedUrl: String? = null

        val client = createMockClient { request ->
            capturedMethod = request.method
            capturedUrl = request.url.toString()
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(204)
                .message("No Content")
                .body("".toResponseBody(null))
                .build()
        }

        val dataSource = SupabaseRemoteSyncDataSourceImpl(
            config = validConfig(),
            client = client
        )

        val result = dataSource.deleteParcel("pcl-delete-99")
        assertTrue(result.isSuccess)
        assertEquals("DELETE", capturedMethod)
        assertEquals("https://validproject.supabase.co/rest/v1/parcels?id=eq.pcl-delete-99", capturedUrl)
    }

    @Test
    fun testFetchParcelsUpdatedSinceQueriesCorrectPostgRESTFilter() = runBlocking {
        var capturedUrl: String? = null

        val client = createMockClient { request ->
            capturedUrl = request.url.toString()
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("""[{"id":"pcl-delta-1","tracking_number":"TRK-D1","sender_name":"S","recipient_name":"R","recipient_phone":"0912","recipient_postal_code":"111","status":"HUB_SELECTED"}]""".toResponseBody(jsonMediaType))
                .build()
        }

        val dataSource = SupabaseRemoteSyncDataSourceImpl(
            config = validConfig(),
            client = client
        )

        val result = dataSource.fetchParcelsUpdatedSince(5000L)
        assertTrue(result.isSuccess)
        val list = result.getOrNull()
        assertNotNull(list)
        assertEquals(1, list!!.size)
        assertEquals("pcl-delta-1", list[0].id)
        assertEquals("https://validproject.supabase.co/rest/v1/parcels?select=*&updated_at=gte.5000&order=updated_at.asc", capturedUrl)
    }

    @Test
    fun testHttp500ServerErrorReturnsFailureSafely() = runBlocking {
        val client = createMockClient { request ->
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(500)
                .message("Internal Server Error")
                .body("Internal database connection error".toResponseBody(jsonMediaType))
                .build()
        }

        val dataSource = SupabaseRemoteSyncDataSourceImpl(
            config = validConfig(),
            client = client
        )

        val parcelDto = SupabaseParcelDto(
            id = "pcl-err",
            trackingNumber = "TRK-ERR",
            senderName = "Sender",
            recipientName = "Recipient",
            recipientPhone = "0912",
            recipientPostalCode = "111",
            status = "OUT_FOR_DELIVERY"
        )

        val result = dataSource.upsertParcel(parcelDto)
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue(ex is IOException)
        assertTrue(ex?.message?.contains("code 500") == true)
    }

    @Test
    fun testUnconfiguredConfigThrowsIllegalStateExceptionSafely() = runBlocking {
        val unconfigured = SupabaseConfig(
            url = "https://placeholder-pudo-project.supabase.co",
            publishableKey = "placeholder-supabase-anon-key"
        )
        val dataSource = SupabaseRemoteSyncDataSourceImpl(config = unconfigured)

        val parcelDto = SupabaseParcelDto(
            id = "pcl-unconf",
            trackingNumber = "TRK-U",
            senderName = "Sender",
            recipientName = "Recipient",
            recipientPhone = "0912",
            recipientPostalCode = "111",
            status = "OUT_FOR_DELIVERY"
        )
        val result = dataSource.upsertParcel(parcelDto)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun testConnectivityVerifierSafeDiagnostics() {
        val configured = SupabaseConfig(
            url = "https://example-project.supabase.co",
            publishableKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.super_secret_anon_key_long"
        )
        val verifier = SupabaseConnectivityVerifier(configured)
        val report = verifier.validateConfiguration()

        assertTrue(report.isConfigured)
        assertEquals("example-project.supabase.co", report.sanitizedUrlHost)
        assertTrue(report.isKeyPresent)
        assertEquals(configured.publishableKey.length, report.keyLength)

        val schema = verifier.getSchemaCompatibility()
        assertTrue(schema.isCompatible)
        assertEquals(5, schema.verifiedTables.size)
        assertTrue(schema.verifiedTables.contains("parcels"))
        assertTrue(schema.verifiedTables.contains("users"))
        assertTrue(schema.verifiedTables.contains("hubs"))
        assertTrue(schema.verifiedTables.contains("audit_logs"))
        assertTrue(schema.verifiedTables.contains("registration_transactions"))
    }
}
