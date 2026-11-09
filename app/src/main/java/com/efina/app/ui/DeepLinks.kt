package com.efina.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

/**
 * Deep links and back handling.
 *
 * A navigation graph is not usable on Android without deciding two things, and
 * neither is automatic with Navigation Compose:
 *
 *  1. What an inbound link becomes. The manifest declares app links for
 *     https://efina.himz.my.id/..., https://efina.app/... and efina://app/...
 *     but an intent-filter alone does nothing; the MainActivity must hand the
 *     Intent data to the graph.
 *
 *  2. What the system back button does at the graph root. Without a handler the
 *     activity finishes from the start destination, which loses the session.
 */

/** Paths the web app serves that map onto a native destination. */
private val DEEP_LINK_ROUTES = mapOf(
    "/" to Screen.Home.route,
    "/dashboard" to Screen.Home.route,
    "/transactions" to Screen.Transactions.route,
    "/budgets" to Screen.Budgets.route,
    "/goals" to Screen.Goals.route,
    "/statistics" to Screen.CashFlow.route,
    "/settings" to Screen.Settings.route,
    "/profile" to Screen.Settings.route,
    "/family" to Screen.Family.route,
    "/categories" to Screen.Categories.route,
    "/debts" to Screen.Debts.route,
    "/scheduled" to Screen.Scheduled.route,
    "/accounts" to Screen.Accounts.route
)

/**
 * Convert an inbound URI to a graph route, or null when the path is unknown.
 *
 * Accepts the three schemes the manifest declares and tolerates a hash or query,
 * because the web app serves /family/3 and /transactions?account_id=2 shapes.
 */
fun routeForUri(uri: String?): String? {
    if (uri.isNullOrBlank()) return null

    val path = uri
        // strip scheme + host for the two https hosts
        .removePrefix("https://efina.himz.my.id")
        .removePrefix("https://efina.app")
        .removePrefix("efina://app")
        .substringBefore('?')
        .substringBefore('#')

    if (path.isBlank()) return Screen.Home.route
    val normalised = if (path.startsWith('/')) path else "/$path"

    // Longest-prefix match so /family/3 resolves to the family destination.
    DEEP_LINK_ROUTES.entries
        .filter { normalised == it.key || normalised.startsWith(it.key + "/") }
        .maxByOrNull { it.key.length }
        ?.let { return it.value }

    return null
}

/**
 * Back handling is deliberately absent.
 *
 * Navigation Compose already does the right thing: below the graph root, back
 * pops the previous destination; at the root, back is not consumed and the
 * system finishes the activity, which is the platform convention for leaving an
 * app.
 *
 * An earlier version installed a BackHandler at the root that navigated to Home
 * with popUpTo(inclusive = true). That consumed the event without changing the
 * screen, so the back button became a no-op and the user could never leave the
 * app. The handler was worse than no handler, so it is gone rather than patched.
 *
 * Dialogs and sheets handle back themselves through their Material components,
 * which intercept before the activity sees the event.
 */

