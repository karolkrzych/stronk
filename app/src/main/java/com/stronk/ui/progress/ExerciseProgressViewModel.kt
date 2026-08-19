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

/**
 * Stan zakładki „Historia" jednego ćwiczenia (mock `pack-historia-profil`,
 * ramka 1): karta rekordu, wykres słupkowy z przyrostem i tabela sesji.
 *
 * Wszystkie liczby liczą czyste funkcje z `ExerciseHistory.kt` ([exerciseRecord],
 * [chartBars], [exerciseSessions]) — ViewModel tylko spina je z repozytorium.
 */
data class ExerciseProgressUiState(
    val loading: Boolean = true,
    val exerciseName: String = "",
    /** false, gdy ćwiczenie nie ma jeszcze żadnej serii roboczej w historii. */
    val hasHistory: Boolean = false,
    /** Rekord jako para statów; null gdy brak serii roboczych. */
    val record: ExerciseRecordUi? = null,
    /** Kapitalik nad wykresem, np. "Ciężar roboczy · 8 sesji". */
    val chartCaption: String = "",
    /** Przyrost między pierwszą a ostatnią pokazaną sesją; null gdy brak zmiany. */
    val delta: StatValueUi? = null,
    /** Słupki od najstarszej do najnowszej sesji. */
    val bars: List<ChartBarUi> = emptyList(),
    /** Sesje od najnowszej — wiersze tabeli. */
    val sessions: List<ExerciseSessionUi> = emptyList(),
    /** Kapitaliki lewej szyny tabeli (np. KG / POWT.) — podane RAZ na sekcję. */
    val railLabels: SessionRailLabels = SessionRailLabels("Kg", "Powt."),
    /** Liczba kolumn SERIA 1..N. */
    val columnCount: Int = 1,
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

    val uiState: StateFlow<ExerciseProgressUiState> = combine(
        workoutRepository.observeWorkouts(),
        lookup,
    ) { workouts, exerciseLookup ->
        if (exerciseLookup == null) ExerciseProgressUiState(loading = true)
        else buildState(workouts, exerciseLookup.exercise)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseProgressUiState())

    init {
        viewModelScope.launch {
            lookup.value = ExerciseLookup(exerciseRepository.getById(exerciseId))
        }
    }

    private fun buildState(allWorkouts: List<Workout>, exercise: Exercise?): ExerciseProgressUiState {
        val workouts = allWorkouts.filter { workingSets(it, exerciseId).isNotEmpty() }
        val metric = historyMetric(workouts, exerciseId)
        val sessions = exerciseSessions(workouts, exerciseId)
        return ExerciseProgressUiState(
            loading = false,
            exerciseName = exercise?.namePl ?: exerciseId,
            hasHistory = workouts.isNotEmpty(),
            record = exerciseRecord(workouts, exerciseId),
            chartCaption = metric?.let {
                "${ProgressFormat.metricLabel(it)} · ${ProgressFormat.sessionsCount(workouts.size)}"
            }.orEmpty(),
            delta = trendDelta(workouts, exerciseId),
            bars = chartBars(workouts, exerciseId),
            sessions = sessions,
            railLabels = metric?.let(::sessionRailLabels) ?: SessionRailLabels("Kg", "Powt."),
            columnCount = sessions.firstOrNull()?.cells?.size ?: 1,
        )
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
