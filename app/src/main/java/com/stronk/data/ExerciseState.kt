package com.stronk.data

import kotlinx.serialization.Serializable

/**
 * Zmaterializowany stan per ćwiczenie pod silnik progresji (ADR-004) —
 * dokument `users/{code}/exerciseState/{exerciseId}`, aktualizowany przy
 * zapisie treningu zamiast skanowania historii (działa offline i natychmiast).
 * Konsumowany od Fazy 5/6, ale zapisywany od pierwszego logu — historia
 * stanu buduje się od początku. [exerciseId] to id dokumentu, nie pole w danych.
 */
@Serializable
data class ExerciseState(
    val exerciseId: String,
    /** Ostatni wynik — prefill "ostatnio X kg × Y". */
    val lastSets: List<SetLog> = emptyList(),
    /** Liczba nieudanych treningów z rzędu — pod deload reaktywny (ADR-004). */
    val failStreak: Int = 0,
    /** Bieżący ciężar roboczy wg progresji (tylko WEIGHT_REPS). */
    val currentWeightKg: Double? = null,
    val updatedAt: Long,
)
