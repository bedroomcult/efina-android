package com.efina.app.data.sync

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * The replay wire contract, asserted against a real HTTP server.
 *
 * These three headers are the entire safety story for a retried write, and they
 * are invisible until something goes wrong in production. A missing
 * `Idempotency-Key` duplicates a create; a missing `X-Base-Sync-Seq` silently
 * overwrites someone else's newer change.
 *
 * No Android runtime needed: this is OkHttp and a socket.
 */
class ReplayRequestTest {

    private lateinit var server: MockWebServer
    private val client = OkHttpClient()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() = server.shutdown()

    private fun send(
        method: String = "PUT",
        path: String = "transactions/42",
        payload: String = """{"amount":75000}""",
        idempotencyKey: String = "key-1",
        token: String? = "tok",
        csrf: String? = "csrf",
        baseSeq: Long? = 5,
        responseCode: Int = 200,
    ) {
        server.enqueue(MockResponse().setResponseCode(responseCode).setBody("{}"))
        val request = ReplayRequest.build(
            baseUrl = server.url("/api/").toString(),
            method = method,
            path = path,
            payloadJson = payload,
            idempotencyKey = idempotencyKey,
            token = token,
            csrfToken = csrf,
            baseSyncSeq = baseSeq,
        )
        client.newCall(request).execute().close()
    }

    @Test
    fun `sends the idempotency key so a retry cannot duplicate`() {
        send(idempotencyKey = "abc-123")
        assertEquals("abc-123", server.takeRequest().getHeader("Idempotency-Key"))
    }

    @Test
    fun `sends the base version so a lost update is detectable`() {
        send(baseSeq = 17)
        assertEquals("17", server.takeRequest().getHeader("X-Base-Sync-Seq"))
    }

    @Test
    fun `omits the base version when there is none`() {
        send(method = "POST", path = "transactions", baseSeq = null)
        assertNull(server.takeRequest().getHeader("X-Base-Sync-Seq"))
    }

    @Test
    fun `sends the session and csrf headers`() {
        send()
        val recorded = server.takeRequest()
        assertEquals("Bearer tok", recorded.getHeader("Authorization"))
        assertEquals("csrf", recorded.getHeader("X-CSRF-Token"))
    }

    @Test
    fun `posts to the collection and puts to the row`() {
        send(method = "POST", path = "transactions", baseSeq = null)
        val create = server.takeRequest()
        assertEquals("POST", create.method)
        assertEquals("/api/transactions", create.path)

        send(method = "PUT", path = "transactions/42")
        val update = server.takeRequest()
        assertEquals("PUT", update.method)
        assertEquals("/api/transactions/42", update.path)
    }

    @Test
    fun `a delete carries no payload`() {
        send(method = "DELETE", path = "transactions/42", payload = """{"ignored":true}""")
        val recorded = server.takeRequest()
        assertEquals("DELETE", recorded.method)
        assertEquals(0L, recorded.bodySize)
    }

    @Test
    fun `the base path is not doubled`() {
        send(path = "/transactions/42")
        assertEquals("/api/transactions/42", server.takeRequest().path)
    }

    @Test
    fun `a 409 from the server is a conflict, not a success`() {
        send(responseCode = 409)
        // The classifier is what SyncEngine keys off; assert they agree.
        assertEquals(SyncAction.Conflict, SyncClassifier.classify(409, "PUT", "{}"))
    }
}
