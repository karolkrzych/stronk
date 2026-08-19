package com.stronk.ui.workout

import com.stronk.data.ExerciseState
import com.stronk.data.SetLog
import com.stronk.data.SetTarget
import com.stronk.progression.ExerciseProposal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun `seria wagowa to dwa osobne staty ciezar i powtorzenia`() {
        assertEquals(
            listOf(
                SetStat("Ciężar", "62,5", "kg"),
                SetStat("Powtórzenia", "8"),
            ),
            WorkoutLabels.setStats(weightSet(62.5, 8)),
        )
    }

    @Test
    fun `staty serii per typ pomiaru`() {
        assertEquals(
            listOf(SetStat("Powtórzenia", "10"), SetStat("Dociążenie", "5", "kg")),
            WorkoutLabels.setStats(
                SetLog.Reps(
                    exerciseId = "pullups", workoutId = "w1", setNumber = 1,
                    isWarmup = false, timestamp = 0L, reps = 10, extraKg = 5.0,
                ),
            ),
        )
        assertEquals(
            listOf(SetStat("Czas", "45", "s")),
            WorkoutLabels.setStats(
                SetLog.Time(
                    exerciseId = "plank", workoutId = "w1", setNumber = 1,
                    isWarmup = false, timestamp = 0L, seconds = 45,
                ),
            ),
        )
        assertEquals(
            listOf(SetStat("Dystans", "1", "km"), SetStat("Czas", "5:00")),
            WorkoutLabels.setStats(
                SetLog.DistanceTime(
                    exerciseId = "run", workoutId = "w1", setNumber = 1,
                    isWarmup = false, timestamp = 0L, meters = 1000.0, seconds = 300,
                ),
            ),
        )
    }

    @Test
    fun `zadna etykieta nie sklaja pary wartosci w jedna fraze`() {
        val stats = WorkoutLabels.setStats(weightSet(60.0, 8)) +
            WorkoutLabels.targetStats(
                ExerciseProposal(
                    exerciseId = "bench", sets = 3,
                    target = SetTarget.WeightReps(8), weightKg = 60.0,
                ),
            )
        assertTrue(stats.none { it.value.contains("×") || it.value.contains("kg") })
        assertEquals("60 kg", WorkoutLabels.setValue(weightSet(60.0, 8)))
        assertEquals("10 powt.", WorkoutLabels.setValue(
            SetLog.Reps(
                exerciseId = "pullups", workoutId = "w1", setNumber = 1,
                isWarmup = false, timestamp = 0L, reps = 10, extraKg = null,
            ),
        ))
    }

    @Test
    fun `cel propozycji jako serie powtorzenia ciezar`() {
        val withWeight = ExerciseProposal(
            exerciseId = "bench", sets = 3, target = SetTarget.WeightReps(8), weightKg = 60.0,
        )
        assertEquals(
            listOf(
                SetStat("Serie", "3"),
                SetStat("Powtórzenia", "8"),
                SetStat("Ciężar", "60", "kg"),
            ),
            WorkoutLabels.targetStats(withWeight),
        )

        assertEquals(
            listOf(SetStat("Serie", "3"), SetStat("Powtórzenia", "8")),
            WorkoutLabels.targetStats(withWeight.copy(weightKg = null)),
        )
    }

    @Test
    fun `ostatnio pokazuje maks ciezar i powtorzenia serii roboczych`() {
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
        assertEquals(
            listOf(SetStat("Ciężar", "60", "kg"), SetStat("Powtórzenia", "8, 7")),
            WorkoutLabels.lastStats(state),
        )
        assertEquals(emptyList<SetStat>(), WorkoutLabels.lastStats(null))
        assertEquals(
            emptyList<SetStat>(),
            WorkoutLabels.lastStats(state.copy(lastSets = emptyList())),
        )
    }

    @Test
    fun `polska odmiana liczby serii`() {
        assertEquals("1 seria", WorkoutLabels.setCount(1))
        assertEquals("2 serie", WorkoutLabels.setCount(2))
        assertEquals("4 serie", WorkoutLabels.setCount(4))
        assertEquals("5 serii", WorkoutLabels.setCount(5))
        assertEquals("12 serii", WorkoutLabels.setCount(12))
        assertEquals("14 serii", WorkoutLabels.setCount(14))
        assertEquals("22 serie", WorkoutLabels.setCount(22))
        assertEquals("0 serii", WorkoutLabels.setCount(0))
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
