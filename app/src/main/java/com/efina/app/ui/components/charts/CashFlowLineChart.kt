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
 * is emerald green, with a semi-transparent fill underneath. Positive values
 * (above zero) indicate net income; negative values (below zero) indicate net expense.
 *
 * @param points The data points to render, ordered by date ascending.
 * @param modifier Compose modifier for layout.
 */
@Composable
fun CashFlowLineChart(
    points: List<CashFlowPoint>,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            LineChart(context).apply {
                setTouchEnabled(true)
                setDragEnabled(true)
                setScaleEnabled(false)
                setPinchZoom(false)
                setDoubleTapToZoomEnabled(false)
                description.isEnabled = false
                legend.isEnabled = false

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

                val leftAxis = axisLeft
                leftAxis.isEnabled = true
                leftAxis.labelCount = 5
                leftAxis.isDrawGridLinesEnabled = false

                val rightAxis = axisRight
                rightAxis.isEnabled = false
            }
        },
        update = { chart ->
            chart.apply {
                val entries = points.mapIndexed { index, point ->
                    Entry(index.toFloat(), point.value.toFloat())
                }

                val dataSet = LineDataSet(entries, "Net Flow").apply {
                    setColor(0xFF10B981.toInt())
                    setCircleColor(0xFF10B981.toInt())
                    circleRadius = 3f
                    circleHoleRadius = 1.5f
                    lineWidth = 2f
                    setDrawFilled(true)
                    fillColor = 0xFF10B981.toInt()
                    fillAlpha = 80
                    setDrawValues(false)
                }

                val lineData = LineData(dataSet)
                lineData.setDrawValues(false)

                chart.data = lineData
                chart.invalidate()
            }
        },
    )
}
