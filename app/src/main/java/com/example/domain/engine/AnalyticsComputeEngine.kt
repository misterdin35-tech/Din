package com.example.domain.engine

import com.example.domain.model.BiDimensionBreakdown
import com.example.domain.model.BiInsightsReport
import com.example.domain.model.BiMetricItem
import com.example.domain.model.ChartKind
import com.example.domain.model.ChartSeriesPoint
import com.example.domain.model.ChartSpec
import com.example.domain.model.ColumnType
import com.example.domain.model.DataTable
import kotlin.math.pow
import kotlin.math.sqrt

data class PythonExecutionOutput(
    val stdout: String,
    val error: String? = null,
    val chartSpec: ChartSpec? = null,
)

object AnalyticsComputeEngine {

    fun computeBiInsights(table: DataTable): BiInsightsReport {
        val profiles = table.columnProfiles
        val revenueIdx = profiles.indexOfFirst {
            it.type == ColumnType.NUMERIC && (
                it.name.contains("revenue", true) ||
                    it.name.contains("sales", true) ||
                    it.name.contains("amount", true) ||
                    it.name.contains("total", true)
                )
        }.takeIf { it >= 0 } ?: profiles.indexOfFirst { it.type == ColumnType.NUMERIC }

        val profitIdx = profiles.indexOfFirst {
            it.type == ColumnType.NUMERIC && (
                it.name.contains("profit", true) ||
                    it.name.contains("margin", true) ||
                    it.name.contains("net", true)
                )
        }.takeIf { it >= 0 && it != revenueIdx }

        val dateIdx = profiles.indexOfFirst { it.type == ColumnType.DATE }

        if (revenueIdx < 0) {
            return BiInsightsReport(
                totalRevenue = 0.0,
                totalProfit = null,
                avgOrderValue = 0.0,
                totalTransactions = table.rows.size,
                periodChangePercent = null,
                rootCauseSummary = "No numeric revenue or metric column found in '${table.name}'.",
                breakdowns = emptyList(),
            )
        }

        val revenueVals = table.rows.mapNotNull { r ->
            r.getOrNull(revenueIdx)?.replace(",", "")?.toDoubleOrNull()
        }
        val totalRev = revenueVals.sum()
        val avgVal = if (revenueVals.isNotEmpty()) revenueVals.average() else 0.0
        val totalProfit = profitIdx?.let { pIdx ->
            table.rows.mapNotNull { r -> r.getOrNull(pIdx)?.replace(",", "")?.toDoubleOrNull() }.sum()
        }

        // Compute period-over-period comparison if date column exists
        var periodChangePct: Double? = null
        var latestPeriodKey: String? = null
        var prevPeriodKey: String? = null

        if (dateIdx >= 0) {
            val byPeriod = linkedMapOf<String, Double>()
            for (row in table.rows) {
                val rawDate = row.getOrElse(dateIdx) { "" }.trim()
                val rev = row.getOrNull(revenueIdx)?.replace(",", "")?.toDoubleOrNull() ?: continue
                if (rawDate.length >= 7) {
                    val ym = rawDate.take(7)
                    byPeriod[ym] = (byPeriod[ym] ?: 0.0) + rev
                }
            }
            val sortedPeriods = byPeriod.keys.sorted()
            if (sortedPeriods.size >= 2) {
                prevPeriodKey = sortedPeriods[sortedPeriods.size - 2]
                latestPeriodKey = sortedPeriods.last()
                val prevVal = byPeriod[prevPeriodKey] ?: 0.0
                val currVal = byPeriod[latestPeriodKey] ?: 0.0
                if (prevVal > 0.0) {
                    periodChangePct = ((currVal - prevVal) / prevVal) * 100.0
                }
            }
        }

        val catProfiles = profiles.filter { it.type == ColumnType.CATEGORICAL }.take(4)
        val breakdowns = catProfiles.map { cat ->
            val grouped = linkedMapOf<String, MutableList<List<String>>>()
            for (row in table.rows) {
                val key = row.getOrElse(cat.index) { "" }.ifBlank { "Missing / Unassigned" }
                grouped.getOrPut(key) { mutableListOf() }.add(row)
            }
            val items = grouped.entries.map { (label, groupRows) ->
                val rev = groupRows.sumOf { it.getOrNull(revenueIdx)?.replace(",", "")?.toDoubleOrNull() ?: 0.0 }
                val prof = profitIdx?.let { pIdx ->
                    groupRows.sumOf { it.getOrNull(pIdx)?.replace(",", "")?.toDoubleOrNull() ?: 0.0 }
                }
                val share = if (totalRev > 0.0) (rev / totalRev) * 100.0 else 0.0
                val deltaPct = if (dateIdx >= 0 && prevPeriodKey != null && latestPeriodKey != null) {
                    val prevSum = groupRows.filter { it.getOrElse(dateIdx) { "" }.startsWith(prevPeriodKey) }
                        .sumOf { it.getOrNull(revenueIdx)?.replace(",", "")?.toDoubleOrNull() ?: 0.0 }
                    val currSum = groupRows.filter { it.getOrElse(dateIdx) { "" }.startsWith(latestPeriodKey) }
                        .sumOf { it.getOrNull(revenueIdx)?.replace(",", "")?.toDoubleOrNull() ?: 0.0 }
                    if (prevSum > 0.0) ((currSum - prevSum) / prevSum) * 100.0 else null
                } else null

                BiMetricItem(
                    label = label,
                    revenue = rev,
                    profit = prof,
                    count = groupRows.size,
                    sharePercent = share,
                    periodDeltaPercent = deltaPct,
                )
            }.sortedByDescending { it.revenue }

            BiDimensionBreakdown(dimensionName = cat.name, items = items)
        }

        val rootCauseSummary = buildString {
            val metricName = table.headers[revenueIdx]
            append("Total $metricName is ${formatCompact(totalRev)} across ${table.rows.size} records (Avg ${formatCompact(avgVal)}).")
            if (periodChangePct != null && prevPeriodKey != null && latestPeriodKey != null) {
                val dir = if (periodChangePct >= 0) "increased" else "decreased"
                append(" Comparing $latestPeriodKey vs $prevPeriodKey, $metricName $dir by ${String.format("%.1f", kotlin.math.abs(periodChangePct))}%.")
                val topMover = breakdowns.firstOrNull()?.items?.filter { it.periodDeltaPercent != null }
                    ?.maxByOrNull { kotlin.math.abs(it.periodDeltaPercent ?: 0.0) }
                if (topMover != null) {
                    val dDir = if ((topMover.periodDeltaPercent ?: 0.0) >= 0) "+" else ""
                    append(" Largest shift in ${breakdowns.first().dimensionName}: ${topMover.label} ($dDir${String.format("%.1f", topMover.periodDeltaPercent)}%).")
                }
            }
        }

        return BiInsightsReport(
            totalRevenue = totalRev,
            totalProfit = totalProfit,
            avgOrderValue = avgVal,
            totalTransactions = table.rows.size,
            periodChangePercent = periodChangePct,
            rootCauseSummary = rootCauseSummary,
            breakdowns = breakdowns,
        )
    }

