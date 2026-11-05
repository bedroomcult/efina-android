package com.efina.app.data.remote.dto

import com.efina.app.domain.model.*

/**
 * DTO -> domain mapping.
 *
 * Kept in one file so a backend field rename breaks exactly one place. Every
 * mapper tolerates nulls, because the list endpoints JOIN and may omit columns
 * for rows the caller cannot fully resolve (for example a category deleted after
 * the transaction was recorded).
 */

fun AccountDto.toDomain(): Account = Account(
    id = id,
    name = name,
    type = AccountType.from(type),
    balance = balance,
    currency = currency,
    description = description,
    color = color,
    displayOrder = display_order,
    disabled = disabled == 1,
    initialBalance = initial_balance,
    createdAt = created_at,
    isShared = is_shared == 1,
    familyId = family_id,
    role = AccountShareRole.from(account_role),
    memberCount = member_count,
    ownerUserId = user_id
)

fun TransactionDto.toDomain(): Transaction = Transaction(
    id = id,
    userId = user_id,
    accountId = account_id,
    accountName = account_name,
    accountRole = AccountShareRole.from(account_role),
    currency = account_currency ?: "IDR",
    categoryId = category_id,
    categoryName = category_name,
    categoryColor = category_color,
    categoryIcon = category_icon,
    toAccountId = to_account_id,
    toAccountName = to_account_name,
    amount = amount,
    type = TransactionType.from(type),
    description = description,
    date = date,
    mood = mood,
    transferPairId = transfer_pair_id,
    familyId = family_id,
    creatorUserId = creator_user_id,
    creatorName = creator_name,
    createdAt = created_at
)

fun CategoryDto.toDomain(): Category = Category(
    id = id,
    name = name,
    color = color ?: "blue",
    icon = icon,
    parentId = parent_id,
    isSystem = is_system == 1,
    type = type,
    children = subcategories.map { it.toDomain() }
)

fun BudgetDto.toDomain(): Budget = Budget(
    id = id,
    categoryId = category_id,
    categoryName = category_name,
    categoryColor = category_color,
    amount = amount,
    spent = spent,
    periodType = PeriodType.from(period_type),
    periodStart = period_start,
    periodEnd = period_end,
    familyId = family_id,
    isShared = is_shared == 1
)

fun GoalDto.toDomain(): Goal = Goal(
    id = id,
    name = name,
    targetAmount = target_amount,
    currentAmount = current_amount,
    startDate = start_date,
    targetDate = target_date,
    periodType = period_type,
    linkedAccountId = linked_account_id,
    icon = icon,
    accountName = account_name
)

fun UserDto.toDomain(): User = User(
    id = id,
    email = email,
    name = name ?: email.substringBefore('@'),
    role = role,
    planId = plan_id,
    currency = currency ?: "IDR",
    language = language ?: "id",
    timezone = timezone ?: "Asia/Jakarta",
    hasSeenTutorial = has_seen_tutorial,
    forceOnboarding = force_onboarding,
    accountCount = account_count
)

fun AchievementDto.toDomain(): Achievement = Achievement(
    id = id,
    // `id` is a TEXT key; domains still key on a string.
    name = name,
    description = description,
    icon = icon,
    color = color,
    target = target,
    progress = progress,
    achieved = achieved,
    achievedAt = achieved_at
)

fun FamilyGroupDto.toDomain(): FamilyGroup = FamilyGroup(
    id = id,
    name = name,
    memberRole = member_role ?: "member",
    memberCount = member_count,
    members = members.map { it.toDomain() }
)

fun FamilyMemberDto.toDomain(): FamilyMember = FamilyMember(
    userId = user_id,
    role = role,
    name = name ?: email?.substringBefore('@') ?: "Member",
    email = email,
    joinedAt = joined_at
)

fun AccountMemberDto.toDomain(): AccountMember = AccountMember(
    userId = user_id,
    role = AccountShareRole.from(role) ?: AccountShareRole.MEMBER,
    name = name ?: email?.substringBefore('@') ?: "Member",
    email = email,
    joinedAt = joined_at
)

fun NotificationDto.toDomain(): AppNotification = AppNotification(
    id = id,
    title = title,
    body = message,
    type = type,
    link = link,
    isRead = is_read == 1,
    createdAt = created_at
)

