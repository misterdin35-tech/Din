package com.example.domain.engine

import com.example.domain.model.CleaningComparison
import com.example.domain.model.CleaningOptions
import com.example.domain.model.ColumnProfile
import com.example.domain.model.ColumnType
import com.example.domain.model.DataQualityReport
import com.example.domain.model.DataTable
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

object DataQualityEngine {

    private val dateRegexes = listOf(
        Regex("^\\d{4}-\\d{2}-\\d{2}.*"),
        Regex("^\\d{1,2}/\\d{1,2}/\\d{2,4}.*"),
        Regex("^\\d{4}/\\d{2}/\\d{2}.*"),
    )

    fun buildTable(
        id: String,
        name: String,
        sourceType: String,
        headers: List<String>,
        rows: List<List<String>>,
        isCleaned: Boolean = false,
    ): DataTable {
        val profiles = headers.mapIndexed { colIndex, header ->
            profileColumn(header, colIndex, rows.map { it.getOrElse(colIndex) { "" } })
        }
        val missingCells = profiles.sumOf { it.missingCount }
        val outlierCells = profiles.sumOf { it.outlierCount }
        val duplicateRows = countDuplicates(rows)
        val totalCells = (rows.size * headers.size).coerceAtLeast(1)

        val missingPenalty = (missingCells.toDouble() / totalCells) * 55.0
        val dupPenalty = (duplicateRows.toDouble() / rows.size.coerceAtLeast(1)) * 30.0
        val outlierPenalty = (outlierCells.toDouble() / totalCells) * 20.0
        val score = (100.0 - missingPenalty - dupPenalty - outlierPenalty).roundToInt().coerceIn(0, 100)

        val suggestions = buildList {
            if (duplicateRows > 0) add("Remove $duplicateRows exact duplicate row(s) to prevent double-counted aggregations.")
            val colsWithMissing = profiles.filter { it.missingCount > 0 }
            if (colsWithMissing.isNotEmpty()) {
                add("Impute $missingCells missing value(s) across ${colsWithMissing.joinToString { it.name }} (median for numeric, mode for categories).")
            }
            val colsWithOutliers = profiles.filter { it.outlierCount > 0 }
            if (colsWithOutliers.isNotEmpty()) {
                add("Review or Winsorize $outlierCells statistical IQR outlier(s) in ${colsWithOutliers.joinToString { it.name }}.")
            }
            if (isEmpty()) {
                add("Dataset passed all completeness, uniqueness, and range checks.")
            }
        }

        val report = DataQualityReport(
            rowCount = rows.size,
            columnCount = headers.size,
            missingCells = missingCells,
            duplicateRows = duplicateRows,
            outlierCells = outlierCells,
            qualityScore = score,
            suggestions = suggestions,
        )

        return DataTable(
            id = id,
            name = name,
            sourceType = sourceType,
            headers = headers,
            rows = rows,
            columnProfiles = profiles,
            qualityReport = report,
            isCleaned = isCleaned,
        )
    }

    fun cleanDataset(
        original: DataTable,
        options: CleaningOptions = CleaningOptions(),
    ): Pair<DataTable, CleaningComparison> {
        val ops = mutableListOf<String>()
        var workingRows = original.rows.map { it.toMutableList() }

        if (options.trimWhitespace) {
            workingRows = workingRows.map { r -> r.map { it.trim() }.toMutableList() }
            ops.add("Trimmed leading & trailing whitespace")
        }

        if (options.removeDuplicates) {
            val seen = LinkedHashSet<List<String>>()
            val beforeCount = workingRows.size
            workingRows = workingRows.filter { seen.add(it.toList()) }.toMutableList()
            val removed = beforeCount - workingRows.size
            if (removed > 0) ops.add("Removed $removed exact duplicate row(s)")
        }

        val currentProfiles = original.headers.mapIndexed { idx, h ->
            profileColumn(h, idx, workingRows.map { it.getOrElse(idx) { "" } })
        }

        if (options.imputeMissing) {
            var imputed = 0
            for (profile in currentProfiles) {
                val colIdx = profile.index
                val nonEmpty = workingRows.mapNotNull { r -> r.getOrNull(colIdx)?.takeIf { it.isNotBlank() } }
                val fillValue = when (profile.type) {
                    ColumnType.NUMERIC -> formatNumber(profile.median ?: profile.mean ?: 0.0)
                    else -> nonEmpty.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: "Unknown"
                }
                for (row in workingRows) {
                    if (row[colIdx].isBlank()) {
                        row[colIdx] = fillValue
                        imputed++
                    }
                }
            }
            if (imputed > 0) ops.add("Imputed $imputed missing cell(s) using median/mode")
        }

        if (options.capOutliers) {
            var capped = 0
            val refreshedProfiles = original.headers.mapIndexed { idx, h ->
                profileColumn(h, idx, workingRows.map { it.getOrElse(idx) { "" } })
            }
            for (profile in refreshedProfiles) {
                if (profile.type == ColumnType.NUMERIC && profile.q1 != null && profile.q3 != null) {
                    val iqr = (profile.q3 - profile.q1).coerceAtLeast(0.0)
                    if (iqr > 0.0) {
                        val lower = profile.q1 - 1.5 * iqr
                        val upper = profile.q3 + 1.5 * iqr
                        for (row in workingRows) {
                            val num = row[profile.index].toDoubleOrNull() ?: continue
                            if (num < lower) {
                                row[profile.index] = formatNumber(lower)
                                capped++
                            } else if (num > upper) {
                                row[profile.index] = formatNumber(upper)
                                capped++
                            }
                        }
                    }
                }
            }
            if (capped > 0) ops.add("Winsorized $capped statistical IQR outlier(s)")
        }

        val cleanedName = if (original.name.endsWith("(Cleaned)")) original.name else "${original.name} (Cleaned)"
        val cleanedTable = buildTable(
            id = original.id,
            name = cleanedName,
            sourceType = original.sourceType,
            headers = original.headers,
            rows = workingRows.map { it.toList() },
            isCleaned = true,
        )

        val comparison = CleaningComparison(
            beforeRows = original.qualityReport.rowCount,
            afterRows = cleanedTable.qualityReport.rowCount,
            beforeMissing = original.qualityReport.missingCells,
            afterMissing = cleanedTable.qualityReport.missingCells,
            beforeDuplicates = original.qualityReport.duplicateRows,
            afterDuplicates = cleanedTable.qualityReport.duplicateRows,
            beforeOutliers = original.qualityReport.outlierCells,
            afterOutliers = cleanedTable.qualityReport.outlierCells,
            beforeScore = original.qualityReport.qualityScore,
            afterScore = cleanedTable.qualityReport.qualityScore,
            operationsApplied = ops.ifEmpty { listOf("Dataset verified — no modifications needed") },
        )
        return cleanedTable to comparison
    }

