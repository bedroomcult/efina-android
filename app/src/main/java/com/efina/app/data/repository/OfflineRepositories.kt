package com.efina.app.data.repository

import com.efina.app.data.local.EfinaDatabase
import com.efina.app.data.local.PendingState
import com.efina.app.data.local.toDomain as toDomainEntity
import com.efina.app.data.remote.dto.toDomain
import com.efina.app.data.local.toEntity
import com.efina.app.data.remote.api.EfinaApi
import com.efina.app.data.remote.dto.toDto
import com.efina.app.data.remote.dto.CreateAccountRequestDto
import com.efina.app.data.remote.dto.CreateTransactionRequestDto
import com.efina.app.data.remote.dto.UpdateAccountRequestDto
import com.efina.app.data.remote.dto.UpdateTransactionRequestDto
import com.efina.app.data.sync.MutationQueue
import com.efina.app.domain.model.Account
import com.efina.app.domain.model.CreateAccountRequest
import com.efina.app.domain.model.CreateTransactionRequest
import com.efina.app.domain.model.Transaction
import com.efina.app.domain.model.TransactionFilter
import com.efina.app.domain.model.UpdateAccountRequest
import com.efina.app.domain.model.UpdateTransactionRequest
import com.efina.app.domain.repository.AccountRepository
import com.efina.app.domain.repository.TransactionPage
import com.efina.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed repositories.
 *
 * Reads come from the local mirror, so a screen never waits on the network and
 * never shows stale data after a write. Writes go to the mirror first and are
 * queued for the server, which is what makes them work offline.
 *
 * The network is still used for the things that genuinely need it: sharing,
 * members, and the one-shot refresh that feeds the mirror.
 */

@Singleton
class OfflineAccountRepository @Inject constructor(
    private val api: EfinaApi,
    private val db: EfinaDatabase,
    private val queue: MutationQueue,
) : AccountRepository {

    private val json = Json { encodeDefaults = true }

    override fun observeAccounts(): Flow<List<Account>> =
        db.accountDao().observeAll().map { rows -> rows.map { it.toDomainEntity() } }

    /**
     * Refresh the mirror from the server.
     *
     * Kept for callers that want a one-shot read; the screens observe
     * [observeAccounts] instead. A failure here is not fatal — the mirror still
     * has the last known state.
     */
    override suspend fun getAccounts(): Result<List<Account>> =
        safeCall { api.getAccounts() }
            .onSuccess { rows ->
                for (dto in rows) db.accountDao().upsertFromServer(dto.toEntity())
            }
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun createAccount(request: CreateAccountRequest): Result<Account> {
        val localId = queue.newLocalId()
        val entity = com.efina.app.data.local.AccountEntity(
            id = localId,
            userId = 0,
            name = request.name,
            type = request.type.value,
            balance = request.balance,
            currency = request.currency,
            description = request.description,
            color = request.color,
            displayOrder = 0,
            disabled = false,
            initialBalance = request.balance,
            createdAt = null,
            isShared = request.familyId != null,
            familyId = request.familyId,
            role = null,
            memberCount = 0,
            ownerUserId = 0,
            pendingState = PendingState.PENDING_CREATE,
        )
        db.accountDao().upsert(entity)

        queue.enqueue(
            entity = "accounts",
            op = "create",
            method = "POST",
            path = "accounts",
            localId = localId,
            serverId = null,
            payloadJson = json.encodeToString(
                CreateAccountRequestDto.serializer(),
                request.toDto()
            ),
            baseSyncSeq = null,
        )
        return Result.success(entity.toDomainEntity())
    }

    override suspend fun updateAccount(id: Long, request: UpdateAccountRequest): Result<Account> {
        val existing = db.accountDao().byId(id)
            ?: return Result.failure(IllegalStateException("Account not found"))

        val updated = existing.copy(
            name = request.name,
            type = request.type.value,
            balance = request.balance,
            currency = request.currency,
            description = request.description,
            color = request.color,
            pendingState = PendingState.PENDING_UPDATE,
        )
        db.accountDao().upsert(updated)

        queue.enqueue(
            entity = "accounts",
            op = "update",
            method = "PUT",
            path = "accounts/$id",
            localId = id,
            serverId = id.takeIf { it > 0 },
            payloadJson = json.encodeToString(
                UpdateAccountRequestDto.serializer(),
                request.toDto()
            ),
            baseSyncSeq = existing.syncSeq,
        )
        return Result.success(updated.toDomainEntity())
    }

    override suspend fun deleteAccount(id: Long): Result<Unit> {
        val existing = db.accountDao().byId(id)
            ?: return Result.failure(IllegalStateException("Account not found"))

        // A row that never reached the server is simply dropped: there is
        // nothing to delete remotely, and queueing a DELETE for a negative id
        // would 404 forever.
        if (existing.pendingState == PendingState.PENDING_CREATE) {
            db.accountDao().deleteById(id)
            return Result.success(Unit)
        }

        db.accountDao().upsert(existing.copy(pendingState = PendingState.PENDING_DELETE))
        queue.enqueue(
            entity = "accounts",
            op = "delete",
            method = "DELETE",
            path = "accounts/$id",
            localId = id,
            serverId = id,
            payloadJson = "{}",
            baseSyncSeq = existing.syncSeq,
        )
        return Result.success(Unit)
    }

    // Sharing changes who can see the data, which is exactly the thing that must
    // not be guessed offline. These stay online-only and fail with a clear
    // message rather than queueing.
    override suspend fun setShared(id: Long, familyId: Long?): Result<Account> =
        safeCall { api.updateAccountSharing(id, com.efina.app.data.remote.dto.UpdateAccountSharingRequestDto(familyId)) }
            .fold(
                onSuccess = { safeCall { api.getAccount(id) }.map { it.toDomain() } },
                onFailure = { Result.failure(it) },
            )

    override suspend fun getMembers(id: Long) = safeCall { api.getAccountMembers(id) }.map { dto ->
        com.efina.app.domain.repository.AccountMembers(
            role = com.efina.app.domain.model.AccountShareRole.from(dto.account_role),
            owner = dto.owner?.let {
                com.efina.app.domain.model.FamilyMember(it.user_id, "admin", it.name ?: "", it.email, null)
            },
            members = dto.members.map { it.toDomain() },
        )
    }

    override suspend fun shareWith(
        id: Long,
        userId: Long,
        role: com.efina.app.domain.model.AccountShareRole,
    ): Result<Unit> = safeCall {
        api.shareAccount(id, com.efina.app.data.remote.dto.ShareAccountRequestDto(userId, role.value))
    }.map { }

    override suspend fun removeMember(id: Long, userId: Long): Result<Unit> = safeCall {
        api.unshareAccount(id, com.efina.app.data.remote.dto.RemoveMemberRequestDto(userId))
    }.map { }
}

