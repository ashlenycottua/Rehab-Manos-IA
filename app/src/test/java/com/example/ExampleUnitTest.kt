package com.example

import com.example.data.catalog.EbookCatalog
import com.example.data.catalog.ExerciseCatalog
import com.example.data.model.ExerciseCategory
import com.example.data.model.PrimaryGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun catalog_hasAllFourCategoriesAndExercises() {
        val exercises = ExerciseCatalog.allExercises
        assertEquals(12, exercises.size)
        ExerciseCategory.entries.forEach { category ->
            assertTrue(
                "Missing exercises for ${category.id}",
                exercises.any { it.category == category }
            )
        }
    }

    @Test
    fun recommendedRoutine_returnsExercisesForEveryGoal() {
        PrimaryGoal.entries.forEach { goal ->
            val rec = ExerciseCatalog.recommendedForGoal(goal)
            assertTrue("Expected recommendations for $goal", rec.isNotEmpty())
        }
        assertTrue(EbookCatalog.chapters.size >= 5)
    }
}
