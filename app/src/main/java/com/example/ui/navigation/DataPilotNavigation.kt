package com.example.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.data.model.AnalysisRecord
import com.example.data.model.AuditLogRecord
import com.example.data.model.AutomationSchedule
import com.example.data.model.DashboardRecord
import com.example.data.model.DatasetRecord
import com.example.data.model.IntegrationRecord
import com.example.data.model.ReportRecord
import com.example.data.model.SavedQueryRecord
import com.example.data.model.UserProfile
import com.example.domain.engine.PythonExecutionOutput
import com.example.domain.model.AgentStepState
import com.example.domain.model.AnalystMessage
import com.example.domain.model.BiInsightsReport
import com.example.domain.model.CleaningComparison
import com.example.domain.model.CleaningOptions
import com.example.domain.model.DashboardWidgetSpec
import com.example.domain.model.DataTable
import com.example.domain.model.ForecastReport
import com.example.domain.model.SqlExecutionResult
import com.example.ui.ROUTE_ANALYST
import com.example.ui.ROUTE_BUSINESS_INTEL
import com.example.ui.ROUTE_DASHBOARD_BUILDER
import com.example.ui.ROUTE_EXPLORER
import com.example.ui.ROUTE_HISTORY
import com.example.ui.ROUTE_INTEGRATIONS
import com.example.ui.ROUTE_OVERVIEW
import com.example.ui.ROUTE_PREDICTIVE
import com.example.ui.ROUTE_PYTHON_STUDIO
import com.example.ui.ROUTE_REPORTS
import com.example.ui.ROUTE_SETTINGS
import com.example.ui.ROUTE_SQL_STUDIO
import com.example.ui.components.FileUploadUiState
import com.example.ui.screens.AnalystWorkspaceScreen
import com.example.ui.screens.BusinessIntelligenceScreen
import com.example.ui.screens.DashboardBuilderScreen
import com.example.ui.screens.DataExplorerScreen
import com.example.ui.screens.HistoryAutomationScreen
import com.example.ui.screens.IntegrationsScreen
import com.example.ui.screens.OverviewDashboardScreen
import com.example.ui.screens.PredictiveAnalyticsScreen
import com.example.ui.screens.PythonStudioScreen
import com.example.ui.screens.ReportsCenterScreen
import com.example.ui.screens.SettingsPricingScreen
import com.example.ui.screens.SqlStudioScreen

/**
 * Navigates to a top-level workspace destination while saving & restoring tab state
 * and avoiding duplicate backstack entries.
 */
