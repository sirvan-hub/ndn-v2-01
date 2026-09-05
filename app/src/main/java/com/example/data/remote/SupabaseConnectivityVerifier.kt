package com.example.data.remote

import com.example.data.remote.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI

data class ConfigurationValidationReport(
    val isConfigured: Boolean,
    val sanitizedUrlHost: String,
    val isKeyPresent: Boolean,
    val keyLength: Int,
    val statusMessage: String
)

data class SchemaCompatibilityReport(
    val verifiedTables: List<String>,
    val expectedColumns: Map<String, List<String>>,
    val upsertConflictKeys: Map<String, String>,
    val isCompatible: Boolean
)

data class RemotePushVerificationResult(
    val httpStatusCode: Int?,
    val isSuccess: Boolean,
    val entityType: String,
    val entityId: String,
    val syncedTimestamp: Long?,
    val diagnosticMessage: String
)

/**
 * Diagnostic and Connectivity Verification utility for Supabase integration.
 * Adheres strictly to security mandates:
 * - Never prints publishable keys or secrets.
 * - Formats diagnostics for audit and validation.
 */
class SupabaseConnectivityVerifier(
    private val config: SupabaseConfig = SupabaseConfig.default(),
    private val client: OkHttpClient = SupabaseRemoteSyncDataSourceImpl.createDefaultOkHttpClient()
) {

    /**
     * Validates runtime configuration safely.
     */
    fun validateConfiguration(): ConfigurationValidationReport {
        val host = try {
            if (config.url.isNotBlank()) {
                URI.create(config.url).host ?: "unknown-host"
            } else {
                "none"
            }
        } catch (_: Exception) {
            "invalid-url"
        }

        val isKeyPresent = config.publishableKey.isNotBlank()
        val isConfigured = config.isConfigured()

        val statusMessage = if (isConfigured) {
            "Supabase configuration is valid and ready for remote operations."
        } else {
            "Supabase is not configured (placeholder or missing credentials in BuildConfig)."
        }

        return ConfigurationValidationReport(
            isConfigured = isConfigured,
            sanitizedUrlHost = host,
            isKeyPresent = isKeyPresent,
            keyLength = if (isKeyPresent) config.publishableKey.length else 0,
            statusMessage = statusMessage
        )
    }

    /**
     * Returns schema definitions and mapping requirements for PostgREST tables.
     */
    fun getSchemaCompatibility(): SchemaCompatibilityReport {
        val expectedColumns = mapOf(
            "users" to listOf(
                "id", "username", "password_hash", "full_name", "phone",
                "national_id", "email", "postal_code", "address", "role",
                "approval_status", "store_name", "guild_type", "bank_card_number",
                "vehicle_type", "created_at", "updated_at"
            ),
            "hubs" to listOf(
                "id", "name", "type", "type_name", "manager_name", "phone",
                "license_number", "address", "rating", "review_count", "working_hours",
                "is_open", "current_packages_count", "max_capacity", "lat", "lng",
                "created_at", "updated_at"
            ),
            "parcels" to listOf(
                "id", "tracking_number", "sender_name", "recipient_name", "recipient_phone",
                "recipient_postal_code", "recipient_address", "status", "size",
                "assigned_hub_id", "assigned_hub_name", "assigned_courier_id", "assigned_courier_name",
                "registration_source", "base_fee", "is_settled", "handover_otp",
                "created_at", "updated_at"
            ),
            "audit_logs" to listOf(
                "id", "event_type", "actor_id", "actor_role", "entity_id",
                "old_state", "new_state", "transaction_id", "metadata_json", "timestamp"
            ),
            "registration_transactions" to listOf(
                "transaction_id", "parcel_id", "tracking_number", "courier_id",
                "hub_id", "registration_source", "timestamp", "client_generated_id"
            )
        )

        val conflictKeys = mapOf(
            "users" to "id",
            "hubs" to "id",
            "parcels" to "id",
            "audit_logs" to "id",
            "registration_transactions" to "transaction_id"
        )

        return SchemaCompatibilityReport(
            verifiedTables = listOf("users", "hubs", "parcels", "audit_logs", "registration_transactions"),
            expectedColumns = expectedColumns,
            upsertConflictKeys = conflictKeys,
            isCompatible = true
        )
    }

    /**
     * Attempts a live network check to the Supabase endpoint if configured.
     */
    suspend fun verifyLiveEndpointConnectivity(): RemotePushVerificationResult = withContext(Dispatchers.IO) {
        val configReport = validateConfiguration()
        if (!configReport.isConfigured) {
            return@withContext RemotePushVerificationResult(
                httpStatusCode = null,
                isSuccess = false,
                entityType = "PING",
                entityId = "health-check",
                syncedTimestamp = null,
                diagnosticMessage = "Configuration is unconfigured / placeholder. Network push skipped safely."
            )
        }

        try {
            val url = "${config.url.trimEnd('/')}/rest/v1/"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", config.publishableKey)
                .addHeader("Authorization", "Bearer ${config.publishableKey}")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                RemotePushVerificationResult(
                    httpStatusCode = response.code,
                    isSuccess = response.isSuccessful,
                    entityType = "ROOT_REST",
                    entityId = "health-check",
                    syncedTimestamp = if (response.isSuccessful) System.currentTimeMillis() else null,
                    diagnosticMessage = "Supabase endpoint returned HTTP ${response.code} (successful=${response.isSuccessful})"
                )
            }
        } catch (e: Exception) {
            RemotePushVerificationResult(
                httpStatusCode = null,
                isSuccess = false,
                entityType = "ROOT_REST",
                entityId = "health-check",
                syncedTimestamp = null,
                diagnosticMessage = "Network connectivity attempt failed: ${e.javaClass.simpleName}: ${e.message}"
            )
        }
    }

    /**
     * Checks inbound delta query endpoints for all 5 PostgREST entities.
     */
    suspend fun verifyInboundFetchCapabilities(sinceTimestamp: Long = 0L): Map<String, RemotePushVerificationResult> = withContext(Dispatchers.IO) {
        val tables = listOf("users", "hubs", "parcels", "audit_logs", "registration_transactions")
        val timestampColumnMap = mapOf(
            "users" to "updated_at",
            "hubs" to "updated_at",
            "parcels" to "updated_at",
            "audit_logs" to "timestamp",
            "registration_transactions" to "timestamp"
        )

        val results = mutableMapOf<String, RemotePushVerificationResult>()
        val configReport = validateConfiguration()

        if (!configReport.isConfigured) {
            tables.forEach { table ->
                results[table] = RemotePushVerificationResult(
                    httpStatusCode = null,
                    isSuccess = false,
                    entityType = table,
                    entityId = "delta-fetch",
                    syncedTimestamp = null,
                    diagnosticMessage = "Supabase is unconfigured; fetch skipped safely."
                )
            }
            return@withContext results
        }

        tables.forEach { table ->
            val timeCol = timestampColumnMap[table] ?: "updated_at"
            val url = "${config.url.trimEnd('/')}/rest/v1/$table?select=*&$timeCol=gte.$sinceTimestamp&order=$timeCol.asc&limit=5"
            try {
                val request = Request.Builder()
                    .url(url)
                    .addHeader("apikey", config.publishableKey)
                    .addHeader("Authorization", "Bearer ${config.publishableKey}")
                    .addHeader("Accept", "application/json")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    results[table] = RemotePushVerificationResult(
                        httpStatusCode = response.code,
                        isSuccess = response.isSuccessful,
                        entityType = table,
                        entityId = "delta-fetch",
                        syncedTimestamp = if (response.isSuccessful) System.currentTimeMillis() else null,
                        diagnosticMessage = "Table $table returned HTTP ${response.code} (successful=${response.isSuccessful})"
                    )
                }
            } catch (e: Exception) {
                results[table] = RemotePushVerificationResult(
                    httpStatusCode = null,
                    isSuccess = false,
                    entityType = table,
                    entityId = "delta-fetch",
                    syncedTimestamp = null,
                    diagnosticMessage = "Inbound query failed for $table: ${e.javaClass.simpleName}: ${e.message}"
                )
            }
        }

        results
    }
}
