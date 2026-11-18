const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_rehab_123";
const BOB_UID = "bob_rehab_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

test("Unauthenticated user: denied all reads and writes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("sessions").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("routine").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Intruso",
      affectedHand: "DERECHA",
      primaryGoal: "MOVILIDAD",
      recentSurgeryOrAcutePain: false,
      voiceCoachEnabled: true,
      intakeCompleted: true,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Profile: owner can create and update valid profile; rejects ghost fields & future timestamps", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "María García",
      affectedHand: "DERECHA",
      primaryGoal: "MOVILIDAD",
      clinicalContext: "Rigidez matutina leve",
      recentSurgeryOrAcutePain: false,
      voiceCoachEnabled: true,
      intakeCompleted: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial update preserving createdAt (including daily reminder preferences)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      primaryGoal: "FUERZA",
      voiceCoachEnabled: false,
      dailyReminderEnabled: true,
      reminderHour: 18,
      reminderMinute: 30,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject invalid reminderHour out of 0..23
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      reminderHour: 25,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject mutating createdAt
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      affectedHand: "AMBAS",
      primaryGoal: "FUERZA",
      recentSurgeryOrAcutePain: false,
      voiceCoachEnabled: true,
      intakeCompleted: true,
      isAdmin: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject cross-user profile read/write
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hijacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Routine items: owner CRUD and query succeed; cross-user and invalid payloads fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("routine")
      .doc("tendon_glide")
      .set({
        userId: ALICE_UID,
        exerciseId: "tendon_glide",
        exerciseTitle: "Deslizamiento tendinoso",
        categoryId: "MOVILIDAD",
        targetSets: 3,
        targetReps: 10,
        assignedBy: "Programa inicial",
        active: true,
        createdAt: created,
        updatedAt: created,
      })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("routine").get()
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("routine")
      .doc("tendon_glide")
      .update({
        targetReps: 12,
        active: true,
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Reject extra @DocumentId 'id' field
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("routine")
      .doc("wrist_flex")
      .set({
        id: "wrist_flex",
        userId: ALICE_UID,
        exerciseId: "wrist_flex",
        exerciseTitle: "Flexión de muñeca",
        categoryId: "MOVILIDAD",
        targetSets: 2,
        targetReps: 10,
        assignedBy: "Programa inicial",
        active: true,
        createdAt: created,
        updatedAt: created,
      })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("routine").get()
  );
});

test("Sessions: owner create, query, and pain bounds enforced; cross-user rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("sessions")
      .doc("sess_01")
      .set({
        userId: ALICE_UID,
        exerciseId: "tendon_glide",
        exerciseTitle: "Deslizamiento tendinoso",
        categoryId: "MOVILIDAD",
        completedReps: 10,
        durationSeconds: 120,
        painLevel: 2,
        notes: "Movimiento fluido sin dolor agudo",
        usedVoiceGuide: true,
        createdAt: created,
        updatedAt: created,
      })
  );

  // Session without optional notes (omitted, not null)
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("sessions")
      .doc("sess_02")
      .set({
        userId: ALICE_UID,
        exerciseId: "grip_squeeze",
        exerciseTitle: "Apretón suave de pelota",
        categoryId: "FORTALECIMIENTO",
        completedReps: 8,
        durationSeconds: 90,
        painLevel: 1,
        usedVoiceGuide: false,
        createdAt: created,
        updatedAt: created,
      })
  );

  // Query ordered by createdAt (mirrors Kotlin repository)
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("sessions")
      .orderBy("createdAt", "desc")
      .get()
  );

  // Reject out-of-bounds painLevel > 10
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("sessions")
      .doc("sess_bad_pain")
      .set({
        userId: ALICE_UID,
        exerciseId: "grip_squeeze",
        exerciseTitle: "Apretón suave de pelota",
        categoryId: "FORTALECIMIENTO",
        completedReps: 8,
        durationSeconds: 90,
        painLevel: 15,
        usedVoiceGuide: false,
        createdAt: created,
        updatedAt: created,
      })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("sessions")
      .doc("sess_future")
      .set({
        userId: ALICE_UID,
        exerciseId: "grip_squeeze",
        exerciseTitle: "Apretón suave de pelota",
        categoryId: "FORTALECIMIENTO",
        completedReps: 8,
        durationSeconds: 90,
        painLevel: 2,
        usedVoiceGuide: false,
        createdAt: new Date(Date.now() + 60000),
        updatedAt: new Date(Date.now() + 60000),
      })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("sessions").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("sessions")
      .doc("sess_01")
      .delete()
  );
});

test("Exercises library: filtered published query and valid create succeed; unfiltered list, unauth, and mutations fail", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("exercises").where("published", "==", true).get()
  );

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  // Filtered query on empty collection succeeds (mirrors Kotlin repository)
  await assertSucceeds(
    aliceDb.collection("exercises").where("published", "==", true).get()
  );

  // Unfiltered list query on top-level collection must be rejected
  await assertFails(aliceDb.collection("exercises").get());

  // Valid Kotlin-mirrored exercise create payload (no @DocumentId 'id' field)
  await assertSucceeds(
    aliceDb.collection("exercises").doc("wrist_flex_ext").set({
      exerciseId: "wrist_flex_ext",
      title: "Flexión y extensión de muñeca",
      categoryId: "MOVILIDAD",
      motionType: "WRIST_FLEX_EXT",
      difficultyLabel: "Suave",
      suggestedSets: 3,
      suggestedReps: 10,
      suggestedMinutes: 4,
      sortOrder: 0,
      summary: "Movilización progresiva de la muñeca apoyando el antebrazo.",
      postureInstructions: ["Apoya el antebrazo sobre una mesa acolchada."],
      phases: [
        {
          label: "Elevar muñeca",
          voiceCue: "Eleva suavemente la mano hacia arriba",
          durationSeconds: 3,
          targetProgress: 1.0,
        },
      ],
      avoidWarnings: ["Evita levantar el codo."],
      recommendedForGoals: ["MOVILIDAD"],
      visionTargetPoseName: "Extensión suave de muñeca",
      published: true,
      createdBy: ALICE_UID,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Another signed-in user can read published exercises via filtered query & get
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("exercises").where("published", "==", true).get()
  );
  await assertSucceeds(
    bobDb.collection("exercises").doc("wrist_flex_ext").get()
  );

  // Reject updates, deletes, or ghost 'id' field
  await assertFails(
    bobDb.collection("exercises").doc("wrist_flex_ext").update({
      title: "Modificado",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.collection("exercises").doc("wrist_flex_ext").delete()
  );
  await assertFails(
    aliceDb.collection("exercises").doc("tabletop_bend").set({
      id: "tabletop_bend",
      exerciseId: "tabletop_bend",
      title: "Flexión en mesa",
      categoryId: "MOVILIDAD",
      motionType: "TABLETOP_FLEX",
      difficultyLabel: "Suave",
      suggestedSets: 3,
      suggestedReps: 10,
      suggestedMinutes: 4,
      sortOrder: 1,
      summary: "Flexión exclusiva de los nudillos.",
      postureInstructions: ["Apoya el codo sobre la mesa."],
      phases: [
        {
          label: "Doblar",
          voiceCue: "Dobla solo los nudillos",
          durationSeconds: 3,
          targetProgress: 1.0,
        },
      ],
      avoidWarnings: ["No enrosques las puntas."],
      recommendedForGoals: ["MOVILIDAD"],
      visionTargetPoseName: "Mano en posición de mesa",
      published: true,
      createdBy: ALICE_UID,
      createdAt: created,
      updatedAt: created,
    })
  );
});
