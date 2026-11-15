package com.efina.app.data.repository

import com.efina.app.data.local.BudgetEntity
import com.efina.app.data.local.CategoryEntity
import com.efina.app.data.local.DebtEntity
import com.efina.app.data.local.EfinaDatabase
import com.efina.app.data.local.GoalEntity
import com.efina.app.data.local.PendingState
import com.efina.app.data.local.ScheduledEntity
import com.efina.app.data.local.toDomain as toDomainEntity
import com.efina.app.data.remote.dto.toDomain
import com.efina.app.data.local.toEntity
import com.efina.app.data.remote.api.EfinaApi
import com.efina.app.data.remote.dto.toDto
import com.efina.app.data.sync.MutationQueue
import com.efina.app.domain.model.Budget
import com.efina.app.domain.model.Category
import com.efina.app.domain.model.CreateBudgetRequest
import com.efina.app.domain.model.CreateCategoryRequest
import com.efina.app.domain.model.CreateDebtRequest
import com.efina.app.domain.model.CreateGoalRequest
import com.efina.app.domain.model.CreateScheduledRequest
import com.efina.app.domain.model.Debt
import com.efina.app.domain.model.Goal
import com.efina.app.domain.model.PayDebtRequest
import com.efina.app.domain.model.ScheduledTransaction
import com.efina.app.domain.model.UpdateBudgetRequest
import com.efina.app.domain.model.UpdateCategoryRequest
import com.efina.app.domain.model.UpdateDebtRequest
import com.efina.app.domain.model.UpdateGoalRequest
import com.efina.app.domain.repository.BudgetRepository
import com.efina.app.domain.repository.CategoryRepository
import com.efina.app.domain.repository.DebtRepository
import com.efina.app.domain.repository.GoalRepository
import com.efina.app.domain.repository.ScheduledRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed repositories for the rest of the ledger.
 *
 * Same shape as [OfflineAccountRepository]: reads observe the mirror, writes
 * land locally and are queued. The network is only used to refresh the mirror
 * and for the operations that genuinely need a server (paying a debt creates a
 * transaction and moves a balance in one `DB.batch`, so it is queued as one
 * mutation rather than reimplemented here).
 */

private val offlineJson = Json { encodeDefaults = true }

@Singleton
class OfflineCategoryRepository @Inject constructor(
    private val api: EfinaApi,
    private val db: EfinaDatabase,
    private val queue: MutationQueue,
) : CategoryRepository {

    override fun observeCategories(): Flow<List<Category>> =
        db.categoryDao().observeAll().map { rows ->
            // The API returns roots with children nested; the mirror is flat, so
            // rebuild the one-level tree the picker expects.
            val byParent = rows.groupBy { it.parentId }
            (byParent[null] ?: emptyList()).map { root ->
                root.toDomain(
                    children = (byParent[root.id] ?: emptyList()).map { it.toDomainEntity() }
                )
            }
        }

    override suspend fun getCategories(): Result<List<Category>> =
        safeCall { api.getCategories() }
            .onSuccess { roots ->
                for (root in roots) {
                    db.categoryDao().upsertFromServer(root.toEntity())
                    for (child in root.subcategories) {
                        db.categoryDao().upsertFromServer(child.toEntity())
                    }
                }
            }
            .map { roots -> roots.map { it.toDomain() } }

    override suspend fun createCategory(request: CreateCategoryRequest): Result<Category> {
        val localId = queue.newLocalId()
        val entity = CategoryEntity(
            id = localId,
            userId = 0,
            name = request.name,
            color = request.color,
            icon = request.icon,
            parentId = request.parentId,
            parentName = null,
            isSystem = false,
            isShared = false,
            type = request.type.value,
            familyId = null,
            pendingState = PendingState.PENDING_CREATE,
        )
        db.categoryDao().upsert(entity)
        queue.enqueue(
            entity = "categories",
            op = "create",
            method = "POST",
            path = "categories",
            localId = localId,
            serverId = null,
            payloadJson = offlineJson.encodeToString(
                com.efina.app.data.remote.dto.CreateCategoryRequestDto.serializer(),
                request.toDto(),
            ),
            baseSyncSeq = null,
        )
        return Result.success(entity.toDomainEntity())
    }

    override suspend fun updateCategory(id: Long, request: UpdateCategoryRequest): Result<Category> {
        val existing = db.categoryDao().byId(id)
            ?: return Result.failure(IllegalStateException("Category not found"))
        val updated = existing.copy(
            name = request.name ?: existing.name,
            color = request.color ?: existing.color,
            icon = request.icon ?: existing.icon,
            parentId = request.parentId ?: existing.parentId,
            pendingState = PendingState.PENDING_UPDATE,
        )
        db.categoryDao().upsert(updated)
        queue.enqueue(
            entity = "categories",
            op = "update",
            method = "PUT",
            path = "categories/$id",
            localId = id,
            serverId = id.takeIf { it > 0 },
            payloadJson = offlineJson.encodeToString(
                com.efina.app.data.remote.dto.UpdateCategoryRequestDto.serializer(),
                request.toDto(),
            ),
            baseSyncSeq = existing.syncSeq,
        )
        return Result.success(updated.toDomainEntity())
    }

    override suspend fun deleteCategory(id: Long): Result<Unit> {
        val existing = db.categoryDao().byId(id)
            ?: return Result.failure(IllegalStateException("Category not found"))
        if (existing.pendingState == PendingState.PENDING_CREATE) {
            db.categoryDao().deleteById(id)
            return Result.success(Unit)
        }
        db.categoryDao().upsert(existing.copy(pendingState = PendingState.PENDING_DELETE))
        queue.enqueue(
            entity = "categories",
            op = "delete",
            method = "DELETE",
            path = "categories/$id",
            localId = id,
            serverId = id,
            payloadJson = "{}",
            baseSyncSeq = existing.syncSeq,
        )
        return Result.success(Unit)
    }
}

