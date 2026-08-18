package com.stronk.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalDefaultsTest {

    @Test
    fun `cel niewybrany spada na fallback`() {
        assertEquals(GoalDefaults.FALLBACK, GoalDefaults.forGoal(null))
        assertEquals(GoalDefaults.MASS, GoalDefaults.forGoal(null))
    }

    @Test
    fun `parametry sily masy i powrotu do formy`() {
        assertEquals(4..6, GoalDefaults.forGoal(TrainingGoal.STRENGTH).repRange)
        assertEquals(4, GoalDefaults.setsFor(TrainingGoal.STRENGTH))
        assertEquals(180, GoalDefaults.restSecondsFor(TrainingGoal.STRENGTH))

        assertEquals(8..12, GoalDefaults.forGoal(TrainingGoal.MASS).repRange)
        assertEquals(3, GoalDefaults.setsFor(TrainingGoal.MASS))
        assertEquals(90, GoalDefaults.restSecondsFor(TrainingGoal.MASS))

        assertEquals(10..15, GoalDefaults.forGoal(TrainingGoal.RETURN_TO_FORM).repRange)
        assertEquals(3, GoalDefaults.setsFor(TrainingGoal.RETURN_TO_FORM))
        assertEquals(75, GoalDefaults.restSecondsFor(TrainingGoal.RETURN_TO_FORM))
    }

    @Test
    fun `procenty kalibracji per cel`() {
        assertEquals(0.80, GoalDefaults.calibrationPercentFor(TrainingGoal.STRENGTH), 1e-9)
        assertEquals(0.725, GoalDefaults.calibrationPercentFor(TrainingGoal.MASS), 1e-9)
        assertEquals(0.625, GoalDefaults.calibrationPercentFor(TrainingGoal.RETURN_TO_FORM), 1e-9)
        assertEquals(GoalDefaults.FALLBACK.calibrationPercent, GoalDefaults.calibrationPercentFor(null), 1e-9)
    }

    @Test
    fun `mniej powtorzen w celu to wyzszy procent 1RM`() {
        val byPercent = TrainingGoal.entries.sortedBy { GoalDefaults.calibrationPercentFor(it) }
        val byReps = TrainingGoal.entries.sortedByDescending { GoalDefaults.repsFor(it) }
        assertEquals(byReps, byPercent)
        TrainingGoal.entries.forEach { goal ->
            val percent = GoalDefaults.calibrationPercentFor(goal)
            assertTrue("$goal: $percent", percent > 0.0 && percent < 1.0)
        }
    }

    @Test
    fun `cwiczenia akcesoryjne dostaja wiecej powtorzen niz zlozone`() {
        TrainingGoal.entries.forEach { goal ->
            assertTrue(
                "accessory > default dla $goal",
                GoalDefaults.repsFor(goal, accessory = true) > GoalDefaults.repsFor(goal),
            )
        }
    }

    @Test
    fun `domyslne powtorzenia mieszcza sie w zakresie celu`() {
        TrainingGoal.entries.forEach { goal ->
            assertTrue(goal.toString(), GoalDefaults.repsInRange(goal, GoalDefaults.repsFor(goal)))
        }
    }

    @Test
    fun `clampReps wciaga wartosci spoza zakresu`() {
        assertEquals(6, GoalDefaults.clampReps(TrainingGoal.STRENGTH, 20))
        assertEquals(4, GoalDefaults.clampReps(TrainingGoal.STRENGTH, 1))
        assertEquals(12, GoalDefaults.clampReps(TrainingGoal.MASS, 12))
        assertEquals(12, GoalDefaults.clampReps(TrainingGoal.MASS, 30))
        assertFalse(GoalDefaults.repsInRange(TrainingGoal.MASS, 20))
    }

    @Test
    fun `etykiety celu sa po polsku i unikalne`() {
        val labels = TrainingGoal.entries.map { GoalDefaults.label(it) }
        assertEquals(labels.size, labels.toSet().size)
        assertEquals("Siła", GoalDefaults.label(TrainingGoal.STRENGTH))
        assertEquals("4–6", GoalDefaults.repRangeLabel(TrainingGoal.STRENGTH))
        assertEquals("8–12", GoalDefaults.repRangeLabel(null))
        TrainingGoal.entries.forEach { assertTrue(GoalDefaults.description(it).isNotBlank()) }
    }
}
