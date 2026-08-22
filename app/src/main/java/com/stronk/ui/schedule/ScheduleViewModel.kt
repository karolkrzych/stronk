package com.stronk.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stronk.StronkApplication
import com.stronk.data.CardioEntry
import com.stronk.data.CardioRepository
import com.stronk.data.Exercise
import com.stronk.data.ExerciseRepository
import com.stronk.data.Plan
import com.stronk.data.PlanRepository
import com.stronk.data.ScheduleEntry
import com.stronk.data.ScheduleRepository
import com.stronk.data.ScheduleStatus
import com.stronk.progression.ProgressionConstants
import com.stronk.progression.ProgressionEngine
import com.stronk.ui.cardio.CardioRowUi
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Wiersz ćwiczenia w karcie wybranego dnia (mock: `.exrow` = ikona + nazwa + chip).
 * Świadomie NIE ma tu skrótu celu typu „3×8" — liczba serii jedzie jako chip
 * [setsLabel], a ciężar i powtórzenia pokazuje dopiero ekran treningu w statach.
 */
data class ScheduleExerciseRow(
    val exerciseId: String,
    val name: String,
    /** Surowy klucz partii z datasetu (np. "lats") pod dobór ikony; null gdy nieznane. */
    val muscleKey: String?,
    /** Chip liczby serii, np. „3 serie". */
    val setsLabel: String,
)

/** Wpis harmonogramu przygotowany pod kartę wybranego dnia. */
data class ScheduleEntryUi(
    val entryId: String,
    val planId: String,
    val dayIndex: Int,
    /** Tytuł karty, np. „Środa · Full body B". */
    val title: String,
    /** null, gdy plan nie istnieje (np. usunięty) — UI pokazuje to wprost. */
    val planName: String?,
    /** null, gdy plan nie istnieje albo dayIndex poza zakresem. */
    val dayName: String?,
    val status: ScheduleStatus,
    /** Przy status=MOVED: etykieta docelowej daty, np. „piątek 22 sierpnia". */
    val movedToLabel: String?,
    val exercises: List<ScheduleExerciseRow>,
) {
    /** Start treningu tylko z zaplanowanego wpisu wskazującego istniejący dzień planu. */
    val canStart: Boolean
        get() = status == ScheduleStatus.PLANNED && dayName != null

    /** Plan zniknął spod wpisu — karta mówi to wprost zamiast udawać trening. */
    val planMissing: Boolean
        get() = dayName == null
}

/**
 * Stan kwadratu dnia w siatce bloku. Trzy stany widoczne w legendzie (maks 2
 * pozycje + „dziś" jako ring), [MISSED] rysuje się jak [PLANNED] — zaplanowany
 * dzień w przeszłości to nadal „plan, którego nie ma w faktach".
 */
enum class ScheduleDayStatus { DONE, PLANNED, MISSED, FREE }

/** Kwadrat dnia w siatce (mock: `.day`). */
data class ScheduleDayUi(
    val date: LocalDate,
    val dayOfMonth: Int,
    val isToday: Boolean,
    val isSelected: Boolean,
    val status: ScheduleDayStatus,
    /** Czy tego dnia jest wpis cardio — znacznik nakłada [CalendarMarkers]. */
    val hasCardio: Boolean = false,
)

/** Jeden rząd siatki = tydzień bloku (7 kwadratów, poniedziałek–niedziela). */
data class ScheduleWeekUi(
    /** 1-based pozycja tygodnia w bloku. */
    val weekNumber: Int,
    val isCurrentWeek: Boolean,
    val days: List<ScheduleDayUi>,
)

/** Plan możliwy do przypisania do tygodnia (niearchiwalny, z co najmniej 1 dniem). */
data class PlanOption(
    val id: String,
    val name: String,
    /** Nazwy dni planu w kolejności indeksów. */
    val dayNames: List<String>,
    /** `blockLengthWeeks == null` — plan bez końca, dostaje rolling generation. */
    val continuous: Boolean,
)

