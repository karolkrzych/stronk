package com.stronk.ui.schedule

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
import com.stronk.ui.PlLabels
import java.time.DayOfWeek
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

/** Wiersz ćwiczenia w karcie wybranego dnia. */
data class ScheduleExerciseRow(
    val exerciseId: String,
    val name: String,
    /** Polska etykieta głównej partii; pusta gdy ćwiczenie nieznane. */
    val muscleLabel: String,
    /** Surowy klucz partii z datasetu (np. "lats") pod dobór ikony; null gdy nieznane. */
    val muscleKey: String?,
    /** Skrót celu, np. "3×8", "3×60 s", "3 km". */
    val targetLabel: String,
)

/** Wpis harmonogramu przygotowany pod kartę wybranego dnia. */
data class ScheduleEntryUi(
    val entryId: String,
    val planId: String,
    val dayIndex: Int,
    /** null, gdy plan nie istnieje (np. usunięty) — UI pokazuje to wprost. */
    val planName: String?,
    /** null, gdy plan nie istnieje albo dayIndex poza zakresem. */
    val dayName: String?,
    val status: ScheduleStatus,
    /** Przy status=MOVED: etykieta docelowej daty, np. "piątek 22 sierpnia". */
    val movedToLabel: String?,
    val exercises: List<ScheduleExerciseRow>,
) {
    /** Start treningu tylko z zaplanowanego wpisu wskazującego istniejący dzień planu. */
    val canStart: Boolean
        get() = status == ScheduleStatus.PLANNED && dayName != null
}

/** Stan wizualny komórki dnia w siatce tygodnia. */
enum class DayBadge { NONE, PLANNED, DONE, SKIPPED, MOVED }

/** Komórka dnia w siatce tygodnia. */
data class ScheduleDayUi(
    val date: LocalDate,
    /** "Pn", "Wt", … */
    val abbrev: String,
    val dayOfMonth: Int,
    val isToday: Boolean,
    val isSelected: Boolean,
    val badge: DayBadge,
    /** Nazwa dnia planu (np. "Push"); pusta = dzień wolny. */
    val label: String,
)

/** Plan możliwy do przypisania do tygodnia (niearchiwalny, z co najmniej 1 dniem). */
data class PlanOption(
    val id: String,
    val name: String,
    /** Nazwy dni planu w kolejności indeksów. */
    val dayNames: List<String>,
)

/** Stan ekranu harmonogramu. */
data class ScheduleUiState(
    val loading: Boolean = true,
    /** Np. "10–16 sierpnia". */
    val weekLabel: String = "",
    val isCurrentWeek: Boolean = true,
    /** Zawsze 7 komórek (poniedziałek–niedziela). */
    val days: List<ScheduleDayUi> = emptyList(),
    /** "Dziś" albo np. "Piątek 22 sierpnia". */
    val selectedDayLabel: String = "",
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedEntries: List<ScheduleEntryUi> = emptyList(),
    val planOptions: List<PlanOption> = emptyList(),
    /** true = zero wpisów w całym harmonogramie → pusty stan z zachętą. */
    val scheduleEmpty: Boolean = true,
)

