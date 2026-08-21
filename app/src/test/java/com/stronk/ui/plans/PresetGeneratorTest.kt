package com.stronk.ui.plans

import com.stronk.data.Exercise
import com.stronk.data.GoalDefaults
import com.stronk.data.JointStress
import com.stronk.data.MeasurementType
import com.stronk.data.ProfileDetails
import com.stronk.data.SetTarget
import com.stronk.data.StressLevel
import com.stronk.data.TrainingGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Testy parametryzacji presetów profilem (dobór kandydatów, cele, dedupe). */
class PresetGeneratorTest {

    private fun exercise(
        id: String,
        primaryMuscles: List<String> = listOf("quadriceps"),
        equipment: String? = "machine",
        level: String = "beginner",
        category: String = "strength",
        mechanic: String? = "compound",
        measurementType: MeasurementType = MeasurementType.WEIGHT_REPS,
        knee: StressLevel = StressLevel.NONE,
        lowBack: StressLevel = StressLevel.NONE,
    ) = Exercise(
        id = id,
        name = id,
        namePl = id,
        instructionsPl = emptyList(),
        primaryMuscles = primaryMuscles,
        secondaryMuscles = emptyList(),
        equipment = equipment,
        level = level,
        category = category,
        mechanic = mechanic,
        force = null,
        images = emptyList(),
        jointStress = JointStress(
            lowBack = lowBack, knee = knee, shoulder = StressLevel.NONE,
            hip = StressLevel.NONE, elbow = StressLevel.NONE, wrist = StressLevel.NONE,
            neck = StressLevel.NONE,
        ),
        measurementType = measurementType,
    )

    private fun slot(vararg candidateIds: String, sets: Int = 3, reps: Int = 10) =
        PresetSlot(label = "slot", candidateIds = candidateIds.toList(), sets = sets, reps = reps)

    private fun preset(vararg days: PresetDay) = PlanPreset(
        id = "test",
        name = "Test",
        description = "",
        days = days.toList(),
    )

    // --- resolveSlotExercise ---

    @Test
    fun `pierwszy w pelni zgodny kandydat wygrywa`() {
        val a = exercise("a")
        val b = exercise("b")
        val resolved = resolveSlotExercise(
            slot("a", "b"),
            mapOf("a" to a, "b" to b),
            listOf(a, b),
            ProfileDetails(),
        )
        assertEquals("a", resolved?.id)
    }

    @Test
    fun `kandydat naruszajacy limit stawu przegrywa z pozniejszym zgodnym`() {
        val squat = exercise("squat", knee = StressLevel.HIGH)
        val legPress = exercise("legPress", knee = StressLevel.LOW)
        val profile = ProfileDetails(constraints = mapOf("knee" to StressLevel.LOW))
        val resolved = resolveSlotExercise(
            slot("squat", "legPress"),
            mapOf("squat" to squat, "legPress" to legPress),
            listOf(squat, legPress),
            profile,
        )
        assertEquals("legPress", resolved?.id)
    }

    @Test
    fun `kandydat bez sprzetu przegrywa z pozniejszym dostepnym`() {
        val barbell = exercise("barbell", equipment = "barbell")
        val dumbbell = exercise("dumbbell", equipment = "dumbbell")
        val profile = ProfileDetails(equipment = listOf("dumbbell"))
        val resolved = resolveSlotExercise(
            slot("barbell", "dumbbell"),
            mapOf("barbell" to barbell, "dumbbell" to dumbbell),
            listOf(barbell, dumbbell),
            profile,
        )
        assertEquals("dumbbell", resolved?.id)
    }

    @Test
    fun `brak zgodnego kandydata siega po zamiennik bez naruszen`() {
        // Obaj kandydaci naruszają kolano; w bazie jest zgodny zamiennik
        // dzielący partię główną.
        val squat = exercise("squat", knee = StressLevel.HIGH)
        val lunge = exercise("lunge", knee = StressLevel.HIGH)
        val substitute = exercise("substitute", knee = StressLevel.NONE)
        val profile = ProfileDetails(constraints = mapOf("knee" to StressLevel.LOW))
        val resolved = resolveSlotExercise(
            slot("squat", "lunge"),
            mapOf("squat" to squat, "lunge" to lunge),
            listOf(squat, lunge, substitute),
            profile,
        )
        assertEquals("substitute", resolved?.id)
    }

    @Test
    fun `bez zamiennikow zostaje pierwszy kandydat z dostepnym sprzetem`() {
        // Naruszenia nie dyskwalifikują — edytor je oflaguje (CONCEPT: flagować).
        val squat = exercise("squat", knee = StressLevel.HIGH, equipment = "barbell")
        val legPress = exercise("legPress", knee = StressLevel.HIGH, equipment = "machine")
        val profile = ProfileDetails(
            equipment = listOf("machine"),
            constraints = mapOf("knee" to StressLevel.LOW),
        )
        val resolved = resolveSlotExercise(
            slot("squat", "legPress"),
            mapOf("squat" to squat, "legPress" to legPress),
            listOf(squat, legPress),
            profile,
        )
        assertEquals("legPress", resolved?.id)
    }

