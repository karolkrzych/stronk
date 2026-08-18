package com.stronk.ui.schedule

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Testy czystych funkcji kalendarza tygodnia i generacji wpisów. */
class WeekPlannerTest {

    // 2026-08-17 = poniedziałek (tydzień 17–23 sierpnia).
    private val monday = LocalDate.of(2026, 8, 17)

    // ---------- weekStartOf ----------

    @Test
    fun `weekStartOf poniedzialku zwraca ten sam dzien`() {
        assertEquals(monday, weekStartOf(monday))
    }

    @Test
    fun `weekStartOf niedzieli cofa do poniedzialku tego samego tygodnia ISO`() {
        assertEquals(monday, weekStartOf(LocalDate.of(2026, 8, 23)))
    }

    @Test
    fun `weekStartOf srodka tygodnia cofa do poniedzialku`() {
        assertEquals(monday, weekStartOf(LocalDate.of(2026, 8, 19)))
    }

    // ---------- weekLabel ----------

    @Test
    fun `weekLabel w jednym miesiacu sklada zakres bez powtorzenia miesiaca`() {
        assertEquals("17–23 sierpnia", weekLabel(monday, monday))
    }

    @Test
    fun `weekLabel na przelomie miesiecy podaje oba miesiace`() {
        // 2026-07-27 (pn) – 2026-08-02 (nd).
        val label = weekLabel(LocalDate.of(2026, 7, 27), monday)
        assertEquals("27 lipca – 2 sierpnia", label)
    }

    @Test
    fun `weekLabel w innym roku dopisuje rok`() {
        // Tydzień 2025-08-18 (pn) – 2025-08-24 oglądany z 2026 roku.
        val label = weekLabel(LocalDate.of(2025, 8, 18), monday)
        assertEquals("18–24 sierpnia 2025", label)
    }

    @Test
    fun `weekLabel na przelomie lat podaje oba roczniki`() {
        // 2025-12-29 (pn) – 2026-01-04 (nd) oglądane z 2026 roku.
        val label = weekLabel(LocalDate.of(2025, 12, 29), monday)
        assertEquals("29 grudnia 2025 – 4 stycznia", label)
    }

    // ---------- defaultAssignments ----------

    @Test
    fun `defaultAssignments dla planu 3-dniowego to pn-sr-pt`() {
        val assignments = defaultAssignments(3)
        assertEquals(
            mapOf(
                DayOfWeek.MONDAY to 0,
                DayOfWeek.WEDNESDAY to 1,
                DayOfWeek.FRIDAY to 2,
            ),
            assignments,
        )
    }

    @Test
    fun `defaultAssignments dla zera dni jest puste`() {
        assertEquals(emptyMap<DayOfWeek, Int>(), defaultAssignments(0))
    }

    @Test
    fun `defaultAssignments dla planu dluzszego niz tydzien przypisuje pierwsze 7 dni`() {
        val assignments = defaultAssignments(9)
        assertEquals(7, assignments.size)
        assertEquals((0..6).toSet(), assignments.values.toSet())
    }

    // ---------- generatePlannedSlots ----------

    @Test
    fun `generacja od poniedzialku daje tyle wpisow ile dni razy tygodnie`() {
        val slots = generatePlannedSlots(
            assignments = mapOf(DayOfWeek.MONDAY to 0, DayOfWeek.THURSDAY to 1),
            startDate = monday,
            weeks = 4,
        )
        assertEquals(8, slots.size)
        assertEquals(monday, slots.first().date)
        // Ostatni czwartek: 3 tygodnie po pierwszym (2026-09-10).
        assertEquals(LocalDate.of(2026, 9, 10), slots.last().date)
    }

    @Test
    fun `okno kroczace liczy tygodnie od daty startu a nie kalendarzowe`() {
        // Start w środę: poniedziałkowe treningi zaczynają się od NASTĘPNEGO
        // poniedziałku, ale okno siega środy za 4 tygodnie.
        val wednesday = LocalDate.of(2026, 8, 19)
        val slots = generatePlannedSlots(
            assignments = mapOf(DayOfWeek.MONDAY to 0),
            startDate = wednesday,
            weeks = 4,
        )
        assertEquals(4, slots.size)
        assertEquals(LocalDate.of(2026, 8, 24), slots.first().date)
        assertEquals(LocalDate.of(2026, 9, 14), slots.last().date)
    }

    @Test
    fun `data sprzed startu nie dostaje wpisu`() {
        val wednesday = LocalDate.of(2026, 8, 19)
        val slots = generatePlannedSlots(
            assignments = mapOf(DayOfWeek.MONDAY to 0, DayOfWeek.WEDNESDAY to 1),
            startDate = wednesday,
            weeks = 1,
        )
        // Tylko środa startowa i poniedziałek 24-go — pon. 17-go jest przed startem.
        assertEquals(
            listOf(
                PlannedSlot(wednesday, 1),
                PlannedSlot(LocalDate.of(2026, 8, 24), 0),
            ),
            slots,
        )
    }

    @Test
    fun `zajete dni sa pomijane`() {
        val slots = generatePlannedSlots(
            assignments = mapOf(DayOfWeek.MONDAY to 0),
            startDate = monday,
            weeks = 2,
            occupiedDates = setOf(monday),
        )
        assertEquals(listOf(PlannedSlot(LocalDate.of(2026, 8, 24), 0)), slots)
    }

    @Test
    fun `jeden dzien planu moze trafic na kilka dni tygodnia`() {
        // Full body 3×/tydz.: ten sam dayIndex w pn, śr i pt.
        val slots = generatePlannedSlots(
            assignments = mapOf(
                DayOfWeek.MONDAY to 0,
                DayOfWeek.WEDNESDAY to 0,
                DayOfWeek.FRIDAY to 0,
            ),
            startDate = monday,
            weeks = 1,
        )
        assertEquals(3, slots.size)
        assertTrue(slots.all { it.dayIndex == 0 })
    }

    @Test
    fun `wynik jest posortowany chronologicznie`() {
        val slots = generatePlannedSlots(
            assignments = mapOf(DayOfWeek.FRIDAY to 2, DayOfWeek.MONDAY to 0),
            startDate = monday,
            weeks = 2,
        )
        assertEquals(slots.sortedBy { it.date }, slots)
    }

    @Test
    fun `zero lub ujemna liczba tygodni daje pusta liste`() {
        val assignments = mapOf(DayOfWeek.MONDAY to 0)
        assertEquals(emptyList<PlannedSlot>(), generatePlannedSlots(assignments, monday, weeks = 0))
        assertEquals(emptyList<PlannedSlot>(), generatePlannedSlots(assignments, monday, weeks = -1))
    }

    @Test
    fun `puste przypisania daja pusta liste`() {
        assertEquals(
            emptyList<PlannedSlot>(),
            generatePlannedSlots(emptyMap(), monday, weeks = 4),
        )
    }
}
