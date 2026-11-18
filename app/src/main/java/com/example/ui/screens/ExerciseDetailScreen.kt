package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.VoiceCoachHelper
import com.example.data.model.RehabExercise
import com.example.ui.components.HandExerciseCanvas
import com.example.ui.components.SafetyNoticeCard
import com.example.ui.components.categoryAccentContainer
import com.example.ui.components.categoryAccentContent
import com.example.ui.components.exercisePhotoRes
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    exercise: RehabExercise,
    inRoutine: Boolean,
    voiceEnabledDefault: Boolean,
    isSavingSession: Boolean,
    onBack: () -> Unit,
    onToggleRoutine: () -> Unit,
    onOpenVisionForExercise: (String) -> Unit,
    onFinishAndLogSession: (
        completedReps: Int,
        durationSeconds: Int,
        painLevel: Int,
        notes: String?,
        usedVoiceGuide: Boolean
    ) -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val voiceCoach = remember { VoiceCoachHelper(context) }
    DisposableEffect(Unit) {
        onDispose { voiceCoach.release() }
    }

    var voiceEnabled by remember(voiceEnabledDefault) { mutableStateOf(voiceEnabledDefault) }
    var isSessionActive by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var currentRep by remember { mutableIntStateOf(1) }
    var phaseIndex by remember { mutableIntStateOf(0) }
    var phaseSecondsLeft by remember { mutableIntStateOf(exercise.phases.first().durationSeconds) }

    var showFinishDialog by remember { mutableStateOf(false) }
    var painSlider by remember { mutableIntStateOf(1) }
    var sessionNotes by remember { mutableStateOf("") }
    var showRealHandPhoto by remember { mutableStateOf(true) }

    // Demo preview loop when idle, or active guided timer when session running
    LaunchedEffect(isSessionActive, isPaused, exercise.id) {
        if (isSessionActive && !isPaused) {
            val currentPhase = exercise.phases[phaseIndex]
            if (voiceEnabled) {
                voiceCoach.speak("Repetición $currentRep. ${currentPhase.voiceCue}")
            }
            while (isSessionActive && !isPaused) {
                delay(1000L)
                elapsedSeconds += 1
                if (phaseSecondsLeft > 1) {
                    phaseSecondsLeft -= 1
                } else {
                    val nextPhaseIdx = (phaseIndex + 1) % exercise.phases.size
                    if (nextPhaseIdx == 0) {
                        if (currentRep >= exercise.suggestedReps) {
                            isSessionActive = false
                            isPaused = false
                            if (voiceEnabled) {
                                voiceCoach.speak("¡Muy bien! Has completado las repeticiones sugeridas.")
                            }
                            showFinishDialog = true
                            break
                        } else {
                            currentRep += 1
                        }
                    }
                    phaseIndex = nextPhaseIdx
                    val nextPhase = exercise.phases[nextPhaseIdx]
                    phaseSecondsLeft = nextPhase.durationSeconds
                    if (voiceEnabled) {
                        val prefix = if (nextPhaseIdx == 0) "Repetición $currentRep. " else ""
                        voiceCoach.speak("$prefix${nextPhase.voiceCue}")
                    }
                }
            }
        } else if (!isSessionActive) {
            // Gentle visual demonstration cycle while reading the card
            while (true) {
                delay(2400L)
                phaseIndex = (phaseIndex + 1) % exercise.phases.size
            }
        }
    }

    val activePhase = exercise.phases[phaseIndex]
    val animatedProgress by animateFloatAsState(
        targetValue = activePhase.targetProgress,
        animationSpec = tween(durationMillis = 900),
        label = "hand_motion_progress"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = exercise.category.shortLabel,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        voiceCoach.stop()
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            voiceEnabled = !voiceEnabled
                            if (!voiceEnabled) {
                                voiceCoach.stop()
                            } else {
                                voiceCoach.speak("Guía por voz activada. ${exercise.title}.")
                            }
                        },
                        modifier = Modifier.testTag("voice_coach_toggle")
                    ) {
                        Icon(
                            imageVector = if (voiceEnabled) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                            contentDescription = if (voiceEnabled) "Silenciar voz" else "Activar voz",
                            tint = if (voiceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.widthIn(max = 680.dp)) {
                // Header info
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = categoryAccentContainer(exercise.category)
                    ) {
                        Text(
                            text = exercise.category.title,
                            style = MaterialTheme.typography.labelMedium,
                            color = categoryAccentContent(exercise.category),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = exercise.difficultyLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    text = exercise.title,
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = exercise.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(16.dp))

                // Level 1 Animated Hand Demonstration Card
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isSessionActive) "Sesión en curso" else "Demostración visual",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (isSessionActive) {
                                    "Rep. $currentRep / ${exercise.suggestedReps} · ${elapsedSeconds}s"
                                } else {
                                    "${exercise.suggestedSets} series × ${exercise.suggestedReps} rep"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = showRealHandPhoto,
                                onClick = { showRealHandPhoto = true },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                modifier = Modifier.testTag("demo_mode_real_hand")
                            ) {
                                Text("Mano real")
                            }
                            SegmentedButton(
                                selected = !showRealHandPhoto,
                                onClick = { showRealHandPhoto = false },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                modifier = Modifier.testTag("demo_mode_animated")
                            ) {
                                Text("Guía animada")
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(230.dp)
                                .clip(MaterialTheme.shapes.large),
                            contentAlignment = Alignment.Center
                        ) {
                            if (showRealHandPhoto) {
                                Image(
                                    painter = painterResource(id = exercisePhotoRes(exercise.motionType)),
                                    contentDescription = "Mano real realizando ${exercise.title}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = 0.48f)
                                                )
                                            )
                                        )
                                )
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = Color.Black.copy(alpha = 0.58f),
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "Referencia real: ${exercise.visionTargetPoseName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            } else {
                                HandExerciseCanvas(
                                    motionType = exercise.motionType,
                                    progress = animatedProgress
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Current Phase Pill
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = activePhase.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = activePhase.voiceCue,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.88f)
                                    )
                                }
                                if (isSessionActive) {
                                    Text(
                                        text = "${phaseSecondsLeft}s",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (isSessionActive) {
                            Spacer(Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { currentRep.toFloat() / exercise.suggestedReps.toFloat() },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        // Session Action Buttons
                        if (!isSessionActive) {
                            Button(
                                onClick = {
                                    elapsedSeconds = 0
                                    currentRep = 1
                                    phaseIndex = 0
                                    phaseSecondsLeft = exercise.phases.first().durationSeconds
                                    isPaused = false
                                    isSessionActive = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("start_session_button"),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Iniciar sesión guiada", style = MaterialTheme.typography.titleMedium)
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        isPaused = !isPaused
                                        if (isPaused) voiceCoach.stop()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp),
                                    shape = MaterialTheme.shapes.large
                                ) {
                                    Icon(
                                        imageVector = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                        contentDescription = null
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (isPaused) "Reanudar" else "Pausar")
                                }
                                Button(
                                    onClick = {
                                        isSessionActive = false
                                        isPaused = false
                                        voiceCoach.stop()
                                        showFinishDialog = true
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("finish_session_button"),
                                    shape = MaterialTheme.shapes.large,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    Icon(Icons.Rounded.Stop, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Terminar")
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Quick action row: Add to Routine + Hear full instructions + IA Visual
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = onToggleRoutine,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("toggle_routine_button")
                    ) {
                        Icon(
                            imageVector = if (inRoutine) Icons.Rounded.BookmarkAdded else Icons.Rounded.BookmarkAdd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (inRoutine) "En Mi rutina" else "Sumar a rutina")
                    }

                    OutlinedButton(
                        onClick = {
                            voiceCoach.stop()
                            onOpenVisionForExercise(exercise.id)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("open_vision_from_exercise_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Revisar postura")
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Posture instructions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Indicaciones de postura",
                        style = MaterialTheme.typography.titleLarge
                    )
                    TextButton(
                        onClick = {
                            voiceEnabled = true
                            val fullSpeech = buildString {
                                append(exercise.title)
                                append(". ")
                                exercise.postureInstructions.forEach { append(it).append(" ") }
                            }
                            voiceCoach.speak(fullSpeech)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Escuchar")
                    }
                }
                Spacer(Modifier.height(8.dp))

                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        exercise.postureInstructions.forEachIndexed { idx, step ->
                            Row(verticalAlignment = Alignment.Top) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${idx + 1}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = step,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Avoid warnings
                SafetyNoticeCard(
                    title = "Qué evitar y cuándo detenerte",
                    body = exercise.avoidWarnings.joinToString(separator = "\n• ", prefix = "• "),
                    isElevatedAlert = true
                )

                Spacer(Modifier.height(28.dp))
            }
        }
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSavingSession) showFinishDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Registrar sesión") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Completaste $currentRep repeticiones en ${maxOf(elapsedSeconds, 5)} segundos.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "¿Sentiste molestia o dolor? ($painSlider / 10)",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Slider(
                        value = painSlider.toFloat(),
                        onValueChange = { painSlider = it.roundToInt().coerceIn(0, 10) },
                        valueRange = 0f..10f,
                        steps = 9
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0 · Sin molestia", style = MaterialTheme.typography.labelSmall)
                        Text("10 · Dolor intenso", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedTextField(
                        value = sessionNotes,
                        onValueChange = { if (it.length <= 300) sessionNotes = it },
                        label = { Text("Nota opcional (cómo sentiste la mano)") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onFinishAndLogSession(
                            currentRep.coerceAtLeast(1),
                            maxOf(elapsedSeconds, 5),
                            painSlider,
                            sessionNotes.ifBlank { null },
                            voiceEnabled
                        )
                        showFinishDialog = false
                    },
                    enabled = !isSavingSession,
                    modifier = Modifier.testTag("confirm_log_session_button")
                ) {
                    Text(if (isSavingSession) "Guardando..." else "Guardar progreso")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showFinishDialog = false },
                    enabled = !isSavingSession
                ) {
                    Text("Descartar")
                }
            }
        )
    }
}
