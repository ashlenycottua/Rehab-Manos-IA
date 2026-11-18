package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ExerciseCategory
import com.example.data.model.ExerciseSession
import com.example.ui.components.GoogleSignInActionButton
import com.example.ui.components.SafetyNoticeCard
import com.example.ui.viewmodel.UiState
import com.google.firebase.Timestamp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class WeeklySessionBucket(
    val weekStartMillis: Long,
    val label: String,
    val sessionCount: Int,
    val totalReps: Long
)

@Composable
fun ProgressScreen(
    sessionsState: UiState<List<ExerciseSession>>,
    onExploreExercises: () -> Unit,
    onDeleteSession: (String) -> Unit,
    isGuestMode: Boolean = false
) {
    val dateFormatter = remember {
        SimpleDateFormat("dd MMM · HH:mm", Locale("es", "ES"))
    }

    when (sessionsState) {
        is UiState.Loading -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text("Cargando tu actividad...", style = MaterialTheme.typography.bodyMedium)
            }
        }
        is UiState.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SafetyNoticeCard(
                    title = "No se pudo cargar tu historial",
                    body = sessionsState.message,
                    isElevatedAlert = true
                )
            }
        }
        is UiState.Success -> {
            val sessions = sessionsState.data
            val weeklyBuckets = remember(sessions) {
                buildWeeklyBuckets(sessions, weeksCount = 6)
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Column(modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
                        Text(
                            text = "Mi Progreso",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "Sesiones completadas por semana, repeticiones y evolución de comodidad",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isGuestMode) {
                    item {
                        Card(
                            modifier = Modifier
                                .widthIn(max = 720.dp)
                                .fillMaxWidth(),
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
                                    text = "Progreso en modo exploración",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Las sesiones que completes aquí se muestran de forma temporal. Inicia sesión con Google para guardarlas en tu cuenta.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                )
                                GoogleSignInActionButton(label = "Iniciar sesión con Google")
                            }
                        }
                    }
                }

                if (sessions.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .widthIn(max = 600.dp)
                                .fillMaxWidth(),
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.large,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Timeline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                                Spacer(Modifier.height(14.dp))
                                Text(
                                    text = "Aún no tienes sesiones registradas",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Completa tu primer ejercicio guiado para ver aquí tu gráfico semanal, repeticiones y comodidad.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(18.dp))
                                Button(
                                    onClick = onExploreExercises,
                                    modifier = Modifier.testTag("empty_progress_explore_button"),
                                    shape = MaterialTheme.shapes.large
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Ir a ejercicios")
                                }
                            }
                        }
                    }
                } else {
                    val totalReps = sessions.sumOf { it.completedReps }
                    val avgPain = sessions.map { it.painLevel }.average()
                    val currentWeekSessions = weeklyBuckets.lastOrNull()?.sessionCount ?: 0

                    item {
                        Row(
                            modifier = Modifier
                                .widthIn(max = 720.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            KpiCard(
                                label = "Esta semana",
                                value = "$currentWeekSessions",
                                modifier = Modifier.weight(1f)
                            )
                            KpiCard(
                                label = "Sesiones",
                                value = "${sessions.size}",
                                modifier = Modifier.weight(1f)
                            )
                            KpiCard(
                                label = "Repeticiones",
                                value = "$totalReps",
                                modifier = Modifier.weight(1f)
                            )
                            KpiCard(
                                label = "Molestia",
                                value = String.format(Locale.US, "%.1f/10", avgPain),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Gráfico semanal de sesiones completadas (Vico Compose M3)
                    item {
                        Card(
                            modifier = Modifier
                                .widthIn(max = 720.dp)
                                .fillMaxWidth()
                                .testTag("weekly_sessions_chart_card"),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = MaterialTheme.shapes.small,
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.BarChart,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Sesiones completadas por semana",
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = "Últimas ${weeklyBuckets.size} semanas",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = "$currentWeekSessions esta sem.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(14.dp))

                                WeeklySessionsBarChart(
                                    buckets = weeklyBuckets,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(210.dp)
                                        .testTag("weekly_sessions_vico_chart")
                                )

                                Spacer(Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    weeklyBuckets.forEach { bucket ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = "${bucket.sessionCount}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (bucket.sessionCount > 0) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                            Text(
                                                text = bucket.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Trend chart across up to 8 most recent sessions (chronological left-to-right)
                    val chartSessions = sessions.take(8).reversed()
                    item {
                        Card(
                            modifier = Modifier
                                .widthIn(max = 720.dp)
                                .fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Tendencia de molestia (0–10)",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Últimas ${chartSessions.size} sesiones registradas",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(12.dp))
                                PainTrendChart(
                                    painValues = chartSessions.map { it.painLevel.toInt() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                )
                            }
                        }
                    }

                    item {
                        Column(modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
                            Text(
                                text = "Historial reciente",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }

                    items(sessions, key = { it.id.ifBlank { "${it.exerciseId}_${it.createdAt?.seconds}" } }) { session ->
                        val cat = ExerciseCategory.fromId(session.categoryId)
                        val displayDate = (session.updatedAt ?: session.createdAt ?: Timestamp.now()).toDate()

                        Card(
                            modifier = Modifier
                                .widthIn(max = 720.dp)
                                .fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = cat.shortLabel.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = dateFormatter.format(displayDate),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (session.usedVoiceGuide) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                                contentDescription = "Con guía por voz",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = session.exerciseTitle,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "${session.completedReps} rep · ${session.durationSeconds} s · Molestia ${session.painLevel}/10",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (!session.notes.isNullOrBlank()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "\"${session.notes}\"",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                if (session.id.isNotBlank()) {
                                    IconButton(onClick = { onDeleteSession(session.id) }) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Eliminar registro",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklySessionsBarChart(
    buckets: List<WeeklySessionBucket>,
    modifier: Modifier = Modifier
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val counts = remember(buckets) { buckets.map { it.sessionCount } }

    LaunchedEffect(counts) {
        modelProducer.runTransaction {
            columnSeries {
                series(counts)
            }
        }
    }

    val barColor = MaterialTheme.colorScheme.primary
    val bottomAxisFormatter = remember(buckets) {
        CartesianValueFormatter { _, value, _ ->
            val index = value.toInt()
            buckets.getOrNull(index)?.label ?: " "
        }
    }
    val startAxisFormatter = remember {
        CartesianValueFormatter { _, value, _ ->
            value.toInt().toString()
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(
                columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                    rememberLineComponent(
                        fill = fill(barColor),
                        thickness = 22.dp,
                        shape = CorneredShape.rounded(topLeftPercent = 35, topRightPercent = 35)
                    )
                )
            ),
            startAxis = VerticalAxis.rememberStart(
                valueFormatter = startAxisFormatter
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = bottomAxisFormatter
            )
        ),
        modelProducer = modelProducer,
        modifier = modifier
    )
}

private fun buildWeeklyBuckets(
    sessions: List<ExerciseSession>,
    weeksCount: Int = 6
): List<WeeklySessionBucket> {
    val nowWeekStart = weekStartMondayMillis(Date())
    val latestSessionWeekStart = sessions
        .mapNotNull { (it.updatedAt ?: it.createdAt)?.toDate() }
        .maxOfOrNull { weekStartMondayMillis(it) }
        ?: nowWeekStart
    val endWeekStart = maxOf(nowWeekStart, latestSessionWeekStart)

    val cal = Calendar.getInstance()
    val weekFormat = SimpleDateFormat("d MMM", Locale("es", "ES"))

    val weekStarts = (weeksCount - 1 downTo 0).map { weeksAgo ->
        cal.timeInMillis = endWeekStart
        cal.add(Calendar.WEEK_OF_YEAR, -weeksAgo)
        cal.timeInMillis
    }

    val countsByWeek = mutableMapOf<Long, Int>()
    val repsByWeek = mutableMapOf<Long, Long>()
    sessions.forEach { session ->
        val date = (session.updatedAt ?: session.createdAt ?: Timestamp.now()).toDate()
        val weekKey = weekStartMondayMillis(date)
        countsByWeek[weekKey] = (countsByWeek[weekKey] ?: 0) + 1
        repsByWeek[weekKey] = (repsByWeek[weekKey] ?: 0L) + session.completedReps
    }

    return weekStarts.mapIndexed { idx, weekStart ->
        val label = if (idx == weekStarts.lastIndex && weekStart == nowWeekStart) {
            "Actual"
        } else {
            weekFormat.format(Date(weekStart)).replace(".", "")
        }
        WeeklySessionBucket(
            weekStartMillis = weekStart,
            label = label,
            sessionCount = countsByWeek[weekStart] ?: 0,
            totalReps = repsByWeek[weekStart] ?: 0L
        )
    }
}

private fun weekStartMondayMillis(date: Date): Long {
    val cal = Calendar.getInstance().apply {
        time = date
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        val dayOfWeek = get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = (dayOfWeek - Calendar.MONDAY + 7) % 7
        add(Calendar.DAY_OF_MONTH, -daysFromMonday)
    }
    return cal.timeInMillis
}

@Composable
private fun KpiCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PainTrendChart(
    painValues: List<Int>,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val dotColor = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padH = 14.dp.toPx()
        val padV = 12.dp.toPx()
        val plotW = (w - padH * 2).coerceAtLeast(1f)
        val plotH = (h - padV * 2).coerceAtLeast(1f)

        // Horizontal reference lines (0, 5, 10)
        listOf(0f, 0.5f, 1f).forEach { frac ->
            val y = padV + plotH * frac
            drawLine(
                color = gridColor,
                start = Offset(padH, y),
                end = Offset(w - padH, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        if (painValues.isEmpty()) return@Canvas

        val points = painValues.mapIndexed { idx, value ->
            val xFrac = if (painValues.size == 1) 0.5f else idx.toFloat() / (painValues.size - 1).toFloat()
            val yFrac = 1f - (value.coerceIn(0, 10) / 10f)
            Offset(
                x = padH + plotW * xFrac,
                y = padV + plotH * yFrac
            )
        }

        if (points.size >= 2) {
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        points.forEach { pt ->
            drawCircle(color = dotColor, radius = 5.dp.toPx(), center = pt)
        }
    }
}
