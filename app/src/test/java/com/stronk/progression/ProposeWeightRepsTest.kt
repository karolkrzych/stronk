package com.stronk.progression

import com.stronk.data.SetTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Propozycje dla WEIGHT_REPS: overload, deload reaktywny, tydzień lekki, ramp-up, zaokrąglenia. */
class ProposeWeightRepsTest {

    private val plan = planWeightReps(startWeightKg = 60.0, reps = 8, sets = 3)

    // --- pierwszy trening (brak historii) ---

    @Test
    fun `brak stanu - proponuje ciężar startowy z planu`() {
        val p = propose(plan)
        assertEquals(60.0, p.weightKg!!, 1e-9)
        assertEquals(SetTarget.WeightReps(8), p.target)
        assertEquals(3, p.sets)
        assertFalse(p.isRampUp)
        assertFalse(p.isReactiveDeload)
        assertFalse(p.isLightWeek)
    }

    @Test
    fun `brak stanu po przerwie - ramp-up startuje od okolic 55 procent`() {
        val p = propose(plan, returningFromBreak = true)
        // 60 * 0.55 = 33 → zaokrąglone do 2.5 → 32.5
        assertEquals(32.5, p.weightKg!!, 1e-9)
        assertTrue(p.isRampUp)
    }

    @Test
    fun `brak stanu i brak ciężaru startowego - propozycja bez ciężaru`() {
        val p = propose(planWeightReps(startWeightKg = null))
        assertNull(p.weightKg)
        assertEquals(SetTarget.WeightReps(8), p.target)
    }

    // --- reguła 1: progressive overload ---

    @Test
    fun `zaliczony trening - plus 2,5 kg`() {
        val state = stateOf(lastSets = weightSets(100.0, 8), failStreak = 0, currentWeightKg = 100.0)
        val p = propose(plan, state)
        assertEquals(102.5, p.weightKg!!, 1e-9)
        assertEquals(SetTarget.WeightReps(8), p.target) // cel powtórzeń zostaje z planu
    }

    @Test
    fun `zaliczony trening compound-leg - plus 5 kg`() {
        val state = stateOf(lastSets = weightSets(100.0, 8), failStreak = 0, currentWeightKg = 100.0)
        val p = propose(plan, state, isCompoundLeg = true)
        assertEquals(105.0, p.weightKg!!, 1e-9)
    }

    @Test
    fun `stan bez historii serii - bez progresji, ciężar bieżący`() {
        val state = stateOf(lastSets = emptyList(), failStreak = 0, currentWeightKg = 50.0)
        val p = propose(plan, state)
        assertEquals(50.0, p.weightKg!!, 1e-9)
    }

    @Test
    fun `same serie rozgrzewkowe w historii - bez progresji`() {
        val state = stateOf(
            lastSets = listOf(weightSet(40.0, 10, warmup = true)),
            failStreak = 0,
            currentWeightKg = 50.0,
        )
        val p = propose(plan, state)
        assertEquals(50.0, p.weightKg!!, 1e-9)
    }

    // --- reguła 2: deload reaktywny ---

    @Test
    fun `jedna porażka - powtórka tego samego ciężaru`() {
        val state = stateOf(lastSets = weightSets(100.0, 6), failStreak = 1, currentWeightKg = 100.0)
        val p = propose(plan, state)
        assertEquals(100.0, p.weightKg!!, 1e-9)
        assertFalse(p.isReactiveDeload)
    }

    @Test
    fun `dwie porażki z rzędu - deload minus 10 procent`() {
        val state = stateOf(lastSets = weightSets(100.0, 6), failStreak = 2, currentWeightKg = 100.0)
        val p = propose(plan, state)
        assertEquals(90.0, p.weightKg!!, 1e-9)
        assertTrue(p.isReactiveDeload)
    }

    @Test
    fun `deload zaokrągla do 2,5 kg`() {
        val state = stateOf(lastSets = weightSets(102.5, 6), failStreak = 2, currentWeightKg = 102.5)
        val p = propose(plan, state)
        // 102.5 * 0.9 = 92.25 → 92.5
        assertEquals(92.5, p.weightKg!!, 1e-9)
    }

