package com.stronk.progression

import com.stronk.data.GoalDefaults
import com.stronk.data.TrainingGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Kalibracja z treningu testowego: Epley, procent celu, zaokrąglenie do 2,5 kg. */
class CalibrationTest {

    // --- estymacja 1RM (Epley) ---

    @Test
    fun `Epley dla znanej serii testowej`() {
        // 40 * (1 + 10/30) = 53.333...
        assertEquals(53.33, Calibration.estimateOneRepMax(40.0, 10), 0.01)
    }

    @Test
    fun `seria pojedyncza jest maksem`() {
        assertEquals(100.0, Calibration.estimateOneRepMax(100.0, 1), 1e-9)
    }

    @Test
    fun `wiecej powtorzen to wyzsze estymowane 1RM`() {
        assertEquals(70.0, Calibration.estimateOneRepMax(60.0, 5), 1e-9)
        assertTrue(
            Calibration.estimateOneRepMax(60.0, 8) > Calibration.estimateOneRepMax(60.0, 5),
        )
    }

    @Test
    fun `zle wejscie leci wyjatkiem`() {
        assertThrows(IllegalArgumentException::class.java) { Calibration.estimateOneRepMax(0.0, 5) }
        assertThrows(IllegalArgumentException::class.java) { Calibration.estimateOneRepMax(-40.0, 5) }
        assertThrows(IllegalArgumentException::class.java) { Calibration.estimateOneRepMax(40.0, 0) }
        assertThrows(IllegalArgumentException::class.java) { Calibration.estimateOneRepMax(40.0, -3) }
        assertThrows(IllegalArgumentException::class.java) { Calibration.workingWeightKg(0.0, 10, TrainingGoal.MASS) }
        assertThrows(IllegalArgumentException::class.java) { Calibration.workingWeightKg(40.0, 0, TrainingGoal.MASS) }
    }

    // --- ciężar roboczy per cel ---

    @Test
    fun `ciezar roboczy dla sily to 80 procent 1RM zaokraglone do 2,5`() {
        // e1RM 53.33 * 0.80 = 42.67 → 42.5
        assertEquals(42.5, Calibration.workingWeightKg(40.0, 10, TrainingGoal.STRENGTH), 1e-9)
    }

    @Test
    fun `ciezar roboczy dla masy to 72,5 procent 1RM zaokraglone do 2,5`() {
        // e1RM 53.33 * 0.725 = 38.67 → 37.5
        assertEquals(37.5, Calibration.workingWeightKg(40.0, 10, TrainingGoal.MASS), 1e-9)
    }

    @Test
    fun `ciezar roboczy dla powrotu do formy to 62,5 procent 1RM zaokraglone do 2,5`() {
        // e1RM 53.33 * 0.625 = 33.33 → 32.5
        assertEquals(32.5, Calibration.workingWeightKg(40.0, 10, TrainingGoal.RETURN_TO_FORM), 1e-9)
    }

    @Test
    fun `cel niewybrany liczy sie jak fallback`() {
        assertEquals(
            Calibration.workingWeightKg(40.0, 10, TrainingGoal.MASS),
            Calibration.workingWeightKg(40.0, 10, null),
            1e-9,
        )
        assertEquals(GoalDefaults.FALLBACK.calibrationPercent, GoalDefaults.calibrationPercentFor(null), 1e-9)
    }

    @Test
    fun `im ciezszy cel tym wyzszy ciezar roboczy z tego samego testu`() {
        val strength = Calibration.workingWeightKg(60.0, 8, TrainingGoal.STRENGTH)
        val mass = Calibration.workingWeightKg(60.0, 8, TrainingGoal.MASS)
        val returnToForm = Calibration.workingWeightKg(60.0, 8, TrainingGoal.RETURN_TO_FORM)
        assertTrue("$strength > $mass", strength > mass)
        assertTrue("$mass > $returnToForm", mass > returnToForm)
    }

    @Test
    fun `test na jednym powtorzeniu daje wprost procent celu`() {
        // e1RM = 100 → 100 * 0.80 = 80
        assertEquals(80.0, Calibration.workingWeightKg(100.0, 1, TrainingGoal.STRENGTH), 1e-9)
    }

    @Test
    fun `wynik nie schodzi ponizej najmniejszego kroku talerzy`() {
        // e1RM 1 * (1 + 2/30) = 1.07 → * 0.625 = 0.67 → podłoga 2.5
        assertEquals(2.5, Calibration.workingWeightKg(1.0, 2, TrainingGoal.RETURN_TO_FORM), 1e-9)
    }

    @Test
    fun `ciezar roboczy zawsze lezy na siatce 2,5 kg`() {
        val weights = listOf(20.0, 37.5, 42.0, 63.7, 105.0)
        TrainingGoal.entries.forEach { goal ->
            weights.forEach { w ->
                (1..15).forEach { reps ->
                    val result = Calibration.workingWeightKg(w, reps, goal)
                    assertEquals(
                        "$goal $w kg x $reps → $result",
                        0.0,
                        result % ProgressionConstants.WEIGHT_ROUNDING_KG,
                        1e-9,
                    )
                    assertTrue("$goal $w kg x $reps → $result", result >= ProgressionConstants.WEIGHT_ROUNDING_KG)
                }
            }
        }
    }

    // --- wiarygodność estymacji ---

    @Test
    fun `granice wiarygodnego zakresu powtorzen`() {
        assertEquals(2..12, Calibration.RELIABLE_REPS)
        assertTrue(Calibration.isReliable(2))
        assertTrue(Calibration.isReliable(12))
        assertFalse(Calibration.isReliable(1))
        assertFalse(Calibration.isReliable(13))
    }

    @Test
    fun `powtorzenia spoza zakresu nadal licza sie normalnie`() {
        // ostrzeżenie jest sprawą UI — silnik nie blokuje
        assertTrue(Calibration.workingWeightKg(40.0, 20, TrainingGoal.MASS) > 0.0)
        assertTrue(Calibration.workingWeightKg(40.0, 1, TrainingGoal.MASS) > 0.0)
    }
}
