package com.stronk.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Grupowanie sprzętu: lista wartości pochodzi z datasetu, więc podział na sekcje
 * musi być totalny — nic nie może zniknąć ani zdublować się przy nowej wartości.
 */
class ProfileEquipmentTest {

    private val datasetLike = listOf(
        "barbell", "body only", "bands", "cable", "dumbbell", "e-z curl bar",
        "exercise ball", "foam roll", "kettlebells", "machine", "medicine ball", "other",
    )

    @Test
    fun `grupowanie zachowuje każdą wartość dokładnie raz`() {
        val grouped = ProfileEquipment.groupsOf(datasetLike).flatMap { it.items }

        assertEquals(datasetLike.size, grouped.size)
        assertEquals(datasetLike.toSet(), grouped.toSet())
    }

    @Test
    fun `nieznana wartość ląduje w sekcji pozostałych`() {
        val groups = ProfileEquipment.groupsOf(listOf("sledgehammer"))

        assertEquals(1, groups.size)
        assertEquals(ProfileEquipment.OTHER, groups.single().id)
        assertEquals(listOf("sledgehammer"), groups.single().items)
        assertEquals(ProfileEquipment.OTHER, ProfileEquipment.groupIdOf("sledgehammer"))
    }

    @Test
    fun `sekcje idą w stałej kolejności i mają tytuły`() {
        val groups = ProfileEquipment.groupsOf(datasetLike)

        assertEquals(
            listOf(
                ProfileEquipment.FREE_WEIGHTS,
                ProfileEquipment.MACHINES,
                ProfileEquipment.ACCESSORIES,
                ProfileEquipment.BODYWEIGHT,
                ProfileEquipment.OTHER,
            ),
            groups.map { it.id },
        )
        assertTrue(groups.all { it.title.isNotBlank() })
    }

    @Test
    fun `kolejność wewnątrz sekcji jest kolejnością wejściową`() {
        val groups = ProfileEquipment.groupsOf(listOf("dumbbell", "barbell", "kettlebells"))

        assertEquals(listOf("dumbbell", "barbell", "kettlebells"), groups.single().items)
    }

    @Test
    fun `puste wejście daje brak sekcji`() {
        assertTrue(ProfileEquipment.groupsOf(emptyList()).isEmpty())
    }

    @Test
    fun `brak sprzętu (null) traktowany jak bez sprzętu`() {
        assertEquals(ProfileEquipment.BODYWEIGHT, ProfileEquipment.groupIdOf(null))
    }

    @Test
    fun `titleOf zwraca tę samą etykietę co groupsOf`() {
        ProfileEquipment.groupsOf(datasetLike).forEach { group ->
            assertEquals(group.title, ProfileEquipment.titleOf(group.id))
        }
    }

    @Test
    fun `sortGroupIds porządkuje niezależnie od kolejności wejściowej`() {
        val shuffled = listOf(
            ProfileEquipment.OTHER,
            ProfileEquipment.BODYWEIGHT,
            ProfileEquipment.FREE_WEIGHTS,
        )

        assertEquals(
            listOf(ProfileEquipment.FREE_WEIGHTS, ProfileEquipment.BODYWEIGHT, ProfileEquipment.OTHER),
            ProfileEquipment.sortGroupIds(shuffled),
        )
    }

    @Test
    fun `sortGroupIds pomija grupy spoza wejścia`() {
        assertEquals(
            listOf(ProfileEquipment.MACHINES),
            ProfileEquipment.sortGroupIds(listOf(ProfileEquipment.MACHINES)),
        )
    }
}
