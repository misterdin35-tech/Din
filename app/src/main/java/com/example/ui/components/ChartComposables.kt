package com.example.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.engine.AnalyticsComputeEngine
import com.example.domain.model.ChartKind
import com.example.domain.model.ChartSpec
import com.example.domain.model.ForecastReport
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NeuralViolet
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralError
import com.example.ui.theme.SubtleDivider
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private val chartPalette = listOf(
    CyberCyan,
    ElectricBlue,
    NeuralViolet,
    EmeraldSuccess,
    AmberWarning,
    CoralError,
    Color(0xFFEC4899),
    Color(0xFF60A5FA),
)

@Composable
fun DataChartCard(
    chartSpec: ChartSpec,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = chartSpec.title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = chartSpec.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
        Spacer(Modifier.height(14.dp))

        if (chartSpec.points.isEmpty()) {
            Text(
                text = "No numeric series available for this chart selection.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
        } else {
            when (chartSpec.kind) {
                ChartKind.BAR -> BarChartCanvas(chartSpec)
                ChartKind.LINE -> LineChartCanvas(chartSpec)
                ChartKind.PIE -> PieDonutChartCanvas(chartSpec)
                ChartKind.SCATTER -> ScatterChartCanvas(chartSpec)
            }
        }
    }
}

