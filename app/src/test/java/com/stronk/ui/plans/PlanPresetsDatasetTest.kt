package com.stronk.ui.plans

import com.stronk.data.Exercise
import com.stronk.data.ProfileDetails
import com.stronk.data.StressLevel
import com.stronk.data.StronkJson
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testy integralności DANYCH presetów z realnym bundlowanym datasetem —
 * literówka w exerciseId ma wywalić build, nie ciche puste sloty w runtime.
 */
class PlanPresetsDatasetTest {

    private val exercisesById: Map<String, Exercise> by lazy {
        // Unit testy AGP startują w katalogu modułu; fallback dla startu z korzenia repo.
        val file = listOf(
            File("src/main/assets/exercises.json"),
            File("app/src/main/assets/exercises.json"),
        ).firstOrNull { it.exists() }
            ?: error("Nie znaleziono exercises.json względem katalogu roboczego testów")
        StronkJson.decodeFromString<List<Exercise>>(file.readText(Charsets.UTF_8))
            .associateBy { it.id }
    }

    @Test
    fun `kazdy kandydat kazdego slotu istnieje w datasecie`() {
        val missing = PlanPresets.all.flatMap { preset ->
            preset.days.flatMap { day ->
                day.slots.flatMap { slot ->
                    slot.candidateIds
                        .filterNot { it in exercisesById }
                        .map { "${preset.id} / ${day.name} / ${slot.label}: $it" }
                }
            }
        }
        assertEquals("Kandydaci spoza datasetu: $missing", emptyList<String>(), missing)
    }

    @Test
    fun `sloty maja sensowne wartosci startowe`() {
        PlanPresets.all.forEach { preset ->
            assertTrue("Preset ${preset.id} bez dni", preset.days.isNotEmpty())
            preset.days.forEach { day ->
                assertTrue("Pusty dzień w ${preset.id}", day.slots.isNotEmpty())
                assertTrue("Dzień bez nazwy w ${preset.id}", day.name.isNotBlank())
                day.slots.forEach { slot ->
                    assertTrue("Slot bez kandydatów: ${slot.label}", slot.candidateIds.isNotEmpty())
                    assertTrue("Serie poza zakresem: ${slot.label}", slot.sets in 1..PlanDefaults.SETS_MAX)
                    assertTrue("Powtórzenia niedodatnie: ${slot.label}", slot.reps >= 1)
                    assertEquals(
                        "Duplikaty kandydatów: ${slot.label}",
                        slot.candidateIds.size,
                        slot.candidateIds.distinct().size,
                    )
                }
            }
        }
    }

    @Test
    fun `id presetow sa unikalne`() {
        val ids = PlanPresets.all.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun `preset powrotowy generuje sie w calosci przy pustym profilu`() {
        val all = exercisesById.values.toList()
        val days = generatePresetDays(PlanPresets.fullBodyReturn, all, ProfileDetails())
        // Pusty profil = wszystko dostępne → każdy slot ma obsadę.
        assertEquals(PlanPresets.fullBodyReturn.days.size, days.size)
        days.zip(PlanPresets.fullBodyReturn.days).forEach { (generated, definition) ->
            assertEquals(
                "Dzień ${definition.name} zgubił sloty",
                definition.slots.size,
                generated.exercises.size,
            )
        }
    }

    @Test
    fun `preset powrotowy z profilem Karola nie proponuje wysokich obciazen kolana i L5-S1`() {
        // Kryterium sukcesu alfy: kolano (łąkotka) + dolny odcinek pleców (L5-S1).
        val profile = ProfileDetails(
            constraints = mapOf(
                "knee" to StressLevel.LOW,
                "lowBack" to StressLevel.LOW,
            ),
        )
        val all = exercisesById.values.toList()
        val days = generatePresetDays(PlanPresets.fullBodyReturn, all, profile)
        val highStress = days.flatMap { it.exercises }.mapNotNull { planExercise ->
            val exercise = exercisesById[planExercise.exerciseId] ?: return@mapNotNull null
            val knee = exercise.jointStress.knee
            val lowBack = exercise.jointStress.lowBack
            if (knee == StressLevel.HIGH || lowBack == StressLevel.HIGH) {
                "${exercise.id} (knee=$knee, lowBack=$lowBack)"
            } else {
                null
            }
        }
        assertEquals(
            "Preset powrotowy proponuje HIGH dla kolana/L5-S1: $highStress",
            emptyList<String>(),
            highStress,
        )
    }
}
