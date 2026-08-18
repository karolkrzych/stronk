package com.stronk.data

import kotlinx.serialization.Serializable

/**
 * Wykonany trening — dokument `users/{code}/workouts/{workoutId}`
 * (docs/firestore-data-model.md). Serie są embedded w dokumencie
 * (trening = dziesiątki serii = pojedyncze KB; cały trening zapisuje się
 * atomowo, bez setek mikro-dokumentów). [id] to id dokumentu, nie pole w danych.
 */
@Serializable
data class Workout(
    val id: String,
    val startedAt: Long,
    /** Null, dopóki trening trwa. */
    val finishedAt: Long? = null,
    /** Odniesienie do planu, jeśli trening był z planu. */
    val planId: String? = null,
    val dayIndex: Int? = null,
    val scheduleEntryId: String? = null,
    val notes: String? = null,
    val sets: List<SetLog> = emptyList(),
) {
    /**
     * Denormalizacja pod zapytanie "treningi z ćwiczeniem X" — wyliczana
     * z serii (jedno źródło prawdy), zapisywana do Firestore przy konwersji.
     */
    val exerciseIds: List<String>
        get() = sets.map { it.exerciseId }.distinct()
}
