package com.stronk.ui.schedule

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testy czystej funkcji siatki KLASYCZNEGO WIDOKU MIESIĄCA ([monthGridMondays])
 * i etykiet ekranu. Siatka pokrywa cały miesiąc pełnymi tygodniami: pierwszy
 * rząd zaczyna się w poniedziałek tygodnia z 1. dniem, ostatni kończy w
 * niedzielę tygodnia z ostatnim dniem (dni spoza miesiąca to puste placeholdery
 * — patrz [ScheduleViewModel.buildState]).
 */
class ScheduleMonthGridTest {

    // 2026-08-19 = środa.
    private val today = LocalDate.of(2026, 8, 19)

    /** Niedziela zamykająca siatkę = ostatni poniedziałek + 6 dni. */
    private fun lastSunday(mondays: List<LocalDate>): LocalDate =
        mondays.last().plusDays((ScheduleConstants.DAYS_IN_WEEK - 1).toLong())

    // ---------- monthGridMondays ----------

    @Test
    fun `sierpien 2026 - miesiac zaczynajacy sie w sobote ma szesc rzedow`() {
        // 1.08.2026 = sobota, 31.08.2026 = poniedziałek.
        val mondays = monthGridMondays(YearMonth.of(2026, 8))
        assertEquals(6, mondays.size)
        assertEquals(LocalDate.of(2026, 7, 27), mondays.first())
        assertEquals(
            listOf(
                LocalDate.of(2026, 7, 27),
                LocalDate.of(2026, 8, 3),
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 17),
                LocalDate.of(2026, 8, 24),
                LocalDate.of(2026, 8, 31),
            ),
            mondays,
        )
        assertEquals(LocalDate.of(2026, 9, 6), lastSunday(mondays))
    }

    @Test
    fun `miesiac zaczynajacy sie w poniedzialek startuje od pierwszego dnia`() {
        // 1.06.2026 = poniedziałek, 30.06.2026 = wtorek → 5 rzędów.
        val mondays = monthGridMondays(YearMonth.of(2026, 6))
        assertEquals(LocalDate.of(2026, 6, 1), mondays.first())
        assertEquals(5, mondays.size)
        assertEquals(LocalDate.of(2026, 7, 5), lastSunday(mondays))
    }

    @Test
    fun `luty roku nieprzestepnego od poniedzialku miesci sie w czterech rzedach`() {
        // 1.02.2027 = poniedziałek, 28.02.2027 = niedziela — dokładnie 4 tygodnie,
        // jedyny układ bez ANI JEDNEGO placeholdera.
        val mondays = monthGridMondays(YearMonth.of(2027, 2))
        assertEquals(4, mondays.size)
        assertEquals(LocalDate.of(2027, 2, 1), mondays.first())
        assertEquals(LocalDate.of(2027, 2, 28), lastSunday(mondays))
    }

    @Test
    fun `luty roku przestepnego lapie dwudziesty dziewiaty dzien`() {
        // 2028 przestępny: 1.02 = wtorek, 29.02 = wtorek → 5 rzędów.
        val mondays = monthGridMondays(YearMonth.of(2028, 2))
        assertEquals(5, mondays.size)
        assertEquals(LocalDate.of(2028, 1, 31), mondays.first())
        assertEquals(LocalDate.of(2028, 2, 28), mondays.last())
        assertEquals(LocalDate.of(2028, 3, 5), lastSunday(mondays))
    }

    @Test
    fun `miesiac trzydziestodniowy i trzydziestojednodniowy dostaja pelne tygodnie`() {
        // Kwiecień 2026 (30 dni): 1.04 = środa, 30.04 = czwartek.
        val april = monthGridMondays(YearMonth.of(2026, 4))
        assertEquals(5, april.size)
        assertEquals(LocalDate.of(2026, 3, 30), april.first())
        assertEquals(LocalDate.of(2026, 5, 3), lastSunday(april))

        // Maj 2026 (31 dni): 1.05 = piątek, 31.05 = niedziela.
        val may = monthGridMondays(YearMonth.of(2026, 5))
        assertEquals(5, may.size)
        assertEquals(LocalDate.of(2026, 4, 27), may.first())
        assertEquals(LocalDate.of(2026, 5, 31), lastSunday(may))
    }

    @Test
    fun `przelom roku - grudzien i styczen sasiaduja bez dziury`() {
        // 1.12.2026 = wtorek, 31.12.2026 = czwartek.
        val december = monthGridMondays(YearMonth.of(2026, 12))
        assertEquals(5, december.size)
        assertEquals(LocalDate.of(2026, 11, 30), december.first())
        assertEquals(LocalDate.of(2027, 1, 3), lastSunday(december))

        // 1.01.2027 = piątek, 31.01.2027 = niedziela.
        val january = monthGridMondays(YearMonth.of(2027, 1))
        assertEquals(5, january.size)
        assertEquals(LocalDate.of(2026, 12, 28), january.first())
        assertEquals(LocalDate.of(2027, 1, 31), lastSunday(january))
    }

    @Test
    fun `siatka zawsze zaczyna sie w poniedzialek i obejmuje caly miesiac bez zbednego rzedu`() {
        // Dwa lata z rzędu, żeby złapać każdą kombinację długości miesiąca i
        // dnia startu (w tym rok przestępny).
        var month = YearMonth.of(2026, 1)
        repeat(24) {
            val mondays = monthGridMondays(month)
            val label = month.toString()
            mondays.forEach { assertEquals(label, DayOfWeek.MONDAY, it.dayOfWeek) }
            // Miesiąc mieści się w siatce w całości...
            assertTrue(label, mondays.first() <= month.atDay(1))
            assertTrue(label, lastSunday(mondays) >= month.atEndOfMonth())
            // ...i ani jeden rząd nie jest w całości spoza miesiąca.
            assertTrue(label, mondays.first().plusWeeks(1) > month.atDay(1))
            assertTrue(label, mondays.last() <= month.atEndOfMonth())
            month = month.plusMonths(1)
        }
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
    fun `tytul miesiaca ma wielka litere i zawsze rok`() {
        assertEquals("Sierpień 2026", ScheduleTexts.monthTitle(YearMonth.of(2026, 8)))
        assertEquals("Styczeń 2027", ScheduleTexts.monthTitle(YearMonth.of(2027, 1)))
        assertEquals("Luty 2026", ScheduleTexts.monthTitle(YearMonth.of(2026, 2)))
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
