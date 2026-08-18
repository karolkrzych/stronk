package com.stronk.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stronk.StronkApplication
import com.stronk.data.Exercise
import com.stronk.data.ExerciseRepository
import com.stronk.data.Plan
import com.stronk.data.PlanRepository
import com.stronk.data.Workout
import com.stronk.data.WorkoutRepository
import com.stronk.ui.PlLabels
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Jedna seria w rozwinięciu treningu. */
data class SetRowUi(
    val label: String,
    val isWarmup: Boolean,
)

/** Ćwiczenie w rozwinięciu treningu (serie posortowane: rozgrzewka najpierw). */
data class WorkoutExerciseUi(
    val exerciseId: String,
    val name: String,
    val sets: List<SetRowUi>,
)

/** Karta jednego treningu w dzienniku. */
data class WorkoutHistoryUi(
    val workoutId: String,
    /** Np. "wtorek, 18 sierpnia". */
    val dateLabel: String,
    /** Np. "Nogi · Full Body 3d" albo "Trening" gdy bez planu. */
    val title: String,
    /** Np. "5 ćwiczeń · 15 serii · 3 250 kg". */
    val summaryLabel: String,
    /** Rekordy ustanowione w tym treningu (niepuste tylko dla najnowszego). */
    val prLabels: List<String>,
    val exercises: List<WorkoutExerciseUi>,
)

/** Wiersz ćwiczenia z historią — wejście do wykresu progresu. */
data class ExerciseHistoryUi(
    val exerciseId: String,
    val name: String,
    /** Np. "czworogłowe uda · 6 treningów". */
    val subtitleLabel: String,
    /** Główny rekord, np. "najcięższa seria: 80 kg"; null gdy brak rekordów. */
    val bestLabel: String?,
    /** true, gdy najnowszy trening ustanowił rekord tego ćwiczenia. */
    val hasNewPr: Boolean,
)

/** Stan ekranu progresu. */
data class ProgressUiState(
    val loading: Boolean = true,
    /** Dziennik treningów, od najnowszego. */
    val history: List<WorkoutHistoryUi> = emptyList(),
    /** Ćwiczenia z historią, od ostatnio trenowanego. */
    val exercises: List<ExerciseHistoryUi> = emptyList(),
    /** Baner celebracji: rekordy z najnowszego treningu. */
    val celebrationLabels: List<String> = emptyList(),
    val expandedWorkoutIds: Set<String> = emptySet(),
)