@Composable
fun BarChartCanvas(chartSpec: ChartSpec) {
    val points = chartSpec.points
    val maxVal = (points.maxOfOrNull { it.value } ?: 1.0).coerceAtLeast(1.0)

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val w = size.width
            val h = size.height
            val slotW = w / points.size.coerceAtLeast(1)
            val barW = (slotW * 0.56f).coerceAtMost(48.dp.toPx())

            for (g in 1..3) {
                val y = h * (g / 4f)
                drawLine(SubtleDivider, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
            }

            points.forEachIndexed { idx, pt ->
                val ratio = (pt.value / maxVal).toFloat().coerceIn(0.03f, 1f)
                val barH = (h - 10.dp.toPx()) * ratio
                val left = idx * slotW + (slotW - barW) / 2f
                val top = h - barH
                val color = chartPalette[idx % chartPalette.size]
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(color, color.copy(alpha = 0.45f))),
                    topLeft = Offset(left, top),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            points.take(5).forEach { pt ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = AnalyticsComputeEngine.formatCompact(pt.value),
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberCyan,
                        maxLines = 1,
                    )
                    Text(
                        text = pt.label.take(10),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun LineChartCanvas(chartSpec: ChartSpec) {
    val points = chartSpec.points
    val maxVal = (points.maxOfOrNull { it.value } ?: 1.0).coerceAtLeast(1.0)
    val minVal = (points.minOfOrNull { it.value } ?: 0.0).coerceAtMost(maxVal * 0.85)
    val span = (maxVal - minVal).coerceAtLeast(1.0)

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val w = size.width
            val h = size.height
            val pad = 12.dp.toPx()

            for (g in 1..3) {
                val y = h * (g / 4f)
                drawLine(SubtleDivider, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
            }

            if (points.size == 1) {
                drawCircle(CyberCyan, radius = 6.dp.toPx(), center = Offset(w / 2f, h / 2f))
                return@Canvas
            }

            val coords = points.mapIndexed { idx, pt ->
                val x = pad + (w - 2 * pad) * (idx.toFloat() / (points.size - 1))
                val norm = ((pt.value - minVal) / span).toFloat().coerceIn(0f, 1f)
                val y = (h - pad) - norm * (h - 2 * pad)
                Offset(x, y)
            }

            val areaPath = Path().apply {
                moveTo(coords.first().x, h)
                coords.forEach { lineTo(it.x, it.y) }
                lineTo(coords.last().x, h)
                close()
            }
            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    listOf(CyberCyan.copy(alpha = 0.28f), Color.Transparent)
                ),
            )

            val linePath = Path().apply {
                moveTo(coords.first().x, coords.first().y)
                coords.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(
                path = linePath,
                brush = Brush.horizontalGradient(listOf(ElectricBlue, CyberCyan, NeuralViolet)),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
            )

            coords.forEach { pt ->
                drawCircle(CyberCyan, radius = 4.5.dp.toPx(), center = pt)
                drawCircle(Color.White, radius = 2.dp.toPx(), center = pt)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = points.first().label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
            Text(
                text = "Peak: ${AnalyticsComputeEngine.formatCompact(maxVal)}",
                style = MaterialTheme.typography.labelSmall,
                color = CyberCyan,
            )
            Text(
                text = points.last().label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
        }
    }
}

@Composable
fun PieDonutChartCanvas(chartSpec: ChartSpec) {
    val points = chartSpec.points.take(6)
    val total = points.sumOf { it.value }.coerceAtLeast(1.0)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(modifier = Modifier.size(136.dp)) {
            val stroke = 24.dp.toPx()
            var startAngle = -90f
            points.forEachIndexed { idx, pt ->
                val sweep = ((pt.value / total) * 360.0).toFloat()
                drawArc(
                    color = chartPalette[idx % chartPalette.size],
                    startAngle = startAngle,
                    sweepAngle = (sweep - 2f).coerceAtLeast(1f),
                    useCenter = false,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Butt),
                )
                startAngle += sweep
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f),
        ) {
            points.forEachIndexed { idx, pt ->
                val pct = (pt.value / total) * 100.0
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(chartPalette[idx % chartPalette.size])
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = pt.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${String.format("%.1f", pct)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
fun ScatterChartCanvas(chartSpec: ChartSpec) {
    val pts = chartSpec.points.filter { it.secondaryValue != null }
    if (pts.isEmpty()) return
    val minX = pts.minOf { it.secondaryValue!! }
    val maxX = pts.maxOf { it.secondaryValue!! }.let { if (it == minX) minX + 1.0 else it }
    val minY = pts.minOf { it.value }
    val maxY = pts.maxOf { it.value }.let { if (it == minY) minY + 1.0 else it }

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val w = size.width
            val h = size.height
            val pad = 12.dp.toPx()
            drawLine(SubtleDivider, Offset(pad, h - pad), Offset(w - pad, h - pad), 1.dp.toPx())
            drawLine(SubtleDivider, Offset(pad, pad), Offset(pad, h - pad), 1.dp.toPx())

            pts.forEach { pt ->
                val nx = ((pt.secondaryValue!! - minX) / (maxX - minX)).toFloat()
                val ny = ((pt.value - minY) / (maxY - minY)).toFloat()
                val x = pad + nx * (w - 2 * pad)
                val y = (h - pad) - ny * (h - 2 * pad)
                drawCircle(NeuralViolet.copy(alpha = 0.85f), radius = 5.dp.toPx(), center = Offset(x, y))
                drawCircle(CyberCyan, radius = 2.dp.toPx(), center = Offset(x, y))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "X: ${chartSpec.xLabel}  •  Y: ${chartSpec.yLabel}",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
        )
    }
}

@Composable
fun ForecastChartCanvas(
    report: ForecastReport,
    modifier: Modifier = Modifier,
) {
    val points = report.points
    if (points.isEmpty()) return
    val maxVal = points.maxOf { maxOf(it.upperBound, it.actual ?: 0.0, it.predicted) }.coerceAtLeast(1.0)
    val minVal = 0.0

    GlassCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${report.metricColumn} Forecast & 95% CI",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                Text(
                    text = "${points.count { !it.isForecast }} historical + ${points.count { it.isForecast }} projected periods",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
            StatusPill(
                label = if (report.trendSlope >= 0) "UPTREND" else "DOWNTREND",
                color = if (report.trendSlope >= 0) EmeraldSuccess else AmberWarning,
            )
        }
        Spacer(Modifier.height(14.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            val w = size.width
            val h = size.height
            val pad = 10.dp.toPx()

            fun xFor(i: Int) = pad + (w - 2 * pad) * (i.toFloat() / (points.size - 1).coerceAtLeast(1))
            fun yFor(v: Double) = (h - pad) - ((v - minVal) / (maxVal - minVal)).toFloat().coerceIn(0f, 1f) * (h - 2 * pad)

            for (g in 1..3) {
                val y = h * (g / 4f)
                drawLine(SubtleDivider, Offset(0f, y), Offset(w, y), 1.dp.toPx())
            }

            // Confidence band polygon for forecast horizon
            val fcStartIdx = points.indexOfFirst { it.isForecast }.coerceAtLeast(0)
            if (fcStartIdx < points.size) {
                val bandPath = Path().apply {
                    val startX = xFor((fcStartIdx - 1).coerceAtLeast(0))
                    val startPt = points[(fcStartIdx - 1).coerceAtLeast(0)]
                    moveTo(startX, yFor(startPt.upperBound))
                    for (i in fcStartIdx until points.size) {
                        lineTo(xFor(i), yFor(points[i].upperBound))
                    }
                    for (i in points.indices.reversed()) {
                        if (i >= (fcStartIdx - 1).coerceAtLeast(0)) {
                            lineTo(xFor(i), yFor(points[i].lowerBound))
                        }
                    }
                    close()
                }
                drawPath(bandPath, color = NeuralViolet.copy(alpha = 0.22f))
            }

            // Trend / Forecast line
            val predPath = Path().apply {
                moveTo(xFor(0), yFor(points[0].predicted))
                for (i in 1 until points.size) {
                    lineTo(xFor(i), yFor(points[i].predicted))
                }
            }
            drawPath(
                path = predPath,
                color = NeuralViolet,
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f),
                ),
            )

            // Actual historical line
            val actualIndices = points.indices.filter { points[it].actual != null }
            if (actualIndices.size >= 2) {
                val actualPath = Path().apply {
                    val first = actualIndices.first()
                    moveTo(xFor(first), yFor(points[first].actual!!))
                    actualIndices.drop(1).forEach { i ->
                        lineTo(xFor(i), yFor(points[i].actual!!))
                    }
                }
                drawPath(
                    path = actualPath,
                    brush = Brush.horizontalGradient(listOf(ElectricBlue, CyberCyan)),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                )
            }

            points.forEachIndexed { idx, pt ->
                val center = if (pt.actual != null) Offset(xFor(idx), yFor(pt.actual)) else Offset(xFor(idx), yFor(pt.predicted))
                drawCircle(
                    color = if (pt.isForecast) NeuralViolet else CyberCyan,
                    radius = 4.dp.toPx(),
                    center = center,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(points.first().periodLabel, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text("— Actual   --- Forecast + 95% CI", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(points.last().periodLabel, style = MaterialTheme.typography.labelSmall, color = NeuralViolet)
        }
    }
}
