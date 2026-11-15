package com.efina.app.data.sync

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Builds the HTTP request that replays a queued mutation.
 *
 * Extracted from [SyncEngine] so the wire contract can be asserted against a
 * real HTTP server. The three headers here are the whole safety story for a
 * retried write, and they are invisible until something actually goes wrong:
 *
 *  - `Idempotency-Key` is generated once when the mutation is queued and reused
 *    on every attempt. Regenerating it would let a retry duplicate a create.
 *  - `X-Base-Sync-Seq` is the version the edit was based on. Without it the
 *    server cannot detect a lost update and would overwrite a newer change.
 *  - `Authorization` and `X-CSRF-Token` are the session; the CSRF JWT is what
 *    the server checks on every state-changing request.
 */
object ReplayRequest {

    fun build(
        baseUrl: String,
        method: String,
        path: String,
        payloadJson: String,
        idempotencyKey: String,
        token: String?,
        csrfToken: String?,
        baseSyncSeq: Long?,
    ): Request {
        val url = baseUrl.trimEnd('/') + "/" + path.trimStart('/')

        // A DELETE carries no body; sending JSON for it would be a lie about the
        // request, and some proxies rewrite bodies on verbs that should not have
        // one.
        val body = if (method.equals("DELETE", ignoreCase = true)) {
            "".toRequestBody("application/json".toMediaType())
        } else {
            payloadJson.toRequestBody("application/json".toMediaType())
        }

        val builder = Request.Builder()
            .url(url)
            .method(method, body)
            .header("Content-Type", "application/json")
            .header("Idempotency-Key", idempotencyKey)

        token?.let { builder.header("Authorization", "Bearer $it") }
        csrfToken?.let { builder.header("X-CSRF-Token", it) }
        baseSyncSeq?.let { builder.header("X-Base-Sync-Seq", it.toString()) }

        return builder.build()
    }
}