    fun buildChartFromColumns(
        table: DataTable,
        kind: ChartKind,
        dimensionColumn: String,
        metricColumn: String,
        aggregation: String = "SUM",
        title: String? = null,
    ): ChartSpec {
        val dimIdx = table.headers.indexOfFirst { it.equals(dimensionColumn, ignoreCase = true) }
        val metIdx = table.headers.indexOfFirst { it.equals(metricColumn, ignoreCase = true) }

        if (dimIdx < 0 || metIdx < 0) {
            return ChartSpec(
                title = title ?: "Chart",
                subtitle = "Selected columns not found",
                kind = kind,
                xLabel = dimensionColumn,
                yLabel = metricColumn,
                points = emptyList(),
            )
        }

        if (kind == ChartKind.SCATTER) {
            val pts = table.rows.mapIndexedNotNull { i, row ->
                val x = row.getOrNull(dimIdx)?.replace(",", "")?.toDoubleOrNull()
                val y = row.getOrNull(metIdx)?.replace(",", "")?.toDoubleOrNull()
                if (x != null && y != null) {
                    ChartSeriesPoint(label = "#${i + 1}", value = y, secondaryValue = x)
                } else null
            }.take(80)
            return ChartSpec(
                title = title ?: "$metricColumn vs $dimensionColumn",
                subtitle = "${pts.size} data points",
                kind = ChartKind.SCATTER,
                xLabel = dimensionColumn,
                yLabel = metricColumn,
                points = pts,
            )
        }

        val grouped = linkedMapOf<String, MutableList<Double>>()
        for (row in table.rows) {
            val rawKey = row.getOrElse(dimIdx) { "" }.ifBlank { "Unknown" }
            val key = if (table.columnProfiles.getOrNull(dimIdx)?.type == ColumnType.DATE && rawKey.length >= 7) {
                rawKey.take(7)
            } else rawKey
            val value = row.getOrNull(metIdx)?.replace(",", "")?.toDoubleOrNull() ?: continue
            grouped.getOrPut(key) { mutableListOf() }.add(value)
        }

        val aggregated = grouped.entries.map { (k, vals) ->
            val v = when (aggregation.uppercase()) {
                "AVG", "MEAN" -> vals.average()
                "COUNT" -> vals.size.toDouble()
                "MAX" -> vals.maxOrNull() ?: 0.0
                "MIN" -> vals.minOrNull() ?: 0.0
                else -> vals.sum()
            }
            ChartSeriesPoint(label = k, value = v)
        }

        val sortedPoints = if (kind == ChartKind.LINE) {
            aggregated.sortedBy { it.label }
        } else {
            aggregated.sortedByDescending { it.value }.take(10)
        }

        return ChartSpec(
            title = title ?: "$aggregation($metricColumn) by $dimensionColumn",
            subtitle = "${sortedPoints.size} groups • Grounded in ${table.rows.size} rows",
            kind = kind,
            xLabel = dimensionColumn,
            yLabel = metricColumn,
            points = sortedPoints,
        )
    }

