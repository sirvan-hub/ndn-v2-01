package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.HubEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HubDao {

    @Query("SELECT * FROM hubs ORDER BY createdAt DESC")
    fun getAllHubs(): Flow<List<HubEntity>>

    @Query("SELECT * FROM hubs WHERE id = :id LIMIT 1")
    fun getHubById(id: String): Flow<HubEntity?>

    @Query("SELECT * FROM hubs WHERE id = :id LIMIT 1")
    suspend fun getHubByIdDirect(id: String): HubEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHub(hub: HubEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHubs(hubs: List<HubEntity>)

    @Update
    suspend fun updateHub(hub: HubEntity)

    @Query("DELETE FROM hubs WHERE id = :id")
    suspend fun deleteHubById(id: String)

    @Query("SELECT COUNT(*) FROM hubs")
    suspend fun getHubCount(): Int
}
