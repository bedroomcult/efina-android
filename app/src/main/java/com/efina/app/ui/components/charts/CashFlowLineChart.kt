package com.efina.app.ui.components.charts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    val minValue = points.minOfOrNull { it.value } ?: 0.0
    val maxValue = points.maxOfOrNull { it.value } ?: 0.0
    val range = if (maxValue == minValue) 1.0 else maxValue - minValue

    Box(
        modifier = modifier.fillMaxWidth().height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Zero line
            if (minValue < 0 && maxValue > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFF9CA3AF))
                        .align(Alignment.CenterHorizontally)
                )
            }

            // Chart area with points and connecting lines
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
                val pointCount = points.size
                val spacing = if (pointCount > 1) 1f / (pointCount - 1) else 0f

                for (i in points.indices) {
                    val point = points[i]
                    val normalizedY = (point.value.toFloat() - minValue.toFloat()) / range.toFloat()
                    val yFromTop = 1f - normalizedY

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .align(Alignment.Bottom)
                            .padding(top = (yFromTop * 70f).dp)
                    ) {
                        // Connecting line to previous point
                        if (i > 0) {
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .fillMaxHeight()
                                    .background(Color(0xFF10B981))
                                    .align(Alignment.CenterHorizontally)
                            )
                        }

                        // Point dot
                        Surface(
                            modifier = Modifier
                                .size(6.dp)
                                .align(Alignment.CenterHorizontally)
                                .padding(bottom = 2.dp),
                            color = Color(0xFF10B981),
                            shape = CircleShape
                        )

                        // Date label
                        Text(
                            text = point.displayDate,
                            color = Color(0xFF6B7280),
                            fontSize = 10.sp,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }
    }
}
