package com.stronk.progression

import com.stronk.progression.ProgressionConstants.WEEK_MILLIS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Testy liczenia pozycji tygodnia w bloku i wykrywania tygodnia lekkiego (ADR-004 reguła 3). */
class BlockWeekTest {

    private val blockStart = 1_755_000_000_000L
    private val len = ProgressionConstants.BLOCK_LENGTH_WEEKS_DEFAULT // 6

    @Test
    fun `pierwszy tydzień bloku ma indeks 0`() {
        assertEquals(0, ProgressionEngine.weekIndexInBlock(blockStart, blockStart, len))
        assertEquals(0, ProgressionEngine.weekIndexInBlock(blockStart, blockStart + WEEK_MILLIS - 1, len))
    }

    @Test
    fun `szósty tydzień bloku ma indeks 5 i jest lekki`() {
        val index = ProgressionEngine.weekIndexInBlock(blockStart, blockStart + 5 * WEEK_MILLIS, len)
        assertEquals(5, index)
        assertTrue(ProgressionEngine.isLightWeek(index, len))
    }

    @Test
    fun `tygodnie pracy nie są lekkie`() {
        for (week in 0 until len - 1) {
            assertFalse("tydzień $week", ProgressionEngine.isLightWeek(week, len))
        }
    }

    @Test
    fun `po tygodniu lekkim zaczyna się nowy blok od indeksu 0`() {
        assertEquals(0, ProgressionEngine.weekIndexInBlock(blockStart, blockStart + 6 * WEEK_MILLIS, len))
        assertEquals(1, ProgressionEngine.weekIndexInBlock(blockStart, blockStart + 7 * WEEK_MILLIS, len))
        // drugi blok też kończy się tygodniem lekkim
        assertEquals(5, ProgressionEngine.weekIndexInBlock(blockStart, blockStart + 11 * WEEK_MILLIS, len))
    }

    @Test
    fun `czas sprzed startu bloku traktowany jak pierwszy tydzień`() {
        assertEquals(0, ProgressionEngine.weekIndexInBlock(blockStart, blockStart - 3 * WEEK_MILLIS, len))
    }

    @Test
    fun `niedodatnia długość bloku nie wybucha i nie daje tygodnia lekkiego`() {
        assertEquals(0, ProgressionEngine.weekIndexInBlock(blockStart, blockStart + 10 * WEEK_MILLIS, 0))
        assertFalse(ProgressionEngine.isLightWeek(0, 0))
    }

    @Test
    fun `blok jednotygodniowy nigdy nie ma tygodnia lekkiego`() {
        assertFalse(ProgressionEngine.isLightWeek(0, 1))
    }

    @Test
    fun `niestandardowa długość bloku - lekki jest ostatni tydzień`() {
        assertTrue(ProgressionEngine.isLightWeek(3, 4))
        assertFalse(ProgressionEngine.isLightWeek(2, 4))
    }
}
