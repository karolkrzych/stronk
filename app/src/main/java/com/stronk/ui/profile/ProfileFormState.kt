package com.stronk.ui.profile

import com.stronk.data.ProfileDetails
import com.stronk.data.StressLevel
import com.stronk.data.TrainingGoal
import com.stronk.data.UserProfile

/** Stałe konfiguracyjne ekranu profilu — jedno miejsce, zero magic numbers. */
object ProfileDefaults {

    /** Debounce zapisu imienia — bez niego każda litera to osobny zapis Firestore. */
    const val NAME_SAVE_DEBOUNCE_MS = 600L

    /**
     * Klucze stawów w kolejności prezentacji — dokładnie te co w
     * [com.stronk.data.JointStress.all] (test pilnuje zgodności).
     * Dolny odcinek pleców i kolano na górze — główne ograniczenia usera nr 1.
     */
    val JOINT_KEYS = listOf("lowBack", "knee", "shoulder", "hip", "elbow", "wrist", "neck")

    /**
     * Wartość wire dla "brak ograniczenia". Profil zapisuje się przez
     * `SetOptions.merge()`, a merge NIE usuwa kluczy zagnieżdżonej mapy — samo
     * wyrzucenie wpisu z `constraints` zostawiłoby stary limit na serwerze
     * i w cache. Dlatego zapisujemy KAŻDY staw jawnie, a "brak ograniczenia"
     * koduje HIGH: żadne obciążenie nie przekracza HIGH, więc
     * [com.stronk.data.isCompliant] niczego nie flaguje — semantycznie
     * to samo co brak wpisu.
     */
    val NO_LIMIT_WIRE_LEVEL = StressLevel.HIGH
}

/**
 * Stan formularza profilu — lokalna prawda ekranu (write-through do Firestore).
 * [constraints] trzyma wyłącznie realne limity (LOW/MEDIUM); brak wpisu = brak
 * ograniczenia. Kodowanie wire (HIGH dla reszty stawów) robi [toUserProfile].
 */
data class ProfileFormState(
    val displayName: String = "",
    val equipment: Set<String> = emptySet(),
    val constraints: Map<String, StressLevel> = emptyMap(),
    val goal: TrainingGoal? = null,
    val returningFromBreak: Boolean = false,
)

/** Formularz z profilu z Firestore; null (dokument nie istnieje) → defaulty. */
fun profileFormFrom(profile: UserProfile?): ProfileFormState {
    val details = profile?.profile ?: return ProfileFormState()
    return ProfileFormState(
        displayName = profile.displayName.orEmpty(),
        equipment = details.equipment.toSet(),
        constraints = details.constraints.mapNotNull { (joint, level) ->
            when (level) {
                // Wire "brak ograniczenia" (patrz ProfileDefaults.NO_LIMIT_WIRE_LEVEL).
                StressLevel.HIGH -> null
                // UI nie ma opcji NONE — najbliższy dostępny limit to LOW.
                StressLevel.NONE -> joint to StressLevel.LOW
                else -> joint to level
            }
        }.toMap(),
        goal = details.goal,
        returningFromBreak = details.returningFromBreak,
    )
}

/**
 * Profil do zapisu: wszystkie 7 stawów jawnie (merge nie usuwa kluczy mapy),
 * imię przycięte (puste "" też zapisywane, żeby merge nadpisał stare),
 * sprzęt posortowany dla deterministycznego wire.
 */
fun ProfileFormState.toUserProfile(createdAt: Long): UserProfile = UserProfile(
    displayName = displayName.trim(),
    createdAt = createdAt,
    profile = ProfileDetails(
        equipment = equipment.sorted(),
        constraints = ProfileDefaults.JOINT_KEYS.associateWith { joint ->
            constraints[joint] ?: ProfileDefaults.NO_LIMIT_WIRE_LEVEL
        },
        goal = goal,
        returningFromBreak = returningFromBreak,
    ),
)
