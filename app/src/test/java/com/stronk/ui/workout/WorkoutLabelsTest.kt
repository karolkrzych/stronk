package com.stronk.ui.workout

import com.stronk.data.ExerciseState
import com.stronk.data.SetLog
import com.stronk.data.SetTarget
import com.stronk.progression.ExerciseProposal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutLabelsTest {

    private fun weightSet(kg: Double, reps: Int, setNumber: Int = 1) = SetLog.WeightReps(
        exerciseId = "bench", workoutId = "w1", setNumber = setNumber,
        isWarmup = false, timestamp = 0L, kg = kg, reps = reps,
    )

    @Test
    fun `kg bez zbednych zer i z przecinkiem dziesietnym`() {
        assertEquals("60", WorkoutLabels.kg(60.0))
        assertEquals("62,5", WorkoutLabels.kg(62.5))
        assertEquals("61,25", WorkoutLabels.kg(61.25))
    }

    @Test
    fun `countdown w formacie minuty dwucyfrowe sekundy`() {
        assertEquals("1:30", WorkoutLabels.countdown(90))
        assertEquals("0:05", WorkoutLabels.countdown(5))
        assertEquals("0:00", WorkoutLabels.countdown(-3))
    }

    @Test
    fun `metry ponizej kilometra w metrach powyzej w kilometrach`() {
        assertEquals("800 m", WorkoutLabels.meters(800.0))
        assertEquals("2 km", WorkoutLabels.meters(2000.0))
        assertEquals("2,5 km", WorkoutLabels.meters(2500.0))
    }

    @Test
    fun `wartosc serii per typ pomiaru`() {
        assertEquals("60 kg × 8", WorkoutLabels.setValue(weightSet(60.0, 8)))
        assertEquals(
            "10 powt. (+5 kg)",
            WorkoutLabels.setValue(
                SetLog.Reps(
                    exerciseId = "pullups", workoutId = "w1", setNumber = 1,
                    isWarmup = false, timestamp = 0L, reps = 10, extraKg = 5.0,
                ),
            ),
        )
        assertEquals(
            "45 s",
            WorkoutLabels.setValue(
                SetLog.Time(
                    exerciseId = "plank", workoutId = "w1", setNumber = 1,
                    isWarmup = false, timestamp = 0L, seconds = 45,
                ),
            ),
        )
        assertEquals(
            "1 km · 5:00",
            WorkoutLabels.setValue(
                SetLog.DistanceTime(
                    exerciseId = "run", workoutId = "w1", setNumber = 1,
                    isWarmup = false, timestamp = 0L, meters = 1000.0, seconds = 300,
                ),
            ),
        )
    }

    @Test
    fun `cel propozycji z ciezarem i bez`() {
        val withWeight = ExerciseProposal(
            exerciseId = "bench", sets = 3, target = SetTarget.WeightReps(8), weightKg = 60.0,
        )
        assertEquals("3×8 · 60 kg", WorkoutLabels.proposalTarget(withWeight))

        val withoutWeight = withWeight.copy(weightKg = null)
        assertEquals("3×8", WorkoutLabels.proposalTarget(withoutWeight))
    }

    @Test
    fun `ostatnio pokazuje maks kg i liste powtorzen serii roboczych`() {
        val state = ExerciseState(
            exerciseId = "bench",
            lastSets = listOf(
                weightSet(57.5, 8, setNumber = 1),
                weightSet(60.0, 7, setNumber = 2),
                // rozgrzewkowa nie wchodzi do podsumowania
                weightSet(20.0, 12, setNumber = 1).copy(isWarmup = true),
            ),
            updatedAt = 0L,
        )
        assertEquals("Ostatnio: 60 kg × 8, 7", WorkoutLabels.lastTime(state))
        assertNull(WorkoutLabels.lastTime(null))
        assertNull(WorkoutLabels.lastTime(state.copy(lastSets = emptyList())))
    }

    @Test
    fun `plakietki modyfikatorow propozycji`() {
        val base = ExerciseProposal(exerciseId = "x", sets = 3, target = SetTarget.Reps(10))
        assertEquals(emptyList<String>(), WorkoutLabels.proposalBadges(base))
        assertEquals(
            listOf("tydzień lekki −40%", "deload −10%", "ramp-up"),
            WorkoutLabels.proposalBadges(
                base.copy(isLightWeek = true, isReactiveDeload = true, isRampUp = true),
            ),
        )
    }
}