fun NavHostController.navigateToWorkspaceDestination(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun DataPilotNavHost(
    navController: NavHostController,
    startDestination: String = ROUTE_OVERVIEW,
    activeTable: DataTable?,
    datasets: List<DatasetRecord>,
    analyses: List<AnalysisRecord>,
    savedQueries: List<SavedQueryRecord>,
    dashboards: List<DashboardRecord>,
    reports: List<ReportRecord>,
    integrations: List<IntegrationRecord>,
    schedules: List<AutomationSchedule>,
    auditLogs: List<AuditLogRecord>,
    userProfile: UserProfile?,
    userEmail: String,
    cleaningComparison: CleaningComparison?,
    sqlQuery: String,
    sqlReadOnly: Boolean,
    sqlResult: SqlExecutionResult?,
    pythonScript: String,
    pythonOutput: PythonExecutionOutput?,
    biInsights: BiInsightsReport?,
    forecastReport: ForecastReport?,
    forecastHorizon: Int,
    forecastMetric: String?,
    dashboardWidgets: List<DashboardWidgetSpec>,
    dashboardDateFilter: String,
    analystMessages: List<AnalystMessage>,
    liveAgentSteps: List<AgentStepState>,
    isAgentRunning: Boolean,
    fileUploadState: FileUploadUiState = FileUploadUiState.Idle,
    onDismissUploadStatus: () -> Unit = {},
    onUploadClick: () -> Unit,
    onLoadSampleClick: () -> Unit,
    onSelectDataset: (DatasetRecord) -> Unit,
    onDeleteDataset: (String) -> Unit,
    onCleanDataset: (CleaningOptions) -> Unit,
    onExportCleanedCsv: (DataTable) -> Unit,
    onAskQuestion: (String, ((String) -> Unit)?) -> Unit,
    onSqlChange: (String) -> Unit,
    onToggleSqlReadOnly: () -> Unit,
    onExecuteSql: (String?) -> Unit,
    onExportSqlCsv: (SqlExecutionResult) -> Unit,
    onPythonScriptChange: (String) -> Unit,
    onExecutePython: () -> Unit,
    onUpdateForecast: (String?, Int) -> Unit,
    onDateFilterChange: (String) -> Unit,
    onAddWidget: (DashboardWidgetSpec) -> Unit,
    onMoveWidget: (Int, Int) -> Unit,
    onRemoveWidget: (String) -> Unit,
    onSaveDashboard: (String) -> Unit,
    onLoadDashboard: (DashboardRecord) -> Unit,
    onExportPdf: (String, String) -> Unit,
    onExportCsv: (DataTable) -> Unit,
    onSaveExecutiveReport: (String, String) -> Unit,
    onSaveIntegration: (String, String, String, String) -> Unit,
    onDeleteIntegration: (String) -> Unit,
    onCreateSchedule: (String, String, Boolean, Boolean) -> Unit,
    onRunScheduleNow: (AutomationSchedule) -> Unit,
    onSaveSettings: (String, String, String, Boolean, Int) -> Unit,
    onDeleteAllWorkspaceData: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            fadeIn(animationSpec = tween(220)) +
                slideInHorizontally(animationSpec = tween(220)) { it / 24 }
        },
        exitTransition = {
            fadeOut(animationSpec = tween(160))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(220)) +
                slideInHorizontally(animationSpec = tween(220)) { -it / 24 }
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(160))
        },
        modifier = modifier.fillMaxSize(),
    ) {
        composable(ROUTE_OVERVIEW) {
            Box(modifier = Modifier.fillMaxSize().testTag("screen_$ROUTE_OVERVIEW")) {
                OverviewDashboardScreen(
                    activeTable = activeTable,
                    datasets = datasets,
                    analyses = analyses,
                    reports = reports,
                    schedules = schedules,
                    onUploadClick = onUploadClick,
                    onLoadSampleClick = onLoadSampleClick,
                    onSelectDataset = onSelectDataset,
                    onNavigateToAnalyst = { navController.navigateToWorkspaceDestination(ROUTE_ANALYST) },
                    onNavigateToExplorer = { navController.navigateToWorkspaceDestination(ROUTE_EXPLORER) },
                    onNavigateToReports = { navController.navigateToWorkspaceDestination(ROUTE_REPORTS) },
                    onNavigateToHistory = { navController.navigateToWorkspaceDestination(ROUTE_HISTORY) },
                )
            }
        }
        composable(ROUTE_EXPLORER) {
            Box(modifier = Modifier.fillMaxSize().testTag("screen_$ROUTE_EXPLORER")) {
                DataExplorerScreen(
                    activeTable = activeTable,
                    datasets = datasets,
                    cleaningComparison = cleaningComparison,
                    fileUploadState = fileUploadState,
                    onDismissUploadStatus = onDismissUploadStatus,
                    onUploadClick = onUploadClick,
                    onLoadSampleClick = onLoadSampleClick,
                    onSelectDataset = onSelectDataset,
                    onDeleteDataset = onDeleteDataset,
                    onCleanDataset = onCleanDataset,
                    onExportCleanedCsv = onExportCleanedCsv,
                )
            }
        }
        composable(ROUTE_ANALYST) {
            Box(modifier = Modifier.fillMaxSize().testTag("screen_$ROUTE_ANALYST")) {
                AnalystWorkspaceScreen(
                    activeTable = activeTable,
                    messages = analystMessages,
                    liveAgentSteps = liveAgentSteps,
                    isAgentRunning = isAgentRunning,
                    onAskQuestion = onAskQuestion,
                    onOpenInSqlStudio = { sql ->
                        onSqlChange(sql)
                        onExecuteSql(null)
                        navController.navigateToWorkspaceDestination(ROUTE_SQL_STUDIO)
                    },
                    onOpenInPythonStudio = { py ->
                        onPythonScriptChange(py)
                        onExecutePython()
                        navController.navigateToWorkspaceDestination(ROUTE_PYTHON_STUDIO)
                    },
                    onUploadClick = onUploadClick,
                    onLoadSampleClick = onLoadSampleClick,
                )
            }
        }
        composable(ROUTE_SQL_STUDIO) {
            Box(modifier = Modifier.fillMaxSize().testTag("screen_$ROUTE_SQL_STUDIO")) {
                SqlStudioScreen(
                    activeTable = activeTable,
                    sqlQuery = sqlQuery,
                    isReadOnly = sqlReadOnly,
                    sqlResult = sqlResult,
                    savedQueries = savedQueries,
                    onSqlChange = onSqlChange,
                    onToggleReadOnly = onToggleSqlReadOnly,
                    onExecuteSql = onExecuteSql,
                    onExportSqlCsv = onExportSqlCsv,
                    onUploadClick = onUploadClick,
                    onLoadSampleClick = onLoadSampleClick,
                )
            }
        }
        composable(ROUTE_PYTHON_STUDIO) {
            PythonStudioScreen(
                activeTable = activeTable,
                pythonScript = pythonScript,
                pythonOutput = pythonOutput,
                onScriptChange = onPythonScriptChange,
                onRunPython = onExecutePython,
                onUploadClick = onUploadClick,
                onLoadSampleClick = onLoadSampleClick,
            )
        }
        composable(ROUTE_DASHBOARD_BUILDER) {
            DashboardBuilderScreen(
                activeTable = activeTable,
                widgets = dashboardWidgets,
                dateFilter = dashboardDateFilter,
                savedDashboards = dashboards,
                onDateFilterChange = onDateFilterChange,
                onAddWidget = onAddWidget,
                onMoveWidget = onMoveWidget,
                onRemoveWidget = onRemoveWidget,
                onSaveDashboard = onSaveDashboard,
                onLoadDashboard = onLoadDashboard,
                onUploadClick = onUploadClick,
                onLoadSampleClick = onLoadSampleClick,
            )
        }
        composable(ROUTE_PREDICTIVE) {
            PredictiveAnalyticsScreen(
                activeTable = activeTable,
                forecastReport = forecastReport,
                horizonPeriods = forecastHorizon,
                selectedMetric = forecastMetric,
                onUpdateForecast = onUpdateForecast,
                onUploadClick = onUploadClick,
                onLoadSampleClick = onLoadSampleClick,
            )
        }
        composable(ROUTE_BUSINESS_INTEL) {
            BusinessIntelligenceScreen(
                activeTable = activeTable,
                biInsights = biInsights,
                onAskRootCause = { prompt ->
                    navController.navigateToWorkspaceDestination(ROUTE_ANALYST)
                    onAskQuestion(prompt, null)
                },
                onUploadClick = onUploadClick,
                onLoadSampleClick = onLoadSampleClick,
            )
        }
        composable(ROUTE_REPORTS) {
            ReportsCenterScreen(
                activeTable = activeTable,
                biInsights = biInsights,
                forecastReport = forecastReport,
                reports = reports,
                onExportPdf = onExportPdf,
                onExportCsv = onExportCsv,
                onSaveExecutiveReport = onSaveExecutiveReport,
                onUploadClick = onUploadClick,
                onLoadSampleClick = onLoadSampleClick,
            )
        }
        composable(ROUTE_INTEGRATIONS) {
            IntegrationsScreen(
                integrations = integrations,
                onSaveIntegration = onSaveIntegration,
                onDeleteIntegration = onDeleteIntegration,
                onUploadCsvXlsxClick = onUploadClick,
            )
        }
        composable(ROUTE_HISTORY) {
            HistoryAutomationScreen(
                analyses = analyses,
                savedQueries = savedQueries,
                schedules = schedules,
                auditLogs = auditLogs,
                onCreateSchedule = onCreateSchedule,
                onRunScheduleNow = onRunScheduleNow,
                onLoadSqlQuery = { sql ->
                    onSqlChange(sql)
                    onExecuteSql(null)
                    navController.navigateToWorkspaceDestination(ROUTE_SQL_STUDIO)
                },
            )
        }
        composable(ROUTE_SETTINGS) {
            SettingsPricingScreen(
                userProfile = userProfile,
                userEmail = userEmail,
                onSaveSettings = onSaveSettings,
                onDeleteAllWorkspaceData = onDeleteAllWorkspaceData,
                onSignOut = onSignOut,
            )
        }
    }
}
