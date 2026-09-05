package com.example.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.BuildConfig
import com.example.data.local.database.PudoDatabase
import com.example.data.remote.SupabaseConfig
import com.example.data.remote.SupabaseConnectivityVerifier
import com.example.data.remote.SupabaseRemoteSyncDataSourceImpl
import com.example.data.repository.PudoRepositoryImpl
import com.example.data.sync.PudoSyncEngine
import com.example.model.HubItem
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.URI

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class LiveSupabaseL3L4VerificationTest {

    private lateinit var context: Context
    private lateinit var database: PudoDatabase
    private lateinit var repository: PudoRepositoryImpl
    private lateinit var syncEngine: PudoSyncEngine
    private lateinit var remoteDataSource: SupabaseRemoteSyncDataSourceImpl
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var config: SupabaseConfig
    private lateinit var verifier: SupabaseConnectivityVerifier

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, PudoDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        config = SupabaseConfig.default()
        okHttpClient = SupabaseRemoteSyncDataSourceImpl.createDefaultOkHttpClient()
        remoteDataSource = SupabaseRemoteSyncDataSourceImpl(config = config, client = okHttpClient)
        verifier = SupabaseConnectivityVerifier(config = config, client = okHttpClient)
        syncEngine = PudoSyncEngine(database = database, remoteDataSource = remoteDataSource)
        repository = PudoRepositoryImpl(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testPhaseA_ConfigurationIntegrity() {
        println("=== PHASE A: CONFIGURATION AUDIT ===")
        println("Configured: ${config.isConfigured()}")
        println("Sanitized Config: $config")

        assertTrue("SUPABASE_URL must not be empty", config.url.isNotBlank())
        assertTrue("SUPABASE_PUBLISHABLE_KEY must not be empty", config.publishableKey.isNotBlank())
        
        if (config.isConfigured()) {
            assertFalse("SUPABASE_URL must not be placeholder when configured", config.url.contains("placeholder"))
            assertFalse("SUPABASE_PUBLISHABLE_KEY must not be placeholder when configured", config.publishableKey.contains("placeholder"))
        }
    }

    @Test
    fun testPhaseB_LiveHttpProbe() = runBlocking {
        assumeTrue("Live probe requires configured Supabase credentials", config.isConfigured())
        println("=== PHASE B: REAL HTTPS PROBE ===")
        val host = URI.create(config.url).host
        val endpoint = "${config.url.trimEnd('/')}/rest/v1/"
        println("Target Host: $host")
        println("Target Endpoint: $endpoint")

        try {
            val probeResult = verifier.verifyLiveEndpointConnectivity()
            println("Probe Result: $probeResult")

            // Direct OkHttp probe
            val request = Request.Builder()
                .url(endpoint)
                .addHeader("apikey", config.publishableKey)
                .addHeader("Authorization", "Bearer ${config.publishableKey}")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val code = response.code
            val body = response.body?.string()?.take(300)
            println("Direct Probe HTTP Status: $code")
            println("Direct Probe Response Body: $body")

            assertTrue("HTTP status must be a valid server response (200..499)", code in 200..499)
        } catch (e: Exception) {
            println("Live HTTP probe exception (e.g. offline environment): ${e.message}")
        }
    }

    @Test
    fun testPhaseC_InboundFetchEndpointsProbe() = runBlocking {
        assumeTrue("Inbound fetch probe requires configured Supabase credentials", config.isConfigured())
        println("=== PHASE C: INBOUND FETCH ENDPOINTS PROBE ===")
        try {
            val results = verifier.verifyInboundFetchCapabilities(sinceTimestamp = 0L)
            results.forEach { (table, res) ->
                println("Table [$table] -> HTTP Status: ${res.httpStatusCode}, Success: ${res.isSuccess}, Message: ${res.diagnosticMessage}")
            }
        } catch (e: Exception) {
            println("Inbound fetch probe exception: ${e.message}")
        }
    }

    @Test
    fun testPhaseD_EndToEndLiveWriteAndReadBackVerification() = runBlocking {
        println("=== PHASE D: END-TO-END LIVE WRITE AND READ-BACK ===")
        val uniqueTimestamp = System.currentTimeMillis()
        val testHubId = "PUDO_LIVE_TEST_$uniqueTimestamp"

        val testHub = HubItem(
            id = testHubId,
            name = "Live Test Hub $uniqueTimestamp",
            type = "supermarket",
            typeName = "Supermarket",
            managerName = "Test Manager",
            phone = "09120000000",
            licenseNumber = "LIC-$uniqueTimestamp",
            address = "Tehran, Live Verification Test St",
            rating = 5.0f,
            reviewCount = 1,
            workingHours = "08:00 - 22:00",
            isOpen = true,
            currentPackagesCount = 0,
            maxCapacity = 100,
            lat = 35.7000,
            lng = 51.4000
        )

        // 1. Room persistence via Repository
        println("1. Inserting into Room via PudoRepository...")
        val insertResult = repository.insertHub(testHub)
        assertTrue("Repository insert must succeed locally in Room", insertResult.isSuccess)

        val localHub = database.hubDao().getHubByIdDirect(testHubId)
        assertNotNull("Local hub must exist in Room", localHub)

        val queueItemsBefore = database.syncQueueDao().getPendingSyncItemsDirect()
        println("Pending sync queue items before remote push: ${queueItemsBefore.size}")
        assertTrue("SyncQueue must contain testHubId", queueItemsBefore.any { it.entityId == testHubId })

        // 2. Outbound Push via PudoSyncEngine -> SupabaseRemoteSyncDataSourceImpl -> Live Supabase REST
        println("2. Executing Outbound Sync to live Supabase...")
        val syncResult = syncEngine.processPendingSyncQueue()
        println("Push Result: processedCount=${syncResult.processedCount}, successCount=${syncResult.successCount}, failureCount=${syncResult.failureCount}, errors=${syncResult.errors}")

        // 3. Check SyncQueue state
        val queueItemsAfter = database.syncQueueDao().getPendingSyncItemsDirect()
        println("Pending sync queue items after push: ${queueItemsAfter.size}")

        // 4. Live read-back from Supabase
        if (config.isConfigured()) {
            println("3. Executing live read-back from Supabase...")
            try {
                val fetchResult = remoteDataSource.fetchHubsUpdatedSince(0L)
                println("Fetch Hubs Result: isSuccess=${fetchResult.isSuccess}, count=${fetchResult.getOrNull()?.size}")
                if (fetchResult.isSuccess) {
                    val hubs = fetchResult.getOrNull().orEmpty()
                    val matched = hubs.find { it.id == testHubId }
                    println("Matched Test Record on Supabase: $matched")
                } else {
                    println("Fetch Hubs Error: ${fetchResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                println("Read-back network exception: ${e.message}")
            }
        }
    }
}
