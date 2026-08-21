package com.stronk.ui.plans

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Kolejność kroków kreatora jest wymuszona logiką (patrz KDoc [PlanWizardStep]):
 * sprzęt i ograniczenia muszą być znane ZANIM [generatePresetDays] dobierze
 * ćwiczenia z presetu. Ten test pilnuje, żeby ktoś przypadkiem nie przesunął
 * kroku EQUIPMENT za NAME (wtedy preset wygenerowałby się PRZED wyborem sprzętu).
 */
class PlanWizardStepTest {

    @Test
    fun `krok sprzetu siedzi miedzy blokiem a ograniczeniami`() {
        assertEquals(
            listOf(
                PlanWizardStep.TEMPLATE,
                PlanWizardStep.BLOCK,
                PlanWizardStep.EQUIPMENT,
                PlanWizardStep.CONSTRAINTS,
                PlanWizardStep.NAME,
            ),
            PlanWizardStep.entries,
        )
    }

    @Test
    fun `krok sprzetu i ograniczen jest PRZED nazwa, ktora domyka kreator`() {
        val steps = PlanWizardStep.entries
        assertTrue(steps.indexOf(PlanWizardStep.EQUIPMENT) < steps.indexOf(PlanWizardStep.NAME))
        assertTrue(steps.indexOf(PlanWizardStep.CONSTRAINTS) < steps.indexOf(PlanWizardStep.NAME))
    }

    @Test
    fun `kazdy krok ma tytul i podtytul`() {
        PlanWizardStep.entries.forEach { step ->
            assertTrue(step.title.isNotBlank())
            assertTrue(step.subtitle.isNotBlank())
        }
    }
}
