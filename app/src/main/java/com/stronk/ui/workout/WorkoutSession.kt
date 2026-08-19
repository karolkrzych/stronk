package com.stronk.ui.workout

import com.stronk.data.Exercise
import com.stronk.data.ExerciseState
import com.stronk.data.MeasurementType
import com.stronk.data.PlanExercise
import com.stronk.data.SetLog
import com.stronk.data.SetTarget
import com.stronk.data.TrainingGoal
import com.stronk.progression.Calibration
import com.stronk.progression.ExerciseProposal
import com.stronk.progression.ProgressionConstants
import kotlin.math.max
import kotlin.math.round

/**
 * Czysty model trwającej sesji treningowej (ADR-005) — zero Androida,
 * wszystkie przejścia to czyste funkcje (testowalne jednostkowo).
 * Stan żyje w [WorkoutSessionManager]; zapis do Firestore następuje dopiero
 * przy zakończeniu treningu (WorkoutViewModel).
 */

/**
 * Wynik kalibracji z serii testowej — pierwszej serii WEIGHT_REPS ćwiczenia,
 * które nie ma ani ciężaru startowego w planie, ani historii. Estymację 1RM
 * i ciężar roboczy liczy [Calibration]; [nextSetWeightKg] to gotowa wartość do
 * prefillu kolejnych serii TEGO treningu — przy powrocie po przerwie obniżona
 * o ramp-up dokładnie tak, jak zrobi to silnik w kolejnych treningach.
 */
data class CalibrationResult(
    /** Ciężar z serii testowej (surowa próba, nie ciężar roboczy). */
    val testWeightKg: Double,
    val testReps: Int,
    /** Epley na podstawie serii testowej. */
    val estimatedOneRepMaxKg: Double,
    /** Ciężar roboczy = % e1RM wg celu; ten trafia do planu jako startWeightKg. */
    val workingWeightKg: Double,
    /** Ciężar prefillowany w pozostałych seriach tego treningu. */
    val nextSetWeightKg: Double,
    /** Czy [nextSetWeightKg] jest obniżony ramp-upem po przerwie (ADR-004 reguła 4). */
    val isRampUp: Boolean,
) {
    /** Powtórzenia poza zakresem wiarygodności Epleya — UI ostrzega, nie blokuje. */
    val repsUnreliable: Boolean get() = testReps !in Calibration.RELIABLE_REPS
}

/** Jedno ćwiczenie w trwającej sesji treningowej. */
data class SessionExercise(
    /** Ćwiczenie z datasetu; null, gdy id z planu nie istnieje w bazie. */
    val exercise: Exercise?,
    /** Wpis planu, z którego liczono propozycję (po ewentualnej podmianie). */
    val planExercise: PlanExercise,
    /**
     * Propozycja silnika, z którą user wszedł w trening — ta sama instancja
     * MUSI trafić do [com.stronk.progression.ProgressionEngine.updateStateAfterWorkout]
     * (niesie weightKg i isLightWeek).
     */
    val proposal: ExerciseProposal,
    /** Stan progresji sprzed treningu (do updateState i "Ostatnio: …"). */
    val oldState: ExerciseState?,
    /** Pozycja oryginalnego wpisu w Plan.days[dayIndex].exercises — do podmiany na stałe. */
    val planExerciseIndex: Int,
    val loggedSets: List<SetLog> = emptyList(),
    /** Pominięte ręcznie albo porzucone przy podmianie w trakcie serii. */
    val skipped: Boolean = false,
    /** Id oryginału, gdy to ćwiczenie weszło jako zamiennik. */
    val substitutedFromId: String? = null,
    /** Wynik serii testowej; null, dopóki kalibracji nie było (albo jej nie potrzeba). */
    val calibration: CalibrationResult? = null,
) {
    val exerciseId: String get() = planExercise.exerciseId
    val name: String get() = exercise?.namePl ?: planExercise.exerciseId

    /** Zalogowane serie robocze (bez rozgrzewkowych). */
    val workingLogged: List<SetLog> get() = loggedSets.filterNot { it.isWarmup }

    /**
     * Czy następna seria jest SERIĄ TESTOWĄ: ćwiczenie na ciężar, którego apka
     * nie zna z żadnej strony (brak startWeightKg w planie i brak historii),
     * więc pierwsza seria służy do wyznaczenia ciężaru roboczego.
     */
    val needsCalibration: Boolean
        get() = calibration == null &&
            proposal.target is SetTarget.WeightReps &&
            planExercise.startWeightKg == null &&
            oldState == null &&
            workingLogged.isEmpty()

    /**
     * Seria, z której „kleją się" wartości prefillu kolejnych serii. Seria
     * testowa jest wyjątkiem: jej ciężar to próba, nie ciężar roboczy —
     * po kalibracji prefill bierze wynik kalibracji, nie to, co user dźwignął
     * w teście.
     */
    val prefillSource: SetLog?
        get() = (if (calibration != null) workingLogged.drop(1) else workingLogged).lastOrNull()

    val isComplete: Boolean get() = workingLogged.size >= proposal.sets

    /** Zrobione albo pominięte — flow treningu sam już tu nie wraca. */
    val isFinished: Boolean get() = isComplete || skipped

    /** Numer następnej serii roboczej (1-based). */
    val nextSetNumber: Int get() = workingLogged.size + 1
}

