package com.efina.app.data.remote.dto

import com.efina.app.domain.model.*

// ============ Auth Mappers ============

fun AuthResponseDto.toDomain() = AuthToken(
    token = token,
    csrfToken = csrfToken ?: "",
    user = user.toDomain()
)

fun UserDto.toDomain() = User(
    id = id,
    email = email,
    name = name,
    role = role,
    planId = planId ?: "free",
    forceOnboarding = forceOnboarding ?: false
)

fun SessionDto.toDomain() = Session(
    id = id,
    deviceInfo = deviceInfo,
    ipAddress = ipAddress,
    createdAt = createdAt
)

// ============ Account Mappers ============

fun AccountDto.toDomain() = Account(
    id = id,
    name = name,
    type = AccountType.entries.find { it.value == type } ?: AccountType.BANK,
    balance = balance,
    currency = currency ?: "IDR",
    description = description,
    displayOrder = displayOrder ?: 0,
    initialBalance = initialBalance ?: 0.0,
    createdAt = createdAt
)

fun CreateAccountRequest.toDto() = CreateAccountRequestDto(
    name = name,
    type = type.value,
    balance = balance,
    currency = currency,
    description = description
)

// ============ Transaction Mappers ============

fun TransactionDto.toDomain() = Transaction(
    id = id,
    userId = userId,
    accountId = accountId,
    accountName = accountName,
    categoryId = categoryId,
    categoryName = categoryName,
    categoryColor = categoryColor,
    categoryIcon = categoryIcon,
    toAccountId = toAccountId,
    toAccountName = toAccountName,
    amount = amount,
    type = TransactionType.entries.find { it.value == type } ?: TransactionType.EXPENSE,
    description = description,
    date = date,
    mood = mood,
    transferPairId = transferPairId,
    createdAt = createdAt
)

fun TransactionListResponseDto.toDomain() = TransactionListResponse(
    transactions = transactions.map { it.toDomain() },
    total = total,
    limit = limit,
    offset = offset
)

fun CreateTransactionRequest.toDto() = CreateTransactionRequestDto(
    accountId = accountId,
    categoryId = categoryId,
    amount = amount,
    type = type.value,
    description = description,
    date = date,
    mood = mood
)

// ============ Category Mappers ============

fun CategoryDto.toDomain() = Category(
    id = id,
    name = name,
    type = CategoryType.entries.find { it.value == type } ?: CategoryType.EXPENSE,
    color = color,
    icon = icon,
    parentId = parentId
)

fun CreateCategoryRequest.toDto() = CreateCategoryRequestDto(
    name = name,
    type = type.value,
    color = color,
    icon = icon,
    parentId = parentId
)

// ============ Budget Mappers ============

fun BudgetDto.toDomain() = Budget(
    id = id,
    categoryId = categoryId,
    categoryName = categoryName,
    categoryColor = categoryColor,
    categoryIcon = categoryIcon,
    amount = amount,
    periodType = PeriodType.entries.find { it.value == periodType } ?: PeriodType.MONTHLY,
    periodStart = periodStart,
    periodEnd = periodEnd,
    spent = spent,
    percentage = percentage
)

fun CreateBudgetRequest.toDto() = CreateBudgetRequestDto(
    categoryId = categoryId,
    amount = amount,
    periodType = periodType.value
)

// ============ Goal Mappers ============

fun GoalDto.toDomain() = Goal(
    id = id,
    name = name,
    targetAmount = targetAmount,
    currentAmount = currentAmount,
    startDate = startDate,
    targetDate = targetDate,
    periodType = GoalPeriodType.entries.find { it.value == periodType } ?: GoalPeriodType.MONTHLY,
    linkedAccountId = linkedAccountId,
    accountName = accountName,
    totalPeriods = totalPeriods,
    remainingPeriods = remainingPeriods,
    remainingAmount = remainingAmount,
    suggestedSaving = suggestedSaving
)

fun CreateGoalRequest.toDto() = CreateGoalRequestDto(
    name = name,
    targetAmount = targetAmount,
    startDate = startDate,
    targetDate = targetDate,
    periodType = periodType.value,
    linkedAccountId = linkedAccountId
)

// ============ User Stats Mappers ============

fun UserStatsDto.toDomain() = UserStats(
    activeStreak = activeStreak ?: 0,
    totalTransactions = totalTransactions ?: 0,
    lastTransactionDate = lastTransactionDate,
    xp = xp ?: 0,
    level = level ?: 1,
    tier = tier ?: "Bronze",
    xpToNextLevel = xpToNextLevel ?: 100,
    xpProgressPercent = xpProgressPercent ?: 0
)

fun AchievementDto.toDomain() = Achievement(
    id = id,
    name = name,
    description = description,
    tier = tier ?: "bronze",
    xpReward = xpReward ?: 0,
    achievedAt = achievedAt,
    progress = progress
)

fun UserProfileResponseDto.toDomain() = UserProfile(
    stats = stats.toDomain(),
    achievements = achievements.map { it.toDomain() }
)

// ============ Settings Mappers ============

fun UserSettingsDto.toDomain() = UserSettings(
    theme = theme ?: "dark",
    language = language ?: "en",
    currency = currency ?: "IDR",
    timezone = timezone ?: "Asia/Jakarta",
    hasSeenTutorial = hasSeenTutorial ?: false
)

fun UserSettings.toUpdateRequest() = UpdateSettingsRequestDto(
    theme = theme,
    language = language,
    currency = currency,
    timezone = timezone
)
