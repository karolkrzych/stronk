package com.stronk.ui.schedule

import java.time.DayOfWeek

/**
 * Stałe konfiguracyjne harmonogramu — wszystkie progi i słowniki w jednym
 * miejscu, zero magic numbers w kodzie ekranu/ViewModelu.
 */
object ScheduleConstants {

    /** Na ile tygodni do przodu generowane są wpisy przy przypisaniu planu. */
    const val GENERATION_WEEKS = 4

    /**
     * Rolling generation (plan BEZ bloku — [com.stronk.data.Plan.blockLengthWeeks]
     * `== null`): gdy najpóźniejszy zaplanowany wpis danego planu jest bliżej niż
     * tyle tygodni od dziś, dogenerowujemy kolejne [GENERATION_WEEKS] tygodni.
     */
    const val ROLLING_THRESHOLD_WEEKS = 2

    const val DAYS_IN_WEEK = 7

    /** Krótkie polskie nazwy dni tygodnia (nagłówki siatki tygodnia). */
    val DAY_ABBREVIATIONS: Map<DayOfWeek, String> = mapOf(
        DayOfWeek.MONDAY to "Pn",
        DayOfWeek.TUESDAY to "Wt",
        DayOfWeek.WEDNESDAY to "Śr",
        DayOfWeek.THURSDAY to "Cz",
        DayOfWeek.FRIDAY to "Pt",
        DayOfWeek.SATURDAY to "So",
        DayOfWeek.SUNDAY to "Nd",
    )

    /** Pełne polskie nazwy dni tygodnia (dialog przypisania planu). */
    val DAY_NAMES: Map<DayOfWeek, String> = mapOf(
        DayOfWeek.MONDAY to "poniedziałek",
        DayOfWeek.TUESDAY to "wtorek",
        DayOfWeek.WEDNESDAY to "środa",
        DayOfWeek.THURSDAY to "czwartek",
        DayOfWeek.FRIDAY to "piątek",
        DayOfWeek.SATURDAY to "sobota",
        DayOfWeek.SUNDAY to "niedziela",
    )

    /**
     * Domyślne rozłożenie treningów w tygodniu wg liczby dni planu —
     * propozycja startowa w dialogu przypisania (user może zmienić).
     */
    val DEFAULT_TRAINING_DAYS: Map<Int, List<DayOfWeek>> = mapOf(
        1 to listOf(DayOfWeek.MONDAY),
        2 to listOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
        3 to listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
        4 to listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
        5 to listOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
        ),
        6 to listOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
        ),
        7 to listOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY,
        ),
    )
}
