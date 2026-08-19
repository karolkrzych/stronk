package com.stronk.ui.schedule

import java.time.DayOfWeek

/**
 * Stałe konfiguracyjne harmonogramu — wszystkie progi i słowniki w jednym
 * miejscu, zero magic numbers w kodzie ekranu/ViewModelu.
 */
object ScheduleConstants {

    /** Na ile tygodni do przodu generowane są wpisy przy przypisaniu planu. */
    const val GENERATION_WEEKS = 4

    const val DAYS_IN_WEEK = 7

    /**
     * Siatka kwadratów (mock „Tydzień"): 7 kolumn × tyle rzędów, ile ma blok —
     * ale nigdy mniej niż [GRID_WEEKS_MIN] (za chuda siatka przestaje być
     * dominantą ekranu) i nigdy więcej niż [GRID_WEEKS_MAX] (dłuższy blok
     * pokazujemy oknem wokół bieżącego tygodnia).
     */
    const val GRID_WEEKS_MIN = 4

    const val GRID_WEEKS_MAX = 6

    /**
     * Plan BEZ bloku nie ma czego pokazać w całości — siatka dostaje wtedy stałe
     * okno [GRID_WEEKS_CONTINUOUS] tygodni wokół bieżącego, z
     * [GRID_WEEKS_CONTINUOUS_PAST] tygodniami przeszłości dla kontekstu.
     */
    const val GRID_WEEKS_CONTINUOUS = 5

    const val GRID_WEEKS_CONTINUOUS_PAST = 1

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
