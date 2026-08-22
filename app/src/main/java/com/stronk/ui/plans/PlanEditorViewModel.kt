package com.stronk.ui.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stronk.StronkApplication
import com.stronk.data.ComplianceResult
import com.stronk.data.Exercise
import com.stronk.data.ExerciseRepository
import com.stronk.data.GoalDefaults
import com.stronk.data.MeasurementType
import com.stronk.data.Plan
import com.stronk.data.PlanDay
import com.stronk.data.PlanExercise
import com.stronk.data.PlanRepository
import com.stronk.data.ProfileDetails
import com.stronk.data.ScheduleEntry
import com.stronk.data.ScheduleRepository
import com.stronk.data.ScheduleStatus
import com.stronk.data.StressLevel
import com.stronk.data.SubstituteMatch
import com.stronk.data.SubstituteScoring
import com.stronk.data.UserProfile
import com.stronk.data.UserProfileRepository
import com.stronk.data.findSubstitutes
import com.stronk.data.isCompliant
import com.stronk.progression.ProgressionConstants
import com.stronk.progression.ProgressionEngine
import com.stronk.ui.PlLabels
import com.stronk.ui.profile.ProfileDefaults
import com.stronk.ui.schedule.PlannedSlot
import com.stronk.ui.schedule.ScheduleEntryKind
import com.stronk.ui.schedule.ScheduleEntryRef
import com.stronk.ui.schedule.archivedPlanDeadEntryIds
import com.stronk.ui.schedule.clampStartDateToToday
import com.stronk.ui.schedule.planReplacement
import com.stronk.ui.schedule.remapWeekdayAssignments
import com.stronk.ui.schedule.saveReplanWeeks
import com.stronk.ui.schedule.weekPlanBaseline
import com.stronk.ui.schedule.weekdayAssignmentsFromIso
import com.stronk.ui.schedule.weekdayAssignmentsToIso
import java.time.LocalDate
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Ćwiczenie dnia wzbogacone o dane z datasetu i zgodność z profilem. */
data class EditorExerciseUi(
    val planExercise: PlanExercise,
    /** null = id spoza bundlowanej bazy (nie powinno się zdarzyć). */
    val exercise: Exercise?,
    val compliance: ComplianceResult,
) {
    val name: String get() = exercise?.namePl ?: planExercise.exerciseId
}

/** Dzień planu w edytorze. */
data class EditorDayUi(
    val name: String,
    val exercises: List<EditorExerciseUi>,
    /** Duże partie nieobecne w dniu ([missingMajorGroups]) — sekcja "Sugestie". */
    val missingGroups: List<MuscleGroup> = emptyList(),
    /**
     * Czy ten dzień istniał już w zapisanym planie (ma odpowiednik przez
     * `baseDayIndex`) — `false` = dodany w tej sesji edycji, nigdy nie był w
     * harmonogramie. Steruje treścią dialogu potwierdzenia usunięcia
     * ([PlanEditorUiState.planHasSchedule] razem z tym polem decydują, czy
     * pokazać ostrzeżenie o zaplanowanych treningach).
     */
    val existsInSavedPlan: Boolean = false,
)

/** Sugestie ćwiczeń dla brakującej partii w dniu [dayIndex]; tap dodaje wybrane do dnia. */
data class SuggestionsUi(
    val group: MuscleGroup,
    val dayIndex: Int,
    val matches: List<Exercise>,
)

/**
 * Arkusz zamienników. [replaceIndex] = indeks ćwiczenia do PODMIANY w dniu
 * [dayIndex]; null = kontekst pickera (wybrany zamiennik jest DODAWANY do dnia).
 */
data class SubstitutesUi(
    val forExercise: Exercise,
    val matches: List<SubstituteMatch>,
    val dayIndex: Int,
    val replaceIndex: Int?,
    /** Profil w momencie otwarcia arkusza — pod notkę „Twoje stawy" w podglądzie ćwiczenia. */
    val profile: ProfileDetails,
)

/**
 * Krok kreatora nowego planu (mock `pack-dzis-plany.html`, ekran 3).
 * Kolejność jest wymuszona logiką: sprzęt i ograniczenia muszą być znane ZANIM
 * wygenerujemy dni z szablonu, bo to one decydują o doborze ćwiczeń.
 */
enum class PlanWizardStep(val title: String, val subtitle: String) {
    TEMPLATE("Od czego zaczynamy", "Wybierz szablon albo złóż plan od zera."),
    BLOCK("Blok treningowy", "Możesz go wyłączyć — wtedy plan biegnie bez końca."),
    EQUIPMENT("Twój sprzęt", "Zaznacz, na czym trenujesz — resztę ćwiczeń pominiemy."),
    CONSTRAINTS("Twoje ograniczenia", "Zaznacz miejsca, które oszczędzamy."),
    NAME("Nazwa planu", "Tak zobaczysz go na liście i w harmonogramie."),
}

/** Stan kreatora nowego planu; null w [PlanEditorUiState] = jesteśmy w edytorze. */
data class PlanWizardUi(
    val step: PlanWizardStep,
    val stepIndex: Int,
    val stepCount: Int,
    val presets: List<PlanPreset> = emptyList(),
    /** null = „zacznij od zera". */
    val selectedPresetId: String? = null,
    /** Kroki 1 i 5 wymagają wyboru/nazwy; reszta zawsze przepuszcza dalej. */
    val canGoNext: Boolean = false,
    /** Tygodnie PRACY w bloku; null = plan bez bloku (ciągła progresja). */
    val blockLengthWeeks: Int? = null,
    /** Klucze stawów w kolejności prezentacji (jak w profilu). */
    val jointKeys: List<String> = emptyList(),
    val selectedJoints: Set<String> = emptySet(),
    /** Wartości sprzętu z datasetu — te same, co w zakładce Sprzęt profilu. */
    val equipmentOptions: List<String> = emptyList(),
    val selectedEquipment: Set<String> = emptySet(),
    val name: String = "",
    /** Podgląd tego, co powstanie — staty DNI / TYGODNIE / ĆWICZENIA. */
    val summaryDays: Int = 0,
    val summaryExercises: Int = 0,
) {
    val isLastStep: Boolean get() = stepIndex == stepCount - 1
}

