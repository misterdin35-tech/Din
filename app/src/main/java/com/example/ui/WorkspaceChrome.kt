package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SpaceDashboard
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AutomationSchedule
import com.example.data.model.DatasetRecord
import com.example.domain.model.DataTable
import com.example.ui.components.DataPilotLogo
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralError
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateElevated
import com.example.ui.theme.SubtleDivider
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

const val ROUTE_OVERVIEW = "overview"
const val ROUTE_EXPLORER = "explorer"
const val ROUTE_ANALYST = "analyst"
const val ROUTE_SQL_STUDIO = "sql_studio"
const val ROUTE_PYTHON_STUDIO = "python_studio"
const val ROUTE_DASHBOARD_BUILDER = "dashboard_builder"
const val ROUTE_PREDICTIVE = "predictive"
const val ROUTE_BUSINESS_INTEL = "business_intel"
const val ROUTE_REPORTS = "reports"
const val ROUTE_INTEGRATIONS = "integrations"
const val ROUTE_HISTORY = "history"
const val ROUTE_SETTINGS = "settings"

data class WorkspaceNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val group: String,
    val unselectedIcon: ImageVector = icon,
)

val workspaceNavItems = listOf(
    WorkspaceNavItem(ROUTE_OVERVIEW, "Dashboard", Icons.Rounded.SpaceDashboard, "Workspace", Icons.Outlined.SpaceDashboard),
    WorkspaceNavItem(ROUTE_EXPLORER, "Data Explorer", Icons.Rounded.Explore, "Workspace", Icons.Outlined.Explore),
    WorkspaceNavItem(ROUTE_ANALYST, "AI Analyst", Icons.Rounded.Psychology, "Workspace", Icons.Outlined.Psychology),
    WorkspaceNavItem(ROUTE_SQL_STUDIO, "SQL Studio", Icons.Rounded.Code, "Studios", Icons.Outlined.Code),
    WorkspaceNavItem(ROUTE_PYTHON_STUDIO, "Python Studio", Icons.Rounded.Terminal, "Studios"),
    WorkspaceNavItem(ROUTE_DASHBOARD_BUILDER, "Dashboard Builder", Icons.Rounded.Dashboard, "Studios"),
    WorkspaceNavItem(ROUTE_PREDICTIVE, "Predictive", Icons.AutoMirrored.Rounded.TrendingUp, "Intelligence"),
    WorkspaceNavItem(ROUTE_BUSINESS_INTEL, "Business Intel", Icons.Rounded.Insights, "Intelligence"),
    WorkspaceNavItem(ROUTE_REPORTS, "Reports Center", Icons.Rounded.Description, "Intelligence"),
    WorkspaceNavItem(ROUTE_INTEGRATIONS, "Integrations", Icons.Rounded.Hub, "Platform"),
    WorkspaceNavItem(ROUTE_HISTORY, "History & Alerts", Icons.Rounded.History, "Platform"),
    WorkspaceNavItem(ROUTE_SETTINGS, "Settings & Plan", Icons.Rounded.Settings, "Platform"),
)

val bottomBarItems = listOf(
    WorkspaceNavItem(ROUTE_OVERVIEW, "Dashboard", Icons.Rounded.SpaceDashboard, "Workspace", Icons.Outlined.SpaceDashboard),
    WorkspaceNavItem(ROUTE_EXPLORER, "Explorer", Icons.Rounded.Explore, "Workspace", Icons.Outlined.Explore),
    WorkspaceNavItem(ROUTE_ANALYST, "AI Analyst", Icons.Rounded.Psychology, "Workspace", Icons.Outlined.Psychology),
    WorkspaceNavItem(ROUTE_SQL_STUDIO, "SQL Studio", Icons.Rounded.Code, "Studios", Icons.Outlined.Code),
)

