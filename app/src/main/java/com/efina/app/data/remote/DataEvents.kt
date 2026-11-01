package com.efina.app.data.remote

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * "This data changed elsewhere."
 *
 * Every ViewModel loads once in `init` and Android keeps that instance alive
 * while its destination sits on the back stack, so after creating a transaction
 * and popping back the list showed stale rows — money moved and the screen did
 * not. There is no shared cache to invalidate, so screens announce writes and
 * each list re-reads on return.
 *
 * Deliberately coarse: one signal per domain rather than fine-grained keys. With
 * eleven screens a targeted invalidation graph would be more machinery than the
 * problem warrants, and the reloads are single requests.
 *
 * replay = 1 for the same reason as SessionEvents: the collector may attach after
 * the emission when a screen is being recreated.
 */
object DataEvents {

    enum class Domain { ACCOUNTS, TRANSACTIONS, BUDGETS, GOALS, CATEGORIES, FAMILY, DEBTS, SCHEDULED }

    private val _changed = MutableSharedFlow<Domain>(replay = 8, extraBufferCapacity = 8)
    val changed: SharedFlow<Domain> = _changed.asSharedFlow()

    fun emit(domain: Domain) {
        _changed.tryEmit(domain)
    }
}
