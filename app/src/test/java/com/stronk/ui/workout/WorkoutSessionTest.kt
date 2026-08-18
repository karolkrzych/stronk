package com.stronk.ui.workout

import com.stronk.data.Exercise
import com.stronk.data.JointStress
import com.stronk.data.MeasurementType
import com.stronk.data.PlanExercise
import com.stronk.data.SetLog
import com.stronk.data.SetTarget
import com.stronk.data.StressLevel
import com.stronk.progression.ExerciseProposal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// ------------------------------------------------------------ buildery

private val noStress = JointStress(
    lowBack = StressLevel.NONE, knee = StressLevel.NONE, shoulder = StressLevel.NONE,
    hip = StressLevel.NONE, elbow = StressLevel.NONE, wrist = StressLevel.NONE,
    neck = StressLevel.NONE,
)

private fun exercise(
    id: String,
    measurementType: MeasurementType = MeasurementType.WEIGHT_REPS,
) = Exercise(
    id = id, name = id, namePl = id, instructionsPl = emptyList(),
    primaryMuscles = listOf("chest"), secondaryMuscles = emptyList(),
    equipment = null, level = "beginner", category = "strength",
    mechanic = null, force = null, images = emptyList(),
    jointStress = noStress, cautionNotes = null,
    measurementType = measurementType,
)

private fun weightExercise(
    id: String = "bench",
    sets: Int = 3,
    reps: Int = 8,
    weightKg: Double? = 60.0,
    planIndex: Int = 0,
) = SessionExercise(
    exercise = exercise(id),
    planExercise = PlanExercise(
        exerciseId = id, sets = sets, target = SetTarget.WeightReps(reps), startWeightKg = weightKg,
    ),
    proposal = ExerciseProposal(
        exerciseId = id, sets = sets, target = SetTarget.WeightReps(reps), weightKg = weightKg,
    ),
    oldState = null,
    planExerciseIndex = planIndex,
)

private fun session(vararg exercises: SessionExercise, restSeconds: Int = 60) = WorkoutSession(
    workoutId = "w1", planId = "p1", dayIndex = 0, scheduleEntryId = null,
    planName = "Plan", dayName = "Dzień A", startedAt = 0L,
    weekIndexInBlock = 0, fullBlockLengthWeeks = 6, returningFromBreak = false,
    exercises = exercises.toList(), restSeconds = restSeconds,
)

private fun weightSet(kg: Double, reps: Int, exerciseId: String = "bench", setNumber: Int = 1) =
    SetLog.WeightReps(
        exerciseId = exerciseId, workoutId = "w1", setNumber = setNumber,
        isWarmup = false, timestamp = 0L, kg = kg, reps = reps,
    )

// ---------------------------------------------------------------- testy

class WorkoutSessionTest {

    @Test
    fun `wielki tick loguje prefill z propozycji i startuje przerwe`() {
        val s = session(weightExercise(), weightExercise(id = "row"))
            .completeCurrentSet(nowMillis = 1_000L)

        val logged = s.exercises[0].loggedSets.single() as SetLog.WeightReps
        assertEquals(60.0, logged.kg, 1e-9)
        assertEquals(8, logged.reps)
        assertEquals(1, logged.setNumber)
        assertFalse(logged.isWarmup)
        assertEquals("w1", logged.workoutId)
        // przerwa: now + restSeconds
        assertEquals(1_000L + 60_000L, s.restEndsAtMillis)
        // wciąż to samo ćwiczenie (seria 2 z 3)
        assertEquals(0, s.currentExerciseIndex)
        assertEquals(2, s.exercises[0].nextSetNumber)
    }

    @Test
    fun `prefill klei sie do wartosci edytowanej serii`() {
        var s = session(weightExercise())
        s = s.logSet(weightSet(kg = 55.0, reps = 6), 0L)

        val prefill = s.prefillForCurrentSet(10L) as SetLog.WeightReps
        assertEquals(55.0, prefill.kg, 1e-9)
        assertEquals(6, prefill.reps)
        assertEquals(2, prefill.setNumber)
    }

    @Test
    fun `po skompletowaniu cwiczenia przechodzi dalej a po ostatniej serii nie ma przerwy`() {
        var s = session(weightExercise(sets = 1), weightExercise(id = "row", sets = 1))

        s = s.completeCurrentSet(0L)
        assertEquals(1, s.currentExerciseIndex)
        assertNotNull(s.restEndsAtMillis)
        assertFalse(s.allFinished)

        s = s.completeCurrentSet(0L)
        assertTrue(s.allFinished)
        assertNull(s.restEndsAtMillis)
        assertNull(s.currentExercise)
        assertEquals(2, s.completedSetCount)
    }

    @Test
    fun `pierwsza seria bez znanego ciezaru wymaga edycji a po niej tick dziala`() {
        var s = session(weightExercise(weightKg = null))
        assertTrue(s.currentPrefillNeedsInput)
        // ✓ jest no-opem, dopóki nie ma ciężaru
        assertEquals(s, s.completeCurrentSet(0L))

        s = s.logSet(weightSet(kg = 40.0, reps = 8), 0L)
        assertFalse(s.currentPrefillNeedsInput)
        val prefill = s.prefillForCurrentSet(0L) as SetLog.WeightReps
        assertEquals(40.0, prefill.kg, 1e-9)
    }

