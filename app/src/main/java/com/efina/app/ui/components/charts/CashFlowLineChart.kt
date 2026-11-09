package com.efina.app.ui.components.charts

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter

/**
 * Data class representing a single point on the cash flow chart.
 *
 * @param date ISO date string (yyyy-MM-dd)
 * @param displayDate Display string for the x-axis label (e.g. "Mon", "Tue")
 * @param value Cumulative net flow value (income - expense), in the account's currency
 */
data class CashFlowPoint(
    val date: String,
    val displayDate: String,
    val value: Double
)

/**
 * Composable wrapper around MPAndroidChart's [LineChart].
 *
 * Renders a line chart showing cumulative net cash flow over time. The line color
 * is derived from the Material theme's primary color, with a semi-transparent fill
 * underneath. Positive values (above zero) indicate net income; negative values
 * (below zero) indicate net expense.
 *
 * @param points The data points to render, ordered by date ascending.
 * @param modifier Compose modifier for layout.
 */
@Composable
fun CashFlowLineChart(
    points: List<CashFlowPoint>,
    modifier: Modifier = Modifier,
) {
    val primaryColor = androidx.compose.ui.graphics.Color(0xFF10B981).toArgb()

    AndroidView(
        modifier = modifier,
        factory = { context ->
            LineChart(context).apply {
                // Chart appearance
                setTouchEnabled(true)
                setDragEnabled(true)
                setScaleEnabled(false)
                setPinchZoom(false)
                setDoubleTapToZoomEnabled(false)
                setHighlightPerDragEnabled(true)
                setHighlightPerTapEnabled(true)

                // Disable description text
                description.isEnabled = false

                // Disable legend (not needed for a single-line chart)
                legend.isEnabled = false

                // X axis: dates at the bottom
                val xAxis = xAxis
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.isDrawGridLinesEnabled = false
                xAxis.granularity = 1f
                xAxis.labelCount = points.size.coerceAtLeast(1).coerceAtMost(10)
                xAxis.valueFormatter = object : ValueFormatter() {
                    override fun getFormattedLabel(value: Float): String {
                        val idx = value.toInt().coerceIn(0, points.size - 1)
                        return points[idx].displayDate
                    }
                }

                // Y axis: currency values
                val leftAxis = axisLeft
                leftAxis.isEnabled = true
                leftAxis.labelCount = 5
                leftAxis.isDrawGridLinesEnabled = false
                leftAxis.valueFormatter = object : ValueFormatter() {
                    override fun getFormattedLabel(value: Float): String {
                        return formatCurrency(value)
                    }
                }

                val rightAxis = axisRight
                rightAxis.isEnabled = false
            }
        },
        update = { chart ->
            chart.apply {
                // Build entries from points
                val entries = points.mapIndexed { index, point ->
                    Entry(index.toFloat(), point.value.toFloat())
                }

                val dataSet = LineDataSet(entries, "Net Flow").apply {
                    setColor(primaryColor)
                    setCircleColor(primaryColor)
                    circleRadius = 3f
                    circleHoleRadius = 1.5f
                    lineWidth = 2f
                    setDrawFilled(true)
                    fillColor = primaryColor
                    fillAlpha = 80
                    setDrawValues(false)
                    setDrawCircleHighlight(true)
                    setHighLightColor(primaryColor)
                }

                val lineData = LineData(dataSet)
                lineData.setDrawValues(false)

                data = lineData
                invalidate()
            }
        },
    )
}

/**
 * Format a numeric value for display on the Y axis.
 */
private fun formatCurrency(value: Float): String {
    return when {
        value >= 1000 -> "${(value / 1000).toInt()}K"
        value <= -1000 -> "-${(-value / 1000).toInt()}K"
        else -> value.toInt().toString()
    }
}
