package com.stronk.ui.home

import com.stronk.data.Exercise
import com.stronk.data.JointStress
import com.stronk.data.MeasurementType
import com.stronk.data.Plan
import com.stronk.data.PlanDay
import com.stronk.data.PlanExercise
import com.stronk.data.ScheduleEntry
import com.stronk.data.ScheduleStatus
import com.stronk.data.SetTarget
import com.stronk.data.StressLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Mapowanie planu i harmonogramu na model ekranu „Dziś": wybór wpisu na dziś,
 * liczniki pod CTA i belką ukończenia oraz dni planu pod arkusz S2.
 */
class HomeMappingTest {

    private val today = "2026-08-21"

    // ---------- wybór wpisów harmonogramu ----------

    @Test
    fun `zaplanowany trening na dzis wygrywa z pozostalymi`() {
        val entries = HomeMapping.selectEntries(
            schedule = listOf(
                entry("a", today, ScheduleStatus.PLANNED),
                entry("b", "2026-08-23", ScheduleStatus.PLANNED),
            ),
            todayKey = today,
        )
        assertEquals("a", entries.today?.id)
        assertEquals("b", entries.upcoming?.id)
        assertNull(entries.todayDone)
    }

    @Test
    fun `zrobiony dzisiejszy trening wraca osobno, nie jako zaplanowany`() {
        val entries = HomeMapping.selectEntries(
            schedule = listOf(entry("a", today, ScheduleStatus.DONE)),
            todayKey = today,
        )
        assertNull(entries.today)
        assertEquals("a", entries.todayDone?.id)
    }

    @Test
    fun `najblizszy trening to pierwszy PLANNED po dzis`() {
        val entries = HomeMapping.selectEntries(
            schedule = listOf(
                entry("wczoraj", "2026-08-20", ScheduleStatus.PLANNED),
                entry("odwolany", "2026-08-22", ScheduleStatus.SKIPPED),
                entry("nastepny", "2026-08-23", ScheduleStatus.PLANNED),
                entry("dalszy", "2026-08-25", ScheduleStatus.PLANNED),
            ),
            todayKey = today,
        )
        assertNull(entries.today)
        assertEquals("nastepny", entries.upcoming?.id)
    }

    @Test
    fun `pusty harmonogram nie daje zadnego wpisu`() {
        val entries = HomeMapping.selectEntries(emptyList(), today)
        assertNull(entries.today)
        assertNull(entries.todayDone)
        assertNull(entries.upcoming)
    }

    // ---------- liczniki CTA / belki ukończenia ----------

    @Test
    fun `liczba serii dnia to suma serii cwiczen`() {
        assertEquals(18, HomeMapping.setCount(plan().days[2]))
        assertEquals(0, HomeMapping.setCount(PlanDay(name = "Pusty")))
    }

    @Test
    fun `wiersze cwiczen biora polska nazwe i chip serii`() {
        val rows = HomeMapping.exerciseRows(plan().days[0], exercises)
        assertEquals(listOf("Przysiad", "Wiosłowanie"), rows.map { it.name })
        assertEquals(listOf("3 serie", "4 serie"), rows.map { it.setsChip })
        assertEquals(listOf("quadriceps", "lats"), rows.map { it.muscleKey })
    }

    @Test
    fun `cwiczenie spoza datasetu spada do id, a nie do pustej nazwy`() {
        val day = PlanDay(name = "Dziwny", exercises = listOf(planExercise("Duch", sets = 2)))
        val row = HomeMapping.exerciseRows(day, exercises).single()
        assertEquals("Duch", row.name)
        assertNull(row.muscleKey)
        assertEquals("2 serie", row.setsChip)
    }

    // ---------- dni planu pod arkusz „Szczegóły planu" ----------

