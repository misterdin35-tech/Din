package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AnalysisRecord
import com.example.data.model.AutomationSchedule
import com.example.data.model.DatasetRecord
import com.example.data.model.ReportRecord
import com.example.domain.engine.AnalyticsComputeEngine
import com.example.domain.model.ChartKind
import com.example.domain.model.ColumnType
import com.example.domain.model.DataTable
import com.example.ui.components.DataChartCard
import com.example.ui.components.EmptyDatasetPromptCard
import com.example.ui.components.GlassCard
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NeuralViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun OverviewDashboardScreen(
    activeTable: DataTable?,
    datasets: List<DatasetRecord>,
    analyses: List<AnalysisRecord>,
    reports: List<ReportRecord>,
    schedules: List<AutomationSchedule>,
    onUploadClick: () -> Unit,
    onLoadSampleClick: () -> Unit,
    onSelectDataset: (DatasetRecord) -> Unit,
    onNavigateToAnalyst: () -> Unit,
    onNavigateToExplorer: () -> Unit,
    onNavigateToReports: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
) {
    val avgQuality = if (datasets.isNotEmpty()) {
        datasets.map { it.qualityScore }.average().toInt()
    } else {
        activeTable?.qualityReport?.qualityScore ?: 0
    }
    val activeMonitors = schedules.count { it.enabled }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        val width = maxWidth
        val isCompact = width < 600.dp
        val isExpanded = width >= 840.dp
        val isVeryNarrow = width < 400.dp

        LazyColumn(
            modifier = Modifier
                .widthIn(max = 1200.dp)
                .fillMaxSize()
                .testTag("overview_dashboard_list"),
            contentPadding = PaddingValues(if (isCompact) 16.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                OverviewHeaderCard(
                    activeTable = activeTable,
                    datasetCount = datasets.size,
                    isVeryNarrow = isVeryNarrow,
                    onUploadClick = onUploadClick,
                )
            }

            item {
                val metricCards: List<@Composable (Modifier) -> Unit> = listOf(
                    { mod ->
                        KpiMetricCard(
                            title = "Total Datasets",
                            value = datasets.size.toString(),
                            subtitle = if (datasets.isEmpty()) "Upload CSV/XLSX" else "${datasets.sumOf { it.rowCount }} total rows",
                            icon = Icons.Rounded.Storage,
                            accentColor = ElectricBlue,
                            modifier = mod.testTag("kpi_total_datasets_card"),
                            onClick = onNavigateToExplorer,
                        )
                    },
                    { mod ->
                        KpiMetricCard(
                            title = "Recent Analyses",
                            value = analyses.size.toString(),
                            subtitle = if (analyses.isEmpty()) "Ask 7-Agent AI" else "Latest: ${analyses.first().datasetName.take(16)}",
                            icon = Icons.Rounded.Psychology,
                            accentColor = CyberCyan,
                            modifier = mod.testTag("kpi_recent_analyses_card"),
                            onClick = onNavigateToAnalyst,
                        )
                    },
                    { mod ->
                        KpiMetricCard(
                            title = "Data Quality",
                            value = if (datasets.isEmpty() && activeTable == null) "—" else "$avgQuality/100",
                            subtitle = when {
                                datasets.isEmpty() && activeTable == null -> "Awaiting upload"
                                avgQuality >= 90 -> "Production Ready"
                                avgQuality >= 70 -> "Cleaning Advised"
                                else -> "Needs Audit"
                            },
                            icon = Icons.Rounded.HealthAndSafety,
                            accentColor = if (avgQuality >= 85) EmeraldSuccess else AmberWarning,
                            modifier = mod.testTag("kpi_data_quality_card"),
                            onClick = onNavigateToExplorer,
                        )
                    },
                    { mod ->
                        KpiMetricCard(
                            title = "Active Monitors",
                            value = activeMonitors.toString(),
                            subtitle = "${reports.size} saved reports",
                            icon = Icons.Rounded.Schedule,
                            accentColor = NeuralViolet,
                            modifier = mod.testTag("kpi_active_monitors_card"),
                            onClick = onNavigateToHistory,
                        )
                    },
                )

                if (isExpanded) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        metricCards.forEach { card ->
                            card(
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            metricCards[0](
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                            metricCards[1](
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            metricCards[2](
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                            metricCards[3](
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                }
            }

            if (activeTable == null) {
                item {
                    EmptyDatasetPromptCard(
                        onUploadClick = onUploadClick,
                        onLoadSampleClick = onLoadSampleClick,
                    )
                }
            } else {
                val catCol = activeTable.columnProfiles.firstOrNull { it.type == ColumnType.CATEGORICAL }
                val numCol = activeTable.columnProfiles.firstOrNull { it.type == ColumnType.NUMERIC }
                if (catCol != null && numCol != null) {
                    item {
                        val chart = remember(activeTable) {
                            AnalyticsComputeEngine.buildChartFromColumns(
                                table = activeTable,
                                kind = ChartKind.BAR,
                                dimensionColumn = catCol.name,
                                metricColumn = numCol.name,
                                title = "${numCol.name} by ${catCol.name}",
                            )
                        }
                        if (isExpanded) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                DataChartCard(chartSpec = chart, modifier = Modifier.weight(1.2f))
                                RecentAnalysesFeedCard(
                                    analyses = analyses,
                                    onNavigateToAnalyst = onNavigateToAnalyst,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        } else {
                            DataChartCard(chartSpec = chart)
                        }
                    }
                }
            }

            if (datasets.isNotEmpty()) {
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Workspace Datasets", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            TextButton(onClick = onNavigateToExplorer) {
                                Text("Manage", color = CyberCyan)
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = "Manage datasets",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(datasets, key = { it.id }) { ds ->
                                val isActive = ds.id == activeTable?.id || ds.name == activeTable?.name
                                GlassCard(
                                    onClick = { onSelectDataset(ds) },
                                    modifier = Modifier.width(220.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        StatusPill(
                                            label = if (isActive) "ACTIVE" else ds.sourceType.uppercase(),
                                            color = if (isActive) EmeraldSuccess else ElectricBlue,
                                        )
                                        Text("${ds.qualityScore}%", style = MaterialTheme.typography.labelSmall, color = CyberCyan)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = ds.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${ds.rowCount} rows • ${ds.columnCount} cols",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                if (!isCompact) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        if (!isExpanded || activeTable == null) {
                            RecentAnalysesFeedCard(
                                analyses = analyses,
                                onNavigateToAnalyst = onNavigateToAnalyst,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                            )
                        }
                        RecentReportsFeedCard(
                            reports = reports,
                            onNavigateToReports = onNavigateToReports,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        RecentAnalysesFeedCard(
                            analyses = analyses,
                            onNavigateToAnalyst = onNavigateToAnalyst,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        RecentReportsFeedCard(
                            reports = reports,
                            onNavigateToReports = onNavigateToReports,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

