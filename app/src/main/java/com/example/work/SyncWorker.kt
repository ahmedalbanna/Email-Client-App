package com.example.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import com.example.data.EmailDatabase
import com.example.data.EmailRepository
import com.example.data.remote.FakeMailRemoteDataSource
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Phase 2.3 — Periodic IMAP sync.
 * Real implementation would use delta UID + push; fake injects occasional demo messages.
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = EmailDatabase.getDatabase(applicationContext)
            val repo = EmailRepository(db, FakeMailRemoteDataSource())
            val accounts = repo.allAccounts.first()
            var totalSynced = 0
            for (acc in accounts) {
                val r = repo.syncFolder(acc.id, "Inbox")
                if (r.isSuccess) totalSynced += r.getOrNull() ?: 0
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "email_periodic_sync"

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    androidx.work.Constraints.Builder()
                        .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
