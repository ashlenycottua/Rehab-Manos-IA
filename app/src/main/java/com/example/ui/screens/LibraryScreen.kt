package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.data.model.ExerciseCategory
import com.example.data.model.RehabExercise
import com.example.data.model.RoutineItem
import com.example.ui.components.ExerciseSummaryCard
import com.example.ui.components.SafetyNoticeCard
import com.example.ui.components.categoryAccentContainer
import com.example.ui.components.categoryAccentContent
import com.example.ui.components.categoryIcon
import com.example.ui.viewmodel.UiState
import java.text.Normalizer
import java.util.Locale

object ExerciseSearchFilter {
    private val diacriticsRegex = Regex("\\p{InCombiningDiacriticalMarks}+")

    fun normalize(text: String): String {
        val decomposed = Normalizer.normalize(text.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
        return diacriticsRegex.replace(decomposed, "")
    }

    fun filter(
        exercises: List<RehabExercise>,
        query: String,
        selectedCategory: ExerciseCategory?,
        routineOnly: Boolean,
        routineIds: Set<String>
    ): List<RehabExercise> {
        val normalizedQuery = normalize(query)
        val tokens = normalizedQuery.split(Regex("\\s+")).filter { it.isNotBlank() }

        return exercises.filter { exercise ->
            val matchesRoutine = !routineOnly || exercise.id in routineIds
            val matchesCategory = selectedCategory == null || exercise.category == selectedCategory
            if (!matchesRoutine || !matchesCategory) return@filter false
            if (tokens.isEmpty()) return@filter true

            val searchableHaystack = normalize(
                listOf(
                    exercise.title,
                    exercise.category.shortLabel,
                    exercise.category.title,
                    exercise.category.subtitle,
                    exercise.difficultyLabel,
                    exercise.summary
                ).joinToString(" ")
            )
            tokens.all { token -> searchableHaystack.contains(token) }
        }
    }
}

@Composable
fun LibraryScreen(
    exercisesState: UiState<List<RehabExercise>>,
    initialCategoryFilter: ExerciseCategory?,
    routineItems: List<RoutineItem>,
    onRetryExercises: () -> Unit,
    onOpenExercise: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var showRoutineOnly by remember { mutableStateOf(false) }
    var selectedCategory by remember(initialCategoryFilter) { mutableStateOf(initialCategoryFilter) }
    var searchQuery by remember { mutableStateOf("") }

    val routineExerciseIds = remember(routineItems) {
        routineItems.map { it.exerciseId }.toSet()
    }

    val allExercises = (exercisesState as? UiState.Success)?.data.orEmpty()

    val filteredExercises = remember(allExercises, showRoutineOnly, selectedCategory, searchQuery, routineExerciseIds) {
        ExerciseSearchFilter.filter(
            exercises = allExercises,
            query = searchQuery,
            selectedCategory = selectedCategory,
            routineOnly = showRoutineOnly,
            routineIds = routineExerciseIds
        )
    }

    val groupedByCategory = remember(filteredExercises) {
        ExerciseCategory.entries.mapNotNull { category ->
            val items = filteredExercises.filter { it.category == category }
            if (items.isEmpty()) null else category to items
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Text(
                text = "Biblioteca de Ejercicios",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Explora ejercicios categorizados con demostración visual y guía por voz.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !showRoutineOnly,
                    onClick = { showRoutineOnly = false },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier.testTag("tab_all_exercises")
                ) {
                    Text("Biblioteca (${allExercises.size})")
                }
                SegmentedButton(
                    selected = showRoutineOnly,
                    onClick = { showRoutineOnly = true },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.testTag("tab_my_routine")
                ) {
                    Text("Mi rutina (${routineExerciseIds.size})")
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar por nombre o categoría (ej. muñeca, fuerza, dedos)") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Buscar ejercicio",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.testTag("clear_search_button")
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Limpiar búsqueda")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { focusManager.clearFocus() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("library_search_input"),
                shape = MaterialTheme.shapes.large
            )

            Spacer(Modifier.height(10.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("Todos") },
                        modifier = Modifier.testTag("filter_chip_ALL")
                    )
                }
                items(ExerciseCategory.entries) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = {
                            selectedCategory = if (selectedCategory == category) null else category
                        },
                        label = { Text(category.shortLabel) },
                        modifier = Modifier.testTag("filter_chip_${category.id}")
                    )
                }
            }

            AnimatedVisibility(visible = searchQuery.isNotBlank() || selectedCategory != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .testTag("search_results_summary"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${filteredExercises.size} ${if (filteredExercises.size == 1) "ejercicio encontrado" else "ejercicios encontrados"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = {
                            searchQuery = ""
                            selectedCategory = null
                        },
                        modifier = Modifier.testTag("reset_filters_button")
                    ) {
                        Text("Limpiar filtros")
                    }
                }
            }
        }

        when (exercisesState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Cargando biblioteca desde Firestore...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is UiState.Error -> {
                Column(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SafetyNoticeCard(
                        title = "No se pudo cargar la biblioteca",
                        body = exercisesState.message,
                        isElevatedAlert = true
                    )
                    Button(
                        onClick = onRetryExercises,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("retry_exercises_button"),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Reintentar conexión")
                    }
                }
            }

            is UiState.Success -> {
                if (filteredExercises.isEmpty()) {
                    val hasActiveSearch = searchQuery.isNotBlank()
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (hasActiveSearch) Icons.Rounded.SearchOff else Icons.Rounded.BookmarkAdded,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = when {
                                    allExercises.isEmpty() -> "Biblioteca sincronizándose"
                                    hasActiveSearch -> "Sin resultados para \"${searchQuery.trim()}\""
                                    showRoutineOnly -> "Tu rutina no tiene ejercicios en este filtro"
                                    else -> "No encontramos ejercicios"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (allExercises.isEmpty()) {
                                    "Toca sincronizar para cargar los ejercicios iniciales en Firestore."
                                } else {
                                    "Prueba buscar con otro nombre o categoría (Movilidad, Fuerza, Motricidad, Dedos)."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = {
                                    if (allExercises.isEmpty()) {
                                        onRetryExercises()
                                    } else {
                                        showRoutineOnly = false
                                        selectedCategory = null
                                        searchQuery = ""
                                    }
                                },
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("empty_clear_search_button")
                            ) {
                                Text(if (allExercises.isEmpty()) "Sincronizar ejercicios" else "Mostrar todos")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("library_exercises_list"),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        groupedByCategory.forEach { (category, categoryExercises) ->
                            item(key = "header_${category.id}") {
                                CategorySectionHeader(
                                    category = category,
                                    count = categoryExercises.size,
                                    modifier = Modifier
                                        .widthIn(max = 720.dp)
                                        .padding(top = 6.dp)
                                )
                            }
                            items(
                                items = categoryExercises,
                                key = { it.id }
                            ) { exercise ->
                                ExerciseSummaryCard(
                                    exercise = exercise,
                                    inRoutine = exercise.id in routineExerciseIds,
                                    onClick = { onOpenExercise(exercise.id) },
                                    modifier = Modifier.widthIn(max = 720.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySectionHeader(
    category: ExerciseCategory,
    count: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("category_header_${category.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = categoryAccentContainer(category),
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = categoryIcon(category),
                    contentDescription = null,
                    tint = categoryAccentContent(category),
                    modifier = Modifier.padding(7.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = category.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = MaterialTheme.shapes.small,
            color = categoryAccentContainer(category)
        ) {
            Text(
                text = "${category.shortLabel} · $count",
                style = MaterialTheme.typography.labelSmall,
                color = categoryAccentContent(category),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
