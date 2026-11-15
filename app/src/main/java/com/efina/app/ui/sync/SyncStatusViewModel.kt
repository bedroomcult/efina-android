package com.efina.app.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efina.app.data.local.EfinaDatabase
import com.efina.app.data.sync.ConnectivityObserver
import com.efina.app.data.sync.SyncEngine
import com.efina.app.data.sync.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * What the shell needs to know about sync, in one place.
 *
 * The counts come from Room, so they update the moment a write is queued or a
 * flush clears it — no polling and no manual refresh.
 */
data class SyncUiState(
    val online: Boolean = true,
    val syncing: Boolean = false,
    val pendingCount: Int = 0,
    val conflictCount: Int = 0,
    val needsReauth: Boolean = false,
    val lastSyncedAt: Long? = null,
    /** Why the last attempt failed, if it did. Drives the retry UI. */
    val lastError: String? = null,
) {
    val showOfflineBanner: Boolean get() = !online
    val showReauthBanner: Boolean get() = needsReauth && pendingCount > 0

    /** The first sync has not landed and the last attempt failed. */
    val firstSyncFailed: Boolean get() = lastSyncedAt == null && lastError != null
}

@HiltViewModel
class SyncStatusViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: EfinaDatabase,
    private val connectivity: ConnectivityObserver,
    private val engine: SyncEngine,
    private val preferences: com.efina.app.util.PreferencesManager,
) : ViewModel() {

    private val syncing = MutableStateFlow(false)
    private val lastError = MutableStateFlow<String?>(null)

    // Combined in two steps: Kotlin's combine has typed overloads only up to
    // five flows, and the outer one already needs four.
    private val liveState = combine(
        connectivity.observe(),
        syncing,
        lastError,
    ) { online, isSyncing, error -> Triple(online, isSyncing, error) }

    val state: StateFlow<SyncUiState> = combine(
        liveState,
        db.pendingMutationDao().observeCount(),
        db.conflictDao().observeCount(),
        db.syncStateDao().observe(),
    ) { live, pending, conflicts, syncState ->
        SyncUiState(
            online = live.first,
            syncing = live.second,
            pendingCount = pending,
            conflictCount = conflicts,
            needsReauth = syncState?.needsReauth == true,
            lastSyncedAt = syncState?.lastSyncedAt,
            lastError = live.third,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SyncUiState(online = connectivity.isOnline()),
    )

    /**
     * Flush now.
     *
     * Runs the engine inline rather than only enqueuing the worker, so a
     * pull-to-refresh or a "Sync now" tap gives immediate feedback instead of
     * waiting for WorkManager to schedule.
     */
    fun syncNow() {
        if (syncing.value) return
        viewModelScope.launch {
            syncing.value = true
            lastError.value = null
            try {
                // The outcome was previously discarded, which made the first-sync
                // retry unreachable: lastSyncedAt stayed null and nothing ever
                // set an error, so the screen sat on "Preparing your data".
                val outcome = engine.sync()
                lastError.value = when {
                    outcome.disabled -> "Sync is paused. Turn it on in Settings."
                    outcome.needsReauth -> "Your session expired. Sign in again to sync."
                    outcome.error != null -> outcome.error
                    outcome.retryLater -> "Could not reach the server. Check your connection."
                    else -> null
                }
            } catch (e: Exception) {
                lastError.value = e.message ?: "Could not sync"
            } finally {
                syncing.value = false
            }
        }
    }

    /**
     * The kill switch.
     *
     * Stopping sync does not touch the queue. Queued writes are the user's data,
     * and a switch labelled "sync" must not be the thing that deletes them.
     */
    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setOfflineSyncEnabled(enabled)
            if (enabled) SyncWorker.enqueue(context)
        }
    }

    /** Queue a background flush; used when connectivity returns. */
    fun requestBackgroundSync() {
        SyncWorker.enqueue(context)
    }
}
