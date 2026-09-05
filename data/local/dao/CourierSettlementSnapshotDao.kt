package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.CourierSettlementSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CourierSettlementSnapshotDao {

    @Query("SELECT * FROM courier_settlement_snapshots WHERE courierId = :courierId ORDER BY periodEndDate DESC")
    fun getSnapshotsByCourier(courierId: String): Flow<List<CourierSettlementSnapshotEntity>>

    @Query("SELECT * FROM courier_settlement_snapshots ORDER BY periodEndDate DESC")
    fun getAllSnapshots(): Flow<List<CourierSettlementSnapshotEntity>>

    @Query("SELECT * FROM courier_settlement_snapshots WHERE snapshotId = :snapshotId LIMIT 1")
    suspend fun getSnapshotById(snapshotId: String): CourierSettlementSnapshotEntity?

    @Query("SELECT * FROM courier_settlement_snapshots WHERE idempotencyKey = :key LIMIT 1")
    suspend fun getSnapshotByIdempotencyKey(key: String): CourierSettlementSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSnapshot(snapshot: CourierSettlementSnapshotEntity)

    @Update
    suspend fun updateSnapshot(snapshot: CourierSettlementSnapshotEntity)
}
