package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.NeuralViolet
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * UI state for selecting and parsing CSV / Excel files from device storage.
 */
sealed interface FileUploadUiState {
    data object Idle : FileUploadUiState
    data class Parsing(
        val fileName: String,
        val stageLabel: String = "Parsing spreadsheet & profiling schema...",
    ) : FileUploadUiState

    data class Success(
        val fileName: String,
        val rowCount: Int,
        val columnCount: Int,
        val qualityScore: Int,
    ) : FileUploadUiState

    data class Error(
        val fileName: String?,
        val message: String,
    ) : FileUploadUiState
}

/**
 * File upload dropzone component for Data Explorer supporting CSV, TSV, and Excel (.xlsx/.xls XML)
 * selection from device storage with an animated parsing/loading state.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DatasetUploadCard(
    uploadState: FileUploadUiState,
    onSelectFileClick: () -> Unit,
    onLoadSampleClick: () -> Unit,
    onDismissStatus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isParsing = uploadState is FileUploadUiState.Parsing
    val accentColor = when (uploadState) {
        is FileUploadUiState.Error -> CoralError
        is FileUploadUiState.Success -> EmeraldSuccess
        is FileUploadUiState.Parsing -> CyberCyan
        FileUploadUiState.Idle -> ElectricBlue
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
            contentColor = TextPrimary,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("dataset_upload_card"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(SlateCard.copy(alpha = 0.94f), MidnightSurface.copy(alpha = 0.98f))
                    )
                )
                .drawBehind {
                    val stroke = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f),
                    )
                    drawRoundRect(
                        color = accentColor.copy(alpha = 0.55f),
                        cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
                        style = stroke,
                    )
                }
                .padding(16.dp),
        ) {
            AnimatedContent(
                targetState = uploadState,
                label = "dataset_upload_state",
            ) { state ->
                when (state) {
                    is FileUploadUiState.Parsing -> {
                        ParsingStateContent(state = state)
                    }

                    is FileUploadUiState.Success -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(
                                color = EmeraldSuccess.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.45f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("file_upload_success_banner"),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Dataset parsed successfully",
                                        tint = EmeraldSuccess,
                                        modifier = Modifier.size(22.dp),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Parsed ${state.fileName}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = "${state.rowCount} rows × ${state.columnCount} columns • Quality ${state.qualityScore}/100",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                        )
                                    }
                                    IconButton(
                                        onClick = onDismissStatus,
                                        modifier = Modifier.testTag("dismiss_upload_status_button"),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Dismiss upload status",
                                            tint = TextSecondary,
                                        )
                                    }
                                }
                            }
                            UploadActionsRow(
                                isParsing = false,
                                primaryLabel = "Upload Another File",
                                onSelectFileClick = onSelectFileClick,
                                onLoadSampleClick = onLoadSampleClick,
                            )
                        }
                    }

                    is FileUploadUiState.Error -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(
                                color = CoralError.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, CoralError.copy(alpha = 0.45f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("file_upload_error_banner"),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ErrorOutline,
                                        contentDescription = "Upload error",
                                        tint = CoralError,
                                        modifier = Modifier.size(22.dp),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = state.fileName?.let { "Failed to parse $it" } ?: "File upload failed",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary,
                                        )
                                        Text(
                                            text = state.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                        )
                                    }
                                    IconButton(
                                        onClick = onDismissStatus,
                                        modifier = Modifier.testTag("dismiss_upload_status_button"),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Dismiss upload error",
                                            tint = TextSecondary,
                                        )
                                    }
                                }
                            }
                            UploadActionsRow(
                                isParsing = false,
                                primaryLabel = "Try Another File",
                                onSelectFileClick = onSelectFileClick,
                                onLoadSampleClick = onLoadSampleClick,
                            )
                        }
                    }

                    FileUploadUiState.Idle -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(ElectricBlue.copy(alpha = 0.25f), CyberCyan.copy(alpha = 0.22f))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CloudUpload,
                                        contentDescription = "Import dataset file",
                                        tint = CyberCyan,
                                        modifier = Modifier.size(26.dp),
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Import CSV or Excel File",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = "Select a spreadsheet from your device to profile columns, detect anomalies, and unlock AI & SQL analysis.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                    )
                                }
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                FormatBadge(".CSV", CyberCyan)
                                FormatBadge(".TSV", ElectricBlue)
                                FormatBadge(".XLSX Excel", NeuralViolet)
                                FormatBadge("Auto Quality Audit", EmeraldSuccess)
                            }

                            UploadActionsRow(
                                isParsing = isParsing,
                                primaryLabel = "Select CSV / Excel",
                                onSelectFileClick = onSelectFileClick,
                                onLoadSampleClick = onLoadSampleClick,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParsingStateContent(state: FileUploadUiState.Parsing) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("file_upload_loading_state"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            CircularProgressIndicator(
                color = CyberCyan,
                trackColor = SlateElevated,
                strokeWidth = 3.dp,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("file_parsing_spinner"),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Parsing ${state.fileName}...",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = state.stageLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = CyberCyan,
                )
            }
            Icon(
                imageVector = Icons.Rounded.TableChart,
                contentDescription = null,
                tint = ElectricBlue,
            )
        }
        LinearProgressIndicator(
            color = CyberCyan,
            trackColor = SlateElevated,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(4.dp))
                .testTag("file_parsing_progress_bar"),
        )
        Text(
            text = "Detecting column types, missing cells, duplicate rows, and IQR outliers...",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
        )
    }
}

@Composable
private fun FormatBadge(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun UploadActionsRow(
    isParsing: Boolean,
    primaryLabel: String,
    onSelectFileClick: () -> Unit,
    onLoadSampleClick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        GlowActionButton(
            text = primaryLabel,
            icon = Icons.Rounded.CloudUpload,
            onClick = onSelectFileClick,
            enabled = !isParsing,
            modifier = Modifier.weight(1f),
            testTag = "data_explorer_select_file_button",
        )
        OutlinedButton(
            onClick = onLoadSampleClick,
            enabled = !isParsing,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, NeuralViolet.copy(alpha = 0.6f)),
            modifier = Modifier
                .height(48.dp)
                .testTag("data_explorer_load_sample_button"),
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = NeuralViolet,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text("Sample", color = TextPrimary)
        }
    }
}
