package com.stronk.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stronk.StronkApplication
import com.stronk.data.Exercise
import com.stronk.data.ExerciseRepository
import com.stronk.data.SetLog
import com.stronk.data.Workout
import com.stronk.data.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Jedna sesja ćwiczenia na liście pod wykresem. */
data class ExerciseSessionUi(
    val workoutId: String,
    /** Np. "wtorek, 18 sierpnia". */
    val dateLabel: String,
    /** Serie robocze w jednej linii, np. "80 kg × 5 · 80 kg × 5 · 82,5 kg × 3". */
    val setsLabel: String,
    /** true, gdy w tej sesji padł którykolwiek rekord ćwiczenia. */
    val hasPr: Boolean,
)

/** Stan ekranu wykresu progresu jednego ćwiczenia. */
data class ExerciseProgressUiState(
    val loading: Boolean = true,
    val exerciseName: String = "",
    /** false, gdy ćwiczenie nie ma jeszcze żadnej serii roboczej w historii. */
    val hasHistory: Boolean = false,
    /** Rekordy z datami, np. "Najcięższa seria: 80 kg · wtorek, 18 sierpnia". */
    val prLabels: List<String> = emptyList(),
    /** Metryki dostępne dla tego ćwiczenia (przełącznik, gdy więcej niż jedna). */
    val availableMetrics: List<ChartMetric> = emptyList(),
    val selectedMetric: ChartMetric? = null,
    /** Punkty wybranej metryki, chronologicznie rosnąco. */
    val points: List<ChartPoint> = emptyList(),
    /** Sesje od najnowszej. */
    val sessions: List<ExerciseSessionUi> = emptyList(),
)

class ExerciseProgressViewModel(
    workoutRepository: WorkoutRepository,
    exerciseRepository: ExerciseRepository,
    private val exerciseId: String,
) : ViewModel() {

    /**
     * Wrapper zamiast gołego Exercise?, bo null w StateFlow znaczy
     * "dataset jeszcze się ładuje" — a ćwiczenia może w bazie nie być.
     */
    private data class ExerciseLookup(val exercise: Exercise?)

    private val lookup = MutableStateFlow<ExerciseLookup?>(null)
    private val chosenMetric = MutableStateFlow<ChartMetric?>(null)

    val uiState: StateFlow<ExerciseProgressUiState> = combine(
        workoutRepository.observeWorkouts(),
        lookup,
        chosenMetric,
    ) { workouts, exerciseLookup, metric ->
        if (exerciseLookup == null) ExerciseProgressUiState(loading = true)
        else buildState(workouts, exerciseLookup.exercise, metric)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseProgressUiState())

    init {
        viewModelScope.launch {
            lookup.value = ExerciseLookup(exerciseRepository.getById(exerciseId))
        }
    }

    fun onMetricSelect(metric: ChartMetric) {
        chosenMetric.value = metric
    }

    private fun buildState(
        allWorkouts: List<Workout>,
        exercise: Exercise?,
        chosen: ChartMetric?,
    ): ExerciseProgressUiState {
        val workouts = allWorkouts.filter { workingSets(it, exerciseId).isNotEmpty() }
        val available = availableMetrics(workouts)
        val metric = chosen?.takeIf { it in available } ?: available.firstOrNull()
        val records = computePersonalRecords(workouts).filter { it.exerciseId == exerciseId }
        val prWorkoutIds = records.map { it.workoutId }.toSet()

        return ExerciseProgressUiState(
            loading = false,
            exerciseName = exercise?.namePl ?: exerciseId,
            hasHistory = workouts.isNotEmpty(),
            prLabels = records
                .sortedBy { PR_KIND_PRIORITY.indexOf(it.kind) }
                .map { record ->
                    val kindLabel = ProgressFormat.prKindLabel(record.kind)
                        .replaceFirstChar { it.uppercase() }
                    "$kindLabel: ${ProgressFormat.prValue(record.kind, record.value)} · " +
                        ProgressFormat.date(record.achievedAt)
                },
            availableMetrics = available,
            selectedMetric = metric,
            points = metric?.let { chartPoints(workouts, exerciseId, it) } ?: emptyList(),
            sessions = workouts.sortedByDescending { it.startedAt }.map { workout ->
                ExerciseSessionUi(
                    workoutId = workout.id,
                    dateLabel = ProgressFormat.date(workout.startedAt),
                    setsLabel = workingSets(workout, exerciseId)
                        .sortedBy { it.setNumber }
                        .joinToString(" · ") { ProgressFormat.setLabel(it) },
                    hasPr = workout.id in prWorkoutIds,
                )
            },
        )
    }

    /** Metryki wynikające z realnie zalogowanych typów serii (historia = prawda). */
    private fun availableMetrics(workouts: List<Workout>): List<ChartMetric> {
        val sets = workouts.flatMap { workingSets(it, exerciseId) }
        return buildList {
            if (sets.any { it is SetLog.WeightReps }) {
                add(ChartMetric.WEIGHT)
                add(ChartMetric.VOLUME)
            }
            if (sets.any { it is SetLog.Reps }) add(ChartMetric.REPS)
            if (sets.any { it is SetLog.Time }) add(ChartMetric.TIME)
            if (sets.any { it is SetLog.DistanceTime }) add(ChartMetric.DISTANCE)
        }
    }

    companion object {
        /** Fabryka z parametrem id — ręczna kompozycja z [StronkApplication]. */
        fun factory(exerciseId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as StronkApplication
                ExerciseProgressViewModel(
                    workoutRepository = app.workoutRepository,
                    exerciseRepository = app.exerciseRepository,
                    exerciseId = exerciseId,
                )
            }
        }
    }
}
