package com.stronk.data

/**
 * Jawne konwertery model ↔ `Map<String, Any?>` dla Firestore — SDK nie rozumie
 * kotlinx-serialization, więc mapowanie żyje w jednym miejscu i jest symetryczne
 * (toMap/fromMap). Format wire dokładnie per docs/firestore-data-model.md.
 *
 * Zasady:
 * - pola null nie są zapisywane (jak explicitNulls=false w [StronkJson]),
 * - odczyt jest odporny na braki: pola opcjonalne → null/default, wpis
 *   z nieznanym polem "type" ([SetLog]/[SetTarget]) jest pomijany zamiast
 *   crashować, brakujące pola wymagane wariantu też pomijają wpis,
 * - liczby z Firestore wracają jako Long/Double — odczyt koercjonuje
 *   przez [Number], niezależnie od typu zapisanego.
 */
object FirestoreMappers {

    // ---------- SetLog ----------

    /** Typ pomiaru wariantu serii — wartość jawnego pola "type" (jeden słownik z [MeasurementType]). */
    fun measurementTypeOf(log: SetLog): MeasurementType = when (log) {
        is SetLog.WeightReps -> MeasurementType.WEIGHT_REPS
        is SetLog.Reps -> MeasurementType.REPS
        is SetLog.Time -> MeasurementType.TIME
        is SetLog.DistanceTime -> MeasurementType.DISTANCE_TIME
    }

    /**
     * Seria bez pola workoutId — serie żyją embedded w dokumencie treningu
     * (lub w exerciseState), więc id treningu jest kontekstem, nie polem wire.
     */
    fun setLogToMap(log: SetLog): Map<String, Any?> = buildMap {
        put("type", measurementTypeOf(log).name)
        put("exerciseId", log.exerciseId)
        put("setNumber", log.setNumber)
        put("isWarmup", log.isWarmup)
        put("timestamp", log.timestamp)
        when (log) {
            is SetLog.WeightReps -> {
                put("kg", log.kg)
                put("reps", log.reps)
            }
            is SetLog.Reps -> {
                put("reps", log.reps)
                log.extraKg?.let { put("extraKg", it) }
            }
            is SetLog.Time -> put("seconds", log.seconds)
            is SetLog.DistanceTime -> {
                put("meters", log.meters)
                put("seconds", log.seconds)
            }
        }
    }

    /**
     * Odtwarza serię; [workoutId] przychodzi z kontekstu (id dokumentu treningu,
     * a dla lastSets w exerciseState — pusty string, bo kontekstem jest stan).
     * Nieznany "type" albo brak pól wariantu → null (wpis pomijany).
     */
    fun setLogFromMap(map: Map<String, Any?>, workoutId: String): SetLog? {
        val type = map.stringOrNull("type") ?: return null
        val measurementType = MeasurementType.entries.firstOrNull { it.name == type } ?: return null
        val exerciseId = map.stringOrNull("exerciseId") ?: return null
        val setNumber = map.intOrNull("setNumber") ?: 1
        val isWarmup = map.boolOrNull("isWarmup") ?: false
        val timestamp = map.longOrNull("timestamp") ?: 0L
        return when (measurementType) {
            MeasurementType.WEIGHT_REPS -> SetLog.WeightReps(
                exerciseId = exerciseId, workoutId = workoutId,
                setNumber = setNumber, isWarmup = isWarmup, timestamp = timestamp,
                kg = map.doubleOrNull("kg") ?: return null,
                reps = map.intOrNull("reps") ?: return null,
            )
            MeasurementType.REPS -> SetLog.Reps(
                exerciseId = exerciseId, workoutId = workoutId,
                setNumber = setNumber, isWarmup = isWarmup, timestamp = timestamp,
                reps = map.intOrNull("reps") ?: return null,
                extraKg = map.doubleOrNull("extraKg"),
            )
            MeasurementType.TIME -> SetLog.Time(
                exerciseId = exerciseId, workoutId = workoutId,
                setNumber = setNumber, isWarmup = isWarmup, timestamp = timestamp,
                seconds = map.intOrNull("seconds") ?: return null,
            )
            MeasurementType.DISTANCE_TIME -> SetLog.DistanceTime(
                exerciseId = exerciseId, workoutId = workoutId,
                setNumber = setNumber, isWarmup = isWarmup, timestamp = timestamp,
                meters = map.doubleOrNull("meters") ?: return null,
                seconds = map.intOrNull("seconds") ?: return null,
            )
        }
    }

    // ---------- SetTarget ----------

