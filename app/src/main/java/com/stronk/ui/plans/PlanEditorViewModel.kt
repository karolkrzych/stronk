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
import com.stronk.data.StressLevel
import com.stronk.data.SubstituteMatch
import com.stronk.data.UserProfile
import com.stronk.data.UserProfileRepository
import com.stronk.data.findSubstitutes
import com.stronk.data.isCompliant
import com.stronk.progression.ProgressionConstants
import com.stronk.ui.profile.ProfileDefaults
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
)

/**
 * Krok kreatora nowego planu (mock `pack-dzis-plany.html`, ekran 3).
 * Kolejność jest wymuszona logiką: ograniczenia muszą być znane ZANIM
 * wygenerujemy dni z szablonu, bo to one decydują o doborze ćwiczeń.
 */
enum class PlanWizardStep(val title: String, val subtitle: String) {
    TEMPLATE("Od czego zaczynamy", "Wybierz szablon albo złóż plan od zera."),
    BLOCK("Blok treningowy", "Możesz go wyłączyć — wtedy plan biegnie bez końca."),
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
    /** Kroki 1 i 4 wymagają wyboru/nazwy; reszta zawsze przepuszcza dalej. */
    val canGoNext: Boolean = false,
    /** Tygodnie PRACY w bloku; null = plan bez bloku (ciągła progresja). */
    val blockLengthWeeks: Int? = null,
    /** Klucze stawów w kolejności prezentacji (jak w profilu). */
    val jointKeys: List<String> = emptyList(),
    val selectedJoints: Set<String> = emptySet(),
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
    /** true po zleceniu zapisu — ekran woła onBack. */
    val saved: Boolean = false,
)

