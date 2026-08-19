package com.stronk.ui.schedule

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/** Testy czystych funkcji siatki kwadratów (rzędy = tygodnie bloku) i etykiet. */
class ScheduleBlockGridTest {

    // 2026-08-19 = środa; poniedziałek tego tygodnia to 2026-08-17.
    private val today = LocalDate.of(2026, 8, 19)
    private val monday = LocalDate.of(2026, 8, 17)

    // ---------- blockGridWindow ----------

    @Test
    fun `domyslny blok 6 tygodni miesci sie w siatce w calosci`() {
        assertEquals(BlockWindow(startWeek = 0, rows = 6), blockGridWindow(0, 6))
        assertEquals(BlockWindow(startWeek = 0, rows = 6), blockGridWindow(5, 6))
    }

    @Test
    fun `krotki blok dopelniamy do minimalnej liczby rzedow`() {
        assertEquals(BlockWindow(startWeek = 0, rows = 4), blockGridWindow(0, 3))
        assertEquals(BlockWindow(startWeek = 0, rows = 4), blockGridWindow(1, 2))
    }

    @Test
    fun `dlugi blok pokazujemy oknem wysrodkowanym na biezacym tygodniu`() {
        assertEquals(BlockWindow(startWeek = 4, rows = 6), blockGridWindow(6, 13))
    }

    @Test
    fun `okno dlugiego bloku nie wychodzi poza jego granice`() {
        assertEquals(BlockWindow(startWeek = 0, rows = 6), blockGridWindow(0, 13))
        assertEquals(BlockWindow(startWeek = 7, rows = 6), blockGridWindow(12, 13))
    }

    // ---------- plan bez bloku: continuousGridWindow / gridWindow ----------

    @Test
    fun `plan bez bloku dostaje okno wokol biezacego tygodnia`() {
        // 5 rzędów, jeden tydzień przeszłości dla kontekstu.
        assertEquals(BlockWindow(startWeek = 6, rows = 5), continuousGridWindow(7))
        assertEquals(BlockWindow(startWeek = 40, rows = 5), continuousGridWindow(41))
    }

    @Test
    fun `okno planu bez bloku nie schodzi przed pierwszy tydzien`() {
        assertEquals(BlockWindow(startWeek = 0, rows = 5), continuousGridWindow(0))
        assertEquals(BlockWindow(startWeek = 0, rows = 5), continuousGridWindow(1))
    }

    @Test
    fun `gridWindow rozdziela plan z blokiem od planu bez bloku`() {
        assertEquals(blockGridWindow(5, 6), gridWindow(5, 6))
        assertEquals(continuousGridWindow(5), gridWindow(5, null))
    }

    @Test
    fun `siatka planu bez bloku trzyma biezacy tydzien w oknie`() {
        val window = continuousGridWindow(9)
        val mondays = blockWeekMondays(today, weekIndexInBlock = 9, window = window)
        assertEquals(5, mondays.size)
        // Tydzień 8 (jeden wstecz) otwiera okno, bieżący stoi na drugiej pozycji.
        assertEquals(monday.minusWeeks(1), mondays.first())
        assertEquals(monday, mondays[1])
    }

    // ---------- blockWeekMondays ----------

    @Test
    fun `siatka zaczyna sie w poniedzialek tygodnia zerowego bloku`() {
        val mondays = blockWeekMondays(today, weekIndexInBlock = 0, window = BlockWindow(0, 6))
        assertEquals(6, mondays.size)
        assertEquals(monday, mondays.first())
        assertEquals(monday.plusWeeks(5), mondays.last())
    }

    @Test
    fun `pozycja w bloku cofa kotwice siatki o tyle tygodni`() {
        val mondays = blockWeekMondays(today, weekIndexInBlock = 3, window = BlockWindow(0, 6))
        assertEquals(monday.minusWeeks(3), mondays.first())
        // Bieżący tydzień nadal jest w siatce, na czwartej pozycji.
        assertEquals(monday, mondays[3])
    }

    @Test
    fun `okno przesuniete w bloku zaczyna sie od swojego tygodnia`() {
        val mondays = blockWeekMondays(today, weekIndexInBlock = 6, window = BlockWindow(4, 6))
        assertEquals(monday.minusWeeks(2), mondays.first())
        assertEquals(6, mondays.size)
    }

    // ---------- ScheduleTexts ----------

    @Test
    fun `chip serii odmienia sie po polsku`() {
        assertEquals("1 seria", ScheduleTexts.setsLabel(1))
        assertEquals("3 serie", ScheduleTexts.setsLabel(3))
        assertEquals("5 serii", ScheduleTexts.setsLabel(5))
        assertEquals("12 serii", ScheduleTexts.setsLabel(12))
        assertEquals("22 serie", ScheduleTexts.setsLabel(22))
    }

    @Test
    fun `naglowek bloku sklada pozycje tygodnia`() {
        assertEquals("Tydzień 1/6", ScheduleTexts.blockWeekLabel(1, 6))
    }

    @Test
    fun `plan bez bloku ma naglowek bez mianownika`() {
        assertEquals("Tydzień 7", ScheduleTexts.continuousWeekLabel(7))
        assertEquals("Tydzień 7", ScheduleTexts.weekHeaderLabel(7, null))
        assertEquals("Tydzień 2/6", ScheduleTexts.weekHeaderLabel(2, 6))
    }

    @Test
    fun `tytul karty dnia laczy dzien tygodnia z nazwa dnia planu`() {
        assertEquals("Środa · Full body B", ScheduleTexts.dayCardTitle(today, "Full body B"))
        assertEquals("Środa", ScheduleTexts.dayCardTitle(today, null))
    }
}
