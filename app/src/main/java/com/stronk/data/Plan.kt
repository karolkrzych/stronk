package com.stronk.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Plan treningowy — dokument `users/{code}/plans/{planId}`
 * (docs/firestore-data-model.md). Plan jest mały (kilka dni × kilka ćwiczeń),
 * więc całość żyje w jednym dokumencie, dni jako tablica embedded.
 * [id] to id dokumentu, nie pole w danych.
 */
@Serializable
data class Plan(
    val id: String,
    val name: String,
    val createdAt: Long,
    val archived: Boolean = false,
    val days: List<PlanDay> = emptyList(),
)

/** Jeden dzień planu (np. "Push A", "Nogi"). */
@Serializable
data class PlanDay(
    val name: String,
    val exercises: List<PlanExercise> = emptyList(),
)

/** Ćwiczenie w dniu planu. */
@Serializable
data class PlanExercise(
    /** Id z bundlowanej bazy (np. "Barbell_Squat"). */
    val exerciseId: String,
    /** Liczba serii roboczych. */
    val sets: Int,
    val target: SetTarget,
    /** Ciężar startowy — tylko dla WEIGHT_REPS. */
    val startWeightKg: Double? = null,
    /** Czy silnik progresji (ADR-004) ma prowadzić to ćwiczenie. */
    val progressionEnabled: Boolean = true,
)

/**
 * Cel serii per typ pomiaru — sealed z jawnym polem "type" o tych samych
 * wartościach co enum [MeasurementType] (jeden słownik pojęć, jak [SetLog]).
 */
@Serializable
sealed class SetTarget {

    /** Cel na powtórzenia przy ciężarze prowadzonym przez progresję. */
    @Serializable
    @SerialName("WEIGHT_REPS")
    data class WeightReps(val reps: Int) : SetTarget()

    /** Cel na powtórzenia (masa ciała). */
    @Serializable
    @SerialName("REPS")
    data class Reps(val reps: Int) : SetTarget()

    /** Cel na czas. */
    @Serializable
    @SerialName("TIME")
    data class Time(val seconds: Int) : SetTarget()

    /** Cel na dystans + czas (cardio). */
    @Serializable
    @SerialName("DISTANCE_TIME")
    data class DistanceTime(val meters: Double, val seconds: Int) : SetTarget()
}
