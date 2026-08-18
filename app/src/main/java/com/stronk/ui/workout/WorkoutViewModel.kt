package com.stronk.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stronk.StronkApplication
import com.stronk.data.Exercise
import com.stronk.data.ExerciseState
import com.stronk.data.ProfileDetails
import com.stronk.data.ScheduleStatus
import com.stronk.data.SetLog
import com.stronk.data.Workout
import com.stronk.data.findSubstitutes
import com.stronk.progression.ProgressionConstants
import com.stronk.progression.ProgressionEngine
import com.stronk.service.RestTimerService
import com.stronk.ui.PlLabels
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Wiersz ćwiczenia na liście treningu. */
data class WorkoutExerciseUi(
    val index: Int,
    val exerciseId: String,
    val name: String,
    val muscleLabel: String,
    val targetLabel: String,
    val lastLabel: String?,
    val badges: List<String>,
    val doneSets: Int,
    val totalSets: Int,
    val isCurrent: Boolean,
    val isComplete: Boolean,
    val skipped: Boolean,
    val substituted: Boolean,
    val imagePath: String?,
)

/** Bieżąca seria pod wielki ✓ (ADR-005). */
data class CurrentSetUi(
    val exerciseIndex: Int,
    val exerciseId: String,
    val exerciseName: String,
    val setNumber: Int,
    val totalSets: Int,
    /** Gotowa seria do zalogowania jednym tapnięciem (i baza do dialogu edycji). */
    val prefill: SetLog,
    val prefillLabel: String,
    /** true → pierwsza seria WEIGHT_REPS bez znanego ciężaru: ✓ otwiera edycję. */
    val needsInput: Boolean,
    val lastLabel: String?,
    val badges: List<String>,
)

/** Propozycja zamiennika w arkuszu (ADR-005 pkt 6). */
data class SubstituteUi(
    val exercise: Exercise,
    val equipmentLabel: String,
    /** Naruszenia limitów stawów z profilu — flagujemy, nie ukrywamy. */
    val warningLabels: List<String>,
)

data class SubstitutesState(
    val forExerciseName: String,
    val options: List<SubstituteUi>,
)

data class WorkoutUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val dayName: String = "",
    val planName: String = "",
    val exercises: List<WorkoutExerciseUi> = emptyList(),
    val current: CurrentSetUi? = null,
    /** Podgląd tego, co po bieżącej serii ("seria 3 z 3", "następne: Wiosła…"). */
    val nextUp: String? = null,
    val allFinished: Boolean = false,
    val completedSets: Int = 0,
    val totalSets: Int = 0,
    val restRemainingSeconds: Int? = null,
    val restSeconds: Int = WorkoutConstants.DEFAULT_REST_SECONDS,
    val saving: Boolean = false,
    /** Po zapisie — ekran woła onFinished. */
    val finished: Boolean = false,
    val substitutes: SubstitutesState? = null,
    val hasLoggedSets: Boolean = false,
)

/**
 * ViewModel trybu treningu. Stan sesji żyje w [WorkoutSessionManager]
 * (singleton — przeżywa wygaszenie ekranu i podgląd ćwiczenia w bazie);
 * ViewModel buduje sesję z planu + silnika progresji, mapuje ją na UI state
 * i domyka trening (zapis Workout + ExerciseState + harmonogram).
 */
