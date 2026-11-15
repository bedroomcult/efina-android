package com.efina.app.data.sync

/**
 * What to do with a replayed write, decided from the response alone.
 *
 * Split out of [SyncEngine] so it can be tested on the JVM without an Android
 * runtime, a database or a socket. The rules are the part most likely to be
 * wrong in a way that costs data, so they are worth testing exhaustively.
 */
enum class SyncAction {
    /** The write landed. */
    Done,

    /** Someone else changed the row first. Park it for the user. */
    Conflict,

    /** The row is already gone; the user's intent is satisfied. Not a conflict. */
    Gone,

    /** The session died. Keep the queue and ask the user to sign in. */
    NeedsReauth,

    /** CSRF token expired. Refresh and retry once. */
    RefreshCsrf,

    /** Transient. Keep the mutation and try again later. */
    Retry,
}

object SyncClassifier {

    /**
     * Classify a replayed write.
     *
     * Order matters. `404` only means "already gone" for an update or a delete:
     * a `404` on a create means the account or category it referenced is gone,
     * which is a failure the user must see, not something to drop.
     *
     * The CSRF branch is checked before the generic 4xx fallback, and only once —
     * a second `403` after a refresh is a real refusal, not a stale token.
     */
    fun classify(
        status: Int,
        method: String,
        body: String,
        isRetry: Boolean = false,
    ): SyncAction = when {
        status in 200..299 -> SyncAction.Done
        status == 409 -> SyncAction.Conflict
        status == 404 && !method.equals("POST", ignoreCase = true) -> SyncAction.Gone
        status == 401 -> SyncAction.NeedsReauth
        status == 403 && body.contains("CSRF", ignoreCase = true) && !isRetry ->
            SyncAction.RefreshCsrf
        status in 500..599 -> SyncAction.Retry
        else -> SyncAction.Conflict
    }
}
