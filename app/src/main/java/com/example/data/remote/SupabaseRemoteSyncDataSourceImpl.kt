package com.example.data.remote

import com.example.data.remote.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class SupabaseRemoteSyncDataSourceImpl(
    private val config: SupabaseConfig = SupabaseConfig.default(),
    private val client: OkHttpClient = createDefaultOkHttpClient(),
    private val json: Json = defaultJson
) : RemoteSyncDataSource {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun upsertUser(userDto: SupabaseUserDto): Result<SupabaseUserDto> {
        return executeUpsert(
            path = "users",
            payload = json.encodeToString(userDto),
            fallback = userDto
        )
    }

    override suspend fun upsertHub(hubDto: SupabaseHubDto): Result<SupabaseHubDto> {
        return executeUpsert(
            path = "hubs",
            payload = json.encodeToString(hubDto),
            fallback = hubDto
        )
    }

    override suspend fun upsertParcel(parcelDto: SupabaseParcelDto): Result<SupabaseParcelDto> {
        return executeUpsert(
            path = "parcels",
            payload = json.encodeToString(parcelDto),
            fallback = parcelDto
        )
    }

    override suspend fun pushAuditLog(auditLogDto: SupabaseAuditLogDto): Result<SupabaseAuditLogDto> {
        return executeUpsert(
            path = "audit_logs",
            payload = json.encodeToString(auditLogDto),
            fallback = auditLogDto
        )
    }

    override suspend fun pushRegistrationTransaction(txDto: SupabaseRegistrationTransactionDto): Result<SupabaseRegistrationTransactionDto> {
        return executeUpsert(
            path = "registration_transactions",
            payload = json.encodeToString(txDto),
            fallback = txDto
        )
    }

    override suspend fun deleteUser(userId: String): Result<Boolean> {
        return executeDelete("users?id=eq.$userId")
    }

    override suspend fun deleteHub(hubId: String): Result<Boolean> {
        return executeDelete("hubs?id=eq.$hubId")
    }

    override suspend fun deleteParcel(parcelId: String): Result<Boolean> {
        return executeDelete("parcels?id=eq.$parcelId")
    }

    override suspend fun fetchUsersUpdatedSince(timestamp: Long): Result<List<SupabaseUserDto>> {
        return executeFetch("users?select=*&updated_at=gte.$timestamp&order=updated_at.asc")
    }

    override suspend fun fetchHubsUpdatedSince(timestamp: Long): Result<List<SupabaseHubDto>> {
        return executeFetch("hubs?select=*&updated_at=gte.$timestamp&order=updated_at.asc")
    }

    override suspend fun fetchParcelsUpdatedSince(timestamp: Long): Result<List<SupabaseParcelDto>> {
        return executeFetch("parcels?select=*&updated_at=gte.$timestamp&order=updated_at.asc")
    }

    override suspend fun fetchAuditLogsSince(timestamp: Long): Result<List<SupabaseAuditLogDto>> {
        return executeFetch("audit_logs?select=*&timestamp=gte.$timestamp&order=timestamp.asc")
    }

    override suspend fun fetchRegistrationTransactionsSince(timestamp: Long): Result<List<SupabaseRegistrationTransactionDto>> {
        return executeFetch("registration_transactions?select=*&timestamp=gte.$timestamp&order=timestamp.asc")
    }

    private suspend fun <T> executeUpsert(
        path: String,
        payload: String,
        fallback: T
    ): Result<T> = withContext(Dispatchers.IO) {
        runCatching {
            if (!config.isConfigured()) {
                throw IllegalStateException("Supabase is not configured. Set SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY in .env")
            }

            val request = buildRequest(
                path = path,
                method = "POST",
                bodyJson = payload,
                preferHeader = "resolution=merge-duplicates,return=representation"
            )

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    val errorMsg = response.body?.string()?.take(200) ?: "HTTP $code"
                    throw IOException("Supabase upsert failed at $path with code $code: $errorMsg")
                }
                fallback
            }
        }
    }

    private suspend fun executeDelete(
        pathWithQuery: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            if (!config.isConfigured()) {
                throw IllegalStateException("Supabase is not configured. Set SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY in .env")
            }

            val request = buildRequest(
                path = pathWithQuery,
                method = "DELETE"
            )

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful && response.code != 404) {
                    val code = response.code
                    val errorMsg = response.body?.string()?.take(200) ?: "HTTP $code"
                    throw IOException("Supabase delete failed at $pathWithQuery with code $code: $errorMsg")
                }
                true
            }
        }
    }

    private suspend inline fun <reified T> executeFetch(
        pathWithQuery: String
    ): Result<List<T>> = withContext(Dispatchers.IO) {
        runCatching {
            if (!config.isConfigured()) {
                throw IllegalStateException("Supabase is not configured. Set SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY in .env")
            }

            val request = buildRequest(
                path = pathWithQuery,
                method = "GET"
            )

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    val errorMsg = response.body?.string()?.take(200) ?: "HTTP $code"
                    throw IOException("Supabase fetch failed at $pathWithQuery with code $code: $errorMsg")
                }
                val bodyString = response.body?.string().orEmpty()
                if (bodyString.isBlank()) {
                    emptyList()
                } else {
                    json.decodeFromString<List<T>>(bodyString)
                }
            }
        }
    }

    private fun buildRequest(
        path: String,
        method: String = "GET",
        bodyJson: String? = null,
        preferHeader: String? = null
    ): Request {
        val baseUrl = config.url.trimEnd('/')
        val url = "$baseUrl/rest/v1/$path"
        val requestBuilder = Request.Builder()
            .url(url)
            .addHeader("apikey", config.publishableKey)
            .addHeader("Authorization", "Bearer ${config.publishableKey}")
            .addHeader("Accept", "application/json")

        if (preferHeader != null) {
            requestBuilder.addHeader("Prefer", preferHeader)
        }

        val requestBody = bodyJson?.toRequestBody(jsonMediaType)
        when (method) {
            "GET" -> requestBuilder.get()
            "POST" -> requestBuilder.post(requestBody ?: "".toRequestBody(jsonMediaType))
            "DELETE" -> requestBuilder.delete(requestBody)
            "PATCH" -> requestBuilder.patch(requestBody ?: "".toRequestBody(jsonMediaType))
            "PUT" -> requestBuilder.put(requestBody ?: "".toRequestBody(jsonMediaType))
        }

        return requestBuilder.build()
    }

    companion object {
        val defaultJson: Json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }

        fun createDefaultOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        }
    }
}
