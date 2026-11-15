package com.efina.app.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Flushes the queue and pulls the delta.
 *
 * WorkManager rather than a coroutine tied to a screen: a queued write must
 * survive the app being killed, and the flush has to happen when connectivity
 * returns even if the user never reopens the app.
 *
 * The work is unique by name with `KEEP`, so a connectivity change and a
 * foreground resume cannot start two workers that race over the same queue.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val engine: SyncEngine,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val outcome = engine.sync()
        return when {
            // The session died with a queue still waiting. Retrying will not
            // help until the user signs in, and the queue is preserved.
            outcome.needsReauth -> Result.failure()
            outcome.retryLater -> Result.retry()
            else -> Result.success()
        }
    }

    companion object {
        const val NAME = "efina-sync"

        /** Enqueue a flush. Safe to call from anywhere, any number of times. */
        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                NAME,
                ExistingWorkPolicy.KEEP,
                request,
            )
        }
    }
}
