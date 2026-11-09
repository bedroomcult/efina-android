package com.efina.app.ui.components.charts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.StrokeCap
import androidx.compose.ui.graphics.drawscope.StrokeJoin
import androidx.compose.ui.graphics.drawstyle.StrokeStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    if (points.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No data")
        }
        return
    }

    CanvasChart(
        modifier = modifier.fillMaxWidth().height(200.dp),
        points = points,
    )
}

@Composable
private fun CanvasChart(
    modifier: Modifier,
    points: List<CashFlowPoint>,
) {
    androidx.compose.ui.graphics.Canvas(modifier = modifier) {
        val size = size
        val width = size.width
        val height = size.height

        val paddingLeft = 32f
        val paddingRight = 16f
        val paddingTop = 24f
        val paddingBottom = 32f

        val plotWidth = width - paddingLeft - paddingRight
        val plotHeight = height - paddingTop - paddingBottom

        val values = points.map { it.value }
        val minValue = values.minOrNull() ?: 0.0
        val maxValue = values.maxOrNull() ?: 0.0
        val range = if (maxValue == minValue) 1.0 else maxValue - minValue

        // Zero line position
        val zeroY = if (minValue < 0 && maxValue > 0) {
            paddingTop + plotHeight * (1f - ((0f - minValue.toFloat()) / range.toFloat()).toFloat())
        } else {
            paddingTop + plotHeight
        }

        // Draw zero line
        if (minValue < 0 && maxValue > 0) {
            drawLine(
                color = Color(0xFF9CA3AF),
                start = Offset(paddingLeft, zeroY),
                end = Offset(width - paddingRight, zeroY),
                strokeWidth = 1f
            )
        }

        // Draw X-axis labels
        val labelStep = points.size.coerceAtLeast(2).coerceAtMost(6)
        for (i in points.indices) {
            if (i % labelStep == 0 || i == points.size - 1) {
                val x = paddingLeft + plotWidth * i / (points.size - 1)
                val label = points[i].displayDate
                val textWidth = textMeasurer.measure(label).size.width
                drawText(
                    text = label,
                    color = Color(0xFF6B7280),
                    topLeft = Offset(x - textWidth / 2, height - paddingBottom + 4f),
                    fontSize = 10.sp
                )
            }
        }

        // Draw Y-axis labels
        val ySteps = 5
        for (i in 0..ySteps) {
            val value = minValue + range * i / ySteps
            val y = paddingTop + plotHeight * (1f - i / ySteps.toFloat())
            val label = formatValue(value)
            val textWidth = textMeasurer.measure(label).size.width
            drawText(
                text = label,
                color = Color(0xFF6B7280),
                topLeft = Offset(4f, y - 6f),
                fontSize = 10.sp
            )
        }

        // Draw line path
        if (points.size >= 2) {
            val path = Path()
            for (i in points.indices) {
                val x = paddingLeft + plotWidth * i / (points.size - 1)
                val clampedY = if (points[i].value >= 0) {
                    zeroY - plotHeight * points[i].value.toFloat() / range.toFloat()
                } else {
                    zeroY + plotHeight * (-points[i].value.toFloat()) / range.toFloat()
                }
                if (i == 0) {
                    path.moveTo(x, clampedY)
                } else {
                    path.lineTo(x, clampedY)
                }
            }

            // Draw fill below line
            val fillPath = Path(path).apply {
                lineTo(width - paddingRight, zeroY)
                lineTo(paddingLeft, zeroY)
                close()
            }
            drawPath(
                path = fillPath,
                color = Color(0xFF10B981).copy(alpha = 0.2f)
            )

            // Draw line
            drawPath(
                path = path,
                color = Color(0xFF10B981),
                strokeWidth = 2f,
                style = StrokeStyle(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Draw points
            for (i in points.indices) {
                val x = paddingLeft + plotWidth * i / (points.size - 1)
                val clampedY = if (points[i].value >= 0) {
                    zeroY - plotHeight * points[i].value.toFloat() / range.toFloat()
                } else {
                    zeroY + plotHeight * (-points[i].value.toFloat()) / range.toFloat()
                }
                drawCircle(
                    color = Color(0xFF10B981),
                    center = Offset(x, clampedY),
                    radius = 3f
                )
            }
        }
    }
}

private fun formatValue(value: Double): String {
    val abs = Math.abs(value)
    return when {
        abs >= 1_000_000 -> "${(abs / 1_000_000).toInt()}M"
        abs >= 1_000 -> "${(abs / 1_000).toInt()}K"
        else -> value.toInt().toString()
    }
}