@Composable
fun WorkspaceBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = SubtleDivider)
        NavigationBar(
            containerColor = MidnightSurface,
            tonalElevation = 0.dp,
            windowInsets = NavigationBarDefaults.windowInsets,
            modifier = Modifier.testTag("bottom_navigation_bar"),
        ) {
            bottomBarItems.forEach { item ->
                val selected = currentRoute == item.route
                NavigationBarItem(
                    selected = selected,
                    onClick = { onNavigate(item.route) },
                    icon = {
                        Icon(
                            imageVector = if (selected) item.icon else item.unselectedIcon,
                            contentDescription = item.label,
                        )
                    },
                    label = {
                        Text(
                            text = item.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberCyan,
                        selectedTextColor = CyberCyan,
                        indicatorColor = ElectricBlue.copy(alpha = 0.25f),
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                    ),
                    modifier = Modifier.testTag("bottom_nav_${item.route}"),
                )
            }
        }
    }
}

@Composable
fun WorkspaceNavigationRail(
    currentRoute: String,
    onMenuClick: () -> Unit,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationRail(
        containerColor = MidnightSurface,
        header = {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .testTag("rail_menu_button"),
            ) {
                Icon(Icons.Rounded.Menu, contentDescription = "Open full navigation menu", tint = TextPrimary)
            }
        },
        modifier = modifier
            .fillMaxHeight()
            .testTag("workspace_navigation_rail"),
    ) {
        Spacer(Modifier.height(8.dp))
        bottomBarItems.forEach { item ->
            val selected = currentRoute == item.route
            NavigationRailItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = if (selected) item.icon else item.unselectedIcon,
                        contentDescription = item.label,
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = CyberCyan,
                    selectedTextColor = CyberCyan,
                    indicatorColor = ElectricBlue.copy(alpha = 0.25f),
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                ),
                modifier = Modifier.testTag("rail_nav_${item.route}"),
            )
        }
    }
}

@Composable
fun WorkspaceTopBar(
    currentTitle: String,
    globalSearch: String,
    isSearchOpen: Boolean,
    alertCount: Int,
    userInitials: String,
    onMenuClick: () -> Unit,
    onToggleSearch: () -> Unit,
    onSearchChange: (String) -> Unit,
    onAlertsClick: () -> Unit,
    onProfileClick: () -> Unit,
) {
    Surface(
        color = MidnightSurface,
        border = BorderStroke(1.dp, SubtleDivider),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier.testTag("drawer_menu_button"),
                ) {
                    Icon(Icons.Rounded.Menu, contentDescription = "Toggle navigation menu", tint = TextPrimary)
                }

                DataPilotLogo(size = 28.dp)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("top_bar_title"),
                    )
                    Text(
                        text = "DataPilot AI",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberCyan,
                    )
                }

                IconButton(
                    onClick = onToggleSearch,
                    modifier = Modifier.testTag("global_search_button"),
                ) {
                    Icon(
                        imageVector = if (isSearchOpen) Icons.Rounded.Close else Icons.Rounded.Search,
                        contentDescription = "Global search",
                        tint = if (isSearchOpen) CyberCyan else TextSecondary,
                    )
                }

                IconButton(
                    onClick = onAlertsClick,
                    modifier = Modifier.testTag("notifications_button"),
                ) {
                    BadgedBox(
                        badge = {
                            if (alertCount > 0) {
                                Badge(containerColor = CoralError, contentColor = Color.White) {
                                    Text(alertCount.toString())
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Rounded.Notifications, contentDescription = "Workspace alerts", tint = TextSecondary)
                    }
                }

                IconButton(
                    onClick = onProfileClick,
                    modifier = Modifier.testTag("profile_button"),
                ) {
                    if (userInitials.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(ElectricBlue.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(userInitials, style = MaterialTheme.typography.labelMedium, color = CyberCyan)
                        }
                    } else {
                        Icon(Icons.Rounded.AccountCircle, contentDescription = "Profile & settings", tint = CyberCyan)
                    }
                }
            }

            if (isSearchOpen) {
                OutlinedTextField(
                    value = globalSearch,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Jump to module or filter datasets...", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = CyberCyan) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ObsidianBg,
                        unfocusedContainerColor = ObsidianBg,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = SubtleDivider,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("global_search_input"),
                )
            }
        }
    }
}

