package com.efina.app.data.remote.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the request/response shapes that were wrong.
 *
 * Each case encodes a payload the real backend sends, or asserts the JSON the
 * client sends uses the names the handler actually reads. Without these the same
 * defects can reappear silently: `ignoreUnknownKeys` turns a renamed response field
 * into a null, and a renamed request field turns into a 400 or a 500 that the
 * compiler never sees.
 *
 * Backend sources: functions/api/debts, .../scheduled, .../goals, .../accounts.
 */
@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
class DtoShapeTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        isLenient = true
        coerceInputValues = true
        // Mirrors the production Json in AppModule: without this, defaults are
        // omitted from encoded bodies and the assertions below would be vacuous.
        encodeDefaults = true
    }

    @Test
    fun `debt list is wrapped in a debts envelope`() {
        // GET /debts returns { debts: [...] }, not a bare array.
        val page = json.decodeFromString(
            DebtsResponseDto.serializer(),
            """{"debts":[{"id":1,"person_name":"Ana","total_amount":500.0,"remaining_amount":200.0}]}"""
        )
        assertEquals(1, page.debts.size)
        assertEquals("Ana", page.debts[0].person_name)
        assertEquals(500.0, page.debts[0].total_amount, 0.001)
    }

    @Test
    fun `create debt sends person_name and description`() {
        // debts/index.js destructures person_name and reads description.
        val body = json.encodeToString(
            CreateDebtRequestDto.serializer(),
            CreateDebtRequestDto(person_name = "Ana", amount = 10.0, description = "lunch")
        )
        assertTrue(body.contains("\"person_name\""))
        assertTrue(body.contains("\"description\""))
        assertTrue(!body.contains("\"name\""))
        assertTrue(!body.contains("\"notes\""))
    }

    @Test
    fun `scheduled create sends next_run_date`() {
        // scheduled/index.js rejects a body without next_run_date.
        val body = json.encodeToString(
            CreateScheduledRequestDto.serializer(),
            CreateScheduledRequestDto(
                account_id = 1, amount = 5.0, type = "expense", next_run_date = "2026-01-01"
            )
        )
        assertTrue("expected next_run_date in $body", body.contains("\"next_run_date\""))
        assertTrue("must not send next_date", !body.contains("\"next_date\""))
    }

    @Test
    fun `goal create sends period_type and date-only dates`() {
        val body = json.encodeToString(
            CreateGoalRequestDto.serializer(),
            CreateGoalRequestDto(
                name = "Holiday",
                target_amount = 100.0,
                start_date = "2026-01-01",
                target_date = "2026-12-31"
            )
        )
        assertTrue("period_type required by goals/index.js", body.contains("\"period_type\""))
        // The handler validates /^\d{4}-\d{2}-\d{2}$/; a time component is a 400.
        assertTrue("dates must not carry a time", !body.contains("T00:00:00"))
    }

    @Test
    fun `categories keep their nested children`() {
        // GET /categories nests children one level deep; dropping them hid every
        // leaf category (Cafe, Fuel, Salary) from the UI.
        val roots = json.decodeFromString(
            kotlinx.serialization.builtins.ListSerializer(CategoryDto.serializer()),
            """[{"id":1,"name":"Food","subcategories":[{"id":2,"name":"Cafe"}]}]"""
        )
        assertEquals(1, roots.size)
        assertEquals(1, roots[0].subcategories.size)
        assertEquals("Cafe", roots[0].subcategories[0].name)
    }

    @Test
    fun `account balance edit always sends update_strategy`() {
        // accounts/[id].js only writes the reconciliation transaction when
        // update_strategy is present; without it the PUT takes the plain-UPDATE
        // fallback and the edit silently skips the ledger, leaving initial_balance
        // stale and the account out of step with its transactions.
        val body = json.encodeToString(
            UpdateAccountRequestDto.serializer(),
            com.efina.app.domain.model.UpdateAccountRequest(
                name = "Cash",
                type = com.efina.app.domain.model.AccountType.CASH,
                balance = 100.0,
                currency = "IDR"
            ).toDto()
        )
        assertTrue("update_strategy missing from $body", body.contains("\"update_strategy\""))
        assertTrue(body.contains("adjustment"))
    }

    @Test
    fun `budget carries family scope and decodes from the backend row`() {
        // GET budgets returns the raw row; a shared budget carries family_id and is_shared,
        // a personal one sends family_id null and is_shared 0.
        val shared = json.decodeFromString(
            BudgetDto.serializer(),
            """{"id":1,"user_id":1,"category_id":2,"amount":1000.0,"period_type":"monthly",
                "family_id":7,"is_shared":1,"spent":300.0}"""
        )
        assertTrue("shared budget must decode as shared", shared.is_shared == 1)
        assertEquals(7L, shared.family_id)

        val personal = json.decodeFromString(
            BudgetDto.serializer(),
            """{"id":2,"user_id":1,"category_id":2,"amount":500.0,"period_type":"monthly",
                "family_id":null,"is_shared":0}"""
        )
        assertTrue("personal budget must not decode as shared", personal.is_shared == 0)
        assertEquals(null, personal.family_id)
    }

    @Test
    fun `budget share request sends family_id and allows null`() {
        // PATCH budgets/{id} reads { family_id }. Sharing sends the id; unsharing sends
        // null, which the handler treats as "make personal again".
        val share = json.encodeToString(
            UpdateBudgetSharingRequestDto.serializer(),
            UpdateBudgetSharingRequestDto(7L)
        )
        assertTrue("expected family_id in $share", share.contains("\"family_id\""))
        assertTrue(share.contains("7"))

        val unshare = json.encodeToString(
            UpdateBudgetSharingRequestDto.serializer(),
            UpdateBudgetSharingRequestDto(null)
        )
        assertTrue("unshare must still carry the key: $unshare", unshare.contains("\"family_id\""))
        assertTrue("unshare must be null: $unshare", unshare.contains("null"))
    }

    @Test
    fun `sharing result parses both PATCH branches`() {
        // Share returns a full row with is_shared as SQLite 1; unshare returns a stub.
        val shared = json.decodeFromString(
            SharingResultDto.serializer(),
            """{"id":7,"name":"Cash","is_shared":1,"family_id":3}"""
        )
        assertTrue("SQLite 1 must parse as true", shared.is_shared)
        assertEquals(7L, shared.id)

        val unshared = json.decodeFromString(
            SharingResultDto.serializer(),
            """{"is_shared":false,"family_id":null}"""
        )
        assertTrue(!unshared.is_shared)
    }
}
