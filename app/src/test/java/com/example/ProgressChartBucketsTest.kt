package com.example

import com.example.data.model.ExerciseSession
import com.example.ui.screens.buildLast7DaysBuckets
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.Date

class ProgressChartBucketsTest {

    @Test
    fun `buildLast7DaysBuckets creates 7 consecutive days ending in Hoy with accurate counts`() {
        val refCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 18, 15, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val refDate = refCal.time

        fun daysAgoTimestamp(daysAgo: Int): Timestamp {
            val cal = (refCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, -daysAgo)
            }
            return Timestamp(Date(cal.timeInMillis))
        }

        val sessions = listOf(
            ExerciseSession(id = "1", completedReps = 10, createdAt = daysAgoTimestamp(0)),
            ExerciseSession(id = "2", completedReps = 12, createdAt = daysAgoTimestamp(0)),
            ExerciseSession(id = "3", completedReps = 8, createdAt = daysAgoTimestamp(2)),
            ExerciseSession(id = "4", completedReps = 15, createdAt = daysAgoTimestamp(6)),
            ExerciseSession(id = "5", completedReps = 20, createdAt = daysAgoTimestamp(9)) // outside 7d window
        )

        val buckets = buildLast7DaysBuckets(sessions, referenceDate = refDate)

        assertEquals(7, buckets.size)
        assertEquals("Hoy", buckets.last().label)
        assertEquals(2, buckets[6].completedExercises) // today
        assertEquals(0, buckets[5].completedExercises) // yesterday
        assertEquals(1, buckets[4].completedExercises) // 2 days ago
        assertEquals(1, buckets[0].completedExercises) // 6 days ago
        assertEquals(22L, buckets[6].totalReps)
    }
}
