package com.efina.app.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.efina.app.ui.accounts.AccountFormScreen
import com.efina.app.ui.accounts.AccountSharingScreen
import com.efina.app.ui.accounts.AccountsScreen
import com.efina.app.ui.accounts.AccountsViewModel
import com.efina.app.ui.auth.AuthViewModel
import com.efina.app.ui.auth.LoginScreen
import com.efina.app.ui.budgets.BudgetsScreen
import com.efina.app.ui.budgets.GoalsScreen
import com.efina.app.ui.categories.CategoriesScreen
import com.efina.app.ui.debts.DebtsScreen
import com.efina.app.ui.scheduled.ScheduledScreen
import com.efina.app.ui.family.FamilyScreen
import com.efina.app.ui.home.HomeScreen
import com.efina.app.ui.settings.SettingsScreen
import com.efina.app.ui.stats.StatsScreen
import com.efina.app.ui.transactions.TransactionFormScreen
import com.efina.app.ui.transactions.TransactionsScreen
import com.efina.app.ui.transactions.TransactionsViewModel
import com.efina.app.data.remote.DataEvents
import com.efina.app.ui.theme.EfinaTheme
import com.efina.app.ui.theme.ThemePreference

/**
 * Navigation graph.
 *
 * Routes mirror the web app's primary destinations (Dashboard, Transactions,
 * Statistics, Profile/Settings, Family) plus the money-path detail screens.
 * Budgets and Goals are top-level here rather than tabs, because the reference
 * design favours a short bottom bar; PARITY.md records that.
 */

/**
 * Re-reads a screen's data when a write happens anywhere in [domains].
 *
 * Every ViewModel loads once in `init`, and Android keeps that instance alive
 * while its destination sits on the back stack. Without this, creating a
 * transaction and popping back showed the old list — money moved and the screen
 * did not.
 */
@Composable
private fun ReloadOnChange(vararg domains: DataEvents.Domain, reload: () -> Unit) {
    LaunchedEffect(domains.toList()) {
        DataEvents.changed.collect { changed ->
            if (changed in domains) reload()
        }
    }
}

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Home : Screen("home")
    data object Accounts : Screen("accounts")
    data object AccountForm : Screen("account_form?accountId={accountId}") {
        fun route(accountId: Long?) =
            if (accountId == null) "account_form" else "account_form?accountId=$accountId"
    }
    data object AccountSharing : Screen("account_sharing/{accountId}") {
        fun route(id: Long) = "account_sharing/$id"
    }
    data object Transactions : Screen("transactions")
    data object TransactionForm : Screen("transaction_form?transactionId={transactionId}") {
        fun route(id: Long?) =
            if (id == null) "transaction_form" else "transaction_form?transactionId=$id"
    }
    data object Budgets : Screen("budgets")
    data object Goals : Screen("goals")
    data object Stats : Screen("stats")
    data object Settings : Screen("settings")
    data object Categories : Screen("categories")
    data object Family : Screen("family")
    data object Debts : Screen("debts")
    data object Scheduled : Screen("scheduled")
}

private data class NavItem(val screen: Screen, val icon: ImageVector, val label: String)

private val bottomNavItems = listOf(
    NavItem(Screen.Home, Icons.Default.Home, "Home"),
    NavItem(Screen.Transactions, Icons.Default.Receipt, "Activity"),
    NavItem(Screen.Budgets, Icons.Default.PieChart, "Budgets"),
    NavItem(Screen.Stats, Icons.Default.BarChart, "Stats"),
    NavItem(Screen.Settings, Icons.Default.Person, "Profile")
)

