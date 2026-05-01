package com.efina.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.efina.app.ui.auth.LoginScreen
import com.efina.app.ui.auth.AuthViewModel

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object PinSetup : Screen("pin_setup")
    data object PinLogin : Screen("pin_login")
    data object Onboarding : Screen("onboarding")
    
    data object Home : Screen("home")
    data object Accounts : Screen("accounts")
    data object CreateAccount : Screen("create_account")
    data object EditAccount : Screen("edit_account/{accountId}") {
        fun createRoute(accountId: Long) = "edit_account/$accountId"
    }
    
    data object Transactions : Screen("transactions")
    data object CreateTransaction : Screen("create_transaction")
    data object EditTransaction : Screen("edit_transaction/{transactionId}") {
        fun createRoute(transactionId: Long) = "edit_transaction/$transactionId"
    }
    
    data object Transfer : Screen("transfer")
    
    data object Budgets : Screen("budgets")
    data object CreateBudget : Screen("create_budget")
    data object EditBudget : Screen("edit_budget/{budgetId}") {
        fun createRoute(budgetId: Long) = "edit_budget/$budgetId"
    }
    
    data object Goals : Screen("goals")
    data object CreateGoal : Screen("create_goal")
    data object EditGoal : Screen("edit_goal/{goalId}") {
        fun createRoute(goalId: Long) = "edit_goal/$goalId"
    }
    
    data object Stats : Screen("stats")
    data object Achievements : Screen("achievements")
    data object Leaderboard : Screen("leaderboard")
    
    data object Chat : Screen("chat")
    
    data object Settings : Screen("settings")
}

@Composable
fun EfinaAppRoot(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState(initial = null)
    val hasPin by authViewModel.hasPin.collectAsState(initial = false)
    
    // Wait for auth state to load
    if (isLoggedIn == null) {
        return
    }
    
    val startDestination = when {
        isLoggedIn == true && hasPin == true -> Screen.PinLogin.route
        isLoggedIn == true -> Screen.Home.route
        else -> Screen.Login.route
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Auth screens
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
                onNavigateToPinLogin = {
                    navController.navigate(Screen.PinLogin.route)
                }
            )
        }
        
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        
        // Add remaining screens here...
    }
}

@Composable
fun HomeScreen(navController: NavHostController) {
    // Placeholder - will implement in next step
}