/** Stan edytora planu. */
data class PlanEditorUiState(
    val loading: Boolean = true,
    val isNew: Boolean = false,
    /** Niepusty = zamiast edytora rysujemy kreator nowego planu. */
    val wizard: PlanWizardUi? = null,
    val name: String = "",
    /**
     * Tygodnie PRACY w bloku (ADR-004), bez tygodnia lekkiego;
     * null = plan bez bloku — nigdy nie ma tygodnia lekkiego.
     */
    val blockLengthWeeks: Int? = null,
    val days: List<EditorDayUi> = emptyList(),
    /** Cały dataset — dla pickera ćwiczeń. */
    val allExercises: List<Exercise> = emptyList(),
    val profile: ProfileDetails = ProfileDetails(),
    /** Dzień, dla którego otwarty jest picker; null = picker zamknięty. */
    val pickerDayIndex: Int? = null,
    val substitutes: SubstitutesUi? = null,
    val suggestions: SuggestionsUi? = null,
    val canSave: Boolean = false,
    /** Edytowany plan siedzi w archiwum — akcja to „Przywróć", nie „Archiwizuj". */
    val archived: Boolean = false,
    /** Archiwizować da się tylko plan, który już istnieje w bazie. */
    val canArchive: Boolean = false,
    /**
     * Plan ma (miał) harmonogram — `Plan.weekdayAssignments != null` LUB
     * (plany sprzed wprowadzenia tego pola) istnieją wpisy PLANNED tego planu
     * w harmonogramie ([PlanEditorViewModel.hasPlannedScheduleEntries] —
     * inaczej taki plan, realnie zaplanowany, ale nietknięty jeszcze przez
     * dialog „Zaplanuj tydzień", dostawałby słabszy tekst dialogu usunięcia
     * dnia i żaden Snackbar o nowym dniu). Steruje treścią dialogu usunięcia
     * dnia (razem z [EditorDayUi.existsInSavedPlan]) i tym, czy [newDayMessage]
     * w ogóle ma sens po dodaniu dnia (zasada: „żadnych notek gdy plan nie był
     * zaplanowany").
     */
    val planHasSchedule: Boolean = false,
    /** true po zleceniu zapisu — ekran woła onBack (po ew. pokazaniu [newDayMessage]). */
    val saved: Boolean = false,
    /**
     * Komunikat po zapisie planu, w którym przybył nowy dzień, gdy plan MA
     * harmonogram ([planHasSchedule]) — nowy dzień nie wchodzi sam do wzorca,
     * user musi go przypisać w „Zaplanuj tydzień". `null` = nic do pokazania
     * (nowy plan, plan bez harmonogramu, albo żaden dzień nie przybył).
     */
    val newDayMessage: String? = null,
)

