package com.efina.app.data.remote

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * One place that announces "the session is gone".
 *
 * The contract requires that a 401 logs the user out, mirroring apiFetch.ts in
 * the web client (which dispatches an `auth:unauthorized` window event). Without
 * a central signal, only the screen that happened to make the failing call would
 * notice, and every other tab would keep showing stale data behind a dead token.
 *
 * Emitted from Response.toResult() on status 401, which every repository call
 * already funnels through, so no call site can forget. AuthViewModel collects it.
 *
 * Deliberately a plain object rather than an injected dependency: `toResult()` is
 * a top-level extension used from nine repositories, and threading a constructor
 * dependency through all of them to publish one event would be more machinery
 * than the problem deserves.
 */
object SessionEvents {

    private val _unauthorized = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1
    )

    /** Collected once by AuthViewModel; every 401 emission means "log out". */
    val unauthorized: SharedFlow<Unit> = _unauthorized.asSharedFlow()

    fun emitUnauthorized() {
        _unauthorized.tryEmit(Unit)
    }
}
