package com.efina.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.efina.app.ui.accounts.AccountsScreen
import com.efina.app.ui.accounts.CreateAccountScreen
import com.efina.app.ui.auth.LoginScreen
import com.efina.app.ui.auth.AuthViewModel
import com.efina.app.ui.budgets.BudgetsScreen
import com.efina.app.ui.budgets.GoalsScreen
import com.efina.app.ui.home.HomeScreen
import com.efina.app.ui.transactions.CreateTransactionScreen
import com.efina.app.ui.transactions.TransactionsScreen

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Home : Screen("home")
    data object Accounts : Screen("accounts")
    data object CreateAccount : Screen("create_account")
    data object Transactions : Screen("transactions")
    data object CreateTransaction : Screen("create_transaction")
    data object Transfer : Screen("transfer")
    data object Budgets : Screen("budgets")
    data object Goals : Screen("goals")
    data object Stats : Screen("stats")
    data object Chat : Screen("chat")
    data object Settings : Screen("settings")
}

sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    data object Home : BottomNavItem("home", Icons.Default.Home, "Home")
    data object Accounts : BottomNavItem("accounts", Icons.Default.AccountBalanceWallet, "Accounts")
    data object Transactions : BottomNavItem("transactions", Icons.Default.Receipt, "Transactions")
    data object Budgets : BottomNavItem("budgets", Icons.Default.PieChart, "Budgets")
    data object Stats : BottomNavItem("stats", Icons.Default.BarChart, "Stats")
}

@Composable
fun EfinaAppRoot(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState(initial = null)
    
    if (isLoggedIn == null) {
        return
    }
    
    val startDestination = if (isLoggedIn == true) Screen.Home.route else Screen.Login.route
    
    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            
            // Only show bottom nav for main screens
            val showBottomNav = currentRoute in listOf(
                Screen.Home.route,
                Screen.Accounts.route,
                Screen.Transactions.route,
                Screen.Budgets.route,
                Screen.Stats.route
            )
            
            val bottomNavItems = listOf(
                BottomNavItem.Home,
                BottomNavItem.Accounts,
                BottomNavItem.Transactions,
                BottomNavItem.Budgets,
                BottomNavItem.Stats
            )
            
            if (showBottomNav) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentRoute == item.route,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(Screen.Home.route) {
                                            saveState = true
                                        }
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
            // Auth
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(Screen.Register.route)
                    },
                    onNavigateToPinLogin = { }
                )
            }
            
            // Home
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToAccounts = { navController.navigate(Screen.Accounts.route) },
                    onNavigateToTransactions = { navController.navigate(Screen.Transactions.route) },
                    onNavigateToBudgets = { navController.navigate(Screen.Budgets.route) },
                    onNavigateToGoals = { navController.navigate(Screen.Goals.route) },
                    onNavigateToStats = { navController.navigate(Screen.Stats.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            
            // Accounts
            composable(Screen.Accounts.route) {
                AccountsScreen(
                    onNavigateToCreate = { navController.navigate(Screen.CreateAccount.route) },
                    onNavigateToEdit = { }
                )
            }
            
            composable(Screen.CreateAccount.route) {
                CreateAccountScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            // Transactions
            composable(Screen.Transactions.route) {
                TransactionsScreen(
                    onNavigateToCreate = { navController.navigate(Screen.CreateTransaction.route) },
                    onNavigateToEdit = { }
                )
            }
            
            composable(Screen.CreateTransaction.route) {
                CreateTransactionScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            // Budgets
            composable(Screen.Budgets.route) {
                BudgetsScreen(
                    onNavigateToCreate = { }
                )
            }
            
            // Goals
            composable(Screen.Goals.route) {
                GoalsScreen(
                    onNavigateToCreate = { }
                )
            }
            
            // Stats
            composable(Screen.Stats.route) {
                StatsPlaceholder()
            }
            
            // Settings
            composable(Screen.Settings.route) {
                SettingsPlaceholder(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            // Chat
            composable(Screen.Chat.route) {
                ChatPlaceholder()
            }
        }
    }
}

@Composable
fun StatsPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Stats Screen - Coming Soon")
    }
}

@Composable
fun SettingsPlaceholder(onNavigateBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Settings - Coming Soon")
            TextButton(onClick = onNavigateBack) {
                Text("Go Back")
            }
        }
    }
}

@Composable
fun ChatPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("AI Chat - Coming Soon")
    }
}
