package com.efina.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.efina.app.domain.model.Account
import com.efina.app.domain.model.AccountType
import com.efina.app.domain.model.AccountShareRole
import com.efina.app.domain.model.TransactionType
import com.efina.app.ui.theme.EfinaCorners
import com.efina.app.ui.theme.EfinaType
import com.efina.app.ui.theme.LocalIsDarkTheme
import com.efina.app.ui.theme.LocalMoneyPalette
import com.efina.app.ui.theme.parseHexColor
import com.efina.app.ui.theme.tonal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import com.efina.app.ui.theme.springClick
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.alpha
import com.efina.app.ui.theme.EfinaMotion
import androidx.compose.animation.core.FastOutSlowInEasing

/**
 * Shared UI primitives. These carry the design.md rules that would otherwise be
 * re-typed per screen: pill tags, segmented rows, tonal icon tiles, tabular
 * money text, skeleton and empty states.
 */

// ── Money formatting ────────────────────────────────────────────────

/**
 * Formats an amount for display. Uses the account currency, falls back gracefully
 * for unknown ISO codes (the backend allows free-text currency).
 */
fun formatMoney(amount: Double, currency: String = "IDR"): String {
    val locale = Locale("id", "ID")
    return try {
        NumberFormat.getCurrencyInstance(locale).apply {
            this.currency = Currency.getInstance(currency)
            maximumFractionDigits = if (currency == "IDR") 0 else 2
        }.format(amount)
    } catch (_: Exception) {
        NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 2 }
            .format(amount) + " " + currency
    }
}

/** Compact form for dense rows: 1.2jt, 850rb. */
fun formatCompact(amount: Double, currency: String = "IDR"): String {
    val abs = kotlin.math.abs(amount)
    val sign = if (amount < 0) "-" else ""
    val body = when {
        abs >= 1_000_000_000 -> "%.1fM".format(abs / 1_000_000_000)
        abs >= 1_000_000 -> "%.1fjt".format(abs / 1_000_000)
        abs >= 1_000 -> "%.0frb".format(abs / 1_000)
        else -> "%.0f".format(abs)
    }
    return sign + body
}

/** Short date: "12 Nov 2026" from an ISO string. */
fun formatDate(iso: String?): String {
    if (iso.isNullOrBlank()) return "—"
    return runCatching {
        val datePart = iso.substringBefore('T').substringBefore(' ')
        val (y, m, d) = datePart.split("-").map { it.toInt() }
        val months = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )
        "$d ${months[m - 1]} $y"
    }.getOrDefault(iso.take(10))
}

// ── Tags and badges ─────────────────────────────────────────────────

/** Pill tag, design.md: "chips, tags, version badges are full radius". */
@Composable
fun PillTag(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.secondaryContainer,
    content: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    Box(
        modifier = modifier
            .background(container, EfinaCorners.pill)
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = content,
            maxLines = 1
        )
    }
}

/** Marks a shared account, and says what the caller may do with it. */
@Composable
fun SharedAccountBadge(account: Account, modifier: Modifier = Modifier) {
    // The backend computes account_role for EVERY account: 'admin' when you own it
    // (ACCOUNT_ROLE_SQL in accountAccess.js). Keying off role != null therefore
    // badged every account as "Family", including personal ones. Only a genuinely
    // shared account earns a badge.
    if (!account.isShared) return
    val isViewer = account.role == AccountShareRole.VIEWER
    PillTag(
        text = if (isViewer) "View only" else "Family",
        modifier = modifier,
        container = if (isViewer) MaterialTheme.colorScheme.surfaceContainerHighest
        else MaterialTheme.colorScheme.primaryContainer,
        content = if (isViewer) MaterialTheme.colorScheme.onSurfaceVariant
        else MaterialTheme.colorScheme.onPrimaryContainer
    )
}

