package com.efina.app.ui.components.charts

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.DotProperties
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.Line
import ir.ehsannarmani.compose_charts.models.ZeroLineProperties

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
        androidx.compose.foundation.layout.Box(
            modifier = modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No data")
        }
        return
    }

    val values = points.map { it.value }
    val minValue = values.minOrNull() ?: 0.0
    val maxValue = values.maxOrNull() ?: 0.0

    LineChart(
        modifier = modifier.fillMaxWidth().height(200.dp),
        data = listOf(
            Line(
                label = "Net Flow",
                values = values,
                color = Brush.solid(Color(0xFF10B981)),
                dotProperties = DotProperties(
                    enabled = true,
                    radius = 4.dp,
                    color = Brush.solid(Color(0xFF10B981)),
                    strokeWidth = 2.dp,
                    strokeColor = Brush.solid(Color(0xFF10B981))
                )
            )
        ),
        curvedEdges = false,
        labelProperties = LabelProperties(
            enabled = true,
            labels = points.map { it.displayDate },
            textStyle = TextStyle.Default.copy(fontSize = 10.sp),
            padding = 8.dp
        ),
        zeroLineProperties = ZeroLineProperties(
            enabled = minValue < 0 && maxValue > 0,
            color = Brush.solid(Color(0xFF9CA3AF)),
            thickness = 1.dp
        ),
        maxValue = if (maxValue == minValue) maxValue + 1.0 else maxValue,
        minValue = if (maxValue == minValue) minValue - 1.0 else minValue
    )
}
