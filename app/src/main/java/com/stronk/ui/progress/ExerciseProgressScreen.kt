package com.stronk.ui.progress

import androidx.compose.runtime.Composable
import com.stronk.ui.detail.ExerciseDetailScreen
import com.stronk.ui.detail.ExerciseDetailTab

/**
 * SHIM ZGODNOŚCI — trasa `progress/exercise/{exerciseId}` z alfy.
 *
 * Historia ćwiczenia ma dziś JEDEN widok: zakładkę „Historia" w szczegółach
 * ćwiczenia ([ExerciseDetailScreen]). Osobny ekran z wykresem liniowym został
 * skasowany (goła linia bez osi = odrzucona przez Karola), a ta funkcja tylko
 * przekierowuje starą trasę na nowy ekran, żeby nawigacja nie pękła.
 *
 * **Do usunięcia razem z trasą `EXERCISE_PROGRESS`** — gdy NavHost zacznie
 * z Progresu wchodzić wprost w `exercises/{id}` z zakładką „Historia".
 *
 * @param onExerciseDetailClick nieużywane — opis ćwiczenia jest teraz sąsiednią
 *        zakładką tego samego ekranu, nie osobną trasą.
 */
@Composable
fun ExerciseProgressScreen(
    exerciseId: String,
    onBack: () -> Unit,
    onExerciseDetailClick: (exerciseId: String) -> Unit = {},
) {
    ExerciseDetailScreen(
        exerciseId = exerciseId,
        onBack = onBack,
        initialTab = ExerciseDetailTab.HISTORY,
    )
}
