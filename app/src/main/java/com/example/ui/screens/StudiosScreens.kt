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
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SavedQueryRecord
import com.example.domain.engine.LocalSqlEngine
import com.example.domain.engine.PythonExecutionOutput
import com.example.domain.model.DataTable
import com.example.domain.model.SqlExecutionResult
import com.example.ui.components.DataChartCard
import com.example.ui.components.EmptyDatasetPromptCard
import com.example.ui.components.GlassCard
import com.example.ui.components.GlowActionButton
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeuralViolet
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateElevated
import com.example.ui.theme.SubtleDivider
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SqlStudioScreen(
    activeTable: DataTable?,
    sqlQuery: String,
    isReadOnly: Boolean,
    sqlResult: SqlExecutionResult?,
    savedQueries: List<SavedQueryRecord>,
    onSqlChange: (String) -> Unit,
    onToggleReadOnly: () -> Unit,
    onExecuteSql: (saveTitle: String?) -> Unit,
    onExportSqlCsv: (SqlExecutionResult) -> Unit,
    onUploadClick: () -> Unit,
    onLoadSampleClick: () -> Unit,
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveTitleInput by remember { mutableStateOf("") }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save SQL Query") },
            text = {
                OutlinedTextField(
                    value = saveTitleInput,
                    onValueChange = { saveTitleInput = it },
                    label = { Text("Query Title") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onExecuteSql(saveTitleInput.ifBlank { "Saved Query" })
                        saveTitleInput = ""
                        showSaveDialog = false
                    }
                ) { Text("Save & Run") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            },
        )
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
                    Text("SQL Studio", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                    Text("Table name: dataset • In-memory SQLite engine", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Surface(
                    onClick = onToggleReadOnly,
                    shape = RoundedCornerShape(12.dp),
                    color = if (isReadOnly) EmeraldSuccess.copy(alpha = 0.16f) else AmberWarning.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, if (isReadOnly) EmeraldSuccess else AmberWarning),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (isReadOnly) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                            contentDescription = "Toggle Read Only",
                            tint = if (isReadOnly) EmeraldSuccess else AmberWarning,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isReadOnly) "READ-ONLY" else "SANDBOX RW",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isReadOnly) EmeraldSuccess else AmberWarning,
                        )
                    }
                }
            }
        }

        if (activeTable == null) {
            item { EmptyDatasetPromptCard(onUploadClick = onUploadClick, onLoadSampleClick = onLoadSampleClick) }
            return@LazyColumn
        }

        item {
            Text("Schema Columns (tap to insert)", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(activeTable.headers) { h ->
                    val san = LocalSqlEngine.sanitizeIdentifier(h)
                    Surface(
                        onClick = { onSqlChange("$sqlQuery $san") },
                        shape = RoundedCornerShape(8.dp),
                        color = SlateElevated,
                        border = BorderStroke(1.dp, SubtleDivider),
                    ) {
                        Text(
                            text = san,
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = sqlQuery,
                    onValueChange = onSqlChange,
                    textStyle = TextStyle(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = CyberCyan,
                    ),
                    minLines = 5,
                    maxLines = 10,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ObsidianBg,
                        unfocusedContainerColor = ObsidianBg,
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = SubtleDivider,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sql_editor_input"),
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GlowActionButton(
                        text = "Run SQL",
                        icon = Icons.Rounded.PlayArrow,
                        onClick = { onExecuteSql(null) },
                        modifier = Modifier.weight(1f),
                        testTag = "run_sql_button",
                    )
                    OutlinedButton(
                        onClick = { showSaveDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ElectricBlue),
                        modifier = Modifier.height(48.dp),
                    ) {
                        Icon(Icons.Rounded.BookmarkAdd, contentDescription = "Save query", tint = ElectricBlue)
                    }
                    if (sqlResult != null && sqlResult.errorMessage == null && sqlResult.rows.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { onExportSqlCsv(sqlResult) },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, CyberCyan),
                            modifier = Modifier.height(48.dp),
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = "Export CSV", tint = CyberCyan)
                        }
                    }
                }
            }
        }

        if (sqlResult != null) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Query Results", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        StatusPill(
                            label = if (sqlResult.errorMessage != null) "ERROR" else "${sqlResult.rowCount} rows • ${sqlResult.executionMs}ms",
                            color = if (sqlResult.errorMessage != null) CoralError else EmeraldSuccess,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    if (sqlResult.errorMessage != null) {
                        Text(sqlResult.errorMessage, style = MaterialTheme.typography.bodySmall, color = CoralError)
                    } else if (sqlResult.rows.isEmpty()) {
                        Text("Query executed cleanly and returned 0 rows.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    } else {
                        val hScroll = rememberScrollState()
                        Column(modifier = Modifier.horizontalScroll(hScroll)) {
                            Row(
                                modifier = Modifier
                                    .background(ElectricBlue.copy(alpha = 0.2f))
                                    .padding(vertical = 6.dp, horizontal = 8.dp),
                            ) {
                                sqlResult.headers.forEach { col ->
                                    Text(
                                        text = col,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyberCyan,
                                        maxLines = 1,
                                        modifier = Modifier.width(120.dp),
                                    )
                                }
                            }
                            sqlResult.rows.take(50).forEach { row ->
                                Row(modifier = Modifier.padding(vertical = 5.dp, horizontal = 8.dp)) {
                                    row.forEach { cell ->
                                        Text(
                                            text = cell.ifBlank { "NULL" },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.width(120.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (savedQueries.isNotEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Saved SQL Queries", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    savedQueries.take(8).forEach { sq ->
                        Surface(
                            onClick = { onSqlChange(sq.sqlText) },
                            shape = RoundedCornerShape(10.dp),
                            color = SlateElevated,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(sq.title, style = MaterialTheme.typography.titleSmall, color = CyberCyan)
                                    Text("${sq.rowCount} rows • ${sq.executionMs}ms", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                }
                                Text(sq.sqlText, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PythonStudioScreen(
    activeTable: DataTable?,
    pythonScript: String,
    pythonOutput: PythonExecutionOutput?,
    onScriptChange: (String) -> Unit,
    onRunPython: () -> Unit,
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
                Text("Python Analytics Studio", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                Text("Isolated DataFrame compute sandbox (`df` bound to active dataset)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }

        if (activeTable == null) {
            item { EmptyDatasetPromptCard(onUploadClick = onUploadClick, onLoadSampleClick = onLoadSampleClick) }
            return@LazyColumn
        }

        item {
            val catCol = activeTable.columnProfiles.firstOrNull { it.type == com.example.domain.model.ColumnType.CATEGORICAL }?.name ?: activeTable.headers.first()
            val numCol = activeTable.columnProfiles.firstOrNull { it.type == com.example.domain.model.ColumnType.NUMERIC }?.name ?: activeTable.headers.last()
            val snippets = listOf(
                "Summary" to "df.info()\ndf.describe()",
                "Missing" to "df.isnull().sum()",
                "Correlations" to "df.corr()",
                "GroupBy" to "df.groupby('$catCol')['$numCol'].sum()",
                "Value Counts" to "df['$catCol'].value_counts()",
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(snippets) { (label, code) ->
                    Surface(
                        onClick = { onScriptChange(code) },
                        shape = RoundedCornerShape(10.dp),
                        color = SlateElevated,
                        border = BorderStroke(1.dp, NeuralViolet.copy(alpha = 0.5f)),
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = NeuralViolet,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        )
                    }
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = pythonScript,
                    onValueChange = onScriptChange,
                    textStyle = TextStyle(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = EmeraldSuccess,
                    ),
                    minLines = 5,
                    maxLines = 10,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ObsidianBg,
                        unfocusedContainerColor = ObsidianBg,
                        focusedBorderColor = NeuralViolet,
                        unfocusedBorderColor = SubtleDivider,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("python_editor_input"),
                )
                Spacer(Modifier.height(10.dp))
                GlowActionButton(
                    text = "Execute Isolated Script",
                    icon = Icons.Rounded.PlayArrow,
                    onClick = onRunPython,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "run_python_button",
                )
            }
        }

        if (pythonOutput != null) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Console Output", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        StatusPill(
                            label = if (pythonOutput.error != null) "TRACEBACK" else "SANDBOX OK",
                            color = if (pythonOutput.error != null) CoralError else EmeraldSuccess,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    if (pythonOutput.stdout.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ObsidianBg,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = pythonOutput.stdout,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = TextPrimary,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }
                    if (pythonOutput.error != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = pythonOutput.error,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = CoralError,
                        )
                    }
                }
            }
            if (pythonOutput.chartSpec != null) {
                item {
                    DataChartCard(chartSpec = pythonOutput.chartSpec)
                }
            }
        }
    }
}