    @Test
    fun `przeglad planu wiezie wszystkie dni z pelnymi listami`() {
        val overview = HomeMapping.planOverview(plan(), currentDayIndex = 2, exercises = exercises)
        assertEquals("plan-1", overview.planId)
        assertEquals("Full Body 3×/tydz. (powrót po przerwie)", overview.name)
        assertEquals(listOf("Full body A", "Full body B", "Full body C"), overview.days.map { it.name })
        assertEquals(listOf(0, 1, 2), overview.days.map { it.dayIndex })
        assertEquals(listOf(2, 1, 6), overview.days.map { it.exercises.size })
    }

    @Test
    fun `dzisiejszy dzien jest oznaczony dokladnie jeden raz`() {
        val overview = HomeMapping.planOverview(plan(), currentDayIndex = 2, exercises = exercises)
        assertEquals(listOf(false, false, true), overview.days.map { it.current })
    }

    @Test
    fun `bez biezacego dnia zaden nie dostaje krechy`() {
        val overview = HomeMapping.planOverview(plan(), currentDayIndex = null, exercises = exercises)
        assertTrue(overview.days.none { it.current })
    }

    @Test
    fun `plan bez dni daje pusty przeglad`() {
        val empty = Plan(id = "p", name = "Pusty", createdAt = 0L)
        assertTrue(HomeMapping.planOverview(empty, null, exercises).days.isEmpty())
    }

    // ---------- licznik „+N" na karcie dnia ----------

    @Test
    fun `licznik ukrytych miniatur liczy tylko nadmiar`() {
        assertEquals(0, HomeMapping.hiddenCount(0))
        assertEquals(0, HomeMapping.hiddenCount(3))
        assertEquals(3, HomeMapping.hiddenCount(6))
    }

    @Test
    fun `karta dnia z trzema cwiczeniami nie pokazuje kwadracika`() {
        val overview = HomeMapping.planOverview(plan(), currentDayIndex = 0, exercises = exercises)
        assertFalse(HomeMapping.hiddenCount(overview.days[0].exercises.size) > 0)
        assertTrue(HomeMapping.hiddenCount(overview.days[2].exercises.size) > 0)
    }

    // ---------- fixtures ----------

    private fun entry(id: String, date: String, status: ScheduleStatus) = ScheduleEntry(
        id = id,
        date = date,
        planId = "plan-1",
        dayIndex = 0,
        status = status,
    )

    private fun planExercise(exerciseId: String, sets: Int) = PlanExercise(
        exerciseId = exerciseId,
        sets = sets,
        target = SetTarget.WeightReps(reps = 10),
    )

    private fun plan() = Plan(
        id = "plan-1",
        name = "Full Body 3×/tydz. (powrót po przerwie)",
        createdAt = 0L,
        blockLengthWeeks = 5,
        days = listOf(
            PlanDay(
                name = "Full body A",
                exercises = listOf(
                    planExercise("Squat", sets = 3),
                    planExercise("Row", sets = 4),
                ),
            ),
            PlanDay(name = "Full body B", exercises = listOf(planExercise("Squat", sets = 5))),
            PlanDay(
                name = "Full body C",
                exercises = List(6) { planExercise("Squat", sets = 3) },
            ),
        ),
    )

    private val exercises = listOf(
        exercise(id = "Squat", namePl = "Przysiad", muscle = "quadriceps"),
        exercise(id = "Row", namePl = "Wiosłowanie", muscle = "lats"),
    ).associateBy { it.id }

    private fun exercise(id: String, namePl: String, muscle: String) = Exercise(
        id = id,
        name = id,
        namePl = namePl,
        instructionsPl = emptyList(),
        primaryMuscles = listOf(muscle),
        secondaryMuscles = emptyList(),
        level = "beginner",
        category = "strength",
        images = emptyList(),
        jointStress = JointStress(
            lowBack = StressLevel.NONE,
            knee = StressLevel.NONE,
            shoulder = StressLevel.NONE,
            hip = StressLevel.NONE,
            elbow = StressLevel.NONE,
            wrist = StressLevel.NONE,
            neck = StressLevel.NONE,
        ),
        measurementType = MeasurementType.WEIGHT_REPS,
    )
}
