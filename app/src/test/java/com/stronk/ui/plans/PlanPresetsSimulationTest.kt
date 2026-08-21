package com.stronk.ui.plans

import com.stronk.data.Exercise
import com.stronk.data.ProfileDetails
import com.stronk.data.StressLevel
import com.stronk.data.StronkJson
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Symulacje z sekcji 3 rozpiski presetów v2 (2026-08) jako asercje: 3 presety ×
 * 3 profile [(a) pełna siłownia bez kontuzji, (b) home gym (dumbbell+bands+body
 * only) bez kontuzji, (c) pełna siłownia + knee=MEDIUM/lowBack=MEDIUM] na
 * realnym bundlowanym datasecie — dokładnie ten sam sposób sprawdzania jak
 * [PlanPresetsDatasetTest], żeby literówka w ID wywalała testy, nie ciche
 * puste sloty w runtime.
 *
 * Profil (b) w rozpisce ma JEDEN udokumentowany przypadek, gdzie dedup między
 * dniami produkuje inny wynik niż "surowa" (przed-dedupem) wartość w tabeli
 * rozpiski — oznaczony tam gwiazdką: fullBodyReturn, dzień B, slot "Klatka —
 * wyciskanie" — tabela pokazuje "Wyciskanie hantli leżąc" (bo to naturalny,
 * przed-dedupem wybór), ale w praktyce ten kandydat jest już użyty w dniu A,
 * więc dedup przesuwa dzień B na kolejnego kandydata z dostępnym sprzętem —
 * przy home gym bez maszyn ląduje na "Pushups" (rozpiska: "zejdzie na maszynę
 * / pompki"). Test na tę komórkę asercjuje POST-dedup wartość (Pushups), nie
 * surową wartość z tabeli — zgodnie z zadaniem ("dopuszczalna udokumentowana
 * różnica w miejscu duplikatu z gwiazdką").
 */
class PlanPresetsSimulationTest {

    private val exercisesById: Map<String, Exercise> by lazy {
        val file = listOf(
            File("src/main/assets/exercises.json"),
            File("app/src/main/assets/exercises.json"),
        ).firstOrNull { it.exists() }
            ?: error("Nie znaleziono exercises.json względem katalogu roboczego testów")
        StronkJson.decodeFromString<List<Exercise>>(file.readText(Charsets.UTF_8))
            .associateBy { it.id }
    }

    private val allExercises by lazy { exercisesById.values.toList() }

    /** (a) pełna siłownia, bez kontuzji — wszystkie podtypy sprzętu z datasetu. */
    private val fullGym = ProfileDetails(
        equipment = listOf(
            "barbell", "dumbbell", "machine", "leg machine", "leverage machine",
            "smith machine", "cable", "kettlebells", "bands", "body only",
            "exercise ball", "cardio machine", "e-z curl bar", "medicine ball",
            "foam roll", "other",
        ),
    )

    /** (b) home gym: hantle + guma + masa własna, bez kontuzji. */
    private val homeGym = ProfileDetails(equipment = listOf("dumbbell", "bands", "body only"))

    /** (c) pełna siłownia + kolano/L5-S1 (próg z kreatora, WIZARD_CONSTRAINT_LEVEL = MEDIUM). */
    private val fullGymInjured = fullGym.copy(
        constraints = mapOf("knee" to StressLevel.MEDIUM, "lowBack" to StressLevel.MEDIUM),
    )

    private fun generate(preset: PlanPreset, profile: ProfileDetails): List<List<String>> =
        generatePresetDays(preset, allExercises, profile).map { day -> day.exercises.map { it.exerciseId } }

    private fun assertDays(label: String, expected: List<List<String>>, actual: List<List<String>>) {
        assertEquals("$label — liczba dni", expected.size, actual.size)
        expected.forEachIndexed { i, expectedDay ->
            assertEquals("$label — dzień ${i + 1}", expectedDay, actual[i])
        }
    }

    // --- fullBodyReturn ---

    @Test
    fun `fullBodyReturn (a) pelna silownia bez kontuzji`() {
        assertDays(
            "fullBodyReturn (a)",
            listOf(
                listOf(
                    "Barbell_Squat", "Seated_Leg_Curl", "Barbell_Bench_Press_-_Medium_Grip",
                    "Bent_Over_Barbell_Row", "Barbell_Shoulder_Press", "Dead_Bug",
                    "Standing_Dumbbell_Triceps_Extension",
                ),
                listOf(
                    "Barbell_Hip_Thrust", "Lying_Leg_Curls", "Wide-Grip_Lat_Pulldown",
                    "Dumbbell_Bench_Press", "Standing_Barbell_Calf_Raise", "Pallof_Press", "Barbell_Curl",
                ),
                listOf(
                    "Goblet_Squat", "Romanian_Deadlift", "One-Arm_Dumbbell_Row",
                    "Side_Lateral_Raise", "Dumbbell_Bicep_Curl", "Triceps_Pushdown",
                ),
            ),
            generate(PlanPresets.fullBodyReturn, fullGym),
        )
    }

    @Test
    fun `fullBodyReturn (b) home gym bez kontuzji, dedup na dzien B (gwiazdka rozpiski)`() {
        assertDays(
            "fullBodyReturn (b)",
            listOf(
                listOf(
                    "Dumbbell_Squat", "Stiff-Legged_Dumbbell_Deadlift", "Dumbbell_Bench_Press",
                    "Dumbbell_Incline_Row", "Dumbbell_Shoulder_Press", "Dead_Bug",
                    "Standing_Dumbbell_Triceps_Extension",
                ),
                listOf(
                    "Hip_Extension_with_Bands", "Natural_Glute_Ham_Raise", "Chin-Up",
                    // Rozpiska (tabela, przed dedupem): "Wyciskanie hantli leżąc" (Dumbbell_Bench_Press) —
                    // już użyte w dniu A, dedup przesuwa na Pushups (brak "machine" w sprzęcie home gym).
                    "Pushups",
                    "Standing_Dumbbell_Calf_Raise", "Side_Bridge", "Hammer_Curls",
                ),
                listOf(
                    "Bodyweight_Squat", "Butt_Lift_Bridge", "One-Arm_Dumbbell_Row",
                    "Side_Lateral_Raise", "Dumbbell_Bicep_Curl", "Band_Skull_Crusher",
                ),
            ),
            generate(PlanPresets.fullBodyReturn, homeGym),
        )
    }

    @Test
    fun `fullBodyReturn (c) pelna silownia + kolano lowBack medium`() {
        assertDays(
            "fullBodyReturn (c)",
            listOf(
                listOf(
                    "Leg_Press", "Seated_Leg_Curl", "Barbell_Bench_Press_-_Medium_Grip",
                    "Leverage_Iso_Row", "Barbell_Shoulder_Press", "Dead_Bug",
                    "Standing_Dumbbell_Triceps_Extension",
                ),
                // "identyczny jak (a)" per rozpiska — nic w dniu B nie dotyka progu MEDIUM.
                listOf(
                    "Barbell_Hip_Thrust", "Lying_Leg_Curls", "Wide-Grip_Lat_Pulldown",
                    "Dumbbell_Bench_Press", "Standing_Barbell_Calf_Raise", "Pallof_Press", "Barbell_Curl",
                ),
                listOf(
                    "Goblet_Squat", "Glute_Ham_Raise", "One-Arm_Dumbbell_Row",
                    "Side_Lateral_Raise", "Dumbbell_Bicep_Curl", "Triceps_Pushdown",
                ),
            ),
            generate(PlanPresets.fullBodyReturn, fullGymInjured),
        )
    }

    // --- pushPullLegs ---

    @Test
    fun `pushPullLegs (a) pelna silownia bez kontuzji`() {
        assertDays(
            "pushPullLegs (a)",
            listOf(
                listOf(
                    "Barbell_Bench_Press_-_Medium_Grip", "Barbell_Incline_Bench_Press_-_Medium_Grip",
                    "Barbell_Shoulder_Press", "Side_Lateral_Raise", "Triceps_Pushdown",
                ),
                listOf(
                    "Bent_Over_Barbell_Row", "Wide-Grip_Lat_Pulldown", "Dumbbell_Lying_Rear_Lateral_Raise",
                    "Barbell_Curl", "Good_Morning",
                ),
                listOf(
                    "Barbell_Squat", "Romanian_Deadlift", "Dumbbell_Lunges", "Lying_Leg_Curls",
                    "Standing_Barbell_Calf_Raise", "Dead_Bug", "Hammer_Curls",
                ),
            ),
            generate(PlanPresets.pushPullLegs, fullGym),
        )
    }

    @Test
    fun `pushPullLegs (b) home gym bez kontuzji`() {
        assertDays(
            "pushPullLegs (b)",
            listOf(
                listOf(
                    "Dumbbell_Bench_Press", "Hammer_Grip_Incline_DB_Bench_Press",
                    "Dumbbell_Shoulder_Press", "Side_Lateral_Raise", "Standing_Dumbbell_Triceps_Extension",
                ),
                listOf(
                    "One-Arm_Dumbbell_Row", "Pullups", "Dumbbell_Lying_Rear_Lateral_Raise",
                    "Dumbbell_Bicep_Curl", "Hyperextensions_With_No_Hyperextension_Bench",
                ),
                listOf(
                    "Dumbbell_Squat", "Stiff-Legged_Dumbbell_Deadlift", "Dumbbell_Lunges",
                    "Natural_Glute_Ham_Raise", "Standing_Dumbbell_Calf_Raise", "Dead_Bug", "Hammer_Curls",
                ),
            ),
            generate(PlanPresets.pushPullLegs, homeGym),
        )
    }

    @Test
    fun `pushPullLegs (c) pelna silownia + kolano lowBack medium`() {
        assertDays(
            "pushPullLegs (c)",
            listOf(
                // "identyczny jak (a)" per rozpiska (brak ograniczenia barku).
                listOf(
                    "Barbell_Bench_Press_-_Medium_Grip", "Barbell_Incline_Bench_Press_-_Medium_Grip",
                    "Barbell_Shoulder_Press", "Side_Lateral_Raise", "Triceps_Pushdown",
                ),
                listOf(
                    "Leverage_Iso_Row", "Wide-Grip_Lat_Pulldown", "Dumbbell_Lying_Rear_Lateral_Raise",
                    "Barbell_Curl", "Hyperextensions_With_No_Hyperextension_Bench",
                ),
                listOf(
                    "Leg_Press", "Barbell_Hip_Thrust", "Leg_Extensions", "Lying_Leg_Curls",
                    "Standing_Barbell_Calf_Raise", "Dead_Bug", "Hammer_Curls",
                ),
            ),
            generate(PlanPresets.pushPullLegs, fullGymInjured),
        )
    }

    // --- fullBodyTwice ---

    @Test
    fun `fullBodyTwice (a) pelna silownia bez kontuzji`() {
        assertDays(
            "fullBodyTwice (a)",
            listOf(
                listOf(
                    "Barbell_Squat", "Barbell_Bench_Press_-_Medium_Grip", "Bent_Over_Barbell_Row",
                    "Side_Lateral_Raise", "Dead_Bug", "Standing_Dumbbell_Triceps_Extension",
                ),
                listOf(
                    "Romanian_Deadlift", "Standing_Military_Press", "Wide-Grip_Lat_Pulldown",
                    "Barbell_Curl", "Triceps_Pushdown",
                ),
            ),
            generate(PlanPresets.fullBodyTwice, fullGym),
        )
    }

    @Test
    fun `fullBodyTwice (b) home gym bez kontuzji`() {
        assertDays(
            "fullBodyTwice (b)",
            listOf(
                listOf(
                    "Dumbbell_Squat", "Dumbbell_Bench_Press", "One-Arm_Dumbbell_Row",
                    "Side_Lateral_Raise", "Dead_Bug", "Standing_Dumbbell_Triceps_Extension",
                ),
                listOf(
                    "Stiff-Legged_Dumbbell_Deadlift", "Dumbbell_Shoulder_Press", "Pullups",
                    "Dumbbell_Bicep_Curl", "Bench_Dips",
                ),
            ),
            generate(PlanPresets.fullBodyTwice, homeGym),
        )
    }

    @Test
    fun `fullBodyTwice (c) pelna silownia + kolano lowBack medium`() {
        assertDays(
            "fullBodyTwice (c)",
            listOf(
                listOf(
                    "Leg_Press", "Barbell_Bench_Press_-_Medium_Grip", "Seated_Cable_Rows",
                    "Side_Lateral_Raise", "Dead_Bug", "Standing_Dumbbell_Triceps_Extension",
                ),
                listOf(
                    "Barbell_Hip_Thrust", "Standing_Military_Press", "Wide-Grip_Lat_Pulldown",
                    "Barbell_Curl", "Triceps_Pushdown",
                ),
            ),
            generate(PlanPresets.fullBodyTwice, fullGymInjured),
        )
    }

    // --- zero pustych slotów przy profilu (b) we wszystkich presetach (zarzut z v1) ---

    @Test
    fun `home gym nie generuje pustych slotow w zadnym presecie`() {
        PlanPresets.all.forEach { preset ->
            val days = generatePresetDays(preset, allExercises, homeGym)
            days.zip(preset.days).forEach { (generated, definition) ->
                assertEquals(
                    "Preset ${preset.id}, dzień ${definition.name}: puste sloty przy home gym",
                    definition.slots.size,
                    generated.exercises.size,
                )
            }
        }
    }
}
