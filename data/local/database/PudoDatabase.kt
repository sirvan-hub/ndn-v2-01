package com.example.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.*
import com.example.data.local.entities.*

@Database(
    entities = [
        ParcelEntity::class,
        UserEntity::class,
        AuditLogEntity::class,
        RegistrationTransactionEntity::class,
        SyncQueueEntity::class,
        SettlementTariffVersionEntity::class,
        CourierSettlementSnapshotEntity::class,
        MobileChangeRequestEntity::class,
        HubEntity::class,
        SyncCheckpointEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class PudoDatabase : RoomDatabase() {

    abstract fun parcelDao(): ParcelDao
    abstract fun userDao(): UserDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun registrationTransactionDao(): RegistrationTransactionDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun tariffDao(): TariffDao
    abstract fun courierSettlementSnapshotDao(): CourierSettlementSnapshotDao
    abstract fun mobileChangeRequestDao(): MobileChangeRequestDao
    abstract fun hubDao(): HubDao
    abstract fun syncCheckpointDao(): SyncCheckpointDao

    companion object {
        @Volatile
        private var INSTANCE: PudoDatabase? = null

        fun getInstance(context: Context): PudoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PudoDatabase::class.java,
                    "pudo_iran.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun createInMemory(context: Context): PudoDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                PudoDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()
        }
    }
}
