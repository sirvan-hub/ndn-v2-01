package com.example.remote

import com.example.data.local.entities.*
import com.example.data.model.*
import com.example.data.remote.SupabaseConfig
import com.example.data.remote.dto.*
import com.example.model.HubItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class SupabaseDtoSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    @Test
    fun testUserDtoSerializationAndMapping() {
        val domainUser = User(
            id = "usr-100",
            username = "courier_ali",
            passwordHash = "hash123",
            fullName = "Ali Rezaei",
            phone = "09121112233",
            nationalId = "0011223344",
            email = "ali@example.com",
            postalCode = "1983963111",
            address = "Tehran, Saadat Abad",
            role = "COURIER",
            approvalStatus = AccountApprovalStatus.APPROVED,
            storeName = "",
            guildType = "",
            bankCardNumber = "6037991829384756",
            vehicleType = "Motorcycle",
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val dto = SupabaseUserDto.fromDomain(domainUser)
        assertEquals("usr-100", dto.id)
        assertEquals("courier_ali", dto.username)
        assertEquals("09121112233", dto.phone)

        val serialized = json.encodeToString(dto)
        assertTrue(serialized.contains("\"full_name\":\"Ali Rezaei\""))
        assertTrue(serialized.contains("\"bank_card_number\":\"6037991829384756\""))
        assertTrue(serialized.contains("\"updated_at\":2000"))

        val deserialized = json.decodeFromString<SupabaseUserDto>(serialized)
        val mappedDomain = deserialized.toDomain()
        assertEquals(domainUser.id, mappedDomain.id)
        assertEquals(domainUser.fullName, mappedDomain.fullName)
        assertEquals(domainUser.phone, mappedDomain.phone)
        assertEquals(domainUser.role, mappedDomain.role)
        assertEquals(domainUser.approvalStatus, mappedDomain.approvalStatus)

        val entity = dto.toEntity()
        assertEquals(domainUser.id, entity.id)
        assertEquals(domainUser.fullName, entity.fullName)
    }

    @Test
    fun testHubDtoSerializationAndMapping() {
        val hub = HubItem(
            id = "hub-200",
            name = "Saadat Abad Hub",
            type = "supermarket",
            typeName = "سوپرمارکت محله",
            managerName = "Mohammad Hosseini",
            phone = "02122334455",
            licenseNumber = "LIC-9988",
            address = "Saadat Abad, Blvd Daryaa",
            rating = 4.9f,
            reviewCount = 150,
            workingHours = "08:00 - 22:00",
            isOpen = true,
            currentPackagesCount = 5,
            maxCapacity = 60,
            lat = 35.7900,
            lng = 51.3700
        )

        val dto = SupabaseHubDto.fromDomain(hub, createdAt = 1500L, updatedAt = 2500L)
        val serialized = json.encodeToString(dto)
        assertTrue(serialized.contains("\"manager_name\":\"Mohammad Hosseini\""))
        assertTrue(serialized.contains("\"max_capacity\":60"))
        assertTrue(serialized.contains("\"is_open\":true"))

        val deserialized = json.decodeFromString<SupabaseHubDto>(serialized)
        val domainHub = deserialized.toDomain()
        assertEquals(hub.id, domainHub.id)
        assertEquals(hub.name, domainHub.name)
        assertEquals(hub.managerName, domainHub.managerName)
        assertEquals(hub.isOpen, domainHub.isOpen)
    }

    @Test
    fun testParcelDtoSerializationAndMapping() {
        val parcel = Parcel(
            id = "pcl-300",
            trackingNumber = "IR-PUDO-300",
            senderName = "Digikala",
            recipientName = "Sara Ahmadi",
            recipientPhone = "09129876543",
            recipientPostalCode = "1998877665",
            recipientAddress = "Vanak Square",
            status = ParcelStatus.STORED_AT_HUB,
            size = ParcelSize.LARGE,
            assignedHubId = "hub-200",
            assignedHubName = "Saadat Abad Hub",
            assignedCourierId = "usr-100",
            assignedCourierName = "Ali Rezaei",
            registrationSource = RegistrationSource.FAILED_HOME_DELIVERY,
            baseFee = 35000L,
            isSettled = true,
            handoverOtp = "482910",
            createdAt = 3000L,
            updatedAt = 4000L
        )

        val dto = SupabaseParcelDto.fromDomain(parcel)
        val serialized = json.encodeToString(dto)
        assertTrue(serialized.contains("\"tracking_number\":\"IR-PUDO-300\""))
        assertTrue(serialized.contains("\"registration_source\":\"FAILED_HOME_DELIVERY\""))
        assertTrue(serialized.contains("\"is_settled\":true"))
        assertTrue(serialized.contains("\"handover_otp\":\"482910\""))

        val deserialized = json.decodeFromString<SupabaseParcelDto>(serialized)
        val domainParcel = deserialized.toDomain()
        assertEquals(parcel.id, domainParcel.id)
        assertEquals(parcel.trackingNumber, domainParcel.trackingNumber)
        assertEquals(parcel.status, domainParcel.status)
        assertEquals(parcel.size, domainParcel.size)
        assertEquals(parcel.registrationSource, domainParcel.registrationSource)
        assertEquals(parcel.isSettled, domainParcel.isSettled)
    }

    @Test
    fun testAuditLogDtoSerializationAndMapping() {
        val log = AuditLog(
            id = "audit-400",
            eventType = "PARCEL_STATUS_TRANSITION",
            actorId = "usr-100",
            actorRole = "COURIER",
            entityId = "pcl-300",
            oldState = "HANDOVER_IN_PROGRESS",
            newState = "STORED_AT_HUB",
            transactionId = "tx-500",
            timestamp = 5000L,
            metadataJson = "{\"reason\":\"Hub manager confirmed OTP\"}"
        )

        val dto = SupabaseAuditLogDto.fromDomain(log)
        val serialized = json.encodeToString(dto)
        assertTrue(serialized.contains("\"event_type\":\"PARCEL_STATUS_TRANSITION\""))
        assertTrue(serialized.contains("\"old_state\":\"HANDOVER_IN_PROGRESS\""))
        assertTrue(serialized.contains("\"metadata_json\""))

        val deserialized = json.decodeFromString<SupabaseAuditLogDto>(serialized)
        val domainLog = deserialized.toDomain()
        assertEquals(log.id, domainLog.id)
        assertEquals(log.eventType, domainLog.eventType)
        assertEquals(log.oldState, domainLog.oldState)
        assertEquals(log.newState, domainLog.newState)
    }

    @Test
    fun testRegistrationTransactionDtoSerializationAndMapping() {
        val tx = RegistrationTransaction(
            transactionId = "tx-500",
            parcelId = "pcl-300",
            trackingNumber = "IR-PUDO-300",
            courierId = "usr-100",
            hubId = "hub-200",
            registrationSource = RegistrationSource.CUSTOMER_REQUEST,
            timestamp = 6000L,
            clientGeneratedId = "tx-500"
        )

        val dto = SupabaseRegistrationTransactionDto.fromDomain(tx)
        val serialized = json.encodeToString(dto)
        assertTrue(serialized.contains("\"transaction_id\":\"tx-500\""))
        assertTrue(serialized.contains("\"client_generated_id\":\"tx-500\""))

        val deserialized = json.decodeFromString<SupabaseRegistrationTransactionDto>(serialized)
        val domainTx = deserialized.toDomain()
        assertEquals(tx.transactionId, domainTx.transactionId)
        assertEquals(tx.parcelId, domainTx.parcelId)
        assertEquals(tx.registrationSource, domainTx.registrationSource)
    }

    @Test
    fun testSupabaseConfigSecurityAndMasking() {
        val config = SupabaseConfig(
            url = "https://ndnpudo.supabase.co",
            publishableKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.anon_key_sample"
        )

        assertTrue(config.isConfigured())
        val toStringOutput = config.toString()
        // Must NOT leak full key in logs
        assertFalse(toStringOutput.contains("anon_key_sample"))
        assertTrue(toStringOutput.contains("..."))

        // Unconfigured check
        val emptyConfig = SupabaseConfig(url = "", publishableKey = "")
        assertFalse(emptyConfig.isConfigured())
    }
}
