package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AnalysisJob
import com.example.data.model.AnalysisRecord
import com.example.data.model.AuditLogRecord
import com.example.data.model.AutomationSchedule
import com.example.data.model.Dashboard
import com.example.data.model.Dataset
import com.example.data.model.IntegrationRecord
import com.example.data.model.ReportRecord
import com.example.data.model.SavedQueryRecord
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class DataPilotRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
    private val coreDataRepository: AnalyticsDataRepository = FirestoreAnalyticsDataRepository(db, auth),
) : AnalyticsDataRepository by coreDataRepository {

    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth,
    )

    private fun userDoc(userId: String) = db.collection("users").document(userId)

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before writing to Firestore.")

    private fun <T> observeSubcollection(
        userId: String,
        subcollection: String,
        clazz: Class<T>,
    ): Flow<List<T>> {
        val colRef = userDoc(userId).collection(subcollection)
        return colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(clazz, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                val firestoreError = unwrapFirestoreException(error)
                if (firestoreError == null && error is CancellationException) throw error
                val currentUid = try {
                    auth.currentUser?.uid
                } catch (_: Exception) {
                    null
                }
                if (currentUid == userId || firestoreError != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, colRef.path)
                }
                throw error
            }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val docRef = userDoc(userId)
        return docRef.snapshots()
            .map { snap ->
                if (snap.exists()) {
                    snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else null
            }
            .catch { error ->
                val firestoreError = unwrapFirestoreException(error)
                if (firestoreError == null && error is CancellationException) throw error
                handleFirestoreError(firestoreError ?: error, OperationType.GET, docRef.path)
                throw error
            }
    }

    suspend fun ensureUserProfile(
        userId: String,
        displayName: String,
        email: String,
    ): Result<UserProfile> = runCatching {
        val signedInUid = requireUserId()
        val targetUid = userId.ifBlank { signedInUid }
        val docRef = userDoc(targetUid)
        val snap = docRef.get().await()
        if (snap.exists()) {
            snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: UserProfile(id = targetUid, userId = targetUid, displayName = displayName, email = email)
        } else {
            val safeName = displayName.ifBlank { "Analyst" }.take(100)
            val payload = buildMap<String, Any> {
                put("userId", targetUid)
                put("displayName", safeName)
                if (email.isNotBlank()) put("email", email.take(200))
                put("organizationName", "DataPilot Workspace")
                put("planTier", "free")
                put("notificationsEnabled", true)
                put("retentionDays", 90)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            docRef.set(payload).await()
            UserProfile(
                id = targetUid,
                userId = targetUid,
                displayName = safeName,
                email = email,
            )
        }
    }.onFailure { e ->
        if (e is CancellationException) throw e
        if (e !is IllegalStateException) {
            handleFirestoreError(e, OperationType.WRITE, userDoc(userId).path)
        }
    }

    suspend fun updateUserProfile(
        userId: String,
        displayName: String,
        organizationName: String,
        planTier: String,
        notificationsEnabled: Boolean,
        retentionDays: Int,
    ): Result<Unit> = runCatching {
        val signedInUid = requireUserId()
        val targetUid = userId.ifBlank { signedInUid }
        val docRef = userDoc(targetUid)
        val updates = mapOf(
            "displayName" to displayName.ifBlank { "Analyst" }.take(100),
            "organizationName" to organizationName.ifBlank { "DataPilot Workspace" }.take(100),
            "planTier" to planTier.lowercase().takeIf { it in listOf("free", "pro", "business", "enterprise") }!!,
            "notificationsEnabled" to notificationsEnabled,
            "retentionDays" to retentionDays.coerceIn(7, 3650),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        docRef.update(updates).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        if (e !is IllegalStateException) {
            handleFirestoreError(e, OperationType.UPDATE, userDoc(userId).path)
        }
    }

    fun observeAnalyses(userId: String): Flow<List<AnalysisRecord>> =
        observeSubcollection(userId, "analyses", AnalysisRecord::class.java)

    suspend fun saveAnalysis(userId: String, record: AnalysisRecord): Result<String> = runCatching {
        val signedInUid = requireUserId()
        val targetUid = userId.ifBlank { signedInUid }
        val docId = "an_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = userDoc(targetUid).collection("analyses").document(docId)
        val payload = mapOf(
            "userId" to targetUid,
            "datasetId" to record.datasetId.take(128),
            "datasetName" to record.datasetName.take(150),
            "question" to record.question.take(2000),
            "executiveSummary" to record.executiveSummary.take(10_000),
            "sqlGenerated" to record.sqlGenerated.take(5000),
            "pythonGenerated" to record.pythonGenerated.take(10_000),
            "agentTraceSummary" to record.agentTraceSummary.take(5000),
            "createdAt" to FieldValue.serverTimestamp(),
        )
        docRef.set(payload).await()
        docId
    }.onFailure { e ->
        if (e is CancellationException) throw e
        if (e !is IllegalStateException) {
            handleFirestoreError(e, OperationType.CREATE, "users/$userId/analyses")
        }
    }

    fun observeSavedQueries(userId: String): Flow<List<SavedQueryRecord>> =
        observeSubcollection(userId, "saved_queries", SavedQueryRecord::class.java)

    suspend fun saveQuery(userId: String, record: SavedQueryRecord): Result<String> = runCatching {
        val signedInUid = requireUserId()
        val targetUid = userId.ifBlank { signedInUid }
        val docId = "sq_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = userDoc(targetUid).collection("saved_queries").document(docId)
        val payload = mapOf(
            "userId" to targetUid,
            "datasetId" to record.datasetId.take(128),
            "datasetName" to record.datasetName.take(150),
            "title" to record.title.take(150),
            "sqlText" to record.sqlText.take(10_000),
            "rowCount" to record.rowCount,
            "executionMs" to record.executionMs,
            "createdAt" to FieldValue.serverTimestamp(),
        )
        docRef.set(payload).await()
        docId
    }.onFailure { e ->
        if (e is CancellationException) throw e
        if (e !is IllegalStateException) {
            handleFirestoreError(e, OperationType.CREATE, "users/$userId/saved_queries")
        }
    }

    fun observeReports(userId: String): Flow<List<ReportRecord>> =
        observeSubcollection(userId, "reports", ReportRecord::class.java)

    suspend fun saveReport(userId: String, record: ReportRecord): Result<String> = runCatching {
        val signedInUid = requireUserId()
        val targetUid = userId.ifBlank { signedInUid }
        val docId = "rp_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = userDoc(targetUid).collection("reports").document(docId)
        docRef.set(
            mapOf(
                "userId" to targetUid,
                "datasetId" to record.datasetId.take(128),
                "datasetName" to record.datasetName.take(150),
                "title" to record.title.take(150),
                "format" to record.format,
                "summary" to record.summary.take(20_000),
                "createdAt" to FieldValue.serverTimestamp(),
            )
        ).await()
        docId
    }.onFailure { e ->
        if (e is CancellationException) throw e
        if (e !is IllegalStateException) {
            handleFirestoreError(e, OperationType.CREATE, "users/$userId/reports")
        }
    }

    fun observeIntegrations(userId: String): Flow<List<IntegrationRecord>> =
        observeSubcollection(userId, "integrations", IntegrationRecord::class.java)

    suspend fun saveIntegration(userId: String, record: IntegrationRecord): Result<String> = runCatching {
        val signedInUid = requireUserId()
        val targetUid = userId.ifBlank { signedInUid }
        val docId = record.id.takeIf { it.isNotBlank() && it.matches(Regex("^[a-zA-Z0-9_\\-]+$")) }
            ?: "in_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = userDoc(targetUid).collection("integrations").document(docId)
        val isUpdate = record.id.isNotBlank() && record.createdAt != null
        if (isUpdate) {
            docRef.update(
                mapOf(
                    "label" to record.label.take(150),
                    "hostOrUri" to record.hostOrUri.take(500),
                    "status" to record.status,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        } else {
            docRef.set(
                mapOf(
                    "userId" to targetUid,
                    "provider" to record.provider,
                    "label" to record.label.take(150),
                    "hostOrUri" to record.hostOrUri.take(500),
                    "status" to record.status,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        }
        docId
    }.onFailure { e ->
        if (e is CancellationException) throw e
        if (e !is IllegalStateException) {
            handleFirestoreError(e, OperationType.WRITE, "users/$userId/integrations")
        }
    }

    suspend fun deleteIntegration(userId: String, integrationId: String): Result<Unit> = runCatching {
        val signedInUid = requireUserId()
        val targetUid = userId.ifBlank { signedInUid }
        userDoc(targetUid).collection("integrations").document(integrationId).delete().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        if (e !is IllegalStateException) {
            handleFirestoreError(e, OperationType.DELETE, "users/$userId/integrations/$integrationId")
        }
    }

    fun observeSchedules(userId: String): Flow<List<AutomationSchedule>> =
        observeSubcollection(userId, "schedules", AutomationSchedule::class.java)

    suspend fun saveSchedule(userId: String, record: AutomationSchedule): Result<String> = runCatching {
        val signedInUid = requireUserId()
        val targetUid = userId.ifBlank { signedInUid }
        val docId = record.id.takeIf { it.isNotBlank() && it.matches(Regex("^[a-zA-Z0-9_\\-]+$")) }
            ?: "sc_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = userDoc(targetUid).collection("schedules").document(docId)
        val isUpdate = record.id.isNotBlank() && record.createdAt != null
        if (isUpdate) {
            docRef.update(
                mapOf(
                    "title" to record.title.take(150),
                    "frequency" to record.frequency,
                    "enabled" to record.enabled,
                    "anomalyAlerts" to record.anomalyAlerts,
                    "emailDigestConsent" to record.emailDigestConsent,
                    "lastRunSummary" to record.lastRunSummary.take(2000),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        } else {
            docRef.set(
                mapOf(
                    "userId" to targetUid,
                    "datasetId" to record.datasetId.take(128),
                    "title" to record.title.take(150),
                    "frequency" to record.frequency,
                    "enabled" to record.enabled,
                    "anomalyAlerts" to record.anomalyAlerts,
                    "emailDigestConsent" to record.emailDigestConsent,
                    "lastRunSummary" to record.lastRunSummary.take(2000),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        }
        docId
    }.onFailure { e ->
        if (e is CancellationException) throw e
        if (e !is IllegalStateException) {
            handleFirestoreError(e, OperationType.WRITE, "users/$userId/schedules")
        }
    }

    fun observeAuditLogs(userId: String): Flow<List<AuditLogRecord>> =
        observeSubcollection(userId, "audit_logs", AuditLogRecord::class.java)

    suspend fun logAuditEvent(
        userId: String,
        action: String,
        resourceType: String,
        detail: String,
    ): Result<String> = runCatching {
        val signedInUid = requireUserId()
        val targetUid = userId.ifBlank { signedInUid }
        val docId = "al_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = userDoc(targetUid).collection("audit_logs").document(docId)
        docRef.set(
            mapOf(
                "userId" to targetUid,
                "action" to action.take(120),
                "resourceType" to resourceType.take(80),
                "detail" to detail.take(1000),
                "createdAt" to FieldValue.serverTimestamp(),
            )
        ).await()
        docId
    }.onFailure { e ->
        if (e is CancellationException) throw e
        if (e !is IllegalStateException) {
            handleFirestoreError(e, OperationType.CREATE, "users/$userId/audit_logs")
        }
    }

    suspend fun deleteWorkspaceData(userId: String): Result<Unit> = runCatching {
        val signedInUid = requireUserId()
        val targetUid = userId.ifBlank { signedInUid }
        val collections = listOf(
            FirestoreAnalyticsDataRepository.COLLECTION_DATASETS,
            FirestoreAnalyticsDataRepository.COLLECTION_ANALYSIS_JOBS,
            "analyses",
            "saved_queries",
            FirestoreAnalyticsDataRepository.COLLECTION_DASHBOARDS,
            "reports",
            "integrations",
            "schedules",
        )
        for (col in collections) {
            val docs = userDoc(targetUid).collection(col).get().await()
            for (doc in docs.documents) {
                doc.reference.delete().await()
            }
        }
        logAuditEvent(
            userId = targetUid,
            action = "DATA_PURGE",
            resourceType = "workspace",
            detail = "User initiated full workspace data deletion.",
        )
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        if (e !is IllegalStateException) {
            handleFirestoreError(e, OperationType.DELETE, "users/$userId")
        }
    }
}