    @Test
    fun `uzyte cwiczenie jest pomijane na rzecz kolejnego kandydata`() {
        val a = exercise("a")
        val b = exercise("b")
        val resolved = resolveSlotExercise(
            slot("a", "b"),
            mapOf("a" to a, "b" to b),
            listOf(a, b),
            ProfileDetails(),
            usedIds = setOf("a"),
        )
        assertEquals("b", resolved?.id)
    }

    // --- generatePresetDays ---

    @Test
    fun `slot bez istniejacych kandydatow jest pomijany`() {
        val a = exercise("a")
        val days = generatePresetDays(
            preset(PresetDay("Dzień", listOf(slot("a"), slot("nie-ma-takiego")))),
            listOf(a),
            ProfileDetails(),
        )
        assertEquals(1, days.size)
        assertEquals(listOf("a"), days.first().exercises.map { it.exerciseId })
    }

    @Test
    fun `cel budowany wg typu pomiaru kandydata, powtorzenia z GoalDefaults`() {
        // reps < 12 w definicji slotu = złożone (defaultReps), reps >= 12 = akcesoryjne (accessoryReps).
        val weightReps = exercise("wr", measurementType = MeasurementType.WEIGHT_REPS)
        val reps = exercise("r", primaryMuscles = listOf("chest"), measurementType = MeasurementType.REPS)
        val time = exercise("t", primaryMuscles = listOf("abdominals"), measurementType = MeasurementType.TIME)
        val days = generatePresetDays(
            preset(PresetDay("Dzień", listOf(slot("wr", reps = 8), slot("r", reps = 12), slot("t")))),
            listOf(weightReps, reps, time),
            ProfileDetails(), // brak celu -> GoalDefaults.FALLBACK (MASS)
        )
        val exercises = days.first().exercises
        assertEquals(SetTarget.WeightReps(GoalDefaults.forGoal(null).defaultReps), exercises[0].target)
        assertEquals(SetTarget.Reps(GoalDefaults.forGoal(null).accessoryReps), exercises[1].target)
        assertEquals(SetTarget.Time(PlanDefaults.DEFAULT_TIME_SECONDS), exercises[2].target)
    }

    @Test
    fun `dwa sloty z tym samym pierwszym kandydatem nie daja duplikatu w dniu`() {
        val a = exercise("a")
        val b = exercise("b")
        val days = generatePresetDays(
            preset(PresetDay("Dzień", listOf(slot("a", "b"), slot("a", "b")))),
            listOf(a, b),
            ProfileDetails(),
        )
        assertEquals(listOf("a", "b"), days.first().exercises.map { it.exerciseId })
    }

    @Test
    fun `preset ustawia serie z GoalDefaults (brak celu = fallback) i wlaczona progresje`() {
        val a = exercise("a")
        val days = generatePresetDays(
            preset(PresetDay("Dzień", listOf(slot("a", sets = 4)))),
            listOf(a),
            ProfileDetails(),
        )
        val planExercise = days.first().exercises.single()
        assertEquals(GoalDefaults.forGoal(null).defaultSets, planExercise.sets)
        assertTrue(planExercise.progressionEnabled)
        assertNull(planExercise.startWeightKg)
    }

    @Test
    fun `serie i powtorzenia podazaja za celem treningowym z profilu`() {
        val compound = exercise("a") // slot reps < 12 -> złożone
        val accessory = exercise("b", primaryMuscles = listOf("chest"))
        val strength = ProfileDetails(goal = TrainingGoal.STRENGTH)
        val days = generatePresetDays(
            preset(PresetDay("Dzień", listOf(slot("a", sets = 4, reps = 8), slot("b", sets = 3, reps = 15)))),
            listOf(compound, accessory),
            strength,
        )
        val exercises = days.first().exercises
        assertEquals(GoalDefaults.STRENGTH.defaultSets, exercises[0].sets)
        assertEquals(SetTarget.WeightReps(GoalDefaults.STRENGTH.defaultReps), exercises[0].target)
        assertEquals(GoalDefaults.STRENGTH.defaultSets, exercises[1].sets)
        assertEquals(SetTarget.WeightReps(GoalDefaults.STRENGTH.accessoryReps), exercises[1].target)
    }

    // --- mechanizm K/Ł (gentleCandidateIds + relevantJoints) ---

    @Test
    fun `aktywne ograniczenie relevantJoint wybiera liste lagodna`() {
        val classicPick = exercise("classic", knee = StressLevel.HIGH)
        val gentlePick = exercise("gentle", knee = StressLevel.NONE)
        val slotWithGentle = PresetSlot(
            label = "slot",
            candidateIds = listOf("classic"),
            gentleCandidateIds = listOf("gentle"),
            relevantJoints = setOf("knee"),
            sets = 3,
            reps = 10,
        )
        val profile = ProfileDetails(constraints = mapOf("knee" to StressLevel.LOW))
        val resolved = resolveSlotExercise(
            slotWithGentle,
            mapOf("classic" to classicPick, "gentle" to gentlePick),
            listOf(classicPick, gentlePick),
            profile,
        )
        assertEquals("gentle", resolved?.id)
    }

