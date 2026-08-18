package com.stronk.ui.plans

import com.stronk.data.ComplianceResult
import com.stronk.data.Exercise
import com.stronk.data.ProfileDetails
import com.stronk.data.isCompliant

/**
 * Sugestie pokrycia partii mięśniowych w dniu planu — mechaniczne (bez AI),
 * jak reszta modułu 3: duże grupy mięśniowe nieobecne wśród ćwiczeń dnia,
 * plus 2–3 kandydaci z bazy zgodni ze sprzętem i ograniczeniami z profilu
 * ([isCompliant], jak [com.stronk.data.findSubstitutes]).
 */
enum class MuscleGroup(val label: String) {
    CHEST("klatka"),
    BACK("plecy"),
    LEGS("nogi"),
    SHOULDERS("barki"),
    CORE("core"),
}

/**
 * Mapowanie kluczy partii z datasetu (jak w [com.stronk.ui.PlLabels]) na duże
 * grupy — celowo tylko te, które warto pilnować w rozpisce dnia; drobne partie
 * (biceps, triceps, przedramię, łydki, kark) nie generują sugestii.
 */
private val MAJOR_GROUP_BY_MUSCLE: Map<String, MuscleGroup> = mapOf(
    "chest" to MuscleGroup.CHEST,
    "lats" to MuscleGroup.BACK,
    "middle back" to MuscleGroup.BACK,
    "lower back" to MuscleGroup.BACK,
    "traps" to MuscleGroup.BACK,
    "quadriceps" to MuscleGroup.LEGS,
    "hamstrings" to MuscleGroup.LEGS,
    "glutes" to MuscleGroup.LEGS,
    "calves" to MuscleGroup.LEGS,
    "abductors" to MuscleGroup.LEGS,
    "adductors" to MuscleGroup.LEGS,
    "shoulders" to MuscleGroup.SHOULDERS,
    "abdominals" to MuscleGroup.CORE,
)

/** Reprezentatywny klucz partii danej grupy — do doboru ikony ([com.stronk.ui.components.MuscleIcons]). */
fun MuscleGroup.representativeMuscleKey(): String = when (this) {
    MuscleGroup.CHEST -> "chest"
    MuscleGroup.BACK -> "lats"
    MuscleGroup.LEGS -> "quadriceps"
    MuscleGroup.SHOULDERS -> "shoulders"
    MuscleGroup.CORE -> "abdominals"
}

/** Duże grupy pokryte przez [exercises] (po `primaryMuscles`). */
fun coveredMajorGroups(exercises: List<Exercise>): Set<MuscleGroup> =
    exercises.flatMap { it.primaryMuscles }.mapNotNull { MAJOR_GROUP_BY_MUSCLE[it] }.toSet()

/** Duże grupy BRAKUJĄCE wśród [exercises], w stałej kolejności (deterministyczne UI). */
fun missingMajorGroups(exercises: List<Exercise>): List<MuscleGroup> {
    val covered = coveredMajorGroups(exercises)
    return MuscleGroup.entries.filterNot { it in covered }
}

/**
 * Do [limit] (domyślnie 2–3) propozycji ćwiczeń na brakującą grupę: sprzęt musi
 * być dostępny (dyskwalifikuje, jak w [com.stronk.data.findSubstitutes]),
 * ćwiczenia już obecne w dniu ([excludeIds]) są pomijane. Zgodne z limitami
 * stawów wygrywają z naruszającymi; remisy po polskiej nazwie (deterministycznie).
 */
fun suggestExercisesForGroup(
    group: MuscleGroup,
    allExercises: List<Exercise>,
    profile: ProfileDetails,
    excludeIds: Set<String>,
    limit: Int = 3,
): List<Exercise> = allExercises.asSequence()
    .filter { it.id !in excludeIds }
    .filter { exercise -> exercise.primaryMuscles.any { MAJOR_GROUP_BY_MUSCLE[it] == group } }
    .map { exercise -> exercise to isCompliant(exercise, profile) }
    .filter { (_, compliance) -> compliance.equipmentAvailable }
    .sortedWith(
        compareBy<Pair<Exercise, ComplianceResult>> { (_, compliance) -> compliance.constraintViolations.size }
            .thenBy { (exercise, _) -> exercise.namePl },
    )
    .map { (exercise, _) -> exercise }
    .take(limit)
    .toList()
