package com.stronk.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Zalogowana seria ćwiczenia — fundament ADR-003 (na razie bez UI).
 *
 * Serializacja polimorficzna kotlinx.serialization: sealed class dostaje jawne
 * pole dyskryminatora "type" (patrz [StronkJson]), wartości z @SerialName niżej.
 * To przygotowanie pod Firestore — dokument serii będzie miał stabilne pole type.
 *
 * Wspólne pola są zdefiniowane na poziomie sealed jako abstract i nadpisywane
 * w konstruktorach wariantów — tak wymaga kotlinx.serialization, żeby pola
 * bazowe trafiły do JSON-a.
 */
@Serializable
sealed class SetLog {
    /** Id ćwiczenia z datasetu. */
    abstract val exerciseId: String

    /** Id treningu, do którego należy seria. */
    abstract val workoutId: String

    /** Numer serii w ramach ćwiczenia (1-based). */
    abstract val setNumber: Int

    /** Czy to seria rozgrzewkowa. */
    abstract val isWarmup: Boolean

    /** Moment zalogowania serii (epoch millis). */
    abstract val timestamp: Long

    /** Seria na ciężar × powtórzenia (np. przysiad ze sztangą). */
    @Serializable
    @SerialName("WEIGHT_REPS")
    data class WeightReps(
        override val exerciseId: String,
        override val workoutId: String,
        override val setNumber: Int,
        override val isWarmup: Boolean,
        override val timestamp: Long,
        val kg: Double,
        val reps: Int,
    ) : SetLog()

    /** Seria na powtórzenia (masa ciała), opcjonalnie z dodatkowym obciążeniem. */
    @Serializable
    @SerialName("REPS")
    data class Reps(
        override val exerciseId: String,
        override val workoutId: String,
        override val setNumber: Int,
        override val isWarmup: Boolean,
        override val timestamp: Long,
        val reps: Int,
        val extraKg: Double? = null,
    ) : SetLog()

    /** Seria na czas (np. plank, rozciąganie). */
    @Serializable
    @SerialName("TIME")
    data class Time(
        override val exerciseId: String,
        override val workoutId: String,
        override val setNumber: Int,
        override val isWarmup: Boolean,
        override val timestamp: Long,
        val seconds: Int,
    ) : SetLog()

    /** Seria na dystans + czas (cardio). */
    @Serializable
    @SerialName("DISTANCE_TIME")
    data class DistanceTime(
        override val exerciseId: String,
        override val workoutId: String,
        override val setNumber: Int,
        override val isWarmup: Boolean,
        override val timestamp: Long,
        val meters: Double,
        val seconds: Int,
    ) : SetLog()
}
