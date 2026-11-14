package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.NeuralViolet
import com.example.ui.theme.SlateCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DataPilotLogo(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = minOf(w, h) * 0.44f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(CyberCyan.copy(alpha = 0.25f), Color.Transparent),
                center = Offset(cx, cy),
                radius = r * 1.15f,
            ),
            radius = r * 1.15f,
            center = Offset(cx, cy),
        )

        drawCircle(
            brush = Brush.sweepGradient(listOf(CyberCyan, ElectricBlue, NeuralViolet, CyberCyan)),
            radius = r,
            center = Offset(cx, cy),
            style = Stroke(width = w * 0.06f),
        )

        val diamond = Path().apply {
            moveTo(cx, cy - r * 0.72f)
            lineTo(cx + r * 0.56f, cy)
            lineTo(cx, cy + r * 0.72f)
            lineTo(cx - r * 0.56f, cy)
            close()
        }
        drawPath(
            path = diamond,
            brush = Brush.linearGradient(listOf(CyberCyan, ElectricBlue, NeuralViolet)),
            style = Stroke(width = w * 0.055f, cap = StrokeCap.Round),
        )

        drawCircle(
            color = NeuralViolet,
            radius = r * 0.2f,
            center = Offset(cx, cy),
        )
        drawCircle(
            color = CyberCyan,
            radius = r * 0.09f,
            center = Offset(cx, cy),
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    borderBrush: Brush = Brush.linearGradient(
        listOf(
            CyberCyan.copy(alpha = 0.42f),
            ElectricBlue.copy(alpha = 0.22f),
            NeuralViolet.copy(alpha = 0.35f),
        )
    ),
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val cardColors = androidx.compose.material3.CardDefaults.cardColors(
        containerColor = Color.Transparent,
        contentColor = TextPrimary,
    )
    val cardElevation = androidx.compose.material3.CardDefaults.cardElevation(
        defaultElevation = 4.dp,
        pressedElevation = 2.dp,
    )
    val borderStroke = BorderStroke(width = 1.dp, brush = borderBrush)

    val cardContent: @Composable ColumnScope.() -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(SlateCard.copy(alpha = 0.92f), MidnightSurface.copy(alpha = 0.96f))
                    )
                )
                .padding(contentPadding),
            content = content,
        )
    }

    if (onClick != null) {
        androidx.compose.material3.Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = cardColors,
            elevation = cardElevation,
            border = borderStroke,
            content = cardContent,
        )
    } else {
        androidx.compose.material3.Card(
            modifier = modifier,
            shape = shape,
            colors = cardColors,
            elevation = cardElevation,
            border = borderStroke,
            content = cardContent,
        )
    }
}

@Composable
fun KpiMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color = CyberCyan,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    GlassCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = accentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun StatusPill(
    label: String,
    color: Color = CyberCyan,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun GlowActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    testTag: String = "",
) {
    val shape = RoundedCornerShape(14.dp)
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = ElectricBlue,
            contentColor = Color.White,
            disabledContainerColor = SlateCard,
            disabledContentColor = TextSecondary,
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        modifier = modifier
            .height(48.dp)
            .let { if (testTag.isNotEmpty()) it.testTag(testTag) else it },
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun EmptyDatasetPromptCard(
    onUploadClick: () -> Unit,
    onLoadSampleClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DataPilotLogo(size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "No Active Dataset Loaded",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                Text(
                    text = "Upload a CSV or Excel (.xlsx) file to unlock SQL Studio, Python stats, forecasting, and the 7-Agent AI Analyst, or test with the clearly-labeled Sample Retail Workspace.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            GlowActionButton(
                text = "Upload CSV / XLSX",
                icon = Icons.Rounded.UploadFile,
                onClick = onUploadClick,
                modifier = Modifier.weight(1f),
                testTag = "empty_upload_dataset_button",
            )
            OutlinedButton(
                onClick = onLoadSampleClick,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f)),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("empty_load_sample_button"),
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Sample Demo", color = CyberCyan, maxLines = 1)
            }
        }
    }
}

fun statusColorFor(label: String): Color = when (label.uppercase()) {
    "VERIFIED", "CONNECTED", "CLEANED" -> EmeraldSuccess
    "RUNNING", "CONFIGURED" -> CyberCyan
    "RETRIED", "PENDING", "PENDING_CREDENTIALS" -> AmberWarning
    "FAILED", "ERROR" -> CoralError
    else -> ElectricBlue
}