/**
 * Tonal icon tile: colour derived from the accent, per design.md `tonal()`.
 *
 * When the caller passes an explicit tint (transaction type), the tile carries the
 * money colour so income, expense and transfer read apart in a list — the web app
 * does the same with its `bg-emerald-500/10` / `bg-red-500/10` wrappers.
 */
@Composable
fun AccountIconTile(
    account: Account,
    modifier: Modifier = Modifier,
    size: Int = 40
) {
    val dark = LocalIsDarkTheme.current
    val accent = parseHexColor(account.color, MaterialTheme.colorScheme.primary)
    val (container, onContainer) = tonal(accent, dark)

    Box(
        modifier = modifier
            .size(size.dp)
            .background(container, EfinaCorners.icon(size)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = accountTypeGlyph(account.type),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = onContainer
        )
    }
}

private fun accountTypeGlyph(type: AccountType): String = when (type) {
    AccountType.CASH -> "C"
    AccountType.BANK -> "B"
    AccountType.E_WALLET -> "E"
    AccountType.CREDIT -> "K"
    AccountType.OTHER -> "O"
}

/** Signed amount text, coloured by transaction type. */
@Composable
fun AmountText(
    amount: Double,
    type: TransactionType,
    currency: String = "IDR",
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = EfinaType.amountSmall
) {
    val money = LocalMoneyPalette.current
    val (color, prefix) = when (type) {
        TransactionType.INCOME -> money.income to "+"
        TransactionType.EXPENSE -> money.expense to "−"
        TransactionType.TRANSFER -> money.transfer to ""
        TransactionType.ADJUSTMENT -> MaterialTheme.colorScheme.onSurfaceVariant to ""
    }
    Text(
        text = prefix + formatMoney(kotlin.math.abs(amount), currency),
        style = style,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

// ── Section header ──────────────────────────────────────────────────

/** Section title + optional trailing action, design.md `titleLargeEmphasized`. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = EfinaType.titleLargeEmphasized)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        action?.invoke()
    }
}

// ── States ──────────────────────────────────────────────────────────

/** Shaped placeholder mirroring a row, per design.md skeletons. */
@Composable
fun SkeletonRow(modifier: Modifier = Modifier) {
    // Shimmer rather than a flat grey block: a static rectangle reads as a broken row,
    // while a pulse reads as "loading". 900ms alpha 0.45 <-> 0.9, and it stops entirely
    // when animations are disabled.
    val shimmer = if (EfinaMotion.enabled) {
        val transition = rememberInfiniteTransition(label = "skeleton")
        transition.animateFloat(
            initialValue = 0.45f,
            targetValue = 0.9f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "skeletonAlpha"
        ).value
    } else 0.7f

    Column(modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Box(
            Modifier
                .fillMaxWidth(0.45f)
                .height(14.dp)
                .alpha(shimmer)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, EfinaCorners.groupInner)
        )
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth(0.25f)
                .height(10.dp)
                .alpha(shimmer)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, EfinaCorners.groupInner)
        )
    }
}

@Composable
fun LoadingList(rows: Int = 5, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        repeat(rows) { SkeletonRow() }
    }
}

@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            title,
            style = EfinaType.titleMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (message != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        if (action != null) {
            Spacer(Modifier.height(16.dp))
            action()
        }
    }
}

@Composable
fun ErrorState(message: String, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
    EmptyState(
        title = "Something went wrong",
        message = message,
        modifier = modifier,
        action = onRetry?.let { { Button(onClick = it) { Text("Retry") } } }
    )
}

/** Segmented row group, design.md: 20dp outer, 4dp inner, 2dp gaps. */
@Composable
fun SegmentedColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        content()
    }
}

@Composable
fun SegmentedRow(
    modifier: Modifier = Modifier,
    first: Boolean = false,
    last: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val shape = when {
        first && last -> EfinaCorners.groupOuter
        first -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        last -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
        else -> EfinaCorners.groupInner
    }
    Surface(
        modifier = modifier.fillMaxWidth()
            .springClick(EfinaCorners.menu, onClick = onClick ?: {}),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}
