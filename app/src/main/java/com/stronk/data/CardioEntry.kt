package com.stronk.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Rodzaj cardio — zamknięty słownik (poziom 1: ręczny wpis, zero GPS).
 * Wartości wire małymi literami, jak [ScheduleStatus] — jeden zwyczaj w całym
 * modelu danych. Etykieta polska żyje przy enumie, żeby UI nie budowało
 * własnych map nazw.
 */
@Serializable
enum class CardioType(val labelPl: String) {
    @SerialName("bike") BIKE("Rower"),

    @SerialName("run") RUN("Bieg"),

    @SerialName("walk") WALK("Spacer"),

    @SerialName("other") OTHER("Inne"),
}

/**
 * Wpis cardio — dokument `users/{code}/cardio/{entryId}`, kolekcja per-user
 * obok `schedule`/`workouts` (docs/firestore-data-model.md, te same konwencje:
 * dzień kalendarzowy jako "YYYY-MM-DD", czas jako epoch millis).
 * [id] to id dokumentu, nie pole w danych.
 *
 * Cardio poziom 1 jest ręczne i celowo ubogie: typ, czas w minutach i —
 * opcjonalnie — dystans. Nie dotyka silnika progresji (ADR-004) ani planów.
 */
@Serializable
data class CardioEntry(
    val id: String,
    /** Dzień cardio, "YYYY-MM-DD" — dokładnie jak [ScheduleEntry.date]. */
    val date: String,
    val type: CardioType,
    /** Czas trwania w minutach; zawsze dodatni (wpis bez czasu nie istnieje). */
    val durationMin: Int,
    /** Dystans w kilometrach — OPCJONALNY (spacer bez dystansu to normalny wpis). */
    val distanceKm: Double? = null,
    /** Moment zapisania wpisu — porządkuje kilka wpisów tego samego dnia. */
    val createdAt: Long = 0L,
)
