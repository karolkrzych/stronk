package com.stronk.progression

import com.stronk.data.ExerciseState
import com.stronk.data.SetLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Scenariusze wielotreningowe — pełna pętla proposeTargets → trening →
 * updateStateAfterWorkout, tak jak będzie chodzić w apce.
 */
class ProgressionScenarioTest {

    private var clock = 1_755_000_000_000L

    /** Symuluje trening: bierze propozycję i loguje serie wykonane dokładnie na cel (albo słabiej). */
    private fun performWorkout(
        state: ExerciseState?,
        proposal: ExerciseProposal,
        actualKg: Double? = proposal.weightKg,
        actualReps: Int? = null,
    ): ExerciseState {
        clock += 1
        val target = proposal.target as com.stronk.data.SetTarget.WeightReps
        val logged: List<SetLog> = (1..proposal.sets).map { n ->
            weightSet(kg = actualKg ?: 0.0, reps = actualReps ?: target.reps, setNumber = n)
        }
        return ProgressionEngine.updateStateAfterWorkout(state, proposal, logged, clock)
    }

    @Test
    fun `deload po dwóch porażkach i budowanie od nowa`() {
        val plan = planWeightReps(startWeightKg = 100.0, reps = 8)

        // trening 1: start 100, zaliczony
        var state = performWorkout(null, propose(plan))
        assertEquals(100.0, state.currentWeightKg!!, 1e-9)

        // trening 2: propozycja 102.5, porażka (6 powtórzeń)
        var p = propose(plan, state)
        assertEquals(102.5, p.weightKg!!, 1e-9)
        state = performWorkout(state, p, actualReps = 6)
        assertEquals(1, state.failStreak)

        // trening 3: powtórka 102.5, znowu porażka
        p = propose(plan, state)
        assertEquals(102.5, p.weightKg!!, 1e-9)
        state = performWorkout(state, p, actualReps = 7)
        assertEquals(2, state.failStreak)

        // trening 4: deload −10% → 92.5, zaliczony
        p = propose(plan, state)
        assertTrue(p.isReactiveDeload)
        assertEquals(92.5, p.weightKg!!, 1e-9)
        state = performWorkout(state, p)
        assertEquals(0, state.failStreak)
        assertEquals(92.5, state.currentWeightKg!!, 1e-9)

        // trening 5: budowanie od nowa — 95
        p = propose(plan, state)
        assertEquals(95.0, p.weightKg!!, 1e-9)
        assertFalse(p.isReactiveDeload)
    }

    @Test
    fun `ramp-up dogania poziom i przechodzi w zwykłą progresję`() {
        val plan = planWeightReps(startWeightKg = 60.0, reps = 8)
        var state: ExerciseState? = null
        val proposedWeights = mutableListOf<Double>()

        // 8 zaliczonych treningów z flagą powrotu po przerwie
        repeat(8) {
            val p = propose(plan, state, returningFromBreak = true)
            proposedWeights += p.weightKg!!
            state = performWorkout(state, p)
        }

        // 32.5 → 37.5 → 42.5 → 47.5 → 52.5 → 57.5 → 60 (kap) → 62.5 (zwykła progresja)
        assertEquals(listOf(32.5, 37.5, 42.5, 47.5, 52.5, 57.5, 60.0, 62.5), proposedWeights)

        // po dogonieniu poziomu flaga ramp-up gaśnie
        val afterCatchUp = propose(plan, state, returningFromBreak = true)
        assertFalse(afterCatchUp.isRampUp)
        assertEquals(65.0, afterCatchUp.weightKg!!, 1e-9)
    }

    @Test
    fun `przejście przez tydzień lekki - nowy blok startuje wyżej niż poprzedni`() {
        val plan = planWeightReps(startWeightKg = 100.0, reps = 8)

        // ostatni tydzień pracy: zaliczony trening na 100
        var state = performWorkout(null, propose(plan, state = null, weekIndexInBlock = 4))
        assertEquals(100.0, state.currentWeightKg!!, 1e-9)

        // tydzień lekki: −40% od propozycji (102.5 → 61.5 → 62.5)
        val light = propose(plan, state, weekIndexInBlock = 5)
        assertTrue(light.isLightWeek)
        assertEquals(62.5, light.weightKg!!, 1e-9)

        // trening lekki nie zmienia stanu progresji
        state = performWorkout(state, light)
        assertEquals(100.0, state.currentWeightKg!!, 1e-9)
        assertEquals(0, state.failStreak)

        // nowy blok: propozycja wyżej niż szczyt poprzedniego bloku
        val newBlock = propose(plan, state, weekIndexInBlock = 0)
        assertFalse(newBlock.isLightWeek)
        assertEquals(102.5, newBlock.weightKg!!, 1e-9)
    }

    @Test
    fun `porażka przed tygodniem lekkim nie znika - po bloku wciąż powtórka`() {
        val plan = planWeightReps(startWeightKg = 100.0, reps = 8)

        // porażka w tygodniu pracy
        var state = performWorkout(null, propose(plan), actualReps = 5)
        assertEquals(1, state.failStreak)

        // tydzień lekki przechodzi bez wpływu na streak
        state = performWorkout(state, propose(plan, state, weekIndexInBlock = 5))
        assertEquals(1, state.failStreak)

        // po tygodniu lekkim: powtórka tego samego ciężaru, nie progresja
        val after = propose(plan, state, weekIndexInBlock = 0)
        assertEquals(100.0, after.weightKg!!, 1e-9)
    }

    @Test
    fun `pełny cykl z helperem tygodni - blok 6-tygodniowy`() {
        val blockStart = clock
        val len = ProgressionConstants.BLOCK_LENGTH_WEEKS_DEFAULT
        val plan = planWeightReps(startWeightKg = 60.0, reps = 8)

        // tydzień 0..4 to praca, tydzień 5 lekki — licząc prosto z millis
        val week4 = ProgressionEngine.weekIndexInBlock(blockStart, blockStart + 4 * ProgressionConstants.WEEK_MILLIS, len)
        val week5 = ProgressionEngine.weekIndexInBlock(blockStart, blockStart + 5 * ProgressionConstants.WEEK_MILLIS, len)
        assertFalse(ProgressionEngine.isLightWeek(week4, len))
        assertTrue(ProgressionEngine.isLightWeek(week5, len))

        val p = propose(plan, weekIndexInBlock = week5, blockLengthWeeks = len)
        assertTrue(p.isLightWeek)
        // 60 * 0.6 = 36 → 35 (krok 2.5)
        assertEquals(35.0, p.weightKg!!, 1e-9)
    }
}
