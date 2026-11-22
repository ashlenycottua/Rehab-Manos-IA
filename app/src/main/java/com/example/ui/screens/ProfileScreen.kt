package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.notifications.DailyReminderNotificationHelper
import com.example.core.notifications.ReminderConfig
import com.example.core.notifications.ReminderPreferences
import com.example.data.model.AffectedHand
import com.example.data.model.PrimaryGoal
import com.example.data.model.UserRehabProfile
import com.example.data.repository.signOutUser
import com.example.ui.components.GoogleGLogo
import com.example.ui.components.GoogleSignInActionButton
import com.example.ui.components.SafetyNoticeCard

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    userEmail: String?,
    profile: UserRehabProfile?,
    reminderConfig: ReminderConfig,
    onToggleVoiceCoach: (Boolean) -> Unit,
    onUpdateDailyReminder: (enabled: Boolean, hour: Int, minute: Int) -> Unit,
    onTestReminderNotification: () -> Unit,
    onEditIntake: () -> Unit,
    onOpenEbook: () -> Unit,
    onOpenAssistant: () -> Unit,
    onOpenVision: () -> Unit,
    isGuestMode: Boolean = false,
    guestVoiceEnabled: Boolean = true
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var notificationsPermitted by remember {
        mutableStateOf(DailyReminderNotificationHelper.areNotificationsEnabled(context))
    }
    var permissionDeniedFeedback by remember { mutableStateOf(false) }
    var pendingEnableReminder by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var pendingTestNotification by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsPermitted = DailyReminderNotificationHelper.areNotificationsEnabled(context)
                if (notificationsPermitted) {
                    permissionDeniedFeedback = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsPermitted = granted && DailyReminderNotificationHelper.areNotificationsEnabled(context)
        if (granted) {
            permissionDeniedFeedback = false
            pendingEnableReminder?.let { (hour, minute) ->
                onUpdateDailyReminder(true, hour, minute)
            }
            if (pendingTestNotification) {
                onTestReminderNotification()
            }
        } else {
            permissionDeniedFeedback = true
        }
        pendingEnableReminder = null
        pendingTestNotification = false
    }

    fun requestEnableOrSchedule(hour: Int, minute: Int) {
        val hasRuntimePerm = DailyReminderNotificationHelper.hasRuntimeNotificationPermission(context)
        if (hasRuntimePerm) {
            notificationsPermitted = DailyReminderNotificationHelper.areNotificationsEnabled(context)
            if (notificationsPermitted) {
                permissionDeniedFeedback = false
                onUpdateDailyReminder(true, hour, minute)
            } else {
                permissionDeniedFeedback = true
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pendingEnableReminder = hour to minute
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun requestTestNotification() {
        val hasRuntimePerm = DailyReminderNotificationHelper.hasRuntimeNotificationPermission(context)
        if (hasRuntimePerm) {
            notificationsPermitted = DailyReminderNotificationHelper.areNotificationsEnabled(context)
            if (notificationsPermitted) {
                permissionDeniedFeedback = false
                onTestReminderNotification()
            } else {
                permissionDeniedFeedback = true
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pendingTestNotification = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (showTimePickerDialog) {
        val timePickerState = rememberTimePickerState(
            initialHour = reminderConfig.hour,
            initialMinute = reminderConfig.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            title = {
                Text(
                    text = "Hora del recordatorio diario",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Elige a qué hora quieres recibir el aviso diario para tu rutina.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showTimePickerDialog = false
                        requestEnableOrSchedule(timePickerState.hour, timePickerState.minute)
                    },
                    modifier = Modifier.testTag("confirm_reminder_time_button")
                ) {
                    Text("Guardar hora")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                Text(
                    text = "Mi perfil",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Datos personales, ficha inicial y preferencias",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // User Profile Card
        item {
            Card(
                modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isGuestMode) "Modo exploración" else profile?.displayName?.ifBlank { "Usuario" } ?: "Usuario",
                                style = MaterialTheme.typography.titleLarge
                            )
                            if (isGuestMode) {
                                Text(
                                    text = "Acceso abierto sin cuenta iniciada",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else if (!userEmail.isNullOrBlank()) {
                                Text(
                                    text = userEmail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    if (isGuestMode) {
                        Text(
                            text = "Puedes recorrer todos los ejercicios, usar la guía por voz y leer el Ebook educativo completo. Conecta tu cuenta de Google cuando quieras guardar tu rutina y progreso en la nube.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(14.dp))
                        GoogleSignInActionButton(label = "Iniciar sesión con Google")
                    } else {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                GoogleGLogo(modifier = Modifier.size(20.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Sesión iniciada con Google",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Tu rutina, sesiones y recordatorios están respaldados en Firebase.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }

                        val handLabel = AffectedHand.fromId(profile?.affectedHand).label
                        val goalLabel = PrimaryGoal.fromId(profile?.primaryGoal).label
                        Text(
                            text = "Mano principal: $handLabel",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Objetivo: $goalLabel",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (!profile?.clinicalContext.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Notas: ${profile?.clinicalContext}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = onEditIntake,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("profile_edit_intake_button"),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Editar ficha inicial")
                        }
                    }
                }
            }
        }

        // Daily Local Notification Reminder Setting
        item {
            val formattedTime = ReminderPreferences.formatTime(reminderConfig.hour, reminderConfig.minute)
            Card(
                modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    width = if (reminderConfig.enabled) 1.5.dp else 1.dp,
                    color = if (reminderConfig.enabled) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                    }
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (reminderConfig.enabled) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (reminderConfig.enabled) {
                                    Icons.Rounded.NotificationsActive
                                } else {
                                    Icons.Outlined.Notifications
                                },
                                contentDescription = "Recordatorio diario",
                                tint = if (reminderConfig.enabled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(9.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Recordatorio diario de ejercicios",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = if (reminderConfig.enabled) {
                                    "Activo todos los días a las $formattedTime h"
                                } else {
                                    "Recibe una notificación diaria en este dispositivo"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (reminderConfig.enabled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                        Switch(
                            checked = reminderConfig.enabled,
                            onCheckedChange = { shouldEnable ->
                                if (shouldEnable) {
                                    requestEnableOrSchedule(reminderConfig.hour, reminderConfig.minute)
                                } else {
                                    onUpdateDailyReminder(false, reminderConfig.hour, reminderConfig.minute)
                                }
                            },
                            modifier = Modifier.testTag("profile_daily_reminder_switch")
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Surface(
                        onClick = { showTimePickerDialog = true },
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_reminder_time_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Hora programada",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$formattedTime h",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Text(
                                text = "Cambiar",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "Horarios rápidos",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(9 to 0, 13 to 0, 19 to 0).forEach { (presetHour, presetMinute) ->
                            val isSelected = reminderConfig.hour == presetHour &&
                                reminderConfig.minute == presetMinute &&
                                reminderConfig.enabled
                            FilterChip(
                                selected = isSelected,
                                onClick = { requestEnableOrSchedule(presetHour, presetMinute) },
                                label = {
                                    Text("${ReminderPreferences.formatTime(presetHour, presetMinute)} h")
                                }
                            )
                        }
                    }

                    AnimatedVisibility(visible = reminderConfig.enabled) {
                        Column {
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { requestTestNotification() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("profile_test_notification_button"),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsActive,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Probar notificación ahora")
                            }
                        }
                    }

                    AnimatedVisibility(visible = permissionDeniedFeedback || !notificationsPermitted) {
                        if (permissionDeniedFeedback || (reminderConfig.enabled && !notificationsPermitted)) {
                            Spacer(Modifier.height(10.dp))
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Permiso de notificaciones desactivado",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Habilita las notificaciones del sistema para recibir el aviso diario en tu teléfono.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = {
                                            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                                }
                                            } else {
                                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                    data = Uri.fromParts("package", context.packageName, null)
                                                }
                                            }
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier.height(48.dp)
                                    ) {
                                        Text("Abrir ajustes de notificaciones")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Voice Coaching Setting
        item {
            Card(
                modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Voz explicativa en español",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Activar por defecto al iniciar ejercicios",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = if (isGuestMode) guestVoiceEnabled else (profile?.voiceCoachEnabled ?: true),
                        onCheckedChange = onToggleVoiceCoach
                    )
                }
            }
        }

        // Shortcuts to Ebook, Assistant, Vision
        item {
            Column(
                modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Secciones de la plataforma",
                    style = MaterialTheme.typography.titleMedium
                )
                ShortcutRowCard(
                    icon = Icons.Rounded.MenuBook,
                    title = "Ebook educativo",
                    subtitle = "Guía práctica de rehabilitación de mano",
                    onClick = onOpenEbook
                )
                ShortcutRowCard(
                    icon = Icons.Rounded.AutoAwesome,
                    title = "Asistente inteligente",
                    subtitle = "Preguntas sobre ejercicios y uso de la app",
                    onClick = onOpenAssistant
                )
                ShortcutRowCard(
                    icon = Icons.Rounded.CameraAlt,
                    title = "IA visual (Cámara guía)",
                    subtitle = "Revisión de postura en vivo sin guardar videos",
                    onClick = onOpenVision
                )
            }
        }

        // Safety & Regulatory Transparency
        item {
            Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                SafetyNoticeCard(
                    title = "Aviso de seguridad y uso educativo",
                    body = "Rehab Manos AI brinda contenido educativo y de seguimiento personal. No sustituye diagnóstico, prescripción ni evaluación presencial por profesionales habilitados. Ante dolor agudo, hormigueo o mareos, detén la práctica y consulta a tu especialista."
                )
            }
        }

        // Sign Out (only when authenticated)
        if (!isGuestMode) {
            item {
                var isSigningOut by remember { mutableStateOf(false) }
                Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            isSigningOut = true
                            signOutUser(
                                credentialManager = credentialManager,
                                scope = scope,
                                onSignOutComplete = { isSigningOut = false }
                            )
                        },
                        enabled = !isSigningOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("sign_out_button"),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isSigningOut) "Cerrando sesión..." else "Cerrar sesión")
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ShortcutRowCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(9.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
