package com.example

import com.example.data.catalog.ExerciseCatalog
import com.example.data.model.ExerciseCategory
import com.example.ui.screens.ExerciseSearchFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseSearchFilterTest {

    private val allExercises = ExerciseCatalog.allExercises

    @Test
    fun `filters by exercise name ignoring accents and case`() {
        val results = ExerciseSearchFilter.filter(
            exercises = allExercises,
            query = "muneca",
            selectedCategory = null,
            routineOnly = false,
            routineIds = emptySet()
        )
        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.title.contains("muñeca", ignoreCase = true) })
    }

    @Test
    fun `filters by full category title or short label`() {
        val fuerzaResults = ExerciseSearchFilter.filter(
            exercises = allExercises,
            query = "fortalecimiento",
            selectedCategory = null,
            routineOnly = false,
            routineIds = emptySet()
        )
        assertTrue(fuerzaResults.isNotEmpty())
        assertTrue(fuerzaResults.all { it.category == ExerciseCategory.FORTALECIMIENTO })

        val motricidadResults = ExerciseSearchFilter.filter(
            exercises = allExercises,
            query = "motricidad",
            selectedCategory = null,
            routineOnly = false,
            routineIds = emptySet()
        )
        assertTrue(motricidadResults.isNotEmpty())
        assertTrue(motricidadResults.all { it.category == ExerciseCategory.MOTRICIDAD_FINA })
    }

    @Test
    fun `combines search query with selected category chip`() {
        val results = ExerciseSearchFilter.filter(
            exercises = allExercises,
            query = "dedos",
            selectedCategory = ExerciseCategory.MOVILIDAD_DEDOS,
            routineOnly = false,
            routineIds = emptySet()
        )
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.category == ExerciseCategory.MOVILIDAD_DEDOS })
    }

    @Test
    fun `blank query returns all exercises when no filters are active`() {
        val results = ExerciseSearchFilter.filter(
            exercises = allExercises,
            query = "   ",
            selectedCategory = null,
            routineOnly = false,
            routineIds = emptySet()
        )
        assertEquals(allExercises.size, results.size)
    }
}
