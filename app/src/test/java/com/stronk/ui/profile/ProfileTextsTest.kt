package com.stronk.ui.profile

import com.stronk.data.GoalDefaults
import com.stronk.data.StressLevel
import com.stronk.data.TrainingGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Teksty profilu — czysta warstwa słów, więc testowalna bez Compose'a. */
class ProfileTextsTest {

    @Test
    fun `nazwa stawu zaczyna się wielką literą`() {
        assertEquals("Kolano", ProfileTexts.jointTitle("knee"))
        assertEquals("Bark", ProfileTexts.jointTitle("shoulder"))
    }

    @Test
    fun `oferujemy dokładnie dwa poziomy ograniczenia`() {
        assertEquals(listOf(StressLevel.MEDIUM, StressLevel.LOW), ProfileTexts.SEVERITY_OPTIONS)
    }

    @Test
    fun `poziomy ograniczenia mają różne opisy`() {
        ProfileTexts.SEVERITY_OPTIONS.forEach { level ->
            assertTrue(ProfileTexts.severityTitle(level).isNotBlank())
            assertTrue(ProfileTexts.severityDescription(level).isNotBlank())
            assertTrue(ProfileTexts.severityRowText(level).isNotBlank())
        }
        assertNotEquals(
            ProfileTexts.severityTitle(StressLevel.LOW),
            ProfileTexts.severityTitle(StressLevel.MEDIUM),
        )
        assertNotEquals(
            ProfileTexts.severityRowText(StressLevel.LOW),
            ProfileTexts.severityRowText(StressLevel.MEDIUM),
        )
    }

    @Test
    fun `przerwa pełnominutowa pokazuje się w minutach`() {
        assertEquals("3", ProfileTexts.restValue(180))
        assertEquals("min", ProfileTexts.restUnit(180))
        assertEquals("90", ProfileTexts.restValue(90))
        assertEquals("s", ProfileTexts.restUnit(90))
        assertEquals("75 s przerwy", ProfileTexts.restChip(75))
    }

    @Test
    fun `każdy cel ma komplet liczb do pokazania przy wyborze`() {
        TrainingGoal.entries.forEach { goal ->
            val params = GoalDefaults.forGoal(goal)
            assertTrue(GoalDefaults.repRangeLabel(goal).isNotBlank())
            assertTrue(ProfileTexts.setsChip(params.defaultSets).isNotBlank())
            assertTrue(ProfileTexts.restChip(params.restSeconds).isNotBlank())
        }
        assertEquals("1 seria", ProfileTexts.setsChip(1))
        assertEquals("4 serie", ProfileTexts.setsChip(4))
    }

    @Test
    fun `podpowiedź sprzętu rozróżnia pusty wybór od zaznaczonego`() {
        assertTrue(ProfileTexts.equipmentHint(0).contains("wszystkie"))
        assertTrue(ProfileTexts.equipmentHint(3).contains("3"))
    }
}
