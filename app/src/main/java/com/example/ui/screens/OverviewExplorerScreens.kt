package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DatasetRecord
import com.example.domain.engine.AnalyticsComputeEngine
import com.example.domain.model.CleaningComparison
import com.example.domain.model.CleaningOptions
import com.example.domain.model.ColumnType
import com.example.domain.model.DataTable
import com.example.ui.components.DatasetUploadCard
import com.example.ui.components.FileUploadUiState
import com.example.ui.components.GlassCard
import com.example.ui.components.GlowActionButton
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NeuralViolet
import com.example.ui.theme.SlateElevated
import com.example.ui.theme.SubtleDivider
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DataExplorerScreen(
    activeTable: DataTable?,
    datasets: List<DatasetRecord>,
    cleaningComparison: CleaningComparison?,
    fileUploadState: FileUploadUiState = FileUploadUiState.Idle,
    onDismissUploadStatus: () -> Unit = {},
    onUploadClick: () -> Unit,
    onLoadSampleClick: () -> Unit,
    onSelectDataset: (DatasetRecord) -> Unit,
    onDeleteDataset: (String) -> Unit,
    onCleanDataset: (CleaningOptions) -> Unit,
    onExportCleanedCsv: (DataTable) -> Unit,
) {
    var removeDups by remember { mutableStateOf(true) }
    var imputeMissing by remember { mutableStateOf(true) }
    var capOutliers by remember { mutableStateOf(true) }
    var trimWs by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("data_explorer_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Data Explorer & Quality", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                    Text("CSV, TSV & Excel (.xlsx) ingestion & autonomous cleaning", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                GlowActionButton(
                    text = if (fileUploadState is FileUploadUiState.Parsing) "Parsing..." else "Upload",
                    icon = Icons.Rounded.UploadFile,
                    enabled = fileUploadState !is FileUploadUiState.Parsing,
                    onClick = onUploadClick,
                    testTag = "explorer_upload_button",
                )
            }
        }

        item {
            DatasetUploadCard(
                uploadState = fileUploadState,
                onSelectFileClick = onUploadClick,
                onLoadSampleClick = onLoadSampleClick,
                onDismissStatus = onDismissUploadStatus,
            )
        }

        if (datasets.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(datasets) { ds ->
                        val selected = ds.id == activeTable?.id || ds.name == activeTable?.name
                        Surface(
                            onClick = { onSelectDataset(ds) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) ElectricBlue.copy(alpha = 0.22f) else SlateElevated,
                            border = BorderStroke(1.dp, if (selected) CyberCyan else SubtleDivider),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                            ) {
                                Text(ds.name, style = MaterialTheme.typography.labelLarge, color = TextPrimary, maxLines = 1)
                                IconButton(
                                    onClick = { onDeleteDataset(ds.id) },
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete dataset", tint = TextMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (activeTable == null) {
            return@LazyColumn
        }

        val qr = activeTable.qualityReport
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(activeTable.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("${qr.rowCount} rows × ${qr.columnCount} columns", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    StatusPill(
                        label = "SCORE ${qr.qualityScore}/100",
                        color = if (qr.qualityScore >= 85) EmeraldSuccess else AmberWarning,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    QualityCounter("Missing", qr.missingCells, if (qr.missingCells > 0) AmberWarning else EmeraldSuccess)
                    QualityCounter("Duplicates", qr.duplicateRows, if (qr.duplicateRows > 0) CoralError else EmeraldSuccess)
                    QualityCounter("IQR Outliers", qr.outlierCells, if (qr.outlierCells > 0) NeuralViolet else EmeraldSuccess)
                }
                Spacer(Modifier.height(10.dp))
                qr.suggestions.forEach { s ->
                    Text("• $s", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Autonomous Data Cleaning", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text("Preserves original dataset while producing a verified cleaned copy.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                CleanOptionRow("Remove exact duplicate rows", removeDups) { removeDups = it }
                CleanOptionRow("Impute missing cells (median / mode)", imputeMissing) { imputeMissing = it }
                CleanOptionRow("Winsorize statistical IQR outliers", capOutliers) { capOutliers = it }
                CleanOptionRow("Trim whitespace", trimWs) { trimWs = it }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    GlowActionButton(
                        text = "Clean Dataset",
                        icon = Icons.Rounded.CleaningServices,
                        onClick = {
                            onCleanDataset(CleaningOptions(removeDups, imputeMissing, capOutliers, trimWs))
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "clean_dataset_button",
                    )
                    OutlinedButton(
                        onClick = { onExportCleanedCsv(activeTable) },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("export_csv_button"),
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Export CSV", color = CyberCyan)
                    }
                }
            }
        }

        if (cleaningComparison != null) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = EmeraldSuccess)
                        Spacer(Modifier.width(8.dp))
                        Text("Before & After Cleaning Comparison", style = MaterialTheme.typography.titleMedium, color = EmeraldSuccess)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Quality Score: ${cleaningComparison.beforeScore}/100 → ${cleaningComparison.afterScore}/100",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Rows: ${cleaningComparison.beforeRows} → ${cleaningComparison.afterRows}  |  Missing: ${cleaningComparison.beforeMissing} → ${cleaningComparison.afterMissing}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Text(
                        "Duplicates: ${cleaningComparison.beforeDuplicates} → ${cleaningComparison.afterDuplicates}  |  Outliers: ${cleaningComparison.beforeOutliers} → ${cleaningComparison.afterOutliers}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Column Schema & Statistics", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                activeTable.columnProfiles.forEach { col ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(col.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            val detail = if (col.type == ColumnType.NUMERIC) {
                                "Min ${AnalyticsComputeEngine.formatCompact(col.min ?: 0.0)} • Med ${AnalyticsComputeEngine.formatCompact(col.median ?: 0.0)} • Max ${AnalyticsComputeEngine.formatCompact(col.max ?: 0.0)}"
                            } else {
                                "${col.uniqueCount} unique • Missing ${col.missingCount}"
                            }
                            Text(detail, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        StatusPill(label = col.type.name, color = CyberCyan)
                    }
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Dataset Preview (First 20 Rows)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                val scrollState = rememberScrollState()
                Column(modifier = Modifier.horizontalScroll(scrollState)) {
                    Row(
                        modifier = Modifier
                            .background(ElectricBlue.copy(alpha = 0.2f))
                            .padding(vertical = 6.dp, horizontal = 8.dp),
                    ) {
                        activeTable.headers.forEach { h ->
                            Text(
                                text = h,
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.width(115.dp),
                            )
                        }
                    }
                    activeTable.rows.take(20).forEach { row ->
                        Row(
                            modifier = Modifier
                                .padding(vertical = 5.dp, horizontal = 8.dp),
                        ) {
                            row.forEach { cell ->
                                Text(
                                    text = cell.ifBlank { "NULL" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (cell.isBlank()) AmberWarning else TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.width(115.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QualityCounter(label: String, count: Int, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count.toString(), style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

@Composable
private fun CleanOptionRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = CyberCyan),
        )
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
    }
}
