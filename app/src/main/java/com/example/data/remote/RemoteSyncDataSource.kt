package com.example.data.remote

import com.example.data.remote.dto.*

/**
 * Contract for remote synchronization operations with Supabase / Cloud backend.
 * All operations return application-level [Result] types and are strictly non-blocking.
 * 
 * ARCHITECTURAL RULE:
 * UI and ViewModels MUST NEVER interact with this interface directly.
 * All access must be routed through PudoSyncEngine and PudoRepository.
 */
interface RemoteSyncDataSource {

    suspend fun upsertUser(userDto: SupabaseUserDto): Result<SupabaseUserDto>

    suspend fun upsertHub(hubDto: SupabaseHubDto): Result<SupabaseHubDto>

    suspend fun upsertParcel(parcelDto: SupabaseParcelDto): Result<SupabaseParcelDto>

    suspend fun pushAuditLog(auditLogDto: SupabaseAuditLogDto): Result<SupabaseAuditLogDto>

    suspend fun pushRegistrationTransaction(txDto: SupabaseRegistrationTransactionDto): Result<SupabaseRegistrationTransactionDto>

    suspend fun deleteUser(userId: String): Result<Boolean>

    suspend fun deleteHub(hubId: String): Result<Boolean>

    suspend fun deleteParcel(parcelId: String): Result<Boolean>

    suspend fun fetchUsersUpdatedSince(timestamp: Long): Result<List<SupabaseUserDto>>

    suspend fun fetchHubsUpdatedSince(timestamp: Long): Result<List<SupabaseHubDto>>

    suspend fun fetchParcelsUpdatedSince(timestamp: Long): Result<List<SupabaseParcelDto>>

    suspend fun fetchAuditLogsSince(timestamp: Long): Result<List<SupabaseAuditLogDto>>

    suspend fun fetchRegistrationTransactionsSince(timestamp: Long): Result<List<SupabaseRegistrationTransactionDto>>
}