fun DebtDto.toDomain(): Debt = Debt(
    id = id,
    name = person_name,
    amount = total_amount,
    remainingAmount = remaining_amount,
    dueDate = due_date,
    type = DebtType.from(type),
    status = status,
    notes = notes
)

fun ScheduledDto.toDomain(): ScheduledTransaction = ScheduledTransaction(
    id = id,
    accountId = account_id,
    categoryId = category_id,
    amount = amount,
    type = TransactionType.from(type),
    description = description,
    frequency = frequency,
    nextDate = next_run_date,
    isActive = is_active == 1
)

fun UserSettingsDto.toDomain(): UserSettings = UserSettings(
    currency = currency,
    dateFormat = date_format,
    language = language,
    timezone = timezone,
    theme = theme,
    hasSeenTutorial = has_seen_tutorial == 1,
    animationMode = animation_mode,
    animationLevel = animation_level
)

fun LeaderboardEntryDto.toDomain(): LeaderboardEntry = LeaderboardEntry(
    rank = rank,
    userId = userId,
    name = name ?: "User",
    xp = xp,
    level = level,
    tier = tier
)

// ── request mapping: domain -> DTO ──────────────────────────────────

fun CreateAccountRequest.toDto(): CreateAccountRequestDto = CreateAccountRequestDto(
    name = name,
    type = type.value,
    balance = balance,
    currency = currency,
    description = description,
    color = color,
    family_id = familyId
)

fun UpdateAccountRequest.toDto(): UpdateAccountRequestDto = UpdateAccountRequestDto(
    name = name,
    type = type.value,
    balance = balance,
    currency = currency,
    description = description,
    color = color,
    // Without a strategy the backend takes a plain UPDATE: no reconciliation
    // transaction and initial_balance is left stale.
    update_strategy = updateStrategy,
    date = date
)

fun CreateTransactionRequest.toDto(): CreateTransactionRequestDto = CreateTransactionRequestDto(
    account_id = accountId,
    category_id = categoryId,
    amount = amount,
    type = type.value,
    description = description,
    date = date,
    mood = mood
)

fun UpdateTransactionRequest.toDto(): UpdateTransactionRequestDto = UpdateTransactionRequestDto(
    account_id = accountId,
    category_id = categoryId,
    amount = amount,
    type = type.value,
    description = description,
    date = date,
    mood = mood
)

fun CreateCategoryRequest.toDto(): CreateCategoryRequestDto = CreateCategoryRequestDto(
    name = name,
    type = type.value,
    color = color,
    icon = icon,
    parent_id = parentId
)

fun UpdateCategoryRequest.toDto(): UpdateCategoryRequestDto = UpdateCategoryRequestDto(
    name = name,
    color = color,
    icon = icon,
    parent_id = parentId
)

fun CreateBudgetRequest.toDto(): CreateBudgetRequestDto = CreateBudgetRequestDto(
    category_id = categoryId,
    amount = amount,
    period_type = periodType.value
)

fun UpdateBudgetRequest.toDto(): UpdateBudgetRequestDto = UpdateBudgetRequestDto(
    amount = amount,
    period_type = periodType?.value
)

fun CreateGoalRequest.toDto(): CreateGoalRequestDto = CreateGoalRequestDto(
    name = name,
    target_amount = targetAmount,
    current_amount = currentAmount,
    start_date = startDate,
    target_date = targetDate,
    period_type = periodType,
    linked_account_id = linkedAccountId
)

fun UpdateGoalRequest.toDto(): UpdateGoalRequestDto = UpdateGoalRequestDto(
    name = name,
    target_amount = targetAmount,
    current_amount = currentAmount,
    start_date = startDate,
    target_date = targetDate,
    period_type = periodType,
    linked_account_id = linkedAccountId
)

fun TransactionFilter.toDto(): Map<String, String> {
    val q = mutableMapOf<String, String>()
    q["limit"] = limit.toString()
    q["offset"] = offset.toString()
    type?.let { q["type"] = it.value }
    accountId?.let { q["account_id"] = it.toString() }
    categoryId?.let { q["category_id"] = it.toString() }
    startDate?.let { q["start_date"] = it }
    endDate?.let { q["end_date"] = it }
    search?.takeIf { it.isNotBlank() }?.let { q["search"] = it }
    if (includeSubcategories) q["include_subcategories"] = "true"
    return q
}