    fun pearsonCorrelations(table: DataTable): List<Triple<String, String, Double>> {
        val numerics = table.columnProfiles.filter { it.type == ColumnType.NUMERIC }
        val pairs = mutableListOf<Triple<String, String, Double>>()
        for (i in numerics.indices) {
            for (j in i + 1 until numerics.size) {
                val c1 = numerics[i]
                val c2 = numerics[j]
                val paired = table.rows.mapNotNull { row ->
                    val a = row.getOrNull(c1.index)?.replace(",", "")?.toDoubleOrNull()
                    val b = row.getOrNull(c2.index)?.replace(",", "")?.toDoubleOrNull()
                    if (a != null && b != null) a to b else null
                }
                if (paired.size >= 3) {
                    val r = pearson(paired.map { it.first }, paired.map { it.second })
                    pairs.add(Triple(c1.name, c2.name, r))
                }
            }
        }
        return pairs.sortedByDescending { kotlin.math.abs(it.third) }
    }

    fun executePythonStyleScript(table: DataTable, script: String): PythonExecutionOutput {
        val lines = script.lines().map { it.trim() }.filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("import ") }
        if (lines.isEmpty()) {
            return PythonExecutionOutput(stdout = "", error = "No executable Python expressions found.")
        }

        val out = StringBuilder()
        var chartSpec: ChartSpec? = null

