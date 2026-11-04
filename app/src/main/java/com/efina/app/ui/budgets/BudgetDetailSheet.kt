package com.efina.app.ui.budgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.efina.app.domain.model.Budget
import com.efina.app.domain.model.Transaction
import com.efina.app.ui.components.EmptyState
import com.efina.app.ui.components.LoadingList
import com.efina.app.ui.components.formatMoney
import com.efina.app.ui.components.springClick
import com.efina.app.ui.theme.EfinaCorners
import com.efina.app.ui.theme.EfinaType

/**
 * What a budget is actually made of: its transactions, then edit and delete.
 *
 * Tapping a budget used to jump straight into the edit form, which is the wrong default —
 * most taps are "what is in here?", and the sheet is where the pooled figure becomes
 * explainable. Rows recorded by another household member carry a Family badge, because a
 * shared budget's total includes spending they made on a shared account.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDetailSheet(
    budget: Budget,
    transactions: List<Transaction>,
    loading: Boolean,
    currency: String = "IDR",
    currentUserId: Long?,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenTransaction: ((Transaction) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        budget.categoryName ?: "Budget",
                        style = EfinaType.titleLargeEmphasized,
                        maxLines = 1
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            budget.periodType.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (budget.isShared) {
                            Spacer(Modifier.size(6.dp))
                            Icon(
                                Icons.Filled.Groups,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.size(4.dp))
                            Text(
                                "Family",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                "${formatMoney(budget.spent, currency)} of ${formatMoney(budget.amount, currency)}",
                style = EfinaType.amountMedium
            )
            if (budget.isShared) {
                Text(
                    "Counts spending in this category made on shared accounts.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            when {
                loading -> LoadingList(rows = 3)
                transactions.isEmpty() -> EmptyState(
                    title = "Nothing here yet",
                    message = "No spending in this category for this period."
                )
                else -> LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                    items(transactions, key = { it.id }) { tx ->
                        val fromOther =
                            currentUserId != null && tx.userId != 0L && tx.userId != currentUserId
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .springClick(EfinaCorners.menu) { onOpenTransaction?.invoke(tx) }
                                .padding(vertical = 10.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        tx.description ?: tx.categoryName ?: "Transaction",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1
                                    )
                                    if (fromOther) {
                                        Spacer(Modifier.size(6.dp))
                                        Text(
                                            "Family",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Text(
                                    tx.date.take(10) + (tx.accountName?.let { " · $it" } ?: ""),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            Text(
                                formatMoney(tx.amount, tx.currency),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onEdit,
                    shape = EfinaCorners.menu,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Edit")
                }
                OutlinedButton(
                    onClick = onDelete,
                    shape = EfinaCorners.menu,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}
