package com.stronk.progression

import com.stronk.data.SetTarget
import com.stronk.progression.ProgressionConstants.WEEK_MILLIS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Plan BEZ bloku (`Plan.blockLengthWeeks == null`): progresja leci ciągiem,
 * tydzień lekki nie wypada NIGDY, a numer tygodnia rośnie liniowo (bez modulo).
 *
 * To ścieżka DOPISANA obok bloków z ADR-004 — testy bloków zostają nietknięte,
 * tu sprawdzamy tylko nowe warianty funkcji przyjmujące nullowalną długość.
 */
class NoBlockProgressionTest {

    private val blockStart = 1_755_000_000_000L
    private val fullBlock = ProgressionConstants.BLOCK_LENGTH_WEEKS_DEFAULT // 6

    // ---------- długość bloku z planu ----------

    @Test
    fun `brak bloku w planie zostaje brakiem bloku dla silnika`() {
        assertNull(ProgressionEngine.fullBlockWeeks(null))
    }

    @Test
    fun `tygodnie pracy z planu dostają doklejony tydzień lekki`() {
        assertEquals(fullBlock, ProgressionEngine.fullBlockWeeks(ProgressionConstants.BLOCK_WORK_WEEKS_DEFAULT))
        assertEquals(3, ProgressionEngine.fullBlockWeeks(2))
    }

    // ---------- numer tygodnia ----------

    @Test
    fun `bez bloku tygodnie liczą się liniowo, bez zawijania`() {
        assertEquals(0, ProgressionEngine.weeksSince(blockStart, blockStart))
        assertEquals(0, ProgressionEngine.weeksSince(blockStart, blockStart + WEEK_MILLIS - 1))
        assertEquals(6, ProgressionEngine.weeksSince(blockStart, blockStart + 6 * WEEK_MILLIS))
        assertEquals(41, ProgressionEngine.weeksSince(blockStart, blockStart + 41 * WEEK_MILLIS))
    }

    @Test
    fun `czas sprzed startu planu to tydzień zerowy`() {
        assertEquals(0, ProgressionEngine.weeksSince(blockStart, blockStart - 3 * WEEK_MILLIS))
    }

    @Test
    fun `weekIndexForBlock bez bloku nie zawija, z blokiem zawija jak dotąd`() {
        val now = blockStart + 7 * WEEK_MILLIS
        assertEquals(7, ProgressionEngine.weekIndexForBlock(blockStart, now, null))
        assertEquals(
            ProgressionEngine.weekIndexInBlock(blockStart, now, fullBlock),
            ProgressionEngine.weekIndexForBlock(blockStart, now, fullBlock),
        )
    }

    // ---------- tydzień lekki ----------

    @Test
    fun `bez bloku żaden tydzień nie jest lekki`() {
        for (week in 0..20) {
            assertFalse("tydzień $week", ProgressionEngine.isLightWeekForBlock(week, null))
        }
    }

    @Test
    fun `z blokiem ostatni tydzień nadal jest lekki`() {
        assertTrue(ProgressionEngine.isLightWeekForBlock(fullBlock - 1, fullBlock))
        assertFalse(ProgressionEngine.isLightWeekForBlock(fullBlock - 2, fullBlock))
    }

    // ---------- propozycja ----------

    @Test
    fun `bez bloku propozycja nigdy nie schodzi na tydzień lekki`() {
        val plan = planWeightReps(startWeightKg = 60.0)
        val state = stateOf(lastSets = weightSets(60.0, 8), currentWeightKg = 60.0)

        // Tydzień 5 w bloku 6-tygodniowym byłby lekki; bez bloku to zwykły tydzień.
        val proposal = ProgressionEngine.proposeTargetsForBlock(
            planExercise = plan,
            state = state,
            returningFromBreak = false,
            isCompoundLeg = false,
            weekIndex = 5,
            fullBlockWeeks = null,
        )

        assertFalse(proposal.isLightWeek)
        assertEquals(62.5, proposal.weightKg!!, 1e-6)
    }

    @Test
    fun `bez bloku progresja idzie dalej także w odległym tygodniu`() {
        val plan = planWeightReps(startWeightKg = 60.0)
        val state = stateOf(lastSets = weightSets(80.0, 8), currentWeightKg = 80.0)

        val proposal = ProgressionEngine.proposeTargetsForBlock(
            planExercise = plan,
            state = state,
            returningFromBreak = false,
            isCompoundLeg = false,
            weekIndex = 137,
            fullBlockWeeks = null,
        )

        assertFalse(proposal.isLightWeek)
        assertEquals(82.5, proposal.weightKg!!, 1e-6)
        assertEquals(SetTarget.WeightReps(8), proposal.target)
    }

    @Test
    fun `wariant z blokiem zachowuje się dokładnie jak dotychczasowe API`() {
        val plan = planWeightReps(startWeightKg = 60.0)
        val state = stateOf(lastSets = weightSets(60.0, 8), currentWeightKg = 60.0)

        val viaNullable = ProgressionEngine.proposeTargetsForBlock(
            planExercise = plan,
            state = state,
            returningFromBreak = false,
            isCompoundLeg = false,
            weekIndex = fullBlock - 1,
            fullBlockWeeks = fullBlock,
        )
        val viaBlock = ProgressionEngine.proposeTargets(
            planExercise = plan,
            state = state,
            returningFromBreak = false,
            isCompoundLeg = false,
            weekIndexInBlock = fullBlock - 1,
            blockLengthWeeks = fullBlock,
        )

        assertTrue(viaNullable.isLightWeek)
        assertEquals(viaBlock, viaNullable)
    }
}
