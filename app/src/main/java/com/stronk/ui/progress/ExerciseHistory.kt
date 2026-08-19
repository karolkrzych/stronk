package com.stronk.ui.progress

import com.stronk.data.SetLog
import com.stronk.data.Workout
import kotlin.math.abs

/**
 * Historia JEDNEGO ćwiczenia — wspólny rdzeń dla wszystkich miejsc, które ją
 * pokazują (zakładka „Historia" w szczegółach ćwiczenia, mini-trend na liście
 * „Moje ćwiczenia" w Progresie). Czyste funkcje nad [Workout], zero Androida
 * i zero Compose — liczby liczymy RAZ, a UI je tylko układa.
 *
 * Rekord i punkty wykresu biorą się z [ProgressStats] (computePersonalRecords /
 * chartPoints) — tu jest tylko warstwa „jak to pokazać": osobna liczba, osobna
 * jednostka, osobny kapitalik. Nigdy fraza „40 kg × 10".
 */

/** Jedna liczba z podpisem — surowiec pod stat-blok (KAPITALIK + liczba + jednostka). */
data class StatValueUi(
    /** Kapitalik nad liczbą, np. "Ciężar" (wersaliki robi komponent). */
    val label: String,
    /** Sama liczba, np. "37,5". */
    val value: String,
    /** Jednostka jako sufiks, np. "kg"; null = liczba bez jednostki. */
    val unit: String? = null,
)

/** Rekord ćwiczenia: para statów (np. CIĘŻAR + POWTÓRZENIA) i kiedy padł. */
data class ExerciseRecordUi(
    val exerciseId: String,
    val primary: StatValueUi,
    /** Druga liczba pary; null dla ćwiczeń, gdzie rekord to jedna wartość. */
    val secondary: StatValueUi?,
    val workoutId: String,
    val achievedAt: Long,
)

/** Jedna komórka tabeli sesji — liczba główna i (opcjonalnie) liczba pod nią. */
data class SessionCellUi(
    /** Np. "37,5"; null = w tej sesji nie było takiej serii. */
    val main: String?,
    /** Np. "10" (powtórzenia); null gdy typ pomiaru ma jedną liczbę. */
    val sub: String?,
) {
    companion object {
        val Empty = SessionCellUi(main = null, sub = null)
    }
}

/** Jedna sesja w tabeli historii: data w lewej szynie + kolumny serii. */
data class ExerciseSessionUi(
    val workoutId: String,
    /** Krótka data, np. "16.08". */
    val dateLabel: String,
    /** true, gdy w tej sesji padł rekord ćwiczenia — wiersz świeci limonką. */
    val hasPr: Boolean,
    /** Komórki serii 1..N; lista ma zawsze długość wyliczoną przez [columnCount]. */
    val cells: List<SessionCellUi>,
)

/** Jeden słupek wykresu trendu (bez zależności od Compose). */
data class ChartBarUi(
    val value: Float,
    /** Liczba nad słupkiem — tylko pierwszy i ostatni (tak jest w mocku). */
    val label: String?,
    /** Rekord: słupek dostaje jasną limonkę. */
    val isRecord: Boolean,
)

/** Nagłówki lewej szyny tabeli sesji — kapitaliki podane RAZ na sekcję. */
data class SessionRailLabels(val main: String, val sub: String?)

/**
 * Metryka wiodąca ćwiczenia — wynika z realnie zalogowanych serii, nie
 * z deklaracji w datasecie (historia jest prawdą). null = brak historii.
 */
fun historyMetric(workouts: List<Workout>, exerciseId: String): ChartMetric? {
    val sets = workouts.flatMap { workingSets(it, exerciseId) }
    return when {
        sets.any { it is SetLog.WeightReps } -> ChartMetric.WEIGHT
        sets.any { it is SetLog.Reps } -> ChartMetric.REPS
        sets.any { it is SetLog.Time } -> ChartMetric.TIME
        sets.any { it is SetLog.DistanceTime } -> ChartMetric.DISTANCE
        else -> null
    }
}

/** Rodzaj rekordu odpowiadający metryce wiodącej. */
private fun recordKind(metric: ChartMetric): PrKind = when (metric) {
    ChartMetric.WEIGHT -> PrKind.MAX_WEIGHT
    ChartMetric.VOLUME -> PrKind.SESSION_VOLUME
    ChartMetric.REPS -> PrKind.MAX_REPS
    ChartMetric.TIME -> PrKind.MAX_TIME
    ChartMetric.DISTANCE -> PrKind.MAX_DISTANCE
}

