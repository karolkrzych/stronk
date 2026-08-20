package com.stronk.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stronk.StronkApplication
import com.stronk.data.CardioEntry
import com.stronk.data.CardioRepository
import com.stronk.data.CardioType
import com.stronk.data.Exercise
import com.stronk.data.ExerciseRepository
import com.stronk.data.Plan
import com.stronk.data.PlanRepository
import com.stronk.data.ScheduleEntry
import com.stronk.data.ScheduleRepository
import com.stronk.progression.ProgressionEngine
import com.stronk.ui.cardio.CardioRowUi
import com.stronk.ui.plans.PlanTexts
import com.stronk.ui.workout.WorkoutSession
import com.stronk.ui.workout.WorkoutSessionManager
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Wiersz ćwiczenia w karcie dnia (mock `pack-dzis-plany.html`, `.exrow`):
 * piktogram + nazwa + JEDEN chip. Bez ciężarów i bez „3×10" — szczegóły
 * ćwiczenia są za tapnięciem, nie na liście.
 */
data class HomeExerciseRow(
    val exerciseId: String,
    val name: String,
    /** Klucz partii z datasetu (np. "chest") — wybiera piktogram kafelka. */
    val muscleKey: String?,
    /** Etykieta chipa, np. "3 serie". */
    val setsChip: String,
)

/** Zaplanowany trening przygotowany pod strefę treningu na ekranie „Dziś". */
data class ScheduledWorkoutUi(
    val scheduleEntryId: String,
    val planId: String,
    val dayIndex: Int,
    /** KAPITALIK nad nazwą dnia, np. "Środa · 19.08". */
    val dateCaption: String,
    /** Chip bloku progresji, np. "Tydzień 1/6"; null gdy plan nie ma bloku. */
    val weekChip: String?,
    /** Nazwa dnia planu, np. "Full body B" — dominanta ekranu. */
    val dayName: String,
    val exercises: List<HomeExerciseRow>,
    /** Suma serii wszystkich ćwiczeń dnia — belka „Trening ukończony". */
    val setCount: Int,
    /** Cały plan pod sheet „Szczegóły planu" (nazwa planu, wszystkie dni). */
    val plan: PlanOverviewUi,
)

/**
 * Jeden dzień planu w sheecie „Szczegóły planu" (wariant S2 „karty z podglądem").
 * Karta dnia pokazuje pierwsze miniatury OD RAZU, więc wiezie pełną listę ćwiczeń.
 */
data class PlanDayUi(
    val dayIndex: Int,
    val name: String,
    val exercises: List<HomeExerciseRow>,
    /** Dzień, który apka pokazuje jako bieżący — krecha limeDeep przy krawędzi. */
    val current: Boolean,
)

/** Plan pod sheet „Szczegóły planu" — nagłówek + wszystkie dni. */
data class PlanOverviewUi(
    val planId: String,
    val name: String,
    val days: List<PlanDayUi>,
)

/**
 * Trwająca sesja treningowa (singleton [WorkoutSessionManager] przeżywa
 * ubicie aktywności przy żywym foreground service) — notka "wróć do treningu".
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

    /**
     * Dzisiejszy trening jest już zrobiony — strefa treningu zostaje na ekranie
     * (data, dzień, plan), tylko zamiast CTA stoi belka „Trening ukończony".
     */
    data class CompletedWorkout(val workout: ScheduledWorkoutUi) : HomeContent

    /** Dziś nic nie ma — pokazujemy najbliższy zaplanowany. */
    data class UpcomingWorkout(val workout: ScheduledWorkoutUi) : HomeContent

    /** Są plany, ale harmonogram jest pusty. */
    data object NoSchedule : HomeContent

    /** Zero planów — zachęta do stworzenia pierwszego. */
    data object NoPlans : HomeContent
}

/** Trening ze strefy górnej albo null (puste stany) — bez rozpisywania `when`. */
val HomeContent.scheduledWorkout: ScheduledWorkoutUi?
    get() = when (this) {
        is HomeContent.TodayWorkout -> workout
        is HomeContent.CompletedWorkout -> workout
        is HomeContent.UpcomingWorkout -> workout
        HomeContent.NoSchedule, HomeContent.NoPlans -> null
    }

/** Stan ekranu Home. */
data class HomeUiState(
    val loading: Boolean = true,
    /** true, gdy dzisiejszy trening jest już oznaczony jako zrobiony. */
    val todayDone: Boolean = false,
    /** Niepusty = notka "trening w toku" z nawigacją z powrotem do sesji. */
    val activeWorkout: ActiveWorkoutUi? = null,
    val content: HomeContent = HomeContent.NoPlans,
    /** Cardio wpisane DZIŚ — sekcja pod ćwiczeniami (poziom 1: ręczny wpis). */
    val cardio: List<CardioRowUi> = emptyList(),
)

