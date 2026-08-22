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
    /**
     * Liczba tygodni PRACY w bloku (ADR-004; konfigurowalna w kreatorze planu),
     * bez tygodnia lekkiego — albo **null = plan BEZ bloku**: progresja leci
     * ciągiem, NIGDY nie ma tygodnia lekkiego, plan może biec w nieskończoność.
     *
     * Do silnika progresji przekazuje się pełną długość bloku, czyli
     * `ProgressionEngine.fullBlockWeeks(blockLengthWeeks)` (null zostaje nullem).
     * Nowe plany ręczne startują bez bloku; presety włączają go jawnie.
     */
    val blockLengthWeeks: Int? = null,
    /**
     * Wzorzec dnia tygodnia → indeks dnia planu (klucz = ISO 1=poniedziałek..
     * 7=niedziela) zapisany przy ostatnim zatwierdzeniu dialogu planowania
     * tygodnia — źródło prawdy dla materializacji harmonogramu
     * (`ScheduleViewModel.onAssignPlan`) i rolling generation
     * (`ScheduleViewModel.maybeExtendContinuousPlans`), niezależne od tego,
     * co akurat leży w kolekcji `schedule` (pojedyncze „Przesuń" treningu nie
     * ma prawa zmienić tej reguły).
     *
     * `null` = wzorzec nigdy nie zapisany (stary dokument sprzed tego pola,
     * albo plan jeszcze nigdy nie przypisany do tygodnia) — wtedy odczyt
     * spada na `deriveWeekAssignments` z istniejących wpisów PLANNED. Pusta
     * mapa (w odróżnieniu od `null`) to ŚWIADOMY wybór usera: wyzerował
     * wszystkie dni.
     */
    val weekdayAssignments: Map<Int, Int>? = null,
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