/** Kapitaliki lewej szyny tabeli sesji dla danej metryki. */
fun sessionRailLabels(metric: ChartMetric): SessionRailLabels = when (metric) {
    ChartMetric.WEIGHT -> SessionRailLabels("Kg", "Powt.")
    ChartMetric.VOLUME -> SessionRailLabels("Kg", null)
    ChartMetric.REPS -> SessionRailLabels("Powt.", null)
    ChartMetric.TIME -> SessionRailLabels("Czas", null)
    ChartMetric.DISTANCE -> SessionRailLabels("Metry", "Czas")
}

/**
 * Rekord ćwiczenia jako para liczb.
 *
 * Dla WEIGHT_REPS druga liczba to powtórzenia NAJLEPSZEJ serii przy rekordowym
 * ciężarze — dzięki temu karta rekordu ma dwa staty (CIĘŻAR / POWTÓRZENIA)
 * zamiast jednej frazy.
 */
fun exerciseRecord(workouts: List<Workout>, exerciseId: String): ExerciseRecordUi? {
    val metric = historyMetric(workouts, exerciseId) ?: return null
    val record = computePersonalRecords(workouts)
        .firstOrNull { it.exerciseId == exerciseId && it.kind == recordKind(metric) }
        ?: return null
    val recordWorkout = workouts.firstOrNull { it.id == record.workoutId }
    val sets = recordWorkout?.let { workingSets(it, exerciseId) }.orEmpty()

    val primary: StatValueUi
    val secondary: StatValueUi?
    when (metric) {
        ChartMetric.WEIGHT, ChartMetric.VOLUME -> {
            primary = StatValueUi("Ciężar", ProgressFormat.decimal(record.value), ProgressFormat.weightUnit)
            val reps = sets.filterIsInstance<SetLog.WeightReps>()
                .filter { abs(it.kg - record.value) < 1e-6 }
                .maxOfOrNull { it.reps }
            secondary = reps?.let { StatValueUi("Powtórzenia", it.toString()) }
        }

        ChartMetric.REPS -> {
            primary = StatValueUi("Powtórzenia", record.value.toInt().toString())
            val extra = sets.filterIsInstance<SetLog.Reps>()
                .mapNotNull { it.extraKg }
                .filter { it > 0 }
                .maxOrNull()
            secondary = extra?.let {
                StatValueUi("Dociążenie", ProgressFormat.decimal(it), ProgressFormat.weightUnit)
            }
        }

        ChartMetric.TIME -> {
            val seconds = record.value.toInt()
            primary = StatValueUi(
                "Czas",
                ProgressFormat.timeValue(seconds),
                ProgressFormat.timeUnit(seconds),
            )
            secondary = null
        }

        ChartMetric.DISTANCE -> {
            primary = StatValueUi(
                "Dystans",
                ProgressFormat.distanceValue(record.value),
                ProgressFormat.distanceUnit(record.value),
            )
            val seconds = sets.filterIsInstance<SetLog.DistanceTime>()
                .filter { abs(it.meters - record.value) < 1e-6 }
                .minOfOrNull { it.seconds }
            secondary = seconds?.let {
                StatValueUi("Czas", ProgressFormat.timeValue(it), ProgressFormat.timeUnit(it))
            }
        }
    }
    return ExerciseRecordUi(
        exerciseId = exerciseId,
        primary = primary,
        secondary = secondary,
        workoutId = record.workoutId,
        achievedAt = record.achievedAt,
    )
}

/**
 * Wartości mini-trendu do sparkline w wierszu „Moje ćwiczenia" — ostatnie
 * [limit] sesji metryki wiodącej, od najstarszej do najnowszej.
 */
fun trendValues(workouts: List<Workout>, exerciseId: String, limit: Int = 5): List<Float> {
    val metric = historyMetric(workouts, exerciseId) ?: return emptyList()
    return chartPoints(workouts, exerciseId, metric)
        .takeLast(limit)
        .map { it.value.toFloat() }
}

/**
 * Słupki wykresu historii — ostatnie [limit] sesji, liczby TYLKO nad pierwszym
 * i ostatnim słupkiem, rekord podświetlony (mock `pack-historia-profil`).
 */
fun chartBars(workouts: List<Workout>, exerciseId: String, limit: Int = 8): List<ChartBarUi> {
    val metric = historyMetric(workouts, exerciseId) ?: return emptyList()
    val points = chartPoints(workouts, exerciseId, metric).takeLast(limit)
    if (points.isEmpty()) return emptyList()
    val best = points.maxOf { it.value }
    return points.mapIndexed { index, point ->
        val edge = index == 0 || index == points.lastIndex
        ChartBarUi(
            value = point.value.toFloat(),
            label = if (edge) barLabel(metric, point.value) else null,
            isRecord = abs(point.value - best) < 1e-6,
        )
    }
}