class ScheduleViewModel(
    private val scheduleRepository: ScheduleRepository,
    planRepository: PlanRepository,
    exerciseRepository: ExerciseRepository,
) : ViewModel() {

    /** Dataset z assets — null do końca pierwszego ładowania. */
    private val exercisesById = MutableStateFlow<Map<String, Exercise>?>(null)

    /** Jedno źródło pozycji w kalendarzu: tydzień widoku = tydzień wybranego dnia. */
    private val selectedDate = MutableStateFlow(LocalDate.now())

    /** Ostatni znany harmonogram — pod akcje (przesuń/odwołaj/generacja). */
    private var latestEntries: List<ScheduleEntry> = emptyList()

    val uiState: StateFlow<ScheduleUiState> = combine(
        scheduleRepository.observeSchedule(),
        planRepository.observePlans(),
        exercisesById,
        selectedDate,
    ) { schedule, plans, exercises, selected ->
        latestEntries = schedule
        if (exercises == null) ScheduleUiState(loading = true)
        else buildState(schedule, plans, exercises, selected)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScheduleUiState())

    init {
        viewModelScope.launch {
            exercisesById.value = exerciseRepository.getAll().associateBy { it.id }
        }
    }

    // ---------- nawigacja po kalendarzu ----------

    fun onSelectDay(date: LocalDate) {
        selectedDate.value = date
    }

    fun onPreviousWeek() {
        selectedDate.value = selectedDate.value.minusWeeks(1)
    }

    fun onNextWeek() {
        selectedDate.value = selectedDate.value.plusWeeks(1)
    }

    fun onBackToToday() {
        selectedDate.value = LocalDate.now()
    }

    // ---------- akcje na wpisach ----------

    /**
     * Przesunięcie: oryginał dostaje status MOVED + movedTo, na docelowy dzień
     * powstaje nowy wpis PLANNED (model danych, sekcja schedule).
     */
    fun onMoveEntry(entryId: String, newDate: LocalDate) {
        val entry = latestEntries.firstOrNull { it.id == entryId } ?: return
        val newDateKey = newDate.toString()
        if (entry.status != ScheduleStatus.PLANNED || entry.date == newDateKey) return
        scheduleRepository.save(entry.copy(status = ScheduleStatus.MOVED, movedTo = newDateKey))
        scheduleRepository.save(
            ScheduleEntry(
                id = scheduleRepository.newId(),
                date = newDateKey,
                planId = entry.planId,
                dayIndex = entry.dayIndex,
            ),
        )
        // Pokaż od razu dzień, na który trening się przeniósł.
        selectedDate.value = newDate
    }

    fun onCancelEntry(entryId: String) {
        val entry = latestEntries.firstOrNull { it.id == entryId } ?: return
        if (entry.status != ScheduleStatus.PLANNED) return
        scheduleRepository.save(entry.copy(status = ScheduleStatus.SKIPPED))
    }

    /** Cofnięcie odwołania — przypadkowy tap nie może przepadać bez wyjścia. */
    fun onRestoreEntry(entryId: String) {
        val entry = latestEntries.firstOrNull { it.id == entryId } ?: return
        if (entry.status != ScheduleStatus.SKIPPED) return
        scheduleRepository.save(entry.copy(status = ScheduleStatus.PLANNED))
    }

    /**
     * Przypisanie planu do tygodnia: generuje wpisy PLANNED na
     * [ScheduleConstants.GENERATION_WEEKS] tygodni od [startDate].
     * Dni z już aktywnym wpisem (PLANNED/DONE) są pomijane.
     */
    fun onAssignPlan(planId: String, assignments: Map<DayOfWeek, Int>, startDate: LocalDate) {
        if (assignments.isEmpty()) return
        val occupied = latestEntries
            .filter { it.status == ScheduleStatus.PLANNED || it.status == ScheduleStatus.DONE }
            .mapNotNull { entry -> runCatching { LocalDate.parse(entry.date) }.getOrNull() }
            .toSet()
        generatePlannedSlots(assignments, startDate, occupiedDates = occupied).forEach { slot ->
            scheduleRepository.save(
                ScheduleEntry(
                    id = scheduleRepository.newId(),
                    date = slot.date.toString(),
                    planId = planId,
                    dayIndex = slot.dayIndex,
                ),
            )
        }
        selectedDate.value = startDate
    }

    // ---------- budowa stanu ----------

    private fun buildState(
        schedule: List<ScheduleEntry>,
        plans: List<Plan>,
        exercises: Map<String, Exercise>,
        selected: LocalDate,
    ): ScheduleUiState {
        val today = LocalDate.now()
        val weekStart = weekStartOf(selected)
        val entriesByDate = schedule.groupBy { it.date }
        val plansById = plans.associateBy { it.id }

        val days = (0 until ScheduleConstants.DAYS_IN_WEEK).map { offset ->
            val date = weekStart.plusDays(offset.toLong())
            val dayEntries = entriesByDate[date.toString()].orEmpty()
            val badgeEntry = pickBadgeEntry(dayEntries)
            ScheduleDayUi(
                date = date,
                abbrev = ScheduleConstants.DAY_ABBREVIATIONS.getValue(date.dayOfWeek),
                dayOfMonth = date.dayOfMonth,
                isToday = date == today,
                isSelected = date == selected,
                badge = badgeEntry?.let { badgeOf(it.status) } ?: DayBadge.NONE,
                label = badgeEntry?.let { entry ->
                    plansById[entry.planId]?.days?.getOrNull(entry.dayIndex)?.name ?: "?"
                }.orEmpty(),
            )
        }

        return ScheduleUiState(
            loading = false,
            weekLabel = weekLabel(weekStart, today),
            isCurrentWeek = weekStart == weekStartOf(today),
            days = days,
            selectedDayLabel = selectedDayLabel(selected, today),
            selectedDate = selected,
            selectedEntries = entriesByDate[selected.toString()].orEmpty()
                .map { entryUi(it, plansById, exercises) },
            planOptions = plans
                .filter { !it.archived && it.days.isNotEmpty() }
                .map { plan -> PlanOption(plan.id, plan.name, plan.days.map { it.name }) },
            scheduleEmpty = schedule.isEmpty(),
        )
    }

    /** Najważniejszy wpis dnia pod badge siatki: PLANNED > DONE > MOVED > SKIPPED. */
    private fun pickBadgeEntry(entries: List<ScheduleEntry>): ScheduleEntry? =
        entries.minByOrNull { badgePriority.indexOf(it.status) }

    private fun badgeOf(status: ScheduleStatus): DayBadge = when (status) {
        ScheduleStatus.PLANNED -> DayBadge.PLANNED
        ScheduleStatus.DONE -> DayBadge.DONE
        ScheduleStatus.SKIPPED -> DayBadge.SKIPPED
        ScheduleStatus.MOVED -> DayBadge.MOVED
    }

    private fun selectedDayLabel(selected: LocalDate, today: LocalDate): String =
        if (selected == today) {
            "Dziś"
        } else {
            fullDayFormatter.format(selected)
                .replaceFirstChar { it.titlecase(polishLocale) }
        }

    private fun entryUi(
        entry: ScheduleEntry,
        plansById: Map<String, Plan>,
        exercises: Map<String, Exercise>,
    ): ScheduleEntryUi {
        val plan = plansById[entry.planId]
        val day = plan?.days?.getOrNull(entry.dayIndex)
        return ScheduleEntryUi(
            entryId = entry.id,
            planId = entry.planId,
            dayIndex = entry.dayIndex,
            planName = plan?.name,
            dayName = day?.name,
            status = entry.status,
            movedToLabel = entry.movedTo?.let { raw ->
                runCatching { fullDayFormatter.format(LocalDate.parse(raw)) }.getOrDefault(raw)
            },
            exercises = day?.exercises.orEmpty().map { planExercise ->
                val exercise = exercises[planExercise.exerciseId]
                val muscleKey = exercise?.primaryMuscles?.firstOrNull()
                ScheduleExerciseRow(
                    exerciseId = planExercise.exerciseId,
                    name = exercise?.namePl ?: planExercise.exerciseId,
                    muscleLabel = muscleKey?.let { PlLabels.muscle(it) }.orEmpty(),
                    muscleKey = muscleKey,
                    targetLabel = targetLabel(planExercise),
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
        private val polishLocale = Locale.forLanguageTag("pl")
        private val fullDayFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", polishLocale)
        private val badgePriority = listOf(
            ScheduleStatus.PLANNED, ScheduleStatus.DONE, ScheduleStatus.MOVED, ScheduleStatus.SKIPPED,
        )

        /** Ręczna kompozycja: zależności z [StronkApplication]. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as StronkApplication
                ScheduleViewModel(
                    scheduleRepository = app.scheduleRepository,
                    planRepository = app.planRepository,
                    exerciseRepository = app.exerciseRepository,
                )
            }
        }
    }
}
