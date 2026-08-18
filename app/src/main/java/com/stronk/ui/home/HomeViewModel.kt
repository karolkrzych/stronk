package com.stronk.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stronk.StronkApplication
import com.stronk.data.Exercise
import com.stronk.data.ExerciseRepository
import com.stronk.data.Plan
import com.stronk.data.PlanExercise
import com.stronk.data.PlanRepository
import com.stronk.data.ScheduleEntry
import com.stronk.data.ScheduleRepository
import com.stronk.data.ScheduleStatus
import com.stronk.data.SetTarget
import com.stronk.data.UserProfile
import com.stronk.data.UserProfileRepository
import com.stronk.ui.PlLabels
import com.stronk.ui.workout.WorkoutSession
import com.stronk.ui.workout.WorkoutSessionManager
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Wiersz ćwiczenia w karcie treningu na Home. */
data class HomeExerciseRow(
    val exerciseId: String,
    val name: String,
    /** Polska etykieta głównej partii (np. "plecy"); pusta gdy ćwiczenie nieznane. */
    val muscleLabel: String,
    /** Skrót celu, np. "3×8", "3×60 s", "3 km". */
    val targetLabel: String,
    /** Pełne URI miniaturki (assets); pusta ścieżka gdy ćwiczenie nieznane/bez obrazka. */
    val imageUri: String,
)

/** Zaplanowany trening przygotowany pod kartę na Home. */
data class ScheduledWorkoutUi(
    val scheduleEntryId: String,
    val planId: String,
    val dayIndex: Int,
    /** "dziś" albo np. "piątek 22 sierpnia". */
    val dateLabel: String,
    val planName: String,
    /** Nazwa dnia planu, np. "Pull". */
    val dayName: String,
    val exercises: List<HomeExerciseRow>,
)

/**
 * Trwająca sesja treningowa (singleton [WorkoutSessionManager] przeżywa
 * ubicie aktywności przy żywym foreground service) — baner "wróć do treningu".
 */
data class ActiveWorkoutUi(
    val planId: String,
    val dayIndex: Int,
    val scheduleEntryId: String?,
    val dayName: String,
    val completedSets: Int,
    val totalSets: Int,
)

/** Główna zawartość ekranu Home — dokładnie jeden wariant naraz. */
sealed interface HomeContent {
    /** Na dziś jest zaplanowany trening. */
    data class TodayWorkout(val workout: ScheduledWorkoutUi) : HomeContent

    /** Dziś nic nie ma — pokazujemy najbliższy zaplanowany. */
    data class UpcomingWorkout(val workout: ScheduledWorkoutUi) : HomeContent

    /** Są plany, ale harmonogram jest pusty. */
    data object NoSchedule : HomeContent

    /** Zero planów — zachęta do stworzenia pierwszego. */
    data object NoPlans : HomeContent
}

/** Stan ekranu Home. */
data class HomeUiState(
    val loading: Boolean = true,
    val displayName: String? = null,
    /** Dzisiejsza data po polsku, np. "wtorek, 18 sierpnia". */
    val todayLabel: String = "",
    /** true, gdy dzisiejszy trening jest już oznaczony jako zrobiony. */
    val todayDone: Boolean = false,
    /** Niepusty = baner "trening w toku" z nawigacją z powrotem do sesji. */
    val activeWorkout: ActiveWorkoutUi? = null,
    val content: HomeContent = HomeContent.NoPlans,
)

