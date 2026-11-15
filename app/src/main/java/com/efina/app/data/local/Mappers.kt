package com.efina.app.data.local

import com.efina.app.data.remote.dto.AccountDto
import com.efina.app.data.remote.dto.BudgetDto
import com.efina.app.data.remote.dto.CategoryDto
import com.efina.app.data.remote.dto.DebtDto
import com.efina.app.data.remote.dto.GoalDto
import com.efina.app.data.remote.dto.ScheduledDto
import com.efina.app.data.remote.dto.TransactionDto
import com.efina.app.domain.model.Account
import com.efina.app.domain.model.AccountShareRole
import com.efina.app.domain.model.AccountType
import com.efina.app.domain.model.Budget
import com.efina.app.domain.model.Category
import com.efina.app.domain.model.Debt
import com.efina.app.domain.model.DebtType
import com.efina.app.domain.model.Goal
import com.efina.app.domain.model.PeriodType
import com.efina.app.domain.model.ScheduledTransaction
import com.efina.app.domain.model.Transaction
import com.efina.app.domain.model.TransactionType

/**
 * DTO -> entity -> domain.
 *
 * The entity is the storage shape (with syncSeq and pendingState); the domain
 * model is what the UI sees and is deliberately unaware of sync. Two conversions
 * rather than one keeps the offline machinery out of every screen.
 */

fun AccountDto.toEntity(): AccountEntity = AccountEntity(
    id = id,
    userId = user_id,
    name = name,
    type = type,
    balance = balance,
    currency = currency,
    description = description,
    color = color,
    displayOrder = display_order,
    disabled = disabled != 0,
    initialBalance = initial_balance,
    createdAt = created_at,
    isShared = is_shared != 0,
    familyId = family_id,
    role = account_role,
    memberCount = member_count,
    ownerUserId = user_id,
    syncSeq = sync_seq,
)

fun AccountEntity.toDomain(): Account = Account(
    id = id,
    name = name,
    type = AccountType.from(type),
    balance = balance,
    currency = currency,
    description = description,
    color = color,
    displayOrder = displayOrder,
    disabled = disabled,
    initialBalance = initialBalance,
    createdAt = createdAt,
    isShared = isShared,
    familyId = familyId,
    role = AccountShareRole.from(role),
    memberCount = memberCount,
    ownerUserId = ownerUserId,
)

fun CategoryDto.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    userId = user_id,
    name = name,
    color = color ?: "#6366f1",
    icon = icon,
    parentId = parent_id,
    parentName = null,
    isSystem = is_system != 0,
    isShared = is_shared,
    type = type,
    familyId = family_id,
    syncSeq = sync_seq,
)

fun CategoryEntity.toDomain(children: List<Category> = emptyList()): Category = Category(
    id = id,
    name = name,
    color = color,
    icon = icon,
    parentId = parentId,
    parentName = parentName,
    isSystem = isSystem,
    isShared = isShared,
    type = type,
    children = children,
)

fun TransactionDto.toEntity(accountCurrency: String = "IDR"): TransactionEntity = TransactionEntity(
    id = id,
    userId = user_id,
    accountId = account_id,
    accountName = account_name,
    accountRole = account_role,
    currency = account_currency ?: accountCurrency,
    categoryId = category_id,
    categoryName = category_name,
    categoryColor = category_color,
    categoryIcon = category_icon,
    toAccountId = to_account_id,
    toAccountName = to_account_name,
    amount = amount,
    type = type,
    description = description,
    date = date,
    mood = mood,
    transferPairId = transfer_pair_id,
    familyId = family_id,
    creatorUserId = creator_user_id,
    creatorName = creator_name,
    createdAt = created_at,
    syncSeq = sync_seq,
)

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    userId = userId,
    accountId = accountId,
    accountName = accountName,
    accountRole = AccountShareRole.from(accountRole),
    currency = currency,
    categoryId = categoryId,
    categoryName = categoryName,
    categoryColor = categoryColor,
    categoryIcon = categoryIcon,
    toAccountId = toAccountId,
    toAccountName = toAccountName,
    amount = amount,
    type = TransactionType.from(type),
    description = description,
    date = date,
    mood = mood,
    transferPairId = transferPairId,
    familyId = familyId,
    creatorUserId = creatorUserId,
    creatorName = creatorName,
    createdAt = createdAt,
    pending = pendingState != PendingState.SYNCED,
)

fun BudgetDto.toEntity(): BudgetEntity = BudgetEntity(
    id = id,
    userId = user_id,
    categoryId = category_id,
    categoryName = category_name,
    categoryColor = category_color,
    amount = amount,
    spent = spent,
    periodType = period_type,
    periodStart = period_start,
    periodEnd = period_end,
    familyId = family_id,
    isShared = is_shared != 0,
    syncSeq = sync_seq,
)

fun BudgetEntity.toDomain(): Budget = Budget(
    id = id,
    categoryId = categoryId,
    categoryName = categoryName,
    categoryColor = categoryColor,
    amount = amount,
    spent = spent,
    periodType = PeriodType.from(periodType),
    periodStart = periodStart,
    periodEnd = periodEnd,
    familyId = familyId,
    isShared = isShared,
)

fun GoalDto.toEntity(): GoalEntity = GoalEntity(
    id = id,
    userId = user_id,
    name = name,
    targetAmount = target_amount,
    currentAmount = current_amount,
    targetDate = target_date,
    icon = icon,
    linkedAccountId = linked_account_id,
    createdAt = created_at,
    syncSeq = sync_seq,
)

fun GoalEntity.toDomain(): Goal = Goal(
    id = id,
    name = name,
    targetAmount = targetAmount,
    currentAmount = currentAmount,
    startDate = null,
    targetDate = targetDate,
    periodType = null,
    linkedAccountId = linkedAccountId,
    accountName = null,
    icon = icon,
)

fun DebtDto.toEntity(): DebtEntity = DebtEntity(
    id = id,
    userId = user_id,
    name = person_name,
    amount = total_amount,
    remainingAmount = remaining_amount,
    dueDate = due_date,
    type = type,
    status = status,
    notes = notes,
    createdAt = created_at,
    syncSeq = sync_seq,
)

fun DebtEntity.toDomain(): Debt = Debt(
    id = id,
    name = name,
    amount = amount,
    remainingAmount = remainingAmount,
    dueDate = dueDate,
    type = DebtType.from(type),
    status = status ?: "active",
    notes = notes,
)

fun ScheduledDto.toEntity(): ScheduledEntity = ScheduledEntity(
    id = id,
    userId = user_id,
    accountId = account_id,
    categoryId = category_id,
    amount = amount,
    type = type,
    description = description,
    frequency = frequency,
    nextDate = next_run_date,
    isActive = is_active != 0,
    createdAt = created_at,
    syncSeq = sync_seq,
)

fun ScheduledEntity.toDomain(): ScheduledTransaction = ScheduledTransaction(
    id = id,
    accountId = accountId ?: 0L,
    categoryId = categoryId,
    amount = amount,
    type = TransactionType.from(type),
    description = description,
    frequency = frequency ?: "monthly",
    nextDate = nextDate,
    isActive = isActive,
)