@Singleton
class OfflineBudgetRepository @Inject constructor(
    private val api: EfinaApi,
    private val db: EfinaDatabase,
    private val queue: MutationQueue,
) : BudgetRepository {

    override fun observeBudgets(): Flow<List<Budget>> =
        db.budgetDao().observeAll().map { rows -> rows.map { it.toDomainEntity() } }

    override suspend fun getBudgets(): Result<List<Budget>> =
        safeCall { api.getBudgets() }
            .onSuccess { rows -> for (dto in rows) db.budgetDao().upsertFromServer(dto.toEntity()) }
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun createBudget(request: CreateBudgetRequest): Result<Budget> {
        val localId = queue.newLocalId()
        val category = db.categoryDao().byId(request.categoryId)
        val entity = BudgetEntity(
            id = localId,
            userId = 0,
            categoryId = request.categoryId,
            categoryName = category?.name,
            categoryColor = category?.color,
            amount = request.amount,
            spent = 0.0,
            periodType = request.periodType.value,
            periodStart = null,
            periodEnd = null,
            familyId = null,
            isShared = false,
            pendingState = PendingState.PENDING_CREATE,
        )
        db.budgetDao().upsert(entity)
        queue.enqueue(
            entity = "budgets",
            op = "create",
            method = "POST",
            path = "budgets",
            localId = localId,
            serverId = null,
            payloadJson = offlineJson.encodeToString(
                com.efina.app.data.remote.dto.CreateBudgetRequestDto.serializer(),
                request.toDto(),
            ),
            baseSyncSeq = null,
        )
        return Result.success(entity.toDomainEntity())
    }

    override suspend fun updateBudget(id: Long, request: UpdateBudgetRequest): Result<Budget> {
        val existing = db.budgetDao().byId(id)
            ?: return Result.failure(IllegalStateException("Budget not found"))
        val updated = existing.copy(
            categoryId = request.categoryId ?: existing.categoryId,
            amount = request.amount ?: existing.amount,
            periodType = request.periodType?.value ?: existing.periodType,
            pendingState = PendingState.PENDING_UPDATE,
        )
        db.budgetDao().upsert(updated)
        queue.enqueue(
            entity = "budgets",
            op = "update",
            method = "PUT",
            path = "budgets/$id",
            localId = id,
            serverId = id.takeIf { it > 0 },
            payloadJson = offlineJson.encodeToString(
                com.efina.app.data.remote.dto.UpdateBudgetRequestDto.serializer(),
                request.toDto(),
            ),
            baseSyncSeq = existing.syncSeq,
        )
        return Result.success(updated.toDomainEntity())
    }

    override suspend fun deleteBudget(id: Long): Result<Unit> {
        val existing = db.budgetDao().byId(id)
            ?: return Result.failure(IllegalStateException("Budget not found"))
        if (existing.pendingState == PendingState.PENDING_CREATE) {
            db.budgetDao().deleteById(id)
            return Result.success(Unit)
        }
        db.budgetDao().upsert(existing.copy(pendingState = PendingState.PENDING_DELETE))
        queue.enqueue(
            entity = "budgets",
            op = "delete",
            method = "DELETE",
            path = "budgets/$id",
            localId = id,
            serverId = id,
            payloadJson = "{}",
            baseSyncSeq = existing.syncSeq,
        )
        return Result.success(Unit)
    }

    /** Sharing a budget changes who can see it, so it stays online-only. */
    override suspend fun setBudgetSharing(id: Long, familyId: Long?): Result<Budget> =
        safeCall {
            api.updateBudgetSharing(
                id,
                com.efina.app.data.remote.dto.UpdateBudgetSharingRequestDto(familyId),
            )
        }.map { it.toDomain() }

    /** Computed server-side, so this stays online-only and is not mirrored. */
    override suspend fun getBudgetVsActual(): Result<List<com.efina.app.domain.model.BudgetVsActual>> =
        safeCall { api.getBudgetVsActual() }.map { dto ->
            dto.categories.map {
                com.efina.app.domain.model.BudgetVsActual(
                    categoryId = it.category_id,
                    name = it.category_name ?: "Category",
                    budget = it.budget,
                    spent = it.spent,
                    percentage = it.percentage,
                )
            }
        }
}

