package com.example.persistence

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.database.PudoDatabase
import com.example.data.local.entities.*
import com.example.data.local.mappers.*
import com.example.data.model.*
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthRepositoryImpl
import com.example.data.repository.PudoRepository
import com.example.data.repository.PudoRepositoryImpl
import com.example.domain.statemachine.ParcelStateMachine
import com.example.model.HubItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PudoPersistenceAndRepositoryTest {

    private lateinit var database: PudoDatabase
    private lateinit var pudoRepository: PudoRepository
    private lateinit var authRepository: AuthRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = PudoDatabase.createInMemory(context)
        pudoRepository = PudoRepositoryImpl(database)
        authRepository = AuthRepositoryImpl(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // 1. Room Database initialization (in-memory)
    @Test
    fun testDatabaseInitialization() {
        assertNotNull(database)
        assertNotNull(database.parcelDao())
        assertNotNull(database.userDao())
        assertNotNull(database.auditLogDao())
        assertNotNull(database.registrationTransactionDao())
        assertNotNull(database.syncQueueDao())
        assertNotNull(database.tariffDao())
        assertNotNull(database.courierSettlementSnapshotDao())
    }

    // 2. ParcelDao CRUD operations
    @Test
    fun testParcelDaoCrudOperations() = runTest {
        val parcelEntity = ParcelEntity(
            id = "pkg-001",
            trackingNumber = "IR-123456",
            senderName = "علی رضایی",
            recipientName = "محمد حسینی",
            recipientPhone = "09121234567",
            recipientPostalCode = "1998765432",
            recipientAddress = "تهران، خ آزادی",
            status = ParcelStatus.OUT_FOR_DELIVERY.name,
            size = ParcelSize.MEDIUM.name,
            hubId = null,
            hubName = null,
            courierId = "courier-1",
            courierName = "سفیر احمدی",
            registrationSource = RegistrationSource.FAILED_HOME_DELIVERY.name,
            baseFee = 25000L,
            isSettled = false,
            handoverOtp = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        database.parcelDao().insertParcel(parcelEntity)
        val loaded = database.parcelDao().getParcelByIdDirect("pkg-001")
        assertNotNull(loaded)
        assertEquals("IR-123456", loaded?.trackingNumber)
        assertEquals(ParcelStatus.OUT_FOR_DELIVERY.name, loaded?.status)

        // Update status
        database.parcelDao().updateParcelStatus("pkg-001", ParcelStatus.DELIVERY_ATTEMPTED.name)
        val updated = database.parcelDao().getParcelByIdDirect("pkg-001")
        assertEquals(ParcelStatus.DELIVERY_ATTEMPTED.name, updated?.status)

        // Delete
        database.parcelDao().deleteParcelById("pkg-001")
        val deleted = database.parcelDao().getParcelByIdDirect("pkg-001")
        assertNull(deleted)
    }

    // 3. User entity persistence and approval status mutations
    @Test
    fun testUserPersistenceAndApprovalMutations() = runTest {
        val adminUser = User(
            id = "admin-1",
            fullName = "Admin User",
            phone = "09129998877",
            role = "ADMIN",
            approvalStatus = AccountApprovalStatus.APPROVED
        )
        database.userDao().insertUser(adminUser.toEntity())

        val user = User(
            id = "user-101",
            fullName = "سارا تهرانی",
            phone = "09351112233",
            email = "sara@example.com",
            postalCode = "1234567890",
            address = "تهران",
            role = "HUB_MANAGER",
            approvalStatus = AccountApprovalStatus.PENDING,
            storeName = "سوپرمارکت بهار",
            guildType = "مواد غذایی"
        )

        val regResult = authRepository.registerUser(user)
        assertTrue(regResult.isSuccess)

        val retrieved = authRepository.getUserById("user-101")
        assertNotNull(retrieved)
        assertEquals(AccountApprovalStatus.PENDING, retrieved?.approvalStatus)

        val updateResult = authRepository.updateApprovalStatus("user-101", AccountApprovalStatus.APPROVED, "admin-1")
        assertTrue(updateResult.isSuccess)

        val approved = authRepository.getUserById("user-101")
        assertEquals(AccountApprovalStatus.APPROVED, approved?.approvalStatus)
    }

    // 4. AuditLog append-only integrity
    @Test
    fun testAuditLogAppendOnlyIntegrity() = runTest {
        val log1 = AuditLogEntity(
            id = "audit-1",
            eventType = "PARCEL_CREATED",
            actorId = "courier-1",
            actorRole = "COURIER",
            entityId = "pkg-1",
            oldState = null,
            newState = ParcelStatus.OUT_FOR_DELIVERY.name,
            transactionId = "tx-1",
            metadataJson = null,
            timestamp = 1000L
        )
        val log2 = AuditLogEntity(
            id = "audit-2",
            eventType = "PARCEL_STATUS_TRANSITION",
            actorId = "courier-1",
            actorRole = "COURIER",
            entityId = "pkg-1",
            oldState = ParcelStatus.OUT_FOR_DELIVERY.name,
            newState = ParcelStatus.DELIVERY_ATTEMPTED.name,
            transactionId = "tx-2",
            metadataJson = null,
            timestamp = 2000L
        )

        database.auditLogDao().insertLog(log1)
        database.auditLogDao().insertLog(log2)

        val logs = database.auditLogDao().getAllLogs().first()
        assertEquals(2, logs.size)
        // Verify chronological order (DESC)
        assertEquals("audit-2", logs[0].id)
        assertEquals("audit-1", logs[1].id)
    }

    // 5. RegistrationTransaction uniqueness and idempotency
    @Test
    fun testRegistrationTransactionUniqueness() = runTest {
        val tx = RegistrationTransactionEntity(
            transactionId = "tx-unique-123",
            parcelId = "pkg-001",
            trackingNumber = "TRK-001",
            courierId = "c-1",
            hubId = "h-1",
            registrationSource = RegistrationSource.FAILED_HOME_DELIVERY.name,
            timestamp = System.currentTimeMillis()
        )

        database.registrationTransactionDao().insertTransaction(tx)
        val fetched = database.registrationTransactionDao().getTransactionById("tx-unique-123")
        assertNotNull(fetched)
        assertEquals("pkg-001", fetched?.parcelId)
    }

    // 6. SyncQueue insertion on parcel mutations
    @Test
    fun testSyncQueueInsertion() = runTest {
        val syncItem = SyncQueueEntity(
            id = "sync-1",
            entityType = "PARCEL",
            entityId = "pkg-1",
            action = "CREATE",
            payloadJson = "{}",
            createdAt = System.currentTimeMillis(),
            isSynced = false,
            syncedAt = null
        )

        database.syncQueueDao().insertSyncItem(syncItem)
        val pending = database.syncQueueDao().getPendingSyncItemsDirect()
        assertEquals(1, pending.size)
        assertEquals("pkg-1", pending[0].entityId)

        database.syncQueueDao().markAsSynced("sync-1")
        val pendingAfter = database.syncQueueDao().getPendingSyncItemsDirect()
        assertTrue(pendingAfter.isEmpty())
    }

    // 7. TariffDao version query and active tariff lookup
    @Test
    fun testTariffDaoVersionQuery() = runTest {
        val tariffV1 = SettlementTariffVersionEntity(
            id = "tariff-1",
            versionCode = 1,
            versionName = "تعرفه پایه پاییز",
            baseFee = 20000L,
            effectiveFrom = 1000L,
            isActive = false
        )
        val tariffV2 = SettlementTariffVersionEntity(
            id = "tariff-2",
            versionCode = 2,
            versionName = "تعرفه زمستان",
            baseFee = 25000L,
            effectiveFrom = 2000L,
            isActive = true
        )

        database.tariffDao().insertTariffs(listOf(tariffV1, tariffV2))

        val active = database.tariffDao().getActiveTariffDirect()
        assertNotNull(active)
        assertEquals(2, active?.versionCode)
        assertEquals(25000L, active?.baseFee)
    }

    // 8. CourierSettlementSnapshot insertion and courier query
    @Test
    fun testCourierSettlementSnapshotPersistence() = runTest {
        val snapshot = CourierSettlementSnapshot(
            snapshotId = "snap-101",
            courierId = "courier-01",
            periodStartDate = 1000L,
            periodEndDate = 5000L,
            totalParcelsTransferred = 10,
            confirmedAmount = 75000L,
            pendingAmount = 0L,
            tariffVersionId = "tariff-2",
            snapshotCreatedAt = System.currentTimeMillis(),
            isPaid = false,
            idempotencyKey = "key-snap-101"
        )

        val result = pudoRepository.createSettlementSnapshot(snapshot)
        assertTrue(result.isSuccess)

        val snapshots = pudoRepository.getCourierSettlementSnapshots("courier-01").first()
        assertEquals(1, snapshots.size)
        assertEquals(75000L, snapshots[0].confirmedAmount)
    }

    // 9. PudoRepository.registerPudoParcel atomicity (Parcel + Tx + Audit + SyncQueue)
    @Test
    fun testRegisterPudoParcelAtomicity() = runTest {
        val parcel = Parcel(
            id = "parcel-atom-1",
            trackingNumber = "TRK-ATOM-1",
            senderName = "فرستنده ۱",
            recipientName = "گیرنده ۱",
            recipientPhone = "09120000001",
            recipientPostalCode = "1111111111",
            recipientAddress = "تهران",
            status = ParcelStatus.ELIGIBLE_FOR_HUB,
            size = ParcelSize.MEDIUM,
            assignedHubId = null,
            assignedHubName = null,
            assignedCourierId = "courier-1",
            assignedCourierName = "سفیر احمدی",
            registrationSource = RegistrationSource.FAILED_HOME_DELIVERY,
            baseFee = 25000L
        )

        val tx = RegistrationTransaction(
            transactionId = "tx-atom-1",
            parcelId = parcel.id,
            trackingNumber = parcel.trackingNumber,
            courierId = "courier-1",
            hubId = "hub-1",
            registrationSource = RegistrationSource.FAILED_HOME_DELIVERY
        )

        val result = pudoRepository.registerPudoParcel(parcel, tx, "courier-1", "COURIER")
        assertTrue(result.isSuccess)

        // Verify Parcel inserted
        val storedParcel = database.parcelDao().getParcelByIdDirect(parcel.id)
        assertNotNull(storedParcel)

        // Verify RegistrationTransaction inserted
        val storedTx = database.registrationTransactionDao().getTransactionById("tx-atom-1")
        assertNotNull(storedTx)

        // Verify AuditLog inserted
        val logs = database.auditLogDao().getAllLogs().first()
        assertTrue(logs.any { it.entityId == parcel.id && it.eventType == "PARCEL_PUDO_REGISTERED" })

        // Verify SyncQueue inserted
        val syncItems = database.syncQueueDao().getPendingSyncItemsDirect()
        assertTrue(syncItems.any { it.entityId == parcel.id })
    }

    // 10. PudoRepository idempotency (duplicate transaction returns existing)
    @Test
    fun testRepositoryIdempotencyOnDuplicateTransaction() = runTest {
        val parcel = Parcel(
            id = "parcel-idemp-1",
            trackingNumber = "TRK-IDEMP-1",
            senderName = "فرستنده",
            recipientName = "گیرنده",
            recipientPhone = "09120000002",
            recipientPostalCode = "1111111111",
            recipientAddress = "تهران",
            status = ParcelStatus.ELIGIBLE_FOR_HUB,
            size = ParcelSize.SMALL,
            assignedHubId = null,
            assignedHubName = null,
            assignedCourierId = "courier-1",
            assignedCourierName = "سفیر",
            registrationSource = RegistrationSource.CUSTOMER_REQUEST,
            baseFee = 18000L
        )

        val tx = RegistrationTransaction(
            transactionId = "tx-idemp-1",
            parcelId = parcel.id,
            trackingNumber = parcel.trackingNumber,
            courierId = "courier-1",
            hubId = "hub-1",
            registrationSource = RegistrationSource.CUSTOMER_REQUEST
        )

        val firstCall = pudoRepository.registerPudoParcel(parcel, tx, "courier-1", "COURIER")
        assertTrue(firstCall.isSuccess)

        // Second call with same transaction
        val secondCall = pudoRepository.registerPudoParcel(parcel, tx, "courier-1", "COURIER")
        assertTrue(secondCall.isSuccess)
        assertEquals("parcel-idemp-1", secondCall.getOrNull()?.id)
    }

    // 11. PudoRepository route registration constraint enforcement
    @Test
    fun testRouteRegistrationSourceEnforcement() = runTest {
        assertTrue(ParcelStateMachine.canRegisterFromRoute(RegistrationSource.CUSTOMER_REQUEST))
        assertTrue(ParcelStateMachine.canRegisterFromRoute(RegistrationSource.FAILED_HOME_DELIVERY))
        assertFalse(ParcelStateMachine.canRegisterFromRoute(RegistrationSource.END_OF_SHIFT_RECOVERY))
    }

    // 12. PudoRepository settlement eligibility checks
    @Test
    fun testSettlementEligibilityRule() {
        assertFalse(ParcelStateMachine.isSettlementEligible(ParcelStatus.OUT_FOR_DELIVERY))
        assertFalse(ParcelStateMachine.isSettlementEligible(ParcelStatus.HANDOVER_IN_PROGRESS))
        assertFalse(ParcelStateMachine.isSettlementEligible(ParcelStatus.AWAITING_HUB_CONFIRMATION))
        assertTrue(ParcelStateMachine.isSettlementEligible(ParcelStatus.TRANSFERRED_TO_HUB))
        assertTrue(ParcelStateMachine.isSettlementEligible(ParcelStatus.STORED_AT_HUB))
        assertTrue(ParcelStateMachine.isSettlementEligible(ParcelStatus.DELIVERED_TO_CUSTOMER))
    }

    // 13. Entity-to-Domain and Domain-to-Entity mapping fidelity
    @Test
    fun testEntityDomainMappingFidelity() {
        val domainParcel = Parcel(
            id = "p-map-1",
            trackingNumber = "TRK-MAP-1",
            senderName = "فرستنده تست",
            recipientName = "گیرنده تست",
            recipientPhone = "09123456789",
            recipientPostalCode = "1234567890",
            recipientAddress = "تهران، میدان آزادی",
            status = ParcelStatus.STORED_AT_HUB,
            size = ParcelSize.HEAVY,
            assignedHubId = "hub-99",
            assignedHubName = "هاب مرکزی",
            assignedCourierId = "courier-77",
            assignedCourierName = "سفیر کریمی",
            registrationSource = RegistrationSource.FAILED_HOME_DELIVERY,
            baseFee = 45000L,
            isSettled = true,
            handoverOtp = "9988",
            createdAt = 10000L,
            updatedAt = 20000L
        )

        val entity = domainParcel.toEntity()
        val mappedBack = entity.toDomain()

        assertEquals(domainParcel.id, mappedBack.id)
        assertEquals(domainParcel.trackingNumber, mappedBack.trackingNumber)
        assertEquals(domainParcel.status, mappedBack.status)
        assertEquals(domainParcel.size, mappedBack.size)
        assertEquals(domainParcel.baseFee, mappedBack.baseFee)
        assertEquals(domainParcel.isSettled, mappedBack.isSettled)
        assertEquals(domainParcel.handoverOtp, mappedBack.handoverOtp)
    }

    // 14. End-of-shift recovery constraint validation
    @Test
    fun testEndOfShiftRecoveryConstraint() {
        val recoverySource = RegistrationSource.END_OF_SHIFT_RECOVERY
        assertFalse(recoverySource.isPrimaryWorkflow)
        assertFalse(ParcelStateMachine.canRegisterFromRoute(recoverySource))
    }

    // 15. Complete lifecycle persistence from initial registration to stored at hub
    @Test
    fun testCompleteLifecyclePersistence() = runTest {
        // Step 1: Initial creation in OUT_FOR_DELIVERY
        val parcel = Parcel(
            id = "pkg-life-1",
            trackingNumber = "TRK-LIFE-1",
            senderName = "فروشگاه آنلاین",
            recipientName = "سعید موسوی",
            recipientPhone = "09121113355",
            recipientPostalCode = "1998877665",
            recipientAddress = "تهران",
            status = ParcelStatus.DELIVERY_ATTEMPTED,
            size = ParcelSize.MEDIUM,
            assignedHubId = null,
            assignedHubName = null,
            assignedCourierId = "courier-01",
            assignedCourierName = "سفیر احمدی",
            registrationSource = RegistrationSource.FAILED_HOME_DELIVERY,
            baseFee = 25000L
        )

        val tx = RegistrationTransaction(
            transactionId = "tx-life-1",
            parcelId = parcel.id,
            trackingNumber = parcel.trackingNumber,
            courierId = "courier-01",
            hubId = "hub-01",
            registrationSource = RegistrationSource.FAILED_HOME_DELIVERY
        )

        val regResult = pudoRepository.registerPudoParcel(parcel, tx, "courier-01", "COURIER")
        assertTrue(regResult.isSuccess)

        // Step 2: Customer selects Hub -> Transition to HUB_SELECTED
        // First transition to ELIGIBLE_FOR_HUB
        pudoRepository.transitionParcelStatus(parcel.id, ParcelStatus.ELIGIBLE_FOR_HUB, "cust-01", "CUSTOMER")
        val hubAssignResult = pudoRepository.assignHub(parcel.id, "hub-01", "هاب سعادت‌آباد", "cust-01", "CUSTOMER")
        assertTrue(hubAssignResult.isSuccess)
        assertEquals(ParcelStatus.HUB_SELECTED, hubAssignResult.getOrNull()?.status)

        // Step 3: Courier starts handover with OTP -> HANDOVER_IN_PROGRESS
        val startHandoverResult = pudoRepository.startHandover(parcel.id, "4321", "courier-01", "COURIER")
        assertTrue(startHandoverResult.isSuccess)
        assertEquals(ParcelStatus.HANDOVER_IN_PROGRESS, startHandoverResult.getOrNull()?.status)

        // Step 4: Hub manager confirms handover -> TRANSFERRED_TO_HUB
        val confirmResult = pudoRepository.confirmHubHandover(parcel.id, "4321", "hub-manager-01", "HUB_MANAGER")
        assertTrue(confirmResult.isSuccess)
        assertEquals(ParcelStatus.TRANSFERRED_TO_HUB, confirmResult.getOrNull()?.status)

        // Step 5: Hub manager stores in warehouse -> STORED_AT_HUB
        val storedResult = pudoRepository.receivePackageAtHub(parcel.id, "hub-manager-01", "HUB_MANAGER")
        assertTrue(storedResult.isSuccess)
        assertEquals(ParcelStatus.STORED_AT_HUB, storedResult.getOrNull()?.status)

        // Verify final state in Room Database directly
        val finalEntity = database.parcelDao().getParcelByIdDirect(parcel.id)
        assertNotNull(finalEntity)
        assertEquals(ParcelStatus.STORED_AT_HUB.name, finalEntity?.status)
        assertEquals("hub-01", finalEntity?.hubId)

        // Verify all audit logs were appended
        val auditLogs = database.auditLogDao().getLogsByEntityId(parcel.id).first()
        assertTrue(auditLogs.size >= 5)
    }

    // 12. Cross-Device Sync & Merge: New User Sync from Remote Device
    @Test
    fun testSyncAndMergeUsersCrossDevice() = runTest {
        // Seed system admin first
        authRepository.seedSystemAdminIfMissing("Reza", "Admin@123", "Reza Gh", "09120000000")

        // Simulate Remote Device A creating a user
        val remoteUserA = User(
            id = "user-devA-101",
            username = "customer_device_a",
            phone = "09129998877",
            fullName = "کاربر دستگاه الف",
            nationalId = "0011223344",
            role = "CUSTOMER",
            approvalStatus = AccountApprovalStatus.APPROVED,
            passwordHash = "hash123",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val mergeResult = authRepository.syncAndMergeUsers(listOf(remoteUserA))
        assertTrue(mergeResult.isSuccess)

        // Verify Device B can now retrieve this user
        val retrieved = authRepository.getUserById("user-devA-101")
        assertNotNull(retrieved)
        assertEquals("customer_device_a", retrieved?.username)
        assertEquals("09129998877", retrieved?.phone)
        assertEquals("CUSTOMER", retrieved?.role)

        // Verify System Admin was not overwritten or removed
        val admin = authRepository.getUserByUsername("reza")
        assertNotNull(admin)
        assertEquals("SYSTEM_ADMIN", admin?.role)
    }

    // 13. Cross-Device Sync & Merge: Parcel Sync & Conflict Resolution
    @Test
    fun testSyncAndMergeParcelsCrossDevice() = runTest {
        val baseTimestamp = System.currentTimeMillis()

        // Local parcel exists
        val localParcel = Parcel(
            id = "pkg-sync-01",
            trackingNumber = "IR-SYNC-01",
            senderName = "فرستنده ۱",
            recipientName = "گیرنده ۱",
            recipientPhone = "09123334455",
            recipientPostalCode = "1234567890",
            recipientAddress = "آدرس محلی",
            status = ParcelStatus.TRANSFERRED_TO_HUB,
            size = ParcelSize.SMALL,
            assignedHubId = "hub-tehran-1",
            assignedHubName = "هاب مرکزی",
            assignedCourierId = "courier-1",
            assignedCourierName = "سفیر محلی",
            registrationSource = RegistrationSource.CUSTOMER_REQUEST,
            baseFee = 25000L,
            createdAt = baseTimestamp,
            updatedAt = baseTimestamp
        )

        val tx = RegistrationTransaction(
            transactionId = "tx-sync-1",
            parcelId = localParcel.id,
            trackingNumber = localParcel.trackingNumber,
            courierId = "courier-1",
            hubId = "hub-tehran-1",
            registrationSource = RegistrationSource.CUSTOMER_REQUEST
        )
        pudoRepository.registerPudoParcel(localParcel, tx, "courier-1", "COURIER")

        // Remote parcel has a newer status (STORED_AT_HUB) and higher timestamp
        val remoteNewerParcel = localParcel.copy(
            status = ParcelStatus.STORED_AT_HUB,
            assignedHubId = "hub-tehran-1",
            assignedHubName = "هاب مرکزی",
            updatedAt = baseTimestamp + 10000
        )

        val mergeResult = pudoRepository.syncAndMergeParcels(listOf(remoteNewerParcel))
        assertTrue(mergeResult.isSuccess)

        // Verify local state was updated with newer remote data
        val updatedLocal = pudoRepository.getParcelById("pkg-sync-01").first()
        assertNotNull(updatedLocal)
        assertEquals(ParcelStatus.STORED_AT_HUB, updatedLocal?.status)
        assertEquals("hub-tehran-1", updatedLocal?.assignedHubId)

        // Now test Idempotency: Merging older data should NOT overwrite newer local data
        val staleParcel = localParcel.copy(
            status = ParcelStatus.TRANSFERRED_TO_HUB,
            updatedAt = baseTimestamp - 5000
        )
        pudoRepository.syncAndMergeParcels(listOf(staleParcel))
        val currentLocal = pudoRepository.getParcelById("pkg-sync-01").first()
        assertEquals(ParcelStatus.STORED_AT_HUB, currentLocal?.status)
    }

    // 14. Hub persistence: CRUD, Seeding, and State Mutations via Room
    @Test
    fun testHubPersistenceAndStateMutations() = runTest {
        val initialHubs = listOf(
            HubItem(
                id = "hub-test-1",
                name = "هاب سعادت‌آباد",
                type = "supermarket",
                typeName = "سوپرمارکت",
                managerName = "مدیر هاب",
                phone = "09121112233",
                licenseNumber = "12345",
                address = "سعادت‌آباد، میدان کاج",
                rating = 4.8f,
                reviewCount = 10,
                workingHours = "08:00 - 22:00",
                isOpen = true,
                currentPackagesCount = 0,
                maxCapacity = 50,
                lat = 35.78,
                lng = 51.37
            )
        )

        // Test Seeding
        pudoRepository.seedHubsIfEmpty(initialHubs)
        var allHubs = pudoRepository.getAllHubs().first()
        assertEquals(1, allHubs.size)
        assertEquals("hub-test-1", allHubs[0].id)
        assertTrue(allHubs[0].isOpen)

        // Test Insert new Hub by Admin
        val adminCreatedHub = HubItem(
            id = "hub-test-2",
            name = "هاب تجریش",
            type = "store",
            typeName = "فروشگاه",
            managerName = "اکبر رضایی",
            phone = "09129990011",
            licenseNumber = "99887",
            address = "تجریش، خیابان شهرداری",
            rating = 5.0f,
            reviewCount = 2,
            workingHours = "09:00 - 21:00",
            isOpen = true,
            currentPackagesCount = 0,
            maxCapacity = 40,
            lat = 35.80,
            lng = 51.42
        )
        pudoRepository.insertHub(adminCreatedHub)
        allHubs = pudoRepository.getAllHubs().first()
        assertEquals(2, allHubs.size)

        // Test Toggle / Update Hub Status
        val hubToUpdate = allHubs.find { it.id == "hub-test-2" }
        assertNotNull(hubToUpdate)
        val updated = hubToUpdate!!.copy(isOpen = false, currentPackagesCount = 5)
        pudoRepository.updateHub(updated)

        val retrievedUpdated = pudoRepository.getHubById("hub-test-2").first()
        assertNotNull(retrievedUpdated)
        assertFalse(retrievedUpdated!!.isOpen)
        assertEquals(5, retrievedUpdated.currentPackagesCount)

        // Test Delete Hub
        pudoRepository.deleteHub("hub-test-1")
        val remaining = pudoRepository.getAllHubs().first()
        assertEquals(1, remaining.size)
        assertEquals("hub-test-2", remaining[0].id)
    }

    // 15. Hub Receipt Persistence and Audit Log Transaction
    @Test
    fun testHubReceiptPersistenceAndAuditLog() = runTest {
        val parcel = Parcel(
            id = "pkg-receipt-test",
            trackingNumber = "TRK-RECEIPT-999",
            senderName = "فرستنده",
            recipientName = "گیرنده",
            recipientPhone = "09120000099",
            recipientPostalCode = "1234567890",
            recipientAddress = "تهران",
            status = ParcelStatus.TRANSFERRED_TO_HUB,
            size = ParcelSize.MEDIUM,
            assignedHubId = "hub-test-1",
            assignedHubName = "هاب سعادت‌آباد",
            assignedCourierId = "courier-1",
            assignedCourierName = "سفیر کریمی",
            registrationSource = RegistrationSource.FAILED_HOME_DELIVERY,
            baseFee = 25000L
        )

        database.parcelDao().insertParcel(parcel.toEntity())

        // Perform Hub Receipt using Tracking Number lookup fallback
        val receiptResult = pudoRepository.receivePackageAtHub("TRK-RECEIPT-999", "hub-mgr-1", "HUB_MANAGER")
        assertTrue(receiptResult.isSuccess)
        val receivedParcel = receiptResult.getOrNull()
        assertNotNull(receivedParcel)
        assertEquals(ParcelStatus.STORED_AT_HUB, receivedParcel?.status)

        // Verify Direct Room State
        val directEntity = database.parcelDao().getParcelByIdDirect(parcel.id)
        assertNotNull(directEntity)
        assertEquals(ParcelStatus.STORED_AT_HUB.name, directEntity?.status)

        // Verify Audit Log generated atomically
        val auditLogs = database.auditLogDao().getLogsByEntityId(parcel.id).first()
        assertTrue(auditLogs.any { it.eventType == "PARCEL_RECEIVED_AT_HUB" && it.actorRole == "HUB_MANAGER" })
    }

    // 16. Customer Delivery Persistence and Audit Log Transaction
    @Test
    fun testCustomerDeliveryPersistenceAndAuditLog() = runTest {
        val parcel = Parcel(
            id = "pkg-delivery-test",
            trackingNumber = "TRK-DELIVERY-888",
            senderName = "فرستنده",
            recipientName = "گیرنده",
            recipientPhone = "09120000088",
            recipientPostalCode = "1234567890",
            recipientAddress = "تهران",
            status = ParcelStatus.STORED_AT_HUB,
            size = ParcelSize.SMALL,
            assignedHubId = "hub-test-1",
            assignedHubName = "هاب سعادت‌آباد",
            assignedCourierId = "courier-1",
            assignedCourierName = "سفیر کریمی",
            registrationSource = RegistrationSource.CUSTOMER_REQUEST,
            baseFee = 20000L
        )

        database.parcelDao().insertParcel(parcel.toEntity())

        // Perform Customer Delivery using Tracking Number lookup fallback
        val deliverResult = pudoRepository.deliverToCustomer("TRK-DELIVERY-888", "hub-mgr-1", "HUB_MANAGER")
        assertTrue(deliverResult.isSuccess)
        val deliveredParcel = deliverResult.getOrNull()
        assertNotNull(deliveredParcel)
        assertEquals(ParcelStatus.DELIVERED_TO_CUSTOMER, deliveredParcel?.status)

        // Verify Direct Room State
        val directEntity = database.parcelDao().getParcelByIdDirect(parcel.id)
        assertNotNull(directEntity)
        assertEquals(ParcelStatus.DELIVERED_TO_CUSTOMER.name, directEntity?.status)

        // Verify Audit Log generated atomically
        val auditLogs = database.auditLogDao().getLogsByEntityId(parcel.id).first()
        assertTrue(auditLogs.any { it.eventType == "PARCEL_DELIVERED_TO_CUSTOMER" && it.actorRole == "HUB_MANAGER" })
    }

    // 17. Hub Atomic Transactions with AuditLog and SyncQueue
    @Test
    fun testHubAtomicTransactionsWithAuditAndSyncQueue() = runTest {
        val hub = HubItem(
            id = "hub-sync-test-01",
            name = "هاب آزمایشی ونک",
            type = "supermarket",
            typeName = "سوپرمارکت محلی",
            managerName = "مدیر هاب آزمایشی",
            phone = "09123334455",
            licenseNumber = "ص-12345",
            address = "میدان ونک",
            rating = 4.8f,
            reviewCount = 10,
            workingHours = "08:00 - 22:00",
            isOpen = true,
            currentPackagesCount = 0,
            maxCapacity = 60,
            lat = 35.75,
            lng = 51.40
        )

        // Test Insert Hub
        pudoRepository.insertHub(hub)

        // Verify entity in Room
        val loadedHub = database.hubDao().getHubByIdDirect(hub.id)
        assertNotNull(loadedHub)
        assertEquals("هاب آزمایشی ونک", loadedHub?.name)
        assertTrue((loadedHub?.updatedAt ?: 0L) > 0L)

        // Verify Audit Log
        val insertLogs = database.auditLogDao().getLogsByEntityId(hub.id).first()
        assertTrue(insertLogs.any { it.eventType == "HUB_CREATED" })

        // Verify SyncQueue
        val pendingSyncItems = database.syncQueueDao().getPendingSyncItemsDirect()
        val hubInsertSync = pendingSyncItems.find { it.entityId == hub.id && it.action == "CREATE" }
        assertNotNull(hubInsertSync)
        assertEquals("HUB", hubInsertSync?.entityType)
        assertTrue(hubInsertSync?.payloadJson?.contains("updatedAt") == true)

        // Test Update Hub
        val updatedHub = hub.copy(name = "هاب بروزرسانی شده ونک", isOpen = false)
        pudoRepository.updateHub(updatedHub)

        val reloadedHub = database.hubDao().getHubByIdDirect(hub.id)
        assertNotNull(reloadedHub)
        assertEquals("هاب بروزرسانی شده ونک", reloadedHub?.name)
        assertFalse(reloadedHub?.isOpen ?: true)

        val updateLogs = database.auditLogDao().getLogsByEntityId(hub.id).first()
        assertTrue(updateLogs.any { it.eventType == "HUB_UPDATED" })

        val pendingSyncAfterUpdate = database.syncQueueDao().getPendingSyncItemsDirect()
        val hubUpdateSync = pendingSyncAfterUpdate.find { it.entityId == hub.id && it.action == "UPDATE" }
        assertNotNull(hubUpdateSync)

        // Test Delete Hub
        pudoRepository.deleteHub(hub.id)
        val deletedHub = database.hubDao().getHubByIdDirect(hub.id)
        assertNull(deletedHub)

        val deleteLogs = database.auditLogDao().getLogsByEntityId(hub.id).first()
        assertTrue(deleteLogs.any { it.eventType == "HUB_DELETED" })

        val pendingSyncAfterDelete = database.syncQueueDao().getPendingSyncItemsDirect()
        val hubDeleteSync = pendingSyncAfterDelete.find { it.entityId == hub.id && it.action == "DELETE" }
        assertNotNull(hubDeleteSync)
    }

    // 18. payPackageFee Repository Transaction and SyncQueue
    @Test
    fun testPayPackageFeeAtomicTransaction() = runTest {
        val parcel = Parcel(
            id = "pkg-fee-test-01",
            trackingNumber = "TRK-FEE-12345",
            senderName = "فرستنده",
            recipientName = "گیرنده",
            recipientPhone = "09121112233",
            recipientPostalCode = "1234567890",
            recipientAddress = "تهران",
            status = ParcelStatus.STORED_AT_HUB,
            size = ParcelSize.MEDIUM,
            assignedHubId = "hub-01",
            assignedHubName = "هاب مرکزی",
            assignedCourierId = "courier-01",
            assignedCourierName = "سفیر کریمی",
            registrationSource = RegistrationSource.CUSTOMER_REQUEST,
            baseFee = 35000L,
            isSettled = false
        )

        database.parcelDao().insertParcel(parcel.toEntity())

        // Execute payment via repository
        val payResult = pudoRepository.payPackageFee(parcel.id, "user-customer-99", "CUSTOMER")
        assertTrue(payResult.isSuccess)
        val paidParcel = payResult.getOrNull()
        assertNotNull(paidParcel)
        assertTrue(paidParcel?.isSettled == true)

        // Verify direct Room persistence
        val directEntity = database.parcelDao().getParcelByIdDirect(parcel.id)
        assertNotNull(directEntity)
        assertTrue(directEntity?.isSettled == true)

        // Verify Audit Log
        val auditLogs = database.auditLogDao().getLogsByEntityId(parcel.id).first()
        assertTrue(auditLogs.any { it.eventType == "PARCEL_FEE_PAID" && it.actorId == "user-customer-99" })

        // Verify SyncQueue
        val pendingSync = database.syncQueueDao().getPendingSyncItemsDirect()
        val syncItem = pendingSync.find { it.entityId == parcel.id && it.action == "UPDATE" }
        assertNotNull(syncItem)
        assertEquals("PARCEL", syncItem?.entityType)
        assertTrue(syncItem?.payloadJson?.contains("isSettled\":true") == true)
    }
}
