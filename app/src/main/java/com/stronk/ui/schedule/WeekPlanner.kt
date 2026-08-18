package com.stronk.ui.schedule

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * Czyste funkcje kalendarza tygodnia i generacji wpisów harmonogramu
 * (zero Androida — testowalne na JVM, wzorzec jak ExerciseFilter/SubstituteFinder).
 * Daty jako [LocalDate] (bez stref — trening to dzień, nie chwila);
 * na wire konwertuje wołający przez `toString()` = "YYYY-MM-DD".
 */

/** Zaplanowany slot treningu: data + indeks dnia planu. */
data class PlannedSlot(val date: LocalDate, val dayIndex: Int)

private val polishLocale = Locale.forLanguageTag("pl")
private val dayMonthFormatter = DateTimeFormatter.ofPattern("d MMMM", polishLocale)
private val dayMonthYearFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", polishLocale)

/** Poniedziałek tygodnia (ISO), w którym leży [date]. */
fun weekStartOf(date: LocalDate): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/**
 * Etykieta zakresu tygodnia od [weekStart], np. "10–16 sierpnia" (jeden miesiąc)
 * albo "28 lipca – 3 sierpnia" (przełom miesięcy). Rok dopisywany tylko wtedy,
 * gdy różni się od roku [today] — bez szumu w typowym widoku.
 */
fun weekLabel(weekStart: LocalDate, today: LocalDate): String {
    val weekEnd = weekStart.plusDays((ScheduleConstants.DAYS_IN_WEEK - 1).toLong())
    val endLabel =
        if (weekEnd.year == today.year) dayMonthFormatter.format(weekEnd)
        else dayMonthYearFormatter.format(weekEnd)
    return when {
        weekStart.month == weekEnd.month && weekStart.year == weekEnd.year ->
            "${weekStart.dayOfMonth}–$endLabel"

        weekStart.year == weekEnd.year ->
            "${dayMonthFormatter.format(weekStart)} – $endLabel"

        else -> "${dayMonthYearFormatter.format(weekStart)} – $endLabel"
    }
}

/**
 * Domyślne przypisanie dni planu do dni tygodnia (dzień tygodnia → indeks dnia
 * planu) wg [ScheduleConstants.DEFAULT_TRAINING_DAYS]. Plan dłuższy niż 7 dni
 * dostaje przypisane pierwsze 7 — resztę user rozkłada ręcznie.
 */
fun defaultAssignments(planDayCount: Int): Map<DayOfWeek, Int> {
    if (planDayCount <= 0) return emptyMap()
    val weekdays = ScheduleConstants.DEFAULT_TRAINING_DAYS
        .getValue(planDayCount.coerceAtMost(ScheduleConstants.DAYS_IN_WEEK))
    return weekdays.mapIndexed { index, dayOfWeek -> dayOfWeek to index }.toMap()
}

/**
 * Sloty "planned" wygenerowane z przypisania dni planu do dni tygodnia.
 *
 * [assignments]: dzień tygodnia → indeks dnia planu; jeden dzień planu może
 * wystąpić w kilku dniach tygodnia (np. full body 3×/tydz.), ale dzień
 * tygodnia ma najwyżej jeden trening.
 *
 * Okno generacji to [startDate] włącznie + [weeks] pełnych tygodni
 * ("najbliższe N tygodni od daty startu", nie N tygodni kalendarzowych).
 * Daty z [occupiedDates] (dni z już aktywnym wpisem) są pomijane —
 * nie dublujemy treningów na zajętym dniu.
 */
fun generatePlannedSlots(
    assignments: Map<DayOfWeek, Int>,
    startDate: LocalDate,
    weeks: Int = ScheduleConstants.GENERATION_WEEKS,
    occupiedDates: Set<LocalDate> = emptySet(),
): List<PlannedSlot> {
    if (weeks <= 0 || assignments.isEmpty()) return emptyList()
    val firstMonday = weekStartOf(startDate)
    val endExclusive = startDate.plusWeeks(weeks.toLong())
    // 0..weeks (włącznie): okno kroczące może zahaczać o tydzień kalendarzowy
    // za ostatnim pełnym — filtr dat przycina nadmiar.
    return (0..weeks).flatMap { week ->
        val monday = firstMonday.plusWeeks(week.toLong())
        assignments.map { (dayOfWeek, dayIndex) -> PlannedSlot(monday.with(dayOfWeek), dayIndex) }
    }
        .filter { it.date >= startDate && it.date < endExclusive && it.date !in occupiedDates }
        .sortedBy { it.date }
}