@Singleton
class OfflineGoalRepository @Inject constructor(
    private val api: EfinaApi,
    private val db: EfinaDatabase,
    private val queue: MutationQueue,
) : GoalRepository {

    override fun observeGoals(): Flow<List<Goal>> =
        db.goalDao().observeAll().map { rows -> rows.map { it.toDomainEntity() } }

    override suspend fun getGoals(): Result<List<Goal>> =
        safeCall { api.getGoals() }
            .onSuccess { rows -> for (dto in rows) db.goalDao().upsertFromServer(dto.toEntity()) }
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun createGoal(request: CreateGoalRequest): Result<Goal> {
        val localId = queue.newLocalId()
        val entity = GoalEntity(
            id = localId,
            userId = 0,
            name = request.name,
            targetAmount = request.targetAmount,
            currentAmount = request.currentAmount,
            targetDate = request.targetDate,
            icon = request.icon,
            linkedAccountId = request.linkedAccountId,
            createdAt = null,
            pendingState = PendingState.PENDING_CREATE,
        )
        db.goalDao().upsert(entity)
        queue.enqueue(
            entity = "goals",
            op = "create",
            method = "POST",
            path = "goals",
            localId = localId,
            serverId = null,
            payloadJson = offlineJson.encodeToString(
                com.efina.app.data.remote.dto.CreateGoalRequestDto.serializer(),
                request.toDto(),
            ),
            baseSyncSeq = null,
        )
        return Result.success(entity.toDomainEntity())
    }

    override suspend fun updateGoal(id: Long, request: UpdateGoalRequest): Result<Goal> {
        val existing = db.goalDao().byId(id)
            ?: return Result.failure(IllegalStateException("Goal not found"))
        val updated = existing.copy(
            name = request.name ?: existing.name,
            targetAmount = request.targetAmount ?: existing.targetAmount,
            currentAmount = request.currentAmount ?: existing.currentAmount,
            targetDate = request.targetDate ?: existing.targetDate,
            icon = request.icon ?: existing.icon,
            linkedAccountId = request.linkedAccountId ?: existing.linkedAccountId,
            pendingState = PendingState.PENDING_UPDATE,
        )
        db.goalDao().upsert(updated)
        queue.enqueue(
            entity = "goals",
            op = "update",
            method = "PUT",
            path = "goals/$id",
            localId = id,
            serverId = id.takeIf { it > 0 },
            payloadJson = offlineJson.encodeToString(
                com.efina.app.data.remote.dto.UpdateGoalRequestDto.serializer(),
                request.toDto(),
            ),
            baseSyncSeq = existing.syncSeq,
        )
        return Result.success(updated.toDomainEntity())
    }

    override suspend fun deleteGoal(id: Long): Result<Unit> {
        val existing = db.goalDao().byId(id)
            ?: return Result.failure(IllegalStateException("Goal not found"))
        if (existing.pendingState == PendingState.PENDING_CREATE) {
            db.goalDao().deleteById(id)
            return Result.success(Unit)
        }
        db.goalDao().upsert(existing.copy(pendingState = PendingState.PENDING_DELETE))
        queue.enqueue(
            entity = "goals",
            op = "delete",
            method = "DELETE",
            path = "goals/$id",
            localId = id,
            serverId = id,
            payloadJson = "{}",
            baseSyncSeq = existing.syncSeq,
        )
        return Result.success(Unit)
    }

    /**
     * Adding funds creates a transaction and moves the goal's balance in one
     * server-side `DB.batch`, so it is queued as a single mutation. Splitting it
     * would let the transaction land without the goal moving.
     */
    override suspend fun addFunds(id: Long, amount: Double, accountId: Long): Result<Unit> {
        queue.enqueue(
            entity = "goals",
            op = "addFunds",
            method = "POST",
            path = "goals/$id/add-funds",
            localId = id,
            serverId = id.takeIf { it > 0 },
            payloadJson = offlineJson.encodeToString(
                com.efina.app.data.remote.dto.AddGoalFundsRequestDto.serializer(),
                com.efina.app.data.remote.dto.AddGoalFundsRequestDto(amount, accountId),
            ),
            baseSyncSeq = null,
        )
        return Result.success(Unit)
    }
}