/** Stan ekranu Tydzień. */
data class ScheduleUiState(
    val loading: Boolean = true,
    /**
     * Nagłówek: „Tydzień 1/6" w planie z blokiem, samo „Tydzień 7" w planie bez
     * bloku (nie ma mianownika, plan biegnie bez końca); pusty bez planu.
     */
    val blockLabel: String = "",
    /** Podtytuł: miesiąc(-e) objęte siatką, np. „Sierpień – wrzesień". */
    val monthLabel: String = "",
    /** Rzędy siatki kwadratów — tygodnie bieżącego bloku. */
    val weeks: List<ScheduleWeekUi> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    /** „Dziś" albo np. „Piątek 22 sierpnia" — dla dnia bez treningu. */
    val selectedDayLabel: String = "",
    val todaySelected: Boolean = true,
    val selectedEntries: List<ScheduleEntryUi> = emptyList(),
    /** Cardio wybranego dnia — także w dniach przeszłych i przyszłych. */
    val selectedCardio: List<CardioRowUi> = emptyList(),
    val planOptions: List<PlanOption> = emptyList(),
    /** true = zero wpisów w całym harmonogramie → pusty stan z zachętą. */
    val scheduleEmpty: Boolean = true,
    /** Zajęte dni (PLANNED/DONE) ze wszystkich planów — walidacja kolizji w [AssignPlanDialog]. */
    val occupiedEntries: List<OccupiedEntry> = emptyList(),
    /**
     * Aktualny wzorzec wpisów PLANNED per plan — prefill dni w [AssignPlanDialog]
     * ([deriveWeekAssignments] na tym daje wzorzec, jaki user by dostał, gdyby
     * nic nie zmienił).
     */
    val plannedSlotsByPlan: Map<String, List<PlannedSlot>> = emptyMap(),
)

