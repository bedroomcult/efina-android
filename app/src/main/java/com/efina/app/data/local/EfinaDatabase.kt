package com.efina.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

class PendingStateConverter {
    @TypeConverter
    fun toName(value: PendingState): String = value.name

    @TypeConverter
    fun fromName(value: String): PendingState =
        runCatching { PendingState.valueOf(value) }.getOrDefault(PendingState.SYNCED)
}

/**
 * The local ledger.
 *
 * Encrypted with SQLCipher: this is the user's whole financial history, and a
 * plain SQLite file is readable by any backup extraction or rooted shell. The
 * key lives in the app's encrypted preference store, which is what
 * `TokenManager` already uses for the session.
 *
 * `fallbackToDestructiveMigration` is deliberately NOT enabled. Losing the local
 * mirror silently would discard queued writes the user made offline; a schema
 * change that cannot migrate should fail loudly in development instead.
 */
@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        GoalEntity::class,
        DebtEntity::class,
        ScheduledEntity::class,
        PendingMutationEntity::class,
        ConflictEntity::class,
        SyncStateEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(PendingStateConverter::class)
abstract class EfinaDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun goalDao(): GoalDao
    abstract fun debtDao(): DebtDao
    abstract fun scheduledDao(): ScheduledDao
    abstract fun pendingMutationDao(): PendingMutationDao
    abstract fun conflictDao(): ConflictDao
    abstract fun syncStateDao(): SyncStateDao

    companion object {
        const val NAME = "efina.db"

        fun build(context: Context, passphrase: ByteArray): EfinaDatabase {
            SQLiteDatabase.loadLibs(context)
            val factory = SupportFactory(passphrase)
            return Room.databaseBuilder(context, EfinaDatabase::class.java, NAME)
                .openHelperFactory(factory)
                .build()
        }
    }
}
