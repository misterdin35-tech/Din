package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

/**
 * Core analytical dataset entity persisted at `/users/{userId}/datasets/{datasetId}`.
 */
@IgnoreExtraProperties
data class Dataset(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val sourceType: String = "csv",
    val rowCount: Int = 0,
    val columnCount: Int = 0,
    val qualityScore: Int = 100,
    val missingCount: Int = 0,
    val duplicateCount: Int = 0,
    val outlierCount: Int = 0,
    val columnsSummary: List<String> = emptyList(),
    val csvContent: String = "",
    val isCleaned: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

/**
 * Valid lifecycle statuses for an [AnalysisJob] persisted in Firestore.
 */
object AnalysisJobStatus {
    const val QUEUED = "queued"
    const val RUNNING = "running"
    const val COMPLETED = "completed"
    const val FAILED = "failed"

    val ALL = setOf(QUEUED, RUNNING, COMPLETED, FAILED)
    val TERMINAL = setOf(COMPLETED, FAILED)
}

/**
 * Core autonomous analysis job entity persisted at `/users/{userId}/analysis_jobs/{jobId}`.
 * Tracks execution lifecycle, active agent progress, and grounded outputs.
 */
@IgnoreExtraProperties
data class AnalysisJob(
    @DocumentId val id: String = "",
    val userId: String = "",
    val datasetId: String = "",
    val datasetName: String = "",
    val question: String = "",
    val status: String = AnalysisJobStatus.QUEUED,
    val currentAgent: String = "AI Orchestrator",
    val progressPercent: Int = 0,
    val executiveSummary: String = "",
    val sqlGenerated: String = "",
    val pythonGenerated: String = "",
    val agentTraceSummary: String = "",
    val errorMessage: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

/**
 * Core saved BI dashboard entity persisted at `/users/{userId}/dashboards/{dashboardId}`.
 */
@IgnoreExtraProperties
data class Dashboard(
    @DocumentId val id: String = "",
    val userId: String = "",
    val datasetId: String = "",
    val title: String = "",
    val widgetsConfig: String = "[]",
    val dateFilter: String = "All Time",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

typealias DatasetRecord = Dataset
typealias DashboardRecord = Dashboard
