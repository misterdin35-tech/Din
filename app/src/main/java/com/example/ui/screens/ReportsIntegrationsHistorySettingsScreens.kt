package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddLink
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AnalysisRecord
import com.example.data.model.AuditLogRecord
import com.example.data.model.AutomationSchedule
import com.example.data.model.IntegrationRecord
import com.example.data.model.ReportRecord
import com.example.data.model.SavedQueryRecord
import com.example.data.model.UserProfile
import com.example.domain.model.BiInsightsReport
import com.example.domain.model.DataTable
import com.example.domain.model.ForecastReport
import com.example.ui.components.EmptyDatasetPromptCard
import com.example.ui.components.GlassCard
import com.example.ui.components.GlowActionButton
import com.example.ui.components.StatusPill
import com.example.ui.components.statusColorFor
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NeuralViolet
import com.example.ui.theme.SlateElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ReportsCenterScreen(
    activeTable: DataTable?,
    biInsights: BiInsightsReport?,
    forecastReport: ForecastReport?,
    reports: List<ReportRecord>,
    onExportPdf: (title: String, summary: String) -> Unit,
    onExportCsv: (DataTable) -> Unit,
    onSaveExecutiveReport: (title: String, summary: String) -> Unit,
    onUploadClick: () -> Unit,
    onLoadSampleClick: () -> Unit,
) {
    val context = LocalContext.current
    var reportTitle by remember(activeTable?.name) {
        mutableStateOf("${activeTable?.name ?: "Workspace"} Executive Report")
    }

    val generatedSummary = remember(activeTable, biInsights, forecastReport) {
        buildString {
            if (activeTable != null) {
                appendLine("Dataset: ${activeTable.name} (${activeTable.rows.size} rows, Quality ${activeTable.qualityReport.qualityScore}/100)")
            }
            if (biInsights != null) {
                appendLine("BI Insights: ${biInsights.rootCauseSummary}")
            }
            if (forecastReport != null) {
                appendLine("Forecast: ${forecastReport.summary}")
            }
        }.trim()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text("Reports Center", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                Text("Export formatted PDF reports, Excel-compatible CSVs & executive summaries", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }

        if (activeTable == null) {
            item { EmptyDatasetPromptCard(onUploadClick = onUploadClick, onLoadSampleClick = onLoadSampleClick) }
        } else {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Generate New Report", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reportTitle,
                        onValueChange = { reportTitle = it },
                        label = { Text("Report Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(generatedSummary, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        GlowActionButton(
                            text = "Export PDF",
                            icon = Icons.Rounded.PictureAsPdf,
                            onClick = { onExportPdf(reportTitle.ifBlank { "Executive Report" }, generatedSummary) },
                            modifier = Modifier.weight(1f),
                            testTag = "export_pdf_report_button",
                        )
                        OutlinedButton(
                            onClick = { onExportCsv(activeTable) },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, CyberCyan),
                            modifier = Modifier.weight(1f).height(48.dp),
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Excel CSV", color = CyberCyan)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { onSaveExecutiveReport(reportTitle.ifBlank { "Executive Brief" }, generatedSummary) },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, NeuralViolet),
                            modifier = Modifier.weight(1f).height(46.dp),
                        ) {
                            Text("Save Summary", color = NeuralViolet)
                        }
                        OutlinedButton(
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, reportTitle)
                                    putExtra(Intent.EXTRA_TEXT, generatedSummary)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share DataPilot AI Report"))
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, ElectricBlue),
                            modifier = Modifier.weight(1f).height(46.dp),
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Share", color = ElectricBlue)
                        }
                    }
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Saved Reports Archive (${reports.size})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                if (reports.isEmpty()) {
                    Text("No saved reports yet.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                } else {
                    reports.forEach { rp ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                        ) {
                            Icon(Icons.Rounded.Description, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rp.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text(rp.summary, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            }
                            StatusPill(label = rp.format.uppercase(), color = ElectricBlue)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IntegrationsScreen(
    integrations: List<IntegrationRecord>,
    onSaveIntegration: (provider: String, label: String, hostOrUri: String, status: String) -> Unit,
    onDeleteIntegration: (String) -> Unit,
    onUploadCsvXlsxClick: () -> Unit,
) {
    val providers = remember {
        listOf(
            "postgresql" to "PostgreSQL",
            "mysql" to "MySQL",
            "google_sheets" to "Google Sheets",
            "bigquery" to "Google BigQuery",
            "snowflake" to "Snowflake",
            "csv_excel" to "CSV / Excel Local",
        )
    }

    var selectedProvider by remember { mutableStateOf("postgresql") }
    var labelInput by remember { mutableStateOf("") }
    var hostInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text("Data Integrations", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                Text("Local CSV/Excel ingestion + cloud connector registry", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("CSV & Excel (.xlsx) Direct Upload", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("Fully active on-device parser via Android Document Picker.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    StatusPill(label = "CONNECTED", color = EmeraldSuccess)
                }
                Spacer(Modifier.height(10.dp))
                GlowActionButton(
                    text = "Import CSV / XLSX File",
                    onClick = onUploadCsvXlsxClick,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "integrations_import_button",
                )
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Configure External Warehouse Connector", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(
                    "Note: Direct JDBC warehouse drivers require a server-side relay endpoint. Save your connector metadata here; unprovisioned external endpoints stay marked Pending Credentials.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(providers) { (key, title) ->
                        FilterChip(
                            selected = selectedProvider == key,
                            onClick = { selectedProvider = key },
                            label = { Text(title) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = labelInput,
                    onValueChange = { labelInput = it },
                    label = { Text("Connection Label (e.g. Prod Analytics DB)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = hostInput,
                    onValueChange = { hostInput = it },
                    label = { Text("Host / Sheet URL / Project ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                GlowActionButton(
                    text = "Save Connector",
                    icon = Icons.Rounded.AddLink,
                    onClick = {
                        if (labelInput.isNotBlank() && hostInput.isNotBlank()) {
                            val status = if (selectedProvider == "csv_excel") "connected" else "pending_credentials"
                            onSaveIntegration(selectedProvider, labelInput.trim(), hostInput.trim(), status)
                            labelInput = ""
                            hostInput = ""
                        }
                    },
                    enabled = labelInput.isNotBlank() && hostInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Configured Connectors (${integrations.size})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                if (integrations.isEmpty()) {
                    Text("No external warehouse connectors configured yet.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                } else {
                    integrations.forEach { conn ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${conn.label} (${conn.provider})", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text(conn.hostOrUri, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            StatusPill(label = conn.status.uppercase(), color = statusColorFor(conn.status))
                            IconButton(onClick = { onDeleteIntegration(conn.id) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete connector", tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryAutomationScreen(
    analyses: List<AnalysisRecord>,
    savedQueries: List<SavedQueryRecord>,
    schedules: List<AutomationSchedule>,
    auditLogs: List<AuditLogRecord>,
    onCreateSchedule: (title: String, freq: String, anomaly: Boolean, emailConsent: Boolean) -> Unit,
    onRunScheduleNow: (AutomationSchedule) -> Unit,
    onLoadSqlQuery: (String) -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var schedTitle by remember { mutableStateOf("") }
    var schedFreq by remember { mutableStateOf("daily") }
    var anomalyAlerts by remember { mutableStateOf(true) }
    var emailConsent by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text("History & Automation", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                Text("Saved analyses, SQL history, scheduled monitoring & audit trail", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val tabs = listOf("AI Analyses (${analyses.size})", "SQL Queries (${savedQueries.size})", "Automation (${schedules.size})", "Audit Logs (${auditLogs.size})")
                items(tabs.size) { idx ->
                    FilterChip(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        label = { Text(tabs[idx]) },
                    )
                }
            }
        }

        when (selectedTab) {
            0 -> {
                if (analyses.isEmpty()) {
                    item { GlassCard(Modifier.fillMaxWidth()) { Text("No AI analyses recorded yet.", color = TextSecondary) } }
                } else {
                    items(analyses) { an ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(an.question, style = MaterialTheme.typography.titleSmall, color = CyberCyan)
                            Text(an.datasetName, style = MaterialTheme.typography.labelSmall, color = NeuralViolet)
                            Spacer(Modifier.height(4.dp))
                            Text(an.executiveSummary, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        }
                    }
                }
            }

            1 -> {
                if (savedQueries.isEmpty()) {
                    item { GlassCard(Modifier.fillMaxWidth()) { Text("No saved SQL queries yet.", color = TextSecondary) } }
                } else {
                    items(savedQueries) { sq ->
                        GlassCard(modifier = Modifier.fillMaxWidth(), onClick = { onLoadSqlQuery(sq.sqlText) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(sq.title, style = MaterialTheme.typography.titleSmall, color = CyberCyan)
                                Text("${sq.rowCount} rows • ${sq.executionMs}ms", style = MaterialTheme.typography.labelSmall, color = EmeraldSuccess)
                            }
                            Text(sq.sqlText, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }
                }
            }

            2 -> {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Schedule Monitoring & Anomaly Alerts", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = schedTitle,
                            onValueChange = { schedTitle = it },
                            label = { Text("Rule Title (e.g. Daily Revenue Anomaly Check)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("daily", "weekly").forEach { f ->
                                FilterChip(
                                    selected = schedFreq == f,
                                    onClick = { schedFreq = f },
                                    label = { Text(f.uppercase()) },
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = anomalyAlerts, onCheckedChange = { anomalyAlerts = it })
                            Text("Detect data changes & IQR anomalies", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = emailConsent, onCheckedChange = { emailConsent = it })
                            Text("Enable email digest permission", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        }
                        Spacer(Modifier.height(8.dp))
                        GlowActionButton(
                            text = "Activate Schedule",
                            icon = Icons.Rounded.Schedule,
                            onClick = {
                                onCreateSchedule(schedTitle.ifBlank { "Scheduled Monitor" }, schedFreq, anomalyAlerts, emailConsent)
                                schedTitle = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                items(schedules) { sc ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(sc.title, style = MaterialTheme.typography.titleSmall, color = CyberCyan)
                                Text("${sc.frequency.uppercase()} • Anomaly alerts: ${sc.anomalyAlerts}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            IconButton(onClick = { onRunScheduleNow(sc) }) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = "Run check now", tint = EmeraldSuccess)
                            }
                        }
                        if (sc.lastRunSummary.isNotBlank()) {
                            Text(sc.lastRunSummary, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        }
                    }
                }
            }

            else -> {
                if (auditLogs.isEmpty()) {
                    item { GlassCard(Modifier.fillMaxWidth()) { Text("No audit events yet.", color = TextSecondary) } }
                } else {
                    items(auditLogs) { log ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                StatusPill(label = log.action, color = ElectricBlue)
                                Text(log.resourceType, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(log.detail, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsPricingScreen(
    userProfile: UserProfile?,
    userEmail: String,
    onSaveSettings: (displayName: String, orgName: String, planTier: String, notifications: Boolean, retentionDays: Int) -> Unit,
    onDeleteAllWorkspaceData: () -> Unit,
    onSignOut: () -> Unit,
) {
    var displayName by remember(userProfile?.displayName) { mutableStateOf(userProfile?.displayName ?: "Analyst") }
    var orgName by remember(userProfile?.organizationName) { mutableStateOf(userProfile?.organizationName ?: "DataPilot Workspace") }
    var planTier by remember(userProfile?.planTier) { mutableStateOf(userProfile?.planTier ?: "free") }
    var notifications by remember(userProfile?.notificationsEnabled) { mutableStateOf(userProfile?.notificationsEnabled ?: true) }
    var retentionDays by remember(userProfile?.retentionDays) { mutableIntStateOf(userProfile?.retentionDays ?: 90) }
    var showPurgeConfirm by remember { mutableStateOf(false) }

    if (showPurgeConfirm) {
        AlertDialog(
            onDismissRequest = { showPurgeConfirm = false },
            title = { Text("Delete All Workspace Data?") },
            text = { Text("This permanently deletes all uploaded datasets, saved SQL queries, dashboards, reports, and analyses from your Firestore workspace.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteAllWorkspaceData()
                        showPurgeConfirm = false
                    }
                ) { Text("Delete Permanently", color = CoralError) }
            },
            dismissButton = { TextButton(onClick = { showPurgeConfirm = false }) { Text("Cancel") } },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text("Settings & Plan", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                Text("Account, organization, data retention, security & illustrative plans", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Profile & Organization", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text("Signed in as: ${userEmail.ifBlank { userProfile?.email ?: "Google User" }}", style = MaterialTheme.typography.bodySmall, color = CyberCyan)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = orgName,
                    onValueChange = { orgName = it },
                    label = { Text("Organization / Workspace") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Anomaly & Report Notifications", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Switch(checked = notifications, onCheckedChange = { notifications = it })
                }
                Spacer(Modifier.height(6.dp))
                Text("Data Retention Policy: $retentionDays days", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Slider(
                    value = retentionDays.toFloat(),
                    onValueChange = { retentionDays = it.toInt() },
                    valueRange = 30f..365f,
                )
                Spacer(Modifier.height(8.dp))
                Text("Illustrative Subscription Tier (No Billing)", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("free", "pro", "business", "enterprise")) { tier ->
                        FilterChip(
                            selected = planTier == tier,
                            onClick = { planTier = tier },
                            label = { Text(tier.uppercase()) },
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                GlowActionButton(
                    text = "Save Workspace Settings",
                    onClick = { onSaveSettings(displayName, orgName, planTier, notifications, retentionDays) },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "save_settings_button",
                )
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Security, Privacy & Model Governance", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(Modifier.height(6.dp))
                Text(
                    "• Customer datasets are isolated per authenticated user ID in Firestore.\n" +
                        "• Datasets are never used for model training.\n" +
                        "• SQL Studio runs read-only SELECT queries by default.\n" +
                        "• Firebase App Check protects Gemini AI calls.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showPurgeConfirm = true },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, CoralError),
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = CoralError, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Purge Data", color = CoralError)
                    }
                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, AmberWarning),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("sign_out_button"),
                    ) {
                        Icon(Icons.Rounded.Logout, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sign Out", color = AmberWarning)
                    }
                }
            }
        }
    }
}