    @Test
    fun `pominiecie cwiczenia przechodzi dalej a wybor z listy odblokowuje`() {
        var s = session(weightExercise(), weightExercise(id = "row"))

        s = s.skipCurrentExercise()
        assertEquals(1, s.currentExerciseIndex)
        assertTrue(s.exercises[0].skipped)

        s = s.selectExercise(0)
        assertEquals(0, s.currentExerciseIndex)
        assertFalse(s.exercises[0].skipped)
    }

    @Test
    fun `pominiete cwiczenie liczy sie jako zakonczone dla calego treningu`() {
        var s = session(weightExercise(sets = 1), weightExercise(id = "row", sets = 1))
        s = s.completeCurrentSet(0L)
        s = s.skipCurrentExercise()
        assertTrue(s.allFinished)
    }

    @Test
    fun `podmiana bez zalogowanych serii podmienia w miejscu`() {
        val replacement = weightExercise(id = "machine_press")
            .copy(substitutedFromId = "bench")
        val s = session(weightExercise(), weightExercise(id = "row"))
            .substituteCurrent(replacement)

        assertEquals(2, s.exercises.size)
        assertEquals("machine_press", s.exercises[0].exerciseId)
        assertEquals("bench", s.exercises[0].substitutedFromId)
        assertEquals(0, s.currentExerciseIndex)
    }

    @Test
    fun `podmiana w trakcie cwiczenia zachowuje zrobione serie i wstawia zamiennik za oryginalem`() {
        val replacement = weightExercise(id = "machine_press")
            .copy(substitutedFromId = "bench")
        var s = session(weightExercise(sets = 3), weightExercise(id = "row"))
        s = s.completeCurrentSet(0L)
        s = s.substituteCurrent(replacement)

        assertEquals(3, s.exercises.size)
        assertTrue(s.exercises[0].skipped)
        assertEquals(1, s.exercises[0].loggedSets.size)
        assertEquals("machine_press", s.exercises[1].exerciseId)
        assertEquals(1, s.currentExerciseIndex)
    }

    @Test
    fun `adaptPlanExercise zachowuje cel przy zgodnym typie i zeruje ciezar startowy`() {
        val original = PlanExercise(
            exerciseId = "bench", sets = 3,
            target = SetTarget.WeightReps(8), startWeightKg = 60.0,
        )
        val adapted = adaptPlanExercise(original, exercise("machine_press"))

        assertEquals(SetTarget.WeightReps(8), adapted.target)
        assertEquals(3, adapted.sets)
        assertNull(adapted.startWeightKg)
        assertEquals("machine_press", adapted.exerciseId)
    }

    @Test
    fun `adaptPlanExercise przy innym typie pomiaru daje domyslny cel`() {
        val original = PlanExercise(
            exerciseId = "bench", sets = 3,
            target = SetTarget.WeightReps(8), startWeightKg = 60.0,
        )
        val adapted = adaptPlanExercise(
            original,
            exercise("plank", measurementType = MeasurementType.TIME),
        )
        assertEquals(SetTarget.Time(WorkoutConstants.SUBSTITUTE_DEFAULT_TIME_SECONDS), adapted.target)
        assertEquals(3, adapted.sets)
    }

    @Test
    fun `arytmetyka przerwy - odliczanie przedluzenie pominiecie i zaciski dlugosci`() {
        val s = session(weightExercise()).copy(restEndsAtMillis = 10_000L)

        assertEquals(5, s.restRemainingSeconds(5_001L))
        assertNull(s.restRemainingSeconds(10_000L))
        assertEquals(25_000L, s.extendRest(15).restEndsAtMillis)
        assertNull(s.skipRest().restEndsAtMillis)
        assertEquals(WorkoutConstants.REST_MIN_SECONDS, s.withRestSeconds(1).restSeconds)
        assertEquals(WorkoutConstants.REST_MAX_SECONDS, s.withRestSeconds(10_000).restSeconds)
    }

    @Test
    fun `prefill dla typow bez ciezaru bierze wartosci z propozycji`() {
        val id = "plank"
        val se = SessionExercise(
            exercise = exercise(id, MeasurementType.TIME),
            planExercise = PlanExercise(exerciseId = id, sets = 3, target = SetTarget.Time(60)),
            proposal = ExerciseProposal(exerciseId = id, sets = 3, target = SetTarget.Time(65)),
            oldState = null,
            planExerciseIndex = 0,
        )
        val prefill = session(se).prefillForCurrentSet(0L) as SetLog.Time
        assertEquals(65, prefill.seconds)
        // typy bez ciężaru nie blokują ✓
        assertFalse(session(se).currentPrefillNeedsInput)
    }

    @Test
    fun `logSet jest no-opem gdy biezace cwiczenie juz zakonczone`() {
        var s = session(weightExercise(sets = 1))
        s = s.completeCurrentSet(0L)
        assertTrue(s.allFinished)
        val after = s.logSet(weightSet(kg = 60.0, reps = 8, setNumber = 2), 0L)
        assertEquals(s, after)
    }
}
