package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AnalysisJob
import com.example.data.model.AnalysisJobStatus
import com.example.data.model.Dashboard
import com.example.data.model.Dataset
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

/**
 * Contract for persisting and observing core analytical entities
 * ([Dataset], [AnalysisJob], and [Dashboard]) in Cloud Firestore.
 */
interface AnalyticsDataRepository {
    fun observeDatasets(userId: String): Flow<List<Dataset>>
    suspend fun getDataset(userId: String, datasetId: String): Result<Dataset?>
    suspend fun saveDataset(userId: String, dataset: Dataset): Result<String>
    suspend fun deleteDataset(userId: String, datasetId: String): Result<Unit>

    fun observeAnalysisJobs(userId: String): Flow<List<AnalysisJob>>
    suspend fun getAnalysisJob(userId: String, jobId: String): Result<AnalysisJob?>
    suspend fun createAnalysisJob(userId: String, job: AnalysisJob): Result<String>
    suspend fun updateAnalysisJob(userId: String, job: AnalysisJob): Result<Unit>
    suspend fun deleteAnalysisJob(userId: String, jobId: String): Result<Unit>

    fun observeDashboards(userId: String): Flow<List<Dashboard>>
    suspend fun getDashboard(userId: String, dashboardId: String): Result<Dashboard?>
    suspend fun saveDashboard(userId: String, dashboard: Dashboard): Result<String>
    suspend fun deleteDashboard(userId: String, dashboardId: String): Result<Unit>
}

class FirestoreAnalyticsDataRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) : AnalyticsDataRepository {

    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth,
    )

    private fun userDoc(userId: String) = db.collection("users").document(userId)

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

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

    private suspend fun <T> getDocument(
        path: String,
        docRef: com.google.firebase.firestore.DocumentReference,
        clazz: Class<T>,
    ): Result<T?> = runCatching {
        val snap = docRef.get().await()
        if (snap.exists()) {
            snap.toObject(clazz, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } else {
            null
        }
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.GET, path)
    }

    override fun observeDatasets(userId: String): Flow<List<Dataset>> =
        observeSubcollection(userId, COLLECTION_DATASETS, Dataset::class.java)

    override suspend fun getDataset(userId: String, datasetId: String): Result<Dataset?> {
        require(VALID_ID_REGEX.matches(datasetId)) { "Invalid dataset ID" }
        val docRef = userDoc(userId).collection(COLLECTION_DATASETS).document(datasetId)
        return getDocument(docRef.path, docRef, Dataset::class.java)
    }

    override suspend fun saveDataset(userId: String, dataset: Dataset): Result<String> = runCatching {
        val signedInUid = requireUserId()
        val targetUid = userId.ifBlank { signedInUid }
        val docId = dataset.id.takeIf { it.isNotBlank() && VALID_ID_REGEX.matches(it) }
            ?: "ds_${newShortId()}"
        val docRef = userDoc(targetUid).collection(COLLECTION_DATASETS).document(docId)
        val isUpdate = dataset.id.isNotBlank() && dataset.createdAt != null

        if (isUpdate) {
            val updates = mapOf(
                "name" to dataset.name.ifBlank { "Untitled Dataset" }.take(150),
                "rowCount" to dataset.rowCount.coerceAtLeast(0),
                "columnCount" to dataset.columnCount.coerceAtLeast(1),
                "qualityScore" to dataset.qualityScore.coerceIn(0, 100),
                "missingCount" to dataset.missingCount.coerceAtLeast(0),
                "duplicateCount" to dataset.duplicateCount.coerceAtLeast(0),
                "outlierCount" to dataset.outlierCount.coerceAtLeast(0),
                "columnsSummary" to dataset.columnsSummary.take(200),
                "csvContent" to dataset.csvContent.ifBlank { "col\n" }.take(490_000),
                "isCleaned" to dataset.isCleaned,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            docRef.update(updates).await()
        } else {
            val payload = mapOf(
                "userId" to targetUid,
                "name" to dataset.name.ifBlank { "Untitled Dataset" }.take(150),
                "sourceType" to dataset.sourceType.takeIf { it in VALID_SOURCE_TYPES }!!,
                "rowCount" to dataset.rowCount.coerceAtLeast(0),
                "columnCount" to dataset.columnCount.coerceAtLeast(1),
                "qualityScore" to dataset.qualityScore.coerceIn(0, 100),
                "missingCount" to dataset.missingCount.coerceAtLeast(0),
                "duplicateCount" to dataset.duplicateCount.coerceAtLeast(0),
                "outlierCount" to dataset.outlierCount.coerceAtLeast(0),
                "columnsSummary" to dataset.columnsSummary.take(200),
                "csvContent" to dataset.csvContent.ifBlank { "col\n" }.take(490_000),
                "isCleaned" to dataset.isCleaned,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            docRef.set(payload).await()
        }
        docId
    }.onFailure { e ->
        if (e is CancellationException) throw e
        if (e !is IllegalStateException && e !is IllegalArgumentException) {
            handleFirestoreError(e, OperationType.WRITE, "users/$userId/$COLLECTION_DATASETS")
        }
    }

    override suspend fun deleteDataset(userId: String, datasetId: String): Result<Unit> = runCatching {
        val signedInUid = requireUserId()
        val targetUid = userId.ifBlank { signedInUid }
        require(VALID_ID_REGEX.matches(datasetId)) { "Invalid dataset ID" }
        userDoc(targetUid).collection(COLLECTION_DATASETS).document(datasetId).delete().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        if (e !is IllegalStateException && e !is IllegalArgumentException) {
            handleFirestoreError(e, OperationType.DELETE, "users/$userId/$COLLECTION_DATASETS/$datasetId")
        }
    }

    override fun observeAnalysisJobs(userId: String): Flow<List<AnalysisJob>> =
        observeSubcollection(userId, COLLECTION_ANALYSIS_JOBS, AnalysisJob::class.java)

    override suspend fun getAnalysisJob(userId: String, jobId: String): Result<AnalysisJob?> {
        require(VALID_ID_REGEX.matches(jobId)) { "Invalid analysis job ID" }
        val docRef = userDoc(userId).collection(COLLECTION_ANALYSIS_JOBS).document(jobId)
        return getDocument(docRef.path, docRef, AnalysisJob::class.java)
    }

    override suspend fun createAnalysisJob(userId: String, job: AnalysisJob): Result<String> = runCatching {
        val signedInUid = requireUserId()
        val targetUid = userId.ifBlank { signedInUid }
        val docId = job.id.takeIf { it.isNotBlank() && VALID_ID_REGEX.matches(it) }
            ?: "job_${newShortId()}"
        val docRef = userDoc(targetUid).collection(COLLECTION_ANALYSIS_JOBS).document(docId)
        val initialStatus = job.status.lowercase().takeIf {
            it == AnalysisJobStatus.QUEUED || it == AnalysisJobStatus.RUNNING
        } ?: AnalysisJobStatus.QUEUED
        val payload = mapOf(
            "userId" to targetUid,
            "datasetId" to job.datasetId.ifBlank { "ds_default" }.take(128),
            "datasetName" to job.datasetName.ifBlank { "Active Dataset" }.take(150),
            "question" to job.question.ifBlank { "Autonomous dataset analysis" }.take(2000),
            "status" to initialStatus,
            "currentAgent" to job.currentAgent.ifBlank { "AI Orchestrator" }.take(120),
            "progressPercent" to job.progressPercent.coerceIn(0, 100),
            "executiveSummary" to job.executiveSummary.take(10_000),
            "sqlGenerated" to job.sqlGenerated.take(5000),
            "pythonGenerated" to job.pythonGenerated.take(10_000),
            "agentTraceSummary" to job.agentTraceSummary.take(5000),
            "errorMessage" to job.errorMessage.take(2000),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        docRef.set(payload).await()
        docId
    }.onFailure { e ->
        if (e is CancellationException) throw e
        if (e !is IllegalStateException && e !is IllegalArgumentException) {
            handleFirestoreError(e, OperationType.CREATE, "users/$userId/$COLLECTION_ANALYSIS_JOBS")
        }
    }

    override suspend fun updateAnalysisJob(userId: String, job: AnalysisJob): Result<Unit> = runCatching {
        val signedInUid = requireUserId()
        val targetUid = userId.ifBlank { signedInUid }
        require(job.id.isNotBlank() && VALID_ID_REGEX.matches(job.id)) { "Valid job.id is required for update" }
        val normalizedStatus = job.status.lowercase()
        require(normalizedStatus in AnalysisJobStatus.ALL) { "Invalid AnalysisJob status: ${job.status}" }

        val docRef = userDoc(targetUid).collection(COLLECTION_ANALYSIS_JOBS).document(job.id)
        val updates = mapOf(
            "status" to normalizedStatus,
            "currentAgent" to job.currentAgent.ifBlank { "AI Orchestrator" }.take(120),
            "progressPercent" to job.progressPercent.coerceIn(0, 100),
            "executiveSummary" to job.executiveSummary.take(10_000),
            "sqlGenerated" to job.sqlGenerated.take(5000),
            "pythonGenerated" to job.pythonGenerated.take(10_000),
            "agentTraceSummary" to job.agentTraceSummary.take(5000),
            "errorMessage" to job.errorMessage.take(2000),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        docRef.update(updates).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        if (e !is IllegalStateException && e !is IllegalArgumentException) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$userId/$COLLECTION_ANALYSIS_JOBS/${job.id}")
        }
    }

    override suspend fun deleteAnalysisJob(userId: String, jobId: String): Result<Unit> = runCatching {
        val signedInUid = requireUserId()
        val targetUid = userId.ifBlank { signedInUid }
        require(VALID_ID_REGEX.matches(jobId)) { "Invalid analysis job ID" }
        userDoc(targetUid).collection(COLLECTION_ANALYSIS_JOBS).document(jobId).delete().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        if (e !is IllegalStateException && e !is IllegalArgumentException) {
            handleFirestoreError(e, OperationType.DELETE, "users/$userId/$COLLECTION_ANALYSIS_JOBS/$jobId")
        }
    }

    override fun observeDashboards(userId: String): Flow<List<Dashboard>> =
        observeSubcollection(userId, COLLECTION_DASHBOARDS, Dashboard::class.java)

    override suspend fun getDashboard(userId: String, dashboardId: String): Result<Dashboard?> {
        require(VALID_ID_REGEX.matches(dashboardId)) { "Invalid dashboard ID" }
        val docRef = userDoc(userId).collection(COLLECTION_DASHBOARDS).document(dashboardId)
        return getDocument(docRef.path, docRef, Dashboard::class.java)
    }

    override suspend fun saveDashboard(userId: String, dashboard: Dashboard): Result<String> = runCatching {
        val signedInUid = requireUserId()
        val targetUid = userId.ifBlank { signedInUid }
        val docId = dashboard.id.takeIf { it.isNotBlank() && VALID_ID_REGEX.matches(it) }
            ?: "db_${newShortId()}"
        val docRef = userDoc(targetUid).collection(COLLECTION_DASHBOARDS).document(docId)
        val isUpdate = dashboard.id.isNotBlank() && dashboard.createdAt != null
        val safeTitle = dashboard.title.ifBlank { "Untitled Dashboard" }.take(150)
        val safeWidgets = dashboard.widgetsConfig.ifBlank { "[]" }.take(99_000)
        val safeFilter = dashboard.dateFilter.ifBlank { "All Time" }.take(100)

        if (isUpdate) {
            docRef.update(
                mapOf(
                    "title" to safeTitle,
                    "widgetsConfig" to safeWidgets,
                    "dateFilter" to safeFilter,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        } else {
            docRef.set(
                mapOf(
                    "userId" to targetUid,
                    "datasetId" to dashboard.datasetId.ifBlank { "ds_default" }.take(128),
                    "title" to safeTitle,
                    "widgetsConfig" to safeWidgets,
                    "dateFilter" to safeFilter,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        }
        docId
    }.onFailure { e ->
        if (e is CancellationException) throw e
        if (e !is IllegalStateException && e !is IllegalArgumentException) {
            handleFirestoreError(e, OperationType.WRITE, "users/$userId/$COLLECTION_DASHBOARDS")
        }
    }

    override suspend fun deleteDashboard(userId: String, dashboardId: String): Result<Unit> = runCatching {
        val signedInUid = requireUserId()
        val targetUid = userId.ifBlank { signedInUid }
        require(VALID_ID_REGEX.matches(dashboardId)) { "Invalid dashboard ID" }
        userDoc(targetUid).collection(COLLECTION_DASHBOARDS).document(dashboardId).delete().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        if (e !is IllegalStateException && e !is IllegalArgumentException) {
            handleFirestoreError(e, OperationType.DELETE, "users/$userId/$COLLECTION_DASHBOARDS/$dashboardId")
        }
    }

    private fun newShortId(): String = UUID.randomUUID().toString().replace("-", "").take(16)

    companion object {
        const val COLLECTION_DATASETS = "datasets"
        const val COLLECTION_ANALYSIS_JOBS = "analysis_jobs"
        const val COLLECTION_DASHBOARDS = "dashboards"

        private val VALID_ID_REGEX = Regex("^[a-zA-Z0-9_\\-]{1,128}$")
        private val VALID_SOURCE_TYPES = setOf("csv", "xlsx", "sample")
    }
}
