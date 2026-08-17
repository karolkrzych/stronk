package com.stronk.data

import org.junit.Assert.assertEquals
import org.junit.Test

/** Testy wyszukiwania (normalizacja diakrytyków) i filtrów listy ćwiczeń. */
class ExerciseFilterTest {

    private fun exercise(
        id: String,
        namePl: String,
        name: String = id,
        primaryMuscles: List<String> = listOf("quadriceps"),
        equipment: String? = "barbell",
        level: String = "beginner",
        category: String = "strength",
    ) = Exercise(
        id = id,
        name = name,
        namePl = namePl,
        instructionsPl = emptyList(),
        primaryMuscles = primaryMuscles,
        secondaryMuscles = emptyList(),
        equipment = equipment,
        level = level,
        category = category,
        mechanic = null,
        force = null,
        images = emptyList(),
        jointStress = JointStress(
            lowBack = StressLevel.NONE, knee = StressLevel.NONE, shoulder = StressLevel.NONE,
            hip = StressLevel.NONE, elbow = StressLevel.NONE, wrist = StressLevel.NONE,
            neck = StressLevel.NONE,
        ),
        measurementType = MeasurementType.WEIGHT_REPS,
    )

    private val squat = exercise("squat", namePl = "Przysiad ze sztangą", name = "Barbell Squat")
    private val crunch = exercise(
        "crunch", namePl = "Ćwiczenie brzucha", name = "Crunch",
        primaryMuscles = listOf("abdominals"), equipment = "body only", category = "strength",
    )
    private val calfRaise = exercise(
        "calf", namePl = "Wspięcia na łydki", name = "Calf Raise",
        primaryMuscles = listOf("calves"), equipment = "machine", level = "intermediate",
    )
    private val all = listOf(squat, crunch, calfRaise)

    @Test
    fun `normalizacja zdejmuje polskie diakrytyki`() {
        assertEquals("cwiczenie", normalizeForSearch("Ćwiczenie"))
        assertEquals("zolta laka", normalizeForSearch("Żółta łąka"))
        assertEquals("lydki", normalizeForSearch("ŁYDKI"))
        assertEquals("gesla jazn", normalizeForSearch("gęślą jaźń"))
    }

    @Test
    fun `wyszukiwanie ignoruje diakrytyki w zapytaniu i w nazwie`() {
        assertEquals(listOf(crunch), filterExercises(all, "cwiczenie"))
        assertEquals(listOf(crunch), filterExercises(all, "ĆWICZENIE"))
        assertEquals(listOf(calfRaise), filterExercises(all, "lydki"))
        assertEquals(listOf(calfRaise), filterExercises(all, "łydki"))
    }

    @Test
    fun `wyszukiwanie działa też po nazwie oryginalnej`() {
        assertEquals(listOf(squat), filterExercises(all, "barbell sq"))
    }

    @Test
    fun `puste zapytanie zwraca wszystko`() {
        assertEquals(all, filterExercises(all, ""))
        assertEquals(all, filterExercises(all, "   "))
    }

    @Test
    fun `filtr partii mięśniowej patrzy na partie główne`() {
        val result = filterExercises(all, "", ExerciseFilters(muscle = "abdominals"))
        assertEquals(listOf(crunch), result)
    }

    @Test
    fun `filtr sprzętu i poziomu`() {
        assertEquals(
            listOf(calfRaise),
            filterExercises(all, "", ExerciseFilters(equipment = "machine")),
        )
        assertEquals(
            listOf(calfRaise),
            filterExercises(all, "", ExerciseFilters(level = "intermediate")),
        )
    }

    @Test
    fun `filtry łączą się z wyszukiwaniem (AND)`() {
        // zapytanie pasuje do squat i crunch po EN/PL, ale filtr sprzętu zawęża
        val result = filterExercises(all, "c", ExerciseFilters(equipment = "body only"))
        assertEquals(listOf(crunch), result)
    }

    @Test
    fun `filtr kategorii`() {
        val stretch = exercise("str", namePl = "Skłon", category = "stretching")
        val result = filterExercises(all + stretch, "", ExerciseFilters(category = "stretching"))
        assertEquals(listOf(stretch), result)
    }
}
