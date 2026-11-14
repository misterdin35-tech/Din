package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.firebase.attemptAutoSignIn
import com.example.firebase.onGoogleSignInClicked
import com.example.ui.components.DataPilotLogo
import com.example.ui.components.GlassCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.NeuralViolet
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private data class LandingFeature(val title: String, val desc: String, val icon: ImageVector, val color: Color)
private data class AgentCardInfo(val num: String, val name: String, val role: String)
private data class PlanSpec(val name: String, val price: String, val badge: String, val bullets: List<String>)

@Composable
fun LandingAuthScreen(
    onAuthSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var isSigningIn by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var selectedSection by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope,
        )
    }

    val features = remember {
        listOf(
            LandingFeature("7-Agent AI Team", "Autonomous Orchestrator, Data Quality, SQL, Python, BI, Viz & QC verification.", Icons.Rounded.Psychology, CyberCyan),
            LandingFeature("Zero-Fabrication SQL & Python", "Every finding executes real SQLite & DataFrame calculations before synthesis.", Icons.Rounded.Code, ElectricBlue),
            LandingFeature("Predictive Forecasting", "OLS trend models with MAE, RMSE, MAPE, R² & 95% confidence bands.", Icons.AutoMirrored.Rounded.TrendingUp, NeuralViolet),
            LandingFeature("Automated Cleaning & BI", "CSV & XLSX ingestion, IQR outlier detection, root-cause drivers & PDF export.", Icons.Rounded.Insights, EmeraldSuccess),
        )
    }

    val agents = remember {
        listOf(
            AgentCardInfo("01", "AI Orchestrator", "Parses business objective, inspects schema & plans workflow"),
            AgentCardInfo("02", "Data Quality Agent", "Profiles types, nulls, duplicates & IQR anomalies"),
            AgentCardInfo("03", "SQL Analyst", "Generates & runs read-only SQL with auto-retry"),
            AgentCardInfo("04", "Python Analyst", "Computes descriptive stats & Pearson correlations"),
            AgentCardInfo("05", "Business Analyst", "Evaluates MoM changes, segments & root causes"),
            AgentCardInfo("06", "Visualization Agent", "Builds Bar, Line, Donut & Scatter charts from verified rows"),
            AgentCardInfo("07", "Quality Control", "Cross-checks calculations & flags dataset limitations"),
        )
    }

    val plans = remember {
        listOf(
            PlanSpec("FREE", "$0 / mo", "Starter", listOf("Up to 3 cloud datasets", "Basic 7-Agent AI analysis", "Interactive dashboards", "Monthly PDF & CSV exports")),
            PlanSpec("PRO", "$29 / mo", "Popular", listOf("Unlimited datasets", "Advanced BI & Predictive forecasting", "Voice AI Analyst & SQL Studio", "Priority Gemini 3.8 Flash throughput")),
            PlanSpec("BUSINESS", "$79 / mo", "Teams", listOf("Team workspaces", "PostgreSQL, MySQL & BigQuery connectors", "Scheduled anomaly monitoring", "Immutable audit trail")),
            PlanSpec("ENTERPRISE", "Custom", "Scale", listOf("Custom integrations", "Organization governance", "Advanced retention & security", "Dedicated usage quotas")),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(ObsidianBg, MidnightSurface, ObsidianBg))
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DataPilotLogo(size = 40.dp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("DataPilot AI", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                            Text("Autonomous Analytics Platform", style = MaterialTheme.typography.labelMedium, color = CyberCyan)
                        }
                    }
                    StatusPill(label = "v2.6 CLOUD", color = ElectricBlue)
                }
            }

            item {
                GlassCard(contentPadding = PaddingValues(0.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_datapilot_1794584363074),
                            contentDescription = "DataPilot AI Neural Analytics Platform Hero",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, ObsidianBg.copy(alpha = 0.92f))
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            StatusPill(label = "AUTONOMOUS AI DATA ANALYST", color = CyberCyan)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Think. Analyze. Discover. Decide.",
                                style = MaterialTheme.typography.headlineSmall,
                                color = TextPrimary,
                            )
                        }
                    }
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Upload CSV or Excel datasets, run read-only SQL and Python statistics, orchestrate 7 specialized AI agents, forecast revenue with 95% confidence bands, and export executive PDF reports.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = {
                                authError = null
                                isSigningIn = true
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isSigningIn = false
                                        onAuthSuccess()
                                    },
                                    onAuthError = { msg ->
                                        isSigningIn = false
                                        authError = msg
                                    },
                                    scope = scope,
                                    onAuthCancelled = { isSigningIn = false },
                                )
                            },
                            enabled = !isSigningIn,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricBlue,
                                contentColor = Color.White,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("google_sign_in_button"),
                        ) {
                            if (isSigningIn) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                )
                                Spacer(Modifier.width(10.dp))
                                Text("Connecting Workspace...")
                            } else {
                                Icon(Icons.Rounded.Login, contentDescription = null)
                                Spacer(Modifier.width(10.dp))
                                Text("Sign in with Google to Launch Workspace", style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        if (authError != null) {
                            Spacer(Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = CoralError.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, CoralError.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = authError.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CoralError,
                                    modifier = Modifier.padding(10.dp),
                                )
                            }
                        }
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val tabs = listOf("Features", "How It Works", "7-Agent Team", "Pricing Plans")
                    items(tabs.size) { idx ->
                        FilterChip(
                            selected = selectedSection == idx,
                            onClick = { selectedSection = idx },
                            label = { Text(tabs[idx]) },
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

            when (selectedSection) {
                0 -> {
                    items(features) { feat ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(feat.color.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(feat.icon, contentDescription = feat.title, tint = feat.color)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(feat.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Text(feat.desc, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                            }
                        }
                    }
                }

                1 -> {
                    item {
                        val steps = listOf(
                            "1. Ingest & Profile" to "Upload CSV or XLSX spreadsheets. DataPilot automatically detects types, missing cells, duplicates, and IQR outliers.",
                            "2. Clean & Verify" to "Review before-and-after quality scores with 1-tap non-destructive cleaning while preserving raw data.",
                            "3. Ask in Natural Language or Voice" to "The 7-Agent pipeline plans SQL & Python computations, executes them locally, and verifies every metric via Quality Control.",
                            "4. Build Dashboards & Export Reports" to "Arrange interactive KPI/chart widgets, forecast future trends, and download formatted PDF or CSV executive reports.",
                        )
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text("How DataPilot AI Works", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                            Spacer(Modifier.height(12.dp))
                            steps.forEach { (title, body) ->
                                Row(modifier = Modifier.padding(vertical = 6.dp)) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                        Text(body, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    items(agents) { ag ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = NeuralViolet.copy(alpha = 0.2f),
                                ) {
                                    Text(
                                        text = ag.num,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyberCyan,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(ag.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                    Text(ag.role, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                            }
                        }
                    }
                }

                else -> {
                    items(plans) { plan ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(plan.name, style = MaterialTheme.typography.titleMedium, color = CyberCyan)
                                    Text(plan.price, style = MaterialTheme.typography.headlineSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                                }
                                StatusPill(label = plan.badge, color = NeuralViolet)
                            }
                            Spacer(Modifier.height(8.dp))
                            plan.bullets.forEach { b ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                    Icon(Icons.Rounded.FactCheck, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(b, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                            }
                        }
                    }
                    item {
                        Text(
                            text = "Note: Plans are illustrative workspace tiers. No payment provider is charged.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Security, contentDescription = null, tint = EmeraldSuccess)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Zero-Trust Cloud Security: User-isolated Firestore rules, Firebase App Check attestation, read-only SQL by default, and full data deletion.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }
    }
}
