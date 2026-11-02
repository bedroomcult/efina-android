package com.efina.app.data.remote.dto

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * LenientBoolean exists because D1 returns 0/1 for boolean columns while
 * kotlinx's own lenient mode accepts only the tokens true/false. A plain Boolean
 * threw on every response carrying a 0 or 1, which made registration report
 * failure after the server had already created the account.
 */
class LenientBooleanTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun `parses unquoted sqlite integers`() {
        assertFalse(LenientBoolean.parse(JsonPrimitive(0)))
        assertTrue(LenientBoolean.parse(JsonPrimitive(1)))
        assertTrue(LenientBoolean.parse(JsonPrimitive(7)))   // any non-zero
    }

    @Test
    fun `parses real json booleans`() {
        assertTrue(LenientBoolean.parse(JsonPrimitive(true)))
        assertFalse(LenientBoolean.parse(JsonPrimitive(false)))
    }

    @Test
    fun `parses quoted forms`() {
        assertTrue(LenientBoolean.parse(JsonPrimitive("true")))
        assertTrue(LenientBoolean.parse(JsonPrimitive("1")))
        assertFalse(LenientBoolean.parse(JsonPrimitive("false")))
        assertFalse(LenientBoolean.parse(JsonPrimitive("0")))
    }

    @Test
    fun `decodes the exact register payload shape`() {
        // functions/api/auth/register.js returns these two as bare 0.
        val user = json.decodeFromString(
            UserDto.serializer(),
            """{"id":1,"email":"a@b.c","has_seen_tutorial":0,"force_onboarding":0}"""
        )
        assertFalse(user.has_seen_tutorial)
        assertFalse(user.force_onboarding)
    }

    @Test
    fun `decodes a GET user payload with 1s`() {
        // GET /user selects the raw D1 columns, which come back as 1.
        val user = json.decodeFromString(
            UserDto.serializer(),
            """{"id":2,"email":"b@c.d","has_seen_tutorial":1,"force_onboarding":1}"""
        )
        assertTrue(user.has_seen_tutorial)
        assertTrue(user.force_onboarding)
    }

    @Test
    fun `absent fields fall back to the declared defaults`() {
        val user = json.decodeFromString(UserDto.serializer(), """{"id":3,"email":"c@d.e"}""")
        assertFalse(user.has_seen_tutorial)
        assertFalse(user.force_onboarding)
        assertEquals(0, user.account_count)
    }
}
