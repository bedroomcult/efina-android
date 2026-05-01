package com.efina.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.efina.app.ui.theme.Success

@Composable
fun HomeScreen(
    onNavigateToAccounts: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToGoals: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Stats Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Total Balance",
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = "Rp 12,500,000",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Success
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Income",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "Rp 5,000,000",
                            style = MaterialTheme.typography.titleMedium,
                            color = Success
                        )
                    }
                    Column {
                        Text(
                            text = "Expense",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "Rp 2,500,000",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
        
        // Quick Actions
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                icon = Icons.Default.Add,
                title = "Income",
                onClick = onNavigateToTransactions,
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                icon = Icons.Default.Remove,
                title = "Expense",
                onClick = onNavigateToTransactions,
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                icon = Icons.Default.SwapHoriz,
                title = "Transfer",
                onClick = onNavigateToTransactions,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Menu Items
        Text(
            text = "Manage",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        MenuListItem(
            icon = Icons.Default.AccountBalanceWallet,
            title = "Accounts",
            subtitle = "Manage your accounts",
            onClick = onNavigateToAccounts
        )
        
        MenuListItem(
            icon = Icons.Default.Receipt,
            title = "Transactions",
            subtitle = "View all transactions",
            onClick = onNavigateToTransactions
        )
        
        MenuListItem(
            icon = Icons.Default.PieChart,
            title = "Budgets",
            subtitle = "Track your spending",
            onClick = onNavigateToBudgets
        )
        
        MenuListItem(
            icon = Icons.Default.Flag,
            title = "Goals",
            subtitle = "Savings goals",
            onClick = onNavigateToGoals
        )
        
        MenuListItem(
            icon = Icons.Default.BarChart,
            title = "Statistics",
            subtitle = "View insights",
            onClick = onNavigateToStats
        )
    }
}

@Composable
fun QuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
fun MenuListItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null
            )
        }
    }
}
