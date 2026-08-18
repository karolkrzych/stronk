package com.stronk.progression

import com.stronk.data.Exercise
import com.stronk.data.ExerciseState
import com.stronk.data.PlanExercise
import com.stronk.data.SetLog
import com.stronk.data.SetTarget
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/**
 * Silnik progresji (ADR-004) — czyste funkcje, zero Androida i zero Firestore.
 * Wejście: plan + zmaterializowany [ExerciseState]; wyjście: [ExerciseProposal]
 * albo nowy [ExerciseState]. Zapis to sprawa wołającego (repozytoria).
 *
 * Podział odpowiedzialności między funkcjami:
 * - [updateStateAfterWorkout] zapisuje w stanie RZECZYWISTOŚĆ (co user zrobił:
 *   lastSets, failStreak, faktycznie użyty ciężar w currentWeightKg),
 * - [proposeTargets] liczy z tego stanu propozycję na NASTĘPNY trening
 *   (progresja, deload reaktywny, tydzień lekki, ramp-up).
 *
 * Cztery reguły ADR-004:
 * 1. Overload: zaliczony trening → +2.5 kg (+5 kg compound-leg); REPS → +1
 *    powtórzenie; TIME/DISTANCE_TIME → +10% (dystans z czasem skalowane razem,
 *    czyli stałe tempo).
 * 2. Deload reaktywny: failStreak >= 2 → propozycja −10% i budowanie od nowa.
 * 3. Bloki: ostatni tydzień bloku jest lekki (−40%); tydzień lekki nie jest
 *    testem poziomu — stan progresji jest przez niego zamrożony, więc po nim
 *    progresja liczy się od stanu sprzed tygodnia lekkiego (nowy blok startuje
 *    wyżej niż poprzedni start).
 * 4. Ramp-up po przerwie: start ~55% poziomu z planu i podwojone przyrosty,
 *    aż propozycja dogoni poziom; potem zwykła reguła 1.
 */
object ProgressionEngine {

    private const val LIGHT_FACTOR = 1.0 - ProgressionConstants.LIGHT_WEEK_REDUCTION
    private const val DELOAD_FACTOR = 1.0 - ProgressionConstants.REACTIVE_DELOAD_REDUCTION

    // ---------------------------------------------------------------- bloki

    /**
     * Pozycja tygodnia w bloku, 0-based: 0 = pierwszy tydzień pracy,
     * [blockLengthWeeks] − 1 = tydzień lekki. Tygodnie liczone jako pełne
     * 7-dniowe okna od [blockStartMillis] (np. createdAt planu), modulo długość
     * bloku — po tygodniu lekkim automatycznie zaczyna się nowy blok od zera.
     */
    fun weekIndexInBlock(blockStartMillis: Long, nowMillis: Long, blockLengthWeeks: Int): Int {
        if (blockLengthWeeks <= 0) return 0
        val elapsed = nowMillis - blockStartMillis
        if (elapsed < 0) return 0
        val week = elapsed / ProgressionConstants.WEEK_MILLIS
        return (week % blockLengthWeeks).toInt()
    }

    /** Czy dany tydzień bloku jest tygodniem lekkim (ostatni tydzień bloku). */
    fun isLightWeek(weekIndexInBlock: Int, blockLengthWeeks: Int): Boolean =
        blockLengthWeeks >= 2 && weekIndexInBlock == blockLengthWeeks - 1

    // ------------------------------------------------------- heurystyka nóg

    /**
     * Czy ćwiczenie kwalifikuje się na przyrost +5 kg: wielostawowe
     * (mechanic=compound) z partią główną z nóg.
     */
    fun isCompoundLeg(exercise: Exercise): Boolean =
        exercise.mechanic.equals(ProgressionConstants.COMPOUND_MECHANIC, ignoreCase = true) &&
            exercise.primaryMuscles.any { it in ProgressionConstants.LEG_MUSCLES }

    // ----------------------------------------------------------- propozycja

