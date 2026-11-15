package com.efina.app.data.sync

import com.efina.app.data.local.EfinaDatabase
import com.efina.app.data.local.PendingMutationEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Queues a write for the server and applies it locally first.
 *
 * Every write goes through here, online or not. That is deliberate: if the local
 * mirror were only updated on the way back from the network, a write made while
 * offline would either have to be special-cased in every ViewModel or would not
 * appear until the next pull. One path, always.
 *
 * A row created offline gets a negative id (the server assigns the real one on
 * sync). Negative ids keep the domain models unchanged and make "this has not
 * reached the server" visible in the data itself.
 */
@Singleton
class MutationQueue @Inject constructor(
    private val db: EfinaDatabase,
) {
    private val counter = java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis())

    /** A negative, locally-unique id for a row the server has not seen yet. */
    fun newLocalId(): Long = -counter.incrementAndGet()

    /**
     * Record a mutation.
     *
     * The idempotency key is generated here, once, and reused on every retry. If
     * it were generated per attempt, a create that timed out after the server
     * committed would insert a second row on the retry.
     */
    suspend fun enqueue(
        entity: String,
        op: String,
        method: String,
        path: String,
        localId: Long,
        serverId: Long?,
        payloadJson: String,
        baseSyncSeq: Long?,
    ) {
        db.pendingMutationDao().insert(
            PendingMutationEntity(
                id = UUID.randomUUID().toString(),
                entity = entity,
                op = op,
                method = method,
                path = path,
                localId = localId,
                serverId = serverId,
                payloadJson = payloadJson,
                baseSyncSeq = baseSyncSeq,
                idempotencyKey = UUID.randomUUID().toString(),
                createdAt = System.currentTimeMillis(),
            )
        )
    }

    /** Keep a later mutation pointing at the row a create will become. */
    suspend fun bindServerId(localId: Long, serverId: Long) {
        db.pendingMutationDao().setServerId(localId, serverId)
    }

    suspend fun pendingCount(): Int = db.pendingMutationDao().count()
}
