const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function pastTimestamp() {
  return new Date(Date.now() - 2000);
}

test("Unauthenticated user cannot read or write user profile, datasets, or analysis jobs", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("datasets").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("analysis_jobs").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("dashboards").get());
});

test("Authenticated owner can create and update their UserProfile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);
  const ts = pastTimestamp();
  await assertSucceeds(
    profileRef.set({
      userId: ALICE_UID,
      displayName: "Alice Analyst",
      email: "alice@example.com",
      organizationName: "Alice Corp",
      planTier: "pro",
      notificationsEnabled: true,
      retentionDays: 90,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    profileRef.update({
      planTier: "business",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Shadow update with ghost field is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastTimestamp();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      organizationName: "Org",
      planTier: "free",
      notificationsEnabled: true,
      retentionDays: 30,
      isAdmin: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Cross-user isolation: Bob cannot read or write Alice datasets", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastTimestamp();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("datasets").doc("ds_1").set({
      userId: ALICE_UID,
      name: "Sales Q1",
      sourceType: "csv",
      rowCount: 100,
      columnCount: 5,
      qualityScore: 95,
      missingCount: 0,
      duplicateCount: 0,
      outlierCount: 1,
      columnsSummary: ["Date", "Revenue"],
      csvContent: "Date,Revenue\n2026-01-01,1000",
      isCleaned: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("datasets").doc("ds_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("datasets").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("datasets").doc("ds_1").delete());
});

test("AnalysisJob enforces lifecycle states, terminal locking, and owner isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const jobRef = aliceDb.collection("users").doc(ALICE_UID).collection("analysis_jobs").doc("job_1");
  const ts = pastTimestamp();

  // Invalid initial status ('completed') must fail on create
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("analysis_jobs").doc("job_bad").set({
      userId: ALICE_UID,
      datasetId: "ds_1",
      datasetName: "Sales Q1",
      question: "What drove revenue?",
      status: "completed",
      currentAgent: "AI Orchestrator",
      progressPercent: 100,
      executiveSummary: "",
      sqlGenerated: "",
      pythonGenerated: "",
      agentTraceSummary: "",
      errorMessage: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Valid create as queued
  await assertSucceeds(
    jobRef.set({
      userId: ALICE_UID,
      datasetId: "ds_1",
      datasetName: "Sales Q1",
      question: "What drove revenue?",
      status: "queued",
      currentAgent: "AI Orchestrator",
      progressPercent: 0,
      executiveSummary: "",
      sqlGenerated: "",
      pythonGenerated: "",
      agentTraceSummary: "",
      errorMessage: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Cross-user read & update rejected
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("analysis_jobs").doc("job_1").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("analysis_jobs").doc("job_1").update({
      status: "running",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Owner updates running -> completed
  await assertSucceeds(
    jobRef.update({
      status: "running",
      currentAgent: "SQL Analyst",
      progressPercent: 50,
      updatedAt: new Date(Date.now() - 400),
    })
  );

  await assertSucceeds(
    jobRef.update({
      status: "completed",
      currentAgent: "Verification & Business Insights",
      progressPercent: 100,
      executiveSummary: "North America led revenue growth.",
      sqlGenerated: "SELECT region, SUM(revenue) FROM dataset GROUP BY region;",
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Terminal state lock: cannot update after status == completed
  await assertFails(
    jobRef.update({
      status: "running",
      progressPercent: 10,
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("Audit logs are append-only and reject updates and deletes", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const logRef = aliceDb.collection("users").doc(ALICE_UID).collection("audit_logs").doc("log_1");

  await assertSucceeds(
    logRef.set({
      userId: ALICE_UID,
      action: "RUN_SQL",
      resourceType: "query",
      detail: "Executed SELECT * FROM dataset",
      createdAt: pastTimestamp(),
    })
  );

  await assertFails(logRef.update({ action: "TAMPERED" }));
  await assertFails(logRef.delete());
});
