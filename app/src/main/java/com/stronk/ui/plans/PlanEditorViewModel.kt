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
import com.stronk.data.MeasurementType
import com.stronk.data.Plan
import com.stronk.data.PlanDay
import com.stronk.data.PlanExercise
import com.stronk.data.PlanRepository
import com.stronk.data.ProfileDetails
import com.stronk.data.SubstituteMatch
import com.stronk.data.UserProfileRepository
import com.stronk.data.findSubstitutes
import com.stronk.data.isCompliant
import com.stronk.progression.ProgressionConstants
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
    /** Nowy plan przed wyborem trybu startu (preset / od zera). */
    val showStartChooser: Boolean = false,
    val presets: List<PlanPreset> = emptyList(),
    val name: String = "",
    /** Tygodnie PRACY w bloku (ADR-004), bez tygodnia lekkiego. */
    val blockLengthWeeks: Int = ProgressionConstants.BLOCK_WORK_WEEKS_DEFAULT,
    val days: List<EditorDayUi> = emptyList(),
    /** Cały dataset — dla pickera ćwiczeń. */
    val allExercises: List<Exercise> = emptyList(),
    val profile: ProfileDetails = ProfileDetails(),
    /** Dzień, dla którego otwarty jest picker; null = picker zamknięty. */
    val pickerDayIndex: Int? = null,
    val substitutes: SubstitutesUi? = null,
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
        /** Nowy plan: czy wybrano już start (preset / od zera). */
        val started: Boolean = false,
    )

    /** Warstwy UI ponad edytorem (picker, arkusz zamienników). */
    private data class Overlay(
        val pickerDayIndex: Int? = null,
        val substitutes: SubstitutesUi? = null,
    )

    private val draft = MutableStateFlow<Draft?>(null)
    private val allExercises = MutableStateFlow<List<Exercise>?>(null)
    private val profile = MutableStateFlow(ProfileDetails())
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
                showStartChooser = !d.started,
                presets = PlanPresets.all,
                name = d.name,
                blockLengthWeeks = d.blockLengthWeeks,
                days = d.days.map { day ->
                    EditorDayUi(
                        name = day.name,
                        exercises = day.exercises.map { planExercise ->
                            val exercise = byId[planExercise.exerciseId]
                            EditorExerciseUi(
                                planExercise = planExercise,
                                exercise = exercise,
                                compliance = exercise?.let { isCompliant(it, currentProfile) }
                                    ?: ComplianceResult(emptyList(), true),
                            )
                        },
                    )
                },
                allExercises = all,
                profile = currentProfile,
                pickerDayIndex = ov.pickerDayIndex,
                substitutes = ov.substitutes,
                canSave = d.started && d.name.isNotBlank() &&
                    d.days.any { it.exercises.isNotEmpty() },
                saved = isSaved,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanEditorUiState())

    init {
        viewModelScope.launch { allExercises.value = exerciseRepository.getAll() }
        viewModelScope.launch {
            userProfileRepository.observeProfile().collect { userProfile ->
                profile.value = userProfile?.profile ?: ProfileDetails()
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
                    started = true,
                )
            }
        }
    }

    // ---------- start nowego planu ----------

    fun startFromScratch() {
        updateDraft { it.copy(started = true, days = listOf(PlanDay(name = dayName(0)))) }
    }

    fun applyPreset(preset: PlanPreset) {
        val all = allExercises.value ?: return
        val days = generatePresetDays(preset, all, profile.value)
        updateDraft { it.copy(started = true, name = preset.name, days = days) }
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

    // ---------- pomocnicze ----------

    private fun addExercise(dayIndex: Int, exercise: Exercise) = updateDay(dayIndex) { day ->
        day.copy(
            exercises = day.exercises + PlanExercise(
                exerciseId = exercise.id,
                sets = PlanDefaults.DEFAULT_SETS,
                target = defaultTargetFor(exercise.measurementType),
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