/** Cała trwająca sesja treningowa. */
data class WorkoutSession(
    val workoutId: String,
    val planId: String,
    val dayIndex: Int,
    val scheduleEntryId: String?,
    val planName: String,
    val dayName: String,
    val startedAt: Long,
    /** Kontekst progresji ustalony przy starcie sesji (spójny też dla podmian). */
    val weekIndexInBlock: Int,
    /** Pełna długość bloku = tygodnie pracy + tydzień lekki; null = plan bez bloku. */
    val fullBlockLengthWeeks: Int?,
    val returningFromBreak: Boolean,
    val exercises: List<SessionExercise>,
    /** Cel z profilu — wyznacza udział e1RM przy kalibracji z serii testowej. */
    val goal: TrainingGoal? = null,
    val currentExerciseIndex: Int = 0,
    /** Aktualna długość przerwy między seriami (edytowalna w UI). */
    val restSeconds: Int = WorkoutConstants.DEFAULT_REST_SECONDS,
    /** Koniec trwającej przerwy (epoch millis); null = przerwa nie biegnie. */
    val restEndsAtMillis: Long? = null,
) {
    /** Bieżące ćwiczenie; null, gdy wszystko zrobione/pominięte. */
    val currentExercise: SessionExercise?
        get() = exercises.getOrNull(currentExerciseIndex)?.takeUnless { it.isFinished }

    val allFinished: Boolean get() = exercises.all { it.isFinished }

    val completedSetCount: Int get() = exercises.sumOf { it.workingLogged.size }

    val totalSetCount: Int get() = exercises.sumOf { it.proposal.sets }

    val hasLoggedSets: Boolean get() = exercises.any { it.loggedSets.isNotEmpty() }

    /**
     * Czy ✓ jednym tapnięciem jest zablokowane: pierwsza seria WEIGHT_REPS bez
     * znanego ciężaru (brak startWeightKg i historii) wymaga wpisania kg —
     * inaczej jedno tapnięcie zalogowałoby 0 kg. To jest dokładnie moment
     * serii testowej ([SessionExercise.needsCalibration]).
     */
    val currentPrefillNeedsInput: Boolean
        get() {
            val se = currentExercise ?: return false
            return se.proposal.target is SetTarget.WeightReps &&
                se.proposal.weightKg == null &&
                se.calibration == null &&
                se.workingLogged.filterIsInstance<SetLog.WeightReps>().isEmpty()
        }

    /** Czy następna seria bieżącego ćwiczenia jest serią testową (kalibracja). */
    val currentIsCalibrationSet: Boolean get() = currentExercise?.needsCalibration == true

    /** Prefill następnej serii bieżącego ćwiczenia; null, gdy nie ma bieżącego. */
    fun prefillForCurrentSet(nowMillis: Long): SetLog? =
        currentExercise?.let { buildPrefill(it, workoutId, nowMillis) }

    /** Następne niedokończone ćwiczenie PO bieżącym (podgląd "następnie"). */
    fun nextUnfinishedExercise(): SessionExercise? =
        nextUnfinishedIndex(exercises, currentExerciseIndex)?.let { exercises[it] }

    /**
     * Zalogowanie serii bieżącego ćwiczenia: dokłada serię, po skompletowaniu
     * ćwiczenia przechodzi do następnego niedokończonego i startuje przerwę
     * (chyba że to była ostatnia seria całego treningu). Jeśli to była seria
     * testowa — z tej serii wyliczana jest kalibracja ciężaru roboczego.
     */
    fun logSet(set: SetLog, nowMillis: Long): WorkoutSession {
        val idx = currentExerciseIndex
        val se = exercises.getOrNull(idx) ?: return this
        if (se.isFinished) return this
        val updated = se.copy(
            loggedSets = se.loggedSets + set,
            calibration = se.calibration ?: calibrationFrom(se, set),
        )
        val list = exercises.toMutableList().also { it[idx] = updated }
        val newIdx = if (updated.isFinished) nextUnfinishedIndex(list, idx) ?: idx else idx
        val everythingDone = list.all { it.isFinished }
        return copy(
            exercises = list,
            currentExerciseIndex = newIdx,
            restEndsAtMillis = if (everythingDone) null else nowMillis + restSeconds * 1000L,
        )
    }

    /**
     * Zaliczenie serii z prefillem — wielki ✓ i akcja z powiadomienia.
     * No-op, gdy trzeba najpierw wpisać ciężar albo nie ma bieżącej serii.
     */
    fun completeCurrentSet(nowMillis: Long): WorkoutSession {
        if (currentPrefillNeedsInput) return this
        val prefill = prefillForCurrentSet(nowMillis) ?: return this
        return logSet(prefill, nowMillis)
    }

    /** Ręczny wybór ćwiczenia z listy (odblokowuje pominięte); ukończonych nie wybieramy. */
    fun selectExercise(index: Int): WorkoutSession {
        val se = exercises.getOrNull(index) ?: return this
        if (se.isComplete) return this
        val list =
            if (se.skipped) exercises.toMutableList().also { it[index] = se.copy(skipped = false) }
            else exercises
        return copy(exercises = list, currentExerciseIndex = index)
    }

    /** Pominięcie reszty bieżącego ćwiczenia i przejście dalej. */
    fun skipCurrentExercise(): WorkoutSession {
        val idx = currentExerciseIndex
        val se = exercises.getOrNull(idx) ?: return this
        if (se.isFinished) return this
        val list = exercises.toMutableList().also { it[idx] = se.copy(skipped = true) }
        return copy(exercises = list, currentExerciseIndex = nextUnfinishedIndex(list, idx) ?: idx)
    }

    /**
     * Podmiana bieżącego ćwiczenia na [replacement]. Serie już zrobione
     * zostają w logu (oryginał jest oznaczany jako pominięty, a zamiennik
     * wchodzi tuż za nim); bez zalogowanych serii — podmiana w miejscu.
     */
    fun substituteCurrent(replacement: SessionExercise): WorkoutSession {
        val idx = currentExerciseIndex
        val se = exercises.getOrNull(idx) ?: return this
        if (se.isFinished) return this
        return if (se.workingLogged.isEmpty()) {
            copy(exercises = exercises.toMutableList().also { it[idx] = replacement })
        } else {
            val list = exercises.toMutableList().also {
                it[idx] = se.copy(skipped = true)
                it.add(idx + 1, replacement)
            }
            copy(exercises = list, currentExerciseIndex = idx + 1)
        }
    }

    /** Zmiana domyślnej długości przerwy (z zaciśnięciem do sensownych granic). */
    fun withRestSeconds(seconds: Int): WorkoutSession = copy(
        restSeconds = seconds.coerceIn(
            WorkoutConstants.REST_MIN_SECONDS,
            WorkoutConstants.REST_MAX_SECONDS,
        ),
    )

    /** Przedłużenie biegnącej przerwy; no-op, gdy przerwa nie biegnie. */
    fun extendRest(deltaSeconds: Int): WorkoutSession =
        restEndsAtMillis?.let { copy(restEndsAtMillis = it + deltaSeconds * 1000L) } ?: this

    fun skipRest(): WorkoutSession = copy(restEndsAtMillis = null)

    /** Pozostałe sekundy przerwy (zaokrąglone w górę); null, gdy przerwa nie biegnie albo minęła. */
    fun restRemainingSeconds(nowMillis: Long): Int? {
        val end = restEndsAtMillis ?: return null
        val remaining = ((end - nowMillis + 999) / 1000).toInt()
        return remaining.takeIf { it > 0 }
    }

    /**
     * Kalibracja z właśnie zalogowanej serii — tylko gdy to była seria testowa
     * i realnie da się z niej coś policzyć (ciężar > 0, powtórzenia >= 1).
     * Poza tym null: zwykły flow 1-tap (ADR-005) nic tu nie zmienia.
     */
    private fun calibrationFrom(se: SessionExercise, set: SetLog): CalibrationResult? {
        if (!se.needsCalibration) return null
        if (set !is SetLog.WeightReps || set.isWarmup) return null
        if (set.kg <= 0.0 || set.reps < 1) return null
        val working = Calibration.workingWeightKg(set.kg, set.reps, goal)
        return CalibrationResult(
            testWeightKg = set.kg,
            testReps = set.reps,
            estimatedOneRepMaxKg = Calibration.estimateOneRepMax(set.kg, set.reps),
            workingWeightKg = working,
            nextSetWeightKg =
                if (returningFromBreak) {
                    roundWeightKg(working * ProgressionConstants.RAMP_UP_START_FACTOR)
                } else {
                    working
                },
            isRampUp = returningFromBreak,
        )
    }

    private fun nextUnfinishedIndex(list: List<SessionExercise>, from: Int): Int? {
        for (i in from + 1 until list.size) if (!list[i].isFinished) return i
        for (i in 0 until from) if (!list[i].isFinished) return i
        return null
    }
}

