package com.stronk.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.stronk.StronkApplication
import com.stronk.ui.accesscode.AccessCodeScreen
import com.stronk.ui.detail.ExerciseDetailScreen
import com.stronk.ui.list.ExerciseListScreen

/** Trasy nawigacji aplikacji. */
object Routes {
    const val ACCESS_CODE = "access-code"
    const val EXERCISE_LIST = "exercises"
    const val EXERCISE_DETAIL = "exercises/{exerciseId}"

    fun exerciseDetail(exerciseId: String): String = "exercises/${Uri.encode(exerciseId)}"
}

@Composable
fun StronkNavHost() {
    val navController = rememberNavController()
    val app = LocalContext.current.applicationContext as StronkApplication
    // Decyzja raz na start: brak kodu dostępu = pierwsze uruchomienie → ekran kodu.
    val startDestination = remember {
        if (app.accessCodeStore.getCode() == null) Routes.ACCESS_CODE else Routes.EXERCISE_LIST
    }
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.ACCESS_CODE) {
            AccessCodeScreen(
                onCodeReady = {
                    navController.navigate(Routes.EXERCISE_LIST) {
                        popUpTo(Routes.ACCESS_CODE) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.EXERCISE_LIST) {
            ExerciseListScreen(
                onExerciseClick = { id -> navController.navigate(Routes.exerciseDetail(id)) },
            )
        }
        composable(Routes.EXERCISE_DETAIL) { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getString("exerciseId").orEmpty()
            ExerciseDetailScreen(
                exerciseId = exerciseId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
