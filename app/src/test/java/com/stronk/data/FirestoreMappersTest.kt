package com.stronk.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testy konwerterów model ↔ mapa Firestore ([FirestoreMappers]):
 * symetria round-tripu, odporność na braki pól i nieznany "type"
 * oraz koercja liczb (Firestore zwraca Long/Double niezależnie od zapisu).
 */
class FirestoreMappersTest {

    // ---------- dane testowe ----------

    private val weightReps = SetLog.WeightReps(
        exerciseId = "Barbell_Squat", workoutId = "w1",
        setNumber = 1, isWarmup = false, timestamp = 1_755_000_000_000,
        kg = 100.0, reps = 5,
    )
    private val reps = SetLog.Reps(
        exerciseId = "Pullups", workoutId = "w1",
        setNumber = 2, isWarmup = false, timestamp = 1_755_000_060_000,
        reps = 10, extraKg = 5.0,
    )
    private val time = SetLog.Time(
        exerciseId = "Plank", workoutId = "w1",
        setNumber = 1, isWarmup = false, timestamp = 1_755_000_120_000,
        seconds = 60,
    )
    private val distanceTime = SetLog.DistanceTime(
        exerciseId = "Running_Treadmill", workoutId = "w1",
        setNumber = 1, isWarmup = true, timestamp = 1_755_000_180_000,
        meters = 1000.0, seconds = 300,
    )

    private val plan = Plan(
        id = "p1",
        name = "Powrót po przerwie",
        createdAt = 1_755_100_000_000,
        archived = false,
        days = listOf(
            PlanDay(
                name = "Push A",
                exercises = listOf(
                    PlanExercise(
                        exerciseId = "Barbell_Squat", sets = 3,
                        target = SetTarget.WeightReps(reps = 5),
                        startWeightKg = 60.0, progressionEnabled = true,
                    ),
                    PlanExercise(
                        exerciseId = "Pullups", sets = 3,
                        target = SetTarget.Reps(reps = 8),
                        progressionEnabled = false,
                    ),
                ),
            ),
            PlanDay(
                name = "Cardio",
                exercises = listOf(
                    PlanExercise(
                        exerciseId = "Plank", sets = 3,
                        target = SetTarget.Time(seconds = 60),
                    ),
                    PlanExercise(
                        exerciseId = "Running_Treadmill", sets = 1,
                        target = SetTarget.DistanceTime(meters = 3000.0, seconds = 1200),
                    ),
                ),
            ),
        ),
    )

    /**
     * Symuluje odczyt z Firestore: wszystkie Int wracają jako Long
     * (Firestore trzyma liczby całkowite jako int64).
     */
    private fun simulateFirestoreRead(map: Map<String, Any?>): Map<String, Any?> =
        map.mapValues { (_, value) -> simulateValue(value) }

    @Suppress("UNCHECKED_CAST")
    private fun simulateValue(value: Any?): Any? = when (value) {
        is Int -> value.toLong()
        is List<*> -> value.map { simulateValue(it) }
        is Map<*, *> -> simulateFirestoreRead(value as Map<String, Any?>)
        else -> value
    }

    // ---------- SetLog ----------

    @Test
    fun `każdy wariant SetLog wraca z round-tripu mapy bez zmian`() {
        for (log in listOf<SetLog>(weightReps, reps, time, distanceTime)) {
            val map = FirestoreMappers.setLogToMap(log)
            assertEquals(log, FirestoreMappers.setLogFromMap(map, workoutId = "w1"))
        }
    }

    @Test
    fun `SetLog round-trip działa też na liczbach Long jak z Firestore`() {
        for (log in listOf<SetLog>(weightReps, reps, time, distanceTime)) {
            val map = simulateFirestoreRead(FirestoreMappers.setLogToMap(log))
            assertEquals(log, FirestoreMappers.setLogFromMap(map, workoutId = "w1"))
        }
    }

    @Test
    fun `mapa SetLog ma jawne pole type i nie ma workoutId`() {
        val expectedTypes = mapOf<SetLog, String>(
            weightReps to "WEIGHT_REPS",
            reps to "REPS",
            time to "TIME",
            distanceTime to "DISTANCE_TIME",
        )
        for ((log, expectedType) in expectedTypes) {
            val map = FirestoreMappers.setLogToMap(log)
            assertEquals(expectedType, map["type"])
            // workoutId to kontekst (id dokumentu treningu), nie pole serii
            assertFalse("workoutId w mapie ${log::class.simpleName}", "workoutId" in map)
        }
    }

    @Test
    fun `Reps bez extraKg nie zapisuje pola i wraca jako null`() {
        val bodyweight = reps.copy(extraKg = null)
        val map = FirestoreMappers.setLogToMap(bodyweight)
        assertFalse("extraKg" in map)
        assertEquals(bodyweight, FirestoreMappers.setLogFromMap(map, workoutId = "w1"))
    }

