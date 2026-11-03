package com.efina.app.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
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
import com.efina.app.domain.model.Transaction
import com.efina.app.domain.model.TransactionType
import com.efina.app.ui.components.formatMoney
import com.efina.app.ui.theme.EfinaCorners
import com.efina.app.ui.theme.EfinaType
import com.efina.app.ui.theme.LocalMoneyPalette
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Detail sheet for one transaction: what it was, then edit and delete.
 *
 * Tapping a row used to jump straight into the edit form, which is the wrong default —
 * most taps are "what was this?", and the form made looking cost an accidental edit.
 * The row now opens this summary; editing is an explicit choice from here.
 *
 * Delete lives behind a confirmation, because a transaction is financial history and
 * the sheet is one tap from the list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailSheet(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val money = LocalMoneyPalette.current

    val (tint, container, glyph, typeLabel) = when (transaction.type) {
        TransactionType.INCOME ->
            Quad(money.income, money.onIncome, Icons.Filled.ArrowDownward, "Income")
        TransactionType.EXPENSE ->
            Quad(money.expense, money.onExpense, Icons.Filled.ArrowUpward, "Expense")
        TransactionType.TRANSFER ->
            Quad(money.transfer, money.onTransfer, Icons.Filled.SwapHoriz, "Transfer")
        TransactionType.ADJUSTMENT ->
            Quad(
                MaterialTheme.colorScheme.onSurfaceVariant,
                MaterialTheme.colorScheme.surfaceContainerHighest,
                Icons.Filled.Tune,
                "Adjustment"
            )
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).background(container, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(glyph, contentDescription = typeLabel, tint = tint, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.size(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        transaction.description?.takeIf { it.isNotBlank() }
                            ?: transaction.categoryName
                            ?: typeLabel,
                        style = EfinaType.titleLargeEmphasized,
                        maxLines = 2
                    )
                    Text(
                        typeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = tint,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Text(
                signedAmount(transaction, money.income, money.expense, money.transfer),
                style = EfinaType.amountLarge,
                color = tint
            )

            Spacer(Modifier.height(18.dp))
            HorizontalDivider()
            Spacer(Modifier.height(14.dp))

            DetailRow("Date", prettyDate(transaction.date))
            transaction.categoryName?.let { DetailRow("Category", it) }
            DetailRow("Account", transaction.accountName ?: "—")
            transaction.toAccountName?.let { DetailRow("To account", it) }

            Spacer(Modifier.height(22.dp))

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

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

private fun signedAmount(
    tx: Transaction,
    income: androidx.compose.ui.graphics.Color,
    expense: androidx.compose.ui.graphics.Color,
    transfer: androidx.compose.ui.graphics.Color
): String = when (tx.type) {
    TransactionType.INCOME -> "+" + formatMoney(tx.amount, tx.currency)
    TransactionType.EXPENSE -> "−" + formatMoney(tx.amount, tx.currency)
    else -> formatMoney(tx.amount, tx.currency)
}

/** "Tue, 3 Nov 2026 · 14:35" from the stored ISO string, falling back to the raw value. */
private fun prettyDate(raw: String): String {
    val dateOnly = runCatching {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(raw.take(10))
    }.getOrNull() ?: return raw
    val day = SimpleDateFormat("EEE, d MMM yyyy", Locale.US).format(dateOnly)
    val time = raw.drop(11).take(5)
    return if (time.isBlank() || time == "00:00") day else "$day · $time"
}
