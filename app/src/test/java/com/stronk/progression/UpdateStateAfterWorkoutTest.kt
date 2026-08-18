package com.stronk.progression

import com.stronk.data.SetTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Aktualizacja ExerciseState po treningu: definicja "zaliczone", failStreak, ciężar, zamrożenie w tygodniu lekkim. */
class UpdateStateAfterWorkoutTest {

    private val now = 1_755_000_000_000L

    private fun proposal(
        weightKg: Double? = 102.5,
        reps: Int = 8,
        sets: Int = 3,
        lightWeek: Boolean = false,
    ) = ExerciseProposal(
        exerciseId = "Barbell_Bench_Press",
        sets = sets,
        target = SetTarget.WeightReps(reps),
        weightKg = weightKg,
        isLightWeek = lightWeek,
    )

    // --- sukces i porażka (WEIGHT_REPS) ---

    @Test
    fun `wszystkie serie na cel - failStreak zeruje się, ciężar roboczy to użyty ciężar`() {
        val old = stateOf(failStreak = 1, currentWeightKg = 100.0)
        val logged = weightSets(102.5, 8)
        val new = ProgressionEngine.updateStateAfterWorkout(old, proposal(), logged, now)

        assertEquals(0, new.failStreak)
        assertEquals(102.5, new.currentWeightKg!!, 1e-9)
        assertEquals(logged, new.lastSets)
        assertEquals(now, new.updatedAt)
        assertEquals("Barbell_Bench_Press", new.exerciseId)
    }

    @Test
    fun `za mało powtórzeń w jednej serii - porażka, failStreak rośnie`() {
        val old = stateOf(failStreak = 0, currentWeightKg = 100.0)
        val logged = listOf(weightSet(102.5, 8, 1), weightSet(102.5, 8, 2), weightSet(102.5, 6, 3))
        val new = ProgressionEngine.updateStateAfterWorkout(old, proposal(), logged, now)

        assertEquals(1, new.failStreak)
        assertEquals(102.5, new.currentWeightKg!!, 1e-9)
    }

    @Test
    fun `powtórzenia na cel ale ciężar poniżej propozycji - porażka`() {
        val logged = weightSets(100.0, 8)
        val new = ProgressionEngine.updateStateAfterWorkout(null, proposal(weightKg = 102.5), logged, now)

        assertEquals(1, new.failStreak)
        assertEquals(100.0, new.currentWeightKg!!, 1e-9)
    }

    @Test
    fun `propozycja bez ciężaru - sukces liczony po samych powtórzeniach`() {
        val logged = weightSets(40.0, 8)
        val new = ProgressionEngine.updateStateAfterWorkout(null, proposal(weightKg = null), logged, now)

        assertEquals(0, new.failStreak)
        // faktycznie użyty ciężar trafia do stanu — od teraz progresja ma punkt zaczepienia
        assertEquals(40.0, new.currentWeightKg!!, 1e-9)
    }

    @Test
    fun `mniej serii roboczych niż plan - porażka`() {
        val logged = listOf(weightSet(102.5, 8, 1), weightSet(102.5, 8, 2))
        val new = ProgressionEngine.updateStateAfterWorkout(null, proposal(sets = 3), logged, now)
        assertEquals(1, new.failStreak)
    }

    @Test
    fun `serie rozgrzewkowe nie liczą się do wyniku`() {
        val logged = listOf(weightSet(60.0, 10, 1, warmup = true)) + weightSets(102.5, 8)
        val new = ProgressionEngine.updateStateAfterWorkout(null, proposal(), logged, now)

        assertEquals(0, new.failStreak)
        assertEquals(102.5, new.currentWeightKg!!, 1e-9)
    }

    @Test
    fun `nadmiarowe serie ponad plan nie psują wyniku`() {
        val logged = weightSets(102.5, 8) + weightSet(80.0, 12, setNumber = 4)
        val new = ProgressionEngine.updateStateAfterWorkout(null, proposal(sets = 3), logged, now)
        assertEquals(0, new.failStreak)
    }