    @Test
    fun `SetLog z nieznanym type jest pomijany`() {
        val map = FirestoreMappers.setLogToMap(weightReps) + ("type" to "TEMPO_REPS")
        assertNull(FirestoreMappers.setLogFromMap(map, workoutId = "w1"))
    }

    @Test
    fun `SetLog bez pól wariantu jest pomijany, braki pól wspólnych dostają defaulty`() {
        // brak kg → wpis pomijany (nie da się odtworzyć sensownej serii)
        val withoutKg = FirestoreMappers.setLogToMap(weightReps) - "kg"
        assertNull(FirestoreMappers.setLogFromMap(withoutKg, workoutId = "w1"))

        // braki pól wspólnych → defaulty zamiast crasha
        val minimal = mapOf<String, Any?>(
            "type" to "TIME", "exerciseId" to "Plank", "seconds" to 45,
        )
        val decoded = FirestoreMappers.setLogFromMap(minimal, workoutId = "w9")
        assertEquals(
            SetLog.Time(
                exerciseId = "Plank", workoutId = "w9",
                setNumber = 1, isWarmup = false, timestamp = 0L, seconds = 45,
            ),
            decoded,
        )
    }

    // ---------- SetTarget ----------

    @Test
    fun `każdy wariant SetTarget wraca z round-tripu z jawnym polem type`() {
        val targets = mapOf<SetTarget, String>(
            SetTarget.WeightReps(reps = 5) to "WEIGHT_REPS",
            SetTarget.Reps(reps = 8) to "REPS",
            SetTarget.Time(seconds = 60) to "TIME",
            SetTarget.DistanceTime(meters = 3000.0, seconds = 1200) to "DISTANCE_TIME",
        )
        for ((target, expectedType) in targets) {
            val map = FirestoreMappers.setTargetToMap(target)
            assertEquals(expectedType, map["type"])
            assertEquals(target, FirestoreMappers.setTargetFromMap(simulateFirestoreRead(map)))
        }
    }

    @Test
    fun `SetTarget z nieznanym type jest pomijany`() {
        assertNull(FirestoreMappers.setTargetFromMap(mapOf("type" to "AMRAP", "reps" to 5)))
    }

    // ---------- Plan ----------

    @Test
    fun `Plan wraca z round-tripu bez zmian (też na liczbach Long)`() {
        val map = FirestoreMappers.planToMap(plan)
        assertEquals(plan, FirestoreMappers.planFromMap("p1", map))
        assertEquals(plan, FirestoreMappers.planFromMap("p1", simulateFirestoreRead(map)))
    }

    @Test
    fun `ćwiczenie planu z nieznanym celem jest pomijane, reszta planu zostaje`() {
        val map = FirestoreMappers.planToMap(plan)
        val brokenMap = simulateFirestoreRead(map).toMutableMap().apply {
            @Suppress("UNCHECKED_CAST")
            val days = (this["days"] as List<Map<String, Any?>>).toMutableList()
            @Suppress("UNCHECKED_CAST")
            val exercises = (days[0]["exercises"] as List<Map<String, Any?>>).toMutableList()
            exercises[0] = exercises[0] + ("target" to mapOf("type" to "AMRAP"))
            days[0] = days[0] + ("exercises" to exercises)
            this["days"] = days
        }
        val decoded = FirestoreMappers.planFromMap("p1", brokenMap)
        assertEquals(1, decoded.days[0].exercises.size)
        assertEquals("Pullups", decoded.days[0].exercises[0].exerciseId)
        assertEquals(plan.days[1], decoded.days[1])
    }

    @Test
    fun `Plan z pustej mapy dostaje defaulty zamiast crasha`() {
        val decoded = FirestoreMappers.planFromMap("p2", emptyMap())
        assertEquals(Plan(id = "p2", name = "", createdAt = 0L), decoded)
    }

    @Test
    fun `Plan zapisuje blockLengthWeeks i wraca z round-tripu (też na Long)`() {
        val custom = plan.copy(blockLengthWeeks = 3)
        val map = FirestoreMappers.planToMap(custom)
        assertEquals(3, map["blockLengthWeeks"])
        assertEquals(custom, FirestoreMappers.planFromMap("p1", map))
        assertEquals(custom, FirestoreMappers.planFromMap("p1", simulateFirestoreRead(map)))
    }

    @Test
    fun `Plan bez blockLengthWeeks to plan BEZ bloku, nie plan z domyślnym blokiem`() {
        val map = FirestoreMappers.planToMap(plan) - "blockLengthWeeks"
        val decoded = FirestoreMappers.planFromMap("p1", map)
        assertNull(decoded.blockLengthWeeks)
        assertEquals(plan, decoded)
    }

