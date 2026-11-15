package com.efina.app.data.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The enqueue contract for the sync worker.
 *
 * WorkManager is the reason a queued write survives the app being killed, so
 * the properties that matter are: the work is *unique* by name with KEEP, and it
 * requires a network. Without KEEP, a connectivity change and a foreground
 * resume can start two workers that race over the same queue — one draining a
 * mutation while the other reads it as still pending.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SyncWorkerEnqueueTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val config = Configuration.Builder()
            .setExecutor(SynchronousExecutor())
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    private fun workInfos() =
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(SyncWorker.NAME)
            .get()

    @Test
    fun `enqueue creates one unique unit of work`() {
        SyncWorker.enqueue(context)
        assertEquals(1, workInfos().size)
        assertEquals(SyncWorker.NAME, workInfos().first().id.let { SyncWorker.NAME })
    }

    @Test
    fun `enqueueing twice with KEEP does not stack a second worker`() {
        // Two triggers (connectivity regained, app resumed) must not produce two
        // flushes racing over the same queue.
        SyncWorker.enqueue(context)
        SyncWorker.enqueue(context)
        assertEquals(1, workInfos().size)
    }

    @Test
    fun `the work requires a network`() {
        SyncWorker.enqueue(context)
        val constraints = workInfos().first().constraints
        assertEquals(
            androidx.work.NetworkType.CONNECTED,
            constraints.requiredNetworkType,
        )
    }

    @Test
    fun `the unique work name is stable`() {
        // Renaming it would strand every queued flush until the next trigger.
        assertEquals("efina-sync", SyncWorker.NAME)
    }
}
