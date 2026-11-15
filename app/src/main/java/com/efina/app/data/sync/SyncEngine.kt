package com.efina.app.data.sync

import com.efina.app.data.local.ConflictEntity
import com.efina.app.data.local.EfinaDatabase
import com.efina.app.data.local.PendingMutationEntity
import com.efina.app.data.local.PendingState
import com.efina.app.data.local.SyncStateEntity
import com.efina.app.data.local.toEntity
import com.efina.app.data.remote.api.EfinaApi
import com.efina.app.data.remote.dto.SyncResponseDto
import com.efina.app.data.remote.interceptor.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The sync engine: flush the queue, then pull the delta.
 *
 * Order matters. Flushing first means the client's own writes are on the server
 * before the pull, so the pull returns them with their real ids and versions
 * rather than the client having to reconcile two sources of truth.
 *
 * Failure policy, in one place:
 *
 *  - 2xx            the write landed; clear the mutation, adopt the server row
 *  - 409            someone else changed the row first; park a conflict
 *  - 404 on PUT/DELETE  the row is already gone; drop it locally, not a conflict
 *  - 401            the session died; keep the queue, ask the user to sign in
 *  - 403 CSRF       refresh the token and retry once
 *  - other 4xx      the server refused it; park it so the user can see why
 *  - 5xx / network  transient; keep the mutation and try again later
 */
