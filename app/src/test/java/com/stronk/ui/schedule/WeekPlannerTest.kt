package com.stronk.ui.schedule

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    // ---------- needsRollingExtension ----------

    @Test
    fun `needsRollingExtension gdy ostatni wpis blizej niz prog zwraca true`() {
        val today = monday
        val lastPlanned = today.plusDays(10) // < 2 tygodnie (14 dni)
        assertTrue(needsRollingExtension(lastPlanned, today, thresholdWeeks = 2))
    }

    @Test
    fun `needsRollingExtension dokladnie na progu to jeszcze nie powod`() {
        val today = monday
        val lastPlanned = today.plusWeeks(2) // dokladnie prog, zapas sie nie skonczyl
        assertFalse(needsRollingExtension(lastPlanned, today, thresholdWeeks = 2))
    }

    @Test
    fun `needsRollingExtension gdy ostatni wpis daleko w przyszlosci zwraca false`() {
        val today = monday
        val lastPlanned = today.plusWeeks(4)
        assertFalse(needsRollingExtension(lastPlanned, today, thresholdWeeks = 2))
    }

    @Test
    fun `needsRollingExtension gdy ostatni wpis juz w przeszlosci zwraca true`() {
        val today = monday
        val lastPlanned = today.minusDays(3)
        assertTrue(needsRollingExtension(lastPlanned, today, thresholdWeeks = 2))
    }

    // ---------- deriveWeekAssignments ----------

    @Test
    fun `deriveWeekAssignments z pustej listy daje pusta mape`() {
        assertEquals(emptyMap<DayOfWeek, Int>(), deriveWeekAssignments(emptyList()))
    }

    @Test
    fun `deriveWeekAssignments bierze ostatni tydzien wpisow`() {
        // Dwa tygodnie wpisow pn/czw z tym samym wzorcem (0/1) — wynik z 2. tygodnia.
        val entries = listOf(
            PlannedSlot(monday, 0),
            PlannedSlot(monday.plusDays(3), 1), // czwartek
            PlannedSlot(monday.plusWeeks(1), 0),
            PlannedSlot(monday.plusWeeks(1).plusDays(3), 1),
        )
        assertEquals(
            mapOf(DayOfWeek.MONDAY to 0, DayOfWeek.THURSDAY to 1),
            deriveWeekAssignments(entries),
        )
    }

    @Test
    fun `deriveWeekAssignments ignoruje starszy tydzien gdy wzorzec sie zmienil`() {
        // Pierwszy tydzien: pn=0. Ostatni (najpozniejszy) tydzien: pn=1 — wygrywa ostatni.
        val entries = listOf(
            PlannedSlot(monday, 0),
            PlannedSlot(monday.plusWeeks(2), 1),
        )
        assertEquals(mapOf(DayOfWeek.MONDAY to 1), deriveWeekAssignments(entries))
    }

    @Test
    fun `deriveWeekAssignments zachowuje full body na kilku dniach z tym samym dayIndex`() {
        val entries = listOf(
            PlannedSlot(monday, 0),
            PlannedSlot(monday.plusDays(2), 0), // sroda
            PlannedSlot(monday.plusDays(4), 0), // piatek
        )
        assertEquals(
            mapOf(DayOfWeek.MONDAY to 0, DayOfWeek.WEDNESDAY to 0, DayOfWeek.FRIDAY to 0),
            deriveWeekAssignments(entries),
        )
    }

    // ---------- conflictingOtherPlanEntry ----------

    @Test
    fun `conflictingOtherPlanEntry bez zajetych dni to null`() {
        assertEquals(null, conflictingOtherPlanEntry(emptyList(), "planA", monday, weeks = 4))
    }

    @Test
    fun `conflictingOtherPlanEntry zajete przez TEN SAM plan nie blokuje`() {
        val occupied = listOf(OccupiedEntry(monday, "planA", "Full Body"))
        assertEquals(null, conflictingOtherPlanEntry(occupied, "planA", monday, weeks = 4))
    }

    @Test
    fun `conflictingOtherPlanEntry zajete przez inny plan w oknie blokuje`() {
        val occupied = listOf(OccupiedEntry(monday.plusDays(2), "planB", "Push Pull"))
        val conflict = conflictingOtherPlanEntry(occupied, "planA", monday, weeks = 4)
        assertEquals(OccupiedEntry(monday.plusDays(2), "planB", "Push Pull"), conflict)
    }

    @Test
    fun `conflictingOtherPlanEntry poza oknem generacji nie blokuje`() {
        val beforeStart = OccupiedEntry(monday.minusDays(1), "planB", "Push Pull")
        val afterWindow = OccupiedEntry(monday.plusWeeks(4), "planB", "Push Pull")
        val conflict = conflictingOtherPlanEntry(
            listOf(beforeStart, afterWindow),
            "planA",
            monday,
            weeks = 4,
        )
        assertEquals(null, conflict)
    }

    @Test
    fun `conflictingOtherPlanEntry zwraca najwczesniejszy konflikt`() {
        val occupied = listOf(
            OccupiedEntry(monday.plusDays(5), "planB", "Push Pull"),
            OccupiedEntry(monday.plusDays(1), "planC", "Nogi"),
        )
        val conflict = conflictingOtherPlanEntry(occupied, "planA", monday, weeks = 4)
        assertEquals(OccupiedEntry(monday.plusDays(1), "planC", "Nogi"), conflict)
    }

    // ---------- planReplacement ----------

    @Test
    fun `planReplacement kasuje przyszle PLANNED wybranego planu i generuje nowe sloty wg nowego wzorca`() {
        val entries = listOf(
            ScheduleEntryRef("e1", monday, "planA", ScheduleEntryKind.PLANNED), // poniedzialek
            ScheduleEntryRef("e2", monday.plusDays(2), "planA", ScheduleEntryKind.PLANNED), // sroda
            ScheduleEntryRef("e3", monday.plusDays(4), "planA", ScheduleEntryKind.PLANNED), // piatek
        )
        val result = planReplacement(
            currentEntries = entries,
            selectedPlanId = "planA",
            assignments = mapOf(DayOfWeek.TUESDAY to 0, DayOfWeek.THURSDAY to 1),
            startDate = monday,
            weeks = 1,
            today = monday,
        )
        assertEquals(setOf("e1", "e2", "e3"), result.idsToDelete.toSet())
        assertTrue(result.slots.isNotEmpty())
        assertTrue(result.slots.all { it.date.dayOfWeek == DayOfWeek.TUESDAY || it.date.dayOfWeek == DayOfWeek.THURSDAY })
    }

    @Test
    fun `planReplacement nie kasuje wpisow PLANNED sprzed startDate`() {
        val entries = listOf(
            ScheduleEntryRef("przeszly", monday.minusWeeks(1), "planA", ScheduleEntryKind.PLANNED),
            ScheduleEntryRef("przyszly", monday, "planA", ScheduleEntryKind.PLANNED),
        )
        val result = planReplacement(entries, "planA", mapOf(DayOfWeek.MONDAY to 0), monday, weeks = 1, today = monday)
        assertEquals(listOf("przyszly"), result.idsToDelete)
    }

    @Test
    fun `planReplacement kasuje przyszle PLANNED tego samego planu nawet daleko poza oknem generacji`() {
        // Rolling generation mogl dogenerowac np. 12 tygodni do przodu —
        // przeplanowanie ma to wszystko skasowac, nie tylko okno [weeks].
        val entries = listOf(
            ScheduleEntryRef("daleko", monday.plusWeeks(12), "planA", ScheduleEntryKind.PLANNED),
        )
        val result = planReplacement(entries, "planA", mapOf(DayOfWeek.MONDAY to 0), monday, weeks = 4, today = monday)
        assertEquals(listOf("daleko"), result.idsToDelete)
    }

    @Test
    fun `planReplacement nie tyka wpisow DONE i omija ich daty przy generowaniu nowych slotow`() {
        val entries = listOf(
            ScheduleEntryRef("done", monday, "planA", ScheduleEntryKind.DONE), // poniedzialek zaliczony
        )
        val result = planReplacement(
            currentEntries = entries,
            selectedPlanId = "planA",
            assignments = mapOf(DayOfWeek.MONDAY to 0, DayOfWeek.WEDNESDAY to 0),
            startDate = monday,
            weeks = 1,
            today = monday,
        )
        assertTrue("DONE nie jest kasowany", result.idsToDelete.isEmpty())
        assertTrue("data z DONE nie dostaje nowego slotu", result.slots.none { it.date == monday })
        assertTrue("inna data z nowego wzorca dalej powstaje", result.slots.any { it.date == monday.plusDays(2) })
    }

    @Test
    fun `planReplacement omija daty zajete przez PLANNED innego planu`() {
        val entries = listOf(
            ScheduleEntryRef("innyPlan", monday.plusDays(2), "planB", ScheduleEntryKind.PLANNED), // sroda
        )
        val result = planReplacement(
            currentEntries = entries,
            selectedPlanId = "planA",
            assignments = mapOf(DayOfWeek.WEDNESDAY to 0),
            startDate = monday,
            weeks = 1,
            today = monday,
        )
        assertTrue(result.slots.isEmpty())
        assertTrue(result.idsToDelete.isEmpty())
    }

    @Test
    fun `planReplacement ignoruje SKIPPED - nie blokuje nowego slotu i nie jest kasowany`() {
        val entries = listOf(
            ScheduleEntryRef("skipped", monday, "planA", ScheduleEntryKind.OTHER),
        )
        val result = planReplacement(entries, "planA", mapOf(DayOfWeek.MONDAY to 0), monday, weeks = 1, today = monday)
        assertTrue(result.idsToDelete.isEmpty())
        assertTrue(result.slots.any { it.date == monday })
    }

    // ---------- clampStartDateToToday (Łatka 1: data startu w przeszłości) ----------

    @Test
    fun `clampStartDateToToday zostawia dzisiejsza date bez zmian`() {
        assertEquals(monday, clampStartDateToToday(monday, today = monday))
    }

    @Test
    fun `clampStartDateToToday zostawia date w przyszlosci bez zmian`() {
        val future = monday.plusDays(3)
        assertEquals(future, clampStartDateToToday(future, today = monday))
    }

    @Test
    fun `clampStartDateToToday podciaga date sprzed dzisiaj do dzisiaj`() {
        assertEquals(monday, clampStartDateToToday(monday.minusDays(5), today = monday))
    }

    @Test
    fun `planReplacement clampuje startDate sprzed dzisiaj - nie kasuje przeszlych PLANNED ani nie generuje przeszlych slotow`() {
        // "Dzisiaj" jest tydzień PO wybranej (defensywnie: UI już to blokuje)
        // startDate — symuluje przepuszczoną datę sprzed dziś.
        val today = monday.plusWeeks(1)
        val entries = listOf(
            ScheduleEntryRef("przeszly", monday, "planA", ScheduleEntryKind.PLANNED),
            ScheduleEntryRef("dzisiejszy", today, "planA", ScheduleEntryKind.PLANNED),
        )
        val result = planReplacement(
            currentEntries = entries,
            selectedPlanId = "planA",
            assignments = mapOf(DayOfWeek.MONDAY to 0),
            startDate = monday,
            weeks = 1,
            today = today,
        )
        // Przeszły (sprzed "dzisiaj") wpis PLANNED zostaje nietknięty — clamp
        // chroni go, mimo że formalnie spełnia `date >= startDate`.
        assertEquals(listOf("dzisiejszy"), result.idsToDelete)
        assertTrue("nie generuje slotow sprzed dzisiaj", result.slots.all { it.date >= today })
    }

    // ---------- rolling po przeplanowaniu podaza za NOWYM wzorcem ----------

    @Test
    fun `po przeplanowaniu deriveWeekAssignments z nowego harmonogramu daje nowy wzorzec, nie stary`() {
        // Stary wzorzec: pn/sr/pt (dayIndex 0). Przeplanowanie na wt/czw (dayIndex 1).
        val oldEntries = listOf(
            ScheduleEntryRef("e1", monday, "planA", ScheduleEntryKind.PLANNED),
            ScheduleEntryRef("e2", monday.plusDays(2), "planA", ScheduleEntryKind.PLANNED),
            ScheduleEntryRef("e3", monday.plusDays(4), "planA", ScheduleEntryKind.PLANNED),
        )
        val newAssignments = mapOf(DayOfWeek.TUESDAY to 1, DayOfWeek.THURSDAY to 1)
        val replan = planReplacement(oldEntries, "planA", newAssignments, monday, weeks = 4, today = monday)

        // To, co po zapisie realnie ladowaloby do harmonogramu jako PLANNED tego planu.
        val scheduleAfter = replan.slots
        val derived = deriveWeekAssignments(scheduleAfter)
        assertEquals(newAssignments, derived)

        // Dowod na "rolling wg nowego wzorca": kolejna generacja (jak w
        // maybeExtendContinuousPlans) uzywajac WYPROWADZONEGO wzorca produkuje
        // wtorek/czwartek — nigdy stary poniedzialek/piatek.
        val nextExtension = generatePlannedSlots(
            assignments = derived,
            startDate = scheduleAfter.maxOf { it.date }.plusDays(1),
            weeks = 4,
        )
        assertTrue(nextExtension.isNotEmpty())
        assertTrue(
            nextExtension.all { it.date.dayOfWeek == DayOfWeek.TUESDAY || it.date.dayOfWeek == DayOfWeek.THURSDAY },
        )
        assertTrue(
            nextExtension.none { it.date.dayOfWeek == DayOfWeek.MONDAY || it.date.dayOfWeek == DayOfWeek.FRIDAY },
        )
    }
}
