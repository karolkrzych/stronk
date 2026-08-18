package com.stronk.progression

import com.stronk.data.SetTarget

/**
 * Propozycja silnika progresji na następny trening dla jednego ćwiczenia
 * z planu (ADR-004). Silnik zawsze proponuje, nigdy nie wymusza — tryb
 * treningu (ADR-005) prefilluje te wartości, a user może je edytować.
 */
data class ExerciseProposal(
    /** Id ćwiczenia z bundlowanej bazy. */
    val exerciseId: String,
    /** Liczba serii roboczych (z planu — silnik nie zmienia liczby serii). */
    val sets: Int,
    /** Proponowany cel każdej serii (już po progresji/modyfikatorach). */
    val target: SetTarget,
    /**
     * Proponowany ciężar roboczy — tylko dla WEIGHT_REPS; null dla pozostałych
     * typów oraz gdy plan nie ma ciężaru startowego i nie ma jeszcze historii.
     */
    val weightKg: Double? = null,
    /** Czy propozycja jest obniżona modyfikatorem tygodnia lekkiego (−40%). */
    val isLightWeek: Boolean = false,
    /** Czy zadziałał deload reaktywny (−10% po 2 nieudanych z rzędu). */
    val isReactiveDeload: Boolean = false,
    /** Czy propozycja jest częścią ramp-upu po przerwie. */
    val isRampUp: Boolean = false,
)
