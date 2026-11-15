package com.efina.app.di

import com.efina.app.data.repository.*
import com.efina.app.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: OfflineAccountRepository): AccountRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: OfflineTransactionRepository): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: OfflineCategoryRepository): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(impl: OfflineBudgetRepository): BudgetRepository

    @Binds
    @Singleton
    abstract fun bindGoalRepository(impl: OfflineGoalRepository): GoalRepository

    @Binds
    @Singleton
    abstract fun bindDebtRepository(impl: OfflineDebtRepository): DebtRepository

    @Binds
    @Singleton
    abstract fun bindScheduledRepository(impl: OfflineScheduledRepository): ScheduledRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindInsightsRepository(impl: InsightsRepositoryImpl): InsightsRepository

    @Binds
    @Singleton
    abstract fun bindFamilyRepository(impl: FamilyRepositoryImpl): FamilyRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository
}
