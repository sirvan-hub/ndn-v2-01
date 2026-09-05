package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.ParcelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ParcelDao {

    @Query("SELECT * FROM parcels ORDER BY updatedAt DESC")
    fun getAllParcels(): Flow<List<ParcelEntity>>

    @Query("SELECT * FROM parcels ORDER BY updatedAt DESC")
    suspend fun getAllParcelsDirect(): List<ParcelEntity>

    @Query("SELECT * FROM parcels WHERE id = :id")
    fun getParcelById(id: String): Flow<ParcelEntity?>

    @Query("SELECT * FROM parcels WHERE id = :id")
    suspend fun getParcelByIdDirect(id: String): ParcelEntity?

    @Query("SELECT * FROM parcels WHERE trackingNumber = :trackingNumber LIMIT 1")
    suspend fun getParcelByTrackingNumber(trackingNumber: String): ParcelEntity?

    @Query("SELECT * FROM parcels WHERE courierId = :courierId ORDER BY updatedAt DESC")
    fun getParcelsByCourier(courierId: String): Flow<List<ParcelEntity>>

    @Query("SELECT * FROM parcels WHERE hubId = :hubId ORDER BY updatedAt DESC")
    fun getParcelsByHub(hubId: String): Flow<List<ParcelEntity>>

    @Query("SELECT * FROM parcels WHERE status = :status ORDER BY updatedAt DESC")
    fun getParcelsByStatus(status: String): Flow<List<ParcelEntity>>

    @Query("SELECT * FROM parcels WHERE recipientPhone = :phone ORDER BY updatedAt DESC")
    fun getParcelsByRecipientPhone(phone: String): Flow<List<ParcelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParcel(parcel: ParcelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParcels(parcels: List<ParcelEntity>)

    @Update
    suspend fun updateParcel(parcel: ParcelEntity)

    @Query("UPDATE parcels SET status = :newStatus, updatedAt = :updatedAt WHERE id = :parcelId")
    suspend fun updateParcelStatus(parcelId: String, newStatus: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE parcels SET hubId = :hubId, hubName = :hubName, updatedAt = :updatedAt WHERE id = :parcelId")
    suspend fun assignHub(parcelId: String, hubId: String, hubName: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM parcels WHERE id = :id")
    suspend fun deleteParcelById(id: String)
}
