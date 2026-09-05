package com.example.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.database.PudoDatabase
import com.example.data.remote.SupabaseRemoteSyncDataSourceImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Production-grade background sync worker managed by WorkManager.
 * 
 * - Network-constrained: only executes when device has active network.
 * - Outbound push followed by inbound delta synchronization.
 * - Mutex protected within PudoSyncEngine against concurrent worker or foreground execution.
 * - Returns Result.retry() with bounded exponential backoff upon transient failure.
 */
class PudoSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val database = PudoDatabase.getInstance(applicationContext)
            val remoteDataSource = SupabaseRemoteSyncDataSourceImpl()
            val syncEngine = PudoSyncEngine(database, remoteDataSource)

            val fullSyncResult = syncEngine.synchronize()

            // If there were retryable outbound failures and 0 successes, request retry
            if (fullSyncResult.outbound.failureCount > 0 && fullSyncResult.outbound.successCount == 0) {
                if (runAttemptCount < MAX_WORKER_ATTEMPTS) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            if (runAttemptCount < MAX_WORKER_ATTEMPTS) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val MAX_WORKER_ATTEMPTS = 5
    }
}