    private fun profileColumn(name: String, index: Int, values: List<String>): ColumnProfile {
        val missingCount = values.count { it.isBlank() }
        val nonNull = values.filter { it.isNotBlank() }
        val uniqueCount = nonNull.distinct().size

        val numericVals = nonNull.mapNotNull { it.replace(",", "").toDoubleOrNull() }
        val isNumeric = nonNull.isNotEmpty() && numericVals.size >= (nonNull.size * 0.85)
        val isDate = !isNumeric && nonNull.isNotEmpty() && nonNull.count { v -> dateRegexes.any { it.matches(v) } } >= (nonNull.size * 0.8)
        val isBoolean = !isNumeric && !isDate && nonNull.isNotEmpty() && nonNull.all {
            it.lowercase() in setOf("true", "false", "yes", "no", "0", "1")
        }

        val type = when {
            isNumeric -> ColumnType.NUMERIC
            isDate -> ColumnType.DATE
            isBoolean -> ColumnType.BOOLEAN
            uniqueCount <= (nonNull.size * 0.4).coerceAtLeast(15.0) -> ColumnType.CATEGORICAL
            else -> ColumnType.TEXT
        }

        return if (type == ColumnType.NUMERIC && numericVals.isNotEmpty()) {
            val sorted = numericVals.sorted()
            val mean = sorted.average()
            val median = percentile(sorted, 0.5)
            val q1 = percentile(sorted, 0.25)
            val q3 = percentile(sorted, 0.75)
            val variance = sorted.map { (it - mean).pow(2) }.average()
            val std = sqrt(variance)
            val iqr = q3 - q1
            val lowerFence = q1 - 1.5 * iqr
            val upperFence = q3 + 1.5 * iqr
            val outliers = if (iqr > 0.0) sorted.count { it < lowerFence || it > upperFence } else 0

            ColumnProfile(
                name = name,
                index = index,
                type = type,
                nonNullCount = nonNull.size,
                missingCount = missingCount,
                uniqueCount = uniqueCount,
                sampleValues = nonNull.take(4),
                min = sorted.first(),
                max = sorted.last(),
                mean = mean,
                median = median,
                stdDev = std,
                q1 = q1,
                q3 = q3,
                outlierCount = outliers,
            )
        } else {
            ColumnProfile(
                name = name,
                index = index,
                type = type,
                nonNullCount = nonNull.size,
                missingCount = missingCount,
                uniqueCount = uniqueCount,
                sampleValues = nonNull.take(4),
            )
        }
    }

    private fun countDuplicates(rows: List<List<String>>): Int {
        val seen = HashSet<List<String>>()
        var dups = 0
        for (row in rows) {
            if (!seen.add(row)) dups++
        }
        return dups
    }

    fun percentile(sorted: List<Double>, p: Double): Double {
        if (sorted.isEmpty()) return 0.0
        if (sorted.size == 1) return sorted[0]
        val pos = p * (sorted.size - 1)
        val lo = pos.toInt()
        val hi = (lo + 1).coerceAtMost(sorted.lastIndex)
        val frac = pos - lo
        return sorted[lo] + frac * (sorted[hi] - sorted[lo])
    }

    fun formatNumber(value: Double): String {
        return if (value % 1.0 == 0.0) value.toLong().toString()
        else String.format("%.2f", value)
    }
}
