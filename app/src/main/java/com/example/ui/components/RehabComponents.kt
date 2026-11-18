package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.model.ExerciseCategory
import com.example.data.model.HandMotionType
import com.example.data.model.RehabExercise
import com.example.data.repository.onGoogleSignInClicked
import com.example.ui.theme.AmberWarningBg
import com.example.ui.theme.AmberWarningText

fun exercisePhotoRes(motionType: HandMotionType): Int = when (motionType) {
    HandMotionType.WRIST_FLEX_EXT -> R.drawable.img_hand_wrist_mobility
    HandMotionType.GRIP_SQUEEZE -> R.drawable.img_hand_grip_strength
    HandMotionType.THUMB_OPPOSITION,
    HandMotionType.PINCH_CONTROL -> R.drawable.img_hand_thumb_pinch
    HandMotionType.FINGER_SPREAD -> R.drawable.img_hand_finger_spread
    HandMotionType.TENDON_GLIDE,
    HandMotionType.HOOK_FIST,
    HandMotionType.TABLETOP_FLEX -> R.drawable.img_hand_tendon_glide
}

fun categoryIcon(category: ExerciseCategory): ImageVector = when (category) {
    ExerciseCategory.MOVILIDAD -> Icons.Rounded.SelfImprovement
    ExerciseCategory.FORTALECIMIENTO -> Icons.Rounded.FitnessCenter
    ExerciseCategory.MOTRICIDAD_FINA -> Icons.Rounded.TouchApp
    ExerciseCategory.MOVILIDAD_DEDOS -> Icons.Rounded.PanTool
}

@Composable
fun categoryAccentContainer(category: ExerciseCategory): Color = when (category) {
    ExerciseCategory.MOVILIDAD -> MaterialTheme.colorScheme.primaryContainer
    ExerciseCategory.FORTALECIMIENTO -> MaterialTheme.colorScheme.secondaryContainer
    ExerciseCategory.MOTRICIDAD_FINA -> MaterialTheme.colorScheme.tertiaryContainer
    ExerciseCategory.MOVILIDAD_DEDOS -> MaterialTheme.colorScheme.surfaceVariant
}

@Composable
fun categoryAccentContent(category: ExerciseCategory): Color = when (category) {
    ExerciseCategory.MOVILIDAD -> MaterialTheme.colorScheme.onPrimaryContainer
    ExerciseCategory.FORTALECIMIENTO -> MaterialTheme.colorScheme.onSecondaryContainer
    ExerciseCategory.MOTRICIDAD_FINA -> MaterialTheme.colorScheme.onTertiaryContainer
    ExerciseCategory.MOVILIDAD_DEDOS -> MaterialTheme.colorScheme.primary
}

@Composable
fun SafetyNoticeCard(
    title: String,
    body: String,
    isElevatedAlert: Boolean = false,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isElevatedAlert) AmberWarningBg else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isElevatedAlert) AmberWarningText else MaterialTheme.colorScheme.onSurfaceVariant
    val icon = if (isElevatedAlert) Icons.Rounded.WarningAmber else Icons.Rounded.HealthAndSafety

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = bgColor,
        border = BorderStroke(
            width = 1.dp,
            color = if (isElevatedAlert) AmberWarningText.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
fun ExerciseSummaryCard(
    exercise: RehabExercise,
    inRoutine: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("exercise_card_${exercise.id}"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(72.dp)
            ) {
                Image(
                    painter = painterResource(id = exercisePhotoRes(exercise.motionType)),
                    contentDescription = "Demostración real: ${exercise.title}",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(22.dp),
                    shape = MaterialTheme.shapes.small,
                    color = categoryAccentContainer(exercise.category)
                ) {
                    Icon(
                        imageVector = categoryIcon(exercise.category),
                        contentDescription = null,
                        tint = categoryAccentContent(exercise.category),
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = categoryAccentContainer(exercise.category)
                    ) {
                        Text(
                            text = exercise.category.shortLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = categoryAccentContent(exercise.category),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = exercise.difficultyLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    if (inRoutine) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "En tu rutina",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = exercise.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = exercise.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${exercise.suggestedSets} series · ${exercise.suggestedReps} rep · ${exercise.suggestedMinutes} min",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = "Abrir ejercicio",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun GoogleSignInActionButton(
    label: String = "Continuar con Google",
    onAuthSuccess: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        if (errorMessage != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Text(
                    text = errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
        Button(
            onClick = {
                errorMessage = null
                isLoading = true
                onGoogleSignInClicked(
                    context = context,
                    credentialManager = credentialManager,
                    onAuthSuccess = {
                        isLoading = false
                        onAuthSuccess()
                    },
                    onAuthError = { msg ->
                        isLoading = false
                        errorMessage = msg
                    },
                    scope = scope,
                    onAuthCancelled = {
                        isLoading = false
                    }
                )
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("google_sign_in_inline_button"),
            shape = MaterialTheme.shapes.large
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text("Conectando con Google...")
            } else {
                Text(label)
            }
        }
    }
}