class ProgressViewModel(
    workoutRepository: WorkoutRepository,
    planRepository: PlanRepository,
    exerciseRepository: ExerciseRepository,
) : ViewModel() {

    /** Dataset z assets — null do końca pierwszego ładowania. */
    private val exercisesById = MutableStateFlow<Map<String, Exercise>?>(null)
    private val expandedIds = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<ProgressUiState> = combine(
        workoutRepository.observeWorkouts(),
        planRepository.observePlans(),
        exercisesById,
        expandedIds,
    ) { workouts, plans, exercises, expanded ->
        if (exercises == null) ProgressUiState(loading = true)
        else buildState(workouts, plans, exercises, expanded)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    init {
        viewModelScope.launch {
            exercisesById.value = exerciseRepository.getAll().associateBy { it.id }
        }
    }

    /** Rozwija/zwija szczegóły treningu w dzienniku. */
    fun toggleExpanded(workoutId: String) {
        expandedIds.value = expandedIds.value.let {
            if (workoutId in it) it - workoutId else it + workoutId
        }
    }

    private fun buildState(
        workouts: List<Workout>,
        plans: List<Plan>,
        exercises: Map<String, Exercise>,
        expanded: Set<String>,
    ): ProgressUiState {
        val records = computePersonalRecords(workouts)
        val latestId = workouts.maxByOrNull { it.startedAt }?.id
        val newRecords =
            if (latestId == null) emptyList() else records.filter { it.workoutId == latestId }

        fun exerciseName(id: String) = exercises[id]?.namePl ?: id

        fun recordLabel(record: PersonalRecord) =
            "${ProgressFormat.prKindLabel(record.kind)}: " +
                ProgressFormat.prValue(record.kind, record.value)

        val celebrationLabels =
            newRecords.map { "${exerciseName(it.exerciseId)} — ${recordLabel(it)}" }

        // observeWorkouts sortuje malejąco, ale nie polegamy na tym.
        val history = workouts.sortedByDescending { it.startedAt }.map { workout ->
            val plan = plans.firstOrNull { it.id == workout.planId }
            val day = workout.dayIndex?.let { plan?.days?.getOrNull(it) }
            val workingCount = workingSets(workout).size
            val volume = workoutVolume(workout)
            WorkoutHistoryUi(
                workoutId = workout.id,
                dateLabel = ProgressFormat.date(workout.startedAt),
                title = when {
                    plan != null && day != null -> "${day.name} · ${plan.name}"
                    plan != null -> plan.name
                    else -> "Trening"
                },
                summaryLabel = buildString {
                    append(ProgressFormat.exercisesCount(workout.exerciseIds.size))
                    append(" · ")
                    append(ProgressFormat.setsCount(workingCount))
                    if (volume > 0) {
                        append(" · ")
                        append(ProgressFormat.volume(volume))
                    }
                },
                prLabels = if (workout.id == latestId) celebrationLabels else emptyList(),
                exercises = workout.sets.groupBy { it.exerciseId }.map { (id, sets) ->
                    WorkoutExerciseUi(
                        exerciseId = id,
                        name = exerciseName(id),
                        sets = sets
                            .sortedWith(compareBy({ !it.isWarmup }, { it.setNumber }))
                            .map { SetRowUi(ProgressFormat.setLabel(it), it.isWarmup) },
                    )
                },
            )
        }

        val recordsByExercise = records.groupBy { it.exerciseId }
        val exerciseRows = workouts
            .flatMap { workout -> workingSets(workout).map { it.exerciseId to workout } }
            .groupBy({ it.first }, { it.second })
            .map { (id, workoutsWithSets) ->
                val distinct = workoutsWithSets.distinctBy { it.id }
                ExerciseRow(id, distinct.size, distinct.maxOf { it.startedAt })
            }
            .sortedByDescending { it.lastTrainedAt }
            .map { row ->
                val exerciseRecords = recordsByExercise[row.exerciseId].orEmpty()
                val best = exerciseRecords.minByOrNull { PR_KIND_PRIORITY.indexOf(it.kind) }
                ExerciseHistoryUi(
                    exerciseId = row.exerciseId,
                    name = exerciseName(row.exerciseId),
                    subtitleLabel = listOfNotNull(
                        exercises[row.exerciseId]?.primaryMuscles?.firstOrNull()
                            ?.let { PlLabels.muscle(it) },
                        ProgressFormat.sessionsCount(row.sessionCount),
                    ).joinToString(" · "),
                    bestLabel = best?.let(::recordLabel),
                    hasNewPr = latestId != null &&
                        exerciseRecords.any { it.workoutId == latestId },
                )
            }

        return ProgressUiState(
            loading = false,
            history = history,
            exercises = exerciseRows,
            celebrationLabels = celebrationLabels,
            expandedWorkoutIds = expanded,
        )
    }

    /** Pośredni wiersz agregacji ćwiczeń (przed formatowaniem). */
    private data class ExerciseRow(
        val exerciseId: String,
        val sessionCount: Int,
        val lastTrainedAt: Long,
    )

    companion object {
        /** Ręczna kompozycja: zależności z [StronkApplication]. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as StronkApplication
                ProgressViewModel(
                    workoutRepository = app.workoutRepository,
                    planRepository = app.planRepository,
                    exerciseRepository = app.exerciseRepository,
                )
            }
        }
    }
}