    fun setTargetToMap(target: SetTarget): Map<String, Any?> = buildMap {
        when (target) {
            is SetTarget.WeightReps -> {
                put("type", MeasurementType.WEIGHT_REPS.name)
                put("reps", target.reps)
            }
            is SetTarget.Reps -> {
                put("type", MeasurementType.REPS.name)
                put("reps", target.reps)
            }
            is SetTarget.Time -> {
                put("type", MeasurementType.TIME.name)
                put("seconds", target.seconds)
            }
            is SetTarget.DistanceTime -> {
                put("type", MeasurementType.DISTANCE_TIME.name)
                put("meters", target.meters)
                put("seconds", target.seconds)
            }
        }
    }

    fun setTargetFromMap(map: Map<String, Any?>): SetTarget? {
        val type = map.stringOrNull("type") ?: return null
        val measurementType = MeasurementType.entries.firstOrNull { it.name == type } ?: return null
        return when (measurementType) {
            MeasurementType.WEIGHT_REPS -> SetTarget.WeightReps(reps = map.intOrNull("reps") ?: return null)
            MeasurementType.REPS -> SetTarget.Reps(reps = map.intOrNull("reps") ?: return null)
            MeasurementType.TIME -> SetTarget.Time(seconds = map.intOrNull("seconds") ?: return null)
            MeasurementType.DISTANCE_TIME -> SetTarget.DistanceTime(
                meters = map.doubleOrNull("meters") ?: return null,
                seconds = map.intOrNull("seconds") ?: return null,
            )
        }
    }

    // ---------- Plan ----------

    /**
     * Plan bez bloku (`blockLengthWeeks == null`) NIE zapisuje tego pola — brak
     * pola na wire znaczy dokładnie „plan bez bloku, ciągła progresja".
     */
    fun planToMap(plan: Plan): Map<String, Any?> = buildMap {
        put("name", plan.name)
        put("createdAt", plan.createdAt)
        put("archived", plan.archived)
        plan.blockLengthWeeks?.let { put("blockLengthWeeks", it) }
        put(
            "days",
            plan.days.map { day ->
                mapOf(
                    "name" to day.name,
                    "exercises" to day.exercises.map { planExerciseToMap(it) },
                )
            },
        )
    }

    fun planFromMap(id: String, map: Map<String, Any?>): Plan = Plan(
        id = id,
        name = map.stringOrNull("name").orEmpty(),
        createdAt = map.longOrNull("createdAt") ?: 0L,
        archived = map.boolOrNull("archived") ?: false,
        // Brak pola = plan bez bloku (null), nie „domyślne 5 tygodni".
        blockLengthWeeks = map.intOrNull("blockLengthWeeks"),
        days = map.mapList("days").map { day ->
            PlanDay(
                name = day.stringOrNull("name").orEmpty(),
                exercises = day.mapList("exercises").mapNotNull { planExerciseFromMap(it) },
            )
        },
    )

    private fun planExerciseToMap(exercise: PlanExercise): Map<String, Any?> = buildMap {
        put("exerciseId", exercise.exerciseId)
        put("sets", exercise.sets)
        put("target", setTargetToMap(exercise.target))
        exercise.startWeightKg?.let { put("startWeightKg", it) }
        put("progressionEnabled", exercise.progressionEnabled)
    }

    /** Ćwiczenie bez id, liczby serii albo z nieznanym celem → pomijane. */
    private fun planExerciseFromMap(map: Map<String, Any?>): PlanExercise? {
        return PlanExercise(
            exerciseId = map.stringOrNull("exerciseId") ?: return null,
            sets = map.intOrNull("sets") ?: return null,
            target = map.mapOrNull("target")?.let { setTargetFromMap(it) } ?: return null,
            startWeightKg = map.doubleOrNull("startWeightKg"),
            progressionEnabled = map.boolOrNull("progressionEnabled") ?: true,
        )
    }

    // ---------- ScheduleEntry ----------

    fun scheduleEntryToMap(entry: ScheduleEntry): Map<String, Any?> = buildMap {
        put("date", entry.date)
        put("planId", entry.planId)
        put("dayIndex", entry.dayIndex)
        put("status", entry.status.name.lowercase())
        entry.movedTo?.let { put("movedTo", it) }
        entry.workoutId?.let { put("workoutId", it) }
    }

