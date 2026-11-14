package com.example.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.repository.DataPilotRepository
import com.example.domain.agent.MultiAgentOrchestrator
import com.example.domain.engine.AnalyticsComputeEngine
import com.example.domain.engine.DatasetParser
import com.example.domain.engine.PdfReportGenerator
import com.example.firebase.signOutUser
import com.example.ui.navigation.DataPilotNavHost
import com.example.ui.navigation.navigateToWorkspaceDestination
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SubtleDivider
import com.example.ui.theme.TextPrimary
import com.example.ui.viewmodel.DataPilotViewModel
import com.example.ui.viewmodel.StreamState
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MainWorkspaceShell(
    currentUserId: String,
    displayName: String,
    email: String,
    onSignOutComplete: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: DataPilotViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val db = FirebaseFirestore.getInstance(
                    context.applicationContext.getString(R.string.firestore_database_id)
                )
                DataPilotViewModel(
                    repository = DataPilotRepository(db),
                    orchestrator = MultiAgentOrchestrator(),
                    currentUserId = currentUserId,
                    initialDisplayName = displayName,
                    initialEmail = email,
                )
            }
        },
    )

    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val datasetsState by viewModel.datasetsState.collectAsStateWithLifecycle()
    val analysesState by viewModel.analysesState.collectAsStateWithLifecycle()
    val savedQueriesState by viewModel.savedQueriesState.collectAsStateWithLifecycle()
    val dashboardsState by viewModel.dashboardsState.collectAsStateWithLifecycle()
    val reportsState by viewModel.reportsState.collectAsStateWithLifecycle()
    val integrationsState by viewModel.integrationsState.collectAsStateWithLifecycle()
    val schedulesState by viewModel.schedulesState.collectAsStateWithLifecycle()
    val auditLogsState by viewModel.auditLogsState.collectAsStateWithLifecycle()

    val activeTable by viewModel.activeTable.collectAsStateWithLifecycle()
    val cleaningComparison by viewModel.cleaningComparison.collectAsStateWithLifecycle()
    val sqlQuery by viewModel.sqlQueryText.collectAsStateWithLifecycle()
    val sqlReadOnly by viewModel.sqlReadOnly.collectAsStateWithLifecycle()
    val sqlResult by viewModel.sqlResult.collectAsStateWithLifecycle()
    val pythonScript by viewModel.pythonScript.collectAsStateWithLifecycle()
    val pythonOutput by viewModel.pythonOutput.collectAsStateWithLifecycle()
    val biInsights by viewModel.biInsights.collectAsStateWithLifecycle()
    val forecastReport by viewModel.forecastReport.collectAsStateWithLifecycle()
    val forecastHorizon by viewModel.forecastHorizon.collectAsStateWithLifecycle()
    val forecastMetric by viewModel.forecastMetric.collectAsStateWithLifecycle()
    val dashboardWidgets by viewModel.dashboardWidgets.collectAsStateWithLifecycle()
    val dashboardDateFilter by viewModel.dashboardDateFilter.collectAsStateWithLifecycle()
    val analystMessages by viewModel.analystMessages.collectAsStateWithLifecycle()
    val liveAgentSteps by viewModel.liveAgentSteps.collectAsStateWithLifecycle()
    val isAgentRunning by viewModel.isAgentRunning.collectAsStateWithLifecycle()
    val globalSearch by viewModel.globalSearch.collectAsStateWithLifecycle()
    val fileUploadState by viewModel.fileUploadState.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    val datasets = (datasetsState as? StreamState.Ready)?.data.orEmpty()
    val analyses = (analysesState as? StreamState.Ready)?.data.orEmpty()
    val savedQueries = (savedQueriesState as? StreamState.Ready)?.data.orEmpty()
    val dashboards = (dashboardsState as? StreamState.Ready)?.data.orEmpty()
    val reports = (reportsState as? StreamState.Ready)?.data.orEmpty()
    val integrations = (integrationsState as? StreamState.Ready)?.data.orEmpty()
    val schedules = (schedulesState as? StreamState.Ready)?.data.orEmpty()
    val auditLogs = (auditLogsState as? StreamState.Ready)?.data.orEmpty()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    var pendingCsvContent by remember { mutableStateOf<String?>(null) }
    var pendingPdfMeta by remember { mutableStateOf<Pair<String, String>?>(null) }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val fileName = queryDisplayName(context, uri) ?: "dataset.csv"
            val mime = context.contentResolver.getType(uri)
            viewModel.ingestDeviceFile(fileName = fileName, mimeType = mime) {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("Unable to read selected file.")
            }
        }
    }

    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val content = pendingCsvContent
        if (uri != null && content != null) {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(content.toByteArray(Charsets.UTF_8))
                        }
                    }
                    snackbarHostState.showSnackbar("CSV exported successfully.")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("CSV export failed: ${e.localizedMessage}")
                } finally {
                    pendingCsvContent = null
                }
            }
        }
    }

    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val table = activeTable
        val meta = pendingPdfMeta
        if (uri != null && table != null && meta != null) {
            scope.launch {
                try {
                    val bi = biInsights ?: AnalyticsComputeEngine.computeBiInsights(table)
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            PdfReportGenerator.writeExecutivePdf(
                                outputStream = out,
                                reportTitle = meta.first,
                                table = table,
                                biReport = bi,
                                forecastReport = forecastReport,
                                executiveSummary = meta.second,
                            )
                        }
                    }
                    viewModel.recordGeneratedReport(meta.first, "pdf", meta.second)
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("PDF export failed: ${e.localizedMessage}")
                } finally {
                    pendingPdfMeta = null
                }
            }
        }
    }

    val launchUpload: () -> Unit = {
        openDocumentLauncher.launch(
            arrayOf(
                "text/csv",
                "text/comma-separated-values",
                "application/csv",
                "text/tab-separated-values",
                "text/plain",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.ms-excel",
                "*/*",
            )
        )
    }

    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: ROUTE_OVERVIEW
    val currentNavLabel = workspaceNavItems.firstOrNull { it.route == currentRoute }?.label ?: "Dashboard"

    val navigateTo: (String) -> Unit = { route ->
        navController.navigateToWorkspaceDestination(route)
    }

    var isSearchOpen by remember { mutableStateOf(false) }
    var showAlertsDialog by remember { mutableStateOf(false) }
    var isDrawerExpanded by remember { mutableStateOf(true) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val initials = remember(userProfile?.displayName, displayName) {
        (userProfile?.displayName ?: displayName)
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
    }

    val alertCount = remember(activeTable) {
        val qr = activeTable?.qualityReport ?: return@remember 0
        listOf(qr.missingCells, qr.duplicateRows, qr.outlierCells).count { it > 0 }
    }

    if (showAlertsDialog) {
        WorkspaceAlertsDialog(
            activeTable = activeTable,
            schedules = schedules,
            onDismiss = { showAlertsDialog = false },
            onGoToExplorer = { navigateTo(ROUTE_EXPLORER) },
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ObsidianBg, MidnightSurface, ObsidianBg)))
    ) {
        val isCompactWidth = maxWidth < 600.dp
        val isExpandedWidth = maxWidth >= 840.dp

        val scaffoldBody: @Composable (onMenuAction: () -> Unit, showBottomBar: Boolean) -> Unit = { onMenuAction, showBottomBar ->
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    Column {
                        WorkspaceTopBar(
                            currentTitle = currentNavLabel,
                            globalSearch = globalSearch,
                            isSearchOpen = isSearchOpen,
                            alertCount = alertCount,
                            userInitials = initials,
                            onMenuClick = onMenuAction,
                            onToggleSearch = {
                                isSearchOpen = !isSearchOpen
                                if (!isSearchOpen) viewModel.updateGlobalSearch("")
                            },
                            onSearchChange = viewModel::updateGlobalSearch,
                            onAlertsClick = { showAlertsDialog = true },
                            onProfileClick = { navigateTo(ROUTE_SETTINGS) },
                        )
                        if (isSearchOpen && globalSearch.isNotBlank()) {
                            GlobalSearchOverlay(
                                query = globalSearch,
                                datasets = datasets,
                                onSelectRoute = { route ->
                                    isSearchOpen = false
                                    viewModel.updateGlobalSearch("")
                                    navigateTo(route)
                                },
                                onSelectDataset = { ds ->
                                    isSearchOpen = false
                                    viewModel.updateGlobalSearch("")
                                    viewModel.selectDatasetRecord(ds)
                                    navigateTo(ROUTE_EXPLORER)
                                },
                            )
                        }
                    }
                },
                bottomBar = {
                    if (showBottomBar) {
                        WorkspaceBottomBar(
                            currentRoute = currentRoute,
                            onNavigate = navigateTo,
                        )
                    }
                },
            ) { innerPadding ->
                DataPilotNavHost(
                    navController = navController,
                    activeTable = activeTable,
                    datasets = datasets,
                    analyses = analyses,
                    savedQueries = savedQueries,
                    dashboards = dashboards,
                    reports = reports,
                    integrations = integrations,
                    schedules = schedules,
                    auditLogs = auditLogs,
                    userProfile = userProfile,
                    userEmail = email,
                    cleaningComparison = cleaningComparison,
                    sqlQuery = sqlQuery,
                    sqlReadOnly = sqlReadOnly,
                    sqlResult = sqlResult,
                    pythonScript = pythonScript,
                    pythonOutput = pythonOutput,
                    biInsights = biInsights,
                    forecastReport = forecastReport,
                    forecastHorizon = forecastHorizon,
                    forecastMetric = forecastMetric,
                    dashboardWidgets = dashboardWidgets,
                    dashboardDateFilter = dashboardDateFilter,
                    analystMessages = analystMessages,
                    liveAgentSteps = liveAgentSteps,
                    isAgentRunning = isAgentRunning,
                    fileUploadState = fileUploadState,
                    onDismissUploadStatus = viewModel::clearFileUploadState,
                    onUploadClick = launchUpload,
                    onLoadSampleClick = viewModel::loadSampleDemoDataset,
                    onSelectDataset = viewModel::selectDatasetRecord,
                    onDeleteDataset = viewModel::deleteDataset,
                    onCleanDataset = viewModel::cleanActiveDataset,
                    onExportCleanedCsv = { table ->
                        pendingCsvContent = DatasetParser.toCsvString(table)
                        createCsvLauncher.launch("${table.name.substringBeforeLast('.')}_cleaned.csv")
                    },
                    onAskQuestion = viewModel::askAiAnalyst,
                    onSqlChange = viewModel::updateSqlQuery,
                    onToggleSqlReadOnly = viewModel::toggleSqlReadOnly,
                    onExecuteSql = viewModel::executeSql,
                    onExportSqlCsv = { res ->
                        val sb = StringBuilder()
                        sb.appendLine(res.headers.joinToString(","))
                        res.rows.forEach { row -> sb.appendLine(row.joinToString(",")) }
                        pendingCsvContent = sb.toString()
                        createCsvLauncher.launch("sql_query_result.csv")
                    },
                    onPythonScriptChange = viewModel::updatePythonScript,
                    onExecutePython = viewModel::executePython,
                    onUpdateForecast = viewModel::updateForecastParams,
                    onDateFilterChange = viewModel::setDashboardDateFilter,
                    onAddWidget = viewModel::addDashboardWidget,
                    onMoveWidget = viewModel::moveWidget,
                    onRemoveWidget = viewModel::removeDashboardWidget,
                    onSaveDashboard = viewModel::saveCurrentDashboard,
                    onLoadDashboard = viewModel::loadSavedDashboard,
                    onExportPdf = { title, summary ->
                        pendingPdfMeta = title to summary
                        createPdfLauncher.launch("${title.replace(' ', '_').lowercase()}.pdf")
                    },
                    onExportCsv = { table ->
                        pendingCsvContent = DatasetParser.toCsvString(table)
                        createCsvLauncher.launch("${table.name.substringBeforeLast('.')}_report.csv")
                    },
                    onSaveExecutiveReport = { title, summary ->
                        viewModel.recordGeneratedReport(title, "summary", summary)
                    },
                    onSaveIntegration = viewModel::saveIntegration,
                    onDeleteIntegration = viewModel::deleteIntegration,
                    onCreateSchedule = viewModel::saveAutomationSchedule,
                    onRunScheduleNow = viewModel::runScheduleCheckNow,
                    onSaveSettings = viewModel::updateSettings,
                    onDeleteAllWorkspaceData = viewModel::deleteAllWorkspaceData,
                    onSignOut = {
                        signOutUser(
                            credentialManager = credentialManager,
                            scope = scope,
                            onSignOutComplete = onSignOutComplete,
                        )
                    },
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }

        if (isExpandedWidth) {
            Row(modifier = Modifier.fillMaxSize()) {
                if (isDrawerExpanded) {
                    WorkspaceDrawerContent(
                        currentRoute = currentRoute,
                        planTier = userProfile?.planTier ?: "free",
                        activeDatasetName = activeTable?.name,
                        onNavigate = navigateTo,
                        modifier = Modifier.width(268.dp),
                    )
                } else {
                    WorkspaceNavigationRail(
                        currentRoute = currentRoute,
                        onMenuClick = { isDrawerExpanded = true },
                        onNavigate = navigateTo,
                    )
                }
                VerticalDivider(color = SubtleDivider, modifier = Modifier.fillMaxHeight())
                Box(modifier = Modifier.weight(1f)) {
                    scaffoldBody({ isDrawerExpanded = !isDrawerExpanded }, false)
                }
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = MidnightSurface,
                        drawerContentColor = TextPrimary,
                        modifier = Modifier.width(290.dp),
                    ) {
                        WorkspaceDrawerContent(
                            currentRoute = currentRoute,
                            planTier = userProfile?.planTier ?: "free",
                            activeDatasetName = activeTable?.name,
                            onNavigate = { route ->
                                scope.launch { drawerState.close() }
                                navigateTo(route)
                            },
                        )
                    }
                },
            ) {
                if (isCompactWidth) {
                    scaffoldBody({ scope.launch { drawerState.open() } }, true)
                } else {
                    Row(modifier = Modifier.fillMaxSize()) {
                        WorkspaceNavigationRail(
                            currentRoute = currentRoute,
                            onMenuClick = { scope.launch { drawerState.open() } },
                            onNavigate = navigateTo,
                        )
                        VerticalDivider(color = SubtleDivider, modifier = Modifier.fillMaxHeight())
                        Box(modifier = Modifier.weight(1f)) {
                            scaffoldBody({ scope.launch { drawerState.open() } }, false)
                        }
                    }
                }
            }
        }
    }
}

private fun queryDisplayName(context: Context, uri: Uri): String? {
    return context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) cursor.getString(idx) else null
        } else null
    }
}
