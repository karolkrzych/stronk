package com.stronk.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Testy deserializacji [Exercise] z próbki JSON o schemacie datasetu. */
class ExerciseSerializationTest {

    private val sampleJson = """
        {
         "id": "Barbell_Squat",
         "name": "Barbell Squat",
         "namePl": "Przysiad ze sztangą",
         "instructionsPl": ["Ustaw sztangę na plecach.", "Wykonaj przysiad."],
         "primaryMuscles": ["quadriceps"],
         "secondaryMuscles": ["glutes", "hamstrings"],
         "equipment": "barbell",
         "level": "intermediate",
         "category": "strength",
         "mechanic": "compound",
         "force": "push",
         "images": ["Barbell_Squat/0.jpg", "Barbell_Squat/1.jpg"],
         "jointStress": {
          "lowBack": "high",
          "knee": "medium",
          "shoulder": "low",
          "hip": "medium",
          "elbow": "none",
          "wrist": "low",
          "neck": "none"
         },
         "cautionNotes": "Przy problemach z odcinkiem lędźwiowym zacznij od goblet squat.",
         "measurementType": "WEIGHT_REPS"
        }
    """.trimIndent()

    @Test
    fun `deserializuje pełny wpis ćwiczenia`() {
        val exercise = StronkJson.decodeFromString<Exercise>(sampleJson)

        assertEquals("Barbell_Squat", exercise.id)
        assertEquals("Przysiad ze sztangą", exercise.namePl)
        assertEquals("Barbell Squat", exercise.name)
        assertEquals(2, exercise.instructionsPl.size)
        assertEquals(listOf("quadriceps"), exercise.primaryMuscles)
        assertEquals("barbell", exercise.equipment)
        assertEquals(MeasurementType.WEIGHT_REPS, exercise.measurementType)
        assertEquals(StressLevel.HIGH, exercise.jointStress.lowBack)
        assertEquals(StressLevel.MEDIUM, exercise.jointStress.knee)
        assertEquals(StressLevel.NONE, exercise.jointStress.neck)
        assertEquals(
            "Przy problemach z odcinkiem lędźwiowym zacznij od goblet squat.",
            exercise.cautionNotes,
        )
        assertTrue(exercise.hasHighJointStress)
    }

    @Test
    fun `pola opcjonalne mogą być null lub nieobecne`() {
        val json = """
            {
             "id": "Air_Bike",
             "name": "Air Bike",
             "namePl": "Rowerek",
             "instructionsPl": ["Krok pierwszy."],
             "primaryMuscles": ["abdominals"],
             "secondaryMuscles": [],
             "equipment": null,
             "level": "beginner",
             "category": "strength",
             "mechanic": null,
             "force": null,
             "images": ["Air_Bike/0.jpg"],
             "jointStress": {
              "lowBack": "low", "knee": "none", "shoulder": "none",
              "hip": "low", "elbow": "none", "wrist": "none", "neck": "low"
             },
             "measurementType": "REPS"
            }
        """.trimIndent()

        val exercise = StronkJson.decodeFromString<Exercise>(json)

        assertNull(exercise.equipment)
        assertNull(exercise.mechanic)
        assertNull(exercise.force)
        assertNull(exercise.cautionNotes)
        assertEquals(MeasurementType.REPS, exercise.measurementType)
        assertEquals(false, exercise.hasHighJointStress)
    }

    @Test
    fun `deserializuje listę ćwiczeń`() {
        val list = StronkJson.decodeFromString<List<Exercise>>("[$sampleJson]")
        assertEquals(1, list.size)
        assertEquals("Barbell_Squat", list[0].id)
    }
}
