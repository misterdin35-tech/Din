package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AddChart
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.model.DashboardRecord
import com.example.domain.engine.AnalyticsComputeEngine
import com.example.domain.model.BiInsightsReport
import com.example.domain.model.ChartKind
import com.example.domain.model.ColumnType
import com.example.domain.model.DashboardWidgetSpec
import com.example.domain.model.DataTable
import com.example.domain.model.ForecastReport
import com.example.ui.components.DataChartCard
import com.example.ui.components.EmptyDatasetPromptCard
import com.example.ui.components.ForecastChartCanvas
import com.example.ui.components.GlassCard
import com.example.ui.components.GlowActionButton
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NeuralViolet
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateElevated
import com.example.ui.theme.SubtleDivider
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.UUID

@Composable
fun DashboardBuilderScreen(
    activeTable: DataTable?,
    widgets: List<DashboardWidgetSpec>,
    dateFilter: String,
    savedDashboards: List<DashboardRecord>,
    onDateFilterChange: (String) -> Unit,
    onAddWidget: (DashboardWidgetSpec) -> Unit,
    onMoveWidget: (index: Int, delta: Int) -> Unit,
    onRemoveWidget: (String) -> Unit,
    onSaveDashboard: (String) -> Unit,
    onLoadDashboard: (DashboardRecord) -> Unit,
    onUploadClick: () -> Unit,
    onLoadSampleClick: () -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var dashTitle by remember { mutableStateOf("") }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Dashboard") },
            text = {
                OutlinedTextField(
                    value = dashTitle,
                    onValueChange = { dashTitle = it },
                    label = { Text("Dashboard Title") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSaveDashboard(dashTitle)
                        dashTitle = ""
                        showSaveDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") } },
        )
    }

    if (showAddDialog && activeTable != null) {
        val numerics = activeTable.columnProfiles.filter { it.type == ColumnType.NUMERIC }.map { it.name }
        val dims = activeTable.columnProfiles.filter { it.type != ColumnType.NUMERIC }.map { it.name }.ifEmpty { activeTable.headers }
        var selectedType by remember { mutableStateOf("bar") }
        var selectedMetric by remember { mutableStateOf(numerics.firstOrNull() ?: activeTable.headers.last()) }
        var selectedDim by remember { mutableStateOf(dims.firstOrNull() ?: activeTable.headers.first()) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Dashboard Widget") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Chart Type", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("kpi", "bar", "line", "pie", "scatter")) { t ->
                            FilterChip(
                                selected = selectedType == t,
                                onClick = { selectedType = t },
                                label = { Text(t.uppercase()) },
                            )
                        }
                    }
                    Text("Dimension / X-Axis", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(if (selectedType == "scatter") numerics.ifEmpty { dims } else dims) { d ->
                            FilterChip(
                                selected = selectedDim == d,
                                onClick = { selectedDim = d },
                                label = { Text(d) },
                            )
                        }
                    }
                    Text("Metric / Y-Axis", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(numerics.ifEmpty { activeTable.headers }) { m ->
                            FilterChip(
                                selected = selectedMetric == m,
                                onClick = { selectedMetric = m },
                                label = { Text(m) },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAddWidget(
                            DashboardWidgetSpec(
                                id = "w_${UUID.randomUUID().toString().take(8)}",
                                title = "$selectedMetric by $selectedDim",
                                widgetType = selectedType,
                                metricColumn = selectedMetric,
                                dimensionColumn = selectedDim,
                            )
                        )
                        showAddDialog = false
                    }
                ) { Text("Add Widget") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } },
        )
    }

    val filteredTable = remember(activeTable, dateFilter) {
        if (activeTable == null || dateFilter == "All Time") activeTable
        else {
            val dateIdx = activeTable.columnProfiles.indexOfFirst { it.type == ColumnType.DATE }
            if (dateIdx < 0) activeTable
            else {
                val keepCount = when (dateFilter) {
                    "Latest 25%" -> (activeTable.rows.size / 4).coerceAtLeast(3)
                    "Latest 50%" -> (activeTable.rows.size / 2).coerceAtLeast(5)
                    else -> activeTable.rows.size
                }
                activeTable.copy(rows = activeTable.rows.takeLast(keepCount))
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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
                    Text("Dashboard Builder", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                    Text("Arrange KPI & chart widgets, filter windows & save layouts", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                if (activeTable != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Rounded.AddChart, contentDescription = "Add widget", tint = CyberCyan)
                        }
                        IconButton(onClick = { showSaveDialog = true }) {
                            Icon(Icons.Rounded.Save, contentDescription = "Save dashboard", tint = ElectricBlue)
                        }
                    }
                }
            }
        }

        if (filteredTable == null) {
            item { EmptyDatasetPromptCard(onUploadClick = onUploadClick, onLoadSampleClick = onLoadSampleClick) }
            return@LazyColumn
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("All Time", "Latest 50%", "Latest 25%")) { f ->
                    FilterChip(
                        selected = dateFilter == f,
                        onClick = { onDateFilterChange(f) },
                        label = { Text(f) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricBlue.copy(alpha = 0.25f),
                            selectedLabelColor = CyberCyan,
                            containerColor = SlateCard,
                            labelColor = TextSecondary,
                        ),
                    )
                }
            }
        }

        itemsIndexed(widgets, key = { _, w -> w.id }) { index, widget ->
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = widget.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onMoveWidget(index, -1) }, enabled = index > 0, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move widget up", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { onMoveWidget(index, 1) }, enabled = index < widgets.lastIndex, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move widget down", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { onRemoveWidget(widget.id) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove widget", tint = CoralError, modifier = Modifier.size(16.dp))
                    }
                }

                if (widget.widgetType == "kpi") {
                    val mIdx = filteredTable.headers.indexOfFirst { it.equals(widget.metricColumn, true) }
                    val total = if (mIdx >= 0) {
                        filteredTable.rows.sumOf { it.getOrNull(mIdx)?.replace(",", "")?.toDoubleOrNull() ?: 0.0 }
                    } else 0.0
                    KpiMetricCard(
                        title = widget.title,
                        value = AnalyticsComputeEngine.formatCompact(total),
                        subtitle = "${filteredTable.rows.size} rows ($dateFilter)",
                        icon = Icons.Rounded.AttachMoney,
                        accentColor = CyberCyan,
                    )
                } else {
                    val kind = when (widget.widgetType) {
                        "line" -> ChartKind.LINE
                        "pie" -> ChartKind.PIE
                        "scatter" -> ChartKind.SCATTER
                        else -> ChartKind.BAR
                    }
                    val spec = remember(filteredTable, widget) {
                        AnalyticsComputeEngine.buildChartFromColumns(
                            table = filteredTable,
                            kind = kind,
                            dimensionColumn = widget.dimensionColumn,
                            metricColumn = widget.metricColumn,
                            aggregation = widget.aggregation,
                            title = widget.title,
                        )
                    }
                    DataChartCard(chartSpec = spec)
                }
            }
        }

        if (savedDashboards.isNotEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Saved Cloud Dashboards", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    savedDashboards.forEach { db ->
                        Surface(
                            onClick = { onLoadDashboard(db) },
                            shape = RoundedCornerShape(10.dp),
                            color = SlateElevated,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(db.title, style = MaterialTheme.typography.titleSmall, color = CyberCyan)
                                    Text("Filter: ${db.dateFilter}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                StatusPill(label = "LOAD", color = ElectricBlue)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PredictiveAnalyticsScreen(
    activeTable: DataTable?,
    forecastReport: ForecastReport?,
    horizonPeriods: Int,
    selectedMetric: String?,
    onUpdateForecast: (metric: String?, horizon: Int) -> Unit,
    onUploadClick: () -> Unit,
    onLoadSampleClick: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text("Predictive Analytics", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                Text("Time-series OLS trend regression with 95% confidence intervals", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }

        if (activeTable == null) {
            item { EmptyDatasetPromptCard(onUploadClick = onUploadClick, onLoadSampleClick = onLoadSampleClick) }
            return@LazyColumn
        }

        val numerics = activeTable.columnProfiles.filter { it.type == ColumnType.NUMERIC }.map { it.name }
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Forecast Parameters", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                Text("Target Metric", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(numerics) { m ->
                        val active = (selectedMetric ?: forecastReport?.metricColumn) == m
                        FilterChip(
                            selected = active,
                            onClick = { onUpdateForecast(m, horizonPeriods) },
                            label = { Text(m) },
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("Horizon", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(3, 6, 12).forEach { h ->
                        FilterChip(
                            selected = horizonPeriods == h,
                            onClick = { onUpdateForecast(selectedMetric ?: forecastReport?.metricColumn, h) },
                            label = { Text("$h Periods") },
                        )
                    }
                }
            }
        }

        if (forecastReport == null) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "At least 3 numeric time-series periods are required to fit a forecast model.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AmberWarning,
                    )
                }
            }
        } else {
            item { ForecastChartCanvas(report = forecastReport) }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    KpiMetricCard(
                        title = "Next Period Est.",
                        value = AnalyticsComputeEngine.formatCompact(forecastReport.nextPeriodPrediction),
                        subtitle = forecastReport.metricColumn,
                        icon = Icons.AutoMirrored.Rounded.TrendingUp,
                        accentColor = CyberCyan,
                        modifier = Modifier.weight(1f),
                    )
                    KpiMetricCard(
                        title = "Model Fit (R²)",
                        value = String.format("%.2f", forecastReport.rSquared),
                        subtitle = "MAPE ${String.format("%.1f", forecastReport.mapePercent)}%",
                        icon = Icons.AutoMirrored.Rounded.TrendingUp,
                        accentColor = NeuralViolet,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Model Performance & Summary", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(Modifier.height(6.dp))
                    Text(forecastReport.summary, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "MAE: ${AnalyticsComputeEngine.formatCompact(forecastReport.mae)}   •   RMSE: ${AnalyticsComputeEngine.formatCompact(forecastReport.rmse)}   •   Slope: ${AnalyticsComputeEngine.formatCompact(forecastReport.trendSlope)}/period",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberCyan,
                    )
                }
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Forecast Limitations", style = MaterialTheme.typography.titleMedium, color = AmberWarning)
                    Spacer(Modifier.height(6.dp))
                    forecastReport.limitations.forEach { lim ->
                        Text("• $lim", style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun BusinessIntelligenceScreen(
    activeTable: DataTable?,
    biInsights: BiInsightsReport?,
    onAskRootCause: (String) -> Unit,
    onUploadClick: () -> Unit,
    onLoadSampleClick: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text("Business Intelligence", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                Text("Revenue, profitability, segmentation & automated root-cause analysis", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }

        if (activeTable == null || biInsights == null) {
            item { EmptyDatasetPromptCard(onUploadClick = onUploadClick, onLoadSampleClick = onLoadSampleClick) }
            return@LazyColumn
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                KpiMetricCard(
                    title = "Total Revenue",
                    value = AnalyticsComputeEngine.formatCompact(biInsights.totalRevenue),
                    subtitle = "${biInsights.totalTransactions} transactions",
                    icon = Icons.Rounded.AttachMoney,
                    accentColor = CyberCyan,
                    modifier = Modifier.weight(1f),
                )
                KpiMetricCard(
                    title = if (biInsights.totalProfit != null) "Total Profit" else "Avg Transaction",
                    value = AnalyticsComputeEngine.formatCompact(biInsights.totalProfit ?: biInsights.avgOrderValue),
                    subtitle = biInsights.periodChangePercent?.let {
                        val s = if (it >= 0) "+" else ""
                        "MoM $s${String.format("%.1f", it)}%"
                    } ?: "Computed",
                    icon = Icons.AutoMirrored.Rounded.TrendingUp,
                    accentColor = if ((biInsights.periodChangePercent ?: 0.0) >= 0) EmeraldSuccess else AmberWarning,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Automated Root-Cause Investigation", style = MaterialTheme.typography.titleMedium, color = CyberCyan)
                Spacer(Modifier.height(6.dp))
                Text(biInsights.rootCauseSummary, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Spacer(Modifier.height(10.dp))
                GlowActionButton(
                    text = "Deep-Dive with 7-Agent AI",
                    onClick = {
                        onAskRootCause("Investigate the root causes behind the latest period revenue and profit changes across all regions, products, and customer segments.")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "bi_deep_dive_button",
                )
            }
        }

        items(biInsights.breakdowns) { breakdown ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Performance by ${breakdown.dimensionName}", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(Modifier.height(10.dp))
                breakdown.items.take(6).forEach { item ->
                    Column(modifier = Modifier.padding(vertical = 5.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (item.periodDeltaPercent != null) {
                                    val c = if (item.periodDeltaPercent >= 0) EmeraldSuccess else CoralError
                                    val sign = if (item.periodDeltaPercent >= 0) "+" else ""
                                    Text(
                                        text = "$sign${String.format("%.1f", item.periodDeltaPercent)}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = c,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(
                                    text = "${AnalyticsComputeEngine.formatCompact(item.revenue)} (${String.format("%.1f", item.sharePercent)}%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyberCyan,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (item.sharePercent / 100.0).toFloat().coerceIn(0.02f, 1f) },
                            color = ElectricBlue,
                            trackColor = SubtleDivider,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                        )
                    }
                }
            }
        }
    }
}
