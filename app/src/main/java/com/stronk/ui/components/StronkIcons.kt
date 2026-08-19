package com.stronk.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cable
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PersonalInjury
import androidx.compose.material.icons.rounded.PrecisionManufacturing
import androidx.compose.material.icons.rounded.Rowing
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsHandball
import androidx.compose.material.icons.rounded.SportsMartialArts
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.ui.graphics.vector.ImageVector
import com.stronk.data.Exercise

/**
 * Wspólny słownik ikon — jedno źródło prawdy, żeby pięć ekranów nie wybrało pięciu
 * różnych ikon na to samo. Wszystko z `Icons.Rounded.*` (material-icons-extended):
 * zaokrąglony wariant pasuje do miękkich promieni z mocków.
 */
object StronkIcons {
    // nawigacja
    val today = Icons.Rounded.Today
    val week = Icons.Rounded.CalendarMonth
    val plans = Icons.AutoMirrored.Rounded.ListAlt
    val progress = Icons.AutoMirrored.Rounded.TrendingUp
    val database = Icons.Rounded.Search

    // akcje
    val start = Icons.Rounded.FitnessCenter
    val done = Icons.Rounded.Check
    val swap = Icons.Rounded.SwapHoriz
    val add = Icons.Rounded.Add
    val edit = Icons.Rounded.Edit
    val delete = Icons.Rounded.Delete
    val close = Icons.Rounded.Close
    val settings = Icons.Rounded.Settings

    /** Chevron „w szczegóły" na końcu wiersza listy. */
    val chevron = Icons.AutoMirrored.Rounded.KeyboardArrowRight

    /** Chevron wstecz w pasku ekranu. */
    val back = Icons.AutoMirrored.Rounded.KeyboardArrowLeft

    /** Dyskretne „i" — jak wykonać ćwiczenie, skąd ta liczba. */
    val info = Icons.Rounded.Info

    // domeny
    val rest = Icons.Rounded.Timer
    val restDay = Icons.Rounded.Hotel
    val record = Icons.Rounded.EmojiEvents
    val injury = Icons.Rounded.PersonalInjury
    val profile = Icons.Rounded.Accessibility
}

/**
 * Ikona partii mięśniowej / ćwiczenia do [StronkIconBadge]. Mocki mają własne
 * piktogramy SVG (klata/plecy/biceps/nogi/barki/brzuch) — tu ich najbliższe
 * odpowiedniki z material-icons-extended, zgrupowane tak samo.
 */
object MuscleIcons {

    /** Ikona dla klucza partii z datasetu (angielskiego, jak w [com.stronk.ui.PlLabels]). */
    fun forMuscle(muscle: String?): ImageVector = when (muscle) {
        "chest" -> Icons.Rounded.SportsMartialArts
        "lats", "middle back", "lower back", "traps" -> Icons.Rounded.Rowing
        "biceps", "triceps", "forearms" -> Icons.Rounded.FitnessCenter
        "shoulders", "neck" -> Icons.Rounded.SportsHandball
        "quadriceps", "hamstrings", "glutes", "calves", "abductors", "adductors" ->
            Icons.AutoMirrored.Rounded.DirectionsRun
        "abdominals" -> Icons.Rounded.SelfImprovement
        else -> Icons.Rounded.FitnessCenter
    }

    /** Ikona ćwiczenia: partia główna, a dla cardio/rozciągania — charakter kategorii. */
    fun forExercise(exercise: Exercise): ImageVector = when (exercise.category) {
        "cardio" -> Icons.AutoMirrored.Rounded.DirectionsRun
        "stretching" -> Icons.Rounded.SelfImprovement
        else -> forMuscle(exercise.primaryMuscles.firstOrNull())
    }

    /**
     * Krótka polska nazwa grupy pod badge'em (mocki: `.ex-mg` — "plecy", "nogi").
     * Celowo krócej niż [com.stronk.ui.PlLabels.muscle]: to podpis 9-punktowy, nie zdanie.
     */
    fun groupLabel(muscle: String?): String = when (muscle) {
        "chest" -> "klata"
        "lats", "middle back", "lower back", "traps" -> "plecy"
        "biceps" -> "biceps"
        "triceps" -> "triceps"
        "forearms" -> "przedramię"
        "shoulders" -> "barki"
        "neck" -> "kark"
        "quadriceps", "hamstrings", "glutes", "abductors", "adductors" -> "nogi"
        "calves" -> "łydki"
        "abdominals" -> "brzuch"
        else -> "inne"
    }

    /** Ikona sprzętu (klucz z datasetu, np. "barbell"). */
    fun forEquipment(equipment: String?): ImageVector = when (equipment) {
        "body only", null -> Icons.Rounded.Accessibility
        "cable" -> Icons.Rounded.Cable
        "machine" -> Icons.Rounded.PrecisionManufacturing
        "bands" -> Icons.Rounded.Waves
        else -> Icons.Rounded.FitnessCenter
    }
}
