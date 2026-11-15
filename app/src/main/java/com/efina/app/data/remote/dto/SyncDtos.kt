package com.efina.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GET /api/sync — the delta.
 *
 * Every table is returned as a flat list of rows in the same shape the list
 * endpoints use, so the mappers are shared. `deleted` carries the tombstones:
 * a hard delete leaves no row behind, so this is the only way a client learns a
 * row is gone.
 */
@Serializable
data class SyncResponseDto(
    val cursor: Long = 0,
    val visibilityVersion: Long = 0,
    val hasMore: Boolean = false,
    val accounts: List<AccountDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val transactions: List<TransactionDto> = emptyList(),
    val budgets: List<BudgetDto> = emptyList(),
    val goals: List<GoalDto> = emptyList(),
    val debts: List<DebtDto> = emptyList(),
    val scheduled: List<ScheduledDto> = emptyList(),
    val deleted: List<TombstoneDto> = emptyList(),
)

@Serializable
data class TombstoneDto(
    val table: String,
    val id: Long,
    val syncSeq: Long = 0,
)

/**
 * The 409 body when a queued edit was based on a version the server has moved
 * past. `current` is the row as it stands now, which the Conflicts screen shows
 * beside the user's version.
 */
@Serializable
data class ConflictResponseDto(
    val error: String? = null,
    val current: kotlinx.serialization.json.JsonElement? = null,
)
