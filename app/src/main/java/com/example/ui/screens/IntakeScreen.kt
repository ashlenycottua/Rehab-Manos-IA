package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.AffectedHand
import com.example.data.model.PrimaryGoal
import com.example.data.model.UserRehabProfile
import com.example.ui.components.SafetyNoticeCard

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IntakeScreen(
    existingProfile: UserRehabProfile?,
    defaultUserName: String,
    isSaving: Boolean,
    allowBack: Boolean,
    onBack: () -> Unit,
    onSaveIntake: (
        displayName: String,
        affectedHand: AffectedHand,
        primaryGoal: PrimaryGoal,
        clinicalContext: String?,
        recentSurgeryOrAcutePain: Boolean,
        voiceCoachEnabled: Boolean
    ) -> Unit
) {
    if (allowBack) {
        BackHandler(onBack = onBack)
    }

    var displayName by remember(existingProfile) {
        mutableStateOf(
            existingProfile?.displayName?.takeIf { it.isNotBlank() }
                ?: defaultUserName.ifBlank { "Usuario" }
        )
    }
    var selectedHand by remember(existingProfile) {
        mutableStateOf(AffectedHand.fromId(existingProfile?.affectedHand))
    }
    var selectedGoal by remember(existingProfile) {
        mutableStateOf(PrimaryGoal.fromId(existingProfile?.primaryGoal))
    }
    var recentSurgeryOrPain by remember(existingProfile) {
        mutableStateOf(existingProfile?.recentSurgeryOrAcutePain ?: false)
    }
    var voiceEnabled by remember(existingProfile) {
        mutableStateOf(existingProfile?.voiceCoachEnabled ?: true)
    }
    var clinicalContext by remember(existingProfile) {
        mutableStateOf(existingProfile?.clinicalContext.orEmpty())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (existingProfile?.intakeCompleted == true) "Editar ficha inicial" else "Ficha inicial de cuidado",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    if (allowBack) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Volver"
                            )
                        }
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.widthIn(max = 600.dp)) {
                Text(
                    text = "Conozcamos tu objetivo y cuidados",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Evitamos recomendar una rutina única para todos. Completa estos datos sencillos para organizar tus ejercicios sugeridos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(18.dp))

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { if (it.length <= 60) displayName = it },
                    label = { Text("¿Cómo quieres que te llamemos?") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("intake_name_input"),
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "1. Mano en la que quieres enfocarte",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AffectedHand.entries.forEach { hand ->
                        FilterChip(
                            selected = selectedHand == hand,
                            onClick = { selectedHand = hand },
                            label = { Text(hand.label) }
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "2. Objetivo principal",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryGoal.entries.forEach { goal ->
                        val isSelected = selectedGoal == goal
                        Surface(
                            onClick = { selectedGoal = goal },
                            shape = MaterialTheme.shapes.medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = goal.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = goal.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Cirugía reciente, fractura o dolor agudo",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Actívalo si estás en posoperatorio o tienes síntomas recientes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = recentSurgeryOrPain,
                            onCheckedChange = { recentSurgeryOrPain = it }
                        )
                    }
                }

                if (recentSurgeryOrPain) {
                    Spacer(Modifier.height(12.dp))
                    SafetyNoticeCard(
                        title = "Requiere indicación profesional",
                        body = "Al haber cirugía reciente o dolor agudo, utiliza únicamente los ejercicios autorizados por tu médico o kinesiólogo y detén la práctica si aparece molestia.",
                        isElevatedAlert = true
                    )
                }

                Spacer(Modifier.height(16.dp))

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Guía por voz en español",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Escucha las instrucciones paso a paso mientras practicas.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = voiceEnabled,
                            onCheckedChange = { voiceEnabled = it }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = clinicalContext,
                    onValueChange = { if (it.length <= 300) clinicalContext = it },
                    label = { Text("Limitaciones o nota opcional (ej. rigidez matutina)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        onSaveIntake(
                            displayName.ifBlank { "Usuario" },
                            selectedHand,
                            selectedGoal,
                            clinicalContext.ifBlank { null },
                            recentSurgeryOrPain,
                            voiceEnabled
                        )
                    },
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("save_intake_button"),
                    shape = MaterialTheme.shapes.large
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Guardar ficha y preparar mi rutina",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))
            }
        }
    }
}
