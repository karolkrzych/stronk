package com.stronk.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.stronk.ui.detail.ExerciseDetailScreen
import com.stronk.ui.list.ExerciseListScreen

/** Trasy nawigacji aplikacji. */
object Routes {
    const val EXERCISE_LIST = "exercises"
    const val EXERCISE_DETAIL = "exercises/{exerciseId}"

    fun exerciseDetail(exerciseId: String): String = "exercises/${Uri.encode(exerciseId)}"
}

@Composable
fun StronkNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.EXERCISE_LIST) {
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