@Composable
fun EfinaAppRoot(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = hiltViewModel(),
    /** Route resolved from the launch Intent or a later onNewIntent. */
    deepLinkRoute: StateFlow<String?> = MutableStateFlow(null)
) {
    val authState by authViewModel.state.collectAsState()

    // Wait for the stored session to be read before deciding the start screen,
    // otherwise a logged-in user sees a flash of the login form.
    val (themeName, darkTheme) = ThemePreference.current()

    if (!authState.checked) {
        EfinaTheme(
            themeName = themeName,
            darkTheme = darkTheme,
            dynamicColor = themeName == "dynamic"
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        return
    }

    val startDestination = if (authState.isLoggedIn) Screen.Home.route else Screen.Login.route

    // Apply an inbound deep link once the graph exists and the user is signed in.
    // Keyed on the StateFlow so a link arriving while the app runs also navigates.
    val link by deepLinkRoute.collectAsState()
    LaunchedEffect(authState.isLoggedIn, link) {
        val target = link
        if (authState.isLoggedIn && target != null) {
            navController.navigate(target)
        }
    }

    EfinaTheme(themeName = themeName, darkTheme = darkTheme) {
        Scaffold(
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val showBar = bottomNavItems.any { it.screen.route == currentRoute }

                if (showBar) {
                    NavigationBar {
                        bottomNavItems.forEach { item ->
                            NavigationBarItem(
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) },
                                selected = currentRoute == item.screen.route,
                                onClick = {
                                    if (currentRoute != item.screen.route) {
                                        navController.navigate(item.screen.route) {
                                            popUpTo(Screen.Home.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(padding)
            ) {
                composable(Screen.Login.route) {
                    LoginScreen(
                        onLoginSuccess = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        },
                        onNavigateToRegister = { }
                    )
                }

                composable(Screen.Home.route) {
                    val homeVm: com.efina.app.ui.home.HomeViewModel = hiltViewModel()
                    ReloadOnChange(DataEvents.Domain.TRANSACTIONS, DataEvents.Domain.ACCOUNTS, DataEvents.Domain.BUDGETS, DataEvents.Domain.GOALS) { homeVm.refresh() }
                    HomeScreen(
                        onNavigateToAccounts = { navController.navigate(Screen.Accounts.route) },
                        onNavigateToTransactions = { navController.navigate(Screen.Transactions.route) },
                        onNavigateToBudgets = { navController.navigate(Screen.Budgets.route) },
                        onNavigateToGoals = { navController.navigate(Screen.Goals.route) },
                        onNavigateToStats = { navController.navigate(Screen.Stats.route) },
                        onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                    )
                }

                composable(Screen.Accounts.route) {
                    val accVm: AccountsViewModel = hiltViewModel()
                    ReloadOnChange(DataEvents.Domain.ACCOUNTS) { accVm.load() }
                    AccountsScreen(
                        onNavigateToCreate = { navController.navigate(Screen.AccountForm.route(null)) },
                        onNavigateToEdit = { id -> navController.navigate(Screen.AccountForm.route(id)) },
                        onNavigateToShare = { id ->
                            navController.navigate(Screen.AccountSharing.route(id))
                        }
                    )
                }

                composable(
                    route = Screen.AccountForm.route,
                    arguments = listOf(
                        navArgument("accountId") {
                            type = NavType.LongType
                            defaultValue = -1L
                        }
                    )
                ) { entry ->
                    val id = entry.arguments?.getLong("accountId")?.takeIf { it > 0 }
                    val accountsVm: AccountsViewModel = hiltViewModel()
                    val accountsState by accountsVm.state.collectAsState()
                    val existing = accountsState.accounts.firstOrNull { it.id == id }
                    // Fetch once so edit mode has the row before first composition.
                    LaunchedEffect(id) { if (id != null && existing == null) accountsVm.load() }

                    AccountFormScreen(
                        account = existing,
                        onNavigateBack = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() },
                        viewModel = accountsVm
                    )
                }

                composable(
                    route = Screen.AccountSharing.route,
                    arguments = listOf(navArgument("accountId") { type = NavType.LongType })
                ) { entry ->
                    val id = entry.arguments?.getLong("accountId") ?: return@composable
                    AccountSharingScreen(
                        accountId = id,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Transactions.route) {
                    val txListVm: TransactionsViewModel = hiltViewModel()
                    ReloadOnChange(DataEvents.Domain.TRANSACTIONS, DataEvents.Domain.ACCOUNTS) { txListVm.load() }
                    TransactionsScreen(
                        onNavigateToCreate = { navController.navigate(Screen.TransactionForm.route(null)) },
                        onNavigateToEdit = { id ->
                            navController.navigate(Screen.TransactionForm.route(id))
                        }
                    )
                }

                composable(
                    route = Screen.TransactionForm.route,
                    arguments = listOf(
                        navArgument("transactionId") {
                            type = NavType.LongType
                            defaultValue = -1L
                        }
                    )
                ) { entry ->
                    val id = entry.arguments?.getLong("transactionId")?.takeIf { it > 0 }
                    val txVm: TransactionsViewModel = hiltViewModel()
                    val txState by txVm.state.collectAsState()
                    val existing = txState.transactions.firstOrNull { it.id == id }
                    LaunchedEffect(id) { if (id != null && existing == null) txVm.load() }

                    TransactionFormScreen(
                        existing = existing,
                        onNavigateBack = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() },
                        viewModel = txVm
                    )
                }

                composable(Screen.Budgets.route) {
                    val bVm: com.efina.app.ui.budgets.BudgetsViewModel = hiltViewModel()
                    ReloadOnChange(DataEvents.Domain.BUDGETS, DataEvents.Domain.TRANSACTIONS) { bVm.load() }
                    BudgetsScreen(viewModel = bVm)
                }
                composable(Screen.Goals.route) {
                    val gVm: com.efina.app.ui.budgets.GoalsViewModel = hiltViewModel()
                    ReloadOnChange(DataEvents.Domain.GOALS, DataEvents.Domain.ACCOUNTS) { gVm.load() }
                    GoalsScreen(viewModel = gVm)
                }
                composable(Screen.Stats.route) {
                    val sVm: com.efina.app.ui.stats.StatsViewModel = hiltViewModel()
                    ReloadOnChange(DataEvents.Domain.TRANSACTIONS) { sVm.load() }
                    StatsScreen(viewModel = sVm)
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToCategories = { navController.navigate(Screen.Categories.route) },
                        onNavigateToFamily = { navController.navigate(Screen.Family.route) },
                        onNavigateToDebts = { navController.navigate(Screen.Debts.route) },
                        onNavigateToScheduled = { navController.navigate(Screen.Scheduled.route) },
                        onLogout = {
                            authViewModel.logout()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Categories.route) {
                    CategoriesScreen(onNavigateBack = { navController.popBackStack() })
                }

                composable(Screen.Family.route) {
                    FamilyScreen(onNavigateBack = { navController.popBackStack() })
                }

                composable(Screen.Debts.route) {
                    DebtsScreen(onNavigateBack = { navController.popBackStack() })
                }

                composable(Screen.Scheduled.route) {
                    ScheduledScreen(onNavigateBack = { navController.popBackStack() })
                }
            }
        }
    }
}