class ScheduleViewModel(
    private val scheduleRepository: ScheduleRepository,
    planRepository: PlanRepository,
    exerciseRepository: ExerciseRepository,
    cardioRepository: CardioRepository,
) : ViewModel() {

    /** Dataset z assets — null do końca pierwszego ładowania. */
    private val exercisesById = MutableStateFlow<Map<String, Exercise>?>(null)

    /** Jedyne źródło pozycji w kalendarzu: który dzień pokazuje karta pod siatką. */
    private val selectedDate = MutableStateFlow(LocalDate.now())

    /** Ostatni znany harmonogram — pod akcje (przesuń/odwołaj/generacja). */
    private var latestEntries: List<ScheduleEntry> = emptyList()

    /** Ostatnio znane plany — pod rolling generation (mirror wzorca [latestEntries]). */
    private var latestPlans: List<Plan> = emptyList()

    /** Komunikat po nieudanej próbie przypisania planu (Snackbar w [ScheduleScreen]). */
    private val _assignmentMessage = MutableStateFlow<String?>(null)
    val assignmentMessage: StateFlow<String?> = _assignmentMessage

    /**
     * Idempotencja rolling generation: planId → [ScheduleEntry.date] najpóźniejszego
     * wpisu, od którego już próbowaliśmy dogenerować. Chroni przed wielokrotnym
     * odpaleniem w tym samym cyklu (Flow potrafi odpalić się ponownie, zanim własny
     * zapis dotrze do lokalnego cache'u Firestore) — bez blokowania KOLEJNEJ,
     * realnej potrzeby przedłużenia, gdy `lastPlannedDate` ruszy dalej.
     */
    private val rollingExtensionCursor = mutableMapOf<String, LocalDate>()

    /**
     * Guard przeciwko race'owi z rolling generation: [onAssignPlan] przy
     * przeplanowaniu kasuje stare wpisy PLANNED i zapisuje nowe w JEDNEJ
     * paczce ([ScheduleRepository.replacePlannedEntries], atomowej też w
     * lokalnym cache'u). Ale między ZLECENIEM paczki a jej odbiciem w
     * [latestEntries] (kolejna emisja `uiState`) [maybeExtendContinuousPlans]
     * mogłaby się odpalić na wciąż STARYM stanie i dogenerować kolejne wpisy
     * wg STAREGO wzorca — nasz `delete` (policzony wcześniej) o nich nie wie,
     * więc zostałyby jako śmieci poza oknem nowego wzorca.
     *
     * planId → id-ki starych wpisów, które właśnie kasujemy. Dopóki
     * którykolwiek z nich wciąż widnieje w harmonogramie, rolling dla tego
     * planu jest wstrzymana; guard czyści się sam (patrz góra
     * [maybeExtendContinuousPlans]), gdy żaden z tych id już nie występuje —
     * paczka jest atomowa, więc „stare zniknęły" ⇔ „nowe już są".
     */
    private val pendingReplan = mutableMapOf<String, Set<String>>()

    val uiState: StateFlow<ScheduleUiState> = combine(
        scheduleRepository.observeSchedule(),
        planRepository.observePlans(),
        exercisesById,
        selectedDate,
        cardioRepository.observeCardio(),
    ) { schedule, plans, exercises, selected, cardio ->
        latestEntries = schedule
        latestPlans = plans
        if (exercises == null) ScheduleUiState(loading = true)
        else buildState(schedule, plans, exercises, selected, cardio)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScheduleUiState())

    init {
        viewModelScope.launch {
            exercisesById.value = exerciseRepository.getAll().associateBy { it.id }
        }
        // Rolling generation planów bez bloku — osobny mechanizm poza reducerem
        // (buildState zostaje czystą funkcją). Obserwuje uiState, więc odpala się
        // przy każdej zmianie harmonogramu/planów; guard wyżej robi resztę.
        viewModelScope.launch {
            uiState.collect { maybeExtendContinuousPlans(latestEntries, latestPlans) }
        }
    }

    // ---------- nawigacja po kalendarzu ----------

    fun onSelectDay(date: LocalDate) {
        selectedDate.value = date
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
     * Przypisanie planu do tygodnia = PRZEPLANOWANIE: wpisy PLANNED wybranego
     * planu od [startDate] w przód (WSZYSTKIE, nie tylko okno generacji —
     * rolling mógł nagenerować dalej) są kasowane i zastępowane nowymi
     * slotami wg [assignments] na [ScheduleConstants.GENERATION_WEEKS]
     * tygodni (plan bez bloku dociągnie resztę sam przez rolling generation,
     * które od teraz czyta NOWY wzorzec — [deriveWeekAssignments] bierze go z
     * realnych wpisów, nie z tego wywołania).
     *
     * Nietykalne: wpisy DONE (historia treningu, dowolnego planu) i PLANNED
     * INNEGO planu — patrz [planReplacement]. Kasowanie + zapis idą w JEDNEJ
     * paczce ([ScheduleRepository.replacePlannedEntries]); [pendingReplan]
     * dodatkowo chroni przed rolling generation odpalającym się na starym
     * stanie między zleceniem paczki a jej odbiciem w [latestEntries].
     *
     * Jeśli po odfiltrowaniu nietykalnych dat nie powstał ŻADEN nowy slot —
     * nic się nie zapisuje (stare wpisy też zostają nietknięte), Snackbar
     * tłumaczy czemu ([ScheduleTexts.NOTHING_TO_PLAN]).
     *
     * Reentrancja: dopóki dla [planId] wisi niedokończony replan
     * ([pendingReplan] niepusty — poprzednia paczka jeszcze nie odbiła się w
     * [latestEntries]), kolejne wywołanie jest ignorowane. Bez tego dwa
     * szybkie zatwierdzenia policzyłyby `idsToDelete`/zajętość ze STARYCH
     * danych → duplikaty PLANNED na tych samych datach. Dialog i tak zamyka
     * się od razu po `onConfirm` ([ScheduleScreen]), więc early return nie
     * zmienia zachowania UI.
     */
    fun onAssignPlan(planId: String, assignments: Map<DayOfWeek, Int>, startDate: LocalDate) {
        if (assignments.isEmpty()) return
        if (planId in pendingReplan) return
        val currentEntries = latestEntries.mapNotNull { entry ->
            parseDate(entry.date)?.let { date ->
                ScheduleEntryRef(
                    id = entry.id,
                    date = date,
                    planId = entry.planId,
                    kind = when (entry.status) {
                        ScheduleStatus.PLANNED -> ScheduleEntryKind.PLANNED
                        ScheduleStatus.DONE -> ScheduleEntryKind.DONE
                        else -> ScheduleEntryKind.OTHER
                    },
                )
            }
        }
        val replan = planReplacement(currentEntries, planId, assignments, startDate)
        if (replan.slots.isEmpty()) {
            _assignmentMessage.value = ScheduleTexts.NOTHING_TO_PLAN
            return
        }
        val newEntries = replan.slots.map { slot ->
            ScheduleEntry(
                id = scheduleRepository.newId(),
                date = slot.date.toString(),
                planId = planId,
                dayIndex = slot.dayIndex,
            )
        }
        if (replan.idsToDelete.isNotEmpty()) {
            pendingReplan[planId] = replan.idsToDelete.toSet()
        }
        scheduleRepository.replacePlannedEntries(deleteIds = replan.idsToDelete, newEntries = newEntries)
        // Ten sam clamp co w planReplacement — startDate mogła przyjść z UI
        // sprzed dziś tylko przez defensywną ścieżkę (picker to blokuje).
        selectedDate.value = clampStartDateToToday(startDate)
    }

    /** Snackbar w [ScheduleScreen] pokazał komunikat — czyścimy, żeby się nie powtarzał. */
    fun onAssignmentMessageShown() {
        _assignmentMessage.value = null
    }

    // ---------- rolling generation (plan bez bloku) ----------

    /**
     * Dla każdego planu BEZ bloku ([Plan.blockLengthWeeks] `== null`) sprawdza,
     * czy najpóźniejszy zaplanowany wpis jest bliżej niż
     * [ScheduleConstants.ROLLING_THRESHOLD_WEEKS] tygodni od dziś — jeśli tak,
     * dogenerowuje kolejne [ScheduleConstants.GENERATION_WEEKS] tygodni z
     * przypisaniami wyprowadzonymi z istniejących wpisów ([deriveWeekAssignments]).
     * Plany z blokiem — bez zmian. Czysta logika (próg, derywacja, filtr zajętości)
     * żyje w [WeekPlanner]; ta funkcja tylko orkiestruje odczyt/zapis.
     */
    private fun maybeExtendContinuousPlans(schedule: List<ScheduleEntry>, plans: List<Plan>) {
        // Guard [pendingReplan] czyści się sam: paczka replanu jest atomowa,
        // więc gdy żaden ze skasowanych id już nie występuje w schedule —
        // nowe wpisy na pewno już tam są (patrz KDoc pola).
        val scheduleIds = schedule.mapTo(HashSet()) { it.id }
        pendingReplan.keys.toList().forEach { planId ->
            if (pendingReplan.getValue(planId).none { it in scheduleIds }) {
                pendingReplan.remove(planId)
            }
        }

        val today = LocalDate.now()
        val plansById = plans.associateBy { it.id }
        val plannedByPlan = plannedSlotsByPlan(schedule)

        plannedByPlan.forEach { (planId, slots) ->
            val plan = plansById[planId] ?: return@forEach
            if (plan.archived || plan.blockLengthWeeks != null || planId in pendingReplan) return@forEach

            val lastPlannedDate = slots.maxOf { it.date }
            // Guard: ten sam stan (planId, ostatnia data) już przetworzony w tym cyklu.
            if (rollingExtensionCursor[planId] == lastPlannedDate) return@forEach
            if (!needsRollingExtension(lastPlannedDate, today)) return@forEach

            val assignments = deriveWeekAssignments(slots)
            if (assignments.isEmpty()) return@forEach
            rollingExtensionCursor[planId] = lastPlannedDate

            val occupied = schedule
                .filter { it.status == ScheduleStatus.PLANNED || it.status == ScheduleStatus.DONE }
                .mapNotNull { entry -> parseDate(entry.date) }
                .toSet()
            val newSlots = generatePlannedSlots(
                assignments = assignments,
                startDate = lastPlannedDate.plusDays(1),
                weeks = ScheduleConstants.GENERATION_WEEKS,
                occupiedDates = occupied,
            )
            newSlots.forEach { slot ->
                scheduleRepository.save(
                    ScheduleEntry(
                        id = scheduleRepository.newId(),
                        date = slot.date.toString(),
                        planId = planId,
                        dayIndex = slot.dayIndex,
                    ),
                )
            }
        }
    }

    // ---------- budowa stanu ----------

    private fun buildState(
        schedule: List<ScheduleEntry>,
        plans: List<Plan>,
        exercises: Map<String, Exercise>,
        selected: LocalDate,
        cardio: List<CardioEntry>,
    ): ScheduleUiState {
        val today = LocalDate.now()
        val entriesByDate = schedule.groupBy { it.date }
        val cardioByDate = cardio.groupBy { it.date }
        val plansById = plans.associateBy { it.id }
        val plan = activePlan(schedule, plansById, today)

        // Pozycja w bloku liczona WYŁĄCZNIE przez silnik progresji (ADR-004).
        // null = plan bez bloku: tygodnie lecą liniowo, siatka jedzie oknem.
        // Bez planu w ogóle zostaje domyślny blok — siatka ma znajomy kształt.
        val blockWeeks = if (plan == null) {
            ProgressionConstants.BLOCK_LENGTH_WEEKS_DEFAULT
        } else {
            ProgressionEngine.fullBlockWeeks(plan.blockLengthWeeks)
        }
        val weekIndex = plan?.let {
            ProgressionEngine.weekIndexForBlock(
                it.createdAt,
                System.currentTimeMillis(),
                blockWeeks,
            )
        } ?: 0
        val window = gridWindow(weekIndex, blockWeeks)
        val mondays = blockWeekMondays(today, weekIndex, window)

        val weeks = mondays.mapIndexed { row, monday ->
            val weekIndexOfRow = window.startWeek + row
            ScheduleWeekUi(
                // Bez bloku numeracja jest liniowa — nie ma czego zawijać modulo.
                weekNumber = if (blockWeeks == null) {
                    weekIndexOfRow + 1
                } else {
                    weekIndexOfRow % blockWeeks + 1
                },
                isCurrentWeek = weekIndexOfRow == weekIndex,
                days = (0 until ScheduleConstants.DAYS_IN_WEEK).map { offset ->
                    val date = monday.plusDays(offset.toLong())
                    ScheduleDayUi(
                        date = date,
                        dayOfMonth = date.dayOfMonth,
                        isToday = date == today,
                        isSelected = date == selected,
                        status = dayStatus(entriesByDate[date.toString()].orEmpty(), date, today),
                        hasCardio = cardioByDate.containsKey(date.toString()),
                    )
                },
            )
        }

        val gridFrom = mondays.firstOrNull() ?: weekStartOf(today)
        val gridTo = mondays.lastOrNull()?.plusDays((ScheduleConstants.DAYS_IN_WEEK - 1).toLong())
            ?: gridFrom

        return ScheduleUiState(
            loading = false,
            blockLabel = if (plan == null) {
                ""
            } else {
                ScheduleTexts.weekHeaderLabel(weekIndex + 1, blockWeeks)
            },
            monthLabel = ScheduleTexts.monthRangeLabel(gridFrom, gridTo, today),
            weeks = weeks,
            selectedDate = selected,
            selectedDayLabel = ScheduleTexts.selectedDayLabel(selected, today),
            todaySelected = selected == today,
            selectedEntries = entriesByDate[selected.toString()].orEmpty()
                .sortedBy { it.dayIndex }
                .map { entryUi(it, selected, plansById, exercises) },
            selectedCardio = cardioByDate[selected.toString()].orEmpty().map { entry ->
                CardioRowUi(
                    id = entry.id,
                    type = entry.type,
                    durationMin = entry.durationMin,
                    distanceKm = entry.distanceKm,
                )
            },
            planOptions = plans
                .filter { !it.archived && it.days.isNotEmpty() }
                .sortedByDescending { it.createdAt }
                .map { candidate ->
                    PlanOption(
                        id = candidate.id,
                        name = candidate.name,
                        dayNames = candidate.days.map { it.name },
                        continuous = candidate.blockLengthWeeks == null,
                    )
                },
            scheduleEmpty = schedule.isEmpty(),
            occupiedEntries = schedule
                .filter { it.status == ScheduleStatus.PLANNED || it.status == ScheduleStatus.DONE }
                .mapNotNull { entry ->
                    parseDate(entry.date)?.let { date ->
                        OccupiedEntry(
                            date = date,
                            planId = entry.planId,
                            planName = plansById[entry.planId]?.name ?: "usunięty plan",
                        )
                    }
                },
            plannedSlotsByPlan = plannedSlotsByPlan(schedule),
        )
    }

    /**
     * Plan, którego blok pokazuje siatka: plan z wpisu harmonogramu najbliższego
     * dzisiejszemu dniowi (to on jest „tym, w czym teraz jesteś"), a bez
     * harmonogramu — najnowszy niearchiwalny plan. Sortowanie po stronie klienta,
     * zero zapytań z `orderBy` (dokument bez pola wypadałby z takiego zapytania).
     */
    private fun activePlan(
        schedule: List<ScheduleEntry>,
        plansById: Map<String, Plan>,
        today: LocalDate,
    ): Plan? {
        val nearest = schedule
            .filter { it.status == ScheduleStatus.PLANNED || it.status == ScheduleStatus.DONE }
            .mapNotNull { entry -> parseDate(entry.date)?.let { date -> date to entry } }
            .sortedWith(
                compareBy<Pair<LocalDate, ScheduleEntry>> {
                    abs(it.first.toEpochDay() - today.toEpochDay())
                }.thenBy { it.first },
            )
            .firstOrNull()
            ?.second
            ?.let { plansById[it.planId] }
        return nearest
            ?: plansById.values
                .filter { !it.archived && it.days.isNotEmpty() }
                .maxByOrNull { it.createdAt }
    }

    /**
     * Stan kwadratu z istniejących danych: zaliczony trening wygrywa nad planem,
     * a plan w przeszłości bez zaliczenia to [ScheduleDayStatus.MISSED].
     * Odwołany (SKIPPED) i przesunięty (MOVED) zostawiają dzień pusty — fakt
     * treningu jest wtedy pod inną datą.
     */
    private fun dayStatus(
        entries: List<ScheduleEntry>,
        date: LocalDate,
        today: LocalDate,
    ): ScheduleDayStatus = when {
        entries.any { it.status == ScheduleStatus.DONE } -> ScheduleDayStatus.DONE
        entries.none { it.status == ScheduleStatus.PLANNED } -> ScheduleDayStatus.FREE
        date < today -> ScheduleDayStatus.MISSED
        else -> ScheduleDayStatus.PLANNED
    }

    private fun entryUi(
        entry: ScheduleEntry,
        date: LocalDate,
        plansById: Map<String, Plan>,
        exercises: Map<String, Exercise>,
    ): ScheduleEntryUi {
        val plan = plansById[entry.planId]
        val day = plan?.days?.getOrNull(entry.dayIndex)
        return ScheduleEntryUi(
            entryId = entry.id,
            planId = entry.planId,
            dayIndex = entry.dayIndex,
            title = ScheduleTexts.dayCardTitle(date, day?.name),
            planName = plan?.name,
            dayName = day?.name,
            status = entry.status,
            movedToLabel = entry.movedTo?.let { raw ->
                parseDate(raw)?.let(ScheduleTexts::movedToLabel) ?: raw
            },
            exercises = day?.exercises.orEmpty().map { planExercise ->
                val exercise = exercises[planExercise.exerciseId]
                ScheduleExerciseRow(
                    exerciseId = planExercise.exerciseId,
                    name = exercise?.namePl ?: planExercise.exerciseId,
                    muscleKey = exercise?.primaryMuscles?.firstOrNull(),
                    setsLabel = ScheduleTexts.setsLabel(planExercise.sets),
                )
            },
        )
    }

    private fun parseDate(raw: String): LocalDate? = runCatching { LocalDate.parse(raw) }.getOrNull()

    /**
     * Wpisy PLANNED pogrupowane po planId jako [PlannedSlot] — wspólne dla
     * rolling generation ([maybeExtendContinuousPlans]) i prefillu dialogu
     * przypisania ([ScheduleUiState.plannedSlotsByPlan]).
     */
    private fun plannedSlotsByPlan(schedule: List<ScheduleEntry>): Map<String, List<PlannedSlot>> =
        schedule
            .filter { it.status == ScheduleStatus.PLANNED }
            .mapNotNull { entry -> parseDate(entry.date)?.let { date -> entry.planId to PlannedSlot(date, entry.dayIndex) } }
            .groupBy({ it.first }, { it.second })

    companion object {
        /** Ręczna kompozycja: zależności z [StronkApplication]. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as StronkApplication
                ScheduleViewModel(
                    scheduleRepository = app.scheduleRepository,
                    planRepository = app.planRepository,
                    exerciseRepository = app.exerciseRepository,
                    cardioRepository = app.cardioRepository,
                )
            }
        }
    }
}