class HomeViewModel(
    scheduleRepository: ScheduleRepository,
    planRepository: PlanRepository,
    exerciseRepository: ExerciseRepository,
    private val cardioRepository: CardioRepository,
) : ViewModel() {

    /** Dataset z assets — null do końca pierwszego ładowania. */
    private val exercisesById = MutableStateFlow<Map<String, Exercise>?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        scheduleRepository.observeSchedule(),
        planRepository.observePlans(),
        exercisesById,
        WorkoutSessionManager.session,
        cardioRepository.observeCardio(),
    ) { schedule, plans, exercises, session, cardio ->
        if (exercises == null) HomeUiState(loading = true)
        else buildState(schedule, plans, exercises, session, cardio)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        viewModelScope.launch {
            exercisesById.value = exerciseRepository.getAll().associateBy { it.id }
        }
    }

    // ---------- cardio (poziom 1: ręczny wpis, zero GPS) ----------

    /**
     * Zapis wpisu cardio na DZIŚ. [entryId] niepuste = edycja istniejącego
     * wpisu (id i dzień zostają, zmienia się treść).
     */
    fun onSaveCardio(
        entryId: String?,
        type: CardioType,
        durationMin: Int,
        distanceKm: Double?,
    ) {
        val existing = latestCardio.firstOrNull { it.id == entryId }
        cardioRepository.save(
            CardioEntry(
                id = entryId ?: cardioRepository.newId(),
                date = existing?.date ?: LocalDate.now().toString(),
                type = type,
                durationMin = durationMin,
                distanceKm = distanceKm,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            ),
        )
    }

    fun onDeleteCardio(entryId: String) {
        cardioRepository.delete(entryId)
    }

    /** Ostatni znany zestaw wpisów cardio — pod edycję (zachowanie daty wpisu). */
    private var latestCardio: List<CardioEntry> = emptyList()

    private fun buildState(
        schedule: List<ScheduleEntry>,
        plans: List<Plan>,
        exercises: Map<String, Exercise>,
        session: WorkoutSession?,
        cardio: List<CardioEntry>,
    ): HomeUiState {
        latestCardio = cardio
        val today = LocalDate.now()
        val todayKey = today.toString()
        val entries = HomeMapping.selectEntries(schedule, todayKey)

        val todayUi = entries.today?.let { workoutUi(it, plans, exercises) }
        val doneUi = entries.todayDone?.let { workoutUi(it, plans, exercises) }
        val upcomingUi = entries.upcoming?.let { workoutUi(it, plans, exercises) }
        val content = when {
            todayUi != null -> HomeContent.TodayWorkout(todayUi)
            doneUi != null -> HomeContent.CompletedWorkout(doneUi)
            upcomingUi != null -> HomeContent.UpcomingWorkout(upcomingUi)
            plans.any { !it.archived } -> HomeContent.NoSchedule
            else -> HomeContent.NoPlans
        }
        return HomeUiState(
            loading = false,
            todayDone = entries.todayDone != null,
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
            cardio = cardio.filter { it.date == todayKey }.map { entry ->
                CardioRowUi(
                    id = entry.id,
                    type = entry.type,
                    durationMin = entry.durationMin,
                    distanceKm = entry.distanceKm,
                )
            },
        )
    }

    /** null, gdy wpis wskazuje nieistniejący plan/dzień (wtedy go nie pokazujemy). */
    private fun workoutUi(
        entry: ScheduleEntry,
        plans: List<Plan>,
        exercises: Map<String, Exercise>,
    ): ScheduledWorkoutUi? {
        val plan = plans.firstOrNull { it.id == entry.planId } ?: return null
        val day = plan.days.getOrNull(entry.dayIndex) ?: return null
        val date = runCatching { LocalDate.parse(entry.date) }.getOrNull()
        val overview = HomeMapping.planOverview(plan, entry.dayIndex, exercises)
        return ScheduledWorkoutUi(
            scheduleEntryId = entry.id,
            planId = plan.id,
            dayIndex = entry.dayIndex,
            dateCaption = date?.let(::dateCaption) ?: entry.date,
            weekChip = date?.let { weekChip(plan, it) },
            dayName = day.name,
            // Ten sam dzień, który wiezie już przegląd planu — nie mapujemy go
            // drugi raz, żeby lista w sheecie i lista na ekranie nie mogły się
            // rozjechać.
            exercises = overview.days[entry.dayIndex].exercises,
            setCount = HomeMapping.setCount(day),
            plan = overview,
        )
    }

    companion object {
        private val polish = Locale.forLanguageTag("pl")

        /** "środa" — dzień tygodnia; kapitaliki robi komponent. */
        private val weekdayFormatter = DateTimeFormatter.ofPattern("EEEE", polish)

        /** "19.08" — data bez roku; rok w apce treningowej to szum. */
        private val shortDateFormatter = DateTimeFormatter.ofPattern("dd.MM", polish)

        /** KAPITALIK karty dnia z mocka: "Środa · 19.08". */
        private fun dateCaption(date: LocalDate): String =
            "${weekdayFormatter.format(date)} · ${shortDateFormatter.format(date)}"

        /**
         * Chip bloku progresji z mocka: "Tydzień 1/6". Pozycję tygodnia liczy
         * silnik (ADR-004) — tu tylko ją pokazujemy, niczego nie licząc od nowa.
         */
        private fun weekChip(plan: Plan, date: LocalDate): String? {
            val fullBlock = PlanTexts.fullBlockWeeks(plan)
            if (fullBlock <= 0) return null
            val nowMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val index = ProgressionEngine.weekIndexInBlock(plan.createdAt, nowMillis, fullBlock)
            return "Tydzień ${index + 1}/$fullBlock"
        }

        /** Ręczna kompozycja: zależności z [StronkApplication]. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as StronkApplication
                HomeViewModel(
                    scheduleRepository = app.scheduleRepository,
                    planRepository = app.planRepository,
                    exerciseRepository = app.exerciseRepository,
                    cardioRepository = app.cardioRepository,
                )
            }
        }
    }
}
