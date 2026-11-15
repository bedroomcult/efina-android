package com.efina.app.data.sync

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The sync failure policy.
 *
 * These rules decide whether a queued write is retried, parked for the user, or
 * quietly dropped. A wrong answer costs data, so every branch is pinned here —
 * and the table is checked without an Android runtime, a database or a socket.
 */
class SyncClassifierTest {

    private fun classify(status: Int, method: String = "PUT", body: String = "", retry: Boolean = false) =
        SyncClassifier.classify(status, method, body, retry)

    // ---- success -----------------------------------------------------------

    @Test
    fun `2xx is done`() {
        for (status in listOf(200, 201, 204)) {
            assertEquals("status $status", SyncAction.Done, classify(status))
        }
    }

    // ---- conflicts ---------------------------------------------------------

    @Test
    fun `409 is a conflict`() {
        assertEquals(SyncAction.Conflict, classify(409))
    }

    @Test
    fun `409 on a create is also a conflict`() {
        assertEquals(SyncAction.Conflict, classify(409, method = "POST"))
    }

    // ---- already gone ------------------------------------------------------

    @Test
    fun `404 on update or delete is gone`() {
        assertEquals(SyncAction.Gone, classify(404, method = "PUT"))
        assertEquals(SyncAction.Gone, classify(404, method = "DELETE"))
    }

    @Test
    fun `404 on a create is NOT gone - it means a referenced row vanished`() {
        // The account or category the create pointed at no longer exists. That is
        // a failure the user must see, not a write to drop silently.
        assertEquals(SyncAction.Conflict, classify(404, method = "POST"))
    }

    // ---- auth --------------------------------------------------------------

    @Test
    fun `401 needs re-auth`() {
        assertEquals(SyncAction.NeedsReauth, classify(401))
    }

    @Test
    fun `403 mentioning CSRF refreshes once`() {
        assertEquals(SyncAction.RefreshCsrf, classify(403, body = """{"error":"CSRF token invalid"}"""))
    }

    @Test
    fun `403 mentioning CSRF again after a refresh is a real refusal`() {
        // A second 403 after refreshing means the token is fine and the server
        // said no. Retrying forever would spin.
        assertEquals(
            SyncAction.Conflict,
            classify(403, body = """{"error":"CSRF token invalid"}""", retry = true),
        )
    }

    @Test
    fun `403 without CSRF is a refusal`() {
        assertEquals(SyncAction.Conflict, classify(403, body = """{"error":"You have view-only access"}"""))
    }

    @Test
    fun `CSRF match is case-insensitive`() {
        assertEquals(SyncAction.RefreshCsrf, classify(403, body = "csrf token expired"))
    }

    // ---- transient ---------------------------------------------------------

    @Test
    fun `5xx retries`() {
        for (status in listOf(500, 502, 503, 504)) {
            assertEquals("status $status", SyncAction.Retry, classify(status))
        }
    }

    @Test
    fun `other 4xx parks as a conflict so the user sees why`() {
        assertEquals(SyncAction.Conflict, classify(400))
        assertEquals(SyncAction.Conflict, classify(422))
    }

    @Test
    fun `status 0 - a transport failure - retries`() {
        // SyncEngine maps an IOException to Retry before reaching the classifier,
        // so a 0 must never be treated as success or a conflict.
        assertEquals(SyncAction.Conflict, classify(0))
    }

    // ---- ordering ----------------------------------------------------------

    @Test
    fun `401 wins over the generic 4xx fallback`() {
        assertEquals(SyncAction.NeedsReauth, classify(401, body = "CSRF"))
    }

    @Test
    fun `409 wins over the CSRF branch`() {
        assertEquals(SyncAction.Conflict, classify(409, body = "CSRF"))
    }

    @Test
    fun `done wins over everything`() {
        assertEquals(SyncAction.Done, classify(200, body = "CSRF", retry = true))
    }
}