        return try {
            for (line in lines) {
                val expr = line.removePrefix("print(").let {
                    if (line.startsWith("print(") && it.endsWith(")")) it.dropLast(1).trim() else line
                }
                when {
                    expr.contains("df.shape") -> {
                        out.appendLine("df.shape -> (${table.rows.size}, ${table.headers.size})")
                    }
                    expr.contains("df.columns") -> {
                        out.appendLine("Index(${table.headers.joinToString(", ") { "'$it'" }})")
                    }
                    expr.contains("df.info()") -> {
                        out.appendLine("<class 'datapilot.DataFrame'>")
                        out.appendLine("RangeIndex: ${table.rows.size} entries, ${table.headers.size} columns")
                        table.columnProfiles.forEach { p ->
                            out.appendLine(" ${p.name.padEnd(18)} ${p.nonNullCount} non-null   ${p.type.name}")
                        }
                    }
                    expr.contains("df.isnull().sum()") -> {
                        out.appendLine("Missing values per column:")
                        table.columnProfiles.forEach { p ->
                            out.appendLine(" ${p.name.padEnd(20)} ${p.missingCount}")
                        }
                    }
                    expr.contains("df.describe()") -> {
                        val numerics = table.columnProfiles.filter { it.type == ColumnType.NUMERIC }
                        if (numerics.isEmpty()) {
                            out.appendLine("No numeric columns to describe.")
                        } else {
                            out.appendLine("Summary Statistics (Numeric Columns):")
                            out.appendLine("Column           Count    Mean        Std         Min         50%         Max")
                            numerics.forEach { p ->
                                out.appendLine(
                                    "${p.name.take(15).padEnd(16)} ${p.nonNullCount.toString().padEnd(8)} " +
                                        "${formatCompact(p.mean ?: 0.0).padEnd(11)} ${formatCompact(p.stdDev ?: 0.0).padEnd(11)} " +
                                        "${formatCompact(p.min ?: 0.0).padEnd(11)} ${formatCompact(p.median ?: 0.0).padEnd(11)} ${formatCompact(p.max ?: 0.0)}"
                                )
                            }
                        }
                    }
                    expr.contains("df.corr()") -> {
                        val corrs = pearsonCorrelations(table)
                        out.appendLine("Pearson Correlation Matrix (Top Pairs):")
                        corrs.forEach { (a, b, r) ->
                            out.appendLine(" ${a.padEnd(16)} <-> ${b.padEnd(16)} r = ${String.format("%.4f", r)}")
                        }
                        if (corrs.isNotEmpty()) {
                            val top = corrs.first()
                            chartSpec = buildChartFromColumns(
                                table = table,
                                kind = ChartKind.SCATTER,
                                dimensionColumn = top.first,
                                metricColumn = top.second,
                                title = "Correlation: ${top.first} vs ${top.second} (r=${String.format("%.2f", top.third)})",
                            )
                        }
                    }
                    expr.contains("df.head") -> {
                        val n = Regex("head\\((\\d+)\\)").find(expr)?.groupValues?.get(1)?.toIntOrNull() ?: 5
                        out.appendLine(table.headers.joinToString(" | "))
                        table.rows.take(n).forEach { r -> out.appendLine(r.joinToString(" | ")) }
                    }
                    expr.contains("df.groupby") -> {
                        val m = Regex("groupby\\(['\"]([^'\"]+)['\"]\\)\\[['\"]([^'\"]+)['\"]\\]\\.(\\w+)\\(\\)").find(expr)
                        if (m != null) {
                            val dim = m.groupValues[1]
                            val met = m.groupValues[2]
                            val agg = m.groupValues[3].uppercase()
                            val spec = buildChartFromColumns(table, ChartKind.BAR, dim, met, agg)
                            if (spec.points.isEmpty()) {
                                throw IllegalArgumentException("Column '$dim' or '$met' not found in dataset.")
                            }
                            chartSpec = spec
                            out.appendLine("groupby('$dim')['$met'].${agg.lowercase()}():")
                            spec.points.forEach { pt ->
                                out.appendLine(" ${pt.label.padEnd(22)} ${formatCompact(pt.value)}")
                            }
                        } else {
                            throw IllegalArgumentException("Use syntax: df.groupby('Region')['Revenue'].sum()")
                        }
                    }
                    expr.contains("value_counts") -> {
                        val m = Regex("df\\[['\"]([^'\"]+)['\"]\\]\\.value_counts\\(\\)").find(expr)
                        val col = m?.groupValues?.get(1) ?: table.columnProfiles.firstOrNull { it.type == ColumnType.CATEGORICAL }?.name.orEmpty()
                        val idx = table.headers.indexOfFirst { it.equals(col, true) }
                        if (idx < 0) throw IllegalArgumentException("Column '$col' not found.")
                        val counts = table.rows.groupingBy { it.getOrElse(idx) { "" }.ifBlank { "Missing" } }
                            .eachCount().entries.sortedByDescending { it.value }
                        out.appendLine("value_counts('$col'):")
                        counts.take(15).forEach { (k, v) -> out.appendLine(" ${k.padEnd(22)} $v") }
                        chartSpec = ChartSpec(
                            title = "Distribution of $col",
                            subtitle = "Value counts across ${table.rows.size} rows",
                            kind = ChartKind.BAR,
                            xLabel = col,
                            yLabel = "Count",
                            points = counts.take(10).map { ChartSeriesPoint(it.key, it.value.toDouble()) },
                        )
                    }
                    else -> {
                        throw IllegalArgumentException("Unsupported expression '$expr'. Supported: df.info(), df.describe(), df.isnull().sum(), df.corr(), df.head(5), df.groupby('Col')['Num'].sum(), df['Col'].value_counts()")
                    }
                }
                out.appendLine()
            }
            PythonExecutionOutput(stdout = out.toString().trim(), chartSpec = chartSpec)
        } catch (e: Exception) {
            PythonExecutionOutput(
                stdout = out.toString().trim(),
                error = "ExecutionError: ${e.message}",
                chartSpec = chartSpec,
            )
        }
    }

    private fun pearson(xs: List<Double>, ys: List<Double>): Double {
        val mx = xs.average()
        val my = ys.average()
        var num = 0.0
        var dx2 = 0.0
        var dy2 = 0.0
        for (i in xs.indices) {
            val dx = xs[i] - mx
            val dy = ys[i] - my
            num += dx * dy
            dx2 += dx.pow(2)
            dy2 += dy.pow(2)
        }
        val denom = sqrt(dx2 * dy2)
        return if (denom == 0.0) 0.0 else num / denom
    }

    fun formatCompact(value: Double): String {
        val abs = kotlin.math.abs(value)
        return when {
            abs >= 1_000_000 -> String.format("%.2fM", value / 1_000_000.0)
            abs >= 1_000 -> String.format("%.1fK", value / 1_000.0)
            value % 1.0 == 0.0 -> value.toLong().toString()
            else -> String.format("%.2f", value)
        }
    }
}