    @Test
    fun `deload nie schodzi poniżej 2,5 kg`() {
        val state = stateOf(lastSets = weightSets(2.5, 6), failStreak = 2, currentWeightKg = 2.5)
        val p = propose(plan, state)
        assertEquals(2.5, p.weightKg!!, 1e-9)
    }

    // --- reguła 3: tydzień lekki ---

    @Test
    fun `tydzień lekki - minus 40 procent od propozycji, cel powtórzeń bez zmian`() {
        val state = stateOf(lastSets = weightSets(100.0, 8), failStreak = 0, currentWeightKg = 100.0)
        val p = propose(plan, state, weekIndexInBlock = 5, blockLengthWeeks = 6)
        // baza po progresji 102.5 → * 0.6 = 61.5 → zaokrąglone 62.5
        assertEquals(62.5, p.weightKg!!, 1e-9)
        assertTrue(p.isLightWeek)
        assertEquals(SetTarget.WeightReps(8), p.target)
    }

    @Test
    fun `tydzień pracy nie dostaje modyfikatora lekkiego`() {
        val state = stateOf(lastSets = weightSets(100.0, 8), failStreak = 0, currentWeightKg = 100.0)
        val p = propose(plan, state, weekIndexInBlock = 4, blockLengthWeeks = 6)
        assertEquals(102.5, p.weightKg!!, 1e-9)
        assertFalse(p.isLightWeek)
    }

    // --- reguła 4: ramp-up ---

    @Test
    fun `ramp-up - podwojony przyrost przed dogonieniem poziomu`() {
        val state = stateOf(lastSets = weightSets(40.0, 8), failStreak = 0, currentWeightKg = 40.0)
        val p = propose(plan, state, returningFromBreak = true)
        // 40 + 2.5*2 = 45, wciąż poniżej 60
        assertEquals(45.0, p.weightKg!!, 1e-9)
        assertTrue(p.isRampUp)
    }

    @Test
    fun `ramp-up compound-leg - przyrost 10 kg`() {
        val state = stateOf(lastSets = weightSets(40.0, 8), failStreak = 0, currentWeightKg = 40.0)
        val p = propose(plan, state, returningFromBreak = true, isCompoundLeg = true)
        assertEquals(50.0, p.weightKg!!, 1e-9)
    }

    @Test
    fun `ramp-up nie przeskakuje poziomu - kap na ciężarze z planu`() {
        val state = stateOf(lastSets = weightSets(57.5, 8), failStreak = 0, currentWeightKg = 57.5)
        val p = propose(plan, state, returningFromBreak = true)
        // 57.5 + 5 = 62.5 → kap 60
        assertEquals(60.0, p.weightKg!!, 1e-9)
        assertTrue(p.isRampUp)
    }

    @Test
    fun `po dogonieniu poziomu - zwykła progresja mimo flagi powrotu`() {
        val state = stateOf(lastSets = weightSets(60.0, 8), failStreak = 0, currentWeightKg = 60.0)
        val p = propose(plan, state, returningFromBreak = true)
        assertEquals(62.5, p.weightKg!!, 1e-9)
        assertFalse(p.isRampUp)
    }

    @Test
    fun `ramp-up bez ciężaru startowego w planie - brak punktu odniesienia, zwykła progresja`() {
        val noStart = planWeightReps(startWeightKg = null)
        val state = stateOf(lastSets = weightSets(40.0, 8), failStreak = 0, currentWeightKg = 40.0)
        val p = propose(noStart, state, returningFromBreak = true)
        assertEquals(42.5, p.weightKg!!, 1e-9)
        assertFalse(p.isRampUp)
    }

    // --- wyłączona progresja ---

    @Test
    fun `progressionEnabled false - plan 1 do 1, bez modyfikatorów nawet w tygodniu lekkim`() {
        val disabled = planWeightReps(startWeightKg = 60.0, progressionEnabled = false)
        val state = stateOf(lastSets = weightSets(100.0, 8), failStreak = 2, currentWeightKg = 100.0)
        val p = propose(disabled, state, weekIndexInBlock = 5, blockLengthWeeks = 6)
        assertEquals(60.0, p.weightKg!!, 1e-9)
        assertFalse(p.isLightWeek)
        assertFalse(p.isReactiveDeload)
        assertFalse(p.isRampUp)
    }
}
