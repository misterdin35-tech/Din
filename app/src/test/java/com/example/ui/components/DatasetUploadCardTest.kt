package com.example.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.ui.screens.DataExplorerScreen
import com.example.ui.theme.DataPilotTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class DatasetUploadCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun idleState_displaysCsvAndExcelBadgesAndTriggersPickerCallbacks() {
        var selectClicked = false
        var sampleClicked = false

        composeRule.setContent {
            DataPilotTheme {
                DatasetUploadCard(
                    uploadState = FileUploadUiState.Idle,
                    onSelectFileClick = { selectClicked = true },
                    onLoadSampleClick = { sampleClicked = true },
                    onDismissStatus = {},
                )
            }
        }

        composeRule.onNodeWithTag("dataset_upload_card").assertIsDisplayed()
        composeRule.onNodeWithText(".CSV").assertIsDisplayed()
        composeRule.onNodeWithText(".TSV").assertIsDisplayed()
        composeRule.onNodeWithText(".XLSX Excel").assertIsDisplayed()

        composeRule.onNodeWithTag("data_explorer_select_file_button").performClick()
        composeRule.onNodeWithTag("data_explorer_load_sample_button").performClick()

        assertTrue(selectClicked)
        assertTrue(sampleClicked)
    }

    @Test
    fun parsingState_displaysLoadingSpinnerProgressBarAndFilenameInDataExplorer() {
        composeRule.setContent {
            DataPilotTheme {
                DataExplorerScreen(
                    activeTable = null,
                    datasets = emptyList(),
                    cleaningComparison = null,
                    fileUploadState = FileUploadUiState.Parsing(
                        fileName = "quarterly_revenue.xlsx",
                        stageLabel = "Parsing spreadsheet & profiling schema...",
                    ),
                    onDismissUploadStatus = {},
                    onUploadClick = {},
                    onLoadSampleClick = {},
                    onSelectDataset = {},
                    onDeleteDataset = {},
                    onCleanDataset = {},
                    onExportCleanedCsv = {},
                )
            }
        }

        composeRule.onNodeWithTag("dataset_upload_card").assertIsDisplayed()
        composeRule.onNodeWithTag("file_upload_loading_state").assertIsDisplayed()
        composeRule.onNodeWithTag("file_parsing_spinner").assertIsDisplayed()
        composeRule.onNodeWithTag("file_parsing_progress_bar").assertIsDisplayed()
        composeRule.onNodeWithText("Parsing quarterly_revenue.xlsx...").assertIsDisplayed()
        composeRule.onNodeWithText("Parsing spreadsheet & profiling schema...").assertIsDisplayed()
        composeRule.onNodeWithTag("explorer_upload_button").assertIsNotEnabled()
    }

    @Test
    fun successAndErrorStates_renderFeedbackAndDismissAction() {
        var dismissed = false

        composeRule.setContent {
            DataPilotTheme {
                DatasetUploadCard(
                    uploadState = FileUploadUiState.Success(
                        fileName = "customers.csv",
                        rowCount = 120,
                        columnCount = 8,
                        qualityScore = 94,
                    ),
                    onSelectFileClick = {},
                    onLoadSampleClick = {},
                    onDismissStatus = { dismissed = true },
                )
            }
        }

        composeRule.onNodeWithTag("file_upload_success_banner").assertIsDisplayed()
        composeRule.onNodeWithText("Parsed customers.csv").assertIsDisplayed()
        composeRule.onNodeWithText("120 rows × 8 columns • Quality 94/100").assertIsDisplayed()
        composeRule.onNodeWithTag("dismiss_upload_status_button").performClick()
        assertTrue(dismissed)
    }

    @Test
    fun errorState_rendersErrorBannerAndDismissAction() {
        var dismissed = false

        composeRule.setContent {
            DataPilotTheme {
                DatasetUploadCard(
                    uploadState = FileUploadUiState.Error(
                        fileName = "broken.xls",
                        message = "Legacy binary .xls format is not supported directly.",
                    ),
                    onSelectFileClick = {},
                    onLoadSampleClick = {},
                    onDismissStatus = { dismissed = true },
                )
            }
        }

        composeRule.onNodeWithTag("file_upload_error_banner").assertIsDisplayed()
        composeRule.onNodeWithText("Failed to parse broken.xls").assertIsDisplayed()
        composeRule.onNodeWithTag("dismiss_upload_status_button").performClick()
        assertTrue(dismissed)
    }
}
