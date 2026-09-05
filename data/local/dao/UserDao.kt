package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    suspend fun getAllUsersDirect(): List<UserEntity>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun getUserByPhone(phone: String): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username AND username != '' LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE nationalId = :nationalId LIMIT 1")
    suspend fun getUserByNationalId(nationalId: String): UserEntity?

    @Query("SELECT * FROM users WHERE phone = :phone OR (nationalId != '' AND nationalId = :nationalId) OR (username != '' AND username = :username)")
    suspend fun getUserByPhoneOrNationalIdOrUsername(phone: String, nationalId: String, username: String): List<UserEntity>

    @Query("SELECT * FROM users WHERE phone = :phone OR (nationalId != '' AND nationalId = :nationalId)")
    suspend fun getUserByPhoneOrNationalId(phone: String, nationalId: String): List<UserEntity>

    @Query("SELECT * FROM users WHERE role = :role ORDER BY createdAt DESC")
    fun getUsersByRole(role: String): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE approvalStatus = :status ORDER BY createdAt DESC")
    fun getUsersByApprovalStatus(status: String): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET phone = :newPhone, updatedAt = :updatedAt WHERE id = :userId")
    suspend fun updatePhone(userId: String, newPhone: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE users SET approvalStatus = :newStatus, updatedAt = :updatedAt WHERE id = :userId")
    suspend fun updateApprovalStatus(userId: String, newStatus: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUserById(id: String)
}
