package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.AnalysisJob
import com.example.data.model.AnalysisJobStatus
import com.example.data.model.Dashboard
import com.example.data.model.Dataset
import com.example.data.model.SavedQueryRecord
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class DataPilotRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureProfileAndSaveDataset_authenticatedOwner_succeedsAndEmits(): Unit = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = DataPilotRepository(firestore, auth)

        val profileRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserProfile(aliceUid, "Alice", ALICE_EMAIL)
        }
        assertTrue(profileRes.isSuccess)

        val dsId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveDataset(
                aliceUid,
                Dataset(
                    userId = aliceUid,
                    name = "Global Sales ${UUID.randomUUID().toString().take(6)}",
                    sourceType = "csv",
                    rowCount = 24,
                    columnCount = 3,
                    qualityScore = 96,
                    missingCount = 1,
                    duplicateCount = 0,
                    outlierCount = 1,
                    columnsSummary = listOf("Date", "Region", "Revenue"),
                    csvContent = "Date,Region,Revenue\n2026-01-01,NA,1200",
                    isCleaned = false,
                )
            ).getOrThrow()
        }
        assertTrue(dsId.isNotBlank())

        val datasets = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeDatasets(aliceUid).first { list -> list.any { it.id == dsId } }
        }
        assertTrue(datasets.any { it.id == dsId })

        val fetched = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.getDataset(aliceUid, dsId).getOrThrow()
        }
        assertNotNull(fetched)
        assertEquals(dsId, fetched?.id)
    }

    @Test
    fun analysisJobLifecycle_createUpdateObserveDelete_succeeds(): Unit = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val coreRepo: AnalyticsDataRepository = FirestoreAnalyticsDataRepository(firestore, auth)

        val jobId = withTimeout(DEFAULT_TIMEOUT_MS) {
            coreRepo.createAnalysisJob(
                aliceUid,
                AnalysisJob(
                    userId = aliceUid,
                    datasetId = "ds_retail",
                    datasetName = "Retail Sales",
                    question = "Which region has the highest Q4 margin?",
                    status = AnalysisJobStatus.RUNNING,
                    currentAgent = "SQL Analyst",
                    progressPercent = 35,
                )
            ).getOrThrow()
        }
        assertTrue(jobId.isNotBlank())

        val updateRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            coreRepo.updateAnalysisJob(
                aliceUid,
                AnalysisJob(
                    id = jobId,
                    userId = aliceUid,
                    datasetId = "ds_retail",
                    datasetName = "Retail Sales",
                    question = "Which region has the highest Q4 margin?",
                    status = AnalysisJobStatus.COMPLETED,
                    currentAgent = "Verification & Business Insights",
                    progressPercent = 100,
                    executiveSummary = "EMEA led Q4 margin at 28.4%.",
                    sqlGenerated = "SELECT region, AVG(margin) FROM dataset GROUP BY region;",
                    pythonGenerated = "df.groupby('region')['margin'].mean()",
                    agentTraceSummary = "Orchestrator: DONE | SQL Analyst: DONE",
                )
            )
        }
        assertTrue("Expected update to succeed: ${updateRes.exceptionOrNull()}", updateRes.isSuccess)

        val emittedJobs = withTimeout(FLOW_TIMEOUT_MS) {
            coreRepo.observeAnalysisJobs(aliceUid).first { list ->
                list.any { it.id == jobId && it.status == AnalysisJobStatus.COMPLETED }
            }
        }
        val completedJob = emittedJobs.first { it.id == jobId }
        assertEquals(100, completedJob.progressPercent)
        assertEquals("EMEA led Q4 margin at 28.4%.", completedJob.executiveSummary)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            coreRepo.deleteAnalysisJob(aliceUid, jobId).getOrThrow()
        }
        val deletedJob = withTimeout(DEFAULT_TIMEOUT_MS) {
            coreRepo.getAnalysisJob(aliceUid, jobId).getOrThrow()
        }
        assertNull(deletedJob)
    }

    @Test
    fun dashboardCrud_authenticatedOwner_succeedsAndEmits(): Unit = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val coreRepo: AnalyticsDataRepository = FirestoreAnalyticsDataRepository(firestore, auth)

        val dbId = withTimeout(DEFAULT_TIMEOUT_MS) {
            coreRepo.saveDashboard(
                aliceUid,
                Dashboard(
                    userId = aliceUid,
                    datasetId = "ds_retail",
                    title = "Executive KPI Board",
                    widgetsConfig = "[{\"id\":\"w1\",\"widgetType\":\"kpi\"}]",
                    dateFilter = "Last 90 Days",
                )
            ).getOrThrow()
        }

        val created = withTimeout(FLOW_TIMEOUT_MS) {
            coreRepo.observeDashboards(aliceUid).first { list -> list.any { it.id == dbId } }
        }.first { it.id == dbId }
        assertEquals("Executive KPI Board", created.title)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            coreRepo.saveDashboard(
                aliceUid,
                created.copy(title = "Executive KPI Board v2")
            ).getOrThrow()
        }
        val updated = withTimeout(DEFAULT_TIMEOUT_MS) {
            coreRepo.getDashboard(aliceUid, dbId).getOrThrow()
        }
        assertEquals("Executive KPI Board v2", updated?.title)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            coreRepo.deleteDashboard(aliceUid, dbId).getOrThrow()
        }
    }

    @Test
    fun saveQueryAndAuditLog_authenticatedOwner_succeeds(): Unit = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = DataPilotRepository(firestore, auth)

        val queryId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveQuery(
                aliceUid,
                SavedQueryRecord(
                    userId = aliceUid,
                    datasetId = "ds_demo",
                    datasetName = "Global Sales",
                    title = "Revenue by Region",
                    sqlText = "SELECT Region, SUM(Revenue) FROM dataset GROUP BY Region",
                    rowCount = 4,
                    executionMs = 12L,
                )
            ).getOrThrow()
        }
        assertTrue(queryId.isNotBlank())

        val logRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.logAuditEvent(
                userId = aliceUid,
                action = "SQL_RUN",
                resourceType = "saved_queries",
                detail = "Executed query $queryId",
            )
        }
        assertTrue(logRes.isSuccess)
    }

    @Test
    fun writeOperations_unauthenticatedCaller_failFastWithIllegalState(): Unit = runBlocking {
        auth.signOut()
        val coreRepo: AnalyticsDataRepository = FirestoreAnalyticsDataRepository(firestore, auth)
        val res = coreRepo.createAnalysisJob(
            "some_uid",
            AnalysisJob(question = "Test unauth write")
        )
        assertTrue(res.isFailure)
        assertTrue(res.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun observeDatasets_crossUserAccess_failsWithPermissionDenied(): Unit = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = DataPilotRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveDataset(
                aliceUid,
                Dataset(
                    userId = aliceUid,
                    name = "Private Alice Data",
                    sourceType = "csv",
                    rowCount = 1,
                    columnCount = 1,
                    qualityScore = 100,
                    columnsSummary = listOf("Col"),
                    csvContent = "Col\n1",
                )
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = DataPilotRepository(firestore, auth)
        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeDatasets(aliceUid).first()
            }
        } catch (t: Throwable) {
            caught = t
        }
        assertNotNull("Expected PERMISSION_DENIED exception", caught)
        val firestoreEx = caught?.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException, got $caught", firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun observeDatasets_unauthenticatedCaller_failsWithPermissionDenied(): Unit = runBlocking {
        auth.signOut()
        val repo = DataPilotRepository(firestore, auth)
        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeDatasets("some_uid_123").first()
            }
        } catch (t: Throwable) {
            caught = t
        }
        assertNotNull("Expected PERMISSION_DENIED exception", caught)
        val firestoreEx = caught?.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException, got $caught", firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@datapilot.test"
        const val BOB_EMAIL = "bob@datapilot.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
