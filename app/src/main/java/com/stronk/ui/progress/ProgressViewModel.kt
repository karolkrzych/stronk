package com.stronk.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stronk.StronkApplication
import com.stronk.data.Exercise
import com.stronk.data.ExerciseRepository
import com.stronk.data.Workout
import com.stronk.data.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Karta „Ostatni rekord" — nazwa ćwiczenia i para statów (mock `.record`). */
data class LastRecordUi(
    val exerciseId: String,
    val name: String,
    val primary: StatValueUi,
    /** Druga liczba pary; null dla ćwiczeń mierzonych jedną wartością. */
    val secondary: StatValueUi?,
)

/** Wiersz „Moje ćwiczenia": nazwa, mini-trend i wejście w historię. */
data class ExerciseTrendUi(
    val exerciseId: String,
    val name: String,
    /** Klucz partii głównej z datasetu (do doboru ikony) — null, gdy nieznane. */
    val primaryMuscle: String?,
    /** Ostatnie sesje metryki wiodącej, od najstarszej — słupki sparkline. */
    val trend: List<Float>,
)

/**
 * Stan ekranu Progres (mock `pack-progres-baza.html`, ekran 1).
 *
 * Ekran ma DWA byty: kartę ostatniego rekordu (dominanta) i listę ćwiczeń
 * z mini-trendem. Szczegóły — wykres, tabela sesji — siedzą za chevronem,
 * w zakładce „Historia" ćwiczenia.
 */
data class ProgressUiState(
    val loading: Boolean = true,
    /** null, dopóki nie padł żaden rekord (czyli przed pierwszym treningiem). */
    val lastRecord: LastRecordUi? = null,
    /** Ćwiczenia z historią, od ostatnio trenowanego. */
    val exercises: List<ExerciseTrendUi> = emptyList(),
)

class ProgressViewModel(
    workoutRepository: WorkoutRepository,
    exerciseRepository: ExerciseRepository,
) : ViewModel() {

    /** Dataset z assets — null do końca pierwszego ładowania. */
    private val exercisesById = MutableStateFlow<Map<String, Exercise>?>(null)

    val uiState: StateFlow<ProgressUiState> = combine(
        workoutRepository.observeWorkouts(),
        exercisesById,
    ) { workouts, exercises ->
        if (exercises == null) ProgressUiState(loading = true)
        else buildState(workouts, exercises)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    init {
        viewModelScope.launch {
            exercisesById.value = exerciseRepository.getAll().associateBy { it.id }
        }
    }

    private fun buildState(
        workouts: List<Workout>,
        exercises: Map<String, Exercise>,
    ): ProgressUiState {
        fun exerciseName(id: String) = exercises[id]?.namePl ?: id

        val records = computePersonalRecords(workouts)
        val newestAt = records.maxOfOrNull { it.achievedAt }
        // Przy remisie dat wygrywa rekord „najbardziej charakterystyczny"
        // (najcięższa seria przed objętością) — patrz PR_KIND_PRIORITY.
        val latest = records
            .filter { it.achievedAt == newestAt }
            .minByOrNull { PR_KIND_PRIORITY.indexOf(it.kind) }

        val lastRecord = latest?.let { record ->
            exerciseRecord(workouts, record.exerciseId)?.let { stats ->
                LastRecordUi(
                    exerciseId = record.exerciseId,
                    name = exerciseName(record.exerciseId),
                    primary = stats.primary,
                    secondary = stats.secondary,
                )
            }
        }

        val exerciseRows = workouts
            .flatMap { workout -> workingSets(workout).map { it.exerciseId to workout } }
            .groupBy({ it.first }, { it.second })
            .map { (id, workoutsWithSets) ->
                id to workoutsWithSets.maxOf { it.startedAt }
            }
            .sortedByDescending { (_, lastTrainedAt) -> lastTrainedAt }
            .map { (id, _) ->
                ExerciseTrendUi(
                    exerciseId = id,
                    name = exerciseName(id),
                    primaryMuscle = exercises[id]?.primaryMuscles?.firstOrNull(),
                    trend = trendValues(workouts, id),
                )
            }

        return ProgressUiState(
            loading = false,
            lastRecord = lastRecord,
            exercises = exerciseRows,
        )
    }

    companion object {
        /** Ręczna kompozycja: zależności z [StronkApplication]. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as StronkApplication
                ProgressViewModel(
                    workoutRepository = app.workoutRepository,
                    exerciseRepository = app.exerciseRepository,
                )
            }
        }
    }
}
