package com.efina.app.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.efina.app.data.local.EfinaDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A queued write must actually be sent.
 *
 * The bug this pins: `MutationQueue.enqueue` inserted the mutation and stopped.
 * Nothing triggered a flush, so a transaction created with a perfectly good
 * connection sat at PENDING_CREATE — showing the pending badge — until the user
 * opened Settings and tapped "Sync now". The queue was correct; the trigger was
 * missing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MutationQueueTriggersSyncTest {

    private lateinit var context: Context
    private lateinit var db: EfinaDatabase
    private lateinit var queue: MutationQueue

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
        db = Room.inMemoryDatabaseBuilder(context, EfinaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        queue = MutationQueue(context, db)
    }

    @After
    fun tearDown() = db.close()

    private fun syncWork() =
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(SyncWorker.NAME)
            .get()

    @Test
    fun `enqueueing a mutation schedules a flush`() = runTest {
        assertEquals("no work before a write", 0, syncWork().size)

        queue.enqueue(
            entity = "transactions",
            op = "create",
            method = "POST",
            path = "transactions",
            localId = -1,
            serverId = null,
            payloadJson = """{"amount":1}""",
            baseSyncSeq = null,
        )

        assertEquals(
            "a queued write must schedule the sync worker, or it never leaves the device",
            1,
            syncWork().size,
        )
    }

    @Test
    fun `the mutation is durable even if the flush cannot run yet`() = runTest {
        queue.enqueue(
            entity = "transactions",
            op = "create",
            method = "POST",
            path = "transactions",
            localId = -2,
            serverId = null,
            payloadJson = """{"amount":2}""",
            baseSyncSeq = null,
        )
        // The queue is the user's data: scheduling work must not depend on it
        // succeeding, and the row must be there for the next attempt.
        assertEquals(1, db.pendingMutationDao().count())
    }
}