/**
 * Zaokrąglenie ciężaru identyczne z silnikiem progresji (najbliższe 2,5 kg,
 * nie mniej niż 2,5 kg) — prefill ramp-upu po kalibracji ma trafić w tę samą
 * wartość, którą silnik zaproponuje w kolejnym treningu.
 */
private fun roundWeightKg(kg: Double): Double = max(
    ProgressionConstants.WEIGHT_ROUNDING_KG,
    round(kg / ProgressionConstants.WEIGHT_ROUNDING_KG) * ProgressionConstants.WEIGHT_ROUNDING_KG,
)

/**
 * Prefill serii (ADR-005: jedno tapnięcie = seria wg planu): wartości
 * z ostatniej zalogowanej serii roboczej tego ćwiczenia w tej sesji
 * (edycja usera "klei się" do kolejnych serii), a bez niej — z kalibracji
 * (seria testowa) albo z propozycji silnika progresji.
 */
internal fun buildPrefill(se: SessionExercise, workoutId: String, nowMillis: Long): SetLog {
    val last = se.prefillSource
    val setNumber = se.nextSetNumber
    return when (val target = se.proposal.target) {
        is SetTarget.WeightReps -> {
            val lastWr = last as? SetLog.WeightReps
            SetLog.WeightReps(
                exerciseId = se.exerciseId, workoutId = workoutId, setNumber = setNumber,
                isWarmup = false, timestamp = nowMillis,
                kg = lastWr?.kg ?: se.calibration?.nextSetWeightKg ?: se.proposal.weightKg ?: 0.0,
                reps = lastWr?.reps ?: target.reps,
            )
        }

        is SetTarget.Reps -> {
            val lastR = last as? SetLog.Reps
            SetLog.Reps(
                exerciseId = se.exerciseId, workoutId = workoutId, setNumber = setNumber,
                isWarmup = false, timestamp = nowMillis,
                reps = lastR?.reps ?: target.reps,
                extraKg = lastR?.extraKg,
            )
        }

        is SetTarget.Time -> {
            val lastT = last as? SetLog.Time
            SetLog.Time(
                exerciseId = se.exerciseId, workoutId = workoutId, setNumber = setNumber,
                isWarmup = false, timestamp = nowMillis,
                seconds = lastT?.seconds ?: target.seconds,
            )
        }

        is SetTarget.DistanceTime -> {
            val lastD = last as? SetLog.DistanceTime
            SetLog.DistanceTime(
                exerciseId = se.exerciseId, workoutId = workoutId, setNumber = setNumber,
                isWarmup = false, timestamp = nowMillis,
                meters = lastD?.meters ?: target.meters,
                seconds = lastD?.seconds ?: target.seconds,
            )
        }
    }
}

