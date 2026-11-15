package com.efina.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The mirror's two load-bearing rules, against a real (in-memory) Room database.
 *
 *  1. A delta pull must never overwrite a row with an unsent edit, or the user's
 *     change disappears under them mid-edit.
 *  2. A visibility change wipes the mirror, because sync_seq cannot express
 *     "rows you could no longer see".
 *
 * Built with `inMemoryDatabaseBuilder`, so SQLCipher is not involved — the
 * encryption is a property of how the file is opened, not of the schema.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MirrorRulesTest {

    private lateinit var db: EfinaDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, EfinaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = db.close()

    private fun account(
        id: Long,
        name: String = "cash",
        syncSeq: Long = 1,
        pending: PendingState = PendingState.SYNCED,
        balance: Double = 0.0,
    ) = AccountEntity(
        id = id, userId = 1, name = name, type = "Cash", balance = balance,
        currency = "IDR", description = null, color = null, displayOrder = 0,
        disabled = false, initialBalance = 0.0, createdAt = null, isShared = false,
        familyId = null, role = null, memberCount = 0, ownerUserId = 1,
        syncSeq = syncSeq, pendingState = pending,
    )

    // ---- rule 1: a pull must not clobber an unsent edit ---------------------

    @Test
    fun `pull overwrites a row that has no pending edit`() = runTest {
        db.accountDao().upsert(account(1, name = "old", syncSeq = 5))
        db.accountDao().upsertFromServer(account(1, name = "new", syncSeq = 9))

        val row = db.accountDao().byId(1)
        assertEquals("new", row?.name)
        assertEquals(9L, row?.syncSeq)
    }

    @Test
    fun `pull does not overwrite a row with an unsent edit`() = runTest {
        db.accountDao().upsert(account(1, name = "mine", syncSeq = 5, pending = PendingState.PENDING_UPDATE))
        db.accountDao().upsertFromServer(account(1, name = "theirs", syncSeq = 9))

        val row = db.accountDao().byId(1)
        assertEquals("the local edit must survive", "mine", row?.name)
        // But the server's version is adopted, so the queued write conflicts
        // instead of silently overwriting.
        assertEquals(9L, row?.syncSeq)
        assertEquals(PendingState.PENDING_UPDATE, row?.pendingState)
    }

    @Test
    fun `pull inserts a row the mirror has never seen`() = runTest {
        db.accountDao().upsertFromServer(account(7, name = "joint", syncSeq = 3))
        assertNotNull(db.accountDao().byId(7))
    }

    @Test
    fun `a pending create is not replaced by the pull`() = runTest {
        db.accountDao().upsert(account(-42, name = "offline", pending = PendingState.PENDING_CREATE))
        db.accountDao().upsertFromServer(account(-42, name = "server", syncSeq = 2))
        assertEquals("offline", db.accountDao().byId(-42)?.name)
    }

    // ---- rule 2: a visibility change wipes the mirror ----------------------

    @Test
    fun `clear empties every mirrored table`() = runTest {
        db.accountDao().upsert(account(1))
        db.categoryDao().upsert(
            CategoryEntity(1, 1, "Food", "#f00", null, null, null, false, false, "expense", null)
        )
        db.goalDao().upsert(
            GoalEntity(1, 1, "Holiday", 100.0, 0.0, null, null, null, null)
        )

        db.accountDao().clear()
        db.categoryDao().clear()
        db.goalDao().clear()

        assertEquals(0, db.accountDao().observeAll().first().size)
        assertEquals(0, db.categoryDao().observeAll().first().size)
        assertEquals(0, db.goalDao().observeAll().first().size)
    }

    @Test
    fun `wiping a mirror does not touch the queue`() = runTest {
        // The queue is the user's data. A resync must not discard writes that
        // have not reached the server yet.
        db.pendingMutationDao().insert(
            PendingMutationEntity(
                id = "m1", entity = "accounts", op = "update", method = "PUT",
                path = "accounts/1", localId = 1, serverId = 1, payloadJson = "{}",
                baseSyncSeq = 1, idempotencyKey = "k1", createdAt = 1,
            )
        )
        db.accountDao().clear()
        assertEquals(1, db.pendingMutationDao().count())
    }

    // ---- the queue itself --------------------------------------------------

    @Test
    fun `queue counts and clears`() = runTest {
        assertEquals(0, db.pendingMutationDao().count())
        db.pendingMutationDao().insert(
            PendingMutationEntity(
                id = "a", entity = "transactions", op = "create", method = "POST",
                path = "transactions", localId = -1, serverId = null, payloadJson = "{}",
                baseSyncSeq = null, idempotencyKey = "ka", createdAt = 1,
            )
        )
        assertEquals(1, db.pendingMutationDao().count())
        db.pendingMutationDao().delete("a")
        assertEquals(0, db.pendingMutationDao().count())
    }

    @Test
    fun `a create's server id is bound to its local id`() = runTest {
        db.pendingMutationDao().insert(
            PendingMutationEntity(
                id = "b", entity = "transactions", op = "create", method = "POST",
                path = "transactions", localId = -5, serverId = null, payloadJson = "{}",
                baseSyncSeq = null, idempotencyKey = "kb", createdAt = 1,
            )
        )
        db.pendingMutationDao().setServerId(-5, 99)
        assertEquals(99L, db.pendingMutationDao().all().first().serverId)
    }

    // ---- sync state --------------------------------------------------------

    @Test
    fun `sync state round-trips the cursor and visibility version`() = runTest {
        assertNull(db.syncStateDao().get())
        db.syncStateDao().put(SyncStateEntity(cursor = 120, visibilityVersion = 3))
        val row = db.syncStateDao().get()
        assertEquals(120L, row?.cursor)
        assertEquals(3L, row?.visibilityVersion)
    }
}
