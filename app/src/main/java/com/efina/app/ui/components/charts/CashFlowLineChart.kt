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

data class CashFlowPoint(
    val date: String,
    val displayDate: String,
    val value: Double
)

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
                val dataSet = LineDataSet(
                    points.mapIndexed { index, point ->
                        Entry(index.toFloat(), point.value.toFloat())
                    },
                    "Net Flow"
                ).apply {
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