    @Test
    fun `Plan bez bloku nie zapisuje pola blockLengthWeeks`() {
        val map = FirestoreMappers.planToMap(plan.copy(blockLengthWeeks = null))
        assertFalse(map.containsKey("blockLengthWeeks"))
        assertNull(FirestoreMappers.planFromMap("p1", simulateFirestoreRead(map)).blockLengthWeeks)
    }

    // ---------- ScheduleEntry ----------

    @Test
    fun `ScheduleEntry wraca z round-tripu dla każdego statusu`() {
        val base = ScheduleEntry(
            id = "s1", date = "2026-08-18", planId = "p1", dayIndex = 1,
        )
        val variants = listOf(
            base,
            base.copy(status = ScheduleStatus.DONE, workoutId = "w1"),
            base.copy(status = ScheduleStatus.SKIPPED),
            base.copy(status = ScheduleStatus.MOVED, movedTo = "2026-08-20"),
        )
        for (entry in variants) {
            val map = simulateFirestoreRead(FirestoreMappers.scheduleEntryToMap(entry))
            assertEquals(entry, FirestoreMappers.scheduleEntryFromMap("s1", map))
        }
    }

    @Test
    fun `status zapisuje się małymi literami, pola null nie są zapisywane`() {
        val entry = ScheduleEntry(id = "s1", date = "2026-08-18", planId = "p1", dayIndex = 0)
        val map = FirestoreMappers.scheduleEntryToMap(entry)
        assertEquals("planned", map["status"])
        assertFalse("movedTo" in map)
        assertFalse("workoutId" in map)
    }

    @Test
    fun `ScheduleEntry z nieznanym statusem dostaje PLANNED, bez daty jest pomijany`() {
        val map = FirestoreMappers.scheduleEntryToMap(
            ScheduleEntry(id = "s1", date = "2026-08-18", planId = "p1", dayIndex = 0),
        )
        val unknownStatus = FirestoreMappers.scheduleEntryFromMap("s1", map + ("status" to "paused"))
        assertEquals(ScheduleStatus.PLANNED, unknownStatus?.status)

        assertNull(FirestoreMappers.scheduleEntryFromMap("s1", map - "date"))
    }

    // ---------- Workout ----------

    @Test
    fun `Workout z seriami wszystkich wariantów wraca z round-tripu (też na Long)`() {
        val workout = Workout(
            id = "w1",
            startedAt = 1_755_000_000_000,
            finishedAt = 1_755_003_600_000,
            planId = "p1",
            dayIndex = 0,
            scheduleEntryId = "s1",
            notes = "Ciężko, ale poszło",
            sets = listOf(weightReps, reps, time, distanceTime),
        )
        val map = FirestoreMappers.workoutToMap(workout)
        assertEquals(workout, FirestoreMappers.workoutFromMap("w1", map))
        assertEquals(workout, FirestoreMappers.workoutFromMap("w1", simulateFirestoreRead(map)))
    }

    @Test
    fun `mapa Workout ma zdenormalizowane exerciseIds wyliczone z serii`() {
        val workout = Workout(
            id = "w1", startedAt = 1L,
            sets = listOf(weightReps, weightReps.copy(setNumber = 2), reps),
        )
        val map = FirestoreMappers.workoutToMap(workout)
        assertEquals(listOf("Barbell_Squat", "Pullups"), map["exerciseIds"])
    }

    @Test
    fun `Workout w trakcie (pola null) i seria z nieznanym type są obsłużone`() {
        val inProgress = Workout(id = "w2", startedAt = 5L, sets = listOf(time))
        val map = FirestoreMappers.workoutToMap(inProgress)
        assertFalse("finishedAt" in map)
        assertFalse("planId" in map)
        assertFalse("notes" in map)
        // trening spoza planu wraca bez zmian
        val roundTripped = FirestoreMappers.workoutFromMap("w2", map)
        assertEquals(inProgress.copy(sets = listOf(time.copy(workoutId = "w2"))), roundTripped)

        // seria z nieznanym type jest pomijana, reszta serii zostaje
        @Suppress("UNCHECKED_CAST")
        val sets = (map["sets"] as List<Map<String, Any?>>) + mapOf("type" to "TEMPO_REPS")
        val decoded = FirestoreMappers.workoutFromMap("w2", map + ("sets" to sets))
        assertEquals(1, decoded.sets.size)
    }

    // ---------- ExerciseState ----------

