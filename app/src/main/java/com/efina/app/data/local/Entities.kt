package com.efina.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The local mirror.
 *
 * Two things here are not in the domain models:
 *
 *  - `syncSeq` — the server's monotonic version for the row. It is the base a
 *    queued edit sends back as `X-Base-Sync-Seq`, so the server can tell whether
 *    someone else changed the row first.
 *  - `pendingState` — whether the row has a queued write behind it. A row in any
 *    state other than SYNCED is never overwritten by a delta pull, or the user's
 *    unsent edit would vanish under them.
 *
 * Rows created offline get a negative id. The server assigns the real one on
 * sync, and the mutation queue rewrites it. Negative ids keep the domain models
 * unchanged (they all use `Long`).
 *
 * The joined display fields (account name, category name, creator name, role)
 * are denormalized onto the transaction row rather than modelled as relations:
 * the list screen renders them on every row, and a join per page would cost more
 * than the occasional staleness after a rename.
 */

enum class PendingState {
    SYNCED,
    PENDING_CREATE,
    PENDING_UPDATE,
    PENDING_DELETE,
}

@Entity(tableName = "accounts", indices = [Index("userId"), Index("syncSeq")])
data class AccountEntity(
    @PrimaryKey val id: Long,
    val userId: Long,
    val name: String,
    val type: String,
    val balance: Double,
    val currency: String,
    val description: String?,
    val color: String?,
    val displayOrder: Int,
    val disabled: Boolean,
    val initialBalance: Double,
    val createdAt: String?,
    val isShared: Boolean,
    val familyId: Long?,
    val role: String?,
    val memberCount: Int,
    val ownerUserId: Long,
    val syncSeq: Long = 0,
    val pendingState: PendingState = PendingState.SYNCED,
)

@Entity(tableName = "categories", indices = [Index("userId"), Index("syncSeq")])
data class CategoryEntity(
    @PrimaryKey val id: Long,
    val userId: Long,
    val name: String,
    val color: String,
    val icon: String?,
    val parentId: Long?,
    val parentName: String?,
    val isSystem: Boolean,
    val isShared: Boolean,
    val type: String?,
    val familyId: Long?,
    val syncSeq: Long = 0,
    val pendingState: PendingState = PendingState.SYNCED,
)

@Entity(tableName = "transactions", indices = [Index("userId"), Index("accountId"), Index("date"), Index("syncSeq")])
data class TransactionEntity(
    @PrimaryKey val id: Long,
    val userId: Long,
    val accountId: Long,
    val accountName: String?,
    val accountRole: String?,
    val currency: String,
    val categoryId: Long?,
    val categoryName: String?,
    val categoryColor: String?,
    val categoryIcon: String?,
    val toAccountId: Long?,
    val toAccountName: String?,
    val amount: Double,
    val type: String,
    val description: String?,
    val date: String,
    val mood: String?,
    val transferPairId: String?,
    val familyId: Long?,
    val creatorUserId: Long?,
    val creatorName: String?,
    val createdAt: String?,
    val syncSeq: Long = 0,
    val pendingState: PendingState = PendingState.SYNCED,
)

@Entity(tableName = "budgets", indices = [Index("userId"), Index("syncSeq")])
data class BudgetEntity(
    @PrimaryKey val id: Long,
    val userId: Long,
    val categoryId: Long,
    val categoryName: String?,
    val categoryColor: String?,
    val amount: Double,
    val spent: Double,
    val periodType: String,
    val periodStart: String?,
    val periodEnd: String?,
    val familyId: Long?,
    val isShared: Boolean,
    val syncSeq: Long = 0,
    val pendingState: PendingState = PendingState.SYNCED,
)

@Entity(tableName = "goals", indices = [Index("userId"), Index("syncSeq")])
data class GoalEntity(
    @PrimaryKey val id: Long,
    val userId: Long,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val targetDate: String?,
    val icon: String?,
    val linkedAccountId: Long?,
    val createdAt: String?,
    val syncSeq: Long = 0,
    val pendingState: PendingState = PendingState.SYNCED,
)

@Entity(tableName = "debts", indices = [Index("userId"), Index("syncSeq")])
data class DebtEntity(
    @PrimaryKey val id: Long,
    val userId: Long,
    val name: String,
    val amount: Double,
    val remainingAmount: Double,
    val dueDate: String?,
    val type: String?,
    val status: String?,
    val notes: String?,
    val createdAt: String?,
    val syncSeq: Long = 0,
    val pendingState: PendingState = PendingState.SYNCED,
)

@Entity(tableName = "scheduled", indices = [Index("userId"), Index("syncSeq")])
data class ScheduledEntity(
    @PrimaryKey val id: Long,
    val userId: Long,
    val accountId: Long?,
    val categoryId: Long?,
    val amount: Double,
    val type: String?,
    val description: String?,
    val frequency: String?,
    val nextDate: String?,
    val isActive: Boolean,
    val createdAt: String?,
    val syncSeq: Long = 0,
    val pendingState: PendingState = PendingState.SYNCED,
)

/**
 * A write waiting to reach the server.
 *
 * `idempotencyKey` is generated once, when the mutation is queued, and reused on
 * every retry. That is the whole point: a retry after a timeout must not create
 * a second transaction.
 *
 * `baseSyncSeq` is the version the edit was based on. The server compares it
 * with the row's current version and answers 409 when they differ, which is how
 * a lost update is caught instead of silently applied.
 */
@Entity(tableName = "pending_mutations", indices = [Index("createdAt")])
data class PendingMutationEntity(
    @PrimaryKey val id: String,
    val entity: String,
    val op: String,
    /** HTTP verb and path to replay, e.g. POST /api/transactions. */
    val method: String,
    val path: String,
    /** Negative id of the local row this mutation is about. */
    val localId: Long,
    /** Server id, once known. Null for a create that has not synced yet. */
    val serverId: Long?,
    val payloadJson: String,
    val baseSyncSeq: Long?,
    val idempotencyKey: String,
    val createdAt: Long,
    val attempts: Int = 0,
    val lastError: String? = null,
)

/**
 * A queued edit the server refused because the row had moved on.
 *
 * Both payloads are kept so the user can see what they tried to save against
 * what is actually there, and choose.
 */
@Entity(tableName = "conflicts")
data class ConflictEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entity: String,
    val serverId: Long?,
    val localId: Long,
    val localPayloadJson: String,
    val serverPayloadJson: String,
    val detectedAt: Long,
)

/** One row. The cursor and the visibility version the client last saw. */
@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val id: Int = 1,
    val cursor: Long = 0,
    val visibilityVersion: Long = 0,
    val lastSyncedAt: Long? = null,
    /** Set when the session died with a queue still waiting. */
    val needsReauth: Boolean = false,
)
