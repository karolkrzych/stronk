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

    @Test
    fun `deriveWeekAssignments odfiltrowuje dayIndex poza aktualna liczba dni planu`() {
        // Plan skurczony do 2 dni (indeksy 0,1) PO wygenerowaniu tych wpisow -
        // wpis na dzien 2 jest martwy i nie ma prawa wejsc do wzorca.
        val entries = listOf(
            PlannedSlot(monday, 0),
            PlannedSlot(monday.plusDays(2), 1),
            PlannedSlot(monday.plusDays(4), 2),
        )
        assertEquals(
            mapOf(DayOfWeek.MONDAY to 0, DayOfWeek.WEDNESDAY to 1),
            deriveWeekAssignments(entries, dayCount = 2),
        )
    }

    @Test
    fun `deriveWeekAssignments bez podanego dayCount nie filtruje niczego`() {
        val entries = listOf(PlannedSlot(monday, 5))
        assertEquals(mapOf(DayOfWeek.MONDAY to 5), deriveWeekAssignments(entries))
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
            ScheduleEntryRef("skipped", monday, "planA", ScheduleEntryKind.SKIPPED),
        )
        val result = planReplacement(entries, "planA", mapOf(DayOfWeek.MONDAY to 0), monday, weeks = 1, today = monday)
        assertTrue(result.idsToDelete.isEmpty())
        assertTrue(result.slots.any { it.date == monday })
    }

    // ---------- planReplacement: przesuniecia (MOVED) ----------

    @Test
    fun `planReplacement nie generuje PLANNED na dacie z aktywnym MOVED tego samego planu`() {
        // Scenariusz z buga: trening z poniedzialku przesuniety na czwartek,
        // potem ponowne "Zaplanuj tydzien" z tym samym wzorcem (poniedzialek).
        val thursday = monday.plusDays(3)
        val entries = listOf(
            ScheduleEntryRef("zrodlo", monday, "planA", ScheduleEntryKind.MOVED, movedTo = thursday),
            ScheduleEntryRef("cel", thursday, "planA", ScheduleEntryKind.PLANNED),
        )
        val result = planReplacement(entries, "planA", mapOf(DayOfWeek.MONDAY to 0), monday, weeks = 1, today = monday)
        assertTrue(result.slots.none { it.date == monday })
        // Wpis MOVED zostaje (nie jest kasowany), a trening pod nowa data przezywa.
        assertTrue(result.idsToDelete.isEmpty())
    }

    @Test
    fun `planReplacement nie kasuje wpisu docelowego przesuniecia - trening nie znika z tygodnia`() {
        val thursday = monday.plusDays(3)
        val entries = listOf(
            ScheduleEntryRef("zrodlo", monday, "planA", ScheduleEntryKind.MOVED, movedTo = thursday),
            ScheduleEntryRef("cel", thursday, "planA", ScheduleEntryKind.PLANNED),
            ScheduleEntryRef("sroda", monday.plusDays(2), "planA", ScheduleEntryKind.PLANNED),
        )
        val result = planReplacement(
            currentEntries = entries,
            selectedPlanId = "planA",
            assignments = mapOf(DayOfWeek.MONDAY to 0, DayOfWeek.WEDNESDAY to 1),
            startDate = monday,
            weeks = 1,
            today = monday,
        )
        // Zwykly PLANNED tego planu leci do wymiany, wpis docelowy przesuniecia NIE.
        assertEquals(listOf("sroda"), result.idsToDelete)
        // Ani na zrodle, ani na celu nie powstaje nowy slot — trening jest dokladnie jeden.
        assertTrue(result.slots.none { it.date == monday || it.date == thursday })
        assertTrue(result.slots.any { it.date == monday.plusDays(2) })
    }

    @Test
    fun `planReplacement ignoruje osierocony MOVED - cel juz nie istnieje, dzien wraca do planowania`() {
        val entries = listOf(
            ScheduleEntryRef("zrodlo", monday, "planA", ScheduleEntryKind.MOVED, movedTo = monday.plusDays(3)),
        )
        val result = planReplacement(entries, "planA", mapOf(DayOfWeek.MONDAY to 0), monday, weeks = 1, today = monday)
        assertTrue(result.slots.any { it.date == monday })
    }

    @Test
    fun `planReplacement nie blokuje sie na MOVED innego planu`() {
        val thursday = monday.plusDays(3)
        val entries = listOf(
            ScheduleEntryRef("zrodlo", monday, "planB", ScheduleEntryKind.MOVED, movedTo = thursday),
            ScheduleEntryRef("cel", thursday, "planB", ScheduleEntryKind.PLANNED),
        )
        val result = planReplacement(entries, "planA", mapOf(DayOfWeek.MONDAY to 0), monday, weeks = 1, today = monday)
        assertTrue(result.slots.any { it.date == monday })
    }

    // ---------- activeMovedSlots ----------

    @Test
    fun `activeMovedSlots zwraca pare zrodlo-cel gdy cel trzyma zywy wpis tego planu`() {
        val thursday = monday.plusDays(3)
        val entries = listOf(
            ScheduleEntryRef("zrodlo", monday, "planA", ScheduleEntryKind.MOVED, movedTo = thursday),
            ScheduleEntryRef("cel", thursday, "planA", ScheduleEntryKind.PLANNED),
        )
        assertEquals(listOf(MovedSlot(monday, thursday)), activeMovedSlots(entries, "planA", monday))
    }

    @Test
    fun `activeMovedSlots liczy tez cel zaliczony (DONE)`() {
        val thursday = monday.plusDays(3)
        val entries = listOf(
            ScheduleEntryRef("zrodlo", monday, "planA", ScheduleEntryKind.MOVED, movedTo = thursday),
            ScheduleEntryRef("cel", thursday, "planA", ScheduleEntryKind.DONE),
        )
        assertEquals(listOf(MovedSlot(monday, thursday)), activeMovedSlots(entries, "planA", monday))
    }

    @Test
    fun `activeMovedSlots pomija osierocone i cudze przesuniecia oraz daty sprzed since`() {
        val thursday = monday.plusDays(3)
        val entries = listOf(
            // cel skasowany
            ScheduleEntryRef("osierocony", monday, "planA", ScheduleEntryKind.MOVED, movedTo = thursday),
            // inny plan
            ScheduleEntryRef("cudzy", monday, "planB", ScheduleEntryKind.MOVED, movedTo = thursday),
            ScheduleEntryRef("cudzy-cel", thursday, "planB", ScheduleEntryKind.PLANNED),
            // zrodlo sprzed okna
            ScheduleEntryRef("stary", monday.minusDays(7), "planA", ScheduleEntryKind.MOVED, movedTo = monday.plusDays(2)),
            ScheduleEntryRef("stary-cel", monday.plusDays(2), "planA", ScheduleEntryKind.PLANNED),
        )
        assertTrue(activeMovedSlots(entries, "planA", monday).isEmpty())
    }

    // ---------- shadowedEntryIds (duplikat na jednej dacie) ----------

    @Test
    fun `shadowedEntryIds bierze MOVED przykryty wpisem PLANNED tego samego planu na tej samej dacie`() {
        // Dokladnie zepsuty stan z konta: E1 MOVED + E3 PLANNED na 24 sierpnia.
        val entries = listOf(
            ScheduleEntryRef("e1", monday, "planA", ScheduleEntryKind.MOVED, movedTo = monday.plusDays(3)),
            ScheduleEntryRef("e3", monday, "planA", ScheduleEntryKind.PLANNED),
        )
        assertEquals(listOf("e1"), shadowedEntryIds(entries))
    }

    @Test
    fun `shadowedEntryIds bierze SKIPPED przykryty wpisem DONE tego samego planu`() {
        val entries = listOf(
            ScheduleEntryRef("odwolany", monday, "planA", ScheduleEntryKind.SKIPPED),
            ScheduleEntryRef("zaliczony", monday, "planA", ScheduleEntryKind.DONE),
        )
        assertEquals(listOf("odwolany"), shadowedEntryIds(entries))
    }

    @Test
    fun `shadowedEntryIds nie rusza samotnego MOVED ani SKIPPED`() {
        val entries = listOf(
            ScheduleEntryRef("przesuniety", monday, "planA", ScheduleEntryKind.MOVED, movedTo = monday.plusDays(3)),
            ScheduleEntryRef("odwolany", monday.plusDays(1), "planA", ScheduleEntryKind.SKIPPED),
            // zywy wpis, ale INNEGO planu — nie przykrywa
            ScheduleEntryRef("obcy", monday, "planB", ScheduleEntryKind.PLANNED),
        )
        assertTrue(shadowedEntryIds(entries).isEmpty())
    }

    @Test
    fun `shadowedEntryIds nigdy nie zwraca wpisow DONE ani PLANNED`() {
        val entries = listOf(
            ScheduleEntryRef("done", monday, "planA", ScheduleEntryKind.DONE),
            ScheduleEntryRef("planned", monday, "planA", ScheduleEntryKind.PLANNED),
        )
        assertTrue(shadowedEntryIds(entries).isEmpty())
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

    // ---------- weekdayAssignmentsFromIso / weekdayAssignmentsToIso ----------

    @Test
    fun `weekdayAssignmentsFromIso mapuje ISO 1-7 na DayOfWeek`() {
        assertEquals(
            mapOf(DayOfWeek.MONDAY to 0, DayOfWeek.WEDNESDAY to 1, DayOfWeek.SUNDAY to 2),
            weekdayAssignmentsFromIso(mapOf(1 to 0, 3 to 1, 7 to 2)),
        )
    }

    @Test
    fun `weekdayAssignmentsFromIso pomija klucze spoza 1-7`() {
        assertEquals(
            mapOf(DayOfWeek.MONDAY to 0),
            weekdayAssignmentsFromIso(mapOf(1 to 0, 0 to 9, 8 to 9, -1 to 9)),
        )
    }

    @Test
    fun `weekdayAssignmentsToIso jest odwrotnoscia weekdayAssignmentsFromIso`() {
        val assignments = mapOf(DayOfWeek.TUESDAY to 0, DayOfWeek.THURSDAY to 1, DayOfWeek.SATURDAY to 2)
        val iso = weekdayAssignmentsToIso(assignments)
        assertEquals(mapOf(2 to 0, 4 to 1, 6 to 2), iso)
        assertEquals(assignments, weekdayAssignmentsFromIso(iso))
    }

    @Test
    fun `pusta mapa ISO daje pusta mape DayOfWeek i odwrotnie`() {
        assertEquals(emptyMap<DayOfWeek, Int>(), weekdayAssignmentsFromIso(emptyMap()))
        assertEquals(emptyMap<Int, Int>(), weekdayAssignmentsToIso(emptyMap()))
    }

    // ---------- weekPlanBaseline ----------

    @Test
    fun `weekPlanBaseline uzywa zapisanego wzorca gdy istnieje - nawet pustego`() {
        val saved = emptyMap<DayOfWeek, Int>()
        val entries = listOf(PlannedSlot(monday, 0)) // istniejacy wpis - MA byc zignorowany
        assertEquals(saved, weekPlanBaseline(saved, entries))
    }

    @Test
    fun `weekPlanBaseline spada na deriveWeekAssignments gdy zapisany wzorzec to null`() {
        val entries = listOf(PlannedSlot(monday, 0), PlannedSlot(monday.plusDays(2), 1))
        assertEquals(deriveWeekAssignments(entries), weekPlanBaseline(null, entries))
    }

    @Test
    fun `weekPlanBaseline bez zapisanego wzorca i bez wpisow to pusty wzorzec`() {
        assertEquals(emptyMap<DayOfWeek, Int>(), weekPlanBaseline(null, emptyList()))
    }

    @Test
    fun `weekPlanBaseline odfiltrowuje zapisany wzorzec z bazy poza liczba dni planu`() {
        // Plan mial kiedys 3 dni, skurczony do 2 - zapisany w Plan.weekdayAssignments
        // wzorzec z dnia o indeksie 2 jest martwy (bug 1 przed poprawka pozwalal
        // na taki rozjazd, bo edytor gubil to pole przy kazdym zapisie).
        val saved = mapOf(DayOfWeek.MONDAY to 0, DayOfWeek.WEDNESDAY to 1, DayOfWeek.FRIDAY to 2)
        assertEquals(
            mapOf(DayOfWeek.MONDAY to 0, DayOfWeek.WEDNESDAY to 1),
            weekPlanBaseline(saved, emptyList(), dayCount = 2),
        )
    }

    @Test
    fun `weekPlanBaseline przekazuje dayCount do fallbacku wyprowadzonego z wpisow`() {
        val entries = listOf(PlannedSlot(monday, 0), PlannedSlot(monday.plusDays(2), 2))
        assertEquals(
            mapOf(DayOfWeek.MONDAY to 0),
            weekPlanBaseline(null, entries, dayCount = 2),
        )
    }

    // ---------- isWeekPlanDirty ----------

    @Test
    fun `isWeekPlanDirty false gdy przypisania rowne baseline`() {
        val pattern = mapOf(DayOfWeek.MONDAY to 0, DayOfWeek.FRIDAY to 1)
        assertFalse(isWeekPlanDirty(pattern, pattern.toMap()))
    }

    @Test
    fun `isWeekPlanDirty true gdy przypisania rozne od baseline`() {
        assertTrue(
            isWeekPlanDirty(
                mapOf(DayOfWeek.MONDAY to 0),
                mapOf(DayOfWeek.TUESDAY to 0),
            ),
        )
    }

    @Test
    fun `isWeekPlanDirty true gdy baseline pusty a przypisania niepuste`() {
        assertTrue(isWeekPlanDirty(emptyMap(), mapOf(DayOfWeek.MONDAY to 0)))
    }

    @Test
    fun `isWeekPlanDirty false gdy oba puste - sama zmiana daty startu nie liczy sie`() {
        assertFalse(isWeekPlanDirty(emptyMap(), emptyMap()))
    }

    // ---------- blockReplanWeeks ----------

    @Test
    fun `blockReplanWeeks bez istniejacych wpisow to dokladnie fullBlockWeeks`() {
        assertEquals(6, blockReplanWeeks(monday, fullBlockWeeks = 6, existingPlanDates = emptyList()))
    }

    @Test
    fun `blockReplanWeeks nie skraca horyzontu ponizej minimum gdy stare wpisy sa blisko`() {
        val existing = listOf(monday.plusWeeks(1))
        assertEquals(6, blockReplanWeeks(monday, fullBlockWeeks = 6, existingPlanDates = existing))
    }

    @Test
    fun `blockReplanWeeks rozszerza horyzont gdy plan mial juz wpisy dalej niz blok`() {
        // Ostatni istniejacy wpis 10 tygodni od startu, blok to tylko 6 tygodni
        // pracy+lekki - horyzont ma objac ten wpis, nie uciac go.
        val lastExisting = monday.plusWeeks(10)
        val weeks = blockReplanWeeks(monday, fullBlockWeeks = 6, existingPlanDates = listOf(lastExisting))
        assertTrue("horyzont musi objac ostatni istniejacy wpis", weeks > 6)
        assertTrue(monday.plusWeeks(weeks.toLong()).isAfter(lastExisting))
    }

    @Test
    fun `blockReplanWeeks - pierwsze planowanie z data startu w srodku tygodnia`() {
        val wednesday = LocalDate.of(2026, 8, 19)
        assertEquals(6, blockReplanWeeks(wednesday, fullBlockWeeks = 6, existingPlanDates = emptyList()))
    }

    // ---------- rolling wg wzorca z planu (nie wg przesunietego wpisu) ----------

    @Test
    fun `rolling wg wzorca z planu ignoruje pojedynczy przesuniety wpis`() {
        // Wzorzec zapisany w planie: pn/sr/pt (dayIndex 0) - to co ScheduleViewModel
        // czyta z Plan.weekdayAssignments (juz po weekdayAssignmentsFromIso).
        val planPattern = weekdayAssignmentsFromIso(mapOf(1 to 0, 3 to 0, 5 to 0))
        assertEquals(
            mapOf(DayOfWeek.MONDAY to 0, DayOfWeek.WEDNESDAY to 0, DayOfWeek.FRIDAY to 0),
            planPattern,
        )

        // Realny harmonogram po "Przesun": srodowy trening wyladowal we wtorek
        // (nowy wpis PLANNED wtorek; oryginalny wpis srody stal sie MOVED, wiec
        // nie wchodzi juz do listy PLANNED slotow).
        val entriesAfterMove = listOf(
            PlannedSlot(monday, 0), // pn - bez zmian
            PlannedSlot(monday.plusDays(1), 0), // wt - PRZESUNIETY tu (byl sr)
            PlannedSlot(monday.plusDays(4), 0), // pt - bez zmian
        )

        // Gdyby rolling patrzyl na surowe wpisy (stare zachowanie sprzed tego
        // zadania), zarazilby sie przesunieciem - zlapalby wtorek zamiast srody.
        val derivedFromEntries = deriveWeekAssignments(entriesAfterMove)
        assertTrue(DayOfWeek.TUESDAY in derivedFromEntries)
        assertFalse(DayOfWeek.WEDNESDAY in derivedFromEntries)

        // Kolejna generacja WG WZORCA Z PLANU (to co robi maybeExtendContinuousPlans
        // po tej zmianie) produkuje pn/sr/pt - nigdy wtorek.
        val nextExtension = generatePlannedSlots(
            assignments = planPattern,
            startDate = entriesAfterMove.maxOf { it.date }.plusDays(1),
            weeks = 2,
        )
        assertTrue(nextExtension.isNotEmpty())
        assertTrue(nextExtension.none { it.date.dayOfWeek == DayOfWeek.TUESDAY })
        assertTrue(nextExtension.any { it.date.dayOfWeek == DayOfWeek.WEDNESDAY })
    }

    // ---------- bug: zarchiwizowany plan blokuje planowanie nowego (scenariusz usera) ----------

    // ---------- buildOccupiedEntries ----------

    @Test
    fun `buildOccupiedEntries pomija PLANNED zarchiwizowanego planu`() {
        val refs = listOf(
            ScheduleEntryRef("martwy", monday, "planStary", ScheduleEntryKind.PLANNED, archived = true),
        )
        assertEquals(emptyList<OccupiedEntry>(), buildOccupiedEntries(refs) { "Stary plan" })
    }

    @Test
    fun `buildOccupiedEntries zachowuje PLANNED niezarchiwizowanego planu`() {
        val refs = listOf(
            ScheduleEntryRef("zywy", monday, "planNowy", ScheduleEntryKind.PLANNED, archived = false),
        )
        assertEquals(
            listOf(OccupiedEntry(monday, "planNowy", "Nowy plan")),
            buildOccupiedEntries(refs) { "Nowy plan" },
        )
    }

    @Test
    fun `buildOccupiedEntries zachowuje DONE nawet jesli plan-wlasciciel jest zarchiwizowany`() {
        // DONE.archived MUSI zostac false wg kontraktu ScheduleEntryRef.archived
        // (budowany tak przez ScheduleViewModel/PlanEditorViewModel) - tu wprost
        // sprawdzamy, ze buildOccupiedEntries nie potrzebuje tego zalozenia:
        // DONE przechodzi zawsze, niezaleznie od flagi.
        val refs = listOf(
            ScheduleEntryRef("zaliczony", monday, "planStary", ScheduleEntryKind.DONE, archived = false),
        )
        assertEquals(
            listOf(OccupiedEntry(monday, "planStary", "Stary plan")),
            buildOccupiedEntries(refs) { "Stary plan" },
        )
    }

    @Test
    fun `buildOccupiedEntries pomija SKIPPED i MOVED jak dotad`() {
        val refs = listOf(
            ScheduleEntryRef("odwolany", monday, "planA", ScheduleEntryKind.SKIPPED),
            ScheduleEntryRef("przesuniety", monday, "planA", ScheduleEntryKind.MOVED, movedTo = monday.plusDays(3)),
        )
        assertEquals(emptyList<OccupiedEntry>(), buildOccupiedEntries(refs) { "Plan A" })
    }

    @Test
    fun `scenariusz usera - PLANNED zarchiwizowanego planu przepuszczone przez buildOccupiedEntries nie konfliktuje w dialogu`() {
        // Dokladnie zgloszony bug: user archiwizuje aktywny plan (ma przyszle
        // wpisy PLANNED w harmonogramie), potem probuje zaplanowac nowy plan na
        // ten sam okres - CTA nie ma prawa byc zablokowane.
        val refs = listOf(
            ScheduleEntryRef("martwy1", monday, "planStary", ScheduleEntryKind.PLANNED, archived = true),
            ScheduleEntryRef("martwy2", monday.plusDays(2), "planStary", ScheduleEntryKind.PLANNED, archived = true),
        )
        val occupied = buildOccupiedEntries(refs) { "Stary plan" }
        val conflict = conflictingOtherPlanEntry(occupied, "planNowy", monday, weeks = 4)
        assertEquals(null, conflict)
    }

    // ---------- planReplacement: martwe wpisy zarchiwizowanego planu ----------

    @Test
    fun `planReplacement nie blokuje daty zajetej tylko przez PLANNED zarchiwizowanego innego planu`() {
        val entries = listOf(
            ScheduleEntryRef("martwy", monday.plusDays(2), "planStary", ScheduleEntryKind.PLANNED, archived = true),
        )
        val result = planReplacement(
            currentEntries = entries,
            selectedPlanId = "planNowy",
            assignments = mapOf(DayOfWeek.WEDNESDAY to 0),
            startDate = monday,
            weeks = 1,
            today = monday,
        )
        assertTrue("nowy slot MUSI powstac mimo martwego wpisu", result.slots.any { it.date == monday.plusDays(2) })
    }

    @Test
    fun `planReplacement kasuje martwy wpis zarchiwizowanego planu na dacie kolidujacej z nowym slotem`() {
        val entries = listOf(
            ScheduleEntryRef("martwy", monday.plusDays(2), "planStary", ScheduleEntryKind.PLANNED, archived = true),
        )
        val result = planReplacement(
            currentEntries = entries,
            selectedPlanId = "planNowy",
            assignments = mapOf(DayOfWeek.WEDNESDAY to 0),
            startDate = monday,
            weeks = 1,
            today = monday,
        )
        assertEquals(listOf("martwy"), result.idsToDelete)
    }

    @Test
    fun `planReplacement zostawia martwy wpis zarchiwizowanego planu gdy jego data nie dostaje nowego slotu`() {
        // User wyzerowal akurat ten dzien tygodnia w nowym wzorcu - martwy wpis
        // na tej dacie nie koliduje z niczym nowym, wiec go NIE kasujemy tutaj
        // (posprzata go kolejna archiwizacja albo sweep przy starcie ekranu).
        val entries = listOf(
            ScheduleEntryRef("martwy", monday.plusDays(2), "planStary", ScheduleEntryKind.PLANNED, archived = true),
        )
        val result = planReplacement(
            currentEntries = entries,
            selectedPlanId = "planNowy",
            assignments = mapOf(DayOfWeek.MONDAY to 0), // nie generuje nic na srode
            startDate = monday,
            weeks = 1,
            today = monday,
        )
        assertTrue(result.idsToDelete.isEmpty())
    }

    @Test
    fun `planReplacement nadal blokuje na PLANNED innego, NIEzarchiwizowanego planu`() {
        // Regresja: martwe wpisy nie blokuja, ale zywe (inny aktywny plan) nadal maja blokowac.
        val entries = listOf(
            ScheduleEntryRef("zywy", monday.plusDays(2), "planInnyAktywny", ScheduleEntryKind.PLANNED, archived = false),
        )
        val result = planReplacement(
            currentEntries = entries,
            selectedPlanId = "planNowy",
            assignments = mapOf(DayOfWeek.WEDNESDAY to 0),
            startDate = monday,
            weeks = 1,
            today = monday,
        )
        assertTrue(result.slots.none { it.date == monday.plusDays(2) })
        assertTrue("wpis zywego planu nie jest kasowany przy cudzym replanie", result.idsToDelete.isEmpty())
    }

    // ---------- archivedPlanDeadEntryIds ----------

    @Test
    fun `archivedPlanDeadEntryIds bierze przyszle wpisy zarchiwizowanych planow poza DONE`() {
        val today = monday
        val entries = listOf(
            ScheduleEntryRef("przyszly-martwy", today, "planStary", ScheduleEntryKind.PLANNED, archived = true),
            ScheduleEntryRef("przyszly-zywy", today, "planNowy", ScheduleEntryKind.PLANNED, archived = false),
            ScheduleEntryRef("przeszly-martwy", today.minusDays(1), "planStary", ScheduleEntryKind.PLANNED, archived = true),
            ScheduleEntryRef("done-martwy", today, "planStary", ScheduleEntryKind.DONE, archived = false),
            ScheduleEntryRef("skipped-martwy", today, "planStary", ScheduleEntryKind.SKIPPED, archived = true),
        )
        assertEquals(listOf("przyszly-martwy", "skipped-martwy"), archivedPlanDeadEntryIds(entries, today))
    }

    @Test
    fun `archivedPlanDeadEntryIds bierze MOVED zarchiwizowanego planu`() {
        // Karta-widmo z gate'a: poniedzialek zarchiwizowanego planu "Przesuniety
        // -> czwartek" wisi obok normalnego treningu AKTYWNEGO planu.
        val entries = listOf(
            ScheduleEntryRef(
                "widmo",
                monday,
                "planStary",
                ScheduleEntryKind.MOVED,
                archived = true,
                movedTo = monday.plusDays(3),
            ),
        )
        assertEquals(listOf("widmo"), archivedPlanDeadEntryIds(entries, monday))
    }

    @Test
    fun `archivedPlanDeadEntryIds bierze SKIPPED zarchiwizowanego planu`() {
        val entries = listOf(
            ScheduleEntryRef("odwolany", monday, "planStary", ScheduleEntryKind.SKIPPED, archived = true),
        )
        assertEquals(listOf("odwolany"), archivedPlanDeadEntryIds(entries, monday))
    }

    @Test
    fun `archivedPlanDeadEntryIds nie rusza MOVED AKTYWNEGO planu bez zywego wpisu na dacie`() {
        // Breadcrumb aktywnego przesuniecia (cel skasowany albo jeszcze nie
        // powstal) - ten sweep dotyczy WYLACZNIE planow zarchiwizowanych.
        val entries = listOf(
            ScheduleEntryRef(
                "przesuniety",
                monday,
                "planAktywny",
                ScheduleEntryKind.MOVED,
                archived = false,
                movedTo = monday.plusDays(3),
            ),
            ScheduleEntryRef("odwolany", monday.plusDays(1), "planAktywny", ScheduleEntryKind.SKIPPED, archived = false),
        )
        assertTrue(archivedPlanDeadEntryIds(entries, monday).isEmpty())
    }

    // ---------- archivedPlanGhostEntryIds (filtr renderu karty dnia) ----------

    @Test
    fun `archivedPlanGhostEntryIds ukrywa MOVED i SKIPPED zarchiwizowanego planu`() {
        val entries = listOf(
            ScheduleEntryRef(
                "widmo-moved",
                monday,
                "planStary",
                ScheduleEntryKind.MOVED,
                archived = true,
                movedTo = monday.plusDays(3),
            ),
            ScheduleEntryRef("widmo-skipped", monday, "planStary", ScheduleEntryKind.SKIPPED, archived = true),
        )
        assertEquals(listOf("widmo-moved", "widmo-skipped"), archivedPlanGhostEntryIds(entries))
    }

    @Test
    fun `archivedPlanGhostEntryIds ukrywa widma takze w przeszlosci`() {
        // Swiadoma asymetria wobec sweepu: baza trzyma przeszlosc (audit-trail),
        // ale karta dnia nie ma pokazywac przesuniec planu, ktorego juz nie ma.
        val entries = listOf(
            ScheduleEntryRef(
                "stare-widmo",
                monday.minusWeeks(3),
                "planStary",
                ScheduleEntryKind.MOVED,
                archived = true,
                movedTo = monday.minusWeeks(3).plusDays(2),
            ),
        )
        assertEquals(listOf("stare-widmo"), archivedPlanGhostEntryIds(entries))
    }

    @Test
    fun `archivedPlanGhostEntryIds nie rusza DONE ani PLANNED zarchiwizowanego planu`() {
        // DONE = historia; przeszly PLANNED = "zaplanowany, nie zrobiony", tez fakt.
        val entries = listOf(
            ScheduleEntryRef("zaliczony", monday, "planStary", ScheduleEntryKind.DONE, archived = false),
            ScheduleEntryRef("przeszly-planned", monday, "planStary", ScheduleEntryKind.PLANNED, archived = true),
        )
        assertTrue(archivedPlanGhostEntryIds(entries).isEmpty())
    }

    @Test
    fun `archivedPlanGhostEntryIds nie rusza MOVED aktywnego planu`() {
        val entries = listOf(
            ScheduleEntryRef(
                "przesuniety",
                monday,
                "planAktywny",
                ScheduleEntryKind.MOVED,
                archived = false,
                movedTo = monday.plusDays(3),
            ),
        )
        assertTrue(archivedPlanGhostEntryIds(entries).isEmpty())
    }

    @Test
    fun `scenariusz usera - widmo zarchiwizowanego planu obok treningu aktywnego planu`() {
        // Dokladnie stan z gate'a: poniedzialek ma normalny trening planu
        // aktywnego I karte "Przesuniety -> czwartek" po planie zarchiwizowanym,
        // czwartek ma samotna karte "Przesuniety -> poniedzialek".
        // shadowedEntryIds tego nie widzi (kluczuje po (planId, data), plany sa
        // rozne) - lapie to dopiero regula "zarchiwizowany plan".
        val thursday = monday.plusDays(3)
        val entries = listOf(
            ScheduleEntryRef("trening", monday, "planAktywny", ScheduleEntryKind.PLANNED, archived = false),
            ScheduleEntryRef("widmo-pn", monday, "planStary", ScheduleEntryKind.MOVED, archived = true, movedTo = thursday),
            ScheduleEntryRef("widmo-czw", thursday, "planStary", ScheduleEntryKind.MOVED, archived = true, movedTo = monday),
        )
        assertTrue("shadowedEntryIds nie ma prawa tego zlapac", shadowedEntryIds(entries).isEmpty())
        assertEquals(listOf("widmo-pn", "widmo-czw"), archivedPlanGhostEntryIds(entries))
        assertEquals(listOf("widmo-pn", "widmo-czw"), archivedPlanDeadEntryIds(entries, monday))
    }

    @Test
    fun `archivedPlanDeadEntryIds nigdy nie rusza DONE zarchiwizowanego planu`() {
        // Historia treningu jest nietykalna. Kontrakt ScheduleEntryRef.archived
        // trzyma DONE na false (pierwszy wpis - realny przypadek), ale funkcja ma
        // byc odporna takze na DONE blednie oznaczone flaga (drugi wpis): filtr
        // wyklucza DONE po samym `kind`.
        val entries = listOf(
            ScheduleEntryRef("done", monday, "planStary", ScheduleEntryKind.DONE, archived = false),
            ScheduleEntryRef("done-z-flaga", monday, "planStary", ScheduleEntryKind.DONE, archived = true),
        )
        assertTrue(archivedPlanDeadEntryIds(entries, monday).isEmpty())
    }

    @Test
    fun `archivedPlanDeadEntryIds pusta lista gdy nic nie jest zarchiwizowane`() {
        val entries = listOf(
            ScheduleEntryRef("e1", monday, "planA", ScheduleEntryKind.PLANNED, archived = false),
        )
        assertTrue(archivedPlanDeadEntryIds(entries, monday).isEmpty())
    }

    @Test
    fun `archivedPlanDeadEntryIds data rowna dzisiaj tez jest kasowana`() {
        val entries = listOf(
            ScheduleEntryRef("dzis", monday, "planStary", ScheduleEntryKind.PLANNED, archived = true),
        )
        assertEquals(listOf("dzis"), archivedPlanDeadEntryIds(entries, monday))
    }

    // ---------- isEligibleForRollingExtension ----------

    @Test
    fun `isEligibleForRollingExtension false dla zarchiwizowanego planu bez bloku`() {
        assertFalse(isEligibleForRollingExtension(archived = true, blockLengthWeeks = null))
    }

    @Test
    fun `isEligibleForRollingExtension true dla aktywnego planu bez bloku`() {
        assertTrue(isEligibleForRollingExtension(archived = false, blockLengthWeeks = null))
    }

    @Test
    fun `isEligibleForRollingExtension false dla aktywnego planu Z blokiem`() {
        assertFalse(isEligibleForRollingExtension(archived = false, blockLengthWeeks = 6))
    }

    @Test
    fun `isEligibleForRollingExtension false dla zarchiwizowanego planu Z blokiem`() {
        assertFalse(isEligibleForRollingExtension(archived = true, blockLengthWeeks = 6))
    }

    // ---------- remapWeekdayAssignments (zapis edytora planu) ----------

    @Test
    fun `remapWeekdayAssignments przesuwa indeksy po usunieciu srodkowego dnia`() {
        // Push/Pull/Legs (0/1/2), Pull usuniety - remap z PlanEditorSave.dayIndexRemap: {0 to 0, 2 to 1}.
        val pattern = mapOf(DayOfWeek.MONDAY to 0, DayOfWeek.WEDNESDAY to 1, DayOfWeek.FRIDAY to 2)
        val remap = mapOf(0 to 0, 2 to 1)
        assertEquals(
            mapOf(DayOfWeek.MONDAY to 0, DayOfWeek.FRIDAY to 1),
            remapWeekdayAssignments(pattern, remap),
        )
    }

    @Test
    fun `remapWeekdayAssignments usuwa przypisania wskazujace usuniety ostatni dzien`() {
        val pattern = mapOf(DayOfWeek.MONDAY to 0, DayOfWeek.WEDNESDAY to 1)
        val remap = mapOf(0 to 0) // dzien 1 (ostatni) usuniety, brak wpisu w remap
        assertEquals(mapOf(DayOfWeek.MONDAY to 0), remapWeekdayAssignments(pattern, remap))
    }

    @Test
    fun `remapWeekdayAssignments identity remap po dodaniu dnia nie zmienia niczego`() {
        val pattern = mapOf(DayOfWeek.MONDAY to 0, DayOfWeek.FRIDAY to 1)
        val remap = mapOf(0 to 0, 1 to 1) // nowy dzien na koncu nie ma starego indeksu
        assertEquals(pattern, remapWeekdayAssignments(pattern, remap))
    }

    @Test
    fun `remapWeekdayAssignments obsluguje przestawienie dni`() {
        val pattern = mapOf(DayOfWeek.MONDAY to 0, DayOfWeek.FRIDAY to 1)
        val remap = mapOf(0 to 1, 1 to 0) // dni zamienione miejscami
        assertEquals(
            mapOf(DayOfWeek.MONDAY to 1, DayOfWeek.FRIDAY to 0),
            remapWeekdayAssignments(pattern, remap),
        )
    }

    @Test
    fun `remapWeekdayAssignments pustego wzorca daje pusty wynik`() {
        assertEquals(emptyMap<DayOfWeek, Int>(), remapWeekdayAssignments(emptyMap(), mapOf(0 to 0)))
    }

    @Test
    fun `remapWeekdayAssignments po usunieciu PIERWSZEGO dnia przesuwa pozostale i gubi przypisanie usunietego`() {
        // Push/Pull/Legs (0/1/2) na pn/sr/pt, usunieto Push (index 0) -
        // remap z PlanEditorSave.dayIndexRemap(listOf(1, 2)): {1 to 0, 2 to 1}.
        val pattern = mapOf(DayOfWeek.MONDAY to 0, DayOfWeek.WEDNESDAY to 1, DayOfWeek.FRIDAY to 2)
        val remap = mapOf(1 to 0, 2 to 1)
        assertEquals(
            mapOf(DayOfWeek.WEDNESDAY to 0, DayOfWeek.FRIDAY to 1),
            remapWeekdayAssignments(pattern, remap),
        )
    }

    @Test
    fun `remapWeekdayAssignments po usunieciu dwoch dni naraz gubi oba przypisania`() {
        // 4 dni na pn/wt/sr/czw, usunieto srodkowe dwa (1 i 2) -
        // remap z dayIndexRemap(listOf(0, 3)): {0 to 0, 3 to 1}.
        val pattern = mapOf(
            DayOfWeek.MONDAY to 0,
            DayOfWeek.TUESDAY to 1,
            DayOfWeek.WEDNESDAY to 2,
            DayOfWeek.THURSDAY to 3,
        )
        val remap = mapOf(0 to 0, 3 to 1)
        assertEquals(
            mapOf(DayOfWeek.MONDAY to 0, DayOfWeek.THURSDAY to 1),
            remapWeekdayAssignments(pattern, remap),
        )
    }

    @Test
    fun `remapWeekdayAssignments usuniecie i dodanie dnia w jednej sesji gubi tylko przypisanie usunietego`() {
        // 3 dni na pn/sr/pt, usunieto srodkowy (1) i dodano nowy na koncu (null) -
        // remap z dayIndexRemap(listOf(0, 2, null)): {0 to 0, 2 to 1}. Nowy dzien
        // (bez odpowiednika w starym wzorcu) nie ma tu czego dostac - wzorzec nigdy
        // go nie znal, user przypisze go recznie w "Zaplanuj tydzien".
        val pattern = mapOf(DayOfWeek.MONDAY to 0, DayOfWeek.WEDNESDAY to 1, DayOfWeek.FRIDAY to 2)
        val remap = mapOf(0 to 0, 2 to 1)
        assertEquals(
            mapOf(DayOfWeek.MONDAY to 0, DayOfWeek.FRIDAY to 1),
            remapWeekdayAssignments(pattern, remap),
        )
    }

    // ---------- saveReplanWeeks (horyzont przy zapisie edytora planu) ----------

    @Test
    fun `saveReplanWeeks po skroceniu bloku zwraca dokladnie nowa dlugosc, nie stara`() {
        // W odroznieniu od blockReplanWeeks (dialog planowania) zapis edytora
        // MA PRAWO skracac horyzont - funkcja jest bezstanowa, nie zna "starego"
        // horyzontu, wiec nie moze go przypadkiem zachowac.
        assertEquals(3, saveReplanWeeks(fullBlockWeeks = 3))
    }

    @Test
    fun `saveReplanWeeks po wydluzeniu bloku zwraca nowa, wieksza dlugosc`() {
        assertEquals(10, saveReplanWeeks(fullBlockWeeks = 10))
    }

    @Test
    fun `saveReplanWeeks dla planu bez bloku (blok wylaczony) daje stale okno generacji`() {
        assertEquals(ScheduleConstants.GENERATION_WEEKS, saveReplanWeeks(fullBlockWeeks = null))
    }
}