    /**
     * Propozycja na następny trening dla ćwiczenia z planu.
     *
     * @param planExercise wpis z planu (target, sets, startWeightKg, progressionEnabled)
     * @param state zmaterializowany stan progresji albo null (brak historii)
     * @param returningFromBreak flaga ramp-upu z profilu (ADR-004 reguła 4)
     * @param isCompoundLeg wynik [isCompoundLeg] dla ćwiczenia (+5 kg zamiast +2.5)
     * @param weekIndexInBlock pozycja tygodnia w bloku ([weekIndexInBlock]), 0-based
     * @param blockLengthWeeks pełna długość bloku (praca + tydzień lekki)
     */
    fun proposeTargets(
        planExercise: PlanExercise,
        state: ExerciseState?,
        returningFromBreak: Boolean,
        isCompoundLeg: Boolean,
        weekIndexInBlock: Int,
        blockLengthWeeks: Int,
    ): ExerciseProposal {
        // Ćwiczenie wyłączone spod progresji → plan 1:1, bez żadnych modyfikatorów
        if (!planExercise.progressionEnabled) {
            return ExerciseProposal(
                exerciseId = planExercise.exerciseId,
                sets = planExercise.sets,
                target = planExercise.target,
                weightKg = if (planExercise.target is SetTarget.WeightReps) planExercise.startWeightKg else null,
            )
        }

        val raw = when (val target = planExercise.target) {
            is SetTarget.WeightReps -> proposeWeightReps(planExercise, target, state, returningFromBreak, isCompoundLeg)
            is SetTarget.Reps -> proposeReps(target, state, returningFromBreak)
            is SetTarget.Time -> proposeTime(target, state, returningFromBreak)
            is SetTarget.DistanceTime -> proposeDistanceTime(target, state, returningFromBreak)
        }

        val lightWeek = isLightWeek(weekIndexInBlock, blockLengthWeeks)
        val (finalTarget, finalWeight) =
            if (lightWeek) applyLightWeek(raw.target, raw.weightKg) else raw.target to raw.weightKg

        return ExerciseProposal(
            exerciseId = planExercise.exerciseId,
            sets = planExercise.sets,
            target = finalTarget,
            weightKg = finalWeight,
            isLightWeek = lightWeek,
            isReactiveDeload = raw.isReactiveDeload,
            isRampUp = raw.isRampUp,
        )
    }

    /** Wynik pośredni propozycji przed modyfikatorem tygodnia lekkiego. */
    private data class RawProposal(
        val target: SetTarget,
        val weightKg: Double? = null,
        val isReactiveDeload: Boolean = false,
        val isRampUp: Boolean = false,
    )

    // WEIGHT_REPS: progresja idzie ciężarem, cel powtórzeń zostaje z planu
    private fun proposeWeightReps(
        plan: PlanExercise,
        target: SetTarget.WeightReps,
        state: ExerciseState?,
        returningFromBreak: Boolean,
        isCompoundLeg: Boolean,
    ): RawProposal {
        val start = plan.startWeightKg
        val cur = state?.currentWeightKg
        val increment =
            if (isCompoundLeg) ProgressionConstants.WEIGHT_INCREMENT_COMPOUND_LEG_KG
            else ProgressionConstants.WEIGHT_INCREMENT_KG

        if (state == null || cur == null) {
            // Pierwszy trening: ciężar startowy z planu, ewentualnie ramp-up od ~55%
            return if (returningFromBreak && start != null) {
                RawProposal(target, roundWeight(start * ProgressionConstants.RAMP_UP_START_FACTOR), isRampUp = true)
            } else {
                RawProposal(target, start)
            }
        }
        if (state.failStreak >= ProgressionConstants.REACTIVE_DELOAD_FAIL_STREAK) {
            return RawProposal(target, roundWeight(cur * DELOAD_FACTOR), isReactiveDeload = true)
        }
        if (state.failStreak > 0) {
            // Jedna porażka → powtórka tego samego ciężaru
            return RawProposal(target, cur)
        }
        if (workingSets(state).isEmpty()) {
            // Stan bez zaliczonego treningu (np. świeżo zainicjalizowany) → bez progresji
            return RawProposal(target, cur)
        }
        // Ostatni trening zaliczony → progresja
        return if (returningFromBreak && start != null && cur < start - ProgressionConstants.EPSILON) {
            val accelerated = roundWeight(cur + increment * ProgressionConstants.RAMP_UP_INCREMENT_MULTIPLIER)
            RawProposal(target, min(accelerated, start), isRampUp = true)
        } else {
            RawProposal(target, roundWeight(cur + increment))
        }
    }