    @Test
    fun `ExerciseState wraca z round-tripu bez zmian (też na Long)`() {
        val state = ExerciseState(
            exerciseId = "Barbell_Squat",
            // lastSets są embedded w stanie — workoutId nie jest polem wire, kontekst = ""
            lastSets = listOf(weightReps.copy(workoutId = ""), reps.copy(workoutId = "")),
            failStreak = 1,
            currentWeightKg = 102.5,
            updatedAt = 1_755_200_000_000,
        )
        val map = FirestoreMappers.exerciseStateToMap(state)
        assertEquals(state, FirestoreMappers.exerciseStateFromMap("Barbell_Squat", map))
        assertEquals(state, FirestoreMappers.exerciseStateFromMap("Barbell_Squat", simulateFirestoreRead(map)))
    }

    @Test
    fun `ExerciseState bez currentWeightKg i z pustej mapy dostaje defaulty`() {
        val fresh = ExerciseState(exerciseId = "Plank", updatedAt = 7L)
        val map = FirestoreMappers.exerciseStateToMap(fresh)
        assertFalse("currentWeightKg" in map)
        assertEquals(fresh, FirestoreMappers.exerciseStateFromMap("Plank", map))

        val decoded = FirestoreMappers.exerciseStateFromMap("Plank", emptyMap())
        assertEquals(ExerciseState(exerciseId = "Plank", updatedAt = 0L), decoded)
    }

    // ---------- UserProfile ----------

    @Test
    fun `UserProfile wraca z round-tripu bez zmian (też na Long)`() {
        val profile = UserProfile(
            displayName = "Karol",
            createdAt = 1_755_400_000_000,
            profile = ProfileDetails(
                equipment = listOf("barbell", "dumbbell"),
                constraints = mapOf("knee" to StressLevel.LOW, "lowBack" to StressLevel.MEDIUM),
                goal = TrainingGoal.RETURN_TO_FORM,
                returningFromBreak = true,
            ),
        )
        val map = FirestoreMappers.userProfileToMap(profile)
        assertEquals(profile, FirestoreMappers.userProfileFromMap(map))
        assertEquals(profile, FirestoreMappers.userProfileFromMap(simulateFirestoreRead(map)))
    }

    @Test
    fun `constraints zapisują się małymi literami jak w modelu danych`() {
        val profile = UserProfile(
            createdAt = 1L,
            profile = ProfileDetails(constraints = mapOf("knee" to StressLevel.MEDIUM)),
        )
        val map = FirestoreMappers.userProfileToMap(profile)
        @Suppress("UNCHECKED_CAST")
        val details = map["profile"] as Map<String, Any?>
        assertEquals(mapOf("knee" to "medium"), details["constraints"])
        assertFalse("displayName" in map)
    }

    @Test
    fun `goal zapisuje się małymi literami i wraca z round-tripu dla każdej wartości`() {
        for (goal in TrainingGoal.entries) {
            val profile = UserProfile(createdAt = 1L, profile = ProfileDetails(goal = goal))
            val map = FirestoreMappers.userProfileToMap(profile)
            @Suppress("UNCHECKED_CAST")
            val details = map["profile"] as Map<String, Any?>
            assertEquals(goal.name.lowercase(), details["goal"])
            assertEquals(profile, FirestoreMappers.userProfileFromMap(simulateFirestoreRead(map)))
        }
    }

    @Test
    fun `goal null nie jest zapisywany, brak i nieznana wartość dają null`() {
        // null → pole nie istnieje w mapie
        val map = FirestoreMappers.userProfileToMap(UserProfile(createdAt = 1L))
        @Suppress("UNCHECKED_CAST")
        val details = map["profile"] as Map<String, Any?>
        assertFalse("goal" in details)

        // brak pola → null
        assertNull(FirestoreMappers.userProfileFromMap(map).profile.goal)

        // nieznana wartość → null, reszta profilu zostaje
        val unknown = mapOf(
            "createdAt" to 1L,
            "profile" to mapOf("goal" to "cardio", "returningFromBreak" to true),
        )
        val decoded = FirestoreMappers.userProfileFromMap(unknown)
        assertNull(decoded.profile.goal)
        assertTrue(decoded.profile.returningFromBreak)
    }

    @Test
    fun `UserProfile toleruje braki i nieznany poziom ograniczenia`() {
        // pusta mapa → defaulty
        val empty = FirestoreMappers.userProfileFromMap(emptyMap())
        assertEquals(UserProfile(createdAt = 0L), empty)

        // nieznany poziom ograniczenia → wpis pomijany, reszta zostaje
        val map = mapOf(
            "createdAt" to 9L,
            "profile" to mapOf(
                "equipment" to listOf("barbell"),
                "constraints" to mapOf("knee" to "extreme", "lowBack" to "low"),
            ),
        )
        val decoded = FirestoreMappers.userProfileFromMap(map)
        assertEquals(mapOf("lowBack" to StressLevel.LOW), decoded.profile.constraints)
        assertEquals(listOf("barbell"), decoded.profile.equipment)
        assertTrue(!decoded.profile.returningFromBreak)
    }
}
