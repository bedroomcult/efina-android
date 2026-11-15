package com.efina.app.ui.components.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.efina.app.domain.model.MonthlyStat
import com.efina.app.ui.theme.LocalMoneyPalette

/**
 * Grouped income/expense bars for the last few months.
 *
 * Plain Compose layout rather than a Canvas: the bars are rects with rounded
 * tops, and a Row/Column gives the same result without another chart dependency.
 */
@Composable
fun MonthlyBarsChart(
    months: List<MonthlyStat>,
    modifier: Modifier = Modifier
) {
    if (months.isEmpty()) {
        Text(
            "No monthly data",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
        )
        return
    }

    val money = LocalMoneyPalette.current
    val max = months.maxOf { maxOf(it.income, it.expense) }.coerceAtLeast(1.0)
    val barShape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)

    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().height(150.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            months.forEach { month ->
                Column(
                    Modifier.weight(1f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Box(
                            Modifier.weight(1f)
                                .fillMaxHeight((month.income / max).toFloat().coerceIn(0.02f, 1f))
                                .background(money.income, barShape)
                        )
                        Box(
                            Modifier.weight(1f)
                                .fillMaxHeight((month.expense / max).toFloat().coerceIn(0.02f, 1f))
                                .background(money.expense, barShape)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        month.month.takeLast(3),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LegendDot("Income", money.income)
            LegendDot("Expense", money.expense)
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