    // REPS: progresja idzie liczbą powtórzeń; poziom czytamy z lastSets
    // (sukces → minimum z serii roboczych, porażka → najlepsza seria)
    private fun proposeReps(
        target: SetTarget.Reps,
        state: ExerciseState?,
        returningFromBreak: Boolean,
    ): RawProposal {
        val planReps = target.reps
        val working = workingSets(state).filterIsInstance<SetLog.Reps>()

        if (state == null || working.isEmpty()) {
            return if (returningFromBreak) {
                RawProposal(target.copy(reps = roundReps(planReps * ProgressionConstants.RAMP_UP_START_FACTOR)), isRampUp = true)
            } else {
                RawProposal(target)
            }
        }
        if (state.failStreak >= ProgressionConstants.REACTIVE_DELOAD_FAIL_STREAK) {
            val attempted = working.maxOf { it.reps }
            return RawProposal(target.copy(reps = roundReps(attempted * DELOAD_FACTOR)), isReactiveDeload = true)
        }
        if (state.failStreak > 0) {
            // Powtórka: najlepsza seria przybliża próbowany cel, plan jest podłogą
            return RawProposal(target.copy(reps = max(planReps, working.maxOf { it.reps })))
        }
        val achieved = working.minOf { it.reps }
        return if (returningFromBreak && achieved < planReps) {
            val accelerated = achieved + ProgressionConstants.REPS_INCREMENT * ProgressionConstants.RAMP_UP_INCREMENT_MULTIPLIER
            RawProposal(target.copy(reps = min(accelerated, planReps)), isRampUp = true)
        } else {
            RawProposal(target.copy(reps = achieved + ProgressionConstants.REPS_INCREMENT))
        }
    }

    // TIME: progresja czasem, +10% (min +5 s), zaokrąglenie do 5 s
    private fun proposeTime(
        target: SetTarget.Time,
        state: ExerciseState?,
        returningFromBreak: Boolean,
    ): RawProposal {
        val planSeconds = target.seconds
        val working = workingSets(state).filterIsInstance<SetLog.Time>()

        if (state == null || working.isEmpty()) {
            return if (returningFromBreak) {
                RawProposal(target.copy(seconds = roundSeconds(planSeconds * ProgressionConstants.RAMP_UP_START_FACTOR)), isRampUp = true)
            } else {
                RawProposal(target)
            }
        }
        if (state.failStreak >= ProgressionConstants.REACTIVE_DELOAD_FAIL_STREAK) {
            val attempted = working.maxOf { it.seconds }
            return RawProposal(target.copy(seconds = roundSeconds(attempted * DELOAD_FACTOR)), isReactiveDeload = true)
        }
        if (state.failStreak > 0) {
            return RawProposal(target.copy(seconds = max(planSeconds, working.maxOf { it.seconds })))
        }
        val achieved = working.minOf { it.seconds }
        return if (returningFromBreak && achieved < planSeconds) {
            RawProposal(
                target.copy(seconds = min(progressTime(achieved, ProgressionConstants.RAMP_UP_INCREMENT_MULTIPLIER), planSeconds)),
                isRampUp = true,
            )
        } else {
            RawProposal(target.copy(seconds = progressTime(achieved, 1)))
        }
    }