@Singleton
class OfflineDebtRepository @Inject constructor(
    private val api: EfinaApi,
    private val db: EfinaDatabase,
    private val queue: MutationQueue,
) : DebtRepository {

    override fun observeDebts(): Flow<List<Debt>> =
        db.debtDao().observeAll().map { rows -> rows.map { it.toDomainEntity() } }

    override suspend fun getDebts(): Result<List<Debt>> =
        safeCall { api.getDebts() }
            .onSuccess { resp -> for (dto in resp.debts) db.debtDao().upsertFromServer(dto.toEntity()) }
            .map { resp -> resp.debts.map { it.toDomain() } }

    override suspend fun createDebt(request: CreateDebtRequest): Result<Debt> {
        val localId = queue.newLocalId()
        val entity = DebtEntity(
            id = localId,
            userId = 0,
            name = request.name,
            amount = request.amount,
            remainingAmount = request.amount,
            dueDate = request.dueDate,
            type = request.type.value,
            status = "active",
            notes = request.notes,
            createdAt = null,
            pendingState = PendingState.PENDING_CREATE,
        )
        db.debtDao().upsert(entity)
        queue.enqueue(
            entity = "debts",
            op = "create",
            method = "POST",
            path = "debts",
            localId = localId,
            serverId = null,
            payloadJson = offlineJson.encodeToString(
                com.efina.app.data.remote.dto.CreateDebtRequestDto.serializer(),
                com.efina.app.data.remote.dto.CreateDebtRequestDto(
                    person_name = request.name,
                    amount = request.amount,
                    due_date = request.dueDate,
                    type = request.type.value,
                    description = request.notes,
                ),
            ),
            baseSyncSeq = null,
        )
        return Result.success(entity.toDomainEntity())
    }

    override suspend fun updateDebt(id: Long, request: UpdateDebtRequest): Result<Debt> {
        val existing = db.debtDao().byId(id)
            ?: return Result.failure(IllegalStateException("Debt not found"))
        val updated = existing.copy(
            name = request.name ?: existing.name,
            amount = request.amount ?: existing.amount,
            dueDate = request.dueDate ?: existing.dueDate,
            notes = request.notes ?: existing.notes,
            pendingState = PendingState.PENDING_UPDATE,
        )
        db.debtDao().upsert(updated)
        queue.enqueue(
            entity = "debts",
            op = "update",
            method = "PUT",
            path = "debts/$id",
            localId = id,
            serverId = id.takeIf { it > 0 },
            payloadJson = offlineJson.encodeToString(
                com.efina.app.data.remote.dto.UpdateDebtRequestDto.serializer(),
                com.efina.app.data.remote.dto.UpdateDebtRequestDto(
                    person_name = request.name,
                    amount = request.amount,
                    due_date = request.dueDate,
                    type = request.type?.value,
                    description = request.notes,
                ),
            ),
            baseSyncSeq = existing.syncSeq,
        )
        return Result.success(updated.toDomainEntity())
    }

    override suspend fun deleteDebt(id: Long): Result<Unit> {
        val existing = db.debtDao().byId(id)
            ?: return Result.failure(IllegalStateException("Debt not found"))
        if (existing.pendingState == PendingState.PENDING_CREATE) {
            db.debtDao().deleteById(id)
            return Result.success(Unit)
        }
        db.debtDao().upsert(existing.copy(pendingState = PendingState.PENDING_DELETE))
        queue.enqueue(
            entity = "debts",
            op = "delete",
            method = "DELETE",
            path = "debts/$id",
            localId = id,
            serverId = id,
            payloadJson = "{}",
            baseSyncSeq = existing.syncSeq,
        )
        return Result.success(Unit)
    }

    /** Paying a debt creates a transaction and moves a balance in one batch. */
    override suspend fun payDebt(id: Long, request: PayDebtRequest): Result<Unit> {
        queue.enqueue(
            entity = "debts",
            op = "pay",
            method = "POST",
            path = "debts/$id/pay",
            localId = id,
            serverId = id.takeIf { it > 0 },
            payloadJson = offlineJson.encodeToString(
                com.efina.app.data.remote.dto.PayDebtRequestDto.serializer(),
                com.efina.app.data.remote.dto.PayDebtRequestDto(
                    amount = request.amount,
                    account_id = request.accountId,
                    create_transaction = request.createTransaction,
                    date = request.date,
                ),
            ),
            baseSyncSeq = null,
        )
        return Result.success(Unit)
    }
}