@Composable
fun WorkspaceDrawerContent(
    currentRoute: String,
    planTier: String,
    activeDatasetName: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(MidnightSurface)
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 14.dp)
            .testTag("workspace_drawer_content"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            DataPilotLogo(size = 36.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("DataPilot AI", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(
                    text = activeDatasetName ?: "No dataset loaded",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StatusPill(label = planTier.uppercase(), color = ElectricBlue)
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = SubtleDivider)
        Spacer(Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val grouped = workspaceNavItems.groupBy { it.group }
            grouped.forEach { (groupName, groupItems) ->
                item(key = "hdr_$groupName") {
                    Text(
                        text = groupName.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
                items(groupItems, key = { it.route }) { item ->
                    val selected = currentRoute == item.route
                    NavigationDrawerItem(
                        selected = selected,
                        onClick = { onNavigate(item.route) },
                        icon = {
                            Icon(
                                imageVector = if (selected) item.icon else item.unselectedIcon,
                                contentDescription = item.label,
                            )
                        },
                        label = { Text(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = ElectricBlue.copy(alpha = 0.22f),
                            selectedIconColor = CyberCyan,
                            selectedTextColor = TextPrimary,
                            unselectedContainerColor = Color.Transparent,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("drawer_nav_${item.route}"),
                    )
                }
            }
        }
    }
}

@Composable
fun GlobalSearchOverlay(
    query: String,
    datasets: List<DatasetRecord>,
    onSelectRoute: (String) -> Unit,
    onSelectDataset: (DatasetRecord) -> Unit,
) {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return

    val matchedRoutes = workspaceNavItems.filter { it.label.lowercase().contains(q) }
    val matchedDatasets = datasets.filter { it.name.lowercase().contains(q) }

    Surface(
        color = SlateCard,
        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Quick Search Results", style = MaterialTheme.typography.labelMedium, color = CyberCyan)
            if (matchedRoutes.isEmpty() && matchedDatasets.isEmpty()) {
                Text("No matching modules or datasets for \"$query\".", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            } else {
                matchedRoutes.take(4).forEach { nav ->
                    Surface(
                        onClick = { onSelectRoute(nav.route) },
                        color = SlateElevated,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(nav.icon, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Open ${nav.label}", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        }
                    }
                }
                matchedDatasets.take(4).forEach { ds ->
                    Surface(
                        onClick = { onSelectDataset(ds) },
                        color = SlateElevated,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Dataset: ${ds.name}", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            StatusPill(label = "${ds.qualityScore}%", color = EmeraldSuccess)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkspaceAlertsDialog(
    activeTable: DataTable?,
    schedules: List<AutomationSchedule>,
    onDismiss: () -> Unit,
    onGoToExplorer: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Workspace Alerts & Quality Checks") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (activeTable == null) {
                    Text("No dataset is currently loaded. Upload a CSV or XLSX file to run automated anomaly & quality monitoring.", style = MaterialTheme.typography.bodySmall)
                } else {
                    val qr = activeTable.qualityReport
                    Text(
                        text = "${activeTable.name}: Quality Score ${qr.qualityScore}/100",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (qr.qualityScore >= 85) EmeraldSuccess else AmberWarning,
                    )
                    if (qr.missingCells > 0) {
                        Text("• ${qr.missingCells} missing values detected.", style = MaterialTheme.typography.bodySmall)
                    }
                    if (qr.duplicateRows > 0) {
                        Text("• ${qr.duplicateRows} duplicate rows detected.", style = MaterialTheme.typography.bodySmall)
                    }
                    if (qr.outlierCells > 0) {
                        Text("• ${qr.outlierCells} statistical IQR outliers flagged.", style = MaterialTheme.typography.bodySmall)
                    }
                    if (qr.missingCells == 0 && qr.duplicateRows == 0 && qr.outlierCells == 0) {
                        Text("• No data quality anomalies detected in active dataset.", style = MaterialTheme.typography.bodySmall, color = EmeraldSuccess)
                    }
                }
                if (schedules.isNotEmpty()) {
                    HorizontalDivider()
                    Text("Active Monitoring Schedules (${schedules.size})", style = MaterialTheme.typography.labelLarge)
                    schedules.take(3).forEach { s ->
                        Text("• ${s.title} (${s.frequency}): ${s.lastRunSummary}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            if (activeTable != null) {
                TextButton(onClick = {
                    onDismiss()
                    onGoToExplorer()
                }) { Text("Inspect Quality") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}
