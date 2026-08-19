package com.stronk.ui.workout

/**
 * Stałe konfiguracyjne trybu treningu (ADR-005) — wszystkie progi liczbowe
 * w jednym miejscu, zero magic numbers w logice i UI.
 */
object WorkoutConstants {

    // --- Rest timer ---

    /** Domyślna długość przerwy między seriami. */
    const val DEFAULT_REST_SECONDS = 90

    /** Krok zmiany domyślnej długości przerwy (stepper w szczegółach ćwiczenia). */
    const val REST_STEP_SECONDS = 15

    /**
     * Ile dokłada przycisk „+30 s" na biegnącym timerze (mocki `pack-trening`,
     * ekran 2 — przycisk w proporcji 1:4 obok „Pomiń przerwę").
     */
    const val REST_EXTEND_SECONDS = 30

    /** Minimalna/maksymalna konfigurowalna długość przerwy. */
    const val REST_MIN_SECONDS = 15
    const val REST_MAX_SECONDS = 600

    /** Co ile odświeżany jest zegar przerwy w UI. */
    const val UI_TICK_MILLIS = 250L

    /** Margines wake locka ponad planowany koniec przerwy (serwis timera). */
    const val WAKE_LOCK_MARGIN_MILLIS = 10_000L

    /** Wzór wibracji na koniec przerwy: pauza/drga/pauza/drga (millis). */
    val REST_END_VIBRATION_PATTERN = longArrayOf(0, 300, 150, 300)

    // --- Kroki edycji wartości serii (dialog odstępstwa) ---

    const val WEIGHT_EDIT_STEP_KG = 2.5
    const val REPS_EDIT_STEP = 1
    const val TIME_EDIT_STEP_SECONDS = 5
    const val DISTANCE_EDIT_STEP_METERS = 50.0

    // --- Domyślne cele po podmianie na ćwiczenie o innym typie pomiaru ---
    // (cel oryginału nie da się przenieść 1:1, więc startujemy od rozsądnych wartości)

    const val SUBSTITUTE_DEFAULT_REPS = 10
    const val SUBSTITUTE_DEFAULT_TIME_SECONDS = 30
    const val SUBSTITUTE_DEFAULT_DISTANCE_METERS = 1000.0
    const val SUBSTITUTE_DEFAULT_DISTANCE_SECONDS = 420
}
