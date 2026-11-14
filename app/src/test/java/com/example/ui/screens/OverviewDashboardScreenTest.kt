package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import com.example.data.model.AnalysisRecord
import com.example.data.model.DatasetRecord
import com.example.domain.engine.DatasetParser
import com.example.ui.theme.DataPilotTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w1024dp-h900dp")
class OverviewDashboardScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `renders metric cards on compact phone screen`() {
        val sample = DatasetParser.sampleRetailDataset()
        val datasets = listOf(
            DatasetRecord(
                id = "ds1",
                userId = "u1",
                name = sample.name,
                rowCount = sample.rows.size,
                columnCount = sample.headers.size,
                qualityScore = sample.qualityReport.qualityScore,
            ),
        )
        val analyses = listOf(
            AnalysisRecord(
                id = "an1",
                userId = "u1",
                datasetId = "ds1",
                datasetName = sample.name,
                question = "Why did sales dip?",
                executiveSummary = "Revenue declined 12% in LATAM.",
            ),
        )

        composeRule.setContent {
            DataPilotTheme {
                Box(modifier = Modifier.size(width = 360.dp, height = 800.dp)) {
                    OverviewDashboardScreen(
                        activeTable = sample,
                        datasets = datasets,
                        analyses = analyses,
                        reports = emptyList(),
                        schedules = emptyList(),
                        onUploadClick = {},
                        onLoadSampleClick = {},
                        onSelectDataset = {},
                        onNavigateToAnalyst = {},
                        onNavigateToExplorer = {},
                    )
                }
            }
        }

        composeRule.onNodeWithTag("kpi_total_datasets_card").assertIsDisplayed()
        composeRule.onNodeWithTag("kpi_recent_analyses_card").assertIsDisplayed()
        composeRule.onNodeWithText("Total Datasets").assertIsDisplayed()
        composeRule.onNodeWithText("Data Quality").assertIsDisplayed()
        composeRule.onNodeWithText("Active Monitors").assertIsDisplayed()
        composeRule.onNodeWithTag("overview_dashboard_list")
            .performScrollToNode(hasTestTag("recent_analyses_card"))
        composeRule.onNodeWithTag("recent_analyses_card").assertIsDisplayed()
        composeRule.onNodeWithText("Why did sales dip?").assertIsDisplayed()
    }

    @Test
    fun `renders responsive 4-column metric row on expanded tablet screen`() {
        val sample = DatasetParser.sampleRetailDataset()
        val datasets = listOf(
            DatasetRecord(
                id = "ds1",
                userId = "u1",
                name = sample.name,
                rowCount = sample.rows.size,
                columnCount = sample.headers.size,
                qualityScore = 95,
            ),
        )
        val analyses = listOf(
            AnalysisRecord(
                id = "an1",
                userId = "u1",
                datasetId = "ds1",
                datasetName = sample.name,
                question = "Top regions?",
                executiveSummary = "North America leads.",
            ),
        )

        composeRule.setContent {
            DataPilotTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    OverviewDashboardScreen(
                        activeTable = sample,
                        datasets = datasets,
                        analyses = analyses,
                        reports = emptyList(),
                        schedules = emptyList(),
                        onUploadClick = {},
                        onLoadSampleClick = {},
                        onSelectDataset = {},
                        onNavigateToAnalyst = {},
                        onNavigateToExplorer = {},
                    )
                }
            }
        }

        composeRule.onNodeWithTag("kpi_total_datasets_card").assertIsDisplayed()
        composeRule.onNodeWithTag("kpi_recent_analyses_card").assertIsDisplayed()
        composeRule.onNodeWithTag("recent_analyses_card").assertIsDisplayed()
        composeRule.onNodeWithText("Top regions?").assertIsDisplayed()
    }
}
