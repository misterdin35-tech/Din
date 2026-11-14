package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AnalysisJob
import com.example.data.model.AnalysisJobStatus
import com.example.data.model.AnalysisRecord
import com.example.data.model.AuditLogRecord
import com.example.data.model.AutomationSchedule
import com.example.data.model.DashboardRecord
import com.example.data.model.DatasetRecord
import com.example.data.model.IntegrationRecord
import com.example.data.model.ReportRecord
import com.example.data.model.SavedQueryRecord
import com.example.data.model.UserProfile
import com.example.data.repository.DataPilotRepository
import com.example.data.repository.unwrapFirestoreException
import com.example.domain.agent.MultiAgentOrchestrator
import com.example.domain.engine.AnalyticsComputeEngine
import com.example.domain.engine.DataQualityEngine
import com.example.domain.engine.DatasetParser
import com.example.domain.engine.ForecastingEngine
import com.example.domain.engine.LocalSqlEngine
import com.example.domain.engine.PythonExecutionOutput
import com.example.domain.model.AgentStepState
import com.example.domain.model.AnalystMessage
import com.example.domain.model.BiInsightsReport
import com.example.domain.model.CleaningComparison
import com.example.domain.model.CleaningOptions
import com.example.domain.model.ColumnType
import com.example.domain.model.DashboardWidgetSpec
import com.example.domain.model.DataTable
import com.example.domain.model.ForecastReport
import com.example.domain.model.SqlExecutionResult
import com.example.ui.components.FileUploadUiState
import com.google.firebase.Timestamp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

sealed interface StreamState<out T> {
    data object Loading : StreamState<Nothing>
    data class Ready<T>(val data: T) : StreamState<T>
    data class Error(val message: String) : StreamState<Nothing>
}

