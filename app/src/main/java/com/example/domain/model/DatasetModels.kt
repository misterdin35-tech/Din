package com.example.domain.model

enum class ColumnType {
    NUMERIC,
    DATE,
    BOOLEAN,
    CATEGORICAL,
    TEXT,
}

data class ColumnProfile(
    val name: String,
    val index: Int,
    val type: ColumnType,
    val nonNullCount: Int,
    val missingCount: Int,
    val uniqueCount: Int,
    val sampleValues: List<String>,
    val min: Double? = null,
    val max: Double? = null,
    val mean: Double? = null,
    val median: Double? = null,
    val stdDev: Double? = null,
    val q1: Double? = null,
    val q3: Double? = null,
    val outlierCount: Int = 0,
)

data class DataQualityReport(
    val rowCount: Int,
    val columnCount: Int,
    val missingCells: Int,
    val duplicateRows: Int,
    val outlierCells: Int,
    val qualityScore: Int,
    val suggestions: List<String>,
)

data class DataTable(
    val id: String,
    val name: String,
    val sourceType: String,
    val headers: List<String>,
    val rows: List<List<String>>,
    val columnProfiles: List<ColumnProfile>,
    val qualityReport: DataQualityReport,
    val isCleaned: Boolean = false,
)

data class CleaningOptions(
    val removeDuplicates: Boolean = true,
    val imputeMissing: Boolean = true,
    val capOutliers: Boolean = true,
    val trimWhitespace: Boolean = true,
)

data class CleaningComparison(
    val beforeRows: Int,
    val afterRows: Int,
    val beforeMissing: Int,
    val afterMissing: Int,
    val beforeDuplicates: Int,
    val afterDuplicates: Int,
    val beforeOutliers: Int,
    val afterOutliers: Int,
    val beforeScore: Int,
    val afterScore: Int,
    val operationsApplied: List<String>,
)

data class SqlExecutionResult(
    val sql: String,
    val headers: List<String>,
    val rows: List<List<String>>,
    val executionMs: Long,
    val rowCount: Int,
    val isReadOnly: Boolean,
    val errorMessage: String? = null,
)

data class ChartSeriesPoint(
    val label: String,
    val value: Double,
    val secondaryValue: Double? = null,
    val category: String = "",
)

enum class ChartKind {
    BAR,
    LINE,
    PIE,
    SCATTER,
}

data class ChartSpec(
    val title: String,
    val subtitle: String,
    val kind: ChartKind,
    val xLabel: String,
    val yLabel: String,
    val points: List<ChartSeriesPoint>,
)

data class ForecastPoint(
    val periodLabel: String,
    val actual: Double?,
    val predicted: Double,
    val lowerBound: Double,
    val upperBound: Double,
    val isForecast: Boolean,
)

data class ForecastReport(
    val metricColumn: String,
    val dateColumn: String,
    val points: List<ForecastPoint>,
    val mae: Double,
    val rmse: Double,
    val mapePercent: Double,
    val rSquared: Double,
    val trendSlope: Double,
    val nextPeriodPrediction: Double,
    val summary: String,
    val limitations: List<String>,
)

data class BiDimensionBreakdown(
    val dimensionName: String,
    val items: List<BiMetricItem>,
)

data class BiMetricItem(
    val label: String,
    val revenue: Double,
    val profit: Double?,
    val count: Int,
    val sharePercent: Double,
    val periodDeltaPercent: Double?,
)

data class BiInsightsReport(
    val totalRevenue: Double,
    val totalProfit: Double?,
    val avgOrderValue: Double,
    val totalTransactions: Int,
    val periodChangePercent: Double?,
    val rootCauseSummary: String,
    val breakdowns: List<BiDimensionBreakdown>,
)

enum class AgentStatus {
    PENDING,
    RUNNING,
    VERIFIED,
    RETRIED,
    FAILED,
}

data class AgentStepState(
    val agentNumber: Int,
    val agentName: String,
    val roleDescription: String,
    val status: AgentStatus,
    val outputSummary: String = "",
)

data class AnalystStructuredResponse(
    val executiveSummary: String,
    val keyFindings: List<String>,
    val supportingNumbers: List<String>,
    val possibleCauses: List<String>,
    val recommendations: List<String>,
    val limitations: List<String>,
    val followUpQuestions: List<String>,
    val clarificationQuestion: String? = null,
    val sqlQuery: String = "",
    val pythonScript: String = "",
    val chartSpec: ChartSpec? = null,
)

data class AnalystMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val timestampLabel: String,
    val structured: AnalystStructuredResponse? = null,
    val agentTrace: List<AgentStepState> = emptyList(),
)

data class DashboardWidgetSpec(
    val id: String,
    val title: String,
    val widgetType: String, // "kpi", "bar", "line", "pie", "scatter"
    val metricColumn: String,
    val dimensionColumn: String,
    val aggregation: String = "SUM",
)
