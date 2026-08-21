package com.stronk.ui.schedule

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Etykiety ekranu Tydzień — jedno miejsce na wszystkie polskie teksty tego modułu.
 *
 * TWARDA ZASADA (mocki „Limonka", zasady Karola): tu NIE POWSTAJE fraza typu
 * „3×12" ani „40 kg × 10 powt.". Liczba serii jedzie jako osobny byt — chip
 * [setsLabel] („3 serie"); ciężar i powtórzenia mają własne staty na ekranie
 * treningu, nie w skrócie na liście.
 */
internal object ScheduleTexts {

    private val polishLocale: Locale = Locale.forLanguageTag("pl")

    /** „środa", „poniedziałek" — pełna nazwa dnia tygodnia. */
    private val weekdayFormatter = DateTimeFormatter.ofPattern("EEEE", polishLocale)

    /** „środa 19 sierpnia" — dzień z datą, bez roku. */
    private val fullDayFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", polishLocale)

    /** „sierpień" — nazwa miesiąca w mianowniku (wzorzec standalone). */
    private val monthFormatter = DateTimeFormatter.ofPattern("LLLL", polishLocale)

    /** „środa, 19 sierpnia" — data startu w dialogu przypisania planu. */
    private val startDateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", polishLocale)

    /** Chip liczby serii w wierszu ćwiczenia (mock: `.exrow .chip` = „3 serie"). */
    fun setsLabel(sets: Int): String = "$sets ${setWord(sets)}"

    /** Nagłówek ekranu: pozycja tygodnia w bloku (mock: `.h1` = „Tydzień 1/6"). */
    fun blockWeekLabel(weekNumber: Int, weekCount: Int): String = "Tydzień $weekNumber/$weekCount"

    /**
     * Nagłówek dla planu BEZ bloku: sam numer tygodnia, bez „/Y" — plan nie ma
     * końca, więc mianownik nie istnieje.
     */
    fun continuousWeekLabel(weekNumber: Int): String = "Tydzień $weekNumber"

    /** Nagłówek dla planu z blokiem albo bez ([weekCount] null = bez bloku). */
    fun weekHeaderLabel(weekNumber: Int, weekCount: Int?): String =
        if (weekCount == null) {
            continuousWeekLabel(weekNumber)
        } else {
            blockWeekLabel(weekNumber, weekCount)
        }

    /**
     * Podtytuł nagłówka: miesiąc (albo dwa) objęte siatką bloku, np. „Sierpień"
     * lub „Sierpień – wrzesień". Rok dopisywany tylko wtedy, gdy siatka wychodzi
     * poza rok [today] — bez szumu w typowym widoku.
     */
    fun monthRangeLabel(from: LocalDate, to: LocalDate, today: LocalDate): String {
        val sameMonth = from.month == to.month && from.year == to.year
        val base = if (sameMonth) {
            monthFormatter.format(from)
        } else {
            "${monthFormatter.format(from)} – ${monthFormatter.format(to)}"
        }
        val yearSuffix = if (from.year != today.year || to.year != today.year) " ${to.year}" else ""
        return base.replaceFirstChar { it.titlecase(polishLocale) } + yearSuffix
    }

    /** Tytuł karty dnia (mock: `.daycard .nm` = „Środa · Full body B"). */
    fun dayCardTitle(date: LocalDate, dayName: String?): String {
        val weekday = weekdayFormatter.format(date).replaceFirstChar { it.titlecase(polishLocale) }
        return if (dayName.isNullOrBlank()) weekday else "$weekday · $dayName"
    }

    /** Etykieta wybranego dnia poza kartą treningu („Dziś", „Środa 19 sierpnia"). */
    fun selectedDayLabel(date: LocalDate, today: LocalDate): String =
        if (date == today) {
            "Dziś"
        } else {
            fullDayFormatter.format(date).replaceFirstChar { it.titlecase(polishLocale) }
        }

    /** Data docelowa przesuniętego treningu („środa 19 sierpnia"). */
    fun movedToLabel(date: LocalDate): String = fullDayFormatter.format(date)

    /** Data startu w dialogu przypisania planu („środa, 19 sierpnia"). */
    fun startDateLabel(date: LocalDate): String = startDateFormatter.format(date)

    /**
     * CTA przypisania planu w [AssignPlanDialog]. Plan z blokiem faktycznie
     * generuje dokładnie [ScheduleConstants.GENERATION_WEEKS] tygodni na raz —
     * tekst to mówi wprost. Plan bez bloku dogeneruje sobie kolejne tygodnie
     * sam (rolling generation), więc „X tyg." by kłamało — samo „Zaplanuj".
     */
    fun assignPlanCta(continuous: Boolean): String =
        if (continuous) "Zaplanuj" else "Zaplanuj ${ScheduleConstants.GENERATION_WEEKS} tyg."

    /** Notka pod przypisaniami dni — różna treść dla planu z blokiem i bez. */
    fun assignPlanNote(continuous: Boolean): String =
        if (continuous) {
            "Harmonogram przedłuża się sam — kolejne tygodnie dopiszą się, gdy zapas się skróci."
        } else {
            "Wpisy na ${ScheduleConstants.GENERATION_WEEKS} tygodnie; zajęte dni pomijamy."
        }

    /** Po nieudanym potwierdzeniu: cały wybrany okres zajmuje już TEN SAM plan. */
    const val PERIOD_ALREADY_PLANNED = "Ten okres jest już zaplanowany."

    /** Notka w dialogu, gdy okres koliduje z INNYM planem — blokuje CTA. */
    fun periodConflictNote(otherPlanName: String): String =
        "Ten okres ma już zaplanowany plan „$otherPlanName\"."

    private fun setWord(count: Int): String = when {
        count == 1 -> "seria"
        count % 10 in 2..4 && count % 100 !in 12..14 -> "serie"
        else -> "serii"
    }
}
