package com.stronk.ui.plans

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Krok TEMPLATE kreatora — bug: opcja „Od zera" renderowała się jako zaznaczona
 * od startu ([wizard.selectedPresetId] == null zanim user cokolwiek kliknął),
 * mimo że [PlanEditorViewModel.Draft.templateChosen] == false i „Dalej" był
 * szary — trzeba było kliknąć DRUGI raz. Fix: [PlanWizardUi.templateChosen]
 * jako osobna projekcja [PlanEditorViewModel.Draft.templateChosen] — w
 * [PlanWizard.kt] „Od zera" jest zaznaczone tylko gdy `templateChosen && selectedPresetId == null`.
 *
 * [PlanEditorViewModel.wizardUi] jest w `companion object` właśnie po to, żeby
 * dało się go wywołać bez stawiania całego ViewModelu (repozytoria + `init` z
 * `viewModelScope` czynią to niepraktycznym w prostym teście JVM bez
 * kotlinx-coroutines-test).
 */
class PlanWizardStateTest {

    private val preset = PlanPreset(
        id = "full-body",
        name = "Full Body",
        description = "Trzy dni w tygodniu.",
        days = listOf(),
    )

    @Test
    fun `swiezy draft - nic nie zaznaczone i Dalej nieaktywne`() {
        val ui = PlanEditorViewModel.wizardUi(PlanEditorViewModel.Draft(), emptyList())

        assertFalse(ui.templateChosen)
        assertFalse(ui.canGoNext)
        assertNull(ui.selectedPresetId)
    }

    @Test
    fun `wybor Od zera - templateChosen true, selectedPresetId null, Dalej aktywne`() {
        val draft = PlanEditorViewModel.Draft(preset = null, templateChosen = true)
        val ui = PlanEditorViewModel.wizardUi(draft, emptyList())

        assertTrue(ui.templateChosen)
        assertNull(ui.selectedPresetId)
        assertTrue(ui.canGoNext)
    }

    @Test
    fun `wybor szablonu - selectedPresetId ustawione, Dalej aktywne`() {
        val draft = PlanEditorViewModel.Draft(preset = preset, templateChosen = true)
        val ui = PlanEditorViewModel.wizardUi(draft, emptyList())

        assertEquals(preset.id, ui.selectedPresetId)
        assertTrue(ui.templateChosen)
        assertTrue(ui.canGoNext)
    }

    @Test
    fun `powrot na krok TEMPLATE po wyborze nie gubi zaznaczenia`() {
        // Symulacja "wybrano Od zera -> Dalej -> Wstecz": wizardBack/wizardNext
        // zmieniaja WYLACZNIE pole step, templateChosen i preset zostaja bez zmian.
        val wybranoIPoszlismyDalej = PlanEditorViewModel.Draft(
            preset = null,
            templateChosen = true,
            step = PlanWizardStep.BLOCK,
        )
        val poWsteczNaTemplate = wybranoIPoszlismyDalej.copy(step = PlanWizardStep.TEMPLATE)

        val ui = PlanEditorViewModel.wizardUi(poWsteczNaTemplate, emptyList())

        assertTrue(ui.templateChosen)
        assertNull(ui.selectedPresetId)
        assertTrue(ui.canGoNext)
    }
}
