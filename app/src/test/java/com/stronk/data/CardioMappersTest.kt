package com.stronk.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testy konwerterów [CardioEntry] ↔ mapa Firestore: symetria round-tripu,
 * dystans jako pole OPCJONALNE, odporność na braki i nieznany typ oraz koercja
 * liczb (Firestore oddaje Long/Double niezależnie od tego, co zapisano).
 */
class CardioMappersTest {

    private val bike = CardioEntry(
        id = "c1",
        date = "2026-08-19",
        type = CardioType.BIKE,
        durationMin = 42,
        distanceKm = 14.2,
        createdAt = 1_755_600_000_000,
    )

    private val walk = CardioEntry(
        id = "c2",
        date = "2026-08-18",
        type = CardioType.WALK,
        durationMin = 30,
        distanceKm = null,
        createdAt = 1_755_500_000_000,
    )

    @Test
    fun `round trip zachowuje wszystkie pola`() {
        val restored = FirestoreMappers.cardioEntryFromMap(
            bike.id,
            FirestoreMappers.cardioEntryToMap(bike),
        )
        assertEquals(bike, restored)
    }

    @Test
    fun `round trip bez dystansu`() {
        val map = FirestoreMappers.cardioEntryToMap(walk)
        assertTrue("dystans null nie może trafić na wire", !map.containsKey("distanceKm"))
        assertEquals(walk, FirestoreMappers.cardioEntryFromMap(walk.id, map))
    }

    @Test
    fun `typ jedzie na wire malymi literami`() {
        assertEquals("bike", FirestoreMappers.cardioEntryToMap(bike)["type"])
        assertEquals("walk", FirestoreMappers.cardioEntryToMap(walk)["type"])
    }

    @Test
    fun `nieznany typ wraca jako INNE`() {
        val restored = FirestoreMappers.cardioEntryFromMap(
            "c3",
            mapOf("date" to "2026-08-19", "type" to "kajak", "durationMin" to 20),
        )
        assertEquals(CardioType.OTHER, restored?.type)
    }

    @Test
    fun `brak typu wraca jako INNE`() {
        val restored = FirestoreMappers.cardioEntryFromMap(
            "c3",
            mapOf("date" to "2026-08-19", "durationMin" to 20),
        )
        assertEquals(CardioType.OTHER, restored?.type)
    }

    @Test
    fun `wpis bez daty jest pomijany`() {
        assertNull(
            FirestoreMappers.cardioEntryFromMap("c4", mapOf("type" to "run", "durationMin" to 20)),
        )
    }

    @Test
    fun `wpis bez czasu albo z zerowym czasem jest pomijany`() {
        assertNull(FirestoreMappers.cardioEntryFromMap("c5", mapOf("date" to "2026-08-19")))
        assertNull(
            FirestoreMappers.cardioEntryFromMap(
                "c6",
                mapOf("date" to "2026-08-19", "durationMin" to 0),
            ),
        )
    }

    @Test
    fun `dystans zerowy albo ujemny znaczy brak dystansu`() {
        val zero = FirestoreMappers.cardioEntryFromMap(
            "c7",
            mapOf("date" to "2026-08-19", "durationMin" to 20, "distanceKm" to 0.0),
        )
        assertNull(zero?.distanceKm)
    }

    @Test
    fun `liczby z Firestore sa koercjonowane`() {
        val restored = FirestoreMappers.cardioEntryFromMap(
            "c8",
            mapOf(
                "date" to "2026-08-19",
                "type" to "RUN",
                // Firestore oddaje Long dla liczb całkowitych, także dla dystansu
                "durationMin" to 35L,
                "distanceKm" to 7L,
                "createdAt" to 1_755_600_000_000L,
            ),
        )
        assertEquals(CardioType.RUN, restored?.type)
        assertEquals(35, restored?.durationMin)
        assertEquals(7.0, restored?.distanceKm ?: 0.0, 0.0001)
        assertEquals(1_755_600_000_000L, restored?.createdAt)
    }

    @Test
    fun `brak createdAt to zero, nie crash`() {
        val restored = FirestoreMappers.cardioEntryFromMap(
            "c9",
            mapOf("date" to "2026-08-19", "type" to "bike", "durationMin" to 15),
        )
        assertEquals(0L, restored?.createdAt)
    }
}
