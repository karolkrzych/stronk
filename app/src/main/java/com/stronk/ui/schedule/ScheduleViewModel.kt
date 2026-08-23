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
import java.time.YearMonth
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

/**
 * Jeden rząd siatki = tydzień kalendarzowy (7 pozycji, poniedziałek–niedziela).
 *
 * `null` w [days] to dzień SPOZA pokazywanego miesiąca: siatka zawsze rysuje
 * pełne tygodnie (inaczej kolumny dni tygodnia by się rozjechały), ale taka
 * pozycja jest pustym placeholderem — bez liczby, bez stanu, bez tapu. Slot
 * `null` zamiast flagi `inMonth`, żeby nie dało się przypadkiem policzyć stanu
 * (cardio, „dziś", zaznaczenie) dla dnia, którego siatka i tak nie pokazuje.
 */
data class ScheduleWeekUi(val days: List<ScheduleDayUi?>)

/** Plan możliwy do przypisania do tygodnia (niearchiwalny, z co najmniej 1 dniem). */
data class PlanOption(
    val id: String,
    val name: String,
    /** Nazwy dni planu w kolejności indeksów. */
    val dayNames: List<String>,
    /**
     * Pełna długość bloku (praca + tydzień lekki, `ProgressionEngine.fullBlockWeeks`
     * na `Plan.blockLengthWeeks`) — `null` = plan bez końca, dostaje rolling
     * generation zamiast horyzontu bloku.
     */
    val fullBlockWeeks: Int?,
    /** Zapisany wzorzec dnia tygodnia planu (już po ISO→[DayOfWeek]) — baseline dialogu. */
    val weekdayAssignments: Map<DayOfWeek, Int>?,
)

