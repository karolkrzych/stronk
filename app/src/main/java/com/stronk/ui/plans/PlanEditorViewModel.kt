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

/** Stan edytora planu. */
data class PlanEditorUiState(
    val loading: Boolean = true,
    val isNew: Boolean = false,
    /** Krok kreatora nowego planu; null = edycja istniejącego planu (bez kreatora). */
    val wizardStep: PlanWizardStep? = null,
    val presets: List<PlanPreset> = emptyList(),
    val name: String = "",
    /** Tygodnie PRACY w bloku (ADR-004), bez tygodnia lekkiego. */
    val blockLengthWeeks: Int = ProgressionConstants.BLOCK_WORK_WEEKS_DEFAULT,
    val days: List<EditorDayUi> = emptyList(),
    /** Cały dataset — dla pickera ćwiczeń. */
    val allExercises: List<Exercise> = emptyList(),
    val profile: ProfileDetails = ProfileDetails(),
    /** Realne ograniczenia z profilu (bez kodowania „brak limitu”) — krok 3 kreatora. */
    val constraints: Map<String, StressLevel> = emptyMap(),
    /** Dzień, dla którego otwarty jest picker; null = picker zamknięty. */
    val pickerDayIndex: Int? = null,
    val substitutes: SubstitutesUi? = null,
    val suggestions: SuggestionsUi? = null,
    val canSave: Boolean = false,
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
        val blockLengthWeeks: Int = ProgressionConstants.BLOCK_WORK_WEEKS_DEFAULT,
        val days: List<PlanDay> = emptyList(),
        /** Edytowany istniejący plan (id/createdAt/archived); null = nowy. */
        val base: Plan? = null,
        /** Krok kreatora nowego planu; przy edycji istniejącego zawsze [PlanWizardStep.DAYS]. */
        val step: PlanWizardStep = PlanWizardStep.START,
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
    private val overlay = MutableStateFlow(Overlay())
    private val saved = MutableStateFlow(false)

    /** Ostatni dokument profilu — zapis ograniczeń nie może zgubić imienia i createdAt. */
    private var profileDocument: UserProfile? = null

    /** true po pierwszej zmianie ograniczeń w kreatorze (patrz kolektor profilu). */
    private var constraintsDirty = false

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
                wizardStep = d.step.takeIf { planId == null },
                presets = PlanPresets.all,
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
                constraints = realConstraints(currentProfile),
                pickerDayIndex = ov.pickerDayIndex,
                substitutes = ov.substitutes,
                suggestions = ov.suggestions,
                canSave = d.step == PlanWizardStep.DAYS && d.name.isNotBlank() &&
                    d.days.any { it.exercises.isNotEmpty() },
                saved = isSaved,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanEditorUiState())

    init {
        viewModelScope.launch { allExercises.value = exerciseRepository.getAll() }
        viewModelScope.launch {
            userProfileRepository.observeProfile().collect { userProfile ->
                profileDocument = userProfile
                // Po własnej edycji ograniczeń lokalny stan jest prawdą — echo
                // własnego zapisu nie może cofać zaznaczonych chipów.
                if (!constraintsDirty) profile.value = userProfile?.profile ?: ProfileDetails()
            }
        }
        viewModelScope.launch {
            if (planId == null) {
                draft.value = Draft()
            } else {
                // Cache-first: plan otwierany z listy jest już w cache Firestore.
                val plan = planRepository.observePlan(planId).filterNotNull().first()
                draft.value = Draft(
                    name = plan.name,
                    blockLengthWeeks = plan.blockLengthWeeks,
                    days = plan.days,
                    base = plan,
                    step = PlanWizardStep.DAYS,
                )
            }
        }
    }

    // ---------- kreator nowego planu ----------

    fun startFromScratch() {
        updateDraft {
            it.copy(step = PlanWizardStep.BASICS, days = listOf(PlanDay(name = dayName(0))))
        }
    }

    fun applyPreset(preset: PlanPreset) {
        val all = allExercises.value ?: return
        val days = generatePresetDays(preset, all, profile.value)
        updateDraft { it.copy(step = PlanWizardStep.BASICS, name = preset.name, days = days) }
    }

    /** Krok dalej w kreatorze; z ostatniego kroku już nie ma dokąd iść. */
    fun nextStep() = updateDraft { d ->
        val next = PlanWizardStep.entries.getOrNull(d.step.ordinal + 1) ?: return@updateDraft d
        d.copy(step = next)
    }

    /** Krok wstecz w kreatorze; ze [PlanWizardStep.START] wychodzi się przez „Wstecz” ekranu. */
    fun previousStep() = updateDraft { d ->
        val previous = PlanWizardStep.entries.getOrNull(d.step.ordinal - 1) ?: return@updateDraft d
        d.copy(step = previous)
    }

    // ---------- ograniczenia (krok 3 kreatora) ----------

    /** Włącza/wyłącza ograniczenie stawu; zapis profilu fire-and-forget (ADR-002). */
    fun toggleConstraint(joint: String) {
        val details = profile.value
        val level = if (isConstrained(details, joint)) {
            ProfileDefaults.NO_LIMIT_WIRE_LEVEL
        } else {
            PlanDefaults.WIZARD_CONSTRAINT_LEVEL
        }
        persistConstraints(
            ProfileDefaults.JOINT_KEYS.associateWith { key ->
                if (key == joint) level else details.constraints[key] ?: ProfileDefaults.NO_LIMIT_WIRE_LEVEL
            },
        )
    }

    /** „Nie mam ograniczeń” — czyści wszystkie limity i przechodzi dalej. */
    fun clearConstraintsAndSkip() {
        persistConstraints(
            ProfileDefaults.JOINT_KEYS.associateWith { ProfileDefaults.NO_LIMIT_WIRE_LEVEL },
        )
        nextStep()
    }

    private fun persistConstraints(constraints: Map<String, StressLevel>) {
        constraintsDirty = true
        val details = profile.value.copy(constraints = constraints)
        profile.value = details
        val document = profileDocument
        userProfileRepository.save(
            UserProfile(
                displayName = document?.displayName,
                createdAt = document?.createdAt ?: System.currentTimeMillis(),
                profile = details,
            ),
        )
    }

    // ---------- pola planu ----------

    fun onNameChange(name: String) = updateDraft { it.copy(name = name) }

    fun onBlockLengthChange(weeks: Int) = updateDraft {
        it.copy(
            blockLengthWeeks = weeks.coerceIn(
                PlanDefaults.BLOCK_WEEKS_MIN,
                PlanDefaults.BLOCK_WEEKS_MAX,
            ),
        )
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

    /** Przesuwa ćwiczenie w ramach dnia o [delta] pozycji (np. −1 / +1). */
    fun moveExercise(dayIndex: Int, exerciseIndex: Int, delta: Int) = updateDay(dayIndex) { day ->
        val target = exerciseIndex + delta
        if (target !in day.exercises.indices || exerciseIndex !in day.exercises.indices) {
            day
        } else {
            val reordered = day.exercises.toMutableList()
            reordered[exerciseIndex] = reordered[target].also {
                reordered[target] = reordered[exerciseIndex]
            }
            day.copy(exercises = reordered)
        }
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
        val complete = d.step == PlanWizardStep.DAYS && d.name.isNotBlank() &&
            d.days.any { it.exercises.isNotEmpty() }
        if (!complete) return
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
        /**
         * Realne ograniczenia z profilu: bez „brak limitu” (HIGH) — dokładnie to,
         * co chipy kroku 3 mają pokazywać jako zaznaczone.
         */
        fun realConstraints(details: ProfileDetails): Map<String, StressLevel> =
            details.constraints.filterValues { it != StressLevel.HIGH }

        private fun isConstrained(details: ProfileDetails, joint: String): Boolean =
            details.constraints[joint]?.let { it != StressLevel.HIGH } ?: false

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
