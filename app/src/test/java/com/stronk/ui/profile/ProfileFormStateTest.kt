package com.stronk.ui.profile

import com.stronk.data.Exercise
import com.stronk.data.JointStress
import com.stronk.data.MeasurementType
import com.stronk.data.ProfileDetails
import com.stronk.data.StressLevel
import com.stronk.data.TrainingGoal
import com.stronk.data.UserProfile
import com.stronk.data.isCompliant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testy czystej logiki formularza profilu: mapowanie form ↔ UserProfile,
 * kodowanie "brak ograniczenia" jako HIGH (merge nie usuwa kluczy mapy)
 * i zgodność listy stawów z realnym modelem.
 */
class ProfileFormStateTest {

    @Test
    fun `JOINT_KEYS pokrywa dokładnie stawy z modelu JointStress`() {
        val allNone = JointStress(
            lowBack = StressLevel.NONE, knee = StressLevel.NONE, shoulder = StressLevel.NONE,
            hip = StressLevel.NONE, elbow = StressLevel.NONE, wrist = StressLevel.NONE,
            neck = StressLevel.NONE,
        )
        assertEquals(allNone.all.keys, ProfileDefaults.JOINT_KEYS.toSet())
        assertEquals(allNone.all.size, ProfileDefaults.JOINT_KEYS.size)
    }

    @Test
    fun `brak dokumentu profilu daje puste defaulty`() {
        assertEquals(ProfileFormState(), profileFormFrom(null))
    }

    @Test
    fun `zapis koduje wszystkie stawy — limity wprost, reszta jako HIGH`() {
        val form = ProfileFormState(
            constraints = mapOf(
                "knee" to StressLevel.LOW,
                "lowBack" to StressLevel.MEDIUM,
            ),
        )

        val saved = form.toUserProfile(createdAt = 123L)

        assertEquals(123L, saved.createdAt)
        assertEquals(ProfileDefaults.JOINT_KEYS.toSet(), saved.profile.constraints.keys)
        assertEquals(StressLevel.LOW, saved.profile.constraints["knee"])
        assertEquals(StressLevel.MEDIUM, saved.profile.constraints["lowBack"])
        listOf("shoulder", "hip", "elbow", "wrist", "neck").forEach { joint ->
            assertEquals(StressLevel.HIGH, saved.profile.constraints[joint])
        }
    }

    @Test
    fun `odczyt mapuje HIGH na brak wpisu a NONE na LOW`() {
        val profile = UserProfile(
            createdAt = 1L,
            profile = ProfileDetails(
                constraints = mapOf(
                    "knee" to StressLevel.HIGH,
                    "wrist" to StressLevel.NONE,
                    "lowBack" to StressLevel.MEDIUM,
                ),
            ),
        )

        val form = profileFormFrom(profile)

        assertEquals(
            mapOf("wrist" to StressLevel.LOW, "lowBack" to StressLevel.MEDIUM),
            form.constraints,
        )
    }

    @Test
    fun `imię jest przycinane a puste zapisywane jako pusty string`() {
        // "" (nie null), żeby SetOptions.merge() nadpisał starą wartość na serwerze.
        assertEquals("Karol", ProfileFormState(displayName = " Karol ").toUserProfile(1L).displayName)
        assertEquals("", ProfileFormState(displayName = "   ").toUserProfile(1L).displayName)
    }

    @Test
    fun `sprzęt na wire jest posortowany deterministycznie`() {
        val saved = ProfileFormState(equipment = setOf("dumbbell", "barbell")).toUserProfile(1L)
        assertEquals(listOf("barbell", "dumbbell"), saved.profile.equipment)
    }

    @Test
    fun `round-trip formularza przez UserProfile zachowuje wszystkie pola`() {
        val form = ProfileFormState(
            displayName = "Karol",
            equipment = setOf("barbell", "dumbbell"),
            constraints = mapOf("knee" to StressLevel.LOW, "lowBack" to StressLevel.MEDIUM),
            goal = TrainingGoal.RETURN_TO_FORM,
            returningFromBreak = true,
        )

        assertEquals(form, profileFormFrom(form.toUserProfile(createdAt = 5L)))
    }

    @Test
    fun `wpis HIGH w constraints nie flaguje żadnego ćwiczenia`() {
        // Staw bez ograniczenia (wire HIGH) musi przepuszczać nawet ćwiczenia HIGH.
        val squat = Exercise(
            id = "squat", name = "Squat", namePl = "Przysiad",
            instructionsPl = emptyList(),
            primaryMuscles = listOf("quadriceps"), secondaryMuscles = emptyList(),
            equipment = "barbell", level = "beginner", category = "strength",
            mechanic = "compound", force = null, images = emptyList(),
            jointStress = JointStress(
                lowBack = StressLevel.HIGH, knee = StressLevel.HIGH,
                shoulder = StressLevel.NONE, hip = StressLevel.NONE,
                elbow = StressLevel.NONE, wrist = StressLevel.NONE,
                neck = StressLevel.NONE,
            ),
            measurementType = MeasurementType.WEIGHT_REPS,
        )
        val saved = ProfileFormState().toUserProfile(createdAt = 1L)

        assertTrue(isCompliant(squat, saved.profile).isFullyCompliant)
    }
}
