package com.stronk.progression

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Heurystyka compound-leg (+5 kg zamiast +2.5 kg): mechanic=compound + partia główna z nóg. */
class CompoundLegTest {

    @Test
    fun `przysiad - compound z czworogłowymi - kwalifikuje się`() {
        assertTrue(ProgressionEngine.isCompoundLeg(exerciseOf("compound", listOf("quadriceps"))))
    }

    @Test
    fun `martwy ciąg - compound z dwugłowymi - kwalifikuje się`() {
        assertTrue(ProgressionEngine.isCompoundLeg(exerciseOf("compound", listOf("hamstrings", "glutes"))))
    }

    @Test
    fun `wyciskanie - compound bez nóg - nie kwalifikuje się`() {
        assertFalse(ProgressionEngine.isCompoundLeg(exerciseOf("compound", listOf("chest"))))
    }

    @Test
    fun `prostowanie nóg - izolacja nóg - nie kwalifikuje się`() {
        assertFalse(ProgressionEngine.isCompoundLeg(exerciseOf("isolation", listOf("quadriceps"))))
    }

    @Test
    fun `brak mechanic w danych - nie kwalifikuje się`() {
        assertFalse(ProgressionEngine.isCompoundLeg(exerciseOf(null, listOf("quadriceps"))))
    }

    @Test
    fun `wszystkie partie nóg z datasetu są rozpoznawane`() {
        for (muscle in ProgressionConstants.LEG_MUSCLES) {
            assertTrue(muscle, ProgressionEngine.isCompoundLeg(exerciseOf("compound", listOf(muscle))))
        }
    }
}