@Singleton
class OfflineScheduledRepository @Inject constructor(
    private val api: EfinaApi,
    private val db: EfinaDatabase,
    private val queue: MutationQueue,
) : ScheduledRepository {

    override fun observeScheduled(): Flow<List<ScheduledTransaction>> =
        db.scheduledDao().observeAll().map { rows -> rows.map { it.toDomainEntity() } }

    override suspend fun getScheduled(): Result<List<ScheduledTransaction>> =
        safeCall { api.getScheduled() }
            .onSuccess { rows -> for (dto in rows) db.scheduledDao().upsertFromServer(dto.toEntity()) }
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun createScheduled(request: CreateScheduledRequest): Result<ScheduledTransaction> {
        val localId = queue.newLocalId()
        val entity = ScheduledEntity(
            id = localId,
            userId = 0,
            accountId = request.accountId,
            categoryId = request.categoryId,
            amount = request.amount,
            type = request.type.value,
            description = request.description,
            frequency = request.frequency,
            nextDate = request.nextDate,
            isActive = true,
            createdAt = null,
            pendingState = PendingState.PENDING_CREATE,
        )
        db.scheduledDao().upsert(entity)
        queue.enqueue(
            entity = "scheduled",
            op = "create",
            method = "POST",
            path = "scheduled",
            localId = localId,
            serverId = null,
            payloadJson = offlineJson.encodeToString(
                com.efina.app.data.remote.dto.CreateScheduledRequestDto.serializer(),
                com.efina.app.data.remote.dto.CreateScheduledRequestDto(
                    account_id = request.accountId,
                    category_id = request.categoryId,
                    amount = request.amount,
                    type = request.type.value,
                    description = request.description,
                    frequency = request.frequency,
                    next_run_date = request.nextDate.orEmpty(),
                ),
            ),
            baseSyncSeq = null,
        )
        return Result.success(entity.toDomainEntity())
    }

    override suspend fun deleteScheduled(id: Long): Result<Unit> {
        val existing = db.scheduledDao().byId(id)
            ?: return Result.failure(IllegalStateException("Scheduled transaction not found"))
        if (existing.pendingState == PendingState.PENDING_CREATE) {
            db.scheduledDao().deleteById(id)
            return Result.success(Unit)
        }
        db.scheduledDao().upsert(existing.copy(pendingState = PendingState.PENDING_DELETE))
        queue.enqueue(
            entity = "scheduled",
            op = "delete",
            method = "DELETE",
            path = "scheduled/$id",
            localId = id,
            serverId = id,
            payloadJson = "{}",
            baseSyncSeq = existing.syncSeq,
        )
        return Result.success(Unit)
    }
}
