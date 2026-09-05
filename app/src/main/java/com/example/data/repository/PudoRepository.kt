package com.example.data.repository

import com.example.data.model.*
import com.example.model.HubItem
import kotlinx.coroutines.flow.Flow

interface PudoRepository {

    // Hub Operations & Persistence (Single source of truth in Room)
    fun getAllHubs(): Flow<List<HubItem>>
    fun getHubById(id: String): Flow<HubItem?>
    // SECURITY: actorId is the real, currently-signed-in caller's user id. It is independently
    // re-verified against the database (must be ADMIN/SYSTEM_ADMIN) inside the implementation —
    // never trust a caller-supplied role, and never hardcode the audit actor to a literal string.
    suspend fun insertHub(hub: HubItem, actorId: String): Result<HubItem>
    suspend fun updateHub(hub: HubItem, actorId: String): Result<HubItem>
    suspend fun deleteHub(id: String, actorId: String): Result<Unit>
    suspend fun seedHubsIfEmpty(seedHubs: List<HubItem>)

    // Reactive streams
    fun getAllParcels(): Flow<List<Parcel>>
    fun getParcelById(id: String): Flow<Parcel?>
    fun getCourierParcels(courierId: String): Flow<List<Parcel>>
    fun getHubParcels(hubId: String): Flow<List<Parcel>>
    fun getParcelsByStatus(status: ParcelStatus): Flow<List<Parcel>>
    fun getParcelsByRecipientPhone(phone: String): Flow<List<Parcel>>

    // Parcel Lifecycle & Operations (guarded by ParcelStateMachine & Atomic Room Transactions)
    suspend fun registerPudoParcel(
        parcel: Parcel,
        transaction: RegistrationTransaction,
        actorId: String,
        actorRole: String
    ): Result<Parcel>

    suspend fun registerFailedDelivery(
        parcel: Parcel,
        transaction: RegistrationTransaction,
        actorId: String,
        actorRole: String
    ): Result<Parcel>

    suspend fun assignHub(
        parcelId: String,
        hubId: String,
        hubName: String,
        actorId: String,
        actorRole: String
    ): Result<Parcel>

    suspend fun startHandover(
        parcelId: String,
        otp: String,
        actorId: String,
        actorRole: String
    ): Result<Parcel>

    suspend fun confirmHubHandover(
        parcelId: String,
        inputOtp: String?,
        actorId: String,
        actorRole: String
    ): Result<Parcel>

    suspend fun receivePackageAtHub(
        parcelId: String,
        actorId: String,
        actorRole: String
    ): Result<Parcel>

    suspend fun deliverToCustomer(
        parcelId: String,
        actorId: String,
        actorRole: String
    ): Result<Parcel>

    suspend fun payPackageFee(
        parcelId: String,
        actorId: String,
        actorRole: String
    ): Result<Parcel>

    suspend fun returnToSender(
        parcelId: String,
        reason: String,
        actorId: String,
        actorRole: String
    ): Result<Parcel>

    suspend fun transitionParcelStatus(
        parcelId: String,
        targetStatus: ParcelStatus,
        actorId: String,
        actorRole: String,
        reason: String? = null
    ): Result<Parcel>

    // Settlement & Tariff
    fun getActiveTariff(): Flow<SettlementTariffVersion?>
    fun getAllTariffs(): Flow<List<SettlementTariffVersion>>
    suspend fun saveTariff(tariff: SettlementTariffVersion)
    suspend fun createSettlementSnapshot(snapshot: CourierSettlementSnapshot): Result<CourierSettlementSnapshot>
    fun getCourierSettlementSnapshots(courierId: String): Flow<List<CourierSettlementSnapshot>>

    // Audit & Sync Queue
    fun getAllAuditLogs(): Flow<List<AuditLog>>
    fun getAuditLogsForEntity(entityId: String): Flow<List<AuditLog>>
    fun getPendingSyncQueueItems(): Flow<List<SyncQueueItem>>

    // Seed / Direct initialization
    suspend fun seedParcelsIfEmpty(seedParcels: List<Parcel>)
    suspend fun seedTariffIfEmpty(defaultTariff: SettlementTariffVersion)
    suspend fun syncAndMergeParcels(remoteParcels: List<Parcel>): Result<Int>
}