    // DISTANCE_TIME: dystans i czas skalowane razem (stałe tempo), +10%
    private fun proposeDistanceTime(
        target: SetTarget.DistanceTime,
        state: ExerciseState?,
        returningFromBreak: Boolean,
    ): RawProposal {
        val working = workingSets(state).filterIsInstance<SetLog.DistanceTime>()

        if (state == null || working.isEmpty()) {
            return if (returningFromBreak) {
                RawProposal(
                    scaleDistanceTime(target.meters, target.seconds, ProgressionConstants.RAMP_UP_START_FACTOR),
                    isRampUp = true,
                )
            } else {
                RawProposal(target)
            }
        }
        if (state.failStreak >= ProgressionConstants.REACTIVE_DELOAD_FAIL_STREAK) {
            val attempted = working.maxBy { it.meters }
            return RawProposal(scaleDistanceTime(attempted.meters, attempted.seconds, DELOAD_FACTOR), isReactiveDeload = true)
        }
        if (state.failStreak > 0) {
            val best = working.maxBy { it.meters }
            // Powtórka: najlepsza próba, o ile nie spadła poniżej celu z planu
            return if (best.meters >= target.meters - ProgressionConstants.EPSILON) {
                RawProposal(scaleDistanceTime(best.meters, best.seconds, 1.0))
            } else {
                RawProposal(target)
            }
        }
        val achieved = working.minBy { it.meters }
        return if (returningFromBreak && achieved.meters < target.meters - ProgressionConstants.EPSILON) {
            val factor = 1.0 + ProgressionConstants.DISTANCE_INCREMENT_FACTOR * ProgressionConstants.RAMP_UP_INCREMENT_MULTIPLIER
            val scaled = scaleDistanceTime(achieved.meters, achieved.seconds, factor)
            // Dogonienie poziomu: nie proponujemy więcej niż cel z planu
            if (scaled.meters >= target.meters) RawProposal(target, isRampUp = true)
            else RawProposal(scaled, isRampUp = true)
        } else {
            RawProposal(scaleDistanceTime(achieved.meters, achieved.seconds, 1.0 + ProgressionConstants.DISTANCE_INCREMENT_FACTOR))
        }
    }

    // -------------------------------------------------- stan po treningu

    /**
     * Nowy [ExerciseState] po zalogowanym treningu. Czyste przekształcenie —
     * zapis do Firestore robi wołający.
     *
     * Definicja "zaliczone": pierwsze [ExerciseProposal.sets] serii roboczych
     * (bez rozgrzewkowych, po setNumber) istnieje i KAŻDA osiągnęła cel
     * z propozycji; dla WEIGHT_REPS dodatkowo ciężar >= proponowany. Nadmiarowe
     * serie ponad plan nie psują wyniku.
     *
     * Zasady szczególne:
     * - brak serii roboczych → stan bez zmian merytorycznych (tylko updatedAt),
     * - tydzień lekki nie jest testem poziomu → stan zostaje ZAMROŻONY w całości
     *   (też lastSets — dla typów bez ciężaru poziom progresji czytany jest
     *   z lastSets i wynik z −40% by go zatruł); aktualizuje się tylko updatedAt,
     * - currentWeightKg = faktycznie użyty ciężar (max kg z serii roboczych) —
     *   stan opisuje rzeczywistość, progresję dolicza [proposeTargets].
     *
     * @param plannedTargets propozycja, z którą user wszedł w trening
     * @param loggedSets zalogowane serie TEGO ćwiczenia z tego treningu
     * @param updatedAtMillis znacznik czasu zapisu (epoch millis)
     */
    fun updateStateAfterWorkout(
        oldState: ExerciseState?,
        plannedTargets: ExerciseProposal,
        loggedSets: List<SetLog>,
        updatedAtMillis: Long,
    ): ExerciseState {
        val exerciseId = oldState?.exerciseId ?: plannedTargets.exerciseId
        val working = loggedSets.filterNot { it.isWarmup }

        if (working.isEmpty() || plannedTargets.isLightWeek) {
            return oldState?.copy(updatedAt = updatedAtMillis)
                ?: ExerciseState(exerciseId = exerciseId, updatedAt = updatedAtMillis)
        }

        val success = isWorkoutSuccessful(plannedTargets, working)
        val usedWeightKg = working.filterIsInstance<SetLog.WeightReps>().maxOfOrNull { it.kg }
        val newWeightKg = when {
            plannedTargets.target !is SetTarget.WeightReps -> oldState?.currentWeightKg
            usedWeightKg != null -> usedWeightKg
            else -> oldState?.currentWeightKg ?: plannedTargets.weightKg
        }
        return ExerciseState(
            exerciseId = exerciseId,
            lastSets = loggedSets,
            failStreak = if (success) 0 else (oldState?.failStreak ?: 0) + 1,
            currentWeightKg = newWeightKg,
            updatedAt = updatedAtMillis,
        )
    }

