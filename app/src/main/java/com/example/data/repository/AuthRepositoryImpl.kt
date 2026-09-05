package com.example.data.repository

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import com.example.data.local.database.PudoDatabase
import com.example.data.local.entities.AuditLogEntity
import com.example.data.local.entities.SyncQueueEntity
import com.example.data.local.mappers.*
import com.example.data.model.*
import com.example.util.IdentityNormalizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class AuthRepositoryImpl(
    private val database: PudoDatabase
) : AuthRepository {

    private val userDao = database.userDao()
    private val auditLogDao = database.auditLogDao()
    private val syncQueueDao = database.syncQueueDao()
    private val mobileChangeRequestDao = database.mobileChangeRequestDao()

    override fun getAllUsers(): Flow<List<User>> {
        return userDao.getAllUsers().map { list -> list.map { it.toDomain() } }
    }

    override fun getUsersByRole(role: String): Flow<List<User>> {
        return userDao.getUsersByRole(role).map { list -> list.map { it.toDomain() } }
    }

    override fun getUsersByApprovalStatus(status: AccountApprovalStatus): Flow<List<User>> {
        return userDao.getUsersByApprovalStatus(status.name).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getUserById(id: String): User? {
        return userDao.getUserById(id)?.toDomain()
    }

    override suspend fun getUserByPhone(phone: String): User? {
        val normalized = IdentityNormalizer.normalizePhone(phone)
        return userDao.getUserByPhone(normalized)?.toDomain()
    }

    override suspend fun getUserByNationalId(nationalId: String): User? {
        val normalized = IdentityNormalizer.normalizeNationalId(nationalId)
        if (normalized.isBlank()) return null
        return userDao.getUserByNationalId(normalized)?.toDomain()
    }

    override suspend fun getUserByUsername(username: String): User? {
        val normalized = IdentityNormalizer.normalizeUsername(username)
        if (normalized.isBlank()) return null
        return userDao.getUserByUsername(normalized)?.toDomain()
    }

    override suspend fun registerUser(user: User): Result<User> {
        return performUserRegistration(user, "", isCalledByAdmin = false)
    }

    override suspend fun registerUserWithCredentials(user: User, rawPassword: String): Result<User> {
        return performUserRegistration(user, rawPassword, isCalledByAdmin = false)
    }

    private suspend fun performUserRegistration(
        user: User,
        rawPassword: String,
        isCalledByAdmin: Boolean
    ): Result<User> {
        // SECURITY: registerUser/registerUserWithCredentials is the PUBLIC, unauthenticated
        // self-registration entry point (no actor/admin id involved). It must never be able to
        // create an ADMIN/SYSTEM_ADMIN account — that would let anyone self-elevate to admin.
        // Admin-tier accounts may ONLY be created via createAdminUser()/createUserByAdmin(),
        // which require an authenticated, already-admin caller (isCalledByAdmin = true).
        if (!isCalledByAdmin && isAdminRole(user.role)) {
            return Result.failure(RegistrationException.UnauthorizedAdminCreationException())
        }

        val normalizedPhone = IdentityNormalizer.normalizePhone(user.phone)
        val normalizedNationalId = IdentityNormalizer.normalizeNationalId(user.nationalId)
        val rawNormUsername = IdentityNormalizer.normalizeUsername(user.username)
        val normalizedUsername = if (rawNormUsername.isNotBlank()) rawNormUsername else ""

        // 1. Validate Formats
        if (!IdentityNormalizer.isValidIranianMobile(normalizedPhone)) {
            return Result.failure(RegistrationException.InvalidPhoneException(user.phone))
        }
        if (normalizedNationalId.isNotBlank() && !IdentityNormalizer.isValidNationalId(normalizedNationalId)) {
            return Result.failure(RegistrationException.InvalidNationalIdException(user.nationalId))
        }
        if (normalizedUsername.isNotBlank() && !IdentityNormalizer.isValidUsername(normalizedUsername)) {
            return Result.failure(RegistrationException.InvalidUsernameException(user.username))
        }
        if (rawPassword.isNotBlank() && rawPassword.length < 6) {
            return Result.failure(RegistrationException.InvalidPasswordException("رمز عبور باید حداقل ۶ کاراکتر باشد."))
        }

        // 2. Proactive Duplicate Checks
        val existingByPhone = userDao.getUserByPhone(normalizedPhone)
        val existingByNationalId = if (normalizedNationalId.isNotBlank()) {
            userDao.getUserByNationalId(normalizedNationalId)
        } else {
            null
        }
        val existingByUsername = if (normalizedUsername.isNotBlank()) {
            userDao.getUserByUsername(normalizedUsername)
        } else {
            null
        }

        if (existingByUsername != null) {
            return Result.failure(RegistrationException.DuplicateUsernameException(normalizedUsername))
        } else if (existingByPhone != null && existingByNationalId != null) {
            return Result.failure(RegistrationException.DuplicateBothException())
        } else if (existingByPhone != null) {
            return Result.failure(RegistrationException.DuplicatePhoneException(normalizedPhone))
        } else if (existingByNationalId != null) {
            return Result.failure(RegistrationException.DuplicateNationalIdException(normalizedNationalId))
        }

        val passwordHash = if (rawPassword.isNotBlank()) {
            IdentityNormalizer.hashPassword(rawPassword)
        } else {
            user.passwordHash
        }

        val effectiveUsername = if (normalizedUsername.isNotBlank()) {
            normalizedUsername
        } else {
            if (normalizedPhone.isNotBlank()) normalizedPhone else "user_${user.id.replace("-", "")}"
        }

        val effectiveNationalId = if (normalizedNationalId.isNotBlank()) {
            normalizedNationalId
        } else {
            "nid_${user.id.replace("-", "")}"
        }

        val normalizedUser = user.copy(
            phone = normalizedPhone,
            nationalId = effectiveNationalId,
            username = effectiveUsername,
            passwordHash = passwordHash
        )

        val auditLog = AuditLog(
            id = "audit-${UUID.randomUUID()}",
            eventType = "USER_REGISTERED",
            actorId = normalizedUser.id,
            actorRole = normalizedUser.role,
            entityId = normalizedUser.id,
            oldState = null,
            newState = normalizedUser.approvalStatus.name,
            transactionId = null,
            metadataJson = "{\"username\":\"${normalizedUser.username}\",\"phone\":\"${normalizedUser.phone}\",\"nationalId\":\"${normalizedUser.nationalId}\",\"role\":\"${normalizedUser.role}\"}"
        )

        val syncItem = SyncQueueItem(
            id = "sync-${UUID.randomUUID()}",
            entityType = "USER",
            entityId = normalizedUser.id,
            action = "CREATE"
        )

        // 3. Atomic Database Insertion with SQLite Constraint Handling
        return try {
            database.withTransaction {
                userDao.insertUser(normalizedUser.toEntity())
                auditLogDao.insertLog(auditLog.toEntity())
                syncQueueDao.insertSyncItem(syncItem.toEntity())
            }
            Result.success(normalizedUser)
        } catch (e: SQLiteConstraintException) {
            handleConstraintViolation(e, normalizedPhone, normalizedNationalId, normalizedUsername)
        } catch (e: Exception) {
            val msg = e.message.orEmpty()
            if (msg.contains("UNIQUE constraint failed", ignoreCase = true) || msg.contains("1555") || msg.contains("2067")) {
                handleConstraintViolation(e, normalizedPhone, normalizedNationalId, normalizedUsername)
            } else {
                Result.failure(e)
            }
        }
    }

    private fun isAdminRole(role: String): Boolean = role == "ADMIN" || role == "SYSTEM_ADMIN"

    override suspend fun createAdminUser(
        creatorAdminId: String,
        adminUser: User,
        rawPassword: String
    ): Result<User> {
        val creator = userDao.getUserById(creatorAdminId)
        if (creator == null || !isAdminRole(creator.role)) {
            return Result.failure(RegistrationException.UnauthorizedAdminCreationException())
        }

        val adminUserWithApprovedStatus = adminUser.copy(
            role = "ADMIN",
            approvalStatus = AccountApprovalStatus.APPROVED
        )

        val result = performUserRegistration(adminUserWithApprovedStatus, rawPassword, isCalledByAdmin = true)
        if (result.isSuccess) {
            val created = result.getOrThrow()
            val auditLog = AuditLog(
                id = "audit-${UUID.randomUUID()}",
                eventType = "ADMIN_USER_CREATED",
                actorId = creatorAdminId,
                actorRole = creator.role,
                entityId = created.id,
                oldState = null,
                newState = "APPROVED",
                transactionId = null,
                metadataJson = "{\"createdAdminUsername\":\"${created.username}\",\"role\":\"ADMIN\",\"creator\":\"$creatorAdminId\"}"
            )
            auditLogDao.insertLog(auditLog.toEntity())
        }
        return result
    }

    override suspend fun createUserByAdmin(
        creatorAdminId: String,
        newUser: User,
        rawPassword: String
    ): Result<User> {
        val creator = userDao.getUserById(creatorAdminId)
            ?: return Result.failure(RegistrationException.UnauthorizedAdminCreationException())
        if (!isAdminRole(creator.role)) {
            return Result.failure(RegistrationException.UnauthorizedAdminCreationException())
        }
        // Only a SYSTEM_ADMIN may create further ADMIN / SYSTEM_ADMIN accounts.
        if (isAdminRole(newUser.role) && creator.role != "SYSTEM_ADMIN") {
            return Result.failure(RegistrationException.UnauthorizedAdminCreationException())
        }
        if (rawPassword.isBlank() || rawPassword.length < 6) {
            return Result.failure(RegistrationException.InvalidPasswordException())
        }

        // Admin-created accounts are pre-vetted by a human System Admin, so they are approved immediately.
        val approvedUser = newUser.copy(approvalStatus = AccountApprovalStatus.APPROVED)
        val result = performUserRegistration(approvedUser, rawPassword, isCalledByAdmin = true)
        if (result.isSuccess) {
            val created = result.getOrThrow()
            val auditLog = AuditLog(
                id = "audit-${UUID.randomUUID()}",
                eventType = "USER_CREATED_BY_ADMIN",
                actorId = creatorAdminId,
                actorRole = creator.role,
                entityId = created.id,
                oldState = null,
                newState = "APPROVED",
                transactionId = null,
                metadataJson = "{\"createdUsername\":\"${created.username}\",\"role\":\"${created.role}\",\"creator\":\"$creatorAdminId\"}"
            )
            auditLogDao.insertLog(auditLog.toEntity())
        }
        return result
    }

    override suspend fun authenticate(usernameOrPhone: String, rawPassword: String): Result<User> {
        val normalizedUsername = IdentityNormalizer.normalizeUsername(usernameOrPhone)
        val normalizedPhone = IdentityNormalizer.normalizePhone(usernameOrPhone)

        val entity = (if (normalizedUsername.isNotBlank()) userDao.getUserByUsername(normalizedUsername) else null)
            ?: userDao.getUserByPhone(normalizedPhone)
            ?: return Result.failure(RegistrationException.InvalidCredentialsException())

        if (!IdentityNormalizer.verifyPassword(rawPassword, entity.passwordHash) || entity.passwordHash.isBlank()) {
            return Result.failure(RegistrationException.InvalidCredentialsException())
        }

        val domainStatus = AccountApprovalStatus.valueOf(entity.approvalStatus)
        if (domainStatus != AccountApprovalStatus.APPROVED) {
            return Result.failure(RegistrationException.AccountNotApprovedException(domainStatus))
        }

        val auditLog = AuditLog(
            id = "audit-${UUID.randomUUID()}",
            eventType = "USER_LOGIN_SUCCESS",
            actorId = entity.id,
            actorRole = entity.role,
            entityId = entity.id,
            oldState = null,
            newState = "LOGGED_IN",
            transactionId = null,
            metadataJson = "{\"username\":\"${entity.username}\"}"
        )
        auditLogDao.insertLog(auditLog.toEntity())

        return Result.success(entity.toDomain())
    }

    override suspend fun changePassword(
        userId: String,
        currentRawPassword: String,
        newRawPassword: String
    ): Result<Unit> {
        val entity = userDao.getUserById(userId)
            ?: return Result.failure(IllegalArgumentException("کاربر یافت نشد."))

        if (!IdentityNormalizer.verifyPassword(currentRawPassword, entity.passwordHash) || entity.passwordHash.isBlank()) {
            return Result.failure(RegistrationException.InvalidCredentialsException())
        }
        if (newRawPassword.isBlank() || newRawPassword.length < 6) {
            return Result.failure(RegistrationException.InvalidPasswordException())
        }

        return try {
            val now = System.currentTimeMillis()
            val updated = entity.copy(passwordHash = IdentityNormalizer.hashPassword(newRawPassword), updatedAt = now)

            val auditLog = AuditLog(
                id = "audit-${UUID.randomUUID()}",
                eventType = "USER_PASSWORD_CHANGED",
                actorId = userId,
                actorRole = entity.role,
                entityId = userId,
                oldState = null,
                newState = "PASSWORD_CHANGED",
                transactionId = null,
                metadataJson = "{\"userId\":\"$userId\"}"
            )

            val syncItem = SyncQueueItem(
                id = "sync-${UUID.randomUUID()}",
                entityType = "USER",
                entityId = userId,
                action = "UPDATE"
            )

            database.withTransaction {
                userDao.updateUser(updated)
                auditLogDao.insertLog(auditLog.toEntity())
                syncQueueDao.insertSyncItem(syncItem.toEntity())
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteUser(userId: String, actorId: String): Result<Unit> {
        // SECURITY: re-verify the actor's real database role here, independent of the UI/
        // ViewModel layer, so this method can never delete a user when called by an
        // unauthenticated or non-admin actor.
        val actor = userDao.getUserById(actorId)
        if (actor == null || !isAdminRole(actor.role)) {
            return Result.failure(RegistrationException.UnauthorizedAdminCreationException())
        }

        return try {
            val target = userDao.getUserById(userId)
                ?: return Result.failure(IllegalArgumentException("کاربر یافت نشد."))
            if (target.role == "SYSTEM_ADMIN") {
                return Result.failure(IllegalStateException("حساب مدیر ارشد سیستم قابل حذف نیست."))
            }

            val auditLog = AuditLog(
                id = "audit-${UUID.randomUUID()}",
                eventType = "USER_DELETED",
                actorId = actorId,
                actorRole = actor.role,
                entityId = userId,
                oldState = target.role,
                newState = "DELETED",
                transactionId = null,
                metadataJson = "{\"deletedUsername\":\"${target.username}\"}"
            )
            val syncItem = SyncQueueItem(
                id = "sync-${UUID.randomUUID()}",
                entityType = "USER",
                entityId = userId,
                action = "DELETE"
            )

            database.withTransaction {
                userDao.deleteUserById(userId)
                auditLogDao.insertLog(auditLog.toEntity())
                syncQueueDao.insertSyncItem(syncItem.toEntity())
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun handleConstraintViolation(e: Throwable, phone: String, nationalId: String, username: String = ""): Result<User> {
        val msg = e.message.orEmpty().lowercase()
        return when {
            msg.contains("username") || msg.contains("users.username") -> {
                Result.failure(RegistrationException.DuplicateUsernameException(username))
            }
            msg.contains("phone") || msg.contains("users.phone") -> {
                Result.failure(RegistrationException.DuplicatePhoneException(phone))
            }
            msg.contains("nationalid") || msg.contains("users.nationalid") -> {
                Result.failure(RegistrationException.DuplicateNationalIdException(nationalId))
            }
            else -> {
                Result.failure(RegistrationException.DuplicateBothException())
            }
        }
    }

    override suspend fun updateApprovalStatus(
        userId: String,
        newStatus: AccountApprovalStatus,
        actorId: String
    ): Result<User> {
        // SECURITY: this is a security-sensitive admin operation. The actor's real database
        // role is re-verified HERE, independent of any UI-layer check, so this method can never
        // be used to approve/reject a user by an unauthenticated or non-admin caller — even if
        // called directly, bypassing the ViewModel guard.
        val actor = userDao.getUserById(actorId)
        if (actor == null || !isAdminRole(actor.role)) {
            return Result.failure(RegistrationException.UnauthorizedAdminCreationException())
        }

        return try {
            val existingEntity = userDao.getUserById(userId)
                ?: return Result.failure(IllegalArgumentException("کاربر با شناسه $userId یافت نشد."))

            val currentDomain = existingEntity.toDomain()
            val updatedDomain = currentDomain.copy(
                approvalStatus = newStatus,
                updatedAt = System.currentTimeMillis()
            )

            val auditLog = AuditLog(
                id = "audit-${UUID.randomUUID()}",
                eventType = "USER_APPROVAL_STATUS_CHANGED",
                actorId = actorId,
                actorRole = actor.role,
                entityId = userId,
                oldState = currentDomain.approvalStatus.name,
                newState = newStatus.name,
                transactionId = null,
                metadataJson = "{\"previous\":\"${currentDomain.approvalStatus.name}\",\"current\":\"${newStatus.name}\"}"
            )

            val syncItem = SyncQueueItem(
                id = "sync-${UUID.randomUUID()}",
                entityType = "USER",
                entityId = userId,
                action = "UPDATE"
            )

            database.withTransaction {
                userDao.updateUser(updatedDomain.toEntity())
                auditLogDao.insertLog(auditLog.toEntity())
                syncQueueDao.insertSyncItem(syncItem.toEntity())
            }

            Result.success(updatedDomain)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Mobile Number Change Workflow ---

    override fun getMobileChangeRequests(): Flow<List<MobileChangeRequest>> {
        return mobileChangeRequestDao.getAllRequests().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun requestMobileChange(userId: String, requestedPhone: String): Result<MobileChangeRequest> {
        val normalizedRequestedPhone = IdentityNormalizer.normalizePhone(requestedPhone)
        
        if (!IdentityNormalizer.isValidIranianMobile(normalizedRequestedPhone)) {
            return Result.failure(RegistrationException.InvalidPhoneException(requestedPhone))
        }

        val userEntity = userDao.getUserById(userId)
            ?: return Result.failure(IllegalArgumentException("کاربر یافت نشد."))

        if (userEntity.phone == normalizedRequestedPhone) {
            return Result.failure(IllegalArgumentException("شماره جدید با شماره فعلی شما یکسان است."))
        }

        val phoneOwner = userDao.getUserByPhone(normalizedRequestedPhone)
        if (phoneOwner != null) {
            return Result.failure(RegistrationException.DuplicatePhoneException(normalizedRequestedPhone))
        }

        val existingPending = mobileChangeRequestDao.getPendingRequestByUserId(userId)
        if (existingPending != null) {
            return Result.failure(IllegalStateException("شما در حال حاضر یک درخواست تغییر شماره تلفن همراه در حال بررسی دارید."))
        }

        val pendingForPhone = mobileChangeRequestDao.getPendingRequestByPhone(normalizedRequestedPhone)
        if (pendingForPhone != null) {
            return Result.failure(RegistrationException.DuplicatePhoneException(normalizedRequestedPhone))
        }

        val changeRequest = MobileChangeRequest(
            id = "mcr-${UUID.randomUUID()}",
            userId = userEntity.id,
            userFullName = userEntity.fullName,
            currentPhone = userEntity.phone,
            requestedPhone = normalizedRequestedPhone,
            nationalId = userEntity.nationalId,
            status = MobileChangeStatus.PENDING_ADMIN_APPROVAL,
            requestedAt = System.currentTimeMillis()
        )

        return try {
            mobileChangeRequestDao.insertRequest(changeRequest.toEntity())
            Result.success(changeRequest)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun approveMobileChange(
        requestId: String,
        adminId: String,
        adminRole: String
    ): Result<User> {
        // SECURITY: independently re-verify the actor is a real admin in the database — the
        // adminId/adminRole parameters must never be trusted as-is from the caller.
        val actor = userDao.getUserById(adminId)
        if (actor == null || !isAdminRole(actor.role)) {
            return Result.failure(RegistrationException.UnauthorizedAdminCreationException())
        }
        return try {
            database.withTransaction {
                val requestEntity = mobileChangeRequestDao.getRequestById(requestId)
                    ?: throw IllegalArgumentException("درخواست تغییر شماره یافت نشد.")

                if (requestEntity.status != MobileChangeStatus.PENDING_ADMIN_APPROVAL.name) {
                    throw IllegalStateException("این درخواست قبلاً تعیین تکلیف شده است.")
                }

                val userEntity = userDao.getUserById(requestEntity.userId)
                    ?: throw IllegalArgumentException("کاربر مربوط به این درخواست یافت نشد.")

                // Verify phone uniqueness at approval moment
                val existingOwner = userDao.getUserByPhone(requestEntity.requestedPhone)
                if (existingOwner != null && existingOwner.id != userEntity.id) {
                    throw RegistrationException.DuplicatePhoneException(requestEntity.requestedPhone)
                }

                val oldPhone = userEntity.phone
                val newPhone = requestEntity.requestedPhone
                val now = System.currentTimeMillis()

                // Update User phone (National ID is immutable and unchanged)
                userDao.updatePhone(userEntity.id, newPhone, now)

                // Update Request status to APPROVED
                mobileChangeRequestDao.updateRequest(
                    requestEntity.copy(
                        status = MobileChangeStatus.APPROVED.name,
                        reviewedAt = now,
                        reviewedBy = adminId
                    )
                )

                // Authoritative Audit Log
                val auditLog = AuditLog(
                    id = "audit-${UUID.randomUUID()}",
                    eventType = "USER_MOBILE_CHANGED",
                    actorId = adminId,
                    actorRole = adminRole,
                    entityId = userEntity.id,
                    oldState = oldPhone,
                    newState = newPhone,
                    transactionId = requestId,
                    timestamp = now,
                    metadataJson = "{\"action\":\"MOBILE_NUMBER_CHANGE\",\"userId\":\"${userEntity.id}\",\"nationalId\":\"${userEntity.nationalId}\",\"oldPhone\":\"$oldPhone\",\"newPhone\":\"$newPhone\"}"
                )
                auditLogDao.insertLog(auditLog.toEntity())

                // Sync Queue Item
                val syncItem = SyncQueueItem(
                    id = "sync-${UUID.randomUUID()}",
                    entityType = "USER",
                    entityId = userEntity.id,
                    action = "UPDATE"
                )
                syncQueueDao.insertSyncItem(syncItem.toEntity())

                val updatedEntity = userDao.getUserById(userEntity.id)
                    ?: throw IllegalStateException("کاربر پس از به‌روزرسانی یافت نشد.")
                val updatedUser = updatedEntity.toDomain()
                Result.success(updatedUser)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rejectMobileChange(
        requestId: String,
        adminId: String,
        adminRole: String,
        reason: String
    ): Result<Unit> {
        // SECURITY: independently re-verify the actor is a real admin in the database.
        val actor = userDao.getUserById(adminId)
        if (actor == null || !isAdminRole(actor.role)) {
            return Result.failure(RegistrationException.UnauthorizedAdminCreationException())
        }
        return try {
            database.withTransaction {
                val requestEntity = mobileChangeRequestDao.getRequestById(requestId)
                    ?: throw IllegalArgumentException("درخواست تغییر شماره یافت نشد.")

                if (requestEntity.status != MobileChangeStatus.PENDING_ADMIN_APPROVAL.name) {
                    throw IllegalStateException("این درخواست قبلاً تعیین تکلیف شده است.")
                }

                val now = System.currentTimeMillis()

                mobileChangeRequestDao.updateRequest(
                    requestEntity.copy(
                        status = MobileChangeStatus.REJECTED.name,
                        reviewedAt = now,
                        reviewedBy = adminId,
                        reviewNotes = reason
                    )
                )

                val auditLog = AuditLog(
                    id = "audit-${UUID.randomUUID()}",
                    eventType = "USER_MOBILE_CHANGE_REJECTED",
                    actorId = adminId,
                    actorRole = adminRole,
                    entityId = requestEntity.userId,
                    oldState = requestEntity.currentPhone,
                    newState = requestEntity.currentPhone,
                    transactionId = requestId,
                    timestamp = now,
                    metadataJson = "{\"action\":\"MOBILE_NUMBER_CHANGE_REJECTED\",\"userId\":\"${requestEntity.userId}\",\"rejectedPhone\":\"${requestEntity.requestedPhone}\",\"reason\":\"$reason\"}"
                )
                auditLogDao.insertLog(auditLog.toEntity())

                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun seedUsersIfEmpty(seedUsers: List<User>) {
        val existing = userDao.getAllUsersDirect()
        if (existing.isEmpty() && seedUsers.isNotEmpty()) {
            userDao.insertUsers(seedUsers.map { it.toEntity() })
        }
    }

    override suspend fun syncAndMergeUsers(remoteUsers: List<User>): Result<Int> {
        return try {
            var mergedCount = 0
            database.withTransaction {
                val existingUsers = userDao.getAllUsersDirect()
                val existingById = existingUsers.associateBy { it.id }
                val existingByPhone = existingUsers.associateBy { it.phone }
                val existingByUsername = existingUsers.filter { it.username.isNotBlank() }.associateBy { it.username }
                val existingByNationalId = existingUsers.filter { it.nationalId.isNotBlank() }.associateBy { it.nationalId }

                for (remote in remoteUsers) {
                    val current = existingById[remote.id]
                    // Preserve SYSTEM_ADMIN immutability
                    if (current != null && (current.role == "SYSTEM_ADMIN" || current.username.equals("reza", ignoreCase = true))) {
                        continue
                    }

                    val match = current
                        ?: existingByPhone[remote.phone]
                        ?: (if (remote.username.isNotBlank()) existingByUsername[remote.username] else null)
                        ?: (if (remote.nationalId.isNotBlank()) existingByNationalId[remote.nationalId] else null)

                    if (match == null) {
                        // Brand new user from Device A: insert into local Room DB
                        userDao.insertUser(remote.toEntity())
                        mergedCount++
                    } else {
                        // Existing user: update if remote has equal or newer timestamp and not system admin
                        if (match.role != "SYSTEM_ADMIN" && !match.username.equals("reza", ignoreCase = true) && remote.updatedAt >= match.updatedAt) {
                            userDao.updateUser(remote.toEntity().copy(id = match.id))
                            mergedCount++
                        }
                    }
                }
            }
            Result.success(mergedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun seedSystemAdminIfMissing(
        username: String,
        rawPassword: String,
        fullName: String,
        phone: String
    ) {
        val normalizedUsername = IdentityNormalizer.normalizeUsername(username)
        val existingByUsername = userDao.getUserByUsername(normalizedUsername)
        if (existingByUsername != null) return

        val alreadyHasSystemAdmin = userDao.getAllUsersDirect().any { it.role == "SYSTEM_ADMIN" }
        if (alreadyHasSystemAdmin) return

        val systemAdmin = User(
            id = "user-${UUID.randomUUID()}",
            username = normalizedUsername,
            passwordHash = IdentityNormalizer.hashPassword(rawPassword),
            fullName = fullName,
            phone = IdentityNormalizer.normalizePhone(phone),
            nationalId = "",
            role = "SYSTEM_ADMIN",
            approvalStatus = AccountApprovalStatus.APPROVED
        )

        try {
            database.withTransaction {
                userDao.insertUser(systemAdmin.toEntity())
                auditLogDao.insertLog(
                    AuditLog(
                        id = "audit-${UUID.randomUUID()}",
                        eventType = "SYSTEM_ADMIN_SEEDED",
                        actorId = "system",
                        actorRole = "SYSTEM_ADMIN",
                        entityId = systemAdmin.id,
                        oldState = null,
                        newState = "APPROVED",
                        transactionId = null,
                        metadataJson = "{\"username\":\"${systemAdmin.username}\"}"
                    ).toEntity()
                )
                syncQueueDao.insertSyncItem(
                    SyncQueueItem(
                        id = "sync-${UUID.randomUUID()}",
                        entityType = "USER",
                        entityId = systemAdmin.id,
                        action = "CREATE"
                    ).toEntity()
                )
            }
        } catch (e: Exception) {
            // Constraint race (e.g. concurrent first-run on two devices syncing quickly): ignore,
            // an admin already exists.
        }
    }
}
