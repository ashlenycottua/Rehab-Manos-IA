package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.catalog.ExerciseCatalog
import com.example.data.model.AffectedHand
import com.example.data.model.ExerciseCategory
import com.example.data.model.ExerciseSession
import com.example.data.model.PrimaryGoal
import com.example.data.model.RehabExercise
import com.example.data.model.RoutineItem
import com.example.data.model.UserRehabProfile
import com.example.ui.components.GoogleSignInActionButton
import com.example.ui.components.SafetyNoticeCard
import com.example.ui.components.categoryAccentContainer
import com.example.ui.components.categoryAccentContent
import com.example.ui.components.categoryIcon
import com.example.ui.components.exercisePhotoRes
import com.example.ui.viewmodel.UiState
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun HomeScreen(
    profile: UserRehabProfile?,
    routineItems: List<RoutineItem>,
    sessionsState: UiState<List<ExerciseSession>>,
    onOpenLibrary: () -> Unit,
    onSelectCategory: (ExerciseCategory) -> Unit,
    onOpenExercise: (String) -> Unit,
    onOpenProgress: () -> Unit,
    onOpenAssistant: () -> Unit,
    onOpenVision: () -> Unit,
    onOpenEbook: () -> Unit,
    onEditIntake: () -> Unit,
    isGuestMode: Boolean = false
) {
    val firstName = if (isGuestMode) {
        null
    } else {
        profile?.displayName
            ?.trim()
            ?.split(" ")
            ?.firstOrNull()
            ?.takeIf { it.isNotBlank() }
    }

    val timeGreeting = remember(firstName) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val prefix = when {
            hour in 5..11 -> "¡Buenos días"
            hour in 12..19 -> "¡Buenas tardes"
            else -> "¡Buenas noches"
        }
        if (firstName != null) "$prefix, $firstName!" else "$prefix!"
    }

    val sessions = (sessionsState as? UiState.Success)?.data.orEmpty()
    val latestSession = sessions.firstOrNull()
    val dateFormatter = remember {
        SimpleDateFormat("dd MMM · HH:mm", Locale("es", "ES"))
    }

    val routineExercises: List<RehabExercise> = routineItems
        .mapNotNull { ExerciseCatalog.findById(it.exerciseId) }
        .ifEmpty {
            ExerciseCatalog.recommendedForGoal(PrimaryGoal.fromId(profile?.primaryGoal))
        }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
                // Hero Welcome Banner
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_hero_hand_rehab),
                                contentDescription = "Banner Rehab Manos AI",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.08f),
                                                Color(0xFF063838).copy(alpha = 0.80f)
                                            )
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(18.dp)
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f)
                                ) {
                                    Text(
                                        text = "REHAB MANOS AI · PANEL PERSONAL",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = timeGreeting,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = "Tu espacio de ejercicios y seguimiento de mano.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.92f)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (isGuestMode) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Modo exploración libre",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Explora ejercicios y la guía completa sin crear cuenta.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                val handLabel = AffectedHand.fromId(profile?.affectedHand).label
                                val goalLabel = PrimaryGoal.fromId(profile?.primaryGoal).label
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "$handLabel · $goalLabel",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = when (sessionsState) {
                                            is UiState.Loading -> "Sincronizando sesiones..."
                                            is UiState.Error -> "Sin conexión con historial"
                                            is UiState.Success -> "${sessions.size} sesiones registradas"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                FilledTonalButton(
                                    onClick = onEditIntake,
                                    modifier = Modifier.testTag("home_edit_intake_button")
                                ) {
                                    Text("Ficha inicial")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Acceso rápido a la biblioteca de ejercicios y la guía educativa
        item {
            Column(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenLibrary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("home_library_quick_access_button"),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Ver biblioteca de ejercicios",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                FilledTonalButton(
                    onClick = onOpenEbook,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("home_guide_quick_access_button"),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Leer guía práctica de rehabilitación",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }

        if (isGuestMode) {
            item {
                Column(modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "¿Quieres guardar tu progreso en la nube?",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Puedes iniciar sesión con Google cuando quieras para sincronizar tu rutina y sesiones semanales.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                            )
                            GoogleSignInActionButton(label = "Conectar con Google")
                        }
                    }
                }
            }
        }

        if (profile?.recentSurgeryOrAcutePain == true) {
            item {
                Column(modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
                    SafetyNoticeCard(
                        title = "Seguimiento con cuidado especial",
                        body = "Indicaste cirugía reciente o dolor agudo. Realiza solo los movimientos aprobados por tu especialista y detén la sesión si surge dolor nuevo.",
                        isElevatedAlert = true
                    )
                }
            }
        }

        // Resumen breve de la última actividad desde Firestore
        item {
            Column(modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
                Card(
                    onClick = onOpenProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_activity_card"),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        ) {
                            if (sessionsState is UiState.Loading) {
                                Box(contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Timeline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(11.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Última actividad",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.height(2.dp))
                            when (sessionsState) {
                                is UiState.Loading -> {
                                    Text(
                                        text = "Consultando tu última sesión en Firestore...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                                is UiState.Error -> {
                                    Text(
                                        text = "No se pudo cargar tu actividad (${sessionsState.message}).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                is UiState.Success -> {
                                    if (latestSession == null) {
                                        Text(
                                            text = "Aún no registras sesiones. Completa un ejercicio guiado para ver tu resumen aquí.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                        )
                                    } else {
                                        val fecha = (latestSession.updatedAt ?: latestSession.createdAt ?: Timestamp.now()).toDate()
                                        Text(
                                            text = "${latestSession.exerciseTitle} · ${dateFormatter.format(fecha)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${latestSession.completedReps} rep · ${latestSession.durationSeconds} s · Molestia ${latestSession.painLevel}/10",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Ver progreso",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Mi rutina sugerida
        item {
            Column(modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mi rutina recomendada",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "${routineExercises.size} ejercicios",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(routineExercises, key = { it.id }) { exercise ->
                        Card(
                            onClick = { onOpenExercise(exercise.id) },
                            modifier = Modifier
                                .width(236.dp)
                                .testTag("home_routine_card_${exercise.id}"),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                        ) {
                            Column {
                                Image(
                                    painter = painterResource(id = exercisePhotoRes(exercise.motionType)),
                                    contentDescription = "Mano real realizando ${exercise.title}",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp),
                                    contentScale = ContentScale.Crop
                                )
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = categoryAccentContainer(exercise.category)
                                    ) {
                                        Text(
                                            text = exercise.category.shortLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = categoryAccentContent(exercise.category),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = exercise.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "${exercise.suggestedSets} series · ${exercise.suggestedReps} rep · ${exercise.suggestedMinutes} min",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Button(
                                        onClick = { onOpenExercise(exercise.id) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("Practicar")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Explorar ejercicios (4 categorías principales)
        item {
            Column(modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
                Text(
                    text = "Explorar ejercicios",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(10.dp))
                val categories = ExerciseCategory.entries
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    categories.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            pair.forEach { category ->
                                CategoryTile(
                                    category = category,
                                    onClick = { onSelectCategory(category) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Asistente inteligente + IA visual + Ebook
        item {
            Column(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Herramientas inteligentes y guía",
                    style = MaterialTheme.typography.titleLarge
                )

                // Asistente inteligente
                Card(
                    onClick = onOpenAssistant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_assistant_card"),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(50.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Asistente inteligente",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Consulta cómo utilizar los ejercicios y las funciones de tu plataforma.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Abrir asistente",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // IA Visual + Ebook row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        onClick = onOpenVision,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_vision_card"),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CameraAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(9.dp)
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "IA visual",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Cámara guía para revisar postura sin guardar videos.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Card(
                        onClick = onOpenEbook,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_ebook_card"),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_ebook_cover),
                                    contentDescription = "Portada Ebook",
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(MaterialTheme.shapes.small),
                                    contentScale = ContentScale.Crop
                                )
                                Icon(
                                    imageVector = Icons.Rounded.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Ebook guía",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Material educativo completo y descargable.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CategoryTile(
    category: ExerciseCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.testTag("category_tile_${category.id}"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = categoryAccentContainer(category),
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = categoryIcon(category),
                    contentDescription = category.title,
                    tint = categoryAccentContent(category),
                    modifier = Modifier.padding(10.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = category.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = category.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