    /** Czy trening spełnił propozycję (definicja "zaliczone" — patrz [updateStateAfterWorkout]). */
    fun isWorkoutSuccessful(plannedTargets: ExerciseProposal, workingSets: List<SetLog>): Boolean {
        val evaluated = workingSets.sortedBy { it.setNumber }.take(plannedTargets.sets)
        if (evaluated.size < plannedTargets.sets) return false
        return evaluated.all { setMeetsTarget(it, plannedTargets.target, plannedTargets.weightKg) }
    }

    private fun setMeetsTarget(log: SetLog, target: SetTarget, proposedWeightKg: Double?): Boolean = when (target) {
        is SetTarget.WeightReps ->
            log is SetLog.WeightReps && log.reps >= target.reps &&
                (proposedWeightKg == null || log.kg >= proposedWeightKg - ProgressionConstants.EPSILON)
        is SetTarget.Reps ->
            log is SetLog.Reps && log.reps >= target.reps
        is SetTarget.Time ->
            log is SetLog.Time && log.seconds >= target.seconds
        is SetTarget.DistanceTime ->
            log is SetLog.DistanceTime &&
                log.meters >= target.meters - ProgressionConstants.EPSILON &&
                // tempo nie gorsze niż celowe: meters/seconds >= target.meters/target.seconds
                log.meters * target.seconds >= target.meters * log.seconds - ProgressionConstants.EPSILON
    }

    // ------------------------------------------------------------ helpery

    private fun workingSets(state: ExerciseState?): List<SetLog> =
        state?.lastSets.orEmpty().filterNot { it.isWarmup }

    /** Tydzień lekki: −40% miary progresu; cel powtórzeń WEIGHT_REPS zostaje z planu. */
    private fun applyLightWeek(target: SetTarget, weightKg: Double?): Pair<SetTarget, Double?> = when (target) {
        is SetTarget.WeightReps -> target to weightKg?.let { roundWeight(it * LIGHT_FACTOR) }
        is SetTarget.Reps -> target.copy(reps = roundReps(target.reps * LIGHT_FACTOR)) to null
        is SetTarget.Time -> target.copy(seconds = roundSeconds(target.seconds * LIGHT_FACTOR)) to null
        is SetTarget.DistanceTime -> scaleDistanceTime(target.meters, target.seconds, LIGHT_FACTOR) to null
    }

    /** +10% czasu zaokrąglone do 5 s, ale nie mniej niż +5 s ([multiplier]=2 w ramp-upie). */
    private fun progressTime(currentSeconds: Int, multiplier: Int): Int {
        val scaled = roundSeconds(currentSeconds * (1.0 + ProgressionConstants.TIME_INCREMENT_FACTOR * multiplier))
        return max(scaled, currentSeconds + ProgressionConstants.TIME_MIN_INCREMENT_SECONDS * multiplier)
    }

    private fun scaleDistanceTime(meters: Double, seconds: Int, factor: Double): SetTarget.DistanceTime =
        SetTarget.DistanceTime(
            meters = roundMeters(meters * factor),
            seconds = roundSeconds(seconds * factor),
        )

    private fun roundWeight(kg: Double): Double =
        max(
            ProgressionConstants.WEIGHT_ROUNDING_KG,
            round(kg / ProgressionConstants.WEIGHT_ROUNDING_KG) * ProgressionConstants.WEIGHT_ROUNDING_KG,
        )

    private fun roundReps(reps: Double): Int =
        max(ProgressionConstants.REPS_MIN, round(reps).toInt())

    private fun roundSeconds(seconds: Double): Int =
        max(
            ProgressionConstants.TIME_MIN_SECONDS,
            (round(seconds / ProgressionConstants.TIME_ROUNDING_SECONDS) * ProgressionConstants.TIME_ROUNDING_SECONDS).toInt(),
        )

    private fun roundMeters(meters: Double): Double =
        max(
            ProgressionConstants.DISTANCE_MIN_METERS,
            round(meters / ProgressionConstants.DISTANCE_ROUNDING_METERS) * ProgressionConstants.DISTANCE_ROUNDING_METERS,
        )
}
