package com.stronk.ui.plans

import com.stronk.data.Exercise
import com.stronk.data.MeasurementType
import com.stronk.data.PlanDay
import com.stronk.data.PlanExercise
import com.stronk.data.ProfileDetails
import com.stronk.data.SetTarget
import com.stronk.data.findSubstitutes
import com.stronk.data.isCompliant

/**
 * Parametryzacja presetów profilem i budowa domyślnych celów — czyste funkcje
 * (zero Androida), testowalne jednostkowo jak [com.stronk.data.findSubstitutes].
 */

/** Domyślny cel serii dla typu pomiaru; [reps] używane tylko przy typach na powtórzenia. */
fun defaultTargetFor(
    measurementType: MeasurementType,
    reps: Int = PlanDefaults.DEFAULT_REPS,
): SetTarget = when (measurementType) {
    MeasurementType.WEIGHT_REPS -> SetTarget.WeightReps(reps)
    MeasurementType.REPS -> SetTarget.Reps(reps)
    MeasurementType.TIME -> SetTarget.Time(PlanDefaults.DEFAULT_TIME_SECONDS)
    MeasurementType.DISTANCE_TIME -> SetTarget.DistanceTime(
        meters = PlanDefaults.DEFAULT_DISTANCE_METERS,
        seconds = PlanDefaults.DEFAULT_DISTANCE_SECONDS,
    )
}

/**
 * Przełożenie celu na inny typ pomiaru (podmiana ćwiczenia na zamiennik):
 * powtórzenia przenoszą się między WEIGHT_REPS i REPS, zgodny typ zostaje
 * bez zmian, reszta dostaje wartości domyślne.
 */
fun convertTarget(old: SetTarget, newType: MeasurementType): SetTarget {
    val oldReps = when (old) {
        is SetTarget.WeightReps -> old.reps
        is SetTarget.Reps -> old.reps
        else -> null
    }
    return when (newType) {
        MeasurementType.WEIGHT_REPS -> SetTarget.WeightReps(oldReps ?: PlanDefaults.DEFAULT_REPS)
        MeasurementType.REPS -> SetTarget.Reps(oldReps ?: PlanDefaults.DEFAULT_REPS)
        MeasurementType.TIME -> old as? SetTarget.Time
            ?: SetTarget.Time(PlanDefaults.DEFAULT_TIME_SECONDS)
        MeasurementType.DISTANCE_TIME -> old as? SetTarget.DistanceTime
            ?: SetTarget.DistanceTime(
                meters = PlanDefaults.DEFAULT_DISTANCE_METERS,
                seconds = PlanDefaults.DEFAULT_DISTANCE_SECONDS,
            )
    }
}

/**
 * Wybór ćwiczenia do slotu presetu pod profil:
 * 1. pierwszy kandydat w pełni zgodny (sprzęt + limity stawów) wygrywa,
 * 2. brak zgodnego → zamiennik bez naruszeń przez [findSubstitutes]
 *    dla najbardziej preferowanego istniejącego kandydata,
 * 3. dalej brak → pierwszy kandydat z dostępnym sprzętem
 *    (naruszenia limitów oflaguje edytor — filozofia CONCEPT: flagować, nie ukrywać),
 * 4. dalej brak → pierwszy istniejący kandydat,
 * 5. żaden kandydat nie istnieje w datasecie → null (slot pominięty).
 *
 * [usedIds] pozwala unikać duplikatów w ramach dnia — użyte ćwiczenia są
 * pomijane, chyba że nie zostaje żaden kandydat.
 */
fun resolveSlotExercise(
    slot: PresetSlot,
    exercisesById: Map<String, Exercise>,
    allExercises: List<Exercise>,
    profile: ProfileDetails,
    usedIds: Set<String> = emptySet(),
): Exercise? {
    val existingAll = slot.candidateIds.mapNotNull { exercisesById[it] }
    if (existingAll.isEmpty()) return null
    val existing = existingAll.filter { it.id !in usedIds }.ifEmpty { existingAll }

    existing.firstOrNull { isCompliant(it, profile).isFullyCompliant }?.let { return it }

    findSubstitutes(existing.first(), allExercises, profile, PlanDefaults.SUBSTITUTE_LIMIT)
        .firstOrNull { it.warnings.isEmpty() && it.exercise.id !in usedIds }
        ?.let { return it.exercise }

    existing.firstOrNull { isCompliant(it, profile).equipmentAvailable }?.let { return it }
    return existing.first()
}

/**
 * Buduje dni planu z presetu pod profil użytkownika. Wynik trafia do edytora
 * do przejrzenia — nic nie jest zapisywane tutaj. Sloty bez żadnego
 * istniejącego kandydata są pomijane.
 */
fun generatePresetDays(
    preset: PlanPreset,
    allExercises: List<Exercise>,
    profile: ProfileDetails,
): List<PlanDay> {
    val byId = allExercises.associateBy { it.id }
    return preset.days.map { day ->
        val usedIds = mutableSetOf<String>()
        val exercises = day.slots.mapNotNull { slot ->
            val exercise =
                resolveSlotExercise(slot, byId, allExercises, profile, usedIds)
                    ?: return@mapNotNull null
            usedIds += exercise.id
            PlanExercise(
                exerciseId = exercise.id,
                sets = slot.sets,
                target = defaultTargetFor(exercise.measurementType, slot.reps),
            )
        }
        PlanDay(name = day.name, exercises = exercises)
    }
}
