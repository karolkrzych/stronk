package com.stronk.ui.profile

/** Sekcja listy sprzętu: tytuł + wartości z datasetu należące do tej grupy. */
data class EquipmentGroup(val id: String, val title: String, val items: List<String>)

/**
 * Grupowanie sprzętu na czytelne sekcje. Lista wartości pochodzi 1:1 z datasetu,
 * więc grupowanie musi być totalne: klucz spoza słownika ląduje w „Pozostałe”,
 * nic nie znika i nic się nie duplikuje.
 */
object ProfileEquipment {

    const val FREE_WEIGHTS = "free"
    const val MACHINES = "machines"
    const val CARDIO = "cardio"
    const val ACCESSORIES = "accessories"
    const val BODYWEIGHT = "bodyweight"
    const val OTHER = "other"

    /** Kolejność sekcji na ekranie — od tego, co najczęściej decyduje o planie. */
    private val order = listOf(FREE_WEIGHTS, MACHINES, CARDIO, ACCESSORIES, BODYWEIGHT, OTHER)

    private val titles = mapOf(
        FREE_WEIGHTS to "Wolne ciężary",
        MACHINES to "Maszyny i wyciągi",
        CARDIO to "Cardio",
        ACCESSORIES to "Akcesoria",
        BODYWEIGHT to "Bez sprzętu",
        OTHER to "Pozostałe",
    )

    private val groupOfKey = mapOf(
        "barbell" to FREE_WEIGHTS,
        "dumbbell" to FREE_WEIGHTS,
        "kettlebells" to FREE_WEIGHTS,
        "e-z curl bar" to FREE_WEIGHTS,
        "machine" to MACHINES,
        "cable" to MACHINES,
        // Reklasyfikacja equipment="machine" (2026-08-21): podtypy maszyn siłowych
        // zostają w tej samej sekcji co "machine" i "cable" — to wciąż sprzęt
        // stacjonarny na siłowni, tylko rozróżniony na realne urządzenia.
        "smith machine" to MACHINES,
        "leverage machine" to MACHINES,
        "leg machine" to MACHINES,
        // Cardio ma WŁASNĄ sekcję zamiast trafiać do "Maszyny i wyciągi" albo
        // "Pozostałe": to inny rodzaj sprzętu (bieżnia/orbitrek/rower/wioślarz),
        // który user może mieć niezależnie od maszyn siłowych (np. tylko w domu),
        // a "Pozostałe" jest już zbiorem różnorodnych rzadkich wartości — cardio
        // zgubiłoby się tam mimo że to duża, konkretna kategoria (9 wartości).
        "cardio machine" to CARDIO,
        "bands" to ACCESSORIES,
        "medicine ball" to ACCESSORIES,
        "exercise ball" to ACCESSORIES,
        "foam roll" to ACCESSORIES,
        "body only" to BODYWEIGHT,
    )

    /** Grupa dla wartości sprzętu; nieznana wartość → [OTHER]. */
    fun groupIdOf(equipment: String): String = groupOfKey[equipment] ?: OTHER

    /** Jak wyżej, ale brak sprzętu (`null`, np. ćwiczenie bez pola equipment) → [BODYWEIGHT]. */
    @JvmName("groupIdOfNullable")
    fun groupIdOf(equipment: String?): String = equipment?.let(::groupIdOf) ?: BODYWEIGHT

    /** Sekcje w stałej kolejności; puste sekcje pomijamy, kolejność wewnątrz = wejściowa. */
    fun groupsOf(options: List<String>): List<EquipmentGroup> {
        val byGroup = options.groupBy(::groupIdOf)
        return order.mapNotNull { id ->
            byGroup[id]?.let { items -> EquipmentGroup(id, titles.getValue(id), items) }
        }
    }

    /** Etykieta sekcji dla ID grupy (jednej z pięciu stałych powyżej). */
    fun titleOf(groupId: String): String = titles.getValue(groupId)

    /** Podzbiór [ids] w kanonicznej kolejności sekcji — do sortowania chipów filtra. */
    fun sortGroupIds(ids: Collection<String>): List<String> = order.filter { it in ids }
}