    @Test
    fun `brak aktywnego ograniczenia relevantJoint wybiera liste klasyczna`() {
        val classicPick = exercise("classic", knee = StressLevel.HIGH)
        val gentlePick = exercise("gentle", knee = StressLevel.NONE)
        val slotWithGentle = PresetSlot(
            label = "slot",
            candidateIds = listOf("classic"),
            gentleCandidateIds = listOf("gentle"),
            relevantJoints = setOf("knee"),
            sets = 3,
            reps = 10,
        )
        // Profil bez ograniczenia kolana (nawet z innymi ograniczeniami) -> klasyczna.
        val profile = ProfileDetails(constraints = mapOf("lowBack" to StressLevel.LOW))
        val resolved = resolveSlotExercise(
            slotWithGentle,
            mapOf("classic" to classicPick, "gentle" to gentlePick),
            listOf(classicPick, gentlePick),
            profile,
        )
        assertEquals("classic", resolved?.id)
    }

    @Test
    fun `slot bez gentleCandidateIds domyslnie rowny candidateIds`() {
        assertEquals(slot("a", "b").candidateIds, slot("a", "b").gentleCandidateIds)
    }

    // --- dedup miedzy dniami (generatePresetDays) ---

    @Test
    fun `dedup miedzy dniami omija juz uzyte cwiczenie w kolejnym dniu`() {
        val a = exercise("a")
        val b = exercise("b")
        val days = generatePresetDays(
            preset(
                PresetDay("Dzień 1", listOf(slot("a", "b"))),
                PresetDay("Dzień 2", listOf(slot("a", "b"))),
            ),
            listOf(a, b),
            ProfileDetails(),
        )
        assertEquals("a", days[0].exercises.single().exerciseId)
        // Bez dedupu między dniami dzień 2 też wybrałby "a" (per-day usedIds resetowane) —
        // z dedupem na poziomie presetu przechodzi na "b".
        assertEquals("b", days[1].exercises.single().exerciseId)
    }

    @Test
    fun `dedup woli powtorke dobrego cwiczenia niz naruszenie stawu`() {
        // Tylko dwóch kandydatów w slocie: "good" (zgodny) i "bad" (narusza kolano).
        // Dzień 1 zużywa "good"; w dniu 2 jedyna nieużyta opcja to "bad" (narusza) —
        // generator ma wybrać POWTÓRKĘ "good", a nie "bad".
        val good = exercise("good", knee = StressLevel.NONE)
        val bad = exercise("bad", knee = StressLevel.HIGH)
        val profile = ProfileDetails(constraints = mapOf("knee" to StressLevel.LOW))
        val days = generatePresetDays(
            preset(
                PresetDay("Dzień 1", listOf(slot("good", "bad"))),
                PresetDay("Dzień 2", listOf(slot("good", "bad"))),
            ),
            listOf(good, bad),
            profile,
        )
        assertEquals("good", days[0].exercises.single().exerciseId)
        assertEquals("good", days[1].exercises.single().exerciseId)
    }

    @Test
    fun `dedup w ramach jednego dnia dziala jak dotychczas mimo usedIds na poziomie presetu`() {
        val a = exercise("a")
        val b = exercise("b")
        val days = generatePresetDays(
            preset(PresetDay("Dzień", listOf(slot("a", "b"), slot("a", "b")))),
            listOf(a, b),
            ProfileDetails(),
        )
        assertEquals(listOf("a", "b"), days.first().exercises.map { it.exerciseId })
    }

    // --- convertTarget ---

    @Test
    fun `convertTarget przenosi powtorzenia miedzy typami na powtorzenia`() {
        assertEquals(
            SetTarget.Reps(8),
            convertTarget(SetTarget.WeightReps(8), MeasurementType.REPS),
        )
        assertEquals(
            SetTarget.WeightReps(12),
            convertTarget(SetTarget.Reps(12), MeasurementType.WEIGHT_REPS),
        )
    }

    @Test
    fun `convertTarget zachowuje zgodny typ i daje domyslne przy zmianie`() {
        assertEquals(
            SetTarget.Time(45),
            convertTarget(SetTarget.Time(45), MeasurementType.TIME),
        )
        assertEquals(
            SetTarget.Time(PlanDefaults.DEFAULT_TIME_SECONDS),
            convertTarget(SetTarget.WeightReps(8), MeasurementType.TIME),
        )
        assertEquals(
            SetTarget.DistanceTime(
                PlanDefaults.DEFAULT_DISTANCE_METERS,
                PlanDefaults.DEFAULT_DISTANCE_SECONDS,
            ),
            convertTarget(SetTarget.Reps(10), MeasurementType.DISTANCE_TIME),
        )
    }
}