    @Test
    fun `kolejność serii po setNumber, nie po kolejności listy`() {
        val logged = listOf(weightSet(102.5, 6, 4), weightSet(102.5, 8, 1), weightSet(102.5, 8, 2), weightSet(102.5, 8, 3))
        val new = ProgressionEngine.updateStateAfterWorkout(null, proposal(sets = 3), logged, now)
        // słabsza seria ma numer 4 — poza ocenianym planem
        assertEquals(0, new.failStreak)
    }

    // --- brak logów i tydzień lekki ---

    @Test
    fun `brak zalogowanych serii - stan bez zmian merytorycznych`() {
        val old = stateOf(lastSets = weightSets(100.0, 8), failStreak = 1, currentWeightKg = 100.0)
        val new = ProgressionEngine.updateStateAfterWorkout(old, proposal(), emptyList(), now)

        assertEquals(old.copy(updatedAt = now), new)
    }

    @Test
    fun `brak logów i brak starego stanu - świeży pusty stan`() {
        val new = ProgressionEngine.updateStateAfterWorkout(null, proposal(), emptyList(), now)
        assertTrue(new.lastSets.isEmpty())
        assertEquals(0, new.failStreak)
        assertNull(new.currentWeightKg)
    }

    @Test
    fun `tydzień lekki zamraża stan - failStreak, ciężar i lastSets zostają`() {
        val old = stateOf(lastSets = weightSets(100.0, 8), failStreak = 1, currentWeightKg = 100.0)
        val lightLogged = weightSets(60.0, 8)
        val new = ProgressionEngine.updateStateAfterWorkout(old, proposal(weightKg = 60.0, lightWeek = true), lightLogged, now)

        assertEquals(old.copy(updatedAt = now), new)
    }

    // --- pozostałe typy pomiaru ---

    @Test
    fun `REPS sukces - ciężar roboczy zostaje pusty`() {
        val planned = ExerciseProposal(exerciseId = "Pullups", sets = 3, target = SetTarget.Reps(10))
        val logged = listOf(repsSet(10, 1), repsSet(10, 2), repsSet(11, 3))
        val new = ProgressionEngine.updateStateAfterWorkout(null, planned, logged, now)

        assertEquals(0, new.failStreak)
        assertNull(new.currentWeightKg)
        assertEquals(logged, new.lastSets)
    }

    @Test
    fun `TIME porażka - failStreak rośnie`() {
        val planned = ExerciseProposal(exerciseId = "Plank", sets = 2, target = SetTarget.Time(60))
        val logged = listOf(timeSet(60, 1), timeSet(45, 2))
        val new = ProgressionEngine.updateStateAfterWorkout(null, planned, logged, now)
        assertEquals(1, new.failStreak)
    }

    @Test
    fun `DISTANCE_TIME wolniejsze tempo - porażka mimo dystansu`() {
        val planned = ExerciseProposal(exerciseId = "Running_Treadmill", sets = 1, target = SetTarget.DistanceTime(1000.0, 300))
        val new = ProgressionEngine.updateStateAfterWorkout(null, planned, listOf(distSet(1000.0, 320)), now)
        assertEquals(1, new.failStreak)
    }

    @Test
    fun `DISTANCE_TIME dłuższy dystans w proporcjonalnym czasie - sukces`() {
        val planned = ExerciseProposal(exerciseId = "Running_Treadmill", sets = 1, target = SetTarget.DistanceTime(1000.0, 300))
        // 1100 m w 320 s → tempo lepsze niż 1000 m w 300 s
        val new = ProgressionEngine.updateStateAfterWorkout(null, planned, listOf(distSet(1100.0, 320)), now)
        assertEquals(0, new.failStreak)
    }

    @Test
    fun `seria złego typu nie spełnia celu`() {
        val planned = ExerciseProposal(exerciseId = "Plank", sets = 1, target = SetTarget.Time(60))
        val new = ProgressionEngine.updateStateAfterWorkout(null, planned, listOf(repsSet(10, 1, exerciseId = "Plank")), now)
        assertEquals(1, new.failStreak)
    }
}
