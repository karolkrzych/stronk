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
}