@Singleton
class OfflineTransactionRepository @Inject constructor(
    private val api: EfinaApi,
    private val db: EfinaDatabase,
    private val queue: MutationQueue,
) : TransactionRepository {

    private val json = Json { encodeDefaults = true }

    override fun observeTransactions(): Flow<List<Transaction>> =
        db.transactionDao().observeAll().map { rows -> rows.map { it.toDomainEntity() } }

    override fun pagedTransactions(
        filter: TransactionFilter,
    ): Flow<androidx.paging.PagingData<Transaction>> =
        androidx.paging.Pager(
            config = androidx.paging.PagingConfig(pageSize = 50, enablePlaceholders = false),
            pagingSourceFactory = {
                db.transactionDao().pagingSource(
                    type = filter.type?.value,
                    accountId = filter.accountId,
                    categoryId = filter.categoryId,
                    startDate = filter.startDate,
                    endDate = filter.endDate,
                    search = filter.search?.takeIf { it.isNotBlank() },
                    familyOnly = filter.familyOnly,
                )
            },
        ).flow.map { paging -> paging.map { it.toDomainEntity() } }

    override suspend fun getTransactions(filter: TransactionFilter): Result<TransactionPage> =
        safeCall {
            api.getTransactions(
                limit = filter.limit,
                offset = filter.offset,
                type = filter.type?.value,
                accountId = filter.accountId,
                categoryId = filter.categoryId,
                startDate = filter.startDate,
                endDate = filter.endDate,
                search = filter.search?.takeIf { it.isNotBlank() },
                includeSubcategories = filter.includeSubcategories,
                familyOnly = filter.familyOnly.takeIf { it },
            )
        }.onSuccess { page ->
            for (dto in page.transactions) db.transactionDao().upsertFromServer(dto.toEntity())
        }.map { page ->
            TransactionPage(
                transactions = page.transactions.map { it.toDomain() },
                total = page.total,
                limit = page.limit,
                offset = page.offset,
            )
        }

    override suspend fun createTransaction(request: CreateTransactionRequest): Result<Transaction> {
        val localId = queue.newLocalId()
        val account = db.accountDao().byId(request.accountId)

        val entity = com.efina.app.data.local.TransactionEntity(
            id = localId,
            userId = 0,
            accountId = request.accountId,
            accountName = account?.name,
            accountRole = account?.role,
            currency = account?.currency ?: "IDR",
            categoryId = request.categoryId,
            categoryName = db.categoryDao().byId(request.categoryId ?: -1)?.name,
            categoryColor = db.categoryDao().byId(request.categoryId ?: -1)?.color,
            categoryIcon = db.categoryDao().byId(request.categoryId ?: -1)?.icon,
            toAccountId = null,
            toAccountName = null,
            amount = request.amount,
            type = request.type.value,
            description = request.description,
            date = request.date,
            mood = request.mood,
            transferPairId = null,
            familyId = account?.familyId,
            creatorUserId = null,
            creatorName = null,
            createdAt = null,
            pendingState = PendingState.PENDING_CREATE,
        )
        db.transactionDao().upsert(entity)

        // The balance moves locally too, so the account screen is right before
        // the server answers. The pull corrects it if the server disagrees.
        account?.let {
            val delta = if (request.type.value == "income") request.amount else -request.amount
            db.accountDao().upsert(it.copy(balance = it.balance + delta))
        }

        queue.enqueue(
            entity = "transactions",
            op = "create",
            method = "POST",
            path = "transactions",
            localId = localId,
            serverId = null,
            payloadJson = json.encodeToString(
                CreateTransactionRequestDto.serializer(),
                request.toDto(),
            ),
            baseSyncSeq = null,
        )
        return Result.success(entity.toDomainEntity())
    }

    override suspend fun updateTransaction(
        id: Long,
        request: UpdateTransactionRequest,
    ): Result<Transaction> {
        val existing = db.transactionDao().byId(id)
            ?: return Result.failure(IllegalStateException("Transaction not found"))

        val updated = existing.copy(
            accountId = request.accountId,
            categoryId = request.categoryId,
            amount = request.amount,
            type = request.type.value,
            description = request.description,
            date = request.date,
            mood = request.mood,
            pendingState = PendingState.PENDING_UPDATE,
        )
        db.transactionDao().upsert(updated)

        queue.enqueue(
            entity = "transactions",
            op = "update",
            method = "PUT",
            path = "transactions/$id",
            localId = id,
            serverId = id.takeIf { it > 0 },
            payloadJson = json.encodeToString(
                UpdateTransactionRequestDto.serializer(),
                request.toDto(),
            ),
            baseSyncSeq = existing.syncSeq,
        )
        return Result.success(updated.toDomainEntity())
    }

    override suspend fun deleteTransaction(id: Long): Result<Unit> {
        val existing = db.transactionDao().byId(id)
            ?: return Result.failure(IllegalStateException("Transaction not found"))

        if (existing.pendingState == PendingState.PENDING_CREATE) {
            db.transactionDao().deleteById(id)
            return Result.success(Unit)
        }

        db.transactionDao().upsert(existing.copy(pendingState = PendingState.PENDING_DELETE))
        queue.enqueue(
            entity = "transactions",
            op = "delete",
            method = "DELETE",
            path = "transactions/$id",
            localId = id,
            serverId = id,
            payloadJson = "{}",
            baseSyncSeq = existing.syncSeq,
        )
        return Result.success(Unit)
    }

    /**
     * A transfer is one server call that moves two balances atomically
     * (`DB.batch` in functions/api/transfers/index.js), so it is queued as one
     * mutation rather than two. Splitting it would let half a transfer land.
     */
    override suspend fun createTransfer(
        request: com.efina.app.domain.model.CreateTransferRequest,
    ): Result<Unit> {
        queue.enqueue(
            entity = "transactions",
            op = "transfer",
            method = "POST",
            path = "transfers",
            localId = queue.newLocalId(),
            serverId = null,
            payloadJson = json.encodeToString(
                com.efina.app.data.remote.dto.CreateTransferRequestDto.serializer(),
                com.efina.app.data.remote.dto.CreateTransferRequestDto(
                    from_account_id = request.fromAccountId,
                    to_account_id = request.toAccountId,
                    amount = request.amount,
                    date = request.date,
                    description = request.description,
                ),
            ),
            baseSyncSeq = null,
        )
        return Result.success(Unit)
    }
}
