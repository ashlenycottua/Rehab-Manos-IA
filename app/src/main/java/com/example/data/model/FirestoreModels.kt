package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

enum class AffectedHand(val id: String, val label: String) {
    DERECHA("DERECHA", "Mano derecha"),
    IZQUIERDA("IZQUIERDA", "Mano izquierda"),
    AMBAS("AMBAS", "Ambas manos");

    companion object {
        fun fromId(raw: String?): AffectedHand =
            entries.firstOrNull { it.id == raw } ?: DERECHA
    }
}

enum class PrimaryGoal(val id: String, val label: String, val description: String) {
    MOVILIDAD("MOVILIDAD", "Recuperar movilidad", "Reducir rigidez en muñeca y articulaciones"),
    FUERZA("FUERZA", "Ganar fuerza suave", "Mejorar agarre y resistencia funcional"),
    MOTRICIDAD_FINA("MOTRICIDAD_FINA", "Destreza y precisión", "Coordinación de dedos para tareas diarias"),
    RECUPERACION_SUAVE("RECUPERACION_SUAVE", "Movilidad muy suave", "Inicio gradual sin sobrecarga");

    companion object {
        fun fromId(raw: String?): PrimaryGoal =
            entries.firstOrNull { it.id == raw } ?: MOVILIDAD
    }
}

@Keep
@IgnoreExtraProperties
data class UserRehabProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val affectedHand: String = AffectedHand.DERECHA.id,
    val primaryGoal: String = PrimaryGoal.MOVILIDAD.id,
    val clinicalContext: String? = null,
    val recentSurgeryOrAcutePain: Boolean = false,
    val voiceCoachEnabled: Boolean = true,
    val dailyReminderEnabled: Boolean = false,
    val reminderHour: Long = 9L,
    val reminderMinute: Long = 0L,
    val intakeCompleted: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RoutineItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val exerciseId: String = "",
    val exerciseTitle: String = "",
    val categoryId: String = ExerciseCategory.MOVILIDAD.id,
    val targetSets: Long = 3L,
    val targetReps: Long = 10L,
    val assignedBy: String = "Programa inicial",
    val active: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ExerciseSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val exerciseId: String = "",
    val exerciseTitle: String = "",
    val categoryId: String = ExerciseCategory.MOVILIDAD.id,
    val completedReps: Long = 0L,
    val durationSeconds: Long = 0L,
    val painLevel: Long = 0L,
    val notes: String? = null,
    val usedVoiceGuide: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreMotionPhase(
    val label: String = "",
    val voiceCue: String = "",
    val durationSeconds: Long = 3L,
    val targetProgress: Double = 0.0
)

@Keep
@IgnoreExtraProperties
data class FirestoreExercise(
    @DocumentId val id: String = "",
    val exerciseId: String = "",
    val title: String = "",
    val categoryId: String = ExerciseCategory.MOVILIDAD.id,
    val motionType: String = HandMotionType.WRIST_FLEX_EXT.name,
    val difficultyLabel: String = "Suave",
    val suggestedSets: Long = 3L,
    val suggestedReps: Long = 10L,
    val suggestedMinutes: Long = 4L,
    val sortOrder: Long = 0L,
    val summary: String = "",
    val postureInstructions: List<String> = emptyList(),
    val phases: List<FirestoreMotionPhase> = emptyList(),
    val avoidWarnings: List<String> = emptyList(),
    val recommendedForGoals: List<String> = emptyList(),
    val visionTargetPoseName: String = "",
    val published: Boolean = true,
    val createdBy: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toDomain(): RehabExercise {
        val resolvedId = exerciseId.ifBlank { id }
        val domainPhases = phases.map { phase ->
            MotionPhase(
                label = phase.label.ifBlank { "Movimiento" },
                voiceCue = phase.voiceCue.ifBlank { "Realiza el movimiento con control" },
                durationSeconds = phase.durationSeconds.toInt().coerceIn(1, 30),
                targetProgress = phase.targetProgress.toFloat().coerceIn(0f, 1f)
            )
        }.ifEmpty {
            listOf(
                MotionPhase("Iniciar", "Realiza el movimiento suavemente", 3, 1f),
                MotionPhase("Relajar", "Vuelve despacio a la posición inicial", 3, 0f)
            )
        }

        return RehabExercise(
            id = resolvedId,
            title = title,
            category = ExerciseCategory.fromId(categoryId),
            motionType = HandMotionType.fromId(motionType),
            difficultyLabel = difficultyLabel.ifBlank { "Suave" },
            suggestedSets = suggestedSets.toInt().coerceIn(1, 10),
            suggestedReps = suggestedReps.toInt().coerceIn(1, 50),
            suggestedMinutes = suggestedMinutes.toInt().coerceIn(1, 60),
            summary = summary,
            postureInstructions = postureInstructions,
            phases = domainPhases,
            avoidWarnings = avoidWarnings,
            recommendedForGoals = recommendedForGoals.map { PrimaryGoal.fromId(it) }.ifEmpty { listOf(PrimaryGoal.MOVILIDAD) },
            visionTargetPoseName = visionTargetPoseName.ifBlank { title }
        )
    }
}
