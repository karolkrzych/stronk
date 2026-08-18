package com.stronk.ui.plans

import com.stronk.data.Exercise
import com.stronk.data.JointStress
import com.stronk.data.MeasurementType
import com.stronk.data.ProfileDetails
import com.stronk.data.StressLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Testy sugestii pokrycia partii mięśniowych dnia planu (moduł 3, wymaganie "Sugestie"). */
class PlanSuggestionsTest {

    private fun exercise(
        id: String,
        primaryMuscles: List<String>,
        equipment: String? = "machine",
        knee: StressLevel = StressLevel.NONE,
    ) = Exercise(
        id = id,
        name = id,
        namePl = id,
        instructionsPl = emptyList(),
        primaryMuscles = primaryMuscles,
        secondaryMuscles = emptyList(),
        equipment = equipment,
        level = "beginner",
        category = "strength",
        mechanic = "compound",
        force = null,
        images = emptyList(),
        jointStress = JointStress(
            lowBack = StressLevel.NONE, knee = knee, shoulder = StressLevel.NONE,
            hip = StressLevel.NONE, elbow = StressLevel.NONE, wrist = StressLevel.NONE,
            neck = StressLevel.NONE,
        ),
        measurementType = MeasurementType.WEIGHT_REPS,
    )

    // --- missingMajorGroups ---

    @Test
    fun `pusty dzien brakuje wszystkich duzych grup`() {
        assertEquals(MuscleGroup.entries.toList(), missingMajorGroups(emptyList()))
    }

    @Test
    fun `pokryta grupa znika z brakujacych`() {
        val bench = exercise("bench", listOf("chest"))
        val missing = missingMajorGroups(listOf(bench))
        assertTrue(MuscleGroup.CHEST !in missing)
        assertTrue(MuscleGroup.LEGS in missing)
    }

    @Test
    fun `drobne partie nie licza sie do duzych grup`() {
        val curl = exercise("curl", listOf("biceps"))
        // biceps nie jest dużą grupą -> nic nie zostaje pokryte.
        assertEquals(MuscleGroup.entries.toList(), missingMajorGroups(listOf(curl)))
    }

    // --- suggestExercisesForGroup ---

    @Test
    fun `sugestie ograniczone do wybranej grupy`() {
        val squat = exercise("squat", listOf("quadriceps"))
        val bench = exercise("bench", listOf("chest"))
        val matches = suggestExercisesForGroup(
            group = MuscleGroup.LEGS,
            allExercises = listOf(squat, bench),
            profile = ProfileDetails(),
            excludeIds = emptySet(),
        )
        assertEquals(listOf("squat"), matches.map { it.id })
    }

    @Test
    fun `sprzet niedostepny dyskwalifikuje kandydata`() {
        val squat = exercise("squat", listOf("quadriceps"), equipment = "barbell")
        val legPress = exercise("legPress", listOf("quadriceps"), equipment = "machine")
        val profile = ProfileDetails(equipment = listOf("machine"))
        val matches = suggestExercisesForGroup(
            group = MuscleGroup.LEGS,
            allExercises = listOf(squat, legPress),
            profile = profile,
            excludeIds = emptySet(),
        )
        assertEquals(listOf("legPress"), matches.map { it.id })
    }

    @Test
    fun `cwiczenia juz obecne w dniu sa pomijane`() {
        val squat = exercise("squat", listOf("quadriceps"))
        val legPress = exercise("legPress", listOf("quadriceps"))
        val matches = suggestExercisesForGroup(
            group = MuscleGroup.LEGS,
            allExercises = listOf(squat, legPress),
            profile = ProfileDetails(),
            excludeIds = setOf("squat"),
        )
        assertEquals(listOf("legPress"), matches.map { it.id })
    }

    @Test
    fun `zgodny kandydat wygrywa z naruszajacym limit stawu`() {
        val squat = exercise("squat", listOf("quadriceps"), knee = StressLevel.HIGH)
        val legPress = exercise("legPress", listOf("quadriceps"), knee = StressLevel.NONE)
        val profile = ProfileDetails(constraints = mapOf("knee" to StressLevel.LOW))
        val matches = suggestExercisesForGroup(
            group = MuscleGroup.LEGS,
            allExercises = listOf(squat, legPress),
            profile = profile,
            excludeIds = emptySet(),
        )
        assertEquals(listOf("legPress", "squat"), matches.map { it.id })
    }

    @Test
    fun `limit ogranicza liczbe propozycji`() {
        val a = exercise("a", listOf("chest"))
        val b = exercise("b", listOf("chest"))
        val c = exercise("c", listOf("chest"))
        val matches = suggestExercisesForGroup(
            group = MuscleGroup.CHEST,
            allExercises = listOf(a, b, c),
            profile = ProfileDetails(),
            excludeIds = emptySet(),
            limit = 2,
        )
        assertEquals(2, matches.size)
    }
}
