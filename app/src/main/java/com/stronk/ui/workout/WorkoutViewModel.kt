package com.stronk.ui.workout

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stronk.StronkApplication
import com.stronk.data.Exercise
import com.stronk.data.ExerciseState
import com.stronk.data.GoalDefaults
import com.stronk.data.ProfileDetails
import com.stronk.data.ScheduleStatus
import com.stronk.data.SetLog
import com.stronk.data.SetTarget
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
    /** Klucz partii z datasetu (np. "lats") — pod ikony/podpisy z MuscleIcons. */
    val muscle: String?,
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
    /** Wszystkie obrazki ćwiczenia (start/koniec) — podgląd w bottom sheetach. */
    val images: List<String>,
    val instructions: List<String>,
)

/**
 * Wynik kalibracji gotowy do pokazania — liczby osobno od jednostek, żeby
 * ekran mógł je podać jako duże wartości w kafelkach, a nie jako zdanie.
 */
data class CalibrationUi(
    /** Sama liczba, np. "53" (jednostkę dokłada komponent). */
    val oneRepMaxValue: String,
    /** Sama liczba, np. "35". */
    val workingWeightValue: String,
    /** Jednolinijkowa wersja: "Szac. 1RM 53 kg → ciężar roboczy 35 kg". */
    val summary: String,
    /** "z serii testowej: 40 kg × 10". */
    val testLabel: String,
    /** Ciężar w tym treningu obniżony ramp-upem (powrót po przerwie). */
    val isRampUp: Boolean,
    /** Uwaga o powtórzeniach poza zakresem wiarygodności; null = test był w normie. */
    val unreliableNote: String?,
)