/** Stan ekranu Tydzień. */
data class ScheduleUiState(
    val loading: Boolean = true,
    /**
     * Podtytuł nagłówka: „Tydzień 1/6" w planie z blokiem, samo „Tydzień 7" w
     * planie bez bloku (nie ma mianownika, plan biegnie bez końca); pusty bez
     * planu. Mówi o TERAŹNIEJSZOŚCI (pozycja w bloku dziś), nie o miesiącu
     * pokazywanym w siatce — strzałki ‹ › go nie ruszają.
     */
    val blockLabel: String = "",
    /** Tytuł nagłówka: pokazywany miesiąc z rokiem, np. „Sierpień 2026". */
    val monthTitle: String = "",
    /** Rzędy siatki kwadratów — tygodnie pokrywające pokazywany miesiąc. */
    val weeks: List<ScheduleWeekUi> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    /** „Dziś" albo np. „Piątek 22 sierpnia" — dla dnia bez treningu. */
    val selectedDayLabel: String = "",
    /**
     * Czy pokazać akcję „Dziś": widok odjechał od teraźniejszości — albo
     * zaznaczony jest inny dzień niż dzisiejszy, albo siatka stoi na innym
     * miesiącu niż bieżący (strzałki ‹ ›).
     */
    val showTodayAction: Boolean = false,
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
    private val planRepository: PlanRepository,
    exerciseRepository: ExerciseRepository,
    cardioRepository: CardioRepository,
) : ViewModel() {

    /** Dataset z assets — null do końca pierwszego ładowania. */
    private val exercisesById = MutableStateFlow<Map<String, Exercise>?>(null)

    /** Jedyne źródło pozycji w kalendarzu: który dzień pokazuje karta pod siatką. */
    private val selectedDate = MutableStateFlow(LocalDate.now())

    /**
     * Miesiąc pokazywany w siatce — klasyczny widok miesiąca, przewijany
     * strzałkami ‹ › ([onPreviousMonth]/[onNextMonth]). Czysto rendering:
     * generacja wpisów, rolling i silnik progresji o tym stanie nie wiedzą.
     */
    private val visibleMonth = MutableStateFlow(YearMonth.now())

    /** Pozycja w kalendarzu jako jedna wartość — [combine] przyjmuje maks 5 flow. */
    private data class CalendarPosition(val selectedDate: LocalDate, val visibleMonth: YearMonth)

    private val calendarPosition = combine(selectedDate, visibleMonth) { date, month ->
        CalendarPosition(date, month)
    }

    /** Ostatni znany harmonogram — pod akcje (przesuń/odwołaj/generacja). */
    private var latestEntries: List<ScheduleEntry> = emptyList()

    /** Ostatnio znane plany — pod rolling generation (mirror wzorca [latestEntries]). */
    private var latestPlans: List<Plan> = emptyList()

    /** Komunikat po nieudanej próbie przypisania planu (Snackbar w [ScheduleScreen]). */
    private val _assignmentMessage = MutableStateFlow<String?>(null)
    val assignmentMessage: StateFlow<String?> = _assignmentMessage

    /**
     * Zdarzenie odwołania wpisu — pod Snackbar „Trening odwołany" z akcją
     * „Cofnij" w [ScheduleScreen] (zero-friction: [onCancelEntry] działa od razu,
     * bez dialogu; to jedyna siatka bezpieczeństwa przed przypadkowym tapem).
     * [CancelEvent.token] rośnie przy każdym wywołaniu, więc odwołanie TEGO
     * SAMEGO wpisu drugi raz (po cofnięciu i ponownym odwołaniu) też odpala
     * `LaunchedEffect` — sam `entryId` jako klucz by się powtórzył i StateFlow
     * wygasiłby emisję jako „bez zmiany". [ScheduleScreen] zamyka stary
     * snackbar, zanim pokaże nowy, więc akcja „Cofnij" trafia zawsze we
     * WŁAŚCIWY, ostatnio odwołany wpis.
     */
    data class CancelEvent(val entryId: String, val token: Long)

    private val _cancelEvent = MutableStateFlow<CancelEvent?>(null)
    val cancelEvent: StateFlow<CancelEvent?> = _cancelEvent
    private var cancelEventToken = 0L

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
        calendarPosition,
        cardioRepository.observeCardio(),
    ) { schedule, plans, exercises, position, cardio ->
        latestEntries = schedule
        latestPlans = plans
        if (exercises == null) ScheduleUiState(loading = true)
        else buildState(schedule, plans, exercises, position, cardio)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScheduleUiState())

    /**
     * Idempotencja sweepu martwych wpisów ([cleanupArchivedPlanEntries]): id-ki
     * wpisów, dla których już zlecono kasację — bez tego każda emisja `uiState`
     * (Flow odpala się wielokrotnie, zanim własny zapis dotrze do lokalnego
     * cache'u Firestore) zlecałaby to samo kasowanie od nowa. Wpis znika stąd
     * naturalnie razem z sobą samym z [latestEntries] (batch delete jest
     * atomowy) — nie trzeba osobnego czyszczenia tego seta.
     */
    private val archivedCleanupRequested = mutableSetOf<String>()

    /** Idempotencja sweepu przykrytych wpisów ([cleanupShadowedEntries]) — wzorem [archivedCleanupRequested]. */
    private val shadowedCleanupRequested = mutableSetOf<String>()

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
        // Jednorazowy sweep martwych wpisów PLANNED zarchiwizowanych planów —
        // samonaprawa stanu sprzed istnienia czyszczenia w
        // PlanEditorViewModel.setArchived (patrz KDoc [cleanupArchivedPlanEntries]).
        viewModelScope.launch {
            uiState.collect {
                cleanupArchivedPlanEntries(latestEntries, latestPlans)
                cleanupShadowedEntries(latestEntries, latestPlans)
            }
        }
    }

    // ---------- nawigacja po kalendarzu ----------

    fun onSelectDay(date: LocalDate) {
        selectedDate.value = date
    }

    /** Poprzedni miesiąc w całości (siatka nie kotwiczy się już blokiem treningowym). */
    fun onPreviousMonth() {
        visibleMonth.value = visibleMonth.value.minusMonths(1)
    }

    /** Kolejny miesiąc w całości. */
    fun onNextMonth() {
        visibleMonth.value = visibleMonth.value.plusMonths(1)
    }

    /** Powrót do teraźniejszości: i zaznaczenie, i siatka wracają na dziś. */
    fun onBackToToday() {
        val today = LocalDate.now()
        selectedDate.value = today
        visibleMonth.value = YearMonth.from(today)
    }

    /**
     * Zaznaczony dzień MUSI być widoczny w siatce — akcje, które przestawiają
     * [selectedDate] na dowolną datę (przesunięcie treningu, start planowania),
     * przeciągają za sobą [visibleMonth]. Bez tego karta dnia pokazywałaby dzień
     * spoza pokazywanego miesiąca, którego w siatce nawet nie widać.
     */
    private fun selectDayAndReveal(date: LocalDate) {
        selectedDate.value = date
        visibleMonth.value = YearMonth.from(date)
    }

    // ---------- akcje na wpisach ----------

    /**
     * Przesunięcie treningu na inny dzień. Co się dzieje z okruchami MOVED
     * („Przesunięty → …") decyduje czysta funkcja
     * [WeekPlanner.moveResolution] — tu zostaje samo wykonanie operacji na
     * repozytorium, w JEDNEJ paczce ([ScheduleRepository.replacePlannedEntries]),
     * żeby snapshot listenery nigdy nie zobaczyły stanu pośredniego.
     *
     * Dwa warianty (pełna semantyka w KDoc [WeekPlanner.moveResolution]):
     * - data źródłowa to normalny dzień wzorca → oryginał dostaje status MOVED +
     *   `movedTo`, a na docelowym dniu powstaje nowy wpis PLANNED (jak dotąd);
     * - data źródłowa była tylko PRZYSTANKIEM (trening już tam przyjechał
     *   przesunięciem) → nowy okruch NIE powstaje, wpis po prostu jedzie dalej
     *   pod nową datę, a okruch z początku łańcucha przejmuje nowy cel albo —
     *   gdy trening wrócił do punktu wyjścia — leci do kasacji.
     */
    fun onMoveEntry(entryId: String, newDate: LocalDate) {
        val entry = latestEntries.firstOrNull { it.id == entryId } ?: return
        val newDateKey = newDate.toString()
        if (entry.status != ScheduleStatus.PLANNED || entry.date == newDateKey) return
        val refs = toEntryRefs(latestEntries, latestPlans.associateBy { it.id })
        val movedRef = refs.firstOrNull { it.id == entryId } ?: return
        val resolution = moveResolution(refs, movedRef, newDate)

        val redirectedCrumbs = resolution.crumbsToRedirect.mapNotNull { redirect ->
            latestEntries.firstOrNull { it.id == redirect.id }
                ?.copy(movedTo = redirect.movedTo.toString())
        }
        val sourceWrites = if (resolution.createCrumbAtSource) {
            listOf(
                entry.copy(status = ScheduleStatus.MOVED, movedTo = newDateKey),
                ScheduleEntry(
                    id = scheduleRepository.newId(),
                    date = newDateKey,
                    planId = entry.planId,
                    dayIndex = entry.dayIndex,
                ),
            )
        } else {
            listOf(entry.copy(date = newDateKey))
        }
        scheduleRepository.replacePlannedEntries(
            deleteIds = resolution.crumbIdsToDelete,
            newEntries = redirectedCrumbs + sourceWrites,
        )
        // Pokaż od razu dzień, na który trening się przeniósł — razem z jego
        // miesiącem, bo przesunięcie potrafi przeskoczyć przełom miesiąca.
        selectDayAndReveal(newDate)
    }

    fun onCancelEntry(entryId: String) {
        val entry = latestEntries.firstOrNull { it.id == entryId } ?: return
        if (entry.status != ScheduleStatus.PLANNED) return
        scheduleRepository.save(entry.copy(status = ScheduleStatus.SKIPPED))
        cancelEventToken += 1
        _cancelEvent.value = CancelEvent(entryId, cancelEventToken)
    }

    /** Snackbar w [ScheduleScreen] pokazał/zamknął się — czyścimy, żeby się nie powtarzał. */
    fun onCancelEventShown() {
        _cancelEvent.value = null
    }

    /** Cofnięcie odwołania — przypadkowy tap nie może przepadać bez wyjścia. */
    fun onRestoreEntry(entryId: String) {
        val entry = latestEntries.firstOrNull { it.id == entryId } ?: return
        if (entry.status != ScheduleStatus.SKIPPED) return
        scheduleRepository.save(entry.copy(status = ScheduleStatus.PLANNED))
    }

    /**
     * Zatwierdzenie [AssignPlanDialog] = zapis WZORCA tygodnia w planie +
     * materializacja harmonogramu na jego podstawie. Dialog jest plannerem
     * jednego tygodnia, który przekłada się na CAŁY czas życia planu — zapis
     * [assignments] do `Plan.weekdayAssignments` ([PlanRepository.save]) jest
     * tym, co przetrwa (kolejne otwarcie dialogu i rolling generation czytają
     * STĄD, nie z tego wywołania).
     *
     * Materializacja (przeplanowanie wpisów PLANNED wybranego planu od
     * [startDate] w przód — WSZYSTKIE, nie tylko okno jednego wywołania,
     * rolling mógł nagenerować dalej) ma horyzont zależny od typu planu:
     * - plan Z BLOKIEM: pełna długość bloku, nie krótszy niż horyzont, jaki
     *   plan już miał ([blockReplanWeeks]) — nowe ułożenie dni obowiązuje
     *   przez cały blok, nie tylko okno generacji;
     * - plan BEZ bloku: [ScheduleConstants.GENERATION_WEEKS] jak dotąd — rolling
     *   generation dociągnie resztę samo, czytając już zapisany wzorzec.
     *
     * Nietykalne: wpisy DONE (historia treningu, dowolnego planu) i PLANNED
     * INNEGO planu — patrz [planReplacement]. Kasowanie + zapis idą w JEDNEJ
     * paczce ([ScheduleRepository.replacePlannedEntries]); [pendingReplan]
     * dodatkowo chroni przed rolling generation odpalającym się na starym
     * stanie między zleceniem paczki a jej odbiciem w [latestEntries].
     *
     * Jeśli [assignments] jest NIEPUSTE, a po odfiltrowaniu nietykalnych dat
     * nie powstał ŻADEN nowy slot — cała operacja jest anulowana (nic się nie
     * zapisuje, ani harmonogram, ani wzorzec w planie), Snackbar tłumaczy
     * czemu ([ScheduleTexts.NOTHING_TO_PLAN]). Puste [assignments] to inny
     * przypadek — user świadomie wyzerował wszystkie dni (CTA to dopuszcza,
     * patrz [WeekPlanner.isWeekPlanDirty]) — wtedy wzorzec i tak się zapisuje
     * (jako pusta mapa) i istniejące przyszłe PLANNED tego planu są kasowane
     * bez zastąpienia.
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
        if (planId in pendingReplan) return
        val plan = latestPlans.firstOrNull { it.id == planId } ?: return
        val currentEntries = toEntryRefs(latestEntries, latestPlans.associateBy { it.id })
        val weeks = replanWeeks(plan, startDate, currentEntries)
        val replan = planReplacement(currentEntries, planId, assignments, startDate, weeks)
        if (assignments.isNotEmpty() && replan.slots.isEmpty()) {
            _assignmentMessage.value = ScheduleTexts.NOTHING_TO_PLAN
            return
        }
        planRepository.save(plan.copy(weekdayAssignments = weekdayAssignmentsToIso(assignments)))
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
        // Paczka sama nie robi nic, gdy obie listy są puste (patrz ScheduleRepository).
        scheduleRepository.replacePlannedEntries(deleteIds = replan.idsToDelete, newEntries = newEntries)
        // Ten sam clamp co w planReplacement — startDate mogła przyjść z UI
        // sprzed dziś tylko przez defensywną ścieżkę (picker to blokuje).
        // Data startu bywa w kolejnym miesiącu — siatka jedzie za nią.
        selectDayAndReveal(clampStartDateToToday(startDate))
    }

    /**
     * Horyzont materializacji (tygodnie od [startDate]) na wejście
     * [planReplacement]: plan bez bloku dostaje stałe okno generacji jak
     * dotąd, plan z blokiem — [blockReplanWeeks] liczony z JEGO ISTNIEJĄCYCH
     * wpisów PLANNED (z [currentEntries], nie z całego harmonogramu — inne
     * plany nie mają tu znaczenia).
     */
    private fun replanWeeks(plan: Plan, startDate: LocalDate, currentEntries: List<ScheduleEntryRef>): Int {
        val fullBlockWeeks = ProgressionEngine.fullBlockWeeks(plan.blockLengthWeeks)
            ?: return ScheduleConstants.GENERATION_WEEKS
        val existingPlanDates = currentEntries
            .filter { it.planId == plan.id && it.kind == ScheduleEntryKind.PLANNED }
            .map { it.date }
        return blockReplanWeeks(clampStartDateToToday(startDate), fullBlockWeeks, existingPlanDates)
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
     * dogenerowuje kolejne [ScheduleConstants.GENERATION_WEEKS] tygodni.
     * Wzorzec dni bierze z [Plan.weekdayAssignments] (źródło prawdy — pojedyncze
     * „Przesuń" treningu na inny dzień tygodnia NIE ma prawa zarazić reguły);
     * dla starych planów bez zapisanego wzorca (`null`) spada na
     * [deriveWeekAssignments] z istniejących wpisów jak dotąd. Plany z blokiem —
     * bez zmian. Czysta logika (próg, derywacja, filtr zajętości) żyje w
     * [WeekPlanner]; ta funkcja tylko orkiestruje odczyt/zapis.
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
        val refs = toEntryRefs(schedule, plansById)

        plannedByPlan.forEach { (planId, slots) ->
            val plan = plansById[planId] ?: return@forEach
            if (!isEligibleForRollingExtension(plan.archived, plan.blockLengthWeeks) || planId in pendingReplan) {
                return@forEach
            }

            val lastPlannedDate = slots.maxOf { it.date }
            // Guard: ten sam stan (planId, ostatnia data) już przetworzony w tym cyklu.
            if (rollingExtensionCursor[planId] == lastPlannedDate) return@forEach
            if (!needsRollingExtension(lastPlannedDate, today)) return@forEach

            val assignments = plan.weekdayAssignments
                ?.let { weekdayAssignmentsFromIso(it) }
                // Wzorzec z bazy mógł powstać PRZED usunięciem dni z planu (edytor
                // planu) — odfiltrowujemy martwe indeksy zamiast kopiować je dalej.
                ?.filterValues { it < plan.days.size }
                ?: deriveWeekAssignments(slots, plan.days.size)
            if (assignments.isEmpty()) return@forEach
            rollingExtensionCursor[planId] = lastPlannedDate

            val generationStart = lastPlannedDate.plusDays(1)
            // Daty źródłowe aktywnych przesunięć TEGO planu są zajęte tak samo
            // jak PLANNED/DONE — trening z nich już wyszedł pod inną datę, więc
            // dogenerowanie tam nowego wpisu zrobiłoby dzień naraz „przesunięty"
            // i „zaplanowany" (ta sama reguła co w [planReplacement]).
            val occupied = schedule
                .filter { it.status == ScheduleStatus.PLANNED || it.status == ScheduleStatus.DONE }
                .mapNotNull { entry -> parseDate(entry.date) }
                .toSet() + activeMovedSlots(refs, planId, generationStart).map { it.from }
            val newSlots = generatePlannedSlots(
                assignments = assignments,
                startDate = generationStart,
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

    /**
     * Jednorazowy sweep: kasuje przyszłe wpisy zarchiwizowanych planów —
     * PLANNED, MOVED i SKIPPED, nigdy DONE
     * ([WeekPlanner.archivedPlanDeadEntryIds]) — samonaprawa kont, na których
     * do archiwizacji doszło ZANIM istniało sprzątanie w
     * [com.stronk.ui.plans.PlanEditorViewModel.setArchived] (dokładnie
     * przypadek z tego zgłoszenia: plan zarchiwizowany, jego martwe wpisy
     * zostały w `schedule` — PLANNED blokowały nowy plan, a MOVED/SKIPPED
     * renderowały się jako karty-widma „Przesunięty → …" obok normalnego
     * treningu). Bez migracji danych — odpala się sam przy pierwszym otwarciu
     * ekranu Tydzień.
     *
     * Przeszłe wpisy zostają w bazie (audit-trail), ale karta dnia i tak ich
     * nie pokazuje — [buildState] ukrywa MOVED/SKIPPED zarchiwizowanych planów
     * niezależnie od daty ([WeekPlanner.archivedPlanGhostEntryIds]).
     *
     * Idempotentne przez [archivedCleanupRequested] (patrz KDoc pola) i bez
     * ryzyka race'u z [maybeExtendContinuousPlans]: rolling już z definicji
     * pomija zarchiwizowane plany ([isEligibleForRollingExtension]), więc oba
     * mechanizmy nigdy nie dotykają tych samych wpisów.
     */
    private fun cleanupArchivedPlanEntries(schedule: List<ScheduleEntry>, plans: List<Plan>) {
        if (plans.none { it.archived }) return
        val plansById = plans.associateBy { it.id }
        val idsToDelete = archivedPlanDeadEntryIds(toEntryRefs(schedule, plansById))
            .filterNot { it in archivedCleanupRequested }
        if (idsToDelete.isEmpty()) return
        archivedCleanupRequested += idsToDelete
        scheduleRepository.replacePlannedEntries(deleteIds = idsToDelete, newEntries = emptyList())
    }

    /**
     * Sweep duplikatów na jednej dacie: kasuje wpisy MOVED/SKIPPED przykryte
     * żywym wpisem (PLANNED/DONE) TEGO SAMEGO planu na TEJ SAMEJ dacie
     * ([WeekPlanner.shadowedEntryIds]) — samonaprawa kont, na których taki
     * duplikat już powstał, zanim [planReplacement] zaczął respektować
     * przesunięcia (dokładnie artefakt z tego zgłoszenia: poniedziałek naraz
     * „Przesunięty na czwartek" i z pełną listą ćwiczeń).
     *
     * DONE nietykalne — [shadowedEntryIds] bierze wyłącznie MOVED/SKIPPED, więc
     * historia treningu nie ma prawa tu wpaść. Idempotentne przez
     * [shadowedCleanupRequested] (patrz KDoc [archivedCleanupRequested]).
     * [buildState] i tak UKRYWA te wpisy natychmiast — sweep tylko domyka temat
     * w bazie, więc opóźnienie zapisu nic nie psuje.
     */
    private fun cleanupShadowedEntries(schedule: List<ScheduleEntry>, plans: List<Plan>) {
        val idsToDelete = shadowedEntryIds(toEntryRefs(schedule, plans.associateBy { it.id }))
            .filterNot { it in shadowedCleanupRequested }
        if (idsToDelete.isEmpty()) return
        shadowedCleanupRequested += idsToDelete
        scheduleRepository.replacePlannedEntries(deleteIds = idsToDelete, newEntries = emptyList())
    }

    /**
     * [ScheduleEntry] → [ScheduleEntryRef]: [ScheduleEntryRef.archived] jest
     * `true` dla KAŻDEGO wpisu zarchiwizowanego planu POZA DONE — czyli dla
     * PLANNED, MOVED i SKIPPED. DONE zawsze zostaje `false` (historia treningu
     * blokuje niezależnie od archiwizacji, patrz KDoc
     * [ScheduleEntryRef.archived]).
     *
     * MOVED/SKIPPED muszą tu wpaść, bo inaczej sweep martwych wpisów
     * ([cleanupArchivedPlanEntries]) i filtr renderu
     * ([WeekPlanner.archivedPlanGhostEntryIds]) nigdy ich nie widzą i
     * breadcrumby po zarchiwizowanym planie wiszą w karcie dnia na zawsze.
     * Wspólne dla [onAssignPlan], [buildState] i obu sweepów.
     */
    private fun toEntryRefs(schedule: List<ScheduleEntry>, plansById: Map<String, Plan>): List<ScheduleEntryRef> =
        schedule.mapNotNull { entry ->
            parseDate(entry.date)?.let { date ->
                ScheduleEntryRef(
                    id = entry.id,
                    date = date,
                    planId = entry.planId,
                    kind = when (entry.status) {
                        ScheduleStatus.PLANNED -> ScheduleEntryKind.PLANNED
                        ScheduleStatus.DONE -> ScheduleEntryKind.DONE
                        ScheduleStatus.MOVED -> ScheduleEntryKind.MOVED
                        ScheduleStatus.SKIPPED -> ScheduleEntryKind.SKIPPED
                    },
                    archived = entry.status != ScheduleStatus.DONE && plansById[entry.planId]?.archived == true,
                    movedTo = entry.movedTo?.let { parseDate(it) },
                    dayIndex = entry.dayIndex,
                )
            }
        }

    // ---------- budowa stanu ----------

    private fun buildState(
        schedule: List<ScheduleEntry>,
        plans: List<Plan>,
        exercises: Map<String, Exercise>,
        position: CalendarPosition,
        cardio: List<CardioEntry>,
    ): ScheduleUiState {
        val selected = position.selectedDate
        val month = position.visibleMonth
        val today = LocalDate.now()
        val entriesByDate = schedule.groupBy { it.date }
        val cardioByDate = cardio.groupBy { it.date }
        val plansById = plans.associateBy { it.id }
        val plan = activePlan(schedule, plansById, today)
        val refs = toEntryRefs(schedule, plansById)
        // Wpisy, które nie mają prawa renderować się w karcie dnia — dwie
        // reguły, oba sweepy kasują je z bazy, tu znikają natychmiast z oczu:
        // - [shadowedEntryIds]: MOVED/SKIPPED przykryte żywym wpisem TEGO
        //   SAMEGO planu na TEJ SAMEJ dacie (dzień z realnym treningiem nie
        //   może być naraz „przesunięty"/„odwołany") → [cleanupShadowedEntries];
        // - [archivedPlanGhostEntryIds]: MOVED/SKIPPED planu ZARCHIWIZOWANEGO
        //   (breadcrumb po planie, którego już nie ma — łapie też widma obok
        //   treningu INNEGO planu, czego kluczowanie po (planId, data) wyżej
        //   nie widzi) → [cleanupArchivedPlanEntries].
        // DONE nie wpada do żadnej z nich — historia treningu jest nietykalna.
        val hiddenIds = shadowedEntryIds(refs).toSet() + archivedPlanGhostEntryIds(refs)

        // Pozycja w bloku liczona WYŁĄCZNIE przez silnik progresji (ADR-004) —
        // po przejściu na klasyczny widok miesiąca służy JUŻ TYLKO etykiecie
        // „Tydzień X/Y" w podtytule; siatka nie kotwiczy się blokiem.
        // Bez planu w ogóle zostaje domyślny blok — etykieta i tak nie powstaje.
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

        // Klasyczny widok miesiąca: pełne tygodnie od 1. do ostatniego dnia
        // [month], dni spoza miesiąca jako puste placeholdery (slot `null`).
        val weeks = monthGridMondays(month).map { monday ->
            ScheduleWeekUi(
                days = (0 until ScheduleConstants.DAYS_IN_WEEK).map { offset ->
                    val date = monday.plusDays(offset.toLong())
                    if (YearMonth.from(date) != month) {
                        null
                    } else {
                        ScheduleDayUi(
                            date = date,
                            dayOfMonth = date.dayOfMonth,
                            isToday = date == today,
                            isSelected = date == selected,
                            status = dayStatus(entriesByDate[date.toString()].orEmpty(), date, today),
                            hasCardio = cardioByDate.containsKey(date.toString()),
                        )
                    }
                },
            )
        }

        return ScheduleUiState(
            loading = false,
            blockLabel = if (plan == null) {
                ""
            } else {
                ScheduleTexts.weekHeaderLabel(weekIndex + 1, blockWeeks)
            },
            monthTitle = ScheduleTexts.monthTitle(month),
            weeks = weeks,
            selectedDate = selected,
            selectedDayLabel = ScheduleTexts.selectedDayLabel(selected, today),
            showTodayAction = selected != today || month != YearMonth.from(today),
            selectedEntries = entriesByDate[selected.toString()].orEmpty()
                .filterNot { it.id in hiddenIds }
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
                        fullBlockWeeks = ProgressionEngine.fullBlockWeeks(candidate.blockLengthWeeks),
                        weekdayAssignments = candidate.weekdayAssignments?.let { weekdayAssignmentsFromIso(it) },
                    )
                },
            scheduleEmpty = schedule.isEmpty(),
            // buildOccupiedEntries odfiltrowuje martwe PLANNED zarchiwizowanych
            // planów — takie wpisy nie mają prawa blokować CTA w AssignPlanDialog
            // (conflictingOtherPlanEntry), DONE blokuje zawsze niezależnie od
            // archiwizacji planu-właściciela.
            occupiedEntries = buildOccupiedEntries(refs) { id ->
                plansById[id]?.name ?: "usunięty plan"
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
