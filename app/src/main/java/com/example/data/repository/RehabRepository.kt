package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.catalog.ExerciseCatalog
import com.example.data.model.AffectedHand
import com.example.data.model.ExerciseSession
import com.example.data.model.FirestoreExercise
import com.example.data.model.PrimaryGoal
import com.example.data.model.RehabExercise
import com.example.data.model.RoutineItem
import com.example.data.model.UserRehabProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class RehabRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireSignedInUid(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Debes iniciar sesión con Google para guardar tu progreso.")
        check(uid == expectedUserId) { "La sesión activa cambió; operación cancelada." }
        return uid
    }

    private fun requireAnySignedInUid(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Debes iniciar sesión con Google para consultar la biblioteca.")
    }

    fun observeExercises(): Flow<List<RehabExercise>> {
        val path = "exercises"
        return db.collection(path)
            .whereEqualTo("published", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FirestoreExercise::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                    .sortedBy { it.sortOrder }
                    .map { it.toDomain() }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun seedExercisesIfEmpty(): Result<Unit> {
        val basePath = "exercises"
        return try {
            val uid = requireAnySignedInUid()
            val collectionRef = db.collection(basePath)
            val existing = collectionRef
                .whereEqualTo("published", true)
                .get()
                .await()
            val existingIds = existing.documents.map { it.id }.toSet()
            val missing = ExerciseCatalog.allExercises
                .mapIndexed { index, exercise -> index to exercise }
                .filter { (_, exercise) -> exercise.id !in existingIds }

            if (missing.isNotEmpty()) {
                val batch = db.batch()
                missing.forEach { (index, exercise) ->
                    val docRef = collectionRef.document(exercise.id)
                    val payload = mapOf(
                        "exerciseId" to exercise.id,
                        "title" to exercise.title.take(100),
                        "categoryId" to exercise.category.id,
                        "motionType" to exercise.motionType.name,
                        "difficultyLabel" to exercise.difficultyLabel.take(30),
                        "suggestedSets" to exercise.suggestedSets.coerceIn(1, 10).toLong(),
                        "suggestedReps" to exercise.suggestedReps.coerceIn(1, 50).toLong(),
                        "suggestedMinutes" to exercise.suggestedMinutes.coerceIn(1, 60).toLong(),
                        "sortOrder" to index.toLong(),
                        "summary" to exercise.summary.take(400),
                        "postureInstructions" to exercise.postureInstructions.take(10),
                        "phases" to exercise.phases.take(8).map { phase ->
                            mapOf(
                                "label" to phase.label,
                                "voiceCue" to phase.voiceCue,
                                "durationSeconds" to phase.durationSeconds.toLong(),
                                "targetProgress" to phase.targetProgress.toDouble()
                            )
                        },
                        "avoidWarnings" to exercise.avoidWarnings.take(10),
                        "recommendedForGoals" to exercise.recommendedForGoals.take(4).map { it.id },
                        "visionTargetPoseName" to exercise.visionTargetPoseName.take(150),
                        "published" to true,
                        "createdBy" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    batch.set(docRef, payload)
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, basePath)
            Result.failure(e)
        }
    }

    fun observeProfile(userId: String): Flow<UserRehabProfile?> {
        val docRef = db.collection("users").document(userId)
        return docRef.snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        UserRehabProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else {
                    null
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(firestoreError ?: error, OperationType.GET, docRef.path)
                }
                throw error
            }
    }

    suspend fun saveProfile(
        userId: String,
        displayName: String,
        affectedHand: AffectedHand,
        primaryGoal: PrimaryGoal,
        clinicalContext: String?,
        recentSurgeryOrAcutePain: Boolean,
        voiceCoachEnabled: Boolean,
        intakeCompleted: Boolean
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            requireSignedInUid(userId)
            val safeName = displayName.trim().ifBlank { "Usuario" }.take(60)
            val safeContext = clinicalContext?.trim()?.take(300)?.ifBlank { null }
            val docRef = db.collection("users").document(userId)
            val existingSnap = docRef.get().await()

            if (existingSnap.exists()) {
                val updates = buildMap<String, Any> {
                    put("displayName", safeName)
                    put("affectedHand", affectedHand.id)
                    put("primaryGoal", primaryGoal.id)
                    if (safeContext != null) {
                        put("clinicalContext", safeContext)
                    } else if (existingSnap.contains("clinicalContext")) {
                        put("clinicalContext", "")
                    }
                    put("recentSurgeryOrAcutePain", recentSurgeryOrAcutePain)
                    put("voiceCoachEnabled", voiceCoachEnabled)
                    put("intakeCompleted", intakeCompleted)
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.update(updates).await()
            } else {
                val payload = mapOf(
                    "userId" to userId,
                    "displayName" to safeName,
                    "affectedHand" to affectedHand.id,
                    "primaryGoal" to primaryGoal.id,
                    "clinicalContext" to safeContext,
                    "recentSurgeryOrAcutePain" to recentSurgeryOrAcutePain,
                    "voiceCoachEnabled" to voiceCoachEnabled,
                    "intakeCompleted" to intakeCompleted,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateVoiceCoachPreference(userId: String, enabled: Boolean): Result<Unit> {
        val path = "users/$userId"
        return try {
            requireSignedInUid(userId)
            val docRef = db.collection("users").document(userId)
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "voiceCoachEnabled" to enabled,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateDailyReminderPreference(
        userId: String,
        enabled: Boolean,
        hour: Int,
        minute: Int
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            requireSignedInUid(userId)
            val docRef = db.collection("users").document(userId)
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "dailyReminderEnabled" to enabled,
                        "reminderHour" to hour.coerceIn(0, 23).toLong(),
                        "reminderMinute" to minute.coerceIn(0, 59).toLong(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeRoutine(userId: String): Flow<List<RoutineItem>> {
        val path = "users/$userId/routine"
        return db.collection("users").document(userId).collection("routine")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    RoutineItem::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).filter { it.active }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun seedInitialRoutineIfEmpty(userId: String, goal: PrimaryGoal): Result<Unit> {
        val basePath = "users/$userId/routine"
        return try {
            requireSignedInUid(userId)
            val collectionRef = db.collection("users").document(userId).collection("routine")
            val existing = collectionRef.get().await()
            if (existing.isEmpty) {
                val recommended = ExerciseCatalog.recommendedForGoal(goal)
                val batch = db.batch()
                recommended.forEach { exercise ->
                    val docRef = collectionRef.document(exercise.id)
                    val payload = mapOf(
                        "userId" to userId,
                        "exerciseId" to exercise.id,
                        "exerciseTitle" to exercise.title.take(100),
                        "categoryId" to exercise.category.id,
                        "targetSets" to exercise.suggestedSets.toLong(),
                        "targetReps" to exercise.suggestedReps.toLong(),
                        "assignedBy" to "Programa inicial",
                        "active" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    batch.set(docRef, payload)
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, basePath)
            Result.failure(e)
        }
    }

    suspend fun toggleExerciseInRoutine(
        userId: String,
        exercise: RehabExercise,
        currentlyActive: Boolean
    ): Result<Unit> {
        val docPath = "users/$userId/routine/${exercise.id}"
        return try {
            requireSignedInUid(userId)
            val docRef = db.collection("users").document(userId)
                .collection("routine").document(exercise.id)
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                docRef.update(
                    mapOf(
                        "active" to !currentlyActive,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val payload = mapOf(
                    "userId" to userId,
                    "exerciseId" to exercise.id,
                    "exerciseTitle" to exercise.title.take(100),
                    "categoryId" to exercise.category.id,
                    "targetSets" to exercise.suggestedSets.toLong(),
                    "targetReps" to exercise.suggestedReps.toLong(),
                    "assignedBy" to "Mi selección",
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    fun observeSessions(userId: String): Flow<List<ExerciseSession>> {
        val path = "users/$userId/sessions"
        return db.collection("users").document(userId).collection("sessions")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ExerciseSession::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun logSession(
        userId: String,
        exercise: RehabExercise,
        completedReps: Int,
        durationSeconds: Int,
        painLevel: Int,
        notes: String?,
        usedVoiceGuide: Boolean
    ): Result<Unit> {
        val basePath = "users/$userId/sessions"
        return try {
            requireSignedInUid(userId)
            val safeReps = completedReps.coerceIn(1, 200).toLong()
            val safeDuration = durationSeconds.coerceIn(5, 3600).toLong()
            val safePain = painLevel.coerceIn(0, 10).toLong()
            val safeNotes = notes?.trim()?.take(300)?.ifBlank { null }

            val docRef = db.collection("users").document(userId)
                .collection("sessions").document()
            val payload = mapOf(
                "userId" to userId,
                "exerciseId" to exercise.id,
                "exerciseTitle" to exercise.title.take(100),
                "categoryId" to exercise.category.id,
                "completedReps" to safeReps,
                "durationSeconds" to safeDuration,
                "painLevel" to safePain,
                "notes" to safeNotes,
                "usedVoiceGuide" to usedVoiceGuide,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, basePath)
            Result.failure(e)
        }
    }

    suspend fun deleteSession(userId: String, sessionId: String): Result<Unit> {
        val docPath = "users/$userId/sessions/$sessionId"
        return try {
            requireSignedInUid(userId)
            require(sessionId.isNotBlank()) { "El ID de sesión no puede estar vacío." }
            db.collection("users").document(userId)
                .collection("sessions").document(sessionId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
