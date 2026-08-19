package com.stronk.ui.plans

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Reorder ćwiczeń w dniu planu (drag & drop w edytorze): przenoszenie elementu,
 * nie zamiana miejscami — reszta listy zsuwa się o jeden.
 */
class PlanReorderTest {

    private val list = listOf("A", "B", "C", "D")

    @Test
    fun `przeniesienie w dol zsuwa elementy po drodze`() {
        assertEquals(listOf("B", "C", "A", "D"), list.movedItem(0, 2))
    }

    @Test
    fun `przeniesienie w gore zsuwa elementy po drodze`() {
        assertEquals(listOf("A", "D", "B", "C"), list.movedItem(3, 1))
    }

    @Test
    fun `przeniesienie o jedna pozycje zamienia sasiadow`() {
        assertEquals(listOf("A", "C", "B", "D"), list.movedItem(1, 2))
    }

    @Test
    fun `przeniesienie na koniec i na poczatek dziala`() {
        assertEquals(listOf("B", "C", "D", "A"), list.movedItem(0, 3))
        assertEquals(listOf("D", "A", "B", "C"), list.movedItem(3, 0))
    }

    @Test
    fun `ruch w to samo miejsce nie zmienia niczego`() {
        assertEquals(list, list.movedItem(2, 2))
    }

    @Test
    fun `indeks spoza zakresu nie psuje listy`() {
        assertEquals(list, list.movedItem(0, 4))
        assertEquals(list, list.movedItem(-1, 2))
        assertEquals(emptyList<String>(), emptyList<String>().movedItem(0, 0))
    }

    @Test
    fun `kolejnosc jest stabilna po serii przesuniec`() {
        val reordered = list
            .movedItem(0, 3)
            .movedItem(0, 1)
            .movedItem(3, 0)
        assertEquals(listOf("A", "C", "B", "D"), reordered)
    }
}