class WorkoutViewModel(
    private val app: StronkApplication,
    private val planId: String,
    private val dayIndex: Int,
    private val scheduleEntryId: String?,
) : ViewModel() {

    private val manager = WorkoutSessionManager

    private val error = MutableStateFlow<String?>(null)
    private val saving = MutableStateFlow(false)
    private val finished = MutableStateFlow(false)
    private val substitutes = MutableStateFlow<SubstitutesState?>(null)

    // Dane pomocnicze załadowane raz przy starcie — pod prefille i zamienniki.
    private var allExercises: List<Exercise> = emptyList()
    private var profileDetails: ProfileDetails = ProfileDetails()
    private var statesById: Map<String, ExerciseState> = emptyMap()

    private data class Snapshot(
        val session: WorkoutSession?,
        val error: String?,
        val saving: Boolean,
        val finished: Boolean,
        val substitutes: SubstitutesState?,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<WorkoutUiState> = combine(
        manager.session, error, saving, finished, substitutes,
    ) { session, err, sav, fin, subs ->
        Snapshot(session, err, sav, fin, subs)
    }.flatMapLatest { snap ->
        // Ticker tylko w trakcie przerwy — poza nią stan jest statyczny.
        if (snap.session?.restEndsAtMillis != null && !snap.finished) {
            tick().map { buildUiState(snap, System.currentTimeMillis()) }
        } else {
            flowOf(buildUiState(snap, System.currentTimeMillis()))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutUiState())

    init {
        viewModelScope.launch {
            allExercises = app.exerciseRepository.getAll()
            profileDetails = app.userProfileRepository.observeProfile().first()?.profile
                ?: ProfileDetails()
            statesById = app.exerciseStateRepository.observeAll().first()
            if (!manager.isActiveFor(planId, dayIndex)) {
                manager.clear()
                buildSession()
            }
            // Serwis timera działa przez cały trening (notyfikacja + ✓ z lock screena).
            if (manager.session.value != null) RestTimerService.start(app)
        }
    }

    private fun tick() = flow {
        while (true) {
            emit(Unit)
            delay(WorkoutConstants.UI_TICK_MILLIS)
        }
    }

    private suspend fun buildSession() {
        val plan = app.planRepository.observePlan(planId).first()
        if (plan == null) {
            error.value = "Nie znaleziono planu treningowego."
            return
        }
        val day = plan.days.getOrNull(dayIndex)
        if (day == null || day.exercises.isEmpty()) {
            error.value = "Ten dzień planu jest pusty."
            return
        }
        val now = System.currentTimeMillis()
        // Kontrakt silnika: pełna długość bloku = tygodnie pracy + tydzień lekki.
        val fullBlock = plan.blockLengthWeeks + ProgressionConstants.BLOCK_LIGHT_WEEKS
        val weekIdx = ProgressionEngine.weekIndexInBlock(plan.createdAt, now, fullBlock)
        val returning = profileDetails.returningFromBreak
        val byId = allExercises.associateBy { it.id }
        val sessionExercises = day.exercises.mapIndexed { index, planExercise ->
            val exercise = byId[planExercise.exerciseId]
            val state = statesById[planExercise.exerciseId]
            SessionExercise(
                exercise = exercise,
                planExercise = planExercise,
                proposal = ProgressionEngine.proposeTargets(
                    planExercise = planExercise,
                    state = state,
                    returningFromBreak = returning,
                    isCompoundLeg = exercise?.let(ProgressionEngine::isCompoundLeg) ?: false,
                    weekIndexInBlock = weekIdx,
                    blockLengthWeeks = fullBlock,
                ),
                oldState = state,
                planExerciseIndex = index,
            )
        }
        manager.start(
            WorkoutSession(
                workoutId = app.workoutRepository.newId(),
                planId = planId,
                dayIndex = dayIndex,
                scheduleEntryId = scheduleEntryId,
                planName = plan.name,
                dayName = day.name,
                startedAt = now,
                weekIndexInBlock = weekIdx,
                fullBlockLengthWeeks = fullBlock,
                returningFromBreak = returning,
                exercises = sessionExercises,
            ),
        )
    }

    private fun buildUiState(snap: Snapshot, now: Long): WorkoutUiState {
        if (snap.finished) return WorkoutUiState(loading = false, finished = true)
        snap.error?.let { return WorkoutUiState(loading = false, error = it) }
        val session = snap.session ?: return WorkoutUiState(loading = true)

        val rows = session.exercises.mapIndexed { index, se ->
            WorkoutExerciseUi(
                index = index,
                exerciseId = se.exerciseId,
                name = se.name,
                muscleLabel = se.exercise?.primaryMuscles?.firstOrNull()
                    ?.let(PlLabels::muscle).orEmpty(),
                targetLabel = WorkoutLabels.proposalTarget(se.proposal),
                lastLabel = WorkoutLabels.lastTime(se.oldState),
                badges = WorkoutLabels.proposalBadges(se.proposal),
                doneSets = se.workingLogged.size.coerceAtMost(se.proposal.sets),
                totalSets = se.proposal.sets,
                isCurrent = index == session.currentExerciseIndex && !se.isFinished,
                isComplete = se.isComplete,
                skipped = se.skipped && !se.isComplete,
                substituted = se.substitutedFromId != null,
                imagePath = se.exercise?.images?.firstOrNull(),
            )
        }
        val currentSe = session.currentExercise
        val currentUi = currentSe?.let { se ->
            val prefill = buildPrefill(se, session.workoutId, now)
            CurrentSetUi(
                exerciseIndex = session.currentExerciseIndex,
                exerciseId = se.exerciseId,
                exerciseName = se.name,
                setNumber = se.nextSetNumber,
                totalSets = se.proposal.sets,
                prefill = prefill,
                prefillLabel = WorkoutLabels.setValue(prefill),
                needsInput = session.currentPrefillNeedsInput,
                lastLabel = WorkoutLabels.lastTime(se.oldState),
                badges = WorkoutLabels.proposalBadges(se.proposal),
            )
        }
        val nextUp = currentSe?.let { se ->
            when {
                se.nextSetNumber < se.proposal.sets ->
                    "Potem: seria ${se.nextSetNumber + 1} z ${se.proposal.sets}"
                else -> session.nextUnfinishedExercise()
                    ?.let { "Potem: ${it.name}" }
                    ?: "To ostatnia seria treningu"
            }
        }
        return WorkoutUiState(
            loading = false,
            dayName = session.dayName,
            planName = session.planName,
            exercises = rows,
            current = currentUi,
            nextUp = nextUp,
            allFinished = session.allFinished,
            completedSets = session.completedSetCount,
            totalSets = session.totalSetCount,
            restRemainingSeconds = session.restRemainingSeconds(now),
            restSeconds = session.restSeconds,
            saving = snap.saving,
            substitutes = snap.substitutes,
            hasLoggedSets = session.hasLoggedSets,
        )
    }

    // ------------------------------------------------------------ akcje serii

    /** Wielki ✓ — seria zaliczona z prefillem. */
    fun completeCurrentSet() = manager.completeCurrentSet()

    /** Zalogowanie serii po edycji odstępstwa (dialog steppera). */
    fun logEditedSet(edited: SetLog) {
        val now = System.currentTimeMillis()
        manager.mutate { session ->
            val se = session.currentExercise ?: return@mutate session
            // Dialog edytuje tylko wartości — pola kontekstu przybijamy tutaj.
            val stamped: SetLog = when (edited) {
                is SetLog.WeightReps -> edited.copy(
                    exerciseId = se.exerciseId, workoutId = session.workoutId,
                    setNumber = se.nextSetNumber, isWarmup = false, timestamp = now,
                )
                is SetLog.Reps -> edited.copy(
                    exerciseId = se.exerciseId, workoutId = session.workoutId,
                    setNumber = se.nextSetNumber, isWarmup = false, timestamp = now,
                )
                is SetLog.Time -> edited.copy(
                    exerciseId = se.exerciseId, workoutId = session.workoutId,
                    setNumber = se.nextSetNumber, isWarmup = false, timestamp = now,
                )
                is SetLog.DistanceTime -> edited.copy(
                    exerciseId = se.exerciseId, workoutId = session.workoutId,
                    setNumber = se.nextSetNumber, isWarmup = false, timestamp = now,
                )
            }
            session.logSet(stamped, now)
        }
    }

    fun selectExercise(index: Int) = manager.mutate { it.selectExercise(index) }

    fun skipCurrentExercise() = manager.mutate { it.skipCurrentExercise() }

    // ------------------------------------------------------------- rest timer

    fun adjustRestLength(deltaSeconds: Int) =
        manager.mutate { it.withRestSeconds(it.restSeconds + deltaSeconds) }

    fun extendRest() = manager.mutate { it.extendRest(WorkoutConstants.REST_STEP_SECONDS) }

    fun skipRest() = manager.mutate { it.skipRest() }

    // ------------------------------------------------------------- zamienniki

    /** "Stanowisko zajęte / brak sprzętu" — ranking zamienników pod profil. */
    fun showSubstitutes() {
        val se = manager.session.value?.currentExercise ?: return
        val exercise = se.exercise ?: return
        val matches = findSubstitutes(exercise, allExercises, profileDetails)
        substitutes.value = SubstitutesState(
            forExerciseName = exercise.namePl,
            options = matches.map { match ->
                SubstituteUi(
                    exercise = match.exercise,
                    equipmentLabel = PlLabels.equipment(match.exercise.equipment),
                    warningLabels = match.warnings.map { "obciąża: ${PlLabels.joint(it.joint)}" },
                )
            },
        )
    }

    fun dismissSubstitutes() {
        substitutes.value = null
    }

    /**
     * Podmiana bieżącego ćwiczenia; [permanent] = true zapisuje ją też
     * w dokumencie planu (PlanRepository). Prefill liczony od nowa dla
     * zamiennika (jego własny stan progresji, ten sam kontekst tygodnia).
     */
    fun applySubstitute(substitute: Exercise, permanent: Boolean) {
        substitutes.value = null
        val session = manager.session.value ?: return
        val se = session.currentExercise ?: return
        val newPlanExercise = adaptPlanExercise(se.planExercise, substitute)
        val newState = statesById[substitute.id]
        val replacement = SessionExercise(
            exercise = substitute,
            planExercise = newPlanExercise,
            proposal = ProgressionEngine.proposeTargets(
                planExercise = newPlanExercise,
                state = newState,
                returningFromBreak = session.returningFromBreak,
                isCompoundLeg = ProgressionEngine.isCompoundLeg(substitute),
                weekIndexInBlock = session.weekIndexInBlock,
                blockLengthWeeks = session.fullBlockLengthWeeks,
            ),
            oldState = newState,
            planExerciseIndex = se.planExerciseIndex,
            substitutedFromId = se.exerciseId,
        )
        manager.mutate { it.substituteCurrent(replacement) }
        if (permanent) persistSubstitution(se.planExerciseIndex, newPlanExercise)
    }

    /** Podmiana na stałe: podmienia wpis w dokumencie planu (cały dokument, jak PlanRepository). */
    private fun persistSubstitution(
        planExerciseIndex: Int,
        newPlanExercise: com.stronk.data.PlanExercise,
    ) {
        if (planExerciseIndex < 0) return
        viewModelScope.launch {
            val plan = app.planRepository.observePlan(planId).first() ?: return@launch
            val day = plan.days.getOrNull(dayIndex) ?: return@launch
            if (planExerciseIndex >= day.exercises.size) return@launch
            val newDay = day.copy(
                exercises = day.exercises.toMutableList()
                    .also { it[planExerciseIndex] = newPlanExercise },
            )
            app.planRepository.save(
                plan.copy(days = plan.days.toMutableList().also { it[dayIndex] = newDay }),
            )
        }
    }

    // ------------------------------------------------------- koniec treningu

    /**
     * Zapis treningu: Workout (serie embedded) + ExerciseState per ćwiczenie
     * (updateStateAfterWorkout z TĄ SAMĄ proposal, z którą user wszedł) +
     * oznaczenie wpisu harmonogramu jako DONE. Zapisy są fire-and-forget
     * (offline-first) — sukces natychmiast, sync w tle.
     */
    fun finishWorkout() {
        val session = manager.session.value ?: return
        if (saving.value || finished.value) return
        if (!session.hasLoggedSets) {
            // Nic nie zalogowano — nie ma czego zapisywać; wyjście jak porzucenie.
            abandonWorkout()
            finished.value = true
            return
        }
        saving.value = true
        viewModelScope.launch {
            val finishedAt = System.currentTimeMillis()
            app.workoutRepository.save(
                Workout(
                    id = session.workoutId,
                    startedAt = session.startedAt,
                    finishedAt = finishedAt,
                    planId = session.planId,
                    dayIndex = session.dayIndex,
                    scheduleEntryId = session.scheduleEntryId,
                    sets = session.exercises.flatMap { it.loggedSets },
                ),
            )
            session.exercises.forEach { se ->
                if (se.workingLogged.isNotEmpty()) {
                    app.exerciseStateRepository.save(
                        ProgressionEngine.updateStateAfterWorkout(
                            oldState = se.oldState,
                            plannedTargets = se.proposal,
                            loggedSets = se.loggedSets,
                            updatedAtMillis = finishedAt,
                        ),
                    )
                }
            }
            session.scheduleEntryId?.let { entryId ->
                val entry = app.scheduleRepository.observeSchedule().first()
                    .firstOrNull { it.id == entryId }
                if (entry != null) {
                    app.scheduleRepository.save(
                        entry.copy(status = ScheduleStatus.DONE, workoutId = session.workoutId),
                    )
                }
            }
            RestTimerService.stop(app)
            manager.clear()
            saving.value = false
            finished.value = true
        }
    }

    /** Porzucenie treningu (decyzja z dialogu) — NIC nie zapisujemy. */
    fun abandonWorkout() {
        RestTimerService.stop(app)
        manager.clear()
    }

    // Celowo BEZ sprzątania w onCleared: sesja w singletonie ma przeżyć
    // ubicie aktywności (swipe z recents przy żywym foreground service) —
    // ponowne wejście w ten trening podłącza się do niej z powrotem.

    companion object {
        /** Fabryka z argumentami trasy — ręczna kompozycja z [StronkApplication]. */
        fun factory(
            planId: String,
            dayIndex: Int,
            scheduleEntryId: String?,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as StronkApplication
                WorkoutViewModel(app, planId, dayIndex, scheduleEntryId)
            }
        }
    }
}