/** Liczba nad słupkiem — bez jednostki, jednostkę niesie kapitalik nagłówka. */
private fun barLabel(metric: ChartMetric, value: Double): String = when (metric) {
    ChartMetric.WEIGHT, ChartMetric.VOLUME -> ProgressFormat.decimal(value)
    ChartMetric.REPS -> value.toInt().toString()
    ChartMetric.TIME -> ProgressFormat.timeValue(value.toInt())
    ChartMetric.DISTANCE -> ProgressFormat.distanceValue(value)
}

/**
 * Przyrost metryki wiodącej między pierwszą a ostatnią z pokazanych sesji
 * (mock: „PRZYROST +7,5 kg"). null, gdy sesja jest jedna albo nic się nie zmieniło.
 */
fun trendDelta(workouts: List<Workout>, exerciseId: String, limit: Int = 8): StatValueUi? {
    val metric = historyMetric(workouts, exerciseId) ?: return null
    val points = chartPoints(workouts, exerciseId, metric).takeLast(limit)
    if (points.size < 2) return null
    val delta = points.last().value - points.first().value
    if (abs(delta) < 1e-6) return null
    val unit = when (metric) {
        ChartMetric.WEIGHT, ChartMetric.VOLUME -> ProgressFormat.weightUnit
        ChartMetric.REPS -> null
        ChartMetric.TIME -> "s"
        ChartMetric.DISTANCE -> "m"
    }
    return StatValueUi("Przyrost", ProgressFormat.signedDecimal(delta), unit)
}

/**
 * Sesje ćwiczenia jako TABELA: data w lewej szynie, serie w kolumnach.
 * Liczby idą do komórek bez jednostek — jednostki stoją raz, w nagłówku
 * ([sessionRailLabels]). Sesje od najnowszej.
 *
 * @param maxColumns twardy limit kolumn (mock ma 3); serie ponad limit nie mieszczą
 *        się na ekranie i są pomijane
 * @param maxSessions ile sesji pokazać (ekran prościuteńki — reszta nie wchodzi)
 */
fun exerciseSessions(
    workouts: List<Workout>,
    exerciseId: String,
    maxColumns: Int = 4,
    maxSessions: Int = 8,
): List<ExerciseSessionUi> {
    val withSets = workouts
        .filter { workingSets(it, exerciseId).isNotEmpty() }
        .sortedByDescending { it.startedAt }
        .take(maxSessions)
    if (withSets.isEmpty()) return emptyList()

    // Rekord liczy się dla METRYKI WIODĄCEJ — inaczej sesja z największą
    // objętością świeciłaby jak rekord ciężaru (a tabela pokazuje ciężary).
    val metric = historyMetric(workouts, exerciseId)
    val prWorkoutIds = computePersonalRecords(workouts)
        .filter { it.exerciseId == exerciseId && (metric == null || it.kind == recordKind(metric)) }
        .map { it.workoutId }
        .toSet()

    val columns = columnCount(withSets, exerciseId, maxColumns)
    return withSets.map { workout ->
        val sets = workingSets(workout, exerciseId).sortedBy { it.setNumber }
        ExerciseSessionUi(
            workoutId = workout.id,
            dateLabel = ProgressFormat.shortDate(workout.startedAt),
            hasPr = workout.id in prWorkoutIds,
            cells = List(columns) { index -> sets.getOrNull(index)?.let(::sessionCell) ?: SessionCellUi.Empty },
        )
    }
}

/** Ile kolumn serii pokazujemy: najdłuższa sesja, przycięta do limitu. */
fun columnCount(workouts: List<Workout>, exerciseId: String, maxColumns: Int = 4): Int =
    workouts
        .maxOfOrNull { workingSets(it, exerciseId).size }
        ?.coerceIn(1, maxColumns)
        ?: 1

/** Jedna zalogowana seria → komórka tabeli (dwie liczby, zero fraz). */
private fun sessionCell(set: SetLog): SessionCellUi = when (set) {
    is SetLog.WeightReps -> SessionCellUi(ProgressFormat.decimal(set.kg), set.reps.toString())
    is SetLog.Reps -> SessionCellUi(
        main = set.reps.toString(),
        sub = set.extraKg?.takeIf { it > 0 }?.let { "+${ProgressFormat.decimal(it)}" },
    )

    is SetLog.Time -> SessionCellUi(ProgressFormat.timeValue(set.seconds), null)
    is SetLog.DistanceTime -> SessionCellUi(
        main = ProgressFormat.distanceValue(set.meters),
        sub = ProgressFormat.timeValue(set.seconds),
    )
}
