package com.stronk.progression

/**
 * Wszystkie progi liczbowe silnika progresji (ADR-004) w jednym miejscu —
 * zero magic numbers w kodzie silnika. Zmiana zachowania progresji = zmiana
 * stałej tutaj, nigdzie indziej.
 */
object ProgressionConstants {

    // --- Reguła 1: progressive overload (WEIGHT_REPS) ---

    /** Przyrost ciężaru po zaliczonym treningu. */
    const val WEIGHT_INCREMENT_KG = 2.5

    /** Przyrost dla wielostawowych ćwiczeń nóg (przysiad, martwy itd.). */
    const val WEIGHT_INCREMENT_COMPOUND_LEG_KG = 5.0

    /** Krok zaokrąglania proponowanych ciężarów (typowe talerze). */
    const val WEIGHT_ROUNDING_KG = 2.5

    // --- Reguła 1: przyrosty dla typów bez ciężaru ---

    /** REPS: +1 powtórzenie po zaliczonym treningu. */
    const val REPS_INCREMENT = 1

    /** Minimalna sensowna liczba powtórzeń w propozycji. */
    const val REPS_MIN = 1

    /** TIME / DISTANCE_TIME: przyrost +10% po zaliczonym treningu. */
    const val TIME_INCREMENT_FACTOR = 0.10

    /** TIME: minimalny przyrost, żeby +10% z krótkich czasów nie stało w miejscu. */
    const val TIME_MIN_INCREMENT_SECONDS = 5

    /** Krok zaokrąglania sekund w propozycjach. */
    const val TIME_ROUNDING_SECONDS = 5

    /** Minimalny sensowny czas w propozycji. */
    const val TIME_MIN_SECONDS = 5

    /** DISTANCE_TIME: przyrost dystansu +10% (czas skalowany proporcjonalnie — stałe tempo). */
    const val DISTANCE_INCREMENT_FACTOR = 0.10

    /** Krok zaokrąglania metrów w propozycjach. */
    const val DISTANCE_ROUNDING_METERS = 50.0

    /** Minimalny sensowny dystans w propozycji. */
    const val DISTANCE_MIN_METERS = 50.0

    // --- Reguła 2: deload reaktywny ---

    /** Po ilu nieudanych treningach z rzędu proponujemy deload. */
    const val REACTIVE_DELOAD_FAIL_STREAK = 2

    /** Zejście przy deloadzie reaktywnym: −10%. */
    const val REACTIVE_DELOAD_REDUCTION = 0.10

    // --- Reguła 3: bloki i tydzień lekki ---

    /** Domyślna liczba tygodni pracy w bloku. */
    const val BLOCK_WORK_WEEKS_DEFAULT = 5

    /** Liczba tygodni lekkich na końcu bloku. */
    const val BLOCK_LIGHT_WEEKS = 1

    /** Domyślna pełna długość bloku (praca + tydzień lekki). */
    const val BLOCK_LENGTH_WEEKS_DEFAULT = BLOCK_WORK_WEEKS_DEFAULT + BLOCK_LIGHT_WEEKS

    /** Zejście w tygodniu lekkim: −40%. */
    const val LIGHT_WEEK_REDUCTION = 0.40

    // --- Reguła 4: ramp-up po przerwie ---

    /** Start ramp-upu: ~55% poziomu docelowego (środek widełek 50–60% z ADR-004). */
    const val RAMP_UP_START_FACTOR = 0.55

    /** Przyspieszenie progresji w ramp-upie: przyrosty × ten mnożnik, aż dogoni poziom. */
    const val RAMP_UP_INCREMENT_MULTIPLIER = 2

    // --- Pomocnicze ---

    /** Tydzień kalendarzowy w milisekundach (do liczenia pozycji w bloku). */
    const val WEEK_MILLIS: Long = 7L * 24 * 60 * 60 * 1000

    /** Tolerancja porównań zmiennoprzecinkowych (kg, metry). */
    const val EPSILON = 1e-6

    /** Wartość pola mechanic z datasetu oznaczająca ćwiczenie wielostawowe. */
    const val COMPOUND_MECHANIC = "compound"

    /** Partie z datasetu uznawane za nogi (heurystyka compound-leg → +5 kg). */
    val LEG_MUSCLES: Set<String> = setOf(
        "quadriceps",
        "hamstrings",
        "glutes",
        "calves",
        "adductors",
        "abductors",
    )
}