class PlanEditorViewModel(
    private val planId: String?,
    private val planRepository: PlanRepository,
    private val userProfileRepository: UserProfileRepository,
    private val scheduleRepository: ScheduleRepository,
    exerciseRepository: ExerciseRepository,
) : ViewModel() {

    /**
     * Dzień draftu ze śladem tożsamości do [Draft.base] — pod zapis
     * ([PlanEditorSave.dayIndexRemap]): [baseDayIndex] to indeks tego dnia w
     * `base.days` PRZED edycją, `null` = dzień dodany w tej sesji ([addDay]),
     * nie istnieje jeszcze w bazie. Przetrwa rename/dodanie/usunięcie
     * ćwiczeń i usunięcie INNYCH dni — edytor nie pozwala dziś przestawiać
     * SAMYCH dni (tylko ćwiczenia wewnątrz dnia, [reorderExercise]), więc
     * kolejność [Draft.days] to jedyne źródło nowego indeksu przy zapisie.
     */
    private data class DraftDay(val baseDayIndex: Int?, val day: PlanDay)

    /** Roboczy stan edycji — zapis do Firestore dopiero przy [save]. */
    private data class Draft(
        val name: String = "",
        /** null = plan bez bloku; nowy plan ręczny startuje właśnie tak. */
        val blockLengthWeeks: Int? = null,
        /**
         * Ostatnio wybrana długość bloku — żeby wyłączenie i ponowne włączenie
         * przełącznika nie kasowało tego, co user ustawił.
         */
        val blockWeeksMemo: Int = ProgressionConstants.BLOCK_WORK_WEEKS_DEFAULT,
        val days: List<DraftDay> = emptyList(),
        /** Edytowany istniejący plan (id/createdAt/archived); null = nowy. */
        val base: Plan? = null,
        /** Nowy plan: czy kreator dobiegł końca (dalej pracuje edytor). */
        val started: Boolean = false,
        /** Krok kreatora; ignorowany, gdy [started]. */
        val step: PlanWizardStep = PlanWizardStep.TEMPLATE,
        /** Wybrany szablon; null = plan od zera. */
        val preset: PlanPreset? = null,
        /** Czy krok „od czego zaczynamy" ma już rozstrzygnięcie. */
        val templateChosen: Boolean = false,
        /** Stawy do oszczędzania, wybrane w kreatorze. */
        val joints: Set<String> = emptySet(),
        /** Ograniczenia wczytane z profilu — prefill robimy dokładnie raz. */
        val jointsPrefilled: Boolean = false,
        /** Sprzęt wybrany w kroku „Twój sprzęt" — puste = pokazuj wszystko (jak w profilu). */
        val equipment: Set<String> = emptySet(),
        /** Sprzęt wczytany z profilu — prefill robimy dokładnie raz, wzorem [jointsPrefilled]. */
        val equipmentPrefilled: Boolean = false,
    )

    /** Warstwy UI ponad edytorem (picker, arkusz zamienników, arkusz sugestii). */
    private data class Overlay(
        val pickerDayIndex: Int? = null,
        val substitutes: SubstitutesUi? = null,
        val suggestions: SuggestionsUi? = null,
    )

    private val draft = MutableStateFlow<Draft?>(null)
    private val allExercises = MutableStateFlow<List<Exercise>?>(null)
    private val profile = MutableStateFlow(ProfileDetails())

    /** Pola profilu spoza [ProfileDetails] — potrzebne przy zapisie z kreatora. */
    private var profileCreatedAt: Long? = null
    private var profileDisplayName: String = ""

    /**
     * Czy przyszedł już PIERWSZY snapshot profilu. Bez tego prefill ograniczeń
     * odpalałby się na pustym [ProfileDetails] i zamykał się (jointsPrefilled),
     * zanim kontuzje z profilu w ogóle dotarły — krok kreatora stał wtedy pusty.
     */
    private var profileLoaded: Boolean = false

    private val overlay = MutableStateFlow(Overlay())

    /**
     * Wynik zapisu — JEDEN stan flow zamiast dwóch osobnych, żeby `saved` i
     * [SaveResult.newDayMessage] dotarły do [uiState] w tej samej emisji:
     * [PlanEditorScreen] czyta oba pola w JEDNYM `LaunchedEffect(state.saved)`
     * (pokaż Snackbar, POTEM onBack) — rozjazd dwóch osobnych flow mógłby dać
     * klatkę z `saved=true` i jeszcze starym (null) komunikatem.
     */
    private data class SaveResult(val newDayMessage: String? = null)

    private val saveResult = MutableStateFlow<SaveResult?>(null)

    /**
     * Guard podwójnego tapu „Zapisz" na okno między zleceniem zapisu w gałęzi
     * [reconcileScheduleOnSave] a realnym ustawieniem [saveResult] PO zapisie
     * (patrz KDoc [save]) — samo `saveResult.value != null` tego okna nie
     * pokrywa, bo w tej gałęzi jest ono ustawiane później, nie od razu.
     * Zwykłe pole `var`, nie [MutableStateFlow] — UI nic z tym nie robi
     * (button nie disable'uje się), to czysto wewnętrzny guard [save].
     */
    private var saveInFlight = false

    /**
     * Czy w harmonogramie istnieje choć jeden wpis PLANNED TEGO planu — fallback
     * dla [PlanEditorUiState.planHasSchedule] na planach sprzed pola
     * `Plan.weekdayAssignments` (patrz KDoc tego pola). Obserwacja cache-first
     * ([ScheduleRepository.observeSchedule] — snapshot listener, zero
     * `get(Source.SERVER)`), filtrowana po `planId` konstruktora: dla nowego
     * planu (`planId == null`) nigdy nie ma dopasowania (`ScheduleEntry.planId`
     * jest zawsze rzeczywistym id), więc zostaje `false` — dokładnie to, czego
     * ta flaga potrzebuje.
     */
    private val hasPlannedScheduleEntries = MutableStateFlow(false)

    private val baseUiState: Flow<PlanEditorUiState> = combine(
        draft, allExercises, profile, overlay, saveResult,
    ) { d, all, currentProfile, ov, savedResult ->
        if (d == null || all == null) {
            PlanEditorUiState(loading = true, isNew = planId == null)
        } else {
            val byId = all.associateBy { it.id }
            PlanEditorUiState(
                loading = false,
                isNew = planId == null,
                wizard = if (d.started) null else wizardUi(d, all),
                name = d.name,
                blockLengthWeeks = d.blockLengthWeeks,
                days = d.days.map { draftDay ->
                    val day = draftDay.day
                    val dayExercises = day.exercises.map { planExercise ->
                        val exercise = byId[planExercise.exerciseId]
                        EditorExerciseUi(
                            planExercise = planExercise,
                            exercise = exercise,
                            compliance = exercise?.let { isCompliant(it, currentProfile) }
                                ?: ComplianceResult(emptyList(), true),
                        )
                    }
                    EditorDayUi(
                        name = day.name,
                        exercises = dayExercises,
                        missingGroups = missingMajorGroups(dayExercises.mapNotNull { it.exercise }),
                        existsInSavedPlan = draftDay.baseDayIndex != null,
                    )
                },
                allExercises = all,
                profile = currentProfile,
                pickerDayIndex = ov.pickerDayIndex,
                substitutes = ov.substitutes,
                suggestions = ov.suggestions,
                canSave = d.started && d.name.isNotBlank() &&
                    d.days.any { it.day.exercises.isNotEmpty() },
                archived = d.base?.archived == true,
                canArchive = d.base != null,
                planHasSchedule = d.base?.weekdayAssignments != null,
                saved = savedResult != null,
                newDayMessage = savedResult?.newDayMessage,
            )
        }
    }

    /**
     * [baseUiState] dopięty o [hasPlannedScheduleEntries] — osobny stopień
     * (wzorzec [com.stronk.ui.workout.WorkoutViewModel.uiState]: `combine`
     * ma typowane przeciążenia tylko do 5 flow, więc szósty wchodzi jako
     * kolejny stopień, nie przez vararg). Efektywne „lub": jeśli
     * `Plan.weekdayAssignments != null` już ustawiło `planHasSchedule = true`
     * w [baseUiState], ten stopień tylko dokłada drugi sygnał (wpisy PLANNED),
     * nigdy nie gasi pierwszego.
     */
    val uiState: StateFlow<PlanEditorUiState> = combine(
        baseUiState, hasPlannedScheduleEntries,
    ) { state, plannedEntriesExist ->
        if (plannedEntriesExist && !state.planHasSchedule) {
            state.copy(planHasSchedule = true)
        } else {
            state
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanEditorUiState())

    init {
        viewModelScope.launch { allExercises.value = exerciseRepository.getAll() }
        viewModelScope.launch {
            userProfileRepository.observeProfile().collect { userProfile ->
                val details = userProfile?.profile ?: ProfileDetails()
                profile.value = details
                profileCreatedAt = userProfile?.createdAt
                profileDisplayName = userProfile?.displayName.orEmpty()
                profileLoaded = true
                prefillJoints(details)
                prefillEquipment(details)
            }
        }
        viewModelScope.launch {
            if (planId == null) {
                draft.value = Draft()
                // Profil mógł dojść przed draftem — wtedy prefill czekał na ten moment.
                if (profileLoaded) {
                    prefillJoints(profile.value)
                    prefillEquipment(profile.value)
                }
            } else {
                // Cache-first: plan otwierany z listy jest już w cache Firestore.
                val plan = planRepository.observePlan(planId).filterNotNull().first()
                draft.value = Draft(
                    name = plan.name,
                    blockLengthWeeks = plan.blockLengthWeeks,
                    blockWeeksMemo = plan.blockLengthWeeks
                        ?: ProgressionConstants.BLOCK_WORK_WEEKS_DEFAULT,
                    // baseDayIndex = pozycja w base.days — punkt odniesienia
                    // dla PlanEditorSave.dayIndexRemap przy zapisie.
                    days = plan.days.mapIndexed { index, day -> DraftDay(baseDayIndex = index, day = day) },
                    base = plan,
                    started = true,
                )
            }
        }
        // Fallback dla planHasSchedule (patrz KDoc [hasPlannedScheduleEntries]) —
        // tylko dla planu istniejącego, nowy (planId == null) nigdy nie ma
        // wpisów, więc nie ma sensu w ogóle podpinać listenera.
        if (planId != null) {
            viewModelScope.launch {
                scheduleRepository.observeSchedule().collect { entries ->
                    hasPlannedScheduleEntries.value = entries.any {
                        it.planId == planId && it.status == ScheduleStatus.PLANNED
                    }
                }
            }
        }
    }

    // ---------- kreator nowego planu ----------

    /** Widok kroku kreatora — czysta projekcja [Draft] (+ dataset dla opcji sprzętu). */
    private fun wizardUi(d: Draft, all: List<Exercise>): PlanWizardUi {
        val steps = PlanWizardStep.entries
        val stepIndex = steps.indexOf(d.step)
        // Te same wartości i ta sama etykieta/sortowanie co w ProfileViewModel —
        // wybrane spoza datasetu (np. stary wpis) doklejamy, żeby dało się odznaczyć.
        val equipmentOptions = all.mapNotNull { it.equipment }.distinct().sortedBy(PlLabels::equipment)
        return PlanWizardUi(
            step = d.step,
            stepIndex = stepIndex,
            stepCount = steps.size,
            presets = PlanPresets.all,
            selectedPresetId = d.preset?.id,
            canGoNext = when (d.step) {
                PlanWizardStep.TEMPLATE -> d.templateChosen
                PlanWizardStep.NAME -> d.name.isNotBlank()
                else -> true
            },
            blockLengthWeeks = d.blockLengthWeeks,
            jointKeys = ProfileDefaults.JOINT_KEYS,
            selectedJoints = d.joints,
            equipmentOptions = equipmentOptions + d.equipment.filterNot { it in equipmentOptions },
            selectedEquipment = d.equipment,
            name = d.name,
            summaryDays = d.preset?.days?.size ?: 1,
            summaryExercises = d.preset?.slotCount ?: 0,
        )
    }

    /**
     * Krok „ograniczenia" startuje z tym, co już jest w profilu — user niczego
     * nie klika drugi raz. Robimy to dokładnie raz, żeby nie nadpisać wyboru
     * przy kolejnym snapshocie z Firestore.
     */
    private fun prefillJoints(details: ProfileDetails) = updateDraft { d ->
        if (d.jointsPrefilled || d.started) {
            d
        } else {
            d.copy(
                joints = details.constraints
                    .filterValues { it != ProfileDefaults.NO_LIMIT_WIRE_LEVEL }
                    .keys.toSet(),
                jointsPrefilled = true,
            )
        }
    }

    /**
     * Krok „Twój sprzęt" startuje z tym, co już jest w profilu — wzorem
     * [prefillJoints]. Dokładnie raz, żeby nie nadpisać wyboru w kreatorze
     * kolejnym snapshotem z Firestore.
     */
    private fun prefillEquipment(details: ProfileDetails) = updateDraft { d ->
        if (d.equipmentPrefilled || d.started) {
            d
        } else {
            d.copy(equipment = details.equipment.toSet(), equipmentPrefilled = true)
        }
    }

    /**
     * [preset] null = „zacznij od zera": jeden pusty dzień, ćwiczenia dobiera user.
     *
     * Szablon przynosi ze sobą blok treningowy (domyślnie 5 tygodni pracy —
     * tak działają presety od zawsze, w tym powrotowy), plan od zera startuje
     * BEZ bloku. W kroku „Długość bloku" user i tak może to odwrócić.
     */
    fun wizardChooseTemplate(preset: PlanPreset?) = updateDraft { d ->
        d.copy(
            preset = preset,
            templateChosen = true,
            blockLengthWeeks = if (preset == null) null else (d.blockLengthWeeks ?: d.blockWeeksMemo),
            // Nazwa z szablonu tylko dopóki user jej nie tknął.
            name = if (d.name.isBlank() || d.name == d.preset?.name) preset?.name.orEmpty() else d.name,
        )
    }

    fun wizardToggleJoint(joint: String) = updateDraft { d ->
        d.copy(joints = if (joint in d.joints) d.joints - joint else d.joints + joint)
    }

    /** „Nie mam ograniczeń — pomiń": czyści wybór i przechodzi dalej. */
    fun wizardSkipConstraints() {
        updateDraft { it.copy(joints = emptySet()) }
        wizardNext()
    }

    fun wizardToggleEquipment(item: String) = updateDraft { d ->
        d.copy(equipment = if (item in d.equipment) d.equipment - item else d.equipment + item)
    }

    /**
     * Pomiń krok „Twój sprzęt": czyści wybór i przechodzi dalej — puste
     * `profile.equipment` to celowa reguła "pokazuj wszystkie ćwiczenia"
     * (patrz [com.stronk.data.isCompliant]), więc pominięcie jest uczciwe.
     */
    fun wizardSkipEquipment() {
        updateDraft { it.copy(equipment = emptySet()) }
        wizardNext()
    }

    fun wizardBack() = updateDraft { d ->
        val steps = PlanWizardStep.entries
        val index = steps.indexOf(d.step)
        if (index <= 0) d else d.copy(step = steps[index - 1])
    }

    /** Ostatni krok domyka kreator: zapisuje sprzęt i ograniczenia, generuje dni. */
    fun wizardNext() {
        val d = draft.value ?: return
        val steps = PlanWizardStep.entries
        val index = steps.indexOf(d.step)
        if (index < steps.lastIndex) {
            updateDraft { it.copy(step = steps[index + 1]) }
        } else {
            finishWizard()
        }
    }

    /**
     * Ostatni krok domyka kreator: zapisuje sprzęt i ograniczenia do profilu,
     * a WYNIKOWYM profilem (nie starym, nie czekamy na echo z Firestore)
     * generuje dni — dopiero wtedy preset realnie widzi wybrany w kroku sprzęt.
     */
    private fun finishWizard() {
        val d = draft.value ?: return
        val all = allExercises.value ?: return
        val details = saveWizardProfile(joints = d.joints, equipment = d.equipment)
        val days = d.preset
            ?.let { generatePresetDays(it, all, details) }
            ?: listOf(PlanDay(name = dayName(0)))
        updateDraft {
            it.copy(
                started = true,
                name = it.name.ifBlank { d.preset?.name.orEmpty() },
                // Nowy plan (base == null) — żaden dzień nie ma jeszcze odpowiednika.
                days = days.map { day -> DraftDay(baseDayIndex = null, day = day) },
            )
        }
    }

    /**
     * Zapisuje sprzęt i ograniczenia z kreatora do profilu i zwraca profil,
     * którym generujemy dni (nie czekamy na powrót snapshotu z Firestore, ADR-002).
     *
     * Wszystkie 7 stawów zapisujemy jawnie: `SetOptions.merge()` nie kasuje
     * kluczy mapy, więc „brak ograniczenia" musi mieć własną wartość wire
     * ([ProfileDefaults.NO_LIMIT_WIRE_LEVEL]). Staw już ograniczony zachowuje
     * swój dokładniejszy limit z profilu — kreator go nie zgrubia. Sprzęt nie ma
     * takiej pułapki (lista, nie mapa) — nadpisujemy go wprost wyborem z kroku,
     * PUSTY = ta sama reguła co w profilu ("pokazuj wszystkie ćwiczenia").
     */
    private fun saveWizardProfile(joints: Set<String>, equipment: Set<String>): ProfileDetails {
        val current = profile.value
        val details = current.copy(
            equipment = equipment.sorted(),
            constraints = ProfileDefaults.JOINT_KEYS.associateWith { joint ->
                if (joint !in joints) {
                    ProfileDefaults.NO_LIMIT_WIRE_LEVEL
                } else {
                    current.constraints[joint]?.takeIf { it != StressLevel.HIGH } ?: StressLevel.LOW
                }
            },
        )
        profile.value = details
        userProfileRepository.save(
            UserProfile(
                displayName = profileDisplayName,
                createdAt = profileCreatedAt ?: System.currentTimeMillis(),
                profile = details,
            ),
        )
        return details
    }

    // ---------- pola planu ----------

    fun onNameChange(name: String) = updateDraft { it.copy(name = name) }

    fun onBlockLengthChange(weeks: Int) = updateDraft {
        val clamped = weeks.coerceIn(PlanDefaults.BLOCK_WEEKS_MIN, PlanDefaults.BLOCK_WEEKS_MAX)
        it.copy(blockLengthWeeks = clamped, blockWeeksMemo = clamped)
    }

    /**
     * Przełącznik „Blok treningowy". Wyłączony = plan bez bloku: progresja leci
     * ciągiem, tydzień lekki nie wypada nigdy, plan może trwać w nieskończoność.
     */
    fun onBlockEnabledChange(enabled: Boolean) = updateDraft {
        if (enabled) {
            it.copy(blockLengthWeeks = it.blockLengthWeeks ?: it.blockWeeksMemo)
        } else {
            it.copy(blockLengthWeeks = null)
        }
    }

    // ---------- dni ----------

    /** Nowy dzień: [DraftDay.baseDayIndex] = null — nie istnieje w bazie, harmonogram go nie zna. */
    fun addDay() = updateDraft {
        it.copy(days = it.days + DraftDay(baseDayIndex = null, day = PlanDay(name = dayName(it.days.size))))
    }

    fun renameDay(dayIndex: Int, name: String) = updateDay(dayIndex) { it.copy(name = name) }

    /**
     * Usunięcie dnia. `baseDayIndex` pozostałych dni draftu jest NIETKNIĘTY —
     * to on (nie bieżąca pozycja) jest tożsamością dnia; [save] z niego
     * buduje mapę oldIndex→newIndex ([PlanEditorSave.dayIndexRemap]) i
     * przemapowuje `Plan.weekdayAssignments` + przepisuje przyszłe wpisy
     * PLANNED tego planu, jeśli plan ma harmonogram (patrz [save]).
     */
    fun removeDay(dayIndex: Int) = updateDraft {
        it.copy(days = it.days.filterIndexed { index, _ -> index != dayIndex })
    }

    // ---------- ćwiczenia ----------

    fun openPicker(dayIndex: Int) {
        overlay.value = overlay.value.copy(pickerDayIndex = dayIndex)
    }

    fun closePicker() {
        overlay.value = overlay.value.copy(pickerDayIndex = null)
    }

    /** Dodaje ćwiczenie z pickera do otwartego dnia i zamyka picker. */
    fun pickExercise(exercise: Exercise) {
        val dayIndex = overlay.value.pickerDayIndex ?: return
        addExercise(dayIndex, exercise)
        overlay.value = Overlay()
    }

    fun updateExercise(dayIndex: Int, exerciseIndex: Int, updated: PlanExercise) =
        updateDay(dayIndex) { day ->
            day.copy(
                exercises = day.exercises.mapIndexed { index, exercise ->
                    if (index == exerciseIndex) updated else exercise
                },
            )
        }

    fun removeExercise(dayIndex: Int, exerciseIndex: Int) = updateDay(dayIndex) { day ->
        day.copy(exercises = day.exercises.filterIndexed { index, _ -> index != exerciseIndex })
    }

    /**
     * Przenosi ćwiczenie w ramach dnia z pozycji [fromIndex] na [toIndex]
     * (drag & drop w edytorze). To PRZESUNIĘCIE, nie zamiana miejscami:
     * pozostałe ćwiczenia zsuwają się o jeden, więc kolejność dnia zmienia się
     * dokładnie tak, jak user widzi ją pod palcem. Indeksy spoza zakresu albo
     * ruch „w to samo miejsce" nie zmieniają nic.
     */
    fun reorderExercise(dayIndex: Int, fromIndex: Int, toIndex: Int) = updateDay(dayIndex) { day ->
        day.copy(exercises = day.exercises.movedItem(fromIndex, toIndex))
    }

    // ---------- zamienniki ----------

    /** Zamienniki dla ćwiczenia już obecnego w planie (wybór = podmiana). */
    fun openSubstitutesForRow(dayIndex: Int, exerciseIndex: Int) {
        val all = allExercises.value ?: return
        val planExercise =
            draft.value?.days?.getOrNull(dayIndex)?.day?.exercises?.getOrNull(exerciseIndex) ?: return
        val exercise = all.firstOrNull { it.id == planExercise.exerciseId } ?: return
        overlay.value = overlay.value.copy(
            substitutes = SubstitutesUi(
                forExercise = exercise,
                // Bez limitu: filtr grupowy w arkuszu działa na pełnej liście, limit
                // (SUBSTITUTE_LIMIT) stosowany DOPIERO PO filtrze (filterSubstitutesByGroup).
                matches = findSubstitutes(exercise, all, profile.value, limit = SubstituteScoring.NO_LIMIT),
                dayIndex = dayIndex,
                replaceIndex = exerciseIndex,
                profile = profile.value,
            ),
        )
    }

    /** Zamienniki z poziomu pickera (wybór = dodanie zamiennika do dnia). */
    fun openSubstitutesForPicker(exercise: Exercise) {
        val all = allExercises.value ?: return
        val dayIndex = overlay.value.pickerDayIndex ?: return
        overlay.value = overlay.value.copy(
            substitutes = SubstitutesUi(
                forExercise = exercise,
                matches = findSubstitutes(exercise, all, profile.value, limit = SubstituteScoring.NO_LIMIT),
                dayIndex = dayIndex,
                replaceIndex = null,
                profile = profile.value,
            ),
        )
    }

    fun closeSubstitutes() {
        overlay.value = overlay.value.copy(substitutes = null)
    }

    // ---------- sugestie pokrycia partii ----------

    /** Otwiera arkusz sugestii dla brakującej [group] w dniu [dayIndex]. */
    fun openSuggestions(dayIndex: Int, group: MuscleGroup) {
        val all = allExercises.value ?: return
        val day = draft.value?.days?.getOrNull(dayIndex)?.day ?: return
        val excludeIds = day.exercises.map { it.exerciseId }.toSet()
        val matches = suggestExercisesForGroup(group, all, profile.value, excludeIds)
        overlay.value = overlay.value.copy(suggestions = SuggestionsUi(group, dayIndex, matches))
    }

    fun closeSuggestions() {
        overlay.value = overlay.value.copy(suggestions = null)
    }

    /** Dodaje wybraną sugestię do dnia i zamyka arkusz. */
    fun pickSuggestion(exercise: Exercise) {
        val suggestions = overlay.value.suggestions ?: return
        addExercise(suggestions.dayIndex, exercise)
        overlay.value = overlay.value.copy(suggestions = null)
    }

    fun chooseSubstitute(match: SubstituteMatch) {
        val substitutes = overlay.value.substitutes ?: return
        val replacement = match.exercise
        if (substitutes.replaceIndex != null) {
            updateDay(substitutes.dayIndex) { day ->
                day.copy(
                    exercises = day.exercises.mapIndexed { index, planExercise ->
                        if (index != substitutes.replaceIndex) {
                            planExercise
                        } else {
                            planExercise.copy(
                                exerciseId = replacement.id,
                                target = convertTarget(
                                    planExercise.target,
                                    replacement.measurementType,
                                ),
                                startWeightKg = planExercise.startWeightKg.takeIf {
                                    replacement.measurementType == MeasurementType.WEIGHT_REPS
                                },
                            )
                        }
                    },
                )
            }
            closeSubstitutes()
        } else {
            addExercise(substitutes.dayIndex, replacement)
            overlay.value = Overlay() // zamyka arkusz i picker
        }
    }

    // ---------- zapis ----------

    /**
     * Buduje dokument planu ([buildPlanForSave] — czysta funkcja, patrz jej KDoc
     * o polach nieedytowanych w tym ekranie) i zapisuje w całości
     * (fire-and-forget, ADR-002).
     *
     * **Zasada nadrzędna: Zapisz zawsze zostawia harmonogram spójny z
     * planem.** Edycja SAMYCH ćwiczeń nie dotyka harmonogramu (jak dotąd —
     * `identityChanged`/`blockChanged` oba `false`, idzie zwykły synchroniczny
     * zapis). Gdy edycja zmienia TOŻSAMOŚĆ dni (usunięcie — [dayIdentityChanged]
     * na mapie z [dayIndexRemap]) LUB długość bloku, [reconcileScheduleOnSave]
     * przemapowuje `Plan.weekdayAssignments` i przepisuje przyszłe wpisy
     * PLANNED tego planu na nowy horyzont — DOKŁADNIE tym samym mechanizmem co
     * [com.stronk.ui.schedule.ScheduleViewModel.onAssignPlan]
     * ([planReplacement]), więc DONE i PLANNED innych, niearchiwalnych planów
     * są tam nietykalne z tych samych powodów.
     *
     * SAMO dodanie dnia nie wymaga przepisania (nowy dzień jeszcze nigdy nie
     * był w harmonogramie, identity remap istniejących dni jest identycznościowy)
     * — user dostaje zamiast tego [PlanEditorUiState.newDayMessage], żeby
     * wiedział, że ma go ręcznie przypisać w „Zaplanuj tydzień".
     *
     * **Zapis nie może dać się uciąć nawigacją.** `saveResult.value != null`
     * (→ `state.saved`) każe [com.stronk.ui.plans.PlanEditorScreen] wywołać `onBack()` →
     * `popBackStack` niszczy `ViewModelStore` tego ekranu → `onCleared()`
     * anuluje `viewModelScope`. Gałąź [reconcileScheduleOnSave] zaczyna od DWÓCH
     * `.first()` (zawsze async hop, nawet z cache) i DOPIERO w niej następuje
     * jedyny zapis planu w tej gałęzi — gdyby `saveResult` był ustawiony przed
     * jej wykonaniem (jak dawniej), cancel z nawigacji mógłby uciąć funkcję
     * ZANIM cokolwiek zapisała, a user widziałby ekran wracający tak, jakby
     * zapis się udał. Fix: cała gałąź async (odczyty + oba zapisy fire-and-forget
     * + samo ustawienie `saveResult`) leci w JEDNYM `viewModelScope.launch {
     * withContext(NonCancellable) { ... } }` — `NonCancellable` sprawia, że
     * zawieszenia w środku (`.first()`) NIE rzucą `CancellationException` nawet
     * gdy `viewModelScope` jest już w trakcie anulowania, więc funkcja zawsze
     * dochodzi do zapisów; `saveResult` ustawiony na samym końcu, WEWNĄTRZ
     * tego samego bloku — samo przeniesienie go za wywołanie (bez
     * `NonCancellable`) by nie wystarczyło, bo cancel mógłby przyjść w oknie
     * między powrotem z `.first()` a tym przypisaniem. Gałąź synchroniczna
     * (bez reconcile) zostaje nietknięta — `planRepository.save(plan)` tam nie
     * zawiesza się w ogóle (fire-and-forget, wraca natychmiast), więc nie ma
     * okna, w którym cancel mógłby ją uciąć przed dotarciem do wywołania.
     *
     * Guard podwójnego tapu: button nie disable'uje się po pierwszym tapnięciu,
     * a odkąd `saveResult` w gałęzi async ustawia się DOPIERO po zapisie, samo
     * `saveResult.value != null` już nie pokrywa okna „kliknięto, zapis w
     * locie, jeszcze nic nie ustawione" — stąd osobna flaga [saveInFlight] tylko
     * na to okno (drugie wywołanie w tym oknie czytałoby harmonogram sprzed
     * commitu pierwszego, ten sam typ race'u co [race z rolling generation]
     * niżej, tylko samozadany). Gałąź synchroniczna tego okna nie ma (zapis i
     * `saveResult` w tej samej linii wykonania), więc jej nie dotyczy.
     */
    fun save() {
        val d = draft.value ?: return
        if (saveResult.value != null || saveInFlight) return
        if (!(d.started && d.name.isNotBlank() && d.days.any { it.day.exercises.isNotEmpty() })) return

        val base = d.base
        val remap = dayIndexRemap(d.days.map { it.baseDayIndex })
        val identityChanged = dayIdentityChanged(remap, base?.days?.size ?: 0)
        val blockChanged = base != null && base.blockLengthWeeks != d.blockLengthWeeks
        val hasNewDay = d.days.any { it.baseDayIndex == null }
        val hadSchedule = base?.weekdayAssignments != null || hasPlannedScheduleEntries.value

        val plan = buildPlanForSave(
            base = base,
            name = d.name,
            blockLengthWeeks = d.blockLengthWeeks,
            days = d.days.map { it.day },
            newId = planRepository::newId,
        )
        val newDayMessage = if (hasNewDay && hadSchedule) PlanTexts.NEW_DAY_NOT_SCHEDULED else null

        if (base != null && (identityChanged || blockChanged)) {
            saveInFlight = true
            viewModelScope.launch {
                withContext(NonCancellable) {
                    reconcileScheduleOnSave(base, plan, remap)
                    saveInFlight = false
                    saveResult.value = SaveResult(newDayMessage = newDayMessage)
                }
            }
        } else {
            planRepository.save(plan)
            saveResult.value = SaveResult(newDayMessage = newDayMessage)
        }
    }

    /**
     * Przemapowuje wzorzec dni tygodnia i przepisuje przyszłe wpisy PLANNED
     * TEGO planu po zapisie z edytora ([save], gdy tożsamość dni się zmieniła
     * albo zmienił się blok) — dokłada się do batcha
     * [ScheduleRepository.replacePlannedEntries] dokładnie jak
     * [com.stronk.ui.schedule.ScheduleViewModel.onAssignPlan].
     *
     * Baseline starego wzorca: [weekPlanBaseline] (ten sam helper co w
     * dialogu „Zaplanuj tydzień") — zapisany wzorzec [base] wygrywa, gdy
     * istnieje, inaczej spada na wpisy PLANNED tego planu jeszcze widoczne w
     * świeżo odczytanym harmonogramie (stary plan sprzed pola
     * `weekdayAssignments`). [remap] (z [dayIndexRemap] w [save]) przekłada go
     * na nowe indeksy dni ([remapWeekdayAssignments]) — przypisania
     * wskazujące USUNIĘTY dzień wypadają.
     *
     * Horyzont: [saveReplanWeeks] — INNY niż [blockReplanWeeks] z dialogu
     * planowania, bo zapis MA PRAWO skracać horyzont po zmniejszeniu bloku
     * (gate: 6→3 tyg. realnie kończy wpisy na 3 tyg., nie zostaje przy starym
     * dłuższym horyzoncie). Start zawsze dziś ([clampStartDateToToday]) —
     * przepisanie nie ma prawa ruszać przeszłości.
     *
     * **Race z rolling generation** ([com.stronk.ui.schedule.ScheduleViewModel.maybeExtendContinuousPlans]):
     * ten VM NIE dzieli z `ScheduleViewModel` żadnego guarda (`pendingReplan`
     * jest prywatnym polem TAMTEGO VM-a) — jeśli ekran Tydzień zostaje pod
     * edytorem na stosie nawigacji (typowe w Compose Navigation — VM żyje,
     * dopóki jego wpis w back stacku nie zostanie zdjęty), jego pętla rolling
     * potrafi się odpalić W TLE przez cały czas edycji. Teoretyczne ryzyko:
     * rolling odczyta [Plan] SPRZED tego zapisu (stary wzorzec/liczba dni) w
     * oknie między odczytem [currentEntries] tutaj a commitem tej paczki i
     * dopisze pojedynczy wpis wg STAREGO wzorca, którego ta paczka już nie
     * obejmie. W praktyce zawężone: (1) rolling dotyczy WYŁĄCZNIE planów BEZ
     * bloku ([isEligibleForRollingExtension]) — edycja BLOKU nie jest w ogóle
     * narażona; (2) odpala się tylko gdy plan jest blisko końca własnego
     * horyzontu ([needsRollingExtension]); (3) samonaprawa — [planReplacement]
     * i tu, i w kolejnym `onAssignPlan`/zapisie tego planu kasuje WSZYSTKIE
     * przyszłe PLANNED tego planu na starcie operacji, więc zabłąkany wpis
     * (jeśli wskaże wciąż istniejący dzień — nieszkodliwy; jeśli usunięty —
     * Tydzień pokaże „Plan usunięty" do tego czasu) zniknie przy najbliższym
     * kolejnym przeplanowaniu. Świadomie bez cross-VM locka — nieproporcjonalne
     * do rzadkości i skutków tego okna.
     */
    private suspend fun reconcileScheduleOnSave(base: Plan, plan: Plan, remap: Map<Int, Int>) {
        val schedule = scheduleRepository.observeSchedule().first()
        val plansById = planRepository.observePlans().first().associateBy { it.id }
        val currentEntries = toEntryRefs(schedule, plansById)

        val existingSlots = schedule
            .filter { it.planId == base.id && it.status == ScheduleStatus.PLANNED }
            .mapNotNull { entry -> parseDate(entry.date)?.let { date -> PlannedSlot(date, entry.dayIndex) } }
        val oldAssignments = weekPlanBaseline(
            base.weekdayAssignments?.let { weekdayAssignmentsFromIso(it) },
            existingSlots,
            base.days.size,
        )
        val remappedAssignments = remapWeekdayAssignments(oldAssignments, remap)

        val startDate = clampStartDateToToday(LocalDate.now())
        val weeks = saveReplanWeeks(ProgressionEngine.fullBlockWeeks(plan.blockLengthWeeks))
        val replan = planReplacement(currentEntries, base.id, remappedAssignments, startDate, weeks)

        planRepository.save(plan.copy(weekdayAssignments = weekdayAssignmentsToIso(remappedAssignments)))
        val newEntries = replan.slots.map { slot ->
            ScheduleEntry(
                id = scheduleRepository.newId(),
                date = slot.date.toString(),
                planId = base.id,
                dayIndex = slot.dayIndex,
            )
        }
        scheduleRepository.replacePlannedEntries(deleteIds = replan.idsToDelete, newEntries = newEntries)
    }

    /**
     * Archiwizacja / przywrócenie planu — akcja szczegółu, nie listy (na liście
     * planów są same karty). Zapisuje aktualny stan edycji razem z flagą, żeby
     * jedno tapnięcie nie gubiło zmian zrobionych przed nim.
     *
     * Archiwizacja (`archived = true`) dodatkowo sprząta przyszłe wpisy PLANNED
     * TEGO planu ([archivedPlanDeadEntryIds] — batch, [ScheduleRepository.replacePlannedEntries]
     * z pustą listą nowych wpisów) — inaczej zostają martwe, niewidoczne w
     * `planOptions` (odfiltrowane jako zarchiwizowane), ale wciąż blokujące
     * planowanie nowego planu na tych datach (dokładnie bug z tego zgłoszenia).
     * DONE nietykalne (historia), przeszłe PLANNED/SKIPPED/MOVED zostają
     * (audit-trail) — [archivedPlanDeadEntryIds] pilnuje obu warunków. Przy
     * przywróceniu (`archived = false`) nic dodatkowo nie robimy — stare wpisy
     * już są skasowane, nowe user zaplanuje przez [com.stronk.ui.schedule.AssignPlanDialog].
     */
    fun setArchived(archived: Boolean) {
        val d = draft.value ?: return
        val base = d.base ?: return
        planRepository.save(
            base.copy(
                name = d.name.trim().ifEmpty { base.name },
                blockLengthWeeks = d.blockLengthWeeks,
                days = d.days.mapIndexed { index, draftDay ->
                    draftDay.day.copy(name = draftDay.day.name.trim().ifEmpty { dayName(index) })
                },
                archived = archived,
            ),
        )
        saveResult.value = SaveResult()
        if (archived) {
            viewModelScope.launch {
                val schedule = scheduleRepository.observeSchedule().first()
                val idsToDelete = archivedPlanDeadEntryIds(scheduleEntryRefsFor(schedule, base.id))
                if (idsToDelete.isNotEmpty()) {
                    scheduleRepository.replacePlannedEntries(deleteIds = idsToDelete, newEntries = emptyList())
                }
            }
        }
    }

    // ---------- pomocnicze ----------

    /** Prefill serii×powtórzeń nowego ćwiczenia z [GoalDefaults] wg celu z profilu. */
    private fun addExercise(dayIndex: Int, exercise: Exercise) = updateDay(dayIndex) { day ->
        val goal = profile.value.goal
        day.copy(
            exercises = day.exercises + PlanExercise(
                exerciseId = exercise.id,
                sets = GoalDefaults.setsFor(goal),
                target = defaultTargetFor(exercise.measurementType, GoalDefaults.repsFor(goal)),
            ),
        )
    }

    /**
     * [ScheduleEntry] → [ScheduleEntryRef] pod [archivedPlanDeadEntryIds]:
     * `archived = true` wyłącznie dla wpisów PLANNED planu [archivedPlanId]
     * (właśnie archiwizowanego przez [setArchived]) — pozostałe plany tu nie
     * mają znaczenia, więc nie potrzeba pełnego `plansById` jak w
     * [com.stronk.ui.schedule.ScheduleViewModel].
     */
    private fun scheduleEntryRefsFor(schedule: List<ScheduleEntry>, archivedPlanId: String): List<ScheduleEntryRef> =
        schedule.mapNotNull { entry ->
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
                    archived = entry.status == ScheduleStatus.PLANNED && entry.planId == archivedPlanId,
                )
            }
        }

    /**
     * [ScheduleEntry] → [ScheduleEntryRef] ogólny (wszystkie plany, nie tylko
     * jeden archiwizowany jak [scheduleEntryRefsFor]) — pod [reconcileScheduleOnSave]
     * / [planReplacement], wzorem `ScheduleViewModel.toEntryRefs`: `archived`
     * na PLANNED odzwierciedla archiwizację WŁAŚCICIELA wpisu (dowolnego
     * planu), żeby [planReplacement] poprawnie traktował martwe wpisy cudzych
     * zarchiwizowanych planów jako niekolidujące.
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
                        else -> ScheduleEntryKind.OTHER
                    },
                    archived = entry.status == ScheduleStatus.PLANNED && plansById[entry.planId]?.archived == true,
                )
            }
        }

    private fun parseDate(raw: String): LocalDate? = runCatching { LocalDate.parse(raw) }.getOrNull()

    private fun updateDraft(transform: (Draft) -> Draft) {
        draft.value = draft.value?.let(transform)
    }

    private fun updateDay(dayIndex: Int, transform: (PlanDay) -> PlanDay) = updateDraft { d ->
        d.copy(
            days = d.days.mapIndexed { index, draftDay ->
                if (index == dayIndex) draftDay.copy(day = transform(draftDay.day)) else draftDay
            },
        )
    }

    companion object {
        /** Fabryka z parametrem planId — ręczna kompozycja z [StronkApplication]. */
        fun factory(planId: String?): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as StronkApplication
                PlanEditorViewModel(
                    planId = planId,
                    planRepository = app.planRepository,
                    userProfileRepository = app.userProfileRepository,
                    scheduleRepository = app.scheduleRepository,
                    exerciseRepository = app.exerciseRepository,
                )
            }
        }
    }
}
