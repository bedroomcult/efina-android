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
) {
    val showOfflineBanner: Boolean get() = !online
    val showReauthBanner: Boolean get() = needsReauth && pendingCount > 0
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

    val state: StateFlow<SyncUiState> = combine(
        connectivity.observe(),
        db.pendingMutationDao().observeCount(),
        db.conflictDao().observeCount(),
        db.syncStateDao().observe(),
        syncing,
    ) { online, pending, conflicts, syncState, isSyncing ->
        SyncUiState(
            online = online,
            syncing = isSyncing,
            pendingCount = pending,
            conflictCount = conflicts,
            needsReauth = syncState?.needsReauth == true,
            lastSyncedAt = syncState?.lastSyncedAt,
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
            try {
                engine.sync()
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