class HomeViewModel(
    scheduleRepository: ScheduleRepository,
    planRepository: PlanRepository,
    userProfileRepository: UserProfileRepository,
    exerciseRepository: ExerciseRepository,
) : ViewModel() {

    /** Dataset z assets — null do końca pierwszego ładowania. */
    private val exercisesById = MutableStateFlow<Map<String, Exercise>?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        scheduleRepository.observeSchedule(),
        planRepository.observePlans(),
        userProfileRepository.observeProfile(),
        exercisesById,
        WorkoutSessionManager.session,
    ) { schedule, plans, profile, exercises, session ->
        if (exercises == null) HomeUiState(loading = true)
        else buildState(schedule, plans, profile, exercises, session)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        viewModelScope.launch {
            exercisesById.value = exerciseRepository.getAll().associateBy { it.id }
        }
    }

    private fun buildState(
        schedule: List<ScheduleEntry>,
        plans: List<Plan>,
        profile: UserProfile?,
        exercises: Map<String, Exercise>,
        session: WorkoutSession?,
    ): HomeUiState {
        val today = LocalDate.now()
        val todayKey = today.toString()
        val planned = schedule.filter { it.status == ScheduleStatus.PLANNED }
        // observeSchedule sortuje chronologicznie, więc first = najwcześniejszy.
        val todayEntry = planned.firstOrNull { it.date == todayKey }
        val upcomingEntry = planned.firstOrNull { it.date > todayKey }

        val todayUi = todayEntry?.let { workoutUi(it, plans, exercises, todayKey) }
        val upcomingUi = upcomingEntry?.let { workoutUi(it, plans, exercises, todayKey) }
        val content = when {
            todayUi != null -> HomeContent.TodayWorkout(todayUi)
            upcomingUi != null -> HomeContent.UpcomingWorkout(upcomingUi)
            plans.any { !it.archived } -> HomeContent.NoSchedule
            else -> HomeContent.NoPlans
        }
        return HomeUiState(
            loading = false,
            displayName = profile?.displayName,
            todayLabel = headerFormatter.format(today),
            todayDone = schedule.any { it.date == todayKey && it.status == ScheduleStatus.DONE },
            activeWorkout = session?.let {
                ActiveWorkoutUi(
                    planId = it.planId,
                    dayIndex = it.dayIndex,
                    scheduleEntryId = it.scheduleEntryId,
                    dayName = it.dayName,
                    completedSets = it.completedSetCount,
                    totalSets = it.totalSetCount,
                )
            },
            content = content,
        )
    }

    /** null, gdy wpis wskazuje nieistniejący plan/dzień (wtedy go nie pokazujemy). */
    private fun workoutUi(
        entry: ScheduleEntry,
        plans: List<Plan>,
        exercises: Map<String, Exercise>,
        todayKey: String,
    ): ScheduledWorkoutUi? {
        val plan = plans.firstOrNull { it.id == entry.planId } ?: return null
        val day = plan.days.getOrNull(entry.dayIndex) ?: return null
        return ScheduledWorkoutUi(
            scheduleEntryId = entry.id,
            planId = plan.id,
            dayIndex = entry.dayIndex,
            dateLabel = if (entry.date == todayKey) {
                "dziś"
            } else {
                runCatching { dayFormatter.format(LocalDate.parse(entry.date)) }
                    .getOrDefault(entry.date)
            },
            planName = plan.name,
            dayName = day.name,
            exercises = day.exercises.map { planExercise ->
                val exercise = exercises[planExercise.exerciseId]
                HomeExerciseRow(
                    exerciseId = planExercise.exerciseId,
                    name = exercise?.namePl ?: planExercise.exerciseId,
                    muscleLabel = exercise?.primaryMuscles?.firstOrNull()
                        ?.let { PlLabels.muscle(it) }.orEmpty(),
                    targetLabel = targetLabel(planExercise),
                    imageUri = ExerciseRepository.IMAGES_BASE_URI +
                        exercise?.images?.firstOrNull().orEmpty(),
                )
            },
        )
    }

    private fun targetLabel(exercise: PlanExercise): String = when (val target = exercise.target) {
        is SetTarget.WeightReps -> "${exercise.sets}×${target.reps}"
        is SetTarget.Reps -> "${exercise.sets}×${target.reps}"
        is SetTarget.Time -> "${exercise.sets}×${target.seconds} s"
        is SetTarget.DistanceTime -> metersLabel(target.meters)
    }

    private fun metersLabel(meters: Double): String {
        val rounded = meters.roundToInt()
        return if (rounded >= 1000 && rounded % 1000 == 0) "${rounded / 1000} km" else "$rounded m"
    }

    companion object {
        private val polish = Locale.forLanguageTag("pl")
        private val headerFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", polish)
        private val dayFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", polish)

        /** Ręczna kompozycja: zależności z [StronkApplication]. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as StronkApplication
                HomeViewModel(
                    scheduleRepository = app.scheduleRepository,
                    planRepository = app.planRepository,
                    userProfileRepository = app.userProfileRepository,
                    exerciseRepository = app.exerciseRepository,
                )
            }
        }
    }
}