/** Bieżąca seria pod wielki ✓ (ADR-005). */
data class CurrentSetUi(
    val exerciseIndex: Int,
    val exerciseId: String,
    val exerciseName: String,
    /** Klucz partii z datasetu — pod ikonę/podpis grupy przy nazwie. */
    val muscle: String?,
    val setNumber: Int,
    val totalSets: Int,
    /** Gotowa seria do zalogowania jednym tapnięciem (i baza do dialogu edycji). */
    val prefill: SetLog,
    val prefillLabel: String,
    /** true → pierwsza seria WEIGHT_REPS bez znanego ciężaru: ✓ otwiera edycję. */
    val needsInput: Boolean,
    /** true → ta seria jest SERIĄ TESTOWĄ: z niej wyliczymy ciężar roboczy. */
    val isCalibrationSet: Boolean,
    /** Wynik kalibracji pokazywany tuż po serii testowej; null poza tym momentem. */
    val calibration: CalibrationUi?,
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

/**
 * Inny trening z zalogowanymi seriami wciąż trwa w [WorkoutSessionManager] —
 * decyzja (zapisz / porzuć / wróć) należy do usera, nic nie kasujemy po cichu.
 */
data class SessionConflictUi(
    val planName: String,
    val dayName: String,
    val loggedSetCount: Int,
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
    /** Niepusty = dialog "masz trening w toku" zamiast budowy nowej sesji. */
    val sessionConflict: SessionConflictUi? = null,
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
    private val sessionConflict = MutableStateFlow<SessionConflictUi?>(null)

    // Dane pomocnicze załadowane raz przy starcie — pod prefille i zamienniki.
    private var allExercises: List<Exercise> = emptyList()
    private var profileDetails: ProfileDetails = ProfileDetails()
    private var statesById: Map<String, ExerciseState> = emptyMap()

    /** Ćwiczenia, których ciężar roboczy z kalibracji trafił już do planu. */
    private val calibrationsPersisted = mutableSetOf<String>()

    private data class Snapshot(
        val session: WorkoutSession?,
        val error: String?,
        val saving: Boolean,
        val finished: Boolean,
        val substitutes: SubstitutesState?,
        val conflict: SessionConflictUi? = null,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<WorkoutUiState> = combine(
        combine(
            manager.session, error, saving, finished, substitutes,
        ) { session, err, sav, fin, subs ->
            Snapshot(session, err, sav, fin, subs)
        },
        sessionConflict,
    ) { snap, conflict ->
        snap.copy(conflict = conflict)
    }.flatMapLatest { snap ->
        // Ticker tylko w trakcie przerwy — poza nią stan jest statyczny
        // (przy konflikcie też: dialog nie pokazuje zegara starej sesji).
        if (snap.session?.restEndsAtMillis != null && !snap.finished && snap.conflict == null) {
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
                val existing = manager.session.value
                if (existing != null && existing.hasLoggedSets) {
                    // Trwa inny trening z zalogowanymi seriami — nie kasujemy go
                    // po cichu; decyzję (zapisz / porzuć / wróć) podejmuje user.
                    sessionConflict.value = SessionConflictUi(
                        planName = existing.planName,
                        dayName = existing.dayName,
                        loggedSetCount = existing.completedSetCount,
                    )
                    return@launch
                }
                manager.clear()
                buildSession()
            }
            // Serwis timera działa przez cały trening (notyfikacja + ✓ z lock screena).
            ensureRestTimerService()
        }
    }

    /**
     * Start foreground serwisu timera — bezpiecznie: na API 31+ start z tła
     * rzuca ForegroundServiceStartNotAllowedException (np. user zgasił ekran,
     * zanim init skończył ładować dane), więc łapiemy i tylko logujemy.
     * Ekran ponawia start przy każdym ON_RESUME, więc timer nie zostaje
     * bez serwisu na dłużej niż do powrotu apki na wierzch.
     */
    fun ensureRestTimerService() {
        if (manager.session.value == null) return
        runCatching { RestTimerService.start(app) }
            .onFailure { Log.w(TAG, "Start serwisu timera nieudany (apka w tle?)", it) }
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
                goal = profileDetails.goal,
                // Domyślna przerwa zależna od celu z profilu (siła 180 s / masa 90 s /
                // powrót 75 s); ręczna zmiana w treningu nadal działa.
                restSeconds = GoalDefaults.restSecondsFor(profileDetails.goal),
            ),
        )
    }

    private fun buildUiState(snap: Snapshot, now: Long): WorkoutUiState {
        if (snap.finished) return WorkoutUiState(loading = false, finished = true)
        snap.error?.let { return WorkoutUiState(loading = false, error = it) }
        // Konflikt przed mapowaniem sesji — sesja w managerze należy wtedy
        // do INNEGO treningu i nie wolno jej pokazać na tym ekranie.
        snap.conflict?.let {
            return WorkoutUiState(loading = false, sessionConflict = it, saving = snap.saving)
        }
        val session = snap.session ?: return WorkoutUiState(loading = true)

        val rows = session.exercises.mapIndexed { index, se ->
            WorkoutExerciseUi(
                index = index,
                exerciseId = se.exerciseId,
                name = se.name,
                muscle = se.exercise?.primaryMuscles?.firstOrNull(),
                muscleLabel = se.exercise?.primaryMuscles?.firstOrNull()
                    ?.let(PlLabels::muscle).orEmpty(),
                targetLabel = WorkoutLabels.proposalTarget(se.proposal),
                lastLabel = WorkoutLabels.lastTime(se.oldState),
                badges = badgesOf(se),
                doneSets = se.workingLogged.size.coerceAtMost(se.proposal.sets),
                totalSets = se.proposal.sets,
                isCurrent = index == session.currentExerciseIndex && !se.isFinished,
                isComplete = se.isComplete,
                skipped = se.skipped && !se.isComplete,
                substituted = se.substitutedFromId != null,
                imagePath = se.exercise?.images?.firstOrNull(),
                images = se.exercise?.images.orEmpty(),
                instructions = se.exercise?.instructionsPl.orEmpty(),
            )
        }
        val currentSe = session.currentExercise
        val currentUi = currentSe?.let { se ->
            val prefill = buildPrefill(se, session.workoutId, now)
            CurrentSetUi(
                exerciseIndex = session.currentExerciseIndex,
                exerciseId = se.exerciseId,
                exerciseName = se.name,
                muscle = se.exercise?.primaryMuscles?.firstOrNull(),
                setNumber = se.nextSetNumber,
                totalSets = se.proposal.sets,
                prefill = prefill,
                prefillLabel = WorkoutLabels.setValue(prefill),
                needsInput = session.currentPrefillNeedsInput,
                isCalibrationSet = session.currentIsCalibrationSet,
                // Wynik kalibracji pokazujemy w momencie, w którym coś znaczy:
                // przy pierwszej serii PO teście (dalej ciężar mówi już sam za siebie).
                calibration = se.calibration
                    ?.takeIf { se.workingLogged.size == 1 }
                    ?.let(::calibrationUi),
                lastLabel = WorkoutLabels.lastTime(se.oldState),
                badges = badgesOf(se),
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

    /** Plakietki ćwiczenia: modyfikatory silnika + ślad po kalibracji. */
    private fun badgesOf(se: SessionExercise): List<String> =
        WorkoutLabels.proposalBadges(se.proposal) + WorkoutLabels.calibrationBadges(se.calibration)

    private fun calibrationUi(c: CalibrationResult) = CalibrationUi(
        oneRepMaxValue = WorkoutLabels.kg(c.estimatedOneRepMaxKg),
        workingWeightValue = WorkoutLabels.kg(c.workingWeightKg),
        summary = WorkoutLabels.calibrationSummary(c),
        testLabel = WorkoutLabels.calibrationTest(c),
        isRampUp = c.isRampUp,
        unreliableNote = WorkoutLabels.calibrationRepsNote(c.testReps),
    )

    // ------------------------------------------------------------ akcje serii

    /** Wielki ✓ — seria zaliczona z prefillem. */
    fun completeCurrentSet() {
        manager.completeCurrentSet()
        persistCalibrations()
    }

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
        persistCalibrations()
    }

    /**
     * Ciężar roboczy z serii testowej trafia do PLANU: wszystkie wpisy tego
     * ćwiczenia, które nie mają jeszcze ciężaru startowego, dostają go na stałe,
     * żeby kolejne treningi startowały z konkretnej liczby (i żeby silnik miał
     * od czego liczyć ramp-up). Ręcznie ustawionych wartości NIE ruszamy.
     * Zapis fire-and-forget (ADR-002) — sync w tle.
     */
    private fun persistCalibrations() {
        val session = manager.session.value ?: return
        // Znacznik stawiamy od razu: kalibracja per ćwiczenie idzie do planu
        // dokładnie raz na sesję, nawet jeśli user zaloguje kolejne serie szybciej,
        // niż wróci odczyt planu.
        val pending = session.exercises
            .mapNotNull { se -> se.calibration?.let { se.exerciseId to it.workingWeightKg } }
            .filter { (exerciseId, _) -> calibrationsPersisted.add(exerciseId) }
        if (pending.isEmpty()) return
        viewModelScope.launch {
            val plan = app.planRepository.observePlan(planId).first() ?: return@launch
            var changed = false
            val days = plan.days.map { day ->
                day.copy(
                    exercises = day.exercises.map { pe ->
                        val weight = pending.firstOrNull { it.first == pe.exerciseId }?.second
                        if (weight != null &&
                            pe.startWeightKg == null &&
                            pe.target is SetTarget.WeightReps
                        ) {
                            changed = true
                            pe.copy(startWeightKg = weight)
                        } else {
                            pe
                        }
                    },
                )
            }
            if (changed) app.planRepository.save(plan.copy(days = days))
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
            persistWorkout(session, System.currentTimeMillis())
            RestTimerService.stop(app)
            manager.clear()
            saving.value = false
            finished.value = true
        }
    }

    /**
     * Zapis sesji do Firestore: Workout (serie embedded) + ExerciseState per
     * ćwiczenie + oznaczenie wpisu harmonogramu jako DONE. Fire-and-forget
     * (offline-first). Wspólne dla [finishWorkout] i zapisu starej sesji
     * z dialogu konfliktu ([resolveConflictSaveOld]).
     */
    private suspend fun persistWorkout(session: WorkoutSession, finishedAt: Long) {
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
            val setsForState = setsForProgression(se)
            if (setsForState.any { !it.isWarmup }) {
                app.exerciseStateRepository.save(
                    ProgressionEngine.updateStateAfterWorkout(
                        oldState = se.oldState,
                        plannedTargets = se.proposal,
                        loggedSets = setsForState,
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
    }

    /**
     * Serie, z których liczymy stan progresji. Seria testowa jest POMIAREM,
     * nie serią roboczą na wyliczonym ciężarze — gdyby weszła do stanu, jej
     * surowy ciężar podszyłby się pod poziom ćwiczenia (`currentWeightKg`)
     * i silnik startowałby w kolejnym treningu od próby, a nie od ciężaru
     * roboczego z kalibracji. W dokumencie treningu zostaje bez zmian.
     */
    private fun setsForProgression(se: SessionExercise): List<SetLog> {
        val testSet = se.calibration?.let { se.workingLogged.firstOrNull() } ?: return se.loggedSets
        return se.loggedSets.filterNot { it === testSet }
    }

    // ------------------------------------------------- konflikt trwającej sesji

    /** Decyzja z dialogu konfliktu: zapisz tamten trening, potem zacznij ten. */
    fun resolveConflictSaveOld() {
        if (saving.value) return
        saving.value = true
        viewModelScope.launch {
            manager.session.value?.let { old ->
                persistWorkout(old, System.currentTimeMillis())
            }
            startFreshAfterConflict()
        }
    }

    /** Decyzja z dialogu konfliktu: porzuć tamten trening bez zapisu. */
    fun resolveConflictDiscardOld() {
        if (saving.value) return
        saving.value = true
        viewModelScope.launch { startFreshAfterConflict() }
    }

    private suspend fun startFreshAfterConflict() {
        manager.clear()
        buildSession()
        // Konflikt gaśnie dopiero po zbudowaniu nowej sesji — inaczej ekran
        // mignąłby stanem starego treningu.
        sessionConflict.value = null
        saving.value = false
        ensureRestTimerService()
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
        private const val TAG = "WorkoutViewModel"

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
