package com.efina.app.data.remote

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertNull
import org.junit.Test

class SessionEventsTest {

    @Test
    fun `resetUnauthorized clears the replayed event`() = runTest {
        SessionEvents.emitUnauthorized()
        SessionEvents.resetUnauthorized()
        val received = withTimeoutOrNull(100) { SessionEvents.unauthorized.first() }
        assertNull(received)
    }
}
