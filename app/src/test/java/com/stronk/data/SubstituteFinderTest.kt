package com.stronk.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Testy zgodności z profilem i rankingu zamienników (kontuzje, sprzęt, pusty profil). */
class SubstituteFinderTest {

    private fun exercise(
        id: String,
        namePl: String = id,
        primaryMuscles: List<String> = listOf("quadriceps"),
        equipment: String? = "barbell",
        level: String = "beginner",
        category: String = "strength",
        mechanic: String? = "compound",
        knee: StressLevel = StressLevel.NONE,
        lowBack: StressLevel = StressLevel.NONE,
        shoulder: StressLevel = StressLevel.NONE,
    ) = Exercise(
        id = id,
        name = id,
        namePl = namePl,
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
            lowBack = lowBack, knee = knee, shoulder = shoulder,
            hip = StressLevel.NONE, elbow = StressLevel.NONE, wrist = StressLevel.NONE,
            neck = StressLevel.NONE,
        ),
        measurementType = MeasurementType.WEIGHT_REPS,
    )

    // --- isCompliant ---

    @Test
    fun `isCompliant zwraca szczegóły naruszenia stawu i brak sprzętu`() {
        val squat = exercise("squat", knee = StressLevel.HIGH, equipment = "barbell")
        val profile = ProfileDetails(
            equipment = listOf("dumbbell"),
            constraints = mapOf("knee" to StressLevel.LOW),
        )

        val result = isCompliant(squat, profile)

        assertEquals(
            listOf(ConstraintViolation("knee", StressLevel.HIGH, StressLevel.LOW)),
            result.constraintViolations,
        )
        assertFalse(result.equipmentAvailable)
        assertFalse(result.isFullyCompliant)
    }

    @Test
    fun `isCompliant nie flaguje obciążenia równego limitowi`() {
        val legExtension = exercise("legExtension", knee = StressLevel.LOW, equipment = "machine")
        val profile = ProfileDetails(
            equipment = listOf("machine"),
            constraints = mapOf("knee" to StressLevel.LOW),
        )

        val result = isCompliant(legExtension, profile)

        assertTrue(result.isFullyCompliant)
    }

    @Test
    fun `isCompliant przy pustym profilu przepuszcza wszystko`() {
        val deadlift = exercise(
            "deadlift", primaryMuscles = listOf("hamstrings"),
            lowBack = StressLevel.HIGH, knee = StressLevel.MEDIUM, equipment = "barbell",
        )

        val result = isCompliant(deadlift, ProfileDetails())

        assertTrue(result.isFullyCompliant)
        assertTrue(result.constraintViolations.isEmpty())
        assertTrue(result.equipmentAvailable)
    }

    @Test
    fun `masa ciała jest dostępna nawet gdy nie ma jej w skonfigurowanym sprzęcie`() {
        val pushUp = exercise("pushUp", primaryMuscles = listOf("chest"), equipment = "body only")
        val profile = ProfileDetails(equipment = listOf("dumbbell"))

        assertTrue(isCompliant(pushUp, profile).equipmentAvailable)
    }

    // --- findSubstitutes: kontuzja kolana ---

    @Test
    fun `kontuzja kolana — zgodny zamiennik przed obciążającym kolano`() {
        val squat = exercise("squat", knee = StressLevel.HIGH)
        val legExtension = exercise(
            "legExtension", namePl = "Prostowanie nóg",
            equipment = "machine", mechanic = "isolation", knee = StressLevel.LOW,
        )
        val lunges = exercise(
            "lunges", namePl = "Wykroki", equipment = "body only", knee = StressLevel.HIGH,
        )
        val hipThrust = exercise(
            "hipThrust", namePl = "Hip thrust", primaryMuscles = listOf("glutes"),
        )
        val profile = ProfileDetails(constraints = mapOf("knee" to StressLevel.LOW))

        val result = findSubstitutes(squat, listOf(squat, legExtension, lunges, hipThrust), profile)

        // hipThrust odpada (inna partia główna); zgodny legExtension przed naruszającymi wykrokami
        assertEquals(listOf("legExtension", "lunges"), result.map { it.exercise.id })
        assertTrue(result[0].warnings.isEmpty())
        assertTrue(result[0].score > 0)
        assertEquals(
            listOf(ConstraintViolation("knee", StressLevel.HIGH, StressLevel.LOW)),
            result[1].warnings,
        )
        assertTrue(result[1].score < 0)
    }

    // --- findSubstitutes: L5-S1 ---

    @Test
    fun `L5-S1 — martwy ciąg dostaje zamiennik bez obciążenia dolnego odcinka`() {
        val deadlift = exercise(
            "deadlift", primaryMuscles = listOf("hamstrings"), lowBack = StressLevel.HIGH,
        )
        val legCurl = exercise(
            "legCurl", namePl = "Uginanie nóg", primaryMuscles = listOf("hamstrings"),
            equipment = "machine", mechanic = "isolation",
        )
        val goodMorning = exercise(
            "goodMorning", namePl = "Good morning", primaryMuscles = listOf("hamstrings"),
            lowBack = StressLevel.HIGH,
        )
        val profile = ProfileDetails(constraints = mapOf("lowBack" to StressLevel.NONE))

        val result = findSubstitutes(deadlift, listOf(deadlift, legCurl, goodMorning), profile)

        assertEquals("legCurl", result.first().exercise.id)
        assertTrue(result.first().warnings.isEmpty())
        assertEquals(
            listOf(ConstraintViolation("lowBack", StressLevel.HIGH, StressLevel.NONE)),
            result[1].warnings,
        )
    }

    // --- findSubstitutes: sprzęt ---

    @Test
    fun `brak sztangi w profilu — zostają hantle i masa ciała, sztanga odpada`() {
        val benchPress = exercise("benchPress", primaryMuscles = listOf("chest"))
        val dumbbellPress = exercise(
            "dumbbellPress", primaryMuscles = listOf("chest"), equipment = "dumbbell",
        )
        val pushUp = exercise("pushUp", primaryMuscles = listOf("chest"), equipment = "body only")
        val inclinePress = exercise("inclinePress", primaryMuscles = listOf("chest"))
        val profile = ProfileDetails(equipment = listOf("dumbbell", "machine"))

        val result = findSubstitutes(
            benchPress, listOf(benchPress, dumbbellPress, pushUp, inclinePress), profile,
        )

        val ids = result.map { it.exercise.id }
        assertEquals(setOf("dumbbellPress", "pushUp"), ids.toSet())
    }

    // --- findSubstitutes: pusty profil ---

    @Test
    fun `pusty profil — wszystko dozwolone i bez ostrzeżeń`() {
        val squat = exercise("squat", knee = StressLevel.HIGH, lowBack = StressLevel.MEDIUM)
        val legPress = exercise("legPress", equipment = "machine", knee = StressLevel.MEDIUM)
        val lunges = exercise("lunges", equipment = "body only", knee = StressLevel.HIGH)

        val result = findSubstitutes(squat, listOf(squat, legPress, lunges), ProfileDetails())

        assertEquals(setOf("legPress", "lunges"), result.map { it.exercise.id }.toSet())
        assertTrue(result.all { it.warnings.isEmpty() })
    }

    // --- findSubstitutes: ranking i mechanika ---

    @Test
    fun `bonusy podbijają ten sam mechanic, kategorię i poziom`() {
        val squat = exercise("squat", level = "intermediate")
        val sameProfile = exercise(
            "frontSquat", equipment = "dumbbell", level = "intermediate",
            category = "strength", mechanic = "compound",
        )
        val different = exercise(
            "quadStretch", equipment = "body only", level = "beginner",
            category = "stretching", mechanic = null,
        )

        val result = findSubstitutes(squat, listOf(squat, sameProfile, different), ProfileDetails())

        assertEquals(listOf("frontSquat", "quadStretch"), result.map { it.exercise.id })
        assertTrue(result[0].score > result[1].score)
    }

    @Test
    fun `wyklucza samo ćwiczenie i respektuje limit`() {
        val squat = exercise("squat")
        val others = (1..5).map { exercise("ex$it", namePl = "Ćwiczenie $it") }

        val result = findSubstitutes(squat, listOf(squat) + others, ProfileDetails(), limit = 3)

        assertEquals(3, result.size)
        assertTrue(result.none { it.exercise.id == "squat" })
    }

    @Test
    fun `częściowe pokrycie partii głównych daje niższy wynik niż pełne`() {
        val original = exercise("orig", primaryMuscles = listOf("quadriceps", "glutes"))
        val fullMatch = exercise("full", primaryMuscles = listOf("quadriceps", "glutes"))
        val halfMatch = exercise("half", primaryMuscles = listOf("quadriceps"))

        val result = findSubstitutes(
            original, listOf(original, fullMatch, halfMatch), ProfileDetails(),
        )

        assertEquals(listOf("full", "half"), result.map { it.exercise.id })
    }
}