class DataPilotViewModel(
    private val repository: DataPilotRepository,
    private val orchestrator: MultiAgentOrchestrator,
    val currentUserId: String,
    initialDisplayName: String,
    initialEmail: String,
) : ViewModel() {

    private fun <T> Flow<List<T>>.toStreamState(label: String): StateFlow<StreamState<List<T>>> =
        this.map<List<T>, StreamState<List<T>>> { StreamState.Ready(it) }
            .catch { error ->
                val fsErr = unwrapFirestoreException(error)
                if (fsErr == null && error is CancellationException) throw error
                val root = fsErr ?: error
                Log.w(TAG, "Error observing $label", root)
                emit(StreamState.Error(root.message ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = StreamState.Loading,
            )

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { error ->
            val fsErr = unwrapFirestoreException(error)
            if (fsErr == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing userProfile", fsErr ?: error)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val datasetsState = repository.observeDatasets(currentUserId)
        .map { list -> list.sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) } }
        .toStreamState("datasets")

    val analysesState = repository.observeAnalyses(currentUserId)
        .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
        .toStreamState("analyses")

    val analysisJobsState = repository.observeAnalysisJobs(currentUserId)
        .map { list -> list.sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) } }
        .toStreamState("analysis_jobs")

    val savedQueriesState = repository.observeSavedQueries(currentUserId)
        .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
        .toStreamState("saved_queries")

    val dashboardsState = repository.observeDashboards(currentUserId)
        .map { list -> list.sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) } }
        .toStreamState("dashboards")

    val reportsState = repository.observeReports(currentUserId)
        .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
        .toStreamState("reports")

    val integrationsState = repository.observeIntegrations(currentUserId)
        .map { list -> list.sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) } }
        .toStreamState("integrations")

    val schedulesState = repository.observeSchedules(currentUserId)
        .map { list -> list.sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) } }
        .toStreamState("schedules")

    val auditLogsState = repository.observeAuditLogs(currentUserId)
        .map { list -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) } }
        .toStreamState("audit_logs")

    private val _activeTable = MutableStateFlow<DataTable?>(null)
    val activeTable: StateFlow<DataTable?> = _activeTable.asStateFlow()

    private val _rawOriginalTable = MutableStateFlow<DataTable?>(null)
    val rawOriginalTable: StateFlow<DataTable?> = _rawOriginalTable.asStateFlow()

    private val _cleaningComparison = MutableStateFlow<CleaningComparison?>(null)
    val cleaningComparison: StateFlow<CleaningComparison?> = _cleaningComparison.asStateFlow()

    private val _sqlQueryText = MutableStateFlow("SELECT * FROM dataset LIMIT 25;")
    val sqlQueryText: StateFlow<String> = _sqlQueryText.asStateFlow()

    private val _sqlReadOnly = MutableStateFlow(true)
    val sqlReadOnly: StateFlow<Boolean> = _sqlReadOnly.asStateFlow()

    private val _sqlResult = MutableStateFlow<SqlExecutionResult?>(null)
    val sqlResult: StateFlow<SqlExecutionResult?> = _sqlResult.asStateFlow()

    private val _pythonScript = MutableStateFlow(
        "df.info()\ndf.describe()\ndf.isnull().sum()\ndf.corr()"
    )
    val pythonScript: StateFlow<String> = _pythonScript.asStateFlow()

    private val _pythonOutput = MutableStateFlow<PythonExecutionOutput?>(null)
    val pythonOutput: StateFlow<PythonExecutionOutput?> = _pythonOutput.asStateFlow()

    private val _biInsights = MutableStateFlow<BiInsightsReport?>(null)
    val biInsights: StateFlow<BiInsightsReport?> = _biInsights.asStateFlow()

    private val _forecastReport = MutableStateFlow<ForecastReport?>(null)
    val forecastReport: StateFlow<ForecastReport?> = _forecastReport.asStateFlow()

    private val _forecastHorizon = MutableStateFlow(6)
    val forecastHorizon: StateFlow<Int> = _forecastHorizon.asStateFlow()

    private val _forecastMetric = MutableStateFlow<String?>(null)
    val forecastMetric: StateFlow<String?> = _forecastMetric.asStateFlow()

    private val _dashboardWidgets = MutableStateFlow<List<DashboardWidgetSpec>>(emptyList())
    val dashboardWidgets: StateFlow<List<DashboardWidgetSpec>> = _dashboardWidgets.asStateFlow()

    private val _dashboardDateFilter = MutableStateFlow("All Time")
    val dashboardDateFilter: StateFlow<String> = _dashboardDateFilter.asStateFlow()

    private val _analystMessages = MutableStateFlow<List<AnalystMessage>>(emptyList())
    val analystMessages: StateFlow<List<AnalystMessage>> = _analystMessages.asStateFlow()

    private val _liveAgentSteps = MutableStateFlow<List<AgentStepState>>(emptyList())
    val liveAgentSteps: StateFlow<List<AgentStepState>> = _liveAgentSteps.asStateFlow()

    private val _isAgentRunning = MutableStateFlow(false)
    val isAgentRunning: StateFlow<Boolean> = _isAgentRunning.asStateFlow()

    private val _globalSearch = MutableStateFlow("")
    val globalSearch: StateFlow<String> = _globalSearch.asStateFlow()

    private val _fileUploadState = MutableStateFlow<FileUploadUiState>(FileUploadUiState.Idle)
    val fileUploadState: StateFlow<FileUploadUiState> = _fileUploadState.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(currentUserId, initialDisplayName, initialEmail)
        }
        viewModelScope.launch {
            datasetsState.collect { state ->
                if (state is StreamState.Ready && _activeTable.value == null && state.data.isNotEmpty()) {
                    selectDatasetRecord(state.data.first())
                }
            }
        }
    }

    fun updateGlobalSearch(query: String) {
        _globalSearch.value = query
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun selectDatasetRecord(record: DatasetRecord) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val parsed = DatasetParser.parseCsv(
                    name = record.name,
                    rawText = record.csvContent,
                    sourceType = record.sourceType,
                    existingId = record.id,
                    isCleaned = record.isCleaned,
                )
                activateTable(parsed)
            } catch (e: Exception) {
                _statusBanner.value = "Failed to load dataset '${record.name}': ${e.localizedMessage}"
            }
        }
    }

    fun clearFileUploadState() {
        _fileUploadState.value = FileUploadUiState.Idle
    }

    fun ingestDeviceFile(
        fileName: String,
        mimeType: String?,
        readBytes: suspend () -> ByteArray,
    ) {
        if (_fileUploadState.value is FileUploadUiState.Parsing) return
        val safeName = fileName.ifBlank { "dataset.csv" }
        _fileUploadState.value = FileUploadUiState.Parsing(
            fileName = safeName,
            stageLabel = "Reading file bytes from device...",
        )
        viewModelScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) { readBytes() }
                _fileUploadState.value = FileUploadUiState.Parsing(
                    fileName = safeName,
                    stageLabel = "Parsing spreadsheet & profiling schema...",
                )
                val parsed = withContext(Dispatchers.Default) {
                    DatasetParser.parseSpreadsheetBytes(safeName, bytes, mimeType)
                }
                uploadParsedTableInternal(parsed)
                _fileUploadState.value = FileUploadUiState.Success(
                    fileName = parsed.name,
                    rowCount = parsed.qualityReport.rowCount,
                    columnCount = parsed.qualityReport.columnCount,
                    qualityScore = parsed.qualityReport.qualityScore,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "Unsupported or malformed CSV/Excel file."
                _fileUploadState.value = FileUploadUiState.Error(safeName, msg)
                _statusBanner.value = "Upload error: $msg"
            }
        }
    }

    fun uploadParsedTable(table: DataTable) {
        viewModelScope.launch {
            uploadParsedTableInternal(table)
        }
    }

    private suspend fun uploadParsedTableInternal(table: DataTable) {
        activateTable(table)
        _rawOriginalTable.value = table
        _cleaningComparison.value = null
        val csv = withContext(Dispatchers.Default) { DatasetParser.toCsvString(table) }
        val record = DatasetRecord(
            id = table.id,
            userId = currentUserId,
            name = table.name,
            sourceType = table.sourceType,
            rowCount = table.qualityReport.rowCount,
            columnCount = table.qualityReport.columnCount,
            qualityScore = table.qualityReport.qualityScore,
            missingCount = table.qualityReport.missingCells,
            duplicateCount = table.qualityReport.duplicateRows,
            outlierCount = table.qualityReport.outlierCells,
            columnsSummary = table.headers,
            csvContent = csv,
            isCleaned = table.isCleaned,
        )
        val res = repository.saveDataset(currentUserId, record)
        if (res.isSuccess) {
            repository.logAuditEvent(
                currentUserId,
                "DATASET_UPLOAD",
                "datasets",
                "Uploaded '${table.name}' (${table.rows.size} rows, score ${table.qualityReport.qualityScore})",
            )
            _statusBanner.value = "Loaded '${table.name}' (${table.rows.size} rows • Quality ${table.qualityReport.qualityScore}/100)"
        } else {
            _statusBanner.value = "Dataset loaded locally, cloud sync error: ${res.exceptionOrNull()?.localizedMessage}"
        }
    }

    fun loadSampleDemoDataset() {
        val sample = DatasetParser.sampleRetailDataset()
        uploadParsedTable(sample)
    }

    fun deleteDataset(datasetId: String) {
        viewModelScope.launch {
            val res = repository.deleteDataset(currentUserId, datasetId)
            if (res.isSuccess) {
                if (_activeTable.value?.id == datasetId) {
                    _activeTable.value = null
                    _biInsights.value = null
                    _forecastReport.value = null
                    _sqlResult.value = null
                }
                repository.logAuditEvent(currentUserId, "DATASET_DELETE", "datasets", "Deleted dataset $datasetId")
                _statusBanner.value = "Dataset deleted."
            } else {
                _statusBanner.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to delete dataset"
            }
        }
    }

    fun cleanActiveDataset(options: CleaningOptions) {
        val current = _activeTable.value ?: return
        viewModelScope.launch {
            val (cleaned, diff) = withContext(Dispatchers.Default) {
                DataQualityEngine.cleanDataset(current, options)
            }
            if (_rawOriginalTable.value == null) _rawOriginalTable.value = current
            _cleaningComparison.value = diff
            activateTable(cleaned)

            val csv = withContext(Dispatchers.Default) { DatasetParser.toCsvString(cleaned) }
            val record = DatasetRecord(
                id = "",
                userId = currentUserId,
                name = cleaned.name,
                sourceType = cleaned.sourceType,
                rowCount = cleaned.qualityReport.rowCount,
                columnCount = cleaned.qualityReport.columnCount,
                qualityScore = cleaned.qualityReport.qualityScore,
                missingCount = cleaned.qualityReport.missingCells,
                duplicateCount = cleaned.qualityReport.duplicateRows,
                outlierCount = cleaned.qualityReport.outlierCells,
                columnsSummary = cleaned.headers,
                csvContent = csv,
                isCleaned = true,
            )
            repository.saveDataset(currentUserId, record)
            repository.logAuditEvent(
                currentUserId,
                "DATA_CLEANING",
                "datasets",
                "Cleaned '${current.name}': Quality ${diff.beforeScore} -> ${diff.afterScore}",
            )
            _statusBanner.value = "Cleaned dataset saved (Quality ${diff.beforeScore} → ${diff.afterScore}/100)"
        }
    }

    private fun activateTable(table: DataTable) {
        _activeTable.value = table
        val suggestedSql = LocalSqlEngine.defaultSuggestedQuery(table)
        _sqlQueryText.value = suggestedSql
        _sqlResult.value = LocalSqlEngine.execute(table, suggestedSql, readOnly = true)
        _biInsights.value = AnalyticsComputeEngine.computeBiInsights(table)
        _forecastReport.value = ForecastingEngine.forecast(table, _forecastMetric.value, _forecastHorizon.value)
        _pythonOutput.value = AnalyticsComputeEngine.executePythonStyleScript(table, _pythonScript.value)
        _dashboardWidgets.value = defaultWidgetsFor(table)
    }

    fun askAiAnalyst(question: String, onSpeakReady: ((String) -> Unit)? = null) {
        val table = _activeTable.value
        val trimmed = question.trim()
        if (trimmed.isEmpty()) return
        if (table == null) {
            _statusBanner.value = "Upload a dataset or load the Sample Retail Workspace first."
            return
        }
        if (_isAgentRunning.value) return

        val timeLabel = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val userMsg = AnalystMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = trimmed,
            timestampLabel = timeLabel,
        )
        _analystMessages.value = _analystMessages.value + userMsg
        _isAgentRunning.value = true

        viewModelScope.launch {
            val activeJobId = repository.createAnalysisJob(
                currentUserId,
                AnalysisJob(
                    userId = currentUserId,
                    datasetId = table.id,
                    datasetName = table.name,
                    question = trimmed,
                    status = AnalysisJobStatus.RUNNING,
                    currentAgent = "AI Orchestrator",
                    progressPercent = 10,
                ),
            ).getOrNull()

            try {
                val response = orchestrator.runAnalysis(table, trimmed) { steps ->
                    _liveAgentSteps.value = steps
                }
                val aiMsg = AnalystMessage(
                    id = UUID.randomUUID().toString(),
                    isUser = false,
                    text = response.executiveSummary,
                    timestampLabel = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                    structured = response,
                    agentTrace = _liveAgentSteps.value,
                )
                _analystMessages.value = _analystMessages.value + aiMsg

                if (response.sqlQuery.isNotBlank()) {
                    _sqlQueryText.value = response.sqlQuery
                    _sqlResult.value = LocalSqlEngine.execute(table, response.sqlQuery, readOnly = true)
                }
                if (response.pythonScript.isNotBlank()) {
                    _pythonScript.value = response.pythonScript
                    _pythonOutput.value = AnalyticsComputeEngine.executePythonStyleScript(table, response.pythonScript)
                }

                val traceSummary = _liveAgentSteps.value.joinToString(" | ") {
                    "${it.agentName}: ${it.status.name} (${it.outputSummary})"
                }

                if (!activeJobId.isNullOrBlank()) {
                    repository.updateAnalysisJob(
                        currentUserId,
                        AnalysisJob(
                            id = activeJobId,
                            userId = currentUserId,
                            datasetId = table.id,
                            datasetName = table.name,
                            question = trimmed,
                            status = AnalysisJobStatus.COMPLETED,
                            currentAgent = "Verification & Business Insights",
                            progressPercent = 100,
                            executiveSummary = response.executiveSummary,
                            sqlGenerated = response.sqlQuery,
                            pythonGenerated = response.pythonScript,
                            agentTraceSummary = traceSummary,
                        ),
                    )
                }

                repository.saveAnalysis(
                    currentUserId,
                    AnalysisRecord(
                        userId = currentUserId,
                        datasetId = table.id,
                        datasetName = table.name,
                        question = trimmed,
                        executiveSummary = response.executiveSummary,
                        sqlGenerated = response.sqlQuery,
                        pythonGenerated = response.pythonScript,
                        agentTraceSummary = traceSummary,
                    ),
                )
                repository.logAuditEvent(
                    currentUserId,
                    "AI_ANALYSIS",
                    "analyses",
                    "Question on '${table.name}': ${trimmed.take(80)}",
                )
                onSpeakReady?.invoke(response.executiveSummary)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Analyst failed", e)
                val errMsg = e.localizedMessage ?: "AI analysis failed."
                if (!activeJobId.isNullOrBlank()) {
                    repository.updateAnalysisJob(
                        currentUserId,
                        AnalysisJob(
                            id = activeJobId,
                            userId = currentUserId,
                            datasetId = table.id,
                            datasetName = table.name,
                            question = trimmed,
                            status = AnalysisJobStatus.FAILED,
                            currentAgent = "AI Orchestrator",
                            progressPercent = 100,
                            errorMessage = errMsg,
                        ),
                    )
                }
                _analystMessages.value = _analystMessages.value + AnalystMessage(
                    id = UUID.randomUUID().toString(),
                    isUser = false,
                    text = "Analysis failed: $errMsg",
                    timestampLabel = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                    agentTrace = _liveAgentSteps.value,
                )
                _statusBanner.value = errMsg
            } finally {
                _isAgentRunning.value = false
            }
        }
    }

    fun updateSqlQuery(sql: String) {
        _sqlQueryText.value = sql
    }

    fun toggleSqlReadOnly() {
        _sqlReadOnly.value = !_sqlReadOnly.value
    }

    fun executeSql(saveTitle: String? = null) {
        val table = _activeTable.value ?: run {
            _statusBanner.value = "Select or upload a dataset first."
            return
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                LocalSqlEngine.execute(table, _sqlQueryText.value, _sqlReadOnly.value)
            }
            _sqlResult.value = result
            if (result.errorMessage == null && !saveTitle.isNullOrBlank()) {
                repository.saveQuery(
                    currentUserId,
                    SavedQueryRecord(
                        userId = currentUserId,
                        datasetId = table.id,
                        datasetName = table.name,
                        title = saveTitle.trim(),
                        sqlText = result.sql,
                        rowCount = result.rowCount,
                        executionMs = result.executionMs,
                    ),
                )
                repository.logAuditEvent(currentUserId, "SQL_SAVE", "saved_queries", "Saved query '$saveTitle'")
                _statusBanner.value = "Saved query '$saveTitle'"
            }
        }
    }

    fun updatePythonScript(code: String) {
        _pythonScript.value = code
    }

    fun executePython() {
        val table = _activeTable.value ?: run {
            _statusBanner.value = "Select or upload a dataset first."
            return
        }
        viewModelScope.launch {
            val output = withContext(Dispatchers.Default) {
                AnalyticsComputeEngine.executePythonStyleScript(table, _pythonScript.value)
            }
            _pythonOutput.value = output
        }
    }

    fun updateForecastParams(metric: String?, horizon: Int) {
        _forecastMetric.value = metric
        _forecastHorizon.value = horizon
        val table = _activeTable.value ?: return
        viewModelScope.launch(Dispatchers.Default) {
            _forecastReport.value = ForecastingEngine.forecast(table, metric, horizon)
        }
    }

    fun setDashboardDateFilter(filter: String) {
        _dashboardDateFilter.value = filter
    }

    fun addDashboardWidget(widget: DashboardWidgetSpec) {
        _dashboardWidgets.value = _dashboardWidgets.value + widget
    }

    fun removeDashboardWidget(id: String) {
        _dashboardWidgets.value = _dashboardWidgets.value.filterNot { it.id == id }
    }

    fun moveWidget(index: Int, delta: Int) {
        val list = _dashboardWidgets.value.toMutableList()
        val target = index + delta
        if (index in list.indices && target in list.indices) {
            val item = list.removeAt(index)
            list.add(target, item)
            _dashboardWidgets.value = list
        }
    }

    fun saveCurrentDashboard(title: String) {
        val table = _activeTable.value ?: return
        viewModelScope.launch {
            val arr = JSONArray()
            _dashboardWidgets.value.forEach { w ->
                arr.put(
                    JSONObject().apply {
                        put("id", w.id)
                        put("title", w.title)
                        put("widgetType", w.widgetType)
                        put("metricColumn", w.metricColumn)
                        put("dimensionColumn", w.dimensionColumn)
                        put("aggregation", w.aggregation)
                    }
                )
            }
            val res = repository.saveDashboard(
                currentUserId,
                DashboardRecord(
                    userId = currentUserId,
                    datasetId = table.id,
                    title = title.ifBlank { "${table.name} Dashboard" },
                    widgetsConfig = arr.toString(),
                    dateFilter = _dashboardDateFilter.value,
                ),
            )
            if (res.isSuccess) {
                repository.logAuditEvent(currentUserId, "DASHBOARD_SAVE", "dashboards", "Saved dashboard '$title'")
                _statusBanner.value = "Dashboard saved to cloud workspace."
            } else {
                _statusBanner.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to save dashboard"
            }
        }
    }

    fun loadSavedDashboard(record: DashboardRecord) {
        try {
            val arr = JSONArray(record.widgetsConfig)
            val parsed = (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                DashboardWidgetSpec(
                    id = o.optString("id", UUID.randomUUID().toString()),
                    title = o.optString("title", "Chart"),
                    widgetType = o.optString("widgetType", "bar"),
                    metricColumn = o.optString("metricColumn", ""),
                    dimensionColumn = o.optString("dimensionColumn", ""),
                    aggregation = o.optString("aggregation", "SUM"),
                )
            }
            _dashboardWidgets.value = parsed
            _dashboardDateFilter.value = record.dateFilter
            _statusBanner.value = "Loaded dashboard '${record.title}'"
        } catch (e: Exception) {
            _statusBanner.value = "Unable to parse saved dashboard: ${e.localizedMessage}"
        }
    }

    fun recordGeneratedReport(title: String, format: String, summary: String) {
        val table = _activeTable.value ?: return
        viewModelScope.launch {
            repository.saveReport(
                currentUserId,
                ReportRecord(
                    userId = currentUserId,
                    datasetId = table.id,
                    datasetName = table.name,
                    title = title,
                    format = format,
                    summary = summary,
                ),
            )
            repository.logAuditEvent(currentUserId, "REPORT_EXPORT", "reports", "Exported $format report '$title'")
            _statusBanner.value = "Report '$title' exported & saved to Reports Center."
        }
    }

    fun saveIntegration(provider: String, label: String, hostOrUri: String, status: String) {
        viewModelScope.launch {
            val res = repository.saveIntegration(
                currentUserId,
                IntegrationRecord(
                    userId = currentUserId,
                    provider = provider,
                    label = label,
                    hostOrUri = hostOrUri,
                    status = status,
                ),
            )
            if (res.isSuccess) {
                repository.logAuditEvent(currentUserId, "INTEGRATION_SAVE", "integrations", "Configured $provider ($label)")
                _statusBanner.value = "Saved $label connector."
            }
        }
    }

    fun deleteIntegration(id: String) {
        viewModelScope.launch {
            repository.deleteIntegration(currentUserId, id)
        }
    }

    fun saveAutomationSchedule(
        title: String,
        frequency: String,
        anomalyAlerts: Boolean,
        emailDigestConsent: Boolean,
    ) {
        val table = _activeTable.value ?: run {
            _statusBanner.value = "Load a dataset before scheduling monitoring."
            return
        }
        viewModelScope.launch {
            val qualityMsg = "Initial check: Quality ${table.qualityReport.qualityScore}/100, ${table.qualityReport.outlierCells} anomalies."
            val res = repository.saveSchedule(
                currentUserId,
                AutomationSchedule(
                    userId = currentUserId,
                    datasetId = table.id,
                    title = title.ifBlank { "Monitor ${table.name}" },
                    frequency = frequency,
                    enabled = true,
                    anomalyAlerts = anomalyAlerts,
                    emailDigestConsent = emailDigestConsent,
                    lastRunSummary = qualityMsg,
                ),
            )
            if (res.isSuccess) {
                repository.logAuditEvent(currentUserId, "SCHEDULE_CREATE", "schedules", "Created $frequency schedule '$title'")
                _statusBanner.value = "Scheduled monitoring rule active."
            }
        }
    }

    fun runScheduleCheckNow(schedule: AutomationSchedule) {
        val table = _activeTable.value ?: return
        val bi = AnalyticsComputeEngine.computeBiInsights(table)
        val summary = "Checked at ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}: " +
            "Quality ${table.qualityReport.qualityScore}/100, ${table.qualityReport.outlierCells} outliers. " +
            bi.rootCauseSummary.take(120)
        viewModelScope.launch {
            repository.saveSchedule(currentUserId, schedule.copy(lastRunSummary = summary))
            repository.logAuditEvent(currentUserId, "SCHEDULE_RUN", "schedules", "Ran '${schedule.title}'")
            _statusBanner.value = "Monitoring check complete."
        }
    }

    fun updateSettings(
        displayName: String,
        organizationName: String,
        planTier: String,
        notificationsEnabled: Boolean,
        retentionDays: Int,
    ) {
        viewModelScope.launch {
            val res = repository.updateUserProfile(
                userId = currentUserId,
                displayName = displayName,
                organizationName = organizationName,
                planTier = planTier,
                notificationsEnabled = notificationsEnabled,
                retentionDays = retentionDays,
            )
            if (res.isSuccess) {
                repository.logAuditEvent(currentUserId, "SETTINGS_UPDATE", "users", "Updated workspace settings (plan=$planTier)")
                _statusBanner.value = "Workspace settings saved."
            } else {
                _statusBanner.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to update settings"
            }
        }
    }

    fun deleteAllWorkspaceData() {
        viewModelScope.launch {
            val res = repository.deleteWorkspaceData(currentUserId)
            if (res.isSuccess) {
                _activeTable.value = null
                _rawOriginalTable.value = null
                _cleaningComparison.value = null
                _biInsights.value = null
                _forecastReport.value = null
                _sqlResult.value = null
                _analystMessages.value = emptyList()
                _statusBanner.value = "All workspace datasets, queries, dashboards, and reports deleted."
            } else {
                _statusBanner.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to purge workspace"
            }
        }
    }

    private fun defaultWidgetsFor(table: DataTable): List<DashboardWidgetSpec> {
        val numCol = table.columnProfiles.firstOrNull {
            it.type == ColumnType.NUMERIC && (it.name.contains("rev", true) || it.name.contains("sales", true))
        }?.name ?: table.columnProfiles.firstOrNull { it.type == ColumnType.NUMERIC }?.name ?: return emptyList()

        val catCol = table.columnProfiles.firstOrNull { it.type == ColumnType.CATEGORICAL }?.name ?: table.headers.first()
        val dateCol = table.columnProfiles.firstOrNull { it.type == ColumnType.DATE }?.name ?: catCol
        val secondCat = table.columnProfiles.filter { it.type == ColumnType.CATEGORICAL }.getOrNull(1)?.name ?: catCol

        return listOf(
            DashboardWidgetSpec("w_kpi_1", "Total $numCol", "kpi", numCol, catCol, "SUM"),
            DashboardWidgetSpec("w_bar_1", "$numCol by $catCol", "bar", numCol, catCol, "SUM"),
            DashboardWidgetSpec("w_line_1", "$numCol Trend", "line", numCol, dateCol, "SUM"),
            DashboardWidgetSpec("w_pie_1", "$numCol Share ($secondCat)", "pie", numCol, secondCat, "SUM"),
        )
    }

    private companion object {
        const val TAG = "DataPilotVM"
    }
}
