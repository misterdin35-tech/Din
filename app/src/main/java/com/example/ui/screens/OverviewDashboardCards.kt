package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AnalysisRecord
import com.example.data.model.ReportRecord
import com.example.domain.model.DataTable
import com.example.ui.components.GlassCard
import com.example.ui.components.GlowActionButton
import com.example.ui.components.StatusPill
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.NeuralViolet
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateElevated
import com.example.ui.theme.SubtleDivider
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
internal fun OverviewHeaderCard(
    activeTable: DataTable?,
    datasetCount: Int,
    isVeryNarrow: Boolean,
    onUploadClick: () -> Unit,
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = SlateCard,
            contentColor = TextPrimary,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("overview_header_card"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            ElectricBlue.copy(alpha = 0.18f),
                            MidnightSurface,
                            NeuralViolet.copy(alpha = 0.16f),
                        )
                    )
                )
                .padding(16.dp),
        ) {
            if (isVeryNarrow) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HeaderTitleBlock(activeTable = activeTable, datasetCount = datasetCount)
                    GlowActionButton(
                        text = "Quick Upload",
                        icon = Icons.Rounded.UploadFile,
                        onClick = onUploadClick,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "overview_quick_upload_button",
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HeaderTitleBlock(
                        activeTable = activeTable,
                        datasetCount = datasetCount,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                    )
                    GlowActionButton(
                        text = "Quick Upload",
                        icon = Icons.Rounded.UploadFile,
                        onClick = onUploadClick,
                        testTag = "overview_quick_upload_button",
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderTitleBlock(
    activeTable: DataTable?,
    datasetCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusPill(
                label = if (activeTable != null) "ACTIVE WORKSPACE" else "READY",
                color = if (activeTable != null) EmeraldSuccess else CyberCyan,
            )
            if (datasetCount > 0) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "$datasetCount synced",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Executive Overview",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = if (activeTable != null) {
                "${activeTable.name} • ${activeTable.rows.size} rows × ${activeTable.headers.size} cols"
            } else {
                "Upload CSV or XLSX files to launch autonomous analytics"
            },
            style = MaterialTheme.typography.bodySmall,
            color = CyberCyan,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun RecentAnalysesFeedCard(
    analyses: List<AnalysisRecord>,
    onNavigateToAnalyst: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        modifier = modifier.testTag("recent_analyses_card"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeuralViolet.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Insights, contentDescription = null, tint = NeuralViolet, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Recent Analyses", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text("${analyses.size} recorded runs", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
            TextButton(onClick = onNavigateToAnalyst) {
                Text("Ask AI", color = CyberCyan)
            }
        }
        Spacer(Modifier.height(10.dp))
        if (analyses.isEmpty()) {
            Surface(
                onClick = onNavigateToAnalyst,
                shape = RoundedCornerShape(12.dp),
                color = SlateElevated,
                border = BorderStroke(1.dp, SubtleDivider),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "No analyses yet. Tap to ask the 7-Agent AI Analyst a question.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }
        } else {
            analyses.take(4).forEachIndexed { idx, an ->
                if (idx > 0) HorizontalDivider(color = SubtleDivider, modifier = Modifier.padding(vertical = 6.dp))
                Column(modifier = Modifier.padding(vertical = 2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = an.question,
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = an.datasetName,
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan,
                            maxLines = 1,
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = an.executiveSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
internal fun RecentReportsFeedCard(
    reports: List<ReportRecord>,
    onNavigateToReports: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        modifier = modifier.testTag("recent_reports_card"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberCyan.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Description, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Recent Reports", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text("${reports.size} exported briefs", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
            TextButton(onClick = onNavigateToReports) {
                Text("Reports", color = CyberCyan)
            }
        }
        Spacer(Modifier.height(10.dp))
        if (reports.isEmpty()) {
            Text(
                text = "No reports exported yet. Generate executive PDFs or CSVs from the Reports Center.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
        } else {
            reports.take(4).forEachIndexed { idx, rp ->
                if (idx > 0) HorizontalDivider(color = SubtleDivider, modifier = Modifier.padding(vertical = 6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = rp.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = rp.datasetName,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    StatusPill(label = rp.format.uppercase(), color = ElectricBlue)
                }
            }
        }
    }
}
