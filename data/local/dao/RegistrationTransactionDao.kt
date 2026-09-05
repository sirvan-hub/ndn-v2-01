package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entities.RegistrationTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistrationTransactionDao {

    @Query("SELECT * FROM registration_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<RegistrationTransactionEntity>>

    @Query("SELECT * FROM registration_transactions WHERE transactionId = :transactionId LIMIT 1")
    suspend fun getTransactionById(transactionId: String): RegistrationTransactionEntity?

    @Query("SELECT * FROM registration_transactions WHERE parcelId = :parcelId LIMIT 1")
    suspend fun getTransactionByParcelId(parcelId: String): RegistrationTransactionEntity?

    @Query("SELECT * FROM registration_transactions WHERE trackingNumber = :trackingNumber LIMIT 1")
    suspend fun getTransactionByTrackingNumber(trackingNumber: String): RegistrationTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(transaction: RegistrationTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactions(transactions: List<RegistrationTransactionEntity>)
}
