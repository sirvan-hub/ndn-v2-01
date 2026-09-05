package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.SettlementTariffVersionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TariffDao {

    @Query("SELECT * FROM settlement_tariffs WHERE isActive = 1 ORDER BY versionCode DESC LIMIT 1")
    fun getActiveTariff(): Flow<SettlementTariffVersionEntity?>

    @Query("SELECT * FROM settlement_tariffs WHERE isActive = 1 ORDER BY versionCode DESC LIMIT 1")
    suspend fun getActiveTariffDirect(): SettlementTariffVersionEntity?

    @Query("SELECT * FROM settlement_tariffs ORDER BY versionCode DESC")
    fun getAllTariffVersions(): Flow<List<SettlementTariffVersionEntity>>

    @Query("SELECT * FROM settlement_tariffs WHERE id = :id LIMIT 1")
    suspend fun getTariffById(id: String): SettlementTariffVersionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTariff(tariff: SettlementTariffVersionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTariffs(tariffs: List<SettlementTariffVersionEntity>)

    @Update
    suspend fun updateTariff(tariff: SettlementTariffVersionEntity)
}
