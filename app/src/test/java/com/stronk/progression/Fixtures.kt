package com.stronk.progression

import com.stronk.data.Exercise
import com.stronk.data.ExerciseState
import com.stronk.data.JointStress
import com.stronk.data.MeasurementType
import com.stronk.data.PlanExercise
import com.stronk.data.SetLog
import com.stronk.data.SetTarget
import com.stronk.data.StressLevel

// Wspólne buildery danych testowych silnika progresji.

internal fun planWeightReps(
    exerciseId: String = "Barbell_Bench_Press",
    sets: Int = 3,
    reps: Int = 8,
    startWeightKg: Double? = 60.0,
    progressionEnabled: Boolean = true,
) = PlanExercise(
    exerciseId = exerciseId,
    sets = sets,
    target = SetTarget.WeightReps(reps),
    startWeightKg = startWeightKg,
    progressionEnabled = progressionEnabled,
)

internal fun planReps(
    reps: Int = 10,
    sets: Int = 3,
    exerciseId: String = "Pullups",
) = PlanExercise(exerciseId = exerciseId, sets = sets, target = SetTarget.Reps(reps))

internal fun planTime(
    seconds: Int = 60,
    sets: Int = 3,
    exerciseId: String = "Plank",
) = PlanExercise(exerciseId = exerciseId, sets = sets, target = SetTarget.Time(seconds))

internal fun planDistanceTime(
    meters: Double = 1000.0,
    seconds: Int = 300,
    sets: Int = 1,
    exerciseId: String = "Running_Treadmill",
) = PlanExercise(exerciseId = exerciseId, sets = sets, target = SetTarget.DistanceTime(meters, seconds))

internal fun weightSet(
    kg: Double,
    reps: Int,
    setNumber: Int = 1,
    warmup: Boolean = false,
    exerciseId: String = "Barbell_Bench_Press",
) = SetLog.WeightReps(
    exerciseId = exerciseId, workoutId = "w1", setNumber = setNumber,
    isWarmup = warmup, timestamp = 0L, kg = kg, reps = reps,
)

/** Trzy identyczne serie robocze WEIGHT_REPS (typowy zaliczony trening). */
internal fun weightSets(kg: Double, reps: Int, count: Int = 3): List<SetLog> =
    (1..count).map { weightSet(kg, reps, setNumber = it) }

internal fun repsSet(
    reps: Int,
    setNumber: Int = 1,
    warmup: Boolean = false,
    exerciseId: String = "Pullups",
) = SetLog.Reps(
    exerciseId = exerciseId, workoutId = "w1", setNumber = setNumber,
    isWarmup = warmup, timestamp = 0L, reps = reps,
)

internal fun timeSet(
    seconds: Int,
    setNumber: Int = 1,
    exerciseId: String = "Plank",
) = SetLog.Time(
    exerciseId = exerciseId, workoutId = "w1", setNumber = setNumber,
    isWarmup = false, timestamp = 0L, seconds = seconds,
)

internal fun distSet(
    meters: Double,
    seconds: Int,
    setNumber: Int = 1,
    exerciseId: String = "Running_Treadmill",
) = SetLog.DistanceTime(
    exerciseId = exerciseId, workoutId = "w1", setNumber = setNumber,
    isWarmup = false, timestamp = 0L, meters = meters, seconds = seconds,
)

internal fun stateOf(
    exerciseId: String = "Barbell_Bench_Press",
    lastSets: List<SetLog> = emptyList(),
    failStreak: Int = 0,
    currentWeightKg: Double? = null,
) = ExerciseState(
    exerciseId = exerciseId,
    lastSets = lastSets,
    failStreak = failStreak,
    currentWeightKg = currentWeightKg,
    updatedAt = 0L,
)

private val noStress = JointStress(
    lowBack = StressLevel.NONE, knee = StressLevel.NONE, shoulder = StressLevel.NONE,
    hip = StressLevel.NONE, elbow = StressLevel.NONE, wrist = StressLevel.NONE,
    neck = StressLevel.NONE,
)

internal fun exerciseOf(
    mechanic: String?,
    primaryMuscles: List<String>,
) = Exercise(
    id = "X", name = "X", namePl = "X", instructionsPl = emptyList(),
    primaryMuscles = primaryMuscles, secondaryMuscles = emptyList(),
    equipment = null, level = "beginner", category = "strength",
    mechanic = mechanic, force = null, images = emptyList(),
    jointStress = noStress, cautionNotes = null,
    measurementType = MeasurementType.WEIGHT_REPS,
)

/** Skrót na proposeTargets z domyślnymi parametrami kontekstu. */
internal fun propose(
    plan: PlanExercise,
    state: ExerciseState? = null,
    returningFromBreak: Boolean = false,
    isCompoundLeg: Boolean = false,
    weekIndexInBlock: Int = 0,
    blockLengthWeeks: Int = ProgressionConstants.BLOCK_LENGTH_WEEKS_DEFAULT,
) = ProgressionEngine.proposeTargets(
    planExercise = plan,
    state = state,
    returningFromBreak = returningFromBreak,
    isCompoundLeg = isCompoundLeg,
    weekIndexInBlock = weekIndexInBlock,
    blockLengthWeeks = blockLengthWeeks,
)
