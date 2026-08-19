package com.stronk.ui.schedule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Reguły znaczników kalendarza po dołożeniu cardio (mock `round4/cardio-l1`):
 * wypełnienie dalej znaczy DOKŁADNIE „trening zrobiony", cardio dokłada obrys
 * albo wewnętrzny ring, a plan nigdy nie przykrywa faktu.
 */
class CalendarMarkersTest {

    @Test
    fun `zrobiony trening bez cardio to wypelniony kwadrat`() {
        assertEquals(
            DayMarker.DONE,
            CalendarMarkers.marker(ScheduleDayStatus.DONE, hasCardio = false),
        )
    }

    @Test
    fun `zrobiony trening z cardio to wypelnienie plus ring`() {
        assertEquals(
            DayMarker.DONE_WITH_CARDIO,
            CalendarMarkers.marker(ScheduleDayStatus.DONE, hasCardio = true),
        )
    }

    @Test
    fun `samo cardio w dniu wolnym to obrys cardio`() {
        assertEquals(
            DayMarker.CARDIO,
            CalendarMarkers.marker(ScheduleDayStatus.FREE, hasCardio = true),
        )
    }

    @Test
    fun `cardio wygrywa z samym planem - fakt przed zamiarem`() {
        assertEquals(
            DayMarker.CARDIO,
            CalendarMarkers.marker(ScheduleDayStatus.PLANNED, hasCardio = true),
        )
        assertEquals(
            DayMarker.CARDIO,
            CalendarMarkers.marker(ScheduleDayStatus.MISSED, hasCardio = true),
        )
    }

    @Test
    fun `plan i przeszly niezaliczony rysuja sie tak samo`() {
        assertEquals(
            DayMarker.PLANNED,
            CalendarMarkers.marker(ScheduleDayStatus.PLANNED, hasCardio = false),
        )
        assertEquals(
            DayMarker.PLANNED,
            CalendarMarkers.marker(ScheduleDayStatus.MISSED, hasCardio = false),
        )
    }

    @Test
    fun `pusty dzien zostaje pusty`() {
        assertEquals(
            DayMarker.FREE,
            CalendarMarkers.marker(ScheduleDayStatus.FREE, hasCardio = false),
        )
    }

    @Test
    fun `legenda cardio pojawia sie tylko gdy w siatce jest cardio`() {
        assertFalse(CalendarMarkers.anyCardio(listOf(DayMarker.DONE, DayMarker.PLANNED, DayMarker.FREE)))
        assertTrue(CalendarMarkers.anyCardio(listOf(DayMarker.FREE, DayMarker.CARDIO)))
        assertTrue(CalendarMarkers.anyCardio(listOf(DayMarker.DONE_WITH_CARDIO)))
    }
}
