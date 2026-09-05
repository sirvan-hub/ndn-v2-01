package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.MobileChangeRequestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MobileChangeRequestDao {

    @Query("SELECT * FROM mobile_change_requests ORDER BY requestedAt DESC")
    fun getAllRequests(): Flow<List<MobileChangeRequestEntity>>

    @Query("SELECT * FROM mobile_change_requests WHERE status = :status ORDER BY requestedAt DESC")
    fun getRequestsByStatus(status: String): Flow<List<MobileChangeRequestEntity>>

    @Query("SELECT * FROM mobile_change_requests WHERE id = :id LIMIT 1")
    suspend fun getRequestById(id: String): MobileChangeRequestEntity?

    @Query("SELECT * FROM mobile_change_requests WHERE userId = :userId AND status = 'PENDING_ADMIN_APPROVAL' LIMIT 1")
    suspend fun getPendingRequestByUserId(userId: String): MobileChangeRequestEntity?

    @Query("SELECT * FROM mobile_change_requests WHERE requestedPhone = :phone AND status = 'PENDING_ADMIN_APPROVAL' LIMIT 1")
    suspend fun getPendingRequestByPhone(phone: String): MobileChangeRequestEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRequest(request: MobileChangeRequestEntity)

    @Update
    suspend fun updateRequest(request: MobileChangeRequestEntity)
}
