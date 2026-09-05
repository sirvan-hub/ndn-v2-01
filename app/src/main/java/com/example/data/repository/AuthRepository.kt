package com.example.data.repository

import com.example.data.model.AccountApprovalStatus
import com.example.data.model.MobileChangeRequest
import com.example.data.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getAllUsers(): Flow<List<User>>
    fun getUsersByRole(role: String): Flow<List<User>>
    fun getUsersByApprovalStatus(status: AccountApprovalStatus): Flow<List<User>>
    suspend fun getUserById(id: String): User?
    suspend fun getUserByPhone(phone: String): User?
    suspend fun getUserByUsername(username: String): User?
    suspend fun getUserByNationalId(nationalId: String): User?
    suspend fun registerUser(user: User): Result<User>
    suspend fun registerUserWithCredentials(user: User, rawPassword: String): Result<User>
    suspend fun createAdminUser(
        creatorAdminId: String,
        adminUser: User,
        rawPassword: String
    ): Result<User>

    /**
     * Creates any-role user (CUSTOMER / COURIER / HUB_MANAGER / ADMIN / SYSTEM_ADMIN) on behalf of
     * an authenticated System Admin. Only a caller whose account role is ADMIN or SYSTEM_ADMIN may
     * succeed. The created account is pre-approved since a System Admin has already vetted it.
     */
    suspend fun createUserByAdmin(
        creatorAdminId: String,
        newUser: User,
        rawPassword: String
    ): Result<User>

    suspend fun updateApprovalStatus(userId: String, newStatus: AccountApprovalStatus, actorId: String): Result<User>

    /** Verifies the account's current password before rotating it to a new hashed password. */
    suspend fun changePassword(userId: String, currentRawPassword: String, newRawPassword: String): Result<Unit>

    /** Authenticates raw credentials against the stored password hash. Returns the user on success. */
    suspend fun authenticate(usernameOrPhone: String, rawPassword: String): Result<User>

    suspend fun deleteUser(userId: String, actorId: String): Result<Unit>
    
    // Mobile Number Change Workflow
    fun getMobileChangeRequests(): Flow<List<MobileChangeRequest>>
    suspend fun requestMobileChange(userId: String, requestedPhone: String): Result<MobileChangeRequest>
    suspend fun approveMobileChange(requestId: String, adminId: String, adminRole: String = "ADMIN"): Result<User>
    suspend fun rejectMobileChange(requestId: String, adminId: String, adminRole: String = "ADMIN", reason: String = ""): Result<Unit>

    suspend fun seedUsersIfEmpty(seedUsers: List<User>)

    suspend fun syncAndMergeUsers(remoteUsers: List<User>): Result<Int>

    /**
     * Ensures exactly one real, database-backed System Admin account exists.
     * Idempotent: does nothing if a "reza" username (or any SYSTEM_ADMIN role) already exists.
     * This is the ONLY account the app ships with; there is no hardcoded/demo login path.
     */
    suspend fun seedSystemAdminIfMissing(
        username: String,
        rawPassword: String,
        fullName: String,
        phone: String
    )
}
