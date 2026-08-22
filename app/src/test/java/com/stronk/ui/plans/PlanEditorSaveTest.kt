package com.stronk.ui.plans

import com.stronk.data.Plan
import com.stronk.data.PlanDay
import com.stronk.data.PlanExercise
import com.stronk.data.SetTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [buildPlanForSave] — bug 1: [PlanEditorViewModel.save] budowal [Plan] od zera
 * i gubil pola nieedytowane w tym ekranie (glownie [Plan.weekdayAssignments] —
 * wzorzec dni tygodnia z dialogu planowania), bo `.set()` bez merge razem z
 * `planToMap` pomijajacym nulle kasowaly pole w Firestore przy KAZDYM zapisie
 * z edytora. Testy pokrywaja round-trip zachowania tych pol.
 */
class PlanEditorSaveTest {

    private val exercise = PlanExercise(
        exerciseId = "Barbell_Squat",
        sets = 3,
        target = SetTarget.WeightReps(reps = 8),
    )
    private val days = listOf(PlanDay(name = "Dzień A", exercises = listOf(exercise)))

    private val existingPlan = Plan(
        id = "plan-1",
        name = "Push / Pull / Legs",
        createdAt = 111L,
        archived = false,
        blockLengthWeeks = 5,
        weekdayAssignments = mapOf(1 to 0, 3 to 1, 5 to 2), // pn/sr/pt
        days = listOf(PlanDay(name = "Stara nazwa", exercises = emptyList())),
    )

    @Test
    fun `zapis istniejacego planu zachowuje weekdayAssignments mimo ze edytor go nie edytuje`() {
        val saved = buildPlanForSave(
            base = existingPlan,
            name = existingPlan.name,
            blockLengthWeeks = existingPlan.blockLengthWeeks,
            days = days,
            newId = { error("nie powinno byc wolane dla istniejacego planu") },
        )
        assertEquals(existingPlan.weekdayAssignments, saved.weekdayAssignments)
    }

    @Test
    fun `zapis istniejacego planu zachowuje id, createdAt i archived z base`() {
        val archivedBase = existingPlan.copy(archived = true)
        val saved = buildPlanForSave(
            base = archivedBase,
            name = "Nowa nazwa",
            blockLengthWeeks = archivedBase.blockLengthWeeks,
            days = days,
            newId = { error("nie powinno byc wolane") },
        )
        assertEquals(archivedBase.id, saved.id)
        assertEquals(archivedBase.createdAt, saved.createdAt)
        assertEquals(true, saved.archived)
        assertEquals("Nowa nazwa", saved.name)
    }

    @Test
    fun `zapis zmienia tylko edytowalne pola - blockLengthWeeks i days`() {
        val saved = buildPlanForSave(
            base = existingPlan,
            name = existingPlan.name,
            blockLengthWeeks = null, // user wylaczyl blok w edytorze
            days = days,
            newId = { error("nie powinno byc wolane") },
        )
        assertNull(saved.blockLengthWeeks)
        assertEquals(days.map { it.name }, saved.days.map { it.name })
        // Pole nieedytowane w tym ekranie - nietkniete mimo zmiany bloku obok.
        assertEquals(existingPlan.weekdayAssignments, saved.weekdayAssignments)
    }

    @Test
    fun `nowy plan dostaje swiezy id i brak wzorca dni tygodnia`() {
        val saved = buildPlanForSave(
            base = null,
            name = "Nowy plan",
            blockLengthWeeks = null,
            days = days,
            newId = { "swiezy-id" },
            now = { 999L },
        )
        assertEquals("swiezy-id", saved.id)
        assertEquals(999L, saved.createdAt)
        assertEquals(false, saved.archived)
        assertNull(saved.weekdayAssignments)
    }

    @Test
    fun `pusta nazwa dnia dostaje domyslna etykiete, niepusta jest trimowana`() {
        val messyDays = listOf(
            PlanDay(name = "   ", exercises = emptyList()),
            PlanDay(name = "  Nogi  ", exercises = emptyList()),
        )
        val saved = buildPlanForSave(
            base = null,
            name = "Plan",
            blockLengthWeeks = null,
            days = messyDays,
            newId = { "id" },
        )
        assertEquals(listOf("Dzień A", "Nogi"), saved.days.map { it.name })
    }

    @Test
    fun `cwiczenia dnia przechodza bez zmian`() {
        val saved = buildPlanForSave(
            base = existingPlan,
            name = existingPlan.name,
            blockLengthWeeks = existingPlan.blockLengthWeeks,
            days = days,
            newId = { error("nie powinno byc wolane") },
        )
        assertEquals(listOf(exercise), saved.days.single().exercises)
    }
}
