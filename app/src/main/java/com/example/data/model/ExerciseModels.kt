package com.example.data.model

enum class ExerciseCategory(
    val id: String,
    val title: String,
    val subtitle: String,
    val shortLabel: String
) {
    MOVILIDAD(
        id = "MOVILIDAD",
        title = "Movilidad articular",
        subtitle = "Flexión, extensión y amplitud de muñeca y mano",
        shortLabel = "Movilidad"
    ),
    FORTALECIMIENTO(
        id = "FORTALECIMIENTO",
        title = "Fortalecimiento",
        subtitle = "Fuerza controlada de agarre y pinza",
        shortLabel = "Fuerza"
    ),
    MOTRICIDAD_FINA(
        id = "MOTRICIDAD_FINA",
        title = "Motricidad fina",
        subtitle = "Coordinación, precisión y destreza cotidiana",
        shortLabel = "Motricidad"
    ),
    MOVILIDAD_DEDOS(
        id = "MOVILIDAD_DEDOS",
        title = "Movilidad de dedos",
        subtitle = "Movimientos guiados de tendones y falanges",
        shortLabel = "Dedos"
    );

    companion object {
        fun fromId(raw: String?): ExerciseCategory =
            entries.firstOrNull { it.id == raw } ?: MOVILIDAD
    }
}

enum class HandMotionType {
    TENDON_GLIDE,
    WRIST_FLEX_EXT,
    FINGER_SPREAD,
    THUMB_OPPOSITION,
    GRIP_SQUEEZE,
    PINCH_CONTROL,
    TABLETOP_FLEX,
    HOOK_FIST;

    companion object {
        fun fromId(raw: String?): HandMotionType =
            entries.firstOrNull { it.name == raw } ?: WRIST_FLEX_EXT
    }
}

data class MotionPhase(
    val label: String,
    val voiceCue: String,
    val durationSeconds: Int,
    val targetProgress: Float // 0f = relaxed/open, 1f = peak flexion/pose
)

data class RehabExercise(
    val id: String,
    val title: String,
    val category: ExerciseCategory,
    val motionType: HandMotionType,
    val difficultyLabel: String,
    val suggestedSets: Int,
    val suggestedReps: Int,
    val suggestedMinutes: Int,
    val summary: String,
    val postureInstructions: List<String>,
    val phases: List<MotionPhase>,
    val avoidWarnings: List<String>,
    val recommendedForGoals: List<PrimaryGoal>,
    val visionTargetPoseName: String
)

data class EbookChapter(
    val id: String,
    val number: Int,
    val title: String,
    val subtitle: String,
    val readMinutes: Int,
    val keyTakeaways: List<String>,
    val bodyParagraphs: List<String>,
    val safetyNote: String? = null
)

data class HandVisionFeedback(
    val postureStatus: String,
    val observedPosition: String,
    val alignmentTip: String,
    val safetyReminder: String
)
