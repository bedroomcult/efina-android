package com.efina.app.data.remote.dto

import com.efina.app.domain.model.*

// ============ Auth Mappers ============

fun AuthResponseDto.toDomain() = AuthToken(
    token = token,
    user = user.toDomain()
)

fun UserDto.toDomain() = User(
    id = id,
    email = email,
    name = name
)

// ============ Account Mappers ============

fun AccountDto.toDomain() = Account(
    id = id,
    name = name,
    type = AccountType.entries.find { it.value == type } ?: AccountType.CHECKING,
    balance = balance,
    currency = currency ?: "IDR",
    description = description,
    color = color,
    icon = icon,
    displayOrder = displayOrder ?: 0,
    includeInTotal = includeInTotal ?: true,
    showInDashboard = showInDashboard ?: true,
    createdAt = createdAt
)

fun CreateAccountRequest.toDto() = CreateAccountRequestDto(
    name = name,
    type = type.value,
    balance = balance,
    currency = currency,
    description = description,
    color = color,
    icon = icon,
    includeInTotal = includeInTotal,
    showInDashboard = showInDashboard
)

// ============ Transaction Mappers ============

fun TransactionDto.toDomain() = Transaction(
    id = id,
    userId = 0,
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
    amount = amount,
    spent = spent,
    remaining = remaining,
    periodType = PeriodType.entries.find { it.value == periodType } ?: PeriodType.MONTHLY,
    periodStart = periodStart,
    periodEnd = periodEnd
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
    periodType = periodType?.let { GoalPeriodType.entries.find { p -> p.value == it } },
    linkedAccountId = linkedAccountId,
    accountName = accountName,
    progressPercentage = progressPercentage,
    status = status
)

fun CreateGoalRequest.toDto() = CreateGoalRequestDto(
    name = name,
    targetAmount = targetAmount,
    startDate = startDate,
    targetDate = targetDate,
    periodType = periodType.value,
    linkedAccountId = linkedAccountId
)

// ============ Settings Mappers ============

fun UserSettingsDto.toDomain() = UserSettings(
    theme = theme ?: "dark",
    language = language ?: "en",
    currency = currency ?: "IDR",
    timezone = timezone ?: "Asia/Jakarta"
)

fun UserSettings.toUpdateRequest() = UpdateSettingsRequestDto(
    theme = theme,
    language = language,
    currency = currency,
    timezone = timezone
)

// ============ User Stats Mappers ============

fun XpSummaryDto.toDomain() = UserStats(
    activeStreak = activeStreak ?: 0,
    totalTransactions = totalTransactions ?: 0,
    xp = xp ?: 0,
    level = level ?: 1,
    tier = tier ?: "Bronze"
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
