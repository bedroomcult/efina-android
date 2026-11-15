package com.efina.app.ui.conflicts

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efina.app.data.local.ConflictEntity
import com.efina.app.data.local.EfinaDatabase
import com.efina.app.data.sync.MutationQueue
import com.efina.app.data.sync.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import javax.inject.Inject

/**
 * A parked conflict, with the two payloads already rendered for display.
 *
 * [serverSyncSeq] is the version the row is at now. "Keep mine" must re-queue
 * against **that**, not the version the edit was originally based on, or the
 * server rejects it again immediately and the user is stuck in a loop.
 */
data class ConflictRow(
    val id: Long,
    val entity: String,
    val title: String,
    val localPayload: String,
    val serverPayload: String,
    val serverSyncSeq: Long?,
)

data class ConflictsUiState(
    val loading: Boolean = true,
    val conflicts: List<ConflictRow> = emptyList(),
    val busy: Boolean = false,
    val message: String? = null,
) {
    val isEmpty: Boolean get() = !loading && conflicts.isEmpty()
}

@HiltViewModel
class ConflictsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: EfinaDatabase,
    private val queue: MutationQueue,
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    val state: StateFlow<ConflictsUiState> = combine(
        db.conflictDao().observeAll(),
        busy,
        message,
    ) { rows, isBusy, msg ->
        ConflictsUiState(
            loading = false,
            conflicts = rows.map(::toRow),
            busy = isBusy,
            message = msg,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ConflictsUiState(),
    )

    private fun toRow(entity: ConflictEntity): ConflictRow {
        val serverSeq = runCatching {
            val body = json.parseToJsonElement(entity.serverPayloadJson).jsonObject
            val current = body["current"] as? JsonObject ?: return@runCatching null
            (current["sync_seq"] as? JsonPrimitive)?.content?.toLongOrNull()
        }.getOrNull()

        return ConflictRow(
            id = entity.id,
            entity = entity.entity,
            title = titleFor(entity),
            localPayload = pretty(entity.localPayloadJson),
            serverPayload = pretty(entity.serverPayloadJson),
            serverSyncSeq = serverSeq,
        )
    }

    /**
     * Re-queue the user's version against the server's current version.
     *
     * The payload was captured when the edit was made, so it is replayed
     * verbatim; only the base version changes.
     */
    fun keepMine(row: ConflictRow) {
        viewModelScope.launch {
            busy.value = true
            try {
                val conflict = db.conflictDao().byId(row.id) ?: return@launch
                val serverId = conflict.serverId
                if (serverId == null) {
                    // A create that lost a race has no server row to update.
                    message.value = "This change cannot be re-applied automatically"
                    db.conflictDao().delete(row.id)
                    return@launch
                }

                queue.enqueue(
                    entity = conflict.entity,
                    op = "conflictResolve",
                    method = "PUT",
                    path = pathFor(conflict.entity, serverId),
                    localId = conflict.localId,
                    serverId = serverId,
                    payloadJson = conflict.localPayloadJson,
                    baseSyncSeq = row.serverSyncSeq,
                )
                db.conflictDao().delete(row.id)
                message.value = "Kept your version"
                SyncWorker.enqueue(context)
            } finally {
                busy.value = false
            }
        }
    }

    /** Discard the user's version. The mirror already holds the server's. */
    fun keepServer(row: ConflictRow) {
        viewModelScope.launch {
            busy.value = true
            try {
                db.conflictDao().delete(row.id)
                message.value = "Kept the server version"
                SyncWorker.enqueue(context)
            } finally {
                busy.value = false
            }
        }
    }

    fun clearMessage() {
        message.value = null
    }

    private fun pretty(payload: String): String =
        runCatching { json.encodeToString(JsonObject.serializer(), json.parseToJsonElement(payload).jsonObject) }
            .getOrDefault(payload)

    private fun titleFor(entity: ConflictEntity): String = when (entity.entity) {
        "transactions" -> "Transaction"
        "accounts" -> "Account"
        "categories" -> "Category"
        "budgets" -> "Budget"
        "goals" -> "Goal"
        "debts" -> "Debt"
        "scheduled" -> "Scheduled transaction"
        else -> entity.entity.replaceFirstChar { it.uppercase() }
    }

    private fun pathFor(entity: String, serverId: Long): String = when (entity) {
        "scheduled" -> "scheduled/$serverId"
        else -> "$entity/$serverId"
    }
}
