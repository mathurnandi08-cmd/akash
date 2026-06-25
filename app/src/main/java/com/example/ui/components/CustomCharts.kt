package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

// Theme Colors for Custom Charts
object ChartColors {
    val Cyan = Color(0xFF00F2FE)
    val Green = Color(0xFF00FF87)
    val Pink = Color(0xFFFF007A)
    val Purple = Color(0xFF9D4EDD)
    val GridLine = Color(0xFF1E293B)
    val Background = Color(0xFF1E293B)
    val TextColor = Color(0xFF94A3B8)
}

/**
 * A beautiful, highly-polished smooth Area Chart with a gradient fill under the curve.
 */
@Composable
fun RealTimeAreaChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = ChartColors.Cyan,
    fillColors: List<Color> = listOf(ChartColors.Cyan.copy(alpha = 0.4f), Color.Transparent),
    gridLinesCount: Int = 4,
    showGrid: Boolean = true
) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600)
        )
    }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (data.isEmpty()) return@Canvas

            val width = size.width
            val height = size.height
            val maxVal = data.maxOrNull()?.coerceAtLeast(1f) ?: 1f
            val minVal = 0f
            val valueRange = maxVal - minVal

            val pointsCount = data.size
            val xIncrement = width / (pointsCount - 1).coerceAtLeast(1)

            // Draw horizontal Grid lines
            if (showGrid) {
                val gridYIncrement = height / gridLinesCount
                for (i in 0..gridLinesCount) {
                    val y = i * gridYIncrement
                    drawLine(
                        color = ChartColors.GridLine.copy(alpha = 0.5f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }
            }

            // Create Path for Area
            val fillPath = Path()
            val strokePath = Path()

            val firstY = height - ((data.first() - minVal) / valueRange) * height
            fillPath.moveTo(0f, height)
            fillPath.lineTo(0f, firstY)
            strokePath.moveTo(0f, firstY)

            for (i in 1 until pointsCount) {
                val nextX = i * xIncrement
                val nextY = height - ((data[i] - minVal) / valueRange) * height

                // Smooth cubic curve calculations
                val prevX = (i - 1) * xIncrement
                val prevY = height - ((data[i - 1] - minVal) / valueRange) * height
                val controlX1 = prevX + (nextX - prevX) / 2f
                val controlY1 = prevY
                val controlX2 = prevX + (nextX - prevX) / 2f
                val controlY2 = nextY

                val animatedY = lerp(prevY, nextY, animatedProgress.value)
                val animatedControlY1 = lerp(prevY, controlY1, animatedProgress.value)
                val animatedControlY2 = lerp(prevY, controlY2, animatedProgress.value)

                fillPath.cubicTo(
                    controlX1, animatedControlY1,
                    controlX2, animatedControlY2,
                    nextX, animatedY
                )
                strokePath.cubicTo(
                    controlX1, animatedControlY1,
                    controlX2, animatedControlY2,
                    nextX, animatedY
                )
            }

            fillPath.lineTo(width, height)
            fillPath.close()

            // Draw gradient fill
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = fillColors,
                    startY = 0f,
                    endY = height
                )
            )

            // Draw line stroke
            drawPath(
                path = strokePath,
                color = lineColor,
                style = Stroke(
                    width = 6f,
                    cap = StrokeCap.Round
                )
            )

            // Draw glowing dot at the last point
            if (data.isNotEmpty()) {
                val lastX = (pointsCount - 1) * xIncrement
                val lastY = height - ((data.last() - minVal) / valueRange) * height
                drawCircle(
                    color = lineColor,
                    radius = 12f,
                    center = Offset(lastX, lastY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 4f,
                    center = Offset(lastX, lastY)
                )
            }
        }
    }
}

/**
 * A sleek vertical rounded bar chart showing distributions (e.g. events by category).
 */
@Composable
fun MetricBarChart(
    metrics: Map<String, Float>,
    modifier: Modifier = Modifier,
    barColor: Color = ChartColors.Purple
) {
    val maxVal = metrics.values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(metrics) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(1f, tween(800))
    }

    Row(
        modifier = modifier.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        metrics.forEach { (label, value) ->
            val ratio = value / maxVal
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // Background bar track
                    Spacer(
                        modifier = Modifier
                            .width(18.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ChartColors.GridLine.copy(alpha = 0.4f))
                    )

                    // Active filled bar with gradient
                    Spacer(
                        modifier = Modifier
                            .width(18.dp)
                            .fillMaxHeight(ratio * animatedProgress.value)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(barColor, barColor.copy(alpha = 0.5f))
                                )
                            )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = ChartColors.TextColor,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value.toInt().toString(),
                    fontSize = 10.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Concentric modern ring gauge representing system performance or platform distribution.
 */
@Composable
fun DistributionDonutChart(
    data: Map<String, Float>,
    colors: Map<String, Color>,
    modifier: Modifier = Modifier,
    thickness: Dp = 16.dp
) {
    val total = data.values.sum().coerceAtLeast(1f)
    val density = LocalDensity.current
    val thicknessPx = with(density) { thickness.toPx() }

    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(1f, tween(1000))
    }

    Row(
        modifier = modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Donut canvas
        Box(
            modifier = Modifier
                .size(120.dp)
                .weight(1.2f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val sizeMin = size.minDimension
                val radius = (sizeMin - thicknessPx) / 2f
                val center = Offset(size.width / 2f, size.height / 2f)

                var startAngle = -90f

                data.forEach { (key, value) ->
                    val sweepAngle = (value / total) * 360f * animatedProgress.value
                    val color = colors[key] ?: Color.Gray

                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = thicknessPx, cap = StrokeCap.Round)
                    )
                    startAngle += sweepAngle
                }
            }

            // Center Text inside Donut
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "TOTAL",
                    fontSize = 10.sp,
                    color = ChartColors.TextColor,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = total.toInt().toString(),
                    fontSize = 16.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Legend list
        Column(
            modifier = Modifier.weight(1.5f),
            verticalArrangement = Arrangement.Center
        ) {
            data.forEach { (key, value) ->
                val percentage = (value / total * 100).toInt()
                val color = colors[key] ?: Color.Gray
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$key ($percentage%)",
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = value.toInt().toString(),
                        fontSize = 11.sp,
                        color = ChartColors.TextColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + fraction * (stop - start)
}
