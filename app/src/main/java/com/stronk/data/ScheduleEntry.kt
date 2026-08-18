package com.stronk.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Status zaplanowanego treningu (wartości wire małymi literami, jak w modelu danych). */
@Serializable
enum class ScheduleStatus {
    @SerialName("planned") PLANNED,
    @SerialName("done") DONE,
    @SerialName("skipped") SKIPPED,
    @SerialName("moved") MOVED,
}

/**
 * Wpis harmonogramu — jeden zaplanowany trening, dokument
 * `users/{code}/schedule/{entryId}` (docs/firestore-data-model.md).
 * Trening to dzień kalendarzowy, nie chwila — stąd data jako "YYYY-MM-DD"
 * (zero problemów ze strefami). [id] to id dokumentu, nie pole w danych.
 */
@Serializable
data class ScheduleEntry(
    val id: String,
    /** Dzień treningu, "YYYY-MM-DD". */
    val date: String,
    val planId: String,
    /** Indeks dnia w [Plan.days]. */
    val dayIndex: Int,
    val status: ScheduleStatus = ScheduleStatus.PLANNED,
    /** Przy status=MOVED: nowa data "YYYY-MM-DD". */
    val movedTo: String? = null,
    /** Przy status=DONE: link do logu treningu. */
    val workoutId: String? = null,
)