/** Typ pomiaru odpowiadający celowi z planu. */
internal fun measurementTypeOfTarget(target: SetTarget): MeasurementType = when (target) {
    is SetTarget.WeightReps -> MeasurementType.WEIGHT_REPS
    is SetTarget.Reps -> MeasurementType.REPS
    is SetTarget.Time -> MeasurementType.TIME
    is SetTarget.DistanceTime -> MeasurementType.DISTANCE_TIME
}

/**
 * Wpis planu dla zamiennika: te same serie i progressionEnabled; cel przechodzi
 * 1:1, gdy typ pomiaru się zgadza, inaczej rozsądny default z [WorkoutConstants].
 * Ciężar startowy oryginału NIE przenosi się na inne ćwiczenie (pierwsza seria
 * WEIGHT_REPS bez historii poprosi o wpisanie kg).
 */
internal fun adaptPlanExercise(original: PlanExercise, substitute: Exercise): PlanExercise {
    val target =
        if (measurementTypeOfTarget(original.target) == substitute.measurementType) original.target
        else when (substitute.measurementType) {
            MeasurementType.WEIGHT_REPS ->
                SetTarget.WeightReps(WorkoutConstants.SUBSTITUTE_DEFAULT_REPS)
            MeasurementType.REPS ->
                SetTarget.Reps(WorkoutConstants.SUBSTITUTE_DEFAULT_REPS)
            MeasurementType.TIME ->
                SetTarget.Time(WorkoutConstants.SUBSTITUTE_DEFAULT_TIME_SECONDS)
            MeasurementType.DISTANCE_TIME -> SetTarget.DistanceTime(
                WorkoutConstants.SUBSTITUTE_DEFAULT_DISTANCE_METERS,
                WorkoutConstants.SUBSTITUTE_DEFAULT_DISTANCE_SECONDS,
            )
        }
    return PlanExercise(
        exerciseId = substitute.id,
        sets = original.sets,
        target = target,
        startWeightKg = null,
        progressionEnabled = original.progressionEnabled,
    )
}