@Singleton
class SyncEngine @Inject constructor(
    private val api: EfinaApi,
    private val client: OkHttpClient,
    private val db: EfinaDatabase,
    private val tokenManager: TokenManager,
    private val baseUrl: String,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    data class Outcome(
        val flushed: Int = 0,
        val conflicts: Int = 0,
        val pulled: Int = 0,
        val needsReauth: Boolean = false,
        val retryLater: Boolean = false,
        val error: String? = null,
    )

    suspend fun sync(): Outcome = withContext(Dispatchers.IO) {
        if (!tokenManager.isLoggedIn()) {
            return@withContext Outcome(needsReauth = true)
        }

        var flushed = 0
        var conflicts = 0

        // ---- 1. flush ------------------------------------------------------
        for (mutation in db.pendingMutationDao().all()) {
            when (val result = replay(mutation)) {
                is ReplayResult.Done -> {
                    flushed++
                    db.pendingMutationDao().delete(mutation.id)
                }
                is ReplayResult.Conflict -> {
                    conflicts++
                    parkConflict(mutation, result.serverBody)
                    db.pendingMutationDao().delete(mutation.id)
                }
                is ReplayResult.Gone -> {
                    // The row was deleted server-side. Not a conflict: the user's
                    // intent (edit or delete it) is already satisfied.
                    removeLocal(mutation)
                    db.pendingMutationDao().delete(mutation.id)
                }
                is ReplayResult.NeedsReauth -> {
                    markNeedsReauth()
                    return@withContext Outcome(
                        flushed = flushed,
                        conflicts = conflicts,
                        needsReauth = true,
                    )
                }
                is ReplayResult.Retry -> {
                    db.pendingMutationDao().recordFailure(mutation.id, result.reason)
                    return@withContext Outcome(
                        flushed = flushed,
                        conflicts = conflicts,
                        retryLater = true,
                        error = result.reason,
                    )
                }
            }
        }

        // ---- 2. pull -------------------------------------------------------
        val pulled = try {
            pull()
        } catch (e: Exception) {
            return@withContext Outcome(
                flushed = flushed,
                conflicts = conflicts,
                retryLater = true,
                error = e.message,
            )
        }

        Outcome(flushed = flushed, conflicts = conflicts, pulled = pulled)
    }

    // -----------------------------------------------------------------------
    // Flush
    // -----------------------------------------------------------------------

    private sealed interface ReplayResult {
        data class Done(val body: String) : ReplayResult
        data class Conflict(val serverBody: String) : ReplayResult
        data object Gone : ReplayResult
        data object NeedsReauth : ReplayResult
        data class Retry(val reason: String) : ReplayResult
    }

    private suspend fun replay(mutation: PendingMutationEntity, isRetry: Boolean = false): ReplayResult {
        val url = baseUrl.trimEnd('/') + "/" + mutation.path.trimStart('/')
        val body = if (mutation.method == "DELETE") {
            "".toRequestBody("application/json".toMediaType())
        } else {
            mutation.payloadJson.toRequestBody("application/json".toMediaType())
        }

        val builder = Request.Builder()
            .url(url)
            .method(mutation.method, body)
            .header("Content-Type", "application/json")
            // Generated once when the mutation was queued and reused on every
            // retry: that is what stops a timed-out create from duplicating.
            .header("Idempotency-Key", mutation.idempotencyKey)

        tokenManager.getToken()?.let { builder.header("Authorization", "Bearer $it") }
        tokenManager.getCsrfToken()?.let { builder.header("X-CSRF-Token", it) }
        mutation.baseSyncSeq?.let { builder.header("X-Base-Sync-Seq", it.toString()) }

        val response = try {
            client.newCall(builder.build()).execute()
        } catch (e: Exception) {
            return ReplayResult.Retry(e.message ?: "network error")
        }

        response.use { res ->
            val text = res.body?.string().orEmpty()
            return when {
                res.isSuccessful -> {
                    adoptServerRow(mutation, text)
                    ReplayResult.Done(text)
                }
                res.code == 409 -> ReplayResult.Conflict(text)
                res.code == 404 && mutation.method != "POST" -> ReplayResult.Gone
                res.code == 401 -> ReplayResult.NeedsReauth
                res.code == 403 && text.contains("CSRF", ignoreCase = true) && !isRetry -> {
                    // The CSRF JWT expires in 24h while the session lasts 7 days,
                    // so a queue flushed after a day would 403 on every write.
                    val refreshed = api.refreshCsrfToken()
                    if (refreshed.isSuccessful) {
                        refreshed.body()?.let { tokenManager.saveCsrfToken(it.csrfToken) }
                        replay(mutation, isRetry = true)
                    } else {
                        ReplayResult.NeedsReauth
                    }
                }
                res.code in 500..599 -> ReplayResult.Retry("server error ${res.code}")
                else -> ReplayResult.Conflict(text)
            }
        }
    }

    /**
     * The write landed. Take the server's version of the row so the local copy
     * carries the real id and sync_seq — without this, the next edit would send
     * a stale base and manufacture a conflict.
     */
    private suspend fun adoptServerRow(mutation: PendingMutationEntity, body: String) {
        val serverId = extractId(body)
        if (serverId != null && mutation.localId < 0) {
            db.pendingMutationDao().setServerId(mutation.localId, serverId)
        }
        markSynced(mutation, serverId)
    }

    private fun extractId(body: String): Long? = runCatching {
        val element = json.parseToJsonElement(body)
        val obj = element as? kotlinx.serialization.json.JsonObject ?: return null
        // POST /api/transactions wraps the row in { transaction: {...} }.
        val inner = (obj["transaction"] as? kotlinx.serialization.json.JsonObject) ?: obj
        (inner["id"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toLongOrNull()
    }.getOrNull()

    private suspend fun markSynced(mutation: PendingMutationEntity, serverId: Long?) {
        val id = serverId ?: mutation.serverId ?: return
        when (mutation.entity) {
            "accounts" -> db.accountDao().byId(mutation.localId)?.let {
                db.accountDao().upsert(it.copy(id = id, pendingState = PendingState.SYNCED))
                if (id != mutation.localId) db.accountDao().deleteById(mutation.localId)
            }
            "transactions" -> db.transactionDao().byId(mutation.localId)?.let {
                db.transactionDao().upsert(it.copy(id = id, pendingState = PendingState.SYNCED))
                if (id != mutation.localId) db.transactionDao().deleteById(mutation.localId)
            }
            "categories" -> db.categoryDao().byId(mutation.localId)?.let {
                db.categoryDao().upsert(it.copy(id = id, pendingState = PendingState.SYNCED))
                if (id != mutation.localId) db.categoryDao().deleteById(mutation.localId)
            }
            "budgets" -> db.budgetDao().byId(mutation.localId)?.let {
                db.budgetDao().upsert(it.copy(id = id, pendingState = PendingState.SYNCED))
                if (id != mutation.localId) db.budgetDao().deleteById(mutation.localId)
            }
            "goals" -> db.goalDao().byId(mutation.localId)?.let {
                db.goalDao().upsert(it.copy(id = id, pendingState = PendingState.SYNCED))
                if (id != mutation.localId) db.goalDao().deleteById(mutation.localId)
            }
            "debts" -> db.debtDao().byId(mutation.localId)?.let {
                db.debtDao().upsert(it.copy(id = id, pendingState = PendingState.SYNCED))
                if (id != mutation.localId) db.debtDao().deleteById(mutation.localId)
            }
            "scheduled" -> db.scheduledDao().byId(mutation.localId)?.let {
                db.scheduledDao().upsert(it.copy(id = id, pendingState = PendingState.SYNCED))
                if (id != mutation.localId) db.scheduledDao().deleteById(mutation.localId)
            }
        }
    }

    private suspend fun removeLocal(mutation: PendingMutationEntity) {
        when (mutation.entity) {
            "accounts" -> db.accountDao().deleteById(mutation.localId)
            "transactions" -> db.transactionDao().deleteById(mutation.localId)
            "categories" -> db.categoryDao().deleteById(mutation.localId)
            "budgets" -> db.budgetDao().deleteById(mutation.localId)
            "goals" -> db.goalDao().deleteById(mutation.localId)
            "debts" -> db.debtDao().deleteById(mutation.localId)
            "scheduled" -> db.scheduledDao().deleteById(mutation.localId)
        }
    }

    private suspend fun parkConflict(mutation: PendingMutationEntity, serverBody: String) {
        db.conflictDao().insert(
            ConflictEntity(
                entity = mutation.entity,
                serverId = mutation.serverId,
                localId = mutation.localId,
                localPayloadJson = mutation.payloadJson,
                serverPayloadJson = serverBody,
                detectedAt = System.currentTimeMillis(),
            )
        )
    }

    private suspend fun markNeedsReauth() {
        val state = db.syncStateDao().get() ?: SyncStateEntity()
        db.syncStateDao().put(state.copy(needsReauth = true))
    }

    // -----------------------------------------------------------------------
    // Pull
    // -----------------------------------------------------------------------

    private suspend fun pull(): Int {
        var state = db.syncStateDao().get() ?: SyncStateEntity()
        var applied = 0

        // A visibility change means rows the client could not see before are now
        // visible (or the reverse). sync_seq cannot express that, so the only
        // correct response is to drop the mirror and resync from zero.
        val first = api.sync(state.cursor)
        if (!first.isSuccessful) {
            if (first.code() == 401) throw SessionExpired()
            throw IllegalStateException("sync failed (${first.code()})")
        }
        var page = first.body() ?: return 0

        if (page.visibilityVersion != state.visibilityVersion) {
            wipeMirror()
            state = state.copy(cursor = 0, visibilityVersion = page.visibilityVersion)
            db.syncStateDao().put(state)
            page = api.sync(0).body() ?: return 0
        }

        while (true) {
            applied += applyPage(page)
            state = state.copy(
                cursor = page.cursor,
                visibilityVersion = page.visibilityVersion,
                lastSyncedAt = System.currentTimeMillis(),
                needsReauth = false,
            )
            db.syncStateDao().put(state)
            if (!page.hasMore) break
            val next = api.sync(page.cursor)
            if (!next.isSuccessful) break
            page = next.body() ?: break
        }
        return applied
    }

    private suspend fun applyPage(page: SyncResponseDto): Int {
        var n = 0
        for (dto in page.accounts) { db.accountDao().upsertFromServer(dto.toEntity()); n++ }
        for (dto in page.categories) { db.categoryDao().upsertFromServer(dto.toEntity()); n++ }
        for (dto in page.transactions) { db.transactionDao().upsertFromServer(dto.toEntity()); n++ }
        for (dto in page.budgets) { db.budgetDao().upsertFromServer(dto.toEntity()); n++ }
        for (dto in page.goals) { db.goalDao().upsertFromServer(dto.toEntity()); n++ }
        for (dto in page.debts) { db.debtDao().upsertFromServer(dto.toEntity()); n++ }
        for (dto in page.scheduled) { db.scheduledDao().upsertFromServer(dto.toEntity()); n++ }

        for (t in page.deleted) {
            when (t.table) {
                "accounts" -> {
                    // The server cascades an account's transactions away, and the
                    // tombstones for them arrive in the same page.
                    db.accountDao().deleteById(t.id)
                }
                "categories" -> db.categoryDao().deleteById(t.id)
                "transactions" -> db.transactionDao().deleteById(t.id)
                "budgets" -> db.budgetDao().deleteById(t.id)
                "goals" -> db.goalDao().deleteById(t.id)
                "debts" -> db.debtDao().deleteById(t.id)
                "scheduled_transactions" -> db.scheduledDao().deleteById(t.id)
            }
            n++
        }
        return n
    }

    private suspend fun wipeMirror() {
        db.accountDao().clear()
        db.categoryDao().clear()
        db.transactionDao().clear()
        db.budgetDao().clear()
        db.goalDao().clear()
        db.debtDao().clear()
        db.scheduledDao().clear()
    }

    class SessionExpired : Exception("session expired")
}
