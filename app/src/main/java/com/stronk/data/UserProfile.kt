package com.stronk.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Profil użytkownika — dokument `users/{code}` (docs/firestore-data-model.md).
 * Kod dostępu jest id dokumentu, nie polem w danych.
 *
 * Szczegóły profilu (cel itd.) dojdą w Fazie 3 — struktura zarezerwowana.
 */
@Serializable
data class UserProfile(
    val displayName: String? = null,
    /** Moment utworzenia profilu (epoch millis). */
    val createdAt: Long,
    val profile: ProfileDetails = ProfileDetails(),
)

/** Ustawienia profilu pod dobór ćwiczeń (Faza 3). */
@Serializable
data class ProfileDetails(
    /** Dostępny sprzęt — wartości jak w datasecie: "barbell", "dumbbell", … */
    val equipment: List<String> = emptyList(),
    /**
     * Limity per staw: klucz jak w [JointStress] (np. "knee", "lowBack"),
     * wartość = maksymalny akceptowany jointStress. Wpis tylko dla stawów
     * z ograniczeniem; ćwiczenia powyżej progu są flagowane.
     */
    val constraints: Map<String, StressLevel> = emptyMap(),
    /** Cel treningowy (moduł 2 CONCEPT); null = jeszcze nie wybrany. */
    val goal: TrainingGoal? = null,
    /** Włącza ramp-up po przerwie (ADR-004). */
    val returningFromBreak: Boolean = false,
)

/** Cel treningowy użytkownika (wartości wire małymi literami, jak [ScheduleStatus]). */
@Serializable
enum class TrainingGoal {
    @SerialName("strength") STRENGTH,
    @SerialName("mass") MASS,
    @SerialName("return_to_form") RETURN_TO_FORM,
}