    /** Wpis bez daty albo planu → null; nieznany status → PLANNED (bezpieczny default). */
    fun scheduleEntryFromMap(id: String, map: Map<String, Any?>): ScheduleEntry? {
        return ScheduleEntry(
            id = id,
            date = map.stringOrNull("date") ?: return null,
            planId = map.stringOrNull("planId") ?: return null,
            dayIndex = map.intOrNull("dayIndex") ?: 0,
            status = map.stringOrNull("status")
                ?.let { raw -> ScheduleStatus.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } }
                ?: ScheduleStatus.PLANNED,
            movedTo = map.stringOrNull("movedTo"),
            workoutId = map.stringOrNull("workoutId"),
        )
    }

    // ---------- Workout ----------

    fun workoutToMap(workout: Workout): Map<String, Any?> = buildMap {
        put("startedAt", workout.startedAt)
        workout.finishedAt?.let { put("finishedAt", it) }
        workout.planId?.let { put("planId", it) }
        workout.dayIndex?.let { put("dayIndex", it) }
        workout.scheduleEntryId?.let { put("scheduleEntryId", it) }
        put("exerciseIds", workout.exerciseIds)
        workout.notes?.let { put("notes", it) }
        put("sets", workout.sets.map { setLogToMap(it) })
    }

    /** Serie z nieznanym "type" są pomijane; exerciseIds odtwarza się z serii. */
    fun workoutFromMap(id: String, map: Map<String, Any?>): Workout = Workout(
        id = id,
        startedAt = map.longOrNull("startedAt") ?: 0L,
        finishedAt = map.longOrNull("finishedAt"),
        planId = map.stringOrNull("planId"),
        dayIndex = map.intOrNull("dayIndex"),
        scheduleEntryId = map.stringOrNull("scheduleEntryId"),
        notes = map.stringOrNull("notes"),
        sets = map.mapList("sets").mapNotNull { setLogFromMap(it, workoutId = id) },
    )

    // ---------- ExerciseState ----------

    fun exerciseStateToMap(state: ExerciseState): Map<String, Any?> = buildMap {
        put("lastSets", state.lastSets.map { setLogToMap(it) })
        put("failStreak", state.failStreak)
        state.currentWeightKg?.let { put("currentWeightKg", it) }
        put("updatedAt", state.updatedAt)
    }

    fun exerciseStateFromMap(exerciseId: String, map: Map<String, Any?>): ExerciseState = ExerciseState(
        exerciseId = exerciseId,
        // workoutId nie jest polem wire serii — dla lastSets kontekstem jest sam stan
        lastSets = map.mapList("lastSets").mapNotNull { setLogFromMap(it, workoutId = "") },
        failStreak = map.intOrNull("failStreak") ?: 0,
        currentWeightKg = map.doubleOrNull("currentWeightKg"),
        updatedAt = map.longOrNull("updatedAt") ?: 0L,
    )

    // ---------- UserProfile ----------

    fun userProfileToMap(profile: UserProfile): Map<String, Any?> = buildMap {
        profile.displayName?.let { put("displayName", it) }
        put("createdAt", profile.createdAt)
        put(
            "profile",
            buildMap {
                put("equipment", profile.profile.equipment)
                put(
                    "constraints",
                    profile.profile.constraints.mapValues { (_, level) -> level.name.lowercase() },
                )
                profile.profile.goal?.let { put("goal", it.name.lowercase()) }
                put("returningFromBreak", profile.profile.returningFromBreak)
            },
        )
    }

    /** Braki → defaulty; ograniczenie z nieznanym poziomem i nieznany cel są pomijane. */
    fun userProfileFromMap(map: Map<String, Any?>): UserProfile {
        val details = map.mapOrNull("profile")
        return UserProfile(
            displayName = map.stringOrNull("displayName"),
            createdAt = map.longOrNull("createdAt") ?: 0L,
            profile = ProfileDetails(
                equipment = (details?.get("equipment") as? List<*>).orEmpty().filterIsInstance<String>(),
                constraints = details?.mapOrNull("constraints").orEmpty()
                    .entries.mapNotNull { (joint, raw) ->
                        val level = (raw as? String)
                            ?.let { value -> StressLevel.entries.firstOrNull { it.name.equals(value, ignoreCase = true) } }
                        level?.let { joint to it }
                    }
                    .toMap(),
                goal = details?.stringOrNull("goal")
                    ?.let { raw -> TrainingGoal.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } },
                returningFromBreak = details?.boolOrNull("returningFromBreak") ?: false,
            ),
        )
    }

    // ---------- Pomocnicze odczyty z koercją typów ----------

    private fun Map<String, Any?>.stringOrNull(key: String): String? = this[key] as? String

    private fun Map<String, Any?>.boolOrNull(key: String): Boolean? = this[key] as? Boolean

    private fun Map<String, Any?>.intOrNull(key: String): Int? = (this[key] as? Number)?.toInt()

    private fun Map<String, Any?>.longOrNull(key: String): Long? = (this[key] as? Number)?.toLong()

    private fun Map<String, Any?>.doubleOrNull(key: String): Double? = (this[key] as? Number)?.toDouble()

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.mapOrNull(key: String): Map<String, Any?>? =
        this[key] as? Map<String, Any?>

    /** Lista zagnieżdżonych map (np. serie, dni planu); elementy innych typów są pomijane. */
    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.mapList(key: String): List<Map<String, Any?>> =
        (this[key] as? List<*>).orEmpty().mapNotNull { it as? Map<String, Any?> }
}
