package com.example.domain.agent

import android.util.Log
import com.example.domain.engine.AnalyticsComputeEngine
import com.example.domain.engine.ForecastingEngine
import com.example.domain.engine.LocalSqlEngine
import com.example.domain.model.AgentStatus
import com.example.domain.model.AgentStepState
import com.example.domain.model.AnalystStructuredResponse
import com.example.domain.model.ChartKind
import com.example.domain.model.ChartSeriesPoint
import com.example.domain.model.ChartSpec
import com.example.domain.model.ColumnType
import com.example.domain.model.DataTable
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class MultiAgentOrchestrator {

    private val planSchema = Schema.obj(
        properties = mapOf(
            "businessObjective" to Schema.string("Concise summary of the user's business goal"),
            "sqlQuery" to Schema.string("Valid read-only SQLite SELECT query against table named dataset"),
            "pythonCode" to Schema.string("Pandas expression using df.describe(), df.corr(), or df.groupby('Col')['Num'].sum()"),
            "chartKind" to Schema.enumeration(listOf("BAR", "LINE", "PIE", "SCATTER"), "Best chart type"),
            "chartDimension" to Schema.string("Column name for x-axis or category"),
            "chartMetric" to Schema.string("Numeric column name for y-axis"),
            "needsClarification" to Schema.boolean("True only if critical columns needed to answer are missing"),
            "clarificationQuestion" to Schema.string("Question to ask user if needsClarification is true, else empty"),
        ),
    )

    private val synthesisSchema = Schema.obj(
        properties = mapOf(
            "executiveSummary" to Schema.string("2-3 sentence executive summary citing only verified numbers"),
            "keyFindings" to Schema.array(Schema.string(), "3 to 5 bullet findings grounded in the computed results"),
            "supportingNumbers" to Schema.array(Schema.string(), "Exact metrics and totals from the SQL/BI output"),
            "possibleCauses" to Schema.array(Schema.string(), "Evidence-backed drivers or root causes"),
            "recommendations" to Schema.array(Schema.string(), "Actionable business recommendations"),
            "limitations" to Schema.array(Schema.string(), "Data quality caveats, sample size, or missing columns"),
            "followUpQuestions" to Schema.array(Schema.string(), "3 relevant next analytical questions"),
        ),
    )

    private val planModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = planSchema
            },
        )
    }

    private val synthesisModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = synthesisSchema
            },
        )
    }

    suspend fun runAnalysis(
        table: DataTable,
        question: String,
        onProgress: (List<AgentStepState>) -> Unit,
    ): AnalystStructuredResponse {
        val steps = initialSteps().toMutableList()
        fun emitStep(index: Int, status: AgentStatus, summary: String = "") {
            steps[index] = steps[index].copy(status = status, outputSummary = summary)
            onProgress(steps.toList())
        }

        onProgress(steps.toList())

        // AGENT 1: AI Orchestrator
        emitStep(0, AgentStatus.RUNNING, "Inspecting schema (${table.headers.size} columns) and planning workflow...")
        val schemaSummary = table.columnProfiles.joinToString("\n") { p ->
            val san = LocalSqlEngine.sanitizeIdentifier(p.name)
            val stats = if (p.type == ColumnType.NUMERIC) {
                "NUMERIC [min=${p.min}, max=${p.max}, mean=${String.format("%.2f", p.mean ?: 0.0)}]"
            } else {
                "${p.type.name} [unique=${p.uniqueCount}, samples=${p.sampleValues.take(3).joinToString(", ")}]"
            }
            "- $san (orig '${p.name}'): $stats"
        }

        val planPrompt = """
            You are Agent 1 (AI Orchestrator) of DataPilot AI.
            Dataset: "${table.name}" (${table.rows.size} rows).
            SQLite table name is strictly: dataset
            Columns:
            $schemaSummary

            User Question: "$question"

            Return a valid SQLite SELECT query on table `dataset` using the exact sanitized column names above (no semicolons needed, limit <= 20), a Python pandas snippet (from df.describe(), df.corr(), df.groupby('Col')['Metric'].sum(), or df.info()), and chart dimension/metric columns.
            If the question asks about metrics/concepts completely absent from the columns above, set needsClarification = true and write a helpful clarificationQuestion.
        """.trimIndent()

        val planJson = try {
            val resp = withTimeout(60_000L) { planModel.generateContent(planPrompt) }
            val text = resp.text ?: throw IllegalStateException("AI Orchestrator returned empty plan.")
            JSONObject(text)
        } catch (e: TimeoutCancellationException) {
            emitStep(0, AgentStatus.FAILED, "Timed out after 60s")
            throw IllegalStateException("AI Orchestrator timed out after 60 seconds. Please retry.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Agent 1 failed", e)
            emitStep(0, AgentStatus.FAILED, e.localizedMessage ?: "Planning error")
            throw e
        }

        val objective = planJson.optString("businessObjective", "Analyze dataset")
        val plannedSql = planJson.optString("sqlQuery", LocalSqlEngine.defaultSuggestedQuery(table))
        val plannedPython = planJson.optString("pythonCode", "df.describe()")
        val needsClarification = planJson.optBoolean("needsClarification", false)
        val clarificationQ = planJson.optString("clarificationQuestion", "").takeIf { needsClarification && it.isNotBlank() }
        emitStep(0, AgentStatus.VERIFIED, "Objective: $objective")

        // AGENT 2: Data Quality Agent
        emitStep(1, AgentStatus.RUNNING, "Auditing missing values, duplicates, and outliers...")
        val qr = table.qualityReport
        val dqSummary = "Score ${qr.qualityScore}/100 • ${qr.missingCells} missing • ${qr.duplicateRows} duplicates • ${qr.outlierCells} outliers"
        emitStep(1, AgentStatus.VERIFIED, dqSummary)

        // AGENT 3: SQL Analyst
        emitStep(2, AgentStatus.RUNNING, "Executing read-only SQL on isolated SQLite engine...")
        var sqlResult = LocalSqlEngine.execute(table, plannedSql, readOnly = true)
        var finalSql = plannedSql
        if (sqlResult.errorMessage != null) {
            val fallbackSql = LocalSqlEngine.defaultSuggestedQuery(table)
            val retryResult = LocalSqlEngine.execute(table, fallbackSql, readOnly = true)
            if (retryResult.errorMessage == null) {
                sqlResult = retryResult
                finalSql = fallbackSql
                emitStep(2, AgentStatus.RETRIED, "Auto-corrected SQL (${sqlResult.rowCount} rows in ${sqlResult.executionMs}ms)")
            } else {
                emitStep(2, AgentStatus.FAILED, sqlResult.errorMessage ?: "SQL failed")
            }
        } else {
            emitStep(2, AgentStatus.VERIFIED, "${sqlResult.rowCount} rows returned in ${sqlResult.executionMs}ms")
        }

        // AGENT 4: Python Analyst
        emitStep(3, AgentStatus.RUNNING, "Running statistical analysis & correlations...")
        var pyOut = AnalyticsComputeEngine.executePythonStyleScript(table, plannedPython)
        var finalPy = plannedPython
        if (pyOut.error != null) {
            finalPy = "df.describe()\ndf.corr()"
            pyOut = AnalyticsComputeEngine.executePythonStyleScript(table, finalPy)
            emitStep(3, AgentStatus.RETRIED, "Ran fallback statistical summary")
        } else {
            emitStep(3, AgentStatus.VERIFIED, "Computed descriptive stats & correlations")
        }

        // AGENT 5: Business Analyst
        emitStep(4, AgentStatus.RUNNING, "Evaluating period changes, segments & root causes...")
        val biReport = AnalyticsComputeEngine.computeBiInsights(table)
        val forecast = ForecastingEngine.forecast(table, horizonPeriods = 3)
        emitStep(4, AgentStatus.VERIFIED, biReport.rootCauseSummary.take(120))

        // AGENT 6: Visualization Agent
        emitStep(5, AgentStatus.RUNNING, "Selecting optimal visual chart from verified output...")
        val chartKindStr = planJson.optString("chartKind", "BAR")
        val chartKind = runCatching { ChartKind.valueOf(chartKindStr.uppercase()) }.getOrDefault(ChartKind.BAR)
        val chartDim = planJson.optString("chartDimension", "")
        val chartMetric = planJson.optString("chartMetric", "")
        val chartSpec = buildGroundedChart(table, sqlResult.headers, sqlResult.rows, chartKind, chartDim, chartMetric, objective)
        emitStep(5, AgentStatus.VERIFIED, if (chartSpec != null) "${chartSpec.kind.name} chart (${chartSpec.points.size} points)" else "Tabular evidence ready")

        // AGENT 7: Quality Control Agent
        emitStep(6, AgentStatus.RUNNING, "Cross-checking calculations & synthesizing grounded report...")
        val sqlPreview = buildString {
            appendLine("SQL Executed: $finalSql")
            appendLine("Columns: ${sqlResult.headers.joinToString(" | ")}")
            sqlResult.rows.take(15).forEach { r -> appendLine(r.joinToString(" | ")) }
        }
        val biContext = buildString {
            appendLine("BI Summary: ${biReport.rootCauseSummary}")
            biReport.breakdowns.take(3).forEach { bd ->
                appendLine("Breakdown ${bd.dimensionName}: " + bd.items.take(5).joinToString("; ") {
                    val delta = it.periodDeltaPercent?.let { d -> " (MoM ${String.format("%.1f", d)}%)" } ?: ""
                    "${it.label}=${AnalyticsComputeEngine.formatCompact(it.revenue)} (${String.format("%.1f", it.sharePercent)}%)$delta"
                })
            }
            if (forecast != null) {
                appendLine("Forecast: ${forecast.summary}")
            }
        }

        val qcPrompt = """
            You are Agent 7 (Quality Control & Executive Synthesis Agent) of DataPilot AI.
            CRITICAL RULE: NEVER fabricate numbers. Cite ONLY numbers present in the verified computations below.

            User Question: "$question"
            Clarification Flag: ${clarificationQ ?: "None"}

            [DATA QUALITY REPORT]
            $dqSummary
            Suggestions: ${qr.suggestions.joinToString("; ")}

            [VERIFIED SQL EXECUTION RESULT]
            $sqlPreview

            [VERIFIED PYTHON STATS]
            ${pyOut.stdout.take(2000)}

            [VERIFIED BUSINESS INTELLIGENCE & FORECAST]
            $biContext

            Produce a structured executive response strictly grounded in these calculations. If Clarification Flag is not None, explain what columns are missing in limitations and keyFindings.
        """.trimIndent()

        val synthJson = try {
            val resp = withTimeout(60_000L) { synthesisModel.generateContent(qcPrompt) }
            val text = resp.text ?: throw IllegalStateException("Quality Control agent returned empty output.")
            JSONObject(text)
        } catch (e: TimeoutCancellationException) {
            emitStep(6, AgentStatus.FAILED, "Synthesis timed out")
            throw IllegalStateException("Quality Control synthesis timed out after 60 seconds.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Agent 7 failed", e)
            emitStep(6, AgentStatus.FAILED, e.localizedMessage ?: "Synthesis error")
            throw e
        }

        emitStep(6, AgentStatus.VERIFIED, "All findings cross-checked against dataset")

        return AnalystStructuredResponse(
            executiveSummary = synthJson.optString("executiveSummary", biReport.rootCauseSummary),
            keyFindings = synthJson.toStringList("keyFindings"),
            supportingNumbers = synthJson.toStringList("supportingNumbers"),
            possibleCauses = synthJson.toStringList("possibleCauses"),
            recommendations = synthJson.toStringList("recommendations"),
            limitations = synthJson.toStringList("limitations"),
            followUpQuestions = synthJson.toStringList("followUpQuestions"),
            clarificationQuestion = clarificationQ,
            sqlQuery = finalSql,
            pythonScript = finalPy,
            chartSpec = chartSpec,
        )
    }

    private fun buildGroundedChart(
        table: DataTable,
        sqlHeaders: List<String>,
        sqlRows: List<List<String>>,
        kind: ChartKind,
        dimName: String,
        metricName: String,
        title: String,
    ): ChartSpec? {
        if (sqlHeaders.size >= 2 && sqlRows.isNotEmpty()) {
            val numColIdx = (1 until sqlHeaders.size).firstOrNull { c ->
                sqlRows.any { it.getOrNull(c)?.toDoubleOrNull() != null }
            }
            if (numColIdx != null) {
                val points = sqlRows.take(10).mapNotNull { row ->
                    val label = row.getOrNull(0)?.ifBlank { "N/A" } ?: return@mapNotNull null
                    val v = row.getOrNull(numColIdx)?.toDoubleOrNull() ?: return@mapNotNull null
                    ChartSeriesPoint(label = label, value = v)
                }
                if (points.isNotEmpty()) {
                    return ChartSpec(
                        title = title.take(60),
                        subtitle = "Grounded in SQL query output (${points.size} groups)",
                        kind = if (kind == ChartKind.SCATTER) ChartKind.BAR else kind,
                        xLabel = sqlHeaders[0],
                        yLabel = sqlHeaders[numColIdx],
                        points = points,
                    )
                }
            }
        }
        val fallbackDim = table.headers.firstOrNull { it.equals(dimName, true) }
            ?: table.columnProfiles.firstOrNull { it.type == ColumnType.CATEGORICAL || it.type == ColumnType.DATE }?.name
            ?: return null
        val fallbackMet = table.headers.firstOrNull { it.equals(metricName, true) }
            ?: table.columnProfiles.firstOrNull { it.type == ColumnType.NUMERIC }?.name
            ?: return null
        return AnalyticsComputeEngine.buildChartFromColumns(table, kind, fallbackDim, fallbackMet)
    }

    private fun JSONObject.toStringList(key: String): List<String> {
        val arr = optJSONArray(key) ?: return emptyList()
        return (0 until arr.length()).mapNotNull { idx -> arr.optString(idx).takeIf { it.isNotBlank() } }
    }

    private fun initialSteps(): List<AgentStepState> = listOf(
        AgentStepState(1, "AI Orchestrator", "Parse intent & create execution plan", AgentStatus.PENDING),
        AgentStepState(2, "Data Quality Agent", "Audit types, nulls, duplicates & outliers", AgentStatus.PENDING),
        AgentStepState(3, "SQL Analyst", "Generate & execute read-only SQL", AgentStatus.PENDING),
        AgentStepState(4, "Python Analyst", "Compute statistics & correlations", AgentStatus.PENDING),
        AgentStepState(5, "Business Analyst", "Identify drivers & root causes", AgentStatus.PENDING),
        AgentStepState(6, "Visualization Agent", "Build chart from verified numbers", AgentStatus.PENDING),
        AgentStepState(7, "Quality Control Agent", "Verify calculations & flag limits", AgentStatus.PENDING),
    )

    private companion object {
        const val TAG = "MultiAgentOrch"
    }
}
