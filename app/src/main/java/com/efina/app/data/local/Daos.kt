package com.efina.app.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Reads are Flows so a screen redraws the moment sync writes a row — there is no
 * cache to invalidate and no reload-on-return.
 *
 * `upsertFromServer` is guarded on `pendingState = SYNCED`: a row with an unsent
 * edit must not be overwritten by a pull, or the user's change disappears under
 * them mid-edit.
 */
@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE disabled = 0 ORDER BY displayOrder ASC, id DESC")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun byId(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE id = :id")
    fun observeById(id: Long): Flow<AccountEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: AccountEntity)

    @Transaction
    suspend fun upsertFromServer(row: AccountEntity) {
        val existing = byId(row.id)
        if (existing == null || existing.pendingState == PendingState.SYNCED) {
            upsert(row.copy(pendingState = PendingState.SYNCED))
        } else {
            // Keep the local edit, but take the server's version number so the
            // queued write conflicts rather than silently overwriting.
            upsert(existing.copy(syncSeq = row.syncSeq))
        }
    }

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM accounts")
    suspend fun clear()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun byId(id: Long): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: CategoryEntity)

    @Transaction
    suspend fun upsertFromServer(row: CategoryEntity) {
        val existing = byId(row.id)
        if (existing == null || existing.pendingState == PendingState.SYNCED) {
            upsert(row.copy(pendingState = PendingState.SYNCED))
        } else {
            upsert(existing.copy(syncSeq = row.syncSeq))
        }
    }

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM categories")
    suspend fun clear()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun byId(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun observeById(id: Long): Flow<TransactionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: TransactionEntity)

    @Transaction
    suspend fun upsertFromServer(row: TransactionEntity) {
        val existing = byId(row.id)
        if (existing == null || existing.pendingState == PendingState.SYNCED) {
            upsert(row.copy(pendingState = PendingState.SYNCED))
        } else {
            upsert(existing.copy(syncSeq = row.syncSeq))
        }
    }

    /**
     * Paged, filtered list. Every filter is a Room query rather than a network
     * call, so the list keeps working offline and scrolling never waits.
     *
     * `search` is matched against description and category name, the same two
     * fields the server's LIKE covers.
     */
    @Query(
        """
        SELECT * FROM transactions
        WHERE (:type IS NULL OR type = :type)
          AND (:accountId IS NULL OR accountId = :accountId OR toAccountId = :accountId)
          AND (:categoryId IS NULL OR categoryId = :categoryId)
          AND (:startDate IS NULL OR date >= :startDate)
          AND (:endDate IS NULL OR date <= :endDate)
          AND (:search IS NULL OR description LIKE '%' || :search || '%'
               OR categoryName LIKE '%' || :search || '%')
          AND (:familyOnly = 0 OR familyId IS NOT NULL)
        ORDER BY date DESC, id DESC
        """
    )
    fun pagingSource(
        type: String?,
        accountId: Long?,
        categoryId: Long?,
        startDate: String?,
        endDate: String?,
        search: String?,
        familyOnly: Boolean,
    ): PagingSource<Int, TransactionEntity>

    @Query("SELECT COUNT(*) FROM transactions")
    fun observeCount(): Flow<Int>

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transactions WHERE accountId = :accountId OR toAccountId = :accountId")
    suspend fun deleteByAccount(accountId: Long)

    @Query("DELETE FROM transactions")
    suspend fun clear()
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets ORDER BY id DESC")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun byId(id: Long): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: BudgetEntity)

    @Transaction
    suspend fun upsertFromServer(row: BudgetEntity) {
        val existing = byId(row.id)
        if (existing == null || existing.pendingState == PendingState.SYNCED) {
            upsert(row.copy(pendingState = PendingState.SYNCED))
        } else {
            upsert(existing.copy(syncSeq = row.syncSeq))
        }
    }

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM budgets")
    suspend fun clear()
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY id DESC")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun byId(id: Long): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: GoalEntity)

    @Transaction
    suspend fun upsertFromServer(row: GoalEntity) {
        val existing = byId(row.id)
        if (existing == null || existing.pendingState == PendingState.SYNCED) {
            upsert(row.copy(pendingState = PendingState.SYNCED))
        } else {
            upsert(existing.copy(syncSeq = row.syncSeq))
        }
    }

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM goals")
    suspend fun clear()
}

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts ORDER BY id DESC")
    fun observeAll(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE id = :id")
    suspend fun byId(id: Long): DebtEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: DebtEntity)

    @Transaction
    suspend fun upsertFromServer(row: DebtEntity) {
        val existing = byId(row.id)
        if (existing == null || existing.pendingState == PendingState.SYNCED) {
            upsert(row.copy(pendingState = PendingState.SYNCED))
        } else {
            upsert(existing.copy(syncSeq = row.syncSeq))
        }
    }

    @Query("DELETE FROM debts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM debts")
    suspend fun clear()
}

@Dao
interface ScheduledDao {
    @Query("SELECT * FROM scheduled ORDER BY nextDate ASC")
    fun observeAll(): Flow<List<ScheduledEntity>>

    @Query("SELECT * FROM scheduled WHERE id = :id")
    suspend fun byId(id: Long): ScheduledEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: ScheduledEntity)

    @Transaction
    suspend fun upsertFromServer(row: ScheduledEntity) {
        val existing = byId(row.id)
        if (existing == null || existing.pendingState == PendingState.SYNCED) {
            upsert(row.copy(pendingState = PendingState.SYNCED))
        } else {
            upsert(existing.copy(syncSeq = row.syncSeq))
        }
    }

    @Query("DELETE FROM scheduled WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM scheduled")
    suspend fun clear()
}

@Dao
interface PendingMutationDao {
    /** Oldest first: writes must replay in the order the user made them. */
    @Query("SELECT * FROM pending_mutations ORDER BY createdAt ASC")
    suspend fun all(): List<PendingMutationEntity>

    @Query("SELECT * FROM pending_mutations ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<PendingMutationEntity>>

    @Query("SELECT COUNT(*) FROM pending_mutations")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_mutations")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(row: PendingMutationEntity)

    @Query("DELETE FROM pending_mutations WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE pending_mutations SET attempts = attempts + 1, lastError = :error WHERE id = :id")
    suspend fun recordFailure(id: String, error: String)

    /** A created row's real server id, so later edits and deletes can find it. */
    @Query("UPDATE pending_mutations SET serverId = :serverId WHERE localId = :localId")
    suspend fun setServerId(localId: Long, serverId: Long)

    @Query("DELETE FROM pending_mutations")
    suspend fun clear()
}

@Dao
interface ConflictDao {
    @Query("SELECT * FROM conflicts ORDER BY detectedAt DESC")
    fun observeAll(): Flow<List<ConflictEntity>>

    @Query("SELECT COUNT(*) FROM conflicts")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM conflicts WHERE id = :id")
    suspend fun byId(id: Long): ConflictEntity?

    @Insert
    suspend fun insert(row: ConflictEntity): Long

    @Query("DELETE FROM conflicts WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM conflicts")
    suspend fun clear()
}

@Dao
interface SyncStateDao {
    @Query("SELECT * FROM sync_state WHERE id = 1")
    suspend fun get(): SyncStateEntity?

    @Query("SELECT * FROM sync_state WHERE id = 1")
    fun observe(): Flow<SyncStateEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(row: SyncStateEntity)
}
