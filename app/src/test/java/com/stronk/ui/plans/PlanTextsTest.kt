package com.stronk.ui.plans

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Teksty zwijanej sekcji "Serie i ciężar" w arkuszu edycji ćwiczenia planu
 * (feedback: "kliknięcie ma pokazywać opis najpierw" — sekcja domyślnie
 * zwinięta, chevron opisuje swój stan w content description).
 */
class PlanTextsTest {

    @Test
    fun `tytul sekcji serii i ciezaru jest staly`() {
        assertEquals("Serie i ciężar", PlanTexts.SERIES_SECTION_TITLE)
    }

    @Test
    fun `opis chevronu zalezy od stanu rozwiniecia`() {
        assertEquals("Zwiń sekcję serii i ciężaru", PlanTexts.seriesSectionToggleDescription(expanded = true))
        assertEquals(
            "Rozwiń sekcję serii i ciężaru",
            PlanTexts.seriesSectionToggleDescription(expanded = false),
        )
    }
}
