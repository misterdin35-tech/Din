package com.example.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.domain.engine.DatasetParser
import com.example.ui.ROUTE_ANALYST
import com.example.ui.ROUTE_EXPLORER
import com.example.ui.ROUTE_OVERVIEW
import com.example.ui.ROUTE_SQL_STUDIO
import com.example.ui.WorkspaceBottomBar
import com.example.ui.WorkspaceDrawerContent
import com.example.ui.WorkspaceTopBar
import com.example.ui.theme.DataPilotTheme
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.workspaceNavItems
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class DataPilotNavigationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun bottomNavigationBar_transitionsBetweenDashboardExplorerAnalystAndSqlStudio() {
        var capturedNavController: NavHostController? = null

        composeRule.setContent {
            val navController = rememberNavController()
            capturedNavController = navController
            val backStack by navController.currentBackStackEntryAsState()
            val currentRoute = backStack?.destination?.route ?: ROUTE_OVERVIEW
            val currentTitle = workspaceNavItems.firstOrNull { it.route == currentRoute }?.label ?: "Dashboard"
            val sampleTable = DatasetParser.sampleRetailDataset()

            DataPilotTheme {
                Scaffold(
                    topBar = {
                        WorkspaceTopBar(
                            currentTitle = currentTitle,
                            globalSearch = "",
                            isSearchOpen = false,
                            alertCount = 0,
                            userInitials = "DP",
                            onMenuClick = {},
                            onToggleSearch = {},
                            onSearchChange = {},
                            onAlertsClick = {},
                            onProfileClick = {},
                        )
                    },
                    bottomBar = {
                        WorkspaceBottomBar(
                            currentRoute = currentRoute,
                            onNavigate = { route -> navController.navigateToWorkspaceDestination(route) },
                        )
                    },
                ) { innerPadding ->
                    DataPilotNavHost(
                        navController = navController,
                        activeTable = sampleTable,
                        datasets = emptyList(),
                        analyses = emptyList(),
                        savedQueries = emptyList(),
                        dashboards = emptyList(),
                        reports = emptyList(),
                        integrations = emptyList(),
                        schedules = emptyList(),
                        auditLogs = emptyList(),
                        userProfile = null,
                        userEmail = "analyst@datapilot.ai",
                        cleaningComparison = null,
                        sqlQuery = "SELECT * FROM dataset LIMIT 10;",
                        sqlReadOnly = true,
                        sqlResult = null,
                        pythonScript = "df.describe()",
                        pythonOutput = null,
                        biInsights = null,
                        forecastReport = null,
                        forecastHorizon = 6,
                        forecastMetric = null,
                        dashboardWidgets = emptyList(),
                        dashboardDateFilter = "All Time",
                        analystMessages = emptyList(),
                        liveAgentSteps = emptyList(),
                        isAgentRunning = false,
                        onUploadClick = {},
                        onLoadSampleClick = {},
                        onSelectDataset = {},
                        onDeleteDataset = {},
                        onCleanDataset = {},
                        onExportCleanedCsv = {},
                        onAskQuestion = { _, _ -> },
                        onSqlChange = {},
                        onToggleSqlReadOnly = {},
                        onExecuteSql = {},
                        onExportSqlCsv = {},
                        onPythonScriptChange = {},
                        onExecutePython = {},
                        onUpdateForecast = { _, _ -> },
                        onDateFilterChange = {},
                        onAddWidget = {},
                        onMoveWidget = { _, _ -> },
                        onRemoveWidget = {},
                        onSaveDashboard = {},
                        onLoadDashboard = {},
                        onExportPdf = { _, _ -> },
                        onExportCsv = {},
                        onSaveExecutiveReport = { _, _ -> },
                        onSaveIntegration = { _, _, _, _ -> },
                        onDeleteIntegration = {},
                        onCreateSchedule = { _, _, _, _ -> },
                        onRunScheduleNow = {},
                        onSaveSettings = { _, _, _, _, _ -> },
                        onDeleteAllWorkspaceData = {},
                        onSignOut = {},
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }

        composeRule.waitForIdle()
        assertEquals(ROUTE_OVERVIEW, capturedNavController?.currentDestination?.route)
        composeRule.onNodeWithTag("screen_$ROUTE_OVERVIEW").assertIsDisplayed()
        composeRule.onNodeWithTag("top_bar_title").assertTextEquals("Dashboard")

        // Navigate to Data Explorer
        composeRule.onNodeWithTag("bottom_nav_$ROUTE_EXPLORER").performClick()
        composeRule.waitForIdle()
        assertEquals(ROUTE_EXPLORER, capturedNavController?.currentDestination?.route)
        composeRule.onNodeWithTag("screen_$ROUTE_EXPLORER").assertIsDisplayed()
        composeRule.onNodeWithTag("top_bar_title").assertTextEquals("Data Explorer")

        // Navigate to AI Analyst
        composeRule.onNodeWithTag("bottom_nav_$ROUTE_ANALYST").performClick()
        composeRule.waitForIdle()
        assertEquals(ROUTE_ANALYST, capturedNavController?.currentDestination?.route)
        composeRule.onNodeWithTag("screen_$ROUTE_ANALYST").assertIsDisplayed()
        composeRule.onNodeWithTag("top_bar_title").assertTextEquals("AI Analyst")

        // Navigate to SQL Studio
        composeRule.onNodeWithTag("bottom_nav_$ROUTE_SQL_STUDIO").performClick()
        composeRule.waitForIdle()
        assertEquals(ROUTE_SQL_STUDIO, capturedNavController?.currentDestination?.route)
        composeRule.onNodeWithTag("screen_$ROUTE_SQL_STUDIO").assertIsDisplayed()
        composeRule.onNodeWithTag("top_bar_title").assertTextEquals("SQL Studio")

        // Navigate back to Dashboard
        composeRule.onNodeWithTag("bottom_nav_$ROUTE_OVERVIEW").performClick()
        composeRule.waitForIdle()
        assertEquals(ROUTE_OVERVIEW, capturedNavController?.currentDestination?.route)
        composeRule.onNodeWithTag("screen_$ROUTE_OVERVIEW").assertIsDisplayed()
        composeRule.onNodeWithTag("top_bar_title").assertTextEquals("Dashboard")
    }

    @Test
    fun navigationDrawer_transitionsBetweenCoreWorkspaceScreens() {
        var capturedNavController: NavHostController? = null

        composeRule.setContent {
            val navController = rememberNavController()
            capturedNavController = navController
            val backStack by navController.currentBackStackEntryAsState()
            val currentRoute = backStack?.destination?.route ?: ROUTE_OVERVIEW
            val currentTitle = workspaceNavItems.firstOrNull { it.route == currentRoute }?.label ?: "Dashboard"
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val scope = rememberCoroutineScope()

            DataPilotTheme {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet(
                            drawerContainerColor = MidnightSurface,
                            drawerContentColor = TextPrimary,
                        ) {
                            WorkspaceDrawerContent(
                                currentRoute = currentRoute,
                                planTier = "pro",
                                activeDatasetName = "retail.csv",
                                onNavigate = { route ->
                                    scope.launch { drawerState.close() }
                                    navController.navigateToWorkspaceDestination(route)
                                },
                            )
                        }
                    },
                ) {
                    Scaffold(
                        topBar = {
                            WorkspaceTopBar(
                                currentTitle = currentTitle,
                                globalSearch = "",
                                isSearchOpen = false,
                                alertCount = 0,
                                userInitials = "DP",
                                onMenuClick = { scope.launch { drawerState.open() } },
                                onToggleSearch = {},
                                onSearchChange = {},
                                onAlertsClick = {},
                                onProfileClick = {},
                            )
                        },
                    ) { innerPadding ->
                        DataPilotNavHost(
                            navController = navController,
                            activeTable = null,
                            datasets = emptyList(),
                            analyses = emptyList(),
                            savedQueries = emptyList(),
                            dashboards = emptyList(),
                            reports = emptyList(),
                            integrations = emptyList(),
                            schedules = emptyList(),
                            auditLogs = emptyList(),
                            userProfile = null,
                            userEmail = "analyst@datapilot.ai",
                            cleaningComparison = null,
                            sqlQuery = "SELECT * FROM dataset;",
                            sqlReadOnly = true,
                            sqlResult = null,
                            pythonScript = "df.info()",
                            pythonOutput = null,
                            biInsights = null,
                            forecastReport = null,
                            forecastHorizon = 6,
                            forecastMetric = null,
                            dashboardWidgets = emptyList(),
                            dashboardDateFilter = "All Time",
                            analystMessages = emptyList(),
                            liveAgentSteps = emptyList(),
                            isAgentRunning = false,
                            onUploadClick = {},
                            onLoadSampleClick = {},
                            onSelectDataset = {},
                            onDeleteDataset = {},
                            onCleanDataset = {},
                            onExportCleanedCsv = {},
                            onAskQuestion = { _, _ -> },
                            onSqlChange = {},
                            onToggleSqlReadOnly = {},
                            onExecuteSql = {},
                            onExportSqlCsv = {},
                            onPythonScriptChange = {},
                            onExecutePython = {},
                            onUpdateForecast = { _, _ -> },
                            onDateFilterChange = {},
                            onAddWidget = {},
                            onMoveWidget = { _, _ -> },
                            onRemoveWidget = {},
                            onSaveDashboard = {},
                            onLoadDashboard = {},
                            onExportPdf = { _, _ -> },
                            onExportCsv = {},
                            onSaveExecutiveReport = { _, _ -> },
                            onSaveIntegration = { _, _, _, _ -> },
                            onDeleteIntegration = {},
                            onCreateSchedule = { _, _, _, _ -> },
                            onRunScheduleNow = {},
                            onSaveSettings = { _, _, _, _, _ -> },
                            onDeleteAllWorkspaceData = {},
                            onSignOut = {},
                            modifier = Modifier.padding(innerPadding),
                        )
                    }
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("drawer_menu_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("drawer_nav_$ROUTE_SQL_STUDIO").performClick()
        composeRule.waitForIdle()

        assertEquals(ROUTE_SQL_STUDIO, capturedNavController?.currentDestination?.route)
        composeRule.onNodeWithTag("screen_$ROUTE_SQL_STUDIO").assertIsDisplayed()

        composeRule.onNodeWithTag("drawer_menu_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("drawer_nav_$ROUTE_EXPLORER").performClick()
        composeRule.waitForIdle()

        assertEquals(ROUTE_EXPLORER, capturedNavController?.currentDestination?.route)
        composeRule.onNodeWithTag("screen_$ROUTE_EXPLORER").assertIsDisplayed()
    }
}
