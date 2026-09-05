package com.example.domain

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.database.PudoDatabase
import com.example.data.local.entities.UserEntity
import com.example.data.local.mappers.toEntity
import com.example.data.model.AccountApprovalStatus
import com.example.data.model.MobileChangeStatus
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RegistrationIntegrityAndUiHardeningTest {

    private lateinit var database: PudoDatabase
    private lateinit var authRepository: AuthRepository

    // Pre-calculated valid Iranian National IDs for testing:
    // "0012345679" -> sum = 112, 112%11=2, 11-2=9 -> Valid
    // "0451234561" -> sum = 153, 153%11=10, 11-10=1 -> Valid
    // "0080000002" -> sum = 64, 64%11=9, 11-9=2 -> Valid
    // "0060000007" -> sum = 48, 48%11=4, 11-4=7 -> Valid
    // "0070000001" -> sum = 56, 56%11=1, r=1 -> Valid
    // "0050000004" -> sum = 40, 40%11=7, 11-7=4 -> Valid
    // "0040000001" -> sum = 32, 32%11=10, 11-10=1 -> Valid
    // "0030000009" -> sum = 24, 24%11=2, 11-2=9 -> Valid
    // "0020000006" -> sum = 16, 16%11=5, 11-5=6 -> Valid
    // "0090000005" -> sum = 72, 72%11=6, 11-6=5 -> Valid

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

    /**
     * Inserts a real ADMIN-role user directly and returns its id, for tests that need a
     * legitimate actor id to pass repository-layer admin-authorization checks (added when
     * approveMobileChange/rejectMobileChange/updateApprovalStatus/deleteUser were hardened to
     * independently re-verify the actor's real database role).
     */
    private suspend fun seedAdminActor(id: String = "admin-actor"): String {
        database.userDao().insertUser(
            User(
                id = id,
                username = "adminactor_$id",
                fullName = "Admin Actor",
                phone = "0912${(1000000..9999999).random()}",
                role = "ADMIN",
                approvalStatus = AccountApprovalStatus.APPROVED
            ).toEntity()
        )
        return id
    }

    // 1. Phone normalization handles prefixes (+98, 0098, 98), Persian numerals, dashes, and spaces
    @Test
    fun testPhoneNormalizationVariants() {
        assertEquals("09121234567", IdentityNormalizer.normalizeIranianPhone("09121234567"))
        assertEquals("09121234567", IdentityNormalizer.normalizeIranianPhone("+989121234567"))
        assertEquals("09121234567", IdentityNormalizer.normalizeIranianPhone("00989121234567"))
        assertEquals("09121234567", IdentityNormalizer.normalizeIranianPhone("989121234567"))
        assertEquals("09121234567", IdentityNormalizer.normalizeIranianPhone("۰۹۱۲۱۲۳۴۵۶۷"))
        assertEquals("09121234567", IdentityNormalizer.normalizeIranianPhone("0912-123-4567"))
        assertEquals("09121234567", IdentityNormalizer.normalizeIranianPhone("  0912 123 4567  "))
    }

    // 2. Phone validation correctly rejects invalid numbers
    @Test
    fun testPhoneValidation() {
        assertTrue(IdentityNormalizer.isValidIranianPhone("09121234567"))
        assertTrue(IdentityNormalizer.isValidIranianPhone("+989351234567"))
        assertFalse(IdentityNormalizer.isValidIranianPhone("08121234567")) // Invalid prefix
        assertFalse(IdentityNormalizer.isValidIranianPhone("091212345"))   // Too short
        assertFalse(IdentityNormalizer.isValidIranianPhone("0912123456789")) // Too long
        assertFalse(IdentityNormalizer.isValidIranianPhone(""))
    }

    // 3. National ID normalization handles Persian numerals, spaces, and dashes
    @Test
    fun testNationalIdNormalization() {
        assertEquals("0012345679", IdentityNormalizer.normalizeNationalId("0012345679"))
        assertEquals("0012345679", IdentityNormalizer.normalizeNationalId("۰۰۱۲۳۴۵۶۷۹"))
        assertEquals("0012345679", IdentityNormalizer.normalizeNationalId("001-234-5679"))
        assertEquals("0012345679", IdentityNormalizer.normalizeNationalId(" 001 2345679 "))
    }

    // 4. National ID validation correctly checks format, checksum, and length
    @Test
    fun testNationalIdValidation() {
        assertTrue(IdentityNormalizer.isValidNationalId("0012345679"))
        assertTrue(IdentityNormalizer.isValidNationalId("0451234561"))
        assertTrue(IdentityNormalizer.isValidNationalId("0080000002"))
        assertFalse(IdentityNormalizer.isValidNationalId("0012345678")) // Invalid checksum
        assertFalse(IdentityNormalizer.isValidNationalId("1111111111")) // Repeated digits
        assertFalse(IdentityNormalizer.isValidNationalId("1234567"))    // 7 digits (too short)
        assertFalse(IdentityNormalizer.isValidNationalId("123456788"))  // 9 digits with invalid checksum
        assertFalse(IdentityNormalizer.isValidNationalId("12345678901")) // 11 digits (too long)
        assertFalse(IdentityNormalizer.isValidNationalId("abcdefghij"))  // Non-digits
        assertFalse(IdentityNormalizer.isValidNationalId(""))
    }

    // 5. Unique User Registration succeeds and stores normalized identity values
    @Test
    fun testRegistrationSuccess() = runTest {
        val user = User(
            id = "user-test-1",
            fullName = "رضا کاظمی",
            phone = "۰۹۱۲۳۴۵۶۷۸۹", // Persian numerals
            nationalId = "۰۰۱۲۳۴۵۶۷۹", // Persian numerals
            email = "reza@example.com",
            address = "تهران، میدان آزادی",
            role = "CUSTOMER",
            approvalStatus = AccountApprovalStatus.PENDING
        )

        val result = authRepository.registerUser(user)
        assertTrue(result.isSuccess)

        val retrieved = authRepository.getUserById("user-test-1")
        assertNotNull(retrieved)
        assertEquals("09123456789", retrieved?.phone) // Normalized English numerals
        assertEquals("0012345679", retrieved?.nationalId) // Normalized
        assertEquals("reza@example.com", retrieved?.email)
    }

    // 6. Registration with duplicate phone throws DuplicatePhoneException
    @Test
    fun testDuplicatePhoneRegistrationRejected() = runTest {
        val user1 = User(
            id = "user-u1",
            fullName = "کاربر اول",
            phone = "09121112233",
            nationalId = "0080000002",
            role = "CUSTOMER"
        )
        val res1 = authRepository.registerUser(user1)
        assertTrue(res1.isSuccess)

        // Attempt second registration with same phone (in different format)
        val user2 = User(
            id = "user-u2",
            fullName = "کاربر دوم",
            phone = "+989121112233",
            nationalId = "0060000007",
            role = "CUSTOMER"
        )
        val res2 = authRepository.registerUser(user2)
        assertTrue(res2.isFailure)
        val exception = res2.exceptionOrNull()
        assertTrue(exception is RegistrationException.DuplicatePhoneException)
        assertEquals("09121112233", (exception as RegistrationException.DuplicatePhoneException).phone)
    }

    // 7. Registration with duplicate nationalId throws DuplicateNationalIdException
    @Test
    fun testDuplicateNationalIdRegistrationRejected() = runTest {
        val user1 = User(
            id = "user-nid1",
            fullName = "کاربر اول",
            phone = "09121110001",
            nationalId = "0070000001",
            role = "CUSTOMER"
        )
        val res1 = authRepository.registerUser(user1)
        assertTrue(res1.isSuccess)

        // Attempt second registration with same national ID (in Persian format)
        val user2 = User(
            id = "user-nid2",
            fullName = "کاربر دوم",
            phone = "09121110002",
            nationalId = "۰۰۷۰۰۰۰۰۰۱",
            role = "CUSTOMER"
        )
        val res2 = authRepository.registerUser(user2)
        assertTrue(res2.isFailure)
        val exception = res2.exceptionOrNull()
        assertTrue(exception is RegistrationException.DuplicateNationalIdException)
        assertEquals("0070000001", (exception as RegistrationException.DuplicateNationalIdException).nationalId)
    }

    // 8. Database level uniqueness constraint enforces duplicate abort
    @Test
    fun testDatabaseUniqueConstraintEnforced() = runTest {
        val entity1 = UserEntity(
            id = "db-u1",
            fullName = "تست دیتابیس ۱",
            phone = "09129998877",
            nationalId = "0050000004",
            email = "db1@test.com",
            address = "تهران",
            role = "CUSTOMER",
            postalCode = "",
            storeName = "",
            guildType = "",
            bankCardNumber = "",
            approvalStatus = AccountApprovalStatus.APPROVED.name,
            createdAt = System.currentTimeMillis()
        )
        database.userDao().insertUser(entity1)

        // Direct entity insertion duplicate should conflict
        val entity2 = entity1.copy(id = "db-u2", email = "db2@test.com")
        try {
            database.userDao().insertUser(entity2)
            fail("Expected SQLiteConstraintException due to duplicate phone / nationalId UNIQUE index")
        } catch (e: Exception) {
            // Expected database constraint abort
            assertTrue(e.message?.contains("UNIQUE") == true || e.message?.contains("constraint") == true || e is android.database.sqlite.SQLiteConstraintException)
        }
    }

    // 9. Requesting mobile change creates PENDING_ADMIN_APPROVAL record
    @Test
    fun testRequestMobileChangeSuccess() = runTest {
        val user = User(
            id = "user-change-1",
            fullName = "سارا احمدی",
            phone = "09125554433",
            nationalId = "0040000001",
            role = "CUSTOMER"
        )
        val regRes = authRepository.registerUser(user)
        assertTrue(regRes.isSuccess)

        val result = authRepository.requestMobileChange("user-change-1", "09127778899")
        assertTrue(result.isSuccess)

        val requests = authRepository.getMobileChangeRequests().first()
        val req = requests.firstOrNull { it.userId == "user-change-1" }
        assertNotNull(req)
        assertEquals("09125554433", req?.currentPhone)
        assertEquals("09127778899", req?.requestedPhone)
        assertEquals("0040000001", req?.nationalId)
        assertEquals(MobileChangeStatus.PENDING_ADMIN_APPROVAL, req?.status)
    }

    // 10. Mobile change request with duplicate phone used by another user is rejected
    @Test
    fun testRequestMobileChangeWithDuplicatePhoneFails() = runTest {
        val user1 = User(id = "u-active1", fullName = "علی ۱", phone = "09121111111", nationalId = "0030000009")
        val user2 = User(id = "u-active2", fullName = "علی ۲", phone = "09122222222", nationalId = "0020000006")
        assertTrue(authRepository.registerUser(user1).isSuccess)
        assertTrue(authRepository.registerUser(user2).isSuccess)

        // User 2 requests to change phone to user 1's phone
        val result = authRepository.requestMobileChange("u-active2", "09121111111")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RegistrationException.DuplicatePhoneException)
    }

    // 11. Approving mobile change updates phone, creates audit log, and queues sync event
    @Test
    fun testApproveMobileChangeWorkflow() = runTest {
        val user = User(
            id = "u-workflow-1",
            fullName = "حسین رضایی",
            phone = "09123334455",
            nationalId = "0090000005",
            role = "HUB_OPERATOR"
        )
        assertTrue(authRepository.registerUser(user).isSuccess)

        assertTrue(authRepository.requestMobileChange("u-workflow-1", "09198887766").isSuccess)
        val requests = authRepository.getMobileChangeRequests().first()
        val reqId = requests.first { it.userId == "u-workflow-1" }.id

        // Admin approves
        val adminId = seedAdminActor()
        val approveResult = authRepository.approveMobileChange(reqId, adminId)
        assertTrue(approveResult.isSuccess)

        // Verify User's phone updated in database
        val updatedUser = authRepository.getUserById("u-workflow-1")
        assertEquals("09198887766", updatedUser?.phone)
        // National ID remains strictly preserved
        assertEquals("0090000005", updatedUser?.nationalId)

        // Verify Request status is APPROVED
        val updatedReqs = authRepository.getMobileChangeRequests().first()
        val updatedReq = updatedReqs.first { it.id == reqId }
        assertEquals(MobileChangeStatus.APPROVED, updatedReq.status)

        // Verify Audit Log was recorded
        val auditLogs = database.auditLogDao().getAllLogs().first()
        assertTrue(auditLogs.any { it.entityId == "u-workflow-1" && it.eventType == "USER_MOBILE_CHANGED" })

        // Verify Sync Queue was updated
        val syncItems = database.syncQueueDao().getPendingSyncItemsDirect()
        assertTrue(syncItems.any { it.entityId == "u-workflow-1" && it.entityType == "USER" })
    }

    // 12. Rejecting mobile change preserves original phone and marks status REJECTED
    @Test
    fun testRejectMobileChangeWorkflow() = runTest {
        val user = User(
            id = "u-reject-1",
            fullName = "مهدی ناصری",
            phone = "09124445566",
            nationalId = "0451234561"
        )
        assertTrue(authRepository.registerUser(user).isSuccess)
        assertTrue(authRepository.requestMobileChange("u-reject-1", "09351112233").isSuccess)

        val requests = authRepository.getMobileChangeRequests().first()
        val reqId = requests.first { it.userId == "u-reject-1" }.id

        val adminId = seedAdminActor()
        val rejectResult = authRepository.rejectMobileChange(reqId, adminId, "ADMIN", "عدم تطابق با مدارک شناسایی")
        assertTrue(rejectResult.isSuccess)

        // User phone unchanged
        val userAfter = authRepository.getUserById("u-reject-1")
        assertEquals("09124445566", userAfter?.phone)

        // Request status REJECTED with reason
        val updatedReqs = authRepository.getMobileChangeRequests().first()
        val req = updatedReqs.first { it.id == reqId }
        assertEquals(MobileChangeStatus.REJECTED, req.status)
        assertEquals("عدم تطابق با مدارک شناسایی", req.reviewNotes)
    }

    // 13. National ID is strictly immutable during profile operations
    @Test
    fun testNationalIdImmutability() = runTest {
        val user = User(
            id = "u-immutable-1",
            fullName = "پروانه علوی",
            phone = "09126667788",
            nationalId = "0080000002",
            role = "CUSTOMER"
        )
        assertTrue(authRepository.registerUser(user).isSuccess)

        val initialUser = authRepository.getUserById("u-immutable-1")
        assertNotNull(initialUser)
        assertEquals("0080000002", initialUser?.nationalId)

        // Changing phone through workflow does not touch nationalId
        assertTrue(authRepository.requestMobileChange("u-immutable-1", "09120001122").isSuccess)
        val reqId = authRepository.getMobileChangeRequests().first().first { it.userId == "u-immutable-1" }.id
        val adminId3 = seedAdminActor("admin-actor-3")
        assertTrue(authRepository.approveMobileChange(reqId, adminId3).isSuccess)

        val afterChange = authRepository.getUserById("u-immutable-1")
        assertEquals("09120001122", afterChange?.phone)
        assertEquals("0080000002", afterChange?.nationalId) // Strictly immutable
    }

    // 14. Registration validation: missing required nationalId or phone fails gracefully
    @Test
    fun testValidationRejectionOnInvalidInputs() = runTest {
        val invalidPhoneUser = User(
            id = "u-inv-1",
            fullName = "نام نامعتبر",
            phone = "12345", // Invalid
            nationalId = "0012345679"
        )
        val resPhone = authRepository.registerUser(invalidPhoneUser)
        assertTrue(resPhone.isFailure)
        assertTrue(resPhone.exceptionOrNull() is RegistrationException.InvalidPhoneException)

        val invalidNidUser = User(
            id = "u-inv-2",
            fullName = "نام نامعتبر",
            phone = "09121234567",
            nationalId = "123" // Invalid
        )
        val resNid = authRepository.registerUser(invalidNidUser)
        assertTrue(resNid.isFailure)
        assertTrue(resNid.exceptionOrNull() is RegistrationException.InvalidNationalIdException)
    }

    // 15. User lookup by phone and nationalId after normalization
    @Test
    fun testUserLookupByPhoneAndNationalId() = runTest {
        val user = User(
            id = "u-lookup-1",
            fullName = "محمد اکبری",
            phone = "۰۹۱۲۸۸۸۹۹۰۰",
            nationalId = "۰۰۸۰۰۰۰۰۰۲",
            role = "CUSTOMER"
        )
        assertTrue(authRepository.registerUser(user).isSuccess)

        // Lookup by normalized phone
        val userByPhone = authRepository.getUserByPhone("+989128889900")
        assertNotNull(userByPhone)
        assertEquals("u-lookup-1", userByPhone?.id)

        // Lookup by national ID
        val userByNid = authRepository.getUserByNationalId("۰۰۸۰۰۰۰۰۰۲")
        assertNotNull(userByNid)
        assertEquals("u-lookup-1", userByNid?.id)
    }

    // 16. Audit Log traceability for mobile change requests and approvals
    @Test
    fun testAuditLoggingOnMobileChangeApproval() = runTest {
        seedAdminActor("admin-1")
        val user = User(
            id = "u-audit-1",
            fullName = "مریم حسینی",
            phone = "09127776655",
            nationalId = "0070000001"
        )
        assertTrue(authRepository.registerUser(user).isSuccess)
        assertTrue(authRepository.requestMobileChange("u-audit-1", "09129990011").isSuccess)
        val reqId = authRepository.getMobileChangeRequests().first().first { it.userId == "u-audit-1" }.id
        assertTrue(authRepository.approveMobileChange(reqId, "admin-1").isSuccess)

        val auditLogs = database.auditLogDao().getAllLogs().first()
        assertTrue(auditLogs.isNotEmpty())
        assertTrue(auditLogs.any { it.entityId == "u-audit-1" && it.eventType == "USER_MOBILE_CHANGED" })
    }

    // 17. Sync Queue item generation on mobile change approval
    @Test
    fun testSyncQueueGenerationOnMobileChange() = runTest {
        seedAdminActor("admin-1")
        val user = User(
            id = "u-sync-1",
            fullName = "سینا صابری",
            phone = "09126665544",
            nationalId = "0060000007"
        )
        assertTrue(authRepository.registerUser(user).isSuccess)
        assertTrue(authRepository.requestMobileChange("u-sync-1", "09124443322").isSuccess)
        val reqId = authRepository.getMobileChangeRequests().first().first { it.userId == "u-sync-1" }.id
        assertTrue(authRepository.approveMobileChange(reqId, "admin-1").isSuccess)

        val syncItems = database.syncQueueDao().getPendingSyncItemsDirect()
        val userUpdateSyncItem = syncItems.firstOrNull { it.entityId == "u-sync-1" && it.entityType == "USER" && it.action == "UPDATE" }
        assertNotNull(userUpdateSyncItem)
        assertEquals("UPDATE", userUpdateSyncItem?.action)
    }
}
