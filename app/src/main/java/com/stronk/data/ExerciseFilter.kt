package com.stronk.data

import java.text.Normalizer

/**
 * Logika wyszukiwania i filtrowania ćwiczeń — czyste funkcje, testowalne bez Androida.
 */

/** Aktywne filtry listy; null = filtr nieustawiony. */
data class ExerciseFilters(
    val muscle: String? = null,
    val equipment: String? = null,
    val level: String? = null,
    val category: String? = null,
) {
    val isEmpty: Boolean
        get() = muscle == null && equipment == null && level == null && category == null
}

private val combiningMarksRegex = Regex("\\p{Mn}+")

/**
 * Normalizacja do porównań wyszukiwarki: małe litery i zdjęte diakrytyki
 * ("ćwiczenie" == "cwiczenie"). Uwaga: "ł" nie ma dekompozycji NFD,
 * więc zamieniamy je jawnie.
 */
fun normalizeForSearch(text: String): String {
    val lower = text.lowercase().replace('ł', 'l')
    val decomposed = Normalizer.normalize(lower, Normalizer.Form.NFD)
    return combiningMarksRegex.replace(decomposed, "")
}

/**
 * Zwraca ćwiczenia pasujące do zapytania (szuka w namePl i name, bez
 * rozróżniania diakrytyków i wielkości liter) oraz do ustawionych filtrów.
 */
fun filterExercises(
    exercises: List<Exercise>,
    query: String,
    filters: ExerciseFilters = ExerciseFilters(),
): List<Exercise> {
    val normalizedQuery = normalizeForSearch(query.trim())
    return exercises.filter { exercise ->
        val matchesQuery = normalizedQuery.isEmpty() ||
            normalizeForSearch(exercise.namePl).contains(normalizedQuery) ||
            normalizeForSearch(exercise.name).contains(normalizedQuery)
        val matchesMuscle = filters.muscle == null || filters.muscle in exercise.primaryMuscles
        val matchesEquipment = filters.equipment == null || filters.equipment == exercise.equipment
        val matchesLevel = filters.level == null || filters.level == exercise.level
        val matchesCategory = filters.category == null || filters.category == exercise.category
        matchesQuery && matchesMuscle && matchesEquipment && matchesLevel && matchesCategory
    }
}
