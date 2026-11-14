package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val organizationName: String = "Personal Workspace",
    val planTier: String = "free",
    val notificationsEnabled: Boolean = true,
    val retentionDays: Int = 90,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class AnalysisRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val datasetId: String = "",
    val datasetName: String = "",
    val question: String = "",
    val executiveSummary: String = "",
    val sqlGenerated: String = "",
    val pythonGenerated: String = "",
    val agentTraceSummary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class SavedQueryRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val datasetId: String = "",
    val datasetName: String = "",
    val title: String = "",
    val sqlText: String = "",
    val rowCount: Int = 0,
    val executionMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ReportRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val datasetId: String = "",
    val datasetName: String = "",
    val title: String = "",
    val format: String = "pdf",
    val summary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class IntegrationRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val provider: String = "csv_excel",
    val label: String = "",
    val hostOrUri: String = "",
    val status: String = "configured",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class AutomationSchedule(
    @DocumentId val id: String = "",
    val userId: String = "",
    val datasetId: String = "",
    val title: String = "",
    val frequency: String = "daily",
    val enabled: Boolean = true,
    val anomalyAlerts: Boolean = true,
    val emailDigestConsent: Boolean = false,
    val lastRunSummary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class AuditLogRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val action: String = "",
    val resourceType: String = "",
    val detail: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
)
