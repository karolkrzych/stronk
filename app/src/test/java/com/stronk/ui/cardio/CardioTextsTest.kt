package com.stronk.ui.cardio

import com.stronk.data.CardioType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Walidacja i formatowanie sheetu cardio: „Zapisz" zależy WYŁĄCZNIE od minut,
 * dystans jest opcjonalny, a liczby wracają osobno od jednostek (zasada Karola —
 * żadnej frazy „42 min · 14,2 km").
 */
class CardioTextsTest {

    // ---------- walidacja minut ----------

    @Test
    fun `puste albo zerowe minuty blokuja zapis`() {
        assertFalse(CardioTexts.canSave(""))
        assertFalse(CardioTexts.canSave("0"))
        assertFalse(CardioTexts.canSave("   "))
        assertFalse(CardioTexts.canSave("abc"))
    }

    @Test
    fun `dodatnie minuty odblokowuja zapis`() {
        assertTrue(CardioTexts.canSave("1"))
        assertTrue(CardioTexts.canSave("42"))
        assertEquals(42, CardioTexts.parseMinutes("42"))
    }

    @Test
    fun `minuty ponad dobe to blad wpisu`() {
        assertNull(CardioTexts.parseMinutes("1441"))
        assertEquals(CardioTexts.MAX_MINUTES, CardioTexts.parseMinutes("1440"))
    }

    // ---------- walidacja dystansu ----------

    @Test
    fun `pusty dystans jest poprawny i znaczy brak dystansu`() {
        assertNull(CardioTexts.parseDistance(""))
        // ...ale nie blokuje zapisu — CTA patrzy tylko na minuty
        assertTrue(CardioTexts.canSave("30"))
    }

    @Test
    fun `dystans akceptuje przecinek i kropke`() {
        assertEquals(14.2, CardioTexts.parseDistance("14,2")!!, 0.0001)
        assertEquals(14.2, CardioTexts.parseDistance("14.2")!!, 0.0001)
        assertEquals(8.0, CardioTexts.parseDistance("8")!!, 0.0001)
    }

    @Test
    fun `dystans zerowy, ujemny albo absurdalny to brak dystansu`() {
        assertNull(CardioTexts.parseDistance("0"))
        assertNull(CardioTexts.parseDistance("-3"))
        assertNull(CardioTexts.parseDistance("1000"))
    }

    @Test
    fun `dystans z wiszacym przecinkiem czyta sie jak liczba calkowita`() {
        // „14," to stan w trakcie pisania — zapis nie może przez to przepaść
        assertEquals(14.0, CardioTexts.parseDistance("14,")!!, 0.0001)
    }

    // ---------- filtry pól ----------

    @Test
    fun `pole minut przyjmuje tylko cyfry bez wiodacych zer`() {
        assertEquals("42", CardioTexts.sanitizeMinutes("4a2"))
        assertEquals("42", CardioTexts.sanitizeMinutes("042"))
        assertEquals("0", CardioTexts.sanitizeMinutes("0"))
        assertEquals("", CardioTexts.sanitizeMinutes("abc"))
        assertEquals("1234", CardioTexts.sanitizeMinutes("123456"))
    }

    @Test
    fun `pole dystansu ma jeden separator i dwa miejsca po przecinku`() {
        assertEquals("14,2", CardioTexts.sanitizeDistance("14.2"))
        assertEquals("14,25", CardioTexts.sanitizeDistance("14,256"))
        assertEquals("14,2", CardioTexts.sanitizeDistance("14,2,5"))
        assertEquals("14", CardioTexts.sanitizeDistance("14km"))
    }

    // ---------- formatowanie do statów ----------

    @Test
    fun `liczba i jednostka to osobne byty`() {
        assertEquals("42", CardioTexts.minutesValue(42))
        assertEquals("min", CardioTexts.UNIT_MINUTES)
        assertEquals("14,2", CardioTexts.distanceValue(14.2))
        assertEquals("8", CardioTexts.distanceValue(8.0))
        assertEquals("km", CardioTexts.UNIT_KILOMETERS)
    }

    @Test
    fun `etykiety typow sa polskie`() {
        assertEquals("Rower", CardioTexts.typeLabel(CardioType.BIKE))
        assertEquals("Bieg", CardioTexts.typeLabel(CardioType.RUN))
        assertEquals("Spacer", CardioTexts.typeLabel(CardioType.WALK))
        assertEquals("Inne", CardioTexts.typeLabel(CardioType.OTHER))
    }

    @Test
    fun `prefill edycji odtwarza wpisane wartosci`() {
        assertEquals("42", CardioTexts.minutesInput(42))
        assertEquals("14,2", CardioTexts.distanceInput(14.2))
        assertEquals("", CardioTexts.distanceInput(null))
    }
}
