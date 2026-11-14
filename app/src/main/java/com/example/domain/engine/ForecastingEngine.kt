package com.example.domain.engine

import com.example.domain.model.ColumnType
import com.example.domain.model.DataTable
import com.example.domain.model.ForecastPoint
import com.example.domain.model.ForecastReport
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

object ForecastingEngine {

    fun forecast(
        table: DataTable,
        metricColumnName: String? = null,
        horizonPeriods: Int = 6,
    ): ForecastReport? {
        val dateCol = table.columnProfiles.firstOrNull { it.type == ColumnType.DATE }
        val metricCol = metricColumnName?.let { name ->
            table.columnProfiles.firstOrNull { it.name.equals(name, true) && it.type == ColumnType.NUMERIC }
        } ?: table.columnProfiles.firstOrNull {
            it.type == ColumnType.NUMERIC && (
                it.name.contains("revenue", true) ||
                    it.name.contains("sales", true) ||
                    it.name.contains("profit", true)
                )
        } ?: table.columnProfiles.firstOrNull { it.type == ColumnType.NUMERIC }
            ?: return null

        val series: List<Pair<String, Double>> = if (dateCol != null) {
            val grouped = linkedMapOf<String, Double>()
            for (row in table.rows) {
                val rawDate = row.getOrElse(dateCol.index) { "" }.trim()
                val valNum = row.getOrNull(metricCol.index)?.replace(",", "")?.toDoubleOrNull() ?: continue
                if (rawDate.length >= 7) {
                    val key = rawDate.take(7)
                    grouped[key] = (grouped[key] ?: 0.0) + valNum
                }
            }
            grouped.entries.sortedBy { it.key }.map { it.key to it.value }
        } else {
            table.rows.mapIndexedNotNull { idx, row ->
                val v = row.getOrNull(metricCol.index)?.replace(",", "")?.toDoubleOrNull()
                if (v != null) "T+${idx + 1}" to v else null
            }
        }

        if (series.size < 3) return null

        val n = series.size
        val xs = (0 until n).map { it.toDouble() }
        val ys = series.map { it.second }
        val meanX = xs.average()
        val meanY = ys.average()

        var num = 0.0
        var den = 0.0
        for (i in 0 until n) {
            num += (xs[i] - meanX) * (ys[i] - meanY)
            den += (xs[i] - meanX).pow(2)
        }
        val slope = if (den > 0.0) num / den else 0.0
        val intercept = meanY - slope * meanX

        val fitted = xs.map { x -> (intercept + slope * x).coerceAtLeast(0.0) }
        val residuals = ys.indices.map { i -> ys[i] - fitted[i] }

        val mae = residuals.map { abs(it) }.average()
        val rmse = sqrt(residuals.map { it.pow(2) }.average())
        val mape = ys.indices.filter { ys[it] != 0.0 }.map { i -> abs(residuals[i] / ys[i]) * 100.0 }
            .takeIf { it.isNotEmpty() }?.average() ?: 0.0

        val ssTot = ys.sumOf { (it - meanY).pow(2) }
        val ssRes = residuals.sumOf { it.pow(2) }
        val rSquared = if (ssTot > 0.0) (1.0 - ssRes / ssTot).coerceIn(0.0, 1.0) else 0.0

        val histPoints = series.indices.map { i ->
            ForecastPoint(
                periodLabel = series[i].first,
                actual = ys[i],
                predicted = fitted[i],
                lowerBound = (fitted[i] - 1.96 * rmse).coerceAtLeast(0.0),
                upperBound = fitted[i] + 1.96 * rmse,
                isForecast = false,
            )
        }

        val lastLabel = series.last().first
        val forecastPoints = (1..horizonPeriods).map { step ->
            val xFuture = (n - 1 + step).toDouble()
            val pred = (intercept + slope * xFuture).coerceAtLeast(0.0)
            val margin = 1.96 * rmse * sqrt(1.0 + step.toDouble() / n)
            ForecastPoint(
                periodLabel = nextPeriodLabel(lastLabel, step),
                actual = null,
                predicted = pred,
                lowerBound = (pred - margin).coerceAtLeast(0.0),
                upperBound = pred + margin,
                isForecast = true,
            )
        }

        val nextVal = forecastPoints.first().predicted
        val lastActual = ys.last()
        val deltaPct = if (lastActual > 0.0) ((nextVal - lastActual) / lastActual) * 100.0 else 0.0
        val trendText = if (slope >= 0) "upward" else "downward"

        val summary = "OLS trend model shows an $trendText trajectory (slope ${AnalyticsComputeEngine.formatCompact(slope)}/period) " +
            "for ${metricCol.name}. Next period forecast: ${AnalyticsComputeEngine.formatCompact(nextVal)} " +
            "(${if (deltaPct >= 0) "+" else ""}${String.format("%.1f", deltaPct)}% vs latest actual), R² = ${String.format("%.2f", rSquared)}."

        val limitations = buildList {
            if (n < 12) add("Limited historical sample ($n periods); confidence intervals widen over longer horizons.")
            if (rSquared < 0.45) add("Linear trend explains ${String.format("%.0f", rSquared * 100)}% of variance — high volatility or seasonality present.")
            if (table.qualityReport.outlierCells > 0) add("Source dataset contains ${table.qualityReport.outlierCells} statistical outlier(s) that may pull slope estimates.")
            add("Exogenous shocks (pricing shifts, macro events, marketing campaigns) are not modeled in univariate time-series regression.")
        }

        return ForecastReport(
            metricColumn = metricCol.name,
            dateColumn = dateCol?.name ?: "Row Index",
            points = histPoints + forecastPoints,
            mae = mae,
            rmse = rmse,
            mapePercent = mape,
            rSquared = rSquared,
            trendSlope = slope,
            nextPeriodPrediction = nextVal,
            summary = summary,
            limitations = limitations,
        )
    }

    private fun nextPeriodLabel(lastLabel: String, step: Int): String {
        val ymMatch = Regex("^(\\d{4})-(\\d{2})$").find(lastLabel)
        if (ymMatch != null) {
            var year = ymMatch.groupValues[1].toInt()
            var month = ymMatch.groupValues[2].toInt() + step
            while (month > 12) {
                month -= 12
                year += 1
            }
            return String.format("%04d-%02d", year, month)
        }
        return "F+$step"
    }
}