class PlanEditorViewModel(
    private val planId: String?,
    private val planRepository: PlanRepository,
    private val userProfileRepository: UserProfileRepository,
    exerciseRepository: ExerciseRepository,
) : ViewModel() {

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
        val days: List<PlanDay> = emptyList(),
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
    private val saved = MutableStateFlow(false)

    val uiState: StateFlow<PlanEditorUiState> = combine(
        draft, allExercises, profile, overlay, saved,
    ) { d, all, currentProfile, ov, isSaved ->
        if (d == null || all == null) {
            PlanEditorUiState(loading = true, isNew = planId == null)
        } else {
            val byId = all.associateBy { it.id }
            PlanEditorUiState(
                loading = false,
                isNew = planId == null,
                wizard = if (d.started) null else wizardUi(d),
                name = d.name,
                blockLengthWeeks = d.blockLengthWeeks,
                days = d.days.map { day ->
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
                    )
                },
                allExercises = all,
                profile = currentProfile,
                pickerDayIndex = ov.pickerDayIndex,
                substitutes = ov.substitutes,
                suggestions = ov.suggestions,
                canSave = d.started && d.name.isNotBlank() &&
                    d.days.any { it.exercises.isNotEmpty() },
                archived = d.base?.archived == true,
                canArchive = d.base != null,
                saved = isSaved,
            )
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
            }
        }
        viewModelScope.launch {
            if (planId == null) {
                draft.value = Draft()
                // Profil mógł dojść przed draftem — wtedy prefill czekał na ten moment.
                if (profileLoaded) prefillJoints(profile.value)
            } else {
                // Cache-first: plan otwierany z listy jest już w cache Firestore.
                val plan = planRepository.observePlan(planId).filterNotNull().first()
                draft.value = Draft(
                    name = plan.name,
                    blockLengthWeeks = plan.blockLengthWeeks,
                    blockWeeksMemo = plan.blockLengthWeeks
                        ?: ProgressionConstants.BLOCK_WORK_WEEKS_DEFAULT,
                    days = plan.days,
                    base = plan,
                    started = true,
                )
            }
        }
    }

    // ---------- kreator nowego planu ----------

    /** Widok kroku kreatora — czysta projekcja [Draft], zero stanu własnego. */
    private fun wizardUi(d: Draft): PlanWizardUi {
        val steps = PlanWizardStep.entries
        val stepIndex = steps.indexOf(d.step)
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

    fun wizardBack() = updateDraft { d ->
        val steps = PlanWizardStep.entries
        val index = steps.indexOf(d.step)
        if (index <= 0) d else d.copy(step = steps[index - 1])
    }

    /** Ostatni krok domyka kreator: zapisuje ograniczenia i generuje dni. */
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

    private fun finishWizard() {
        val d = draft.value ?: return
        val all = allExercises.value ?: return
        val details = saveConstraints(d.joints)
        val days = d.preset
            ?.let { generatePresetDays(it, all, details) }
            ?: listOf(PlanDay(name = dayName(0)))
        updateDraft {
            it.copy(
                started = true,
                name = it.name.ifBlank { d.preset?.name.orEmpty() },
                days = days,
            )
        }
    }

    /**
     * Zapisuje ograniczenia z kreatora do profilu i zwraca profil, którym
     * generujemy dni (nie czekamy na powrót snapshotu z Firestore, ADR-002).
     *
     * Wszystkie 7 stawów zapisujemy jawnie: `SetOptions.merge()` nie kasuje
     * kluczy mapy, więc „brak ograniczenia" musi mieć własną wartość wire
     * ([ProfileDefaults.NO_LIMIT_WIRE_LEVEL]). Staw już ograniczony zachowuje
     * swój dokładniejszy limit z profilu — kreator go nie zgrubia.
     */
    private fun saveConstraints(joints: Set<String>): ProfileDetails {
        val current = profile.value
        val details = current.copy(
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

    fun addDay() = updateDraft { it.copy(days = it.days + PlanDay(name = dayName(it.days.size))) }

    fun renameDay(dayIndex: Int, name: String) = updateDay(dayIndex) { it.copy(name = name) }

    /**
     * UWAGA: usunięcie dnia z istniejącego planu przesuwa dayIndex kolejnych dni —
     * wpisy harmonogramu wskazujące te dni pokażą inny trening (świadomy trade-off alfy).
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
            draft.value?.days?.getOrNull(dayIndex)?.exercises?.getOrNull(exerciseIndex) ?: return
        val exercise = all.firstOrNull { it.id == planExercise.exerciseId } ?: return
        overlay.value = overlay.value.copy(
            substitutes = SubstitutesUi(
                forExercise = exercise,
                matches = findSubstitutes(exercise, all, profile.value, PlanDefaults.SUBSTITUTE_LIMIT),
                dayIndex = dayIndex,
                replaceIndex = exerciseIndex,
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
                matches = findSubstitutes(exercise, all, profile.value, PlanDefaults.SUBSTITUTE_LIMIT),
                dayIndex = dayIndex,
                replaceIndex = null,
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
        val day = draft.value?.days?.getOrNull(dayIndex) ?: return
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

    /** Buduje dokument planu i zapisuje w całości (fire-and-forget, ADR-002). */
    fun save() {
        val d = draft.value ?: return
        if (!(d.started && d.name.isNotBlank() && d.days.any { it.exercises.isNotEmpty() })) return
        val plan = Plan(
            id = d.base?.id ?: planRepository.newId(),
            name = d.name.trim(),
            createdAt = d.base?.createdAt ?: System.currentTimeMillis(),
            archived = d.base?.archived ?: false,
            blockLengthWeeks = d.blockLengthWeeks,
            days = d.days.mapIndexed { index, day ->
                day.copy(name = day.name.trim().ifEmpty { dayName(index) })
            },
        )
        planRepository.save(plan)
        saved.value = true
    }

    /**
     * Archiwizacja / przywrócenie planu — akcja szczegółu, nie listy (na liście
     * planów są same karty). Zapisuje aktualny stan edycji razem z flagą, żeby
     * jedno tapnięcie nie gubiło zmian zrobionych przed nim.
     */
    fun setArchived(archived: Boolean) {
        val d = draft.value ?: return
        val base = d.base ?: return
        planRepository.save(
            base.copy(
                name = d.name.trim().ifEmpty { base.name },
                blockLengthWeeks = d.blockLengthWeeks,
                days = d.days.mapIndexed { index, day ->
                    day.copy(name = day.name.trim().ifEmpty { dayName(index) })
                },
                archived = archived,
            ),
        )
        saved.value = true
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

    private fun updateDraft(transform: (Draft) -> Draft) {
        draft.value = draft.value?.let(transform)
    }

    private fun updateDay(dayIndex: Int, transform: (PlanDay) -> PlanDay) = updateDraft { d ->
        d.copy(
            days = d.days.mapIndexed { index, day ->
                if (index == dayIndex) transform(day) else day
            },
        )
    }

    companion object {
        /** Domyślna nazwa dnia: "Dzień A", "Dzień B", … */
        private fun dayName(index: Int): String =
            if (index < 26) "Dzień ${'A' + index}" else "Dzień ${index + 1}"

        /** Fabryka z parametrem planId — ręczna kompozycja z [StronkApplication]. */
        fun factory(planId: String?): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as StronkApplication
                PlanEditorViewModel(
                    planId = planId,
                    planRepository = app.planRepository,
                    userProfileRepository = app.userProfileRepository,
                    exerciseRepository = app.exerciseRepository,
                )
            }
        }
    }
}
