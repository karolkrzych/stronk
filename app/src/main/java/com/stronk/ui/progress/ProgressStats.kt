package com.stronk.ui.progress

import com.stronk.data.SetLog
import com.stronk.data.Workout

/**
 * Czyste funkcje statystyk progresu (moduł 6 CONCEPT): rekordy osobiste
 * i punkty wykresu liczone client-side z historii treningów. Zero Androida —
 * wszystko testowalne jednostkowo. Serie rozgrzewkowe nigdy się nie liczą.
 */

/** Rodzaj rekordu osobistego. */
enum class PrKind {
    /** Najcięższa pojedyncza seria (kg) — WEIGHT_REPS. */
    MAX_WEIGHT,

    /** Najlepsza objętość sesji (suma kg × powtórzenia) — WEIGHT_REPS. */
    SESSION_VOLUME,

    /** Najwięcej powtórzeń w serii — REPS. */
    MAX_REPS,

    /** Najdłuższy czas serii — TIME. */
    MAX_TIME,

    /** Najdłuższy dystans serii — DISTANCE_TIME. */
    MAX_DISTANCE,
}

/**
 * Priorytet rodzajów rekordu przy wyborze "głównego" rekordu ćwiczenia
 * (np. w wierszu listy pokazujemy jeden — najbardziej charakterystyczny).
 */
internal val PR_KIND_PRIORITY = listOf(
    PrKind.MAX_WEIGHT,
    PrKind.MAX_REPS,
    PrKind.MAX_TIME,
    PrKind.MAX_DISTANCE,
    PrKind.SESSION_VOLUME,
)

/** Rekord osobisty jednego ćwiczenia w jednym rodzaju. */
data class PersonalRecord(
    val exerciseId: String,
    val kind: PrKind,
    val value: Double,
    /**
     * Trening, który PIERWSZY osiągnął tę wartość — wyrównanie rekordu
     * w późniejszym treningu nie przejmuje autorstwa (i nie jest "nowym PR").
     */
    val workoutId: String,
    /** startedAt treningu, w którym padł rekord. */
    val achievedAt: Long,
)

/** Punkt wykresu progresu — jeden trening. */
data class ChartPoint(val startedAt: Long, val value: Double)

/** Metryka wykresu progresu ćwiczenia. */
enum class ChartMetric { WEIGHT, VOLUME, REPS, TIME, DISTANCE }

/** Serie robocze treningu (bez rozgrzewkowych), opcjonalnie jednego ćwiczenia. */
fun workingSets(workout: Workout, exerciseId: String? = null): List<SetLog> =
    workout.sets.filter { !it.isWarmup && (exerciseId == null || it.exerciseId == exerciseId) }

/**
 * Objętość treningu: suma kg × powtórzenia serii roboczych WEIGHT_REPS.
 * Serie REPS z extraKg świadomie pominięte — objętość liczymy tylko tam,
 * gdzie ciężar jest pierwszorzędną miarą.
 */
fun workoutVolume(workout: Workout): Double =
    workingSets(workout).filterIsInstance<SetLog.WeightReps>().sumOf { it.kg * it.reps }

/** Objętość jednej sesji dla jednego ćwiczenia (suma kg × powtórzenia, WEIGHT_REPS). */
fun sessionVolume(workout: Workout, exerciseId: String): Double =
    workingSets(workout, exerciseId).filterIsInstance<SetLog.WeightReps>()
        .sumOf { it.kg * it.reps }

/**
 * Rekordy osobiste per ćwiczenie i rodzaj, wyliczone z całej historii.
 * Kolejność treningów wejściowych nie ma znaczenia (sortowanie w środku).
 */
fun computePersonalRecords(workouts: List<Workout>): List<PersonalRecord> {
    val records = mutableMapOf<Pair<String, PrKind>, PersonalRecord>()

    fun offer(exerciseId: String, kind: PrKind, value: Double, workout: Workout) {
        if (value <= 0.0) return
        val key = exerciseId to kind
        val current = records[key]
        // Ostra nierówność: chronologicznie pierwszy trening z tą wartością
        // zostaje autorem rekordu.
        if (current == null || value > current.value) {
            records[key] = PersonalRecord(exerciseId, kind, value, workout.id, workout.startedAt)
        }
    }

    for (workout in workouts.sortedBy { it.startedAt }) {
        for ((exerciseId, sets) in workingSets(workout).groupBy { it.exerciseId }) {
            val weightSets = sets.filterIsInstance<SetLog.WeightReps>()
            weightSets.maxOfOrNull { it.kg }
                ?.let { offer(exerciseId, PrKind.MAX_WEIGHT, it, workout) }
            if (weightSets.isNotEmpty()) {
                offer(exerciseId, PrKind.SESSION_VOLUME, weightSets.sumOf { it.kg * it.reps }, workout)
            }
            sets.filterIsInstance<SetLog.Reps>().maxOfOrNull { it.reps }
                ?.let { offer(exerciseId, PrKind.MAX_REPS, it.toDouble(), workout) }
            sets.filterIsInstance<SetLog.Time>().maxOfOrNull { it.seconds }
                ?.let { offer(exerciseId, PrKind.MAX_TIME, it.toDouble(), workout) }
            sets.filterIsInstance<SetLog.DistanceTime>().maxOfOrNull { it.meters }
                ?.let { offer(exerciseId, PrKind.MAX_DISTANCE, it, workout) }
        }
    }
    return records.values.sortedWith(compareBy({ it.exerciseId }, { it.kind }))
}

/**
 * Rekordy ustanowione w najnowszym treningu (po startedAt) — pod badge
 * "nowy rekord" i celebrację. Wyrównanie starego rekordu nie wraca tutaj.
 */
fun newRecordsInLatestWorkout(workouts: List<Workout>): List<PersonalRecord> {
    val latest = workouts.maxByOrNull { it.startedAt } ?: return emptyList()
    return computePersonalRecords(workouts).filter { it.workoutId == latest.id }
}

/**
 * Punkty wykresu danej metryki dla jednego ćwiczenia — po jednym na trening,
 * chronologicznie rosnąco. Trening bez pasujących serii roboczych jest pomijany.
 */
fun chartPoints(workouts: List<Workout>, exerciseId: String, metric: ChartMetric): List<ChartPoint> =
    workouts.sortedBy { it.startedAt }.mapNotNull { workout ->
        val sets = workingSets(workout, exerciseId)
        val value = when (metric) {
            ChartMetric.WEIGHT ->
                sets.filterIsInstance<SetLog.WeightReps>().maxOfOrNull { it.kg }

            ChartMetric.VOLUME ->
                sets.filterIsInstance<SetLog.WeightReps>()
                    .takeIf { it.isNotEmpty() }
                    ?.sumOf { it.kg * it.reps }

            ChartMetric.REPS ->
                sets.filterIsInstance<SetLog.Reps>().maxOfOrNull { it.reps }?.toDouble()

            ChartMetric.TIME ->
                sets.filterIsInstance<SetLog.Time>().maxOfOrNull { it.seconds }?.toDouble()

            ChartMetric.DISTANCE ->
                sets.filterIsInstance<SetLog.DistanceTime>().maxOfOrNull { it.meters }
        }
        value?.let { ChartPoint(workout.startedAt, it) }
    }
