package com.example.domain

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.database.PudoDatabase
import com.example.data.local.mappers.*
import com.example.data.model.AccountApprovalStatus
import com.example.data.model.RegistrationException
import com.example.data.model.User
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthRepositoryImpl
import com.example.util.IdentityNormalizer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers the single, authoritative Room/AuthRepository authentication & authorization path:
 *   UI -> NdnViewModel -> AuthRepository -> Room -> Authenticated User + Role
 *
 * These tests exercise AuthRepository directly (the same layer NdnViewModel calls into) using
 * an in-memory Room database, matching the existing pattern used across this test suite.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthenticationAndAuthorizationTest {

    private lateinit var database: PudoDatabase
    private lateinit var authRepository: AuthRepository

    // Pre-validated Iranian National IDs (see RegistrationIntegrityAndUiHardeningTest for the
    // checksum derivation of each value).
    private val nid1 = "0012345679"
    private val nid2 = "0451234561"
    private val nid3 = "0080000002"
    private val nid4 = "0060000007"
    private val nid5 = "0070000001"

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = PudoDatabase.createInMemory(context)
        authRepository = AuthRepositoryImpl(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun seedSystemAdmin() {
        authRepository.seedSystemAdminIfMissing(
            username = "Reza",
            rawPassword = "Admin@123",
            fullName = "Reza",
            phone = "09120000000"
        )
    }

    // ---------------------------------------------------------------------
    // 1. Core login: correct / wrong password / unknown username
    // ---------------------------------------------------------------------

    @Test
    fun testCorrectLoginSucceeds() = runTest {
        seedSystemAdmin()
        val result = authRepository.authenticate("Reza", "Admin@123")
        assertTrue(result.isSuccess)
        val user = result.getOrThrow()
        assertEquals("reza", user.username)
        assertEquals("SYSTEM_ADMIN", user.role)
        assertEquals(AccountApprovalStatus.APPROVED, user.approvalStatus)
    }

    @Test
    fun testWrongPasswordFails() = runTest {
        seedSystemAdmin()
        val result = authRepository.authenticate("Reza", "WrongPassword123")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RegistrationException.InvalidCredentialsException)
    }

    @Test
    fun testUnknownUsernameFails() = runTest {
        seedSystemAdmin()
        val result = authRepository.authenticate("no_such_user", "whatever")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RegistrationException.InvalidCredentialsException)
    }

    @Test
    fun testBlankPasswordHashCanNeverAuthenticate() = runTest {
        // Regression test for a real bug found & fixed in this pass: verifyPassword() used to
        // treat a blank stored hash as "accept any password". A user record with no usable
        // password hash must never be able to log in with any input.
        val brokenUser = User(
            id = "user-broken",
            username = "brokenuser",
            passwordHash = "", // no usable password
            fullName = "Broken User",
            phone = "09121110099",
            nationalId = nid5,
            role = "CUSTOMER",
            approvalStatus = AccountApprovalStatus.APPROVED
        )
        database.userDao().insertUser(brokenUser.toEntity())

        val result = authRepository.authenticate("brokenuser", "anything")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RegistrationException.InvalidCredentialsException)
    }

    @Test
    fun testUnapprovedAccountCannotLogin() = runTest {
        val pendingUser = User(
            id = "user-pending",
            username = "pendinguser",
            passwordHash = IdentityNormalizer.hashPassword("SomePass123"),
            fullName = "Pending User",
            phone = "09121110088",
            nationalId = nid4,
            role = "CUSTOMER",
            approvalStatus = AccountApprovalStatus.PENDING
        )
        database.userDao().insertUser(pendingUser.toEntity())

        val result = authRepository.authenticate("pendinguser", "SomePass123")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RegistrationException.AccountNotApprovedException)
    }

    // ---------------------------------------------------------------------
    // 2. Demo/fake authentication is gone
    // ---------------------------------------------------------------------

    @Test
    fun testNoDemoUsersExistBeforeSeeding() = runTest {
        // Fresh database: nothing should be able to log in until the System Admin is seeded,
        // and no demo/test account should ever exist automatically.
        val allUsers = database.userDao().getAllUsersDirect()
        assertTrue(allUsers.isEmpty())
    }

    @Test
    fun testRezaOnlyWorksThroughRealDatabaseAuthentication() = runTest {
        seedSystemAdmin()
        // Correct DB-backed credentials succeed.
        assertTrue(authRepository.authenticate("Reza", "Admin@123").isSuccess)
        // Any other password for the same account must fail — proves it's a real DB check,
        // not a hardcoded bypass keyed off the username alone.
        assertTrue(authRepository.authenticate("Reza", "wrong").isFailure)
        assertTrue(authRepository.authenticate("Reza", "").isFailure)
    }

    // ---------------------------------------------------------------------
    // 3. System Admin seed
    // ---------------------------------------------------------------------

    @Test
    fun testSystemAdminSeedIsIdempotent() = runTest {
        seedSystemAdmin()
        seedSystemAdmin()
        seedSystemAdmin()

        val allUsers = database.userDao().getAllUsersDirect()
        val admins = allUsers.filter { it.role == "SYSTEM_ADMIN" }
        assertEquals(1, admins.size)
        assertEquals("reza", admins.first().username)
    }

    @Test
    fun testSystemAdminSeedDoesNotOverwriteChangedPassword() = runTest {
        seedSystemAdmin()
        val adminId = database.userDao().getUserByUsername("reza")!!.id
        authRepository.changePassword(adminId, "Admin@123", "NewSecurePass1")

        // Re-running seed must NOT reset the password back to "Admin@123".
        seedSystemAdmin()

        assertTrue(authRepository.authenticate("Reza", "Admin@123").isFailure)
        assertTrue(authRepository.authenticate("Reza", "NewSecurePass1").isSuccess)
    }

    // ---------------------------------------------------------------------
    // 4. Admin-only user creation & authorization enforced in the repository layer
    // ---------------------------------------------------------------------

    @Test
    fun testAdminCanCreateUser() = runTest {
        seedSystemAdmin()
        val admin = database.userDao().getUserByUsername("reza")!!.toDomain()

        val newUser = User(
            id = "user-courier-1",
            username = "courier1",
            fullName = "پیک تست",
            phone = "09121234567",
            nationalId = nid1,
            role = "COURIER"
        )
        val result = authRepository.createUserByAdmin(admin.id, newUser, "CourierPass1")
        assertTrue(result.isSuccess)
        val created = result.getOrThrow()
        assertEquals(AccountApprovalStatus.APPROVED, created.approvalStatus)

        // The created account must be able to log in with the exact password the admin set.
        assertTrue(authRepository.authenticate("courier1", "CourierPass1").isSuccess)
    }

    @Test
    fun testNonAdminCannotCreateUsers() = runTest {
        // A regular, approved CUSTOMER account tries to act as the "creator".
        val customer = User(
            id = "user-customer-1",
            username = "customer1",
            passwordHash = IdentityNormalizer.hashPassword("CustPass1"),
            fullName = "مشتری تست",
            phone = "09121230000",
            nationalId = nid2,
            role = "CUSTOMER",
            approvalStatus = AccountApprovalStatus.APPROVED
        )
        database.userDao().insertUser(customer.toEntity())

        val newUser = User(
            id = "user-courier-2",
            username = "courier2",
            fullName = "پیک تست دو",
            phone = "09121234568",
            nationalId = nid3,
            role = "COURIER"
        )
        val result = authRepository.createUserByAdmin(customer.id, newUser, "CourierPass1")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RegistrationException.UnauthorizedAdminCreationException)

        // And the account must never have been created.
        assertNull(authRepository.getUserByUsername("courier2"))
    }

    @Test
    fun testUnknownCreatorCannotCreateUsers() = runTest {
        val newUser = User(
            id = "user-courier-3",
            username = "courier3",
            fullName = "پیک تست سه",
            phone = "09121234569",
            nationalId = nid5,
            role = "COURIER"
        )
        val result = authRepository.createUserByAdmin("no-such-admin-id", newUser, "CourierPass1")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RegistrationException.UnauthorizedAdminCreationException)
    }

    // ---------------------------------------------------------------------
    // 5. Uniqueness constraints for admin-created users
    // ---------------------------------------------------------------------

    @Test
    fun testAdminCreateUserRejectsDuplicateUsername() = runTest {
        seedSystemAdmin()
        val admin = database.userDao().getUserByUsername("reza")!!

        val first = User(
            id = "user-dupuser-1", username = "dupuser", fullName = "کاربر یک",
            phone = "09122220001", nationalId = nid1, role = "CUSTOMER"
        )
        assertTrue(authRepository.createUserByAdmin(admin.id, first, "Password1").isSuccess)

        val second = User(
            id = "user-dupuser-2", username = "dupuser", fullName = "کاربر دو",
            phone = "09122220002", nationalId = nid2, role = "CUSTOMER"
        )
        val result = authRepository.createUserByAdmin(admin.id, second, "Password1")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RegistrationException.DuplicateUsernameException)
    }

    @Test
    fun testAdminCreateUserRejectsDuplicatePhone() = runTest {
        seedSystemAdmin()
        val admin = database.userDao().getUserByUsername("reza")!!

        val first = User(
            id = "user-dupphone-1", username = "dupphoneuser1", fullName = "کاربر یک",
            phone = "09122220003", nationalId = nid3, role = "CUSTOMER"
        )
        assertTrue(authRepository.createUserByAdmin(admin.id, first, "Password1").isSuccess)

        val second = User(
            id = "user-dupphone-2", username = "dupphoneuser2", fullName = "کاربر دو",
            phone = "09122220003", nationalId = nid4, role = "CUSTOMER"
        )
        val result = authRepository.createUserByAdmin(admin.id, second, "Password1")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RegistrationException.DuplicatePhoneException)
    }

    @Test
    fun testAdminCreateUserRejectsDuplicateNationalId() = runTest {
        seedSystemAdmin()
        val admin = database.userDao().getUserByUsername("reza")!!

        val first = User(
            id = "user-dupnid-1", username = "dupnidone", fullName = "کاربر یک",
            phone = "09122220005", nationalId = nid5, role = "CUSTOMER"
        )
        assertTrue(authRepository.createUserByAdmin(admin.id, first, "Password1").isSuccess)

        val second = User(
            id = "user-dupnid-2", username = "dupnidtwo", fullName = "کاربر دو",
            phone = "09122220006", nationalId = nid5, role = "CUSTOMER"
        )
        val result = authRepository.createUserByAdmin(admin.id, second, "Password1")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RegistrationException.DuplicateNationalIdException)
    }

    // ---------------------------------------------------------------------
    // 6. Change password
    // ---------------------------------------------------------------------

    @Test
    fun testChangePasswordSucceedsAndPersists() = runTest {
        seedSystemAdmin()
        val adminId = database.userDao().getUserByUsername("reza")!!.id

        val result = authRepository.changePassword(adminId, "Admin@123", "BrandNewPass9")
        assertTrue(result.isSuccess)

        assertTrue(authRepository.authenticate("Reza", "BrandNewPass9").isSuccess)
    }

    @Test
    fun testOldPasswordRejectedAfterChange() = runTest {
        seedSystemAdmin()
        val adminId = database.userDao().getUserByUsername("reza")!!.id

        authRepository.changePassword(adminId, "Admin@123", "BrandNewPass9")

        val result = authRepository.authenticate("Reza", "Admin@123")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RegistrationException.InvalidCredentialsException)
    }

    @Test
    fun testChangePasswordRejectsWrongCurrentPassword() = runTest {
        seedSystemAdmin()
        val adminId = database.userDao().getUserByUsername("reza")!!.id

        val result = authRepository.changePassword(adminId, "NotTheRealPassword", "AnotherPass1")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RegistrationException.InvalidCredentialsException)

        // Original password must still work — the change must not have partially applied.
        assertTrue(authRepository.authenticate("Reza", "Admin@123").isSuccess)
    }

    @Test
    fun testChangePasswordRejectsShortNewPassword() = runTest {
        seedSystemAdmin()
        val adminId = database.userDao().getUserByUsername("reza")!!.id

        val result = authRepository.changePassword(adminId, "Admin@123", "abc")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RegistrationException.InvalidPasswordException)
    }

    // ---------------------------------------------------------------------
    // 7. Audit log + Sync queue generated for auth/user-management mutations
    // ---------------------------------------------------------------------

    @Test
    fun testAuditLogGeneratedOnLoginAndPasswordChange() = runTest {
        seedSystemAdmin()
        val adminId = database.userDao().getUserByUsername("reza")!!.id

        authRepository.authenticate("Reza", "Admin@123")
        val loginLogs = database.auditLogDao().getLogsByActorId(adminId).first()
        assertTrue(loginLogs.any { it.eventType == "USER_LOGIN_SUCCESS" })

        authRepository.changePassword(adminId, "Admin@123", "AuditedPass1")
        val afterChangeLogs = database.auditLogDao().getLogsByActorId(adminId).first()
        assertTrue(afterChangeLogs.any { it.eventType == "USER_PASSWORD_CHANGED" })
    }

    @Test
    fun testSyncQueueGeneratedOnPasswordChangeAndUserCreation() = runTest {
        seedSystemAdmin()
        val admin = database.userDao().getUserByUsername("reza")!!

        authRepository.changePassword(admin.id, "Admin@123", "SyncedPass1")
        val newUser = User(
            id = "user-sync-1", username = "syncuser", fullName = "کاربر سینک",
            phone = "09122220009", nationalId = nid1, role = "CUSTOMER"
        )
        authRepository.createUserByAdmin(admin.id, newUser, "Password1")

        val allSyncItems = database.syncQueueDao().getAllSyncItems().first()
        assertTrue(allSyncItems.any { it.entityId == admin.id && it.action == "UPDATE" })
        assertTrue(allSyncItems.any { it.entityId == "user-sync-1" && it.action == "CREATE" })
    }

    @Test
    fun testAuditLogGeneratedOnSystemAdminSeed() = runTest {
        seedSystemAdmin()
        val admin = database.userDao().getUserByUsername("reza")!!
        val logs = database.auditLogDao().getLogsByEntityId(admin.id).first()
        assertTrue(logs.any { it.eventType == "SYSTEM_ADMIN_SEEDED" })
    }

    // ---------------------------------------------------------------------
    // 8. Delete user
    // ---------------------------------------------------------------------

    @Test
    fun testAdminCanDeleteRegularUser() = runTest {
        seedSystemAdmin()
        val admin = database.userDao().getUserByUsername("reza")!!
        val newUser = User(
            id = "user-todelete", username = "todeleteuser", fullName = "کاربر حذفی",
            phone = "09122220010", nationalId = nid2, role = "CUSTOMER"
        )
        authRepository.createUserByAdmin(admin.id, newUser, "Password1")

        val result = authRepository.deleteUser("user-todelete", admin.id)
        assertTrue(result.isSuccess)
        assertNull(authRepository.getUserById("user-todelete"))
    }

    @Test
    fun testSystemAdminAccountCannotBeDeleted() = runTest {
        seedSystemAdmin()
        val admin = database.userDao().getUserByUsername("reza")!!

        val result = authRepository.deleteUser(admin.id, admin.id)
        assertTrue(result.isFailure)
        assertNotNull(authRepository.getUserById(admin.id))
    }
}
