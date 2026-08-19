package com.stronk.ui

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stronk.StronkApplication
import com.stronk.ui.accesscode.AccessCodeScreen
import com.stronk.ui.components.StronkNavigationBar
import com.stronk.ui.components.StronkNavigationBarItem
import com.stronk.ui.detail.ExerciseDetailScreen
import com.stronk.ui.home.HomeScreen
import com.stronk.ui.list.ExerciseListScreen
import com.stronk.ui.plans.PlanEditorScreen
import com.stronk.ui.plans.PlansScreen
import com.stronk.ui.profile.ProfileScreen
import com.stronk.ui.progress.ExerciseProgressScreen
import com.stronk.ui.progress.ProgressScreen
import com.stronk.ui.schedule.ScheduleScreen
import com.stronk.ui.workout.WorkoutScreen

/**
 * Trasy nawigacji aplikacji — KOMPLETNA mapa alfy. Argumenty w ścieżce
 * przechodzą przez [Uri.encode]; funkcje pomocnicze budują gotowe trasy.
 */
object Routes {
    const val ACCESS_CODE = "access-code"
    const val HOME = "home"
    const val PROFILE = "profile"
    const val PLANS = "plans"

    /** Edytor planu; planId = id istniejącego planu albo [NEW_PLAN_ID]. */
    const val PLAN_EDITOR = "plans/editor/{planId}"
    const val SCHEDULE = "schedule"

    /** Tryb treningu; scheduleEntryId opcjonalne (trening spoza harmonogramu). */
    const val WORKOUT = "workout/{planId}/{dayIndex}?scheduleEntryId={scheduleEntryId}"
    const val PROGRESS = "progress"
    const val EXERCISE_PROGRESS = "progress/exercise/{exerciseId}"
    const val EXERCISE_LIST = "exercises"
    const val EXERCISE_DETAIL = "exercises/{exerciseId}"

    /** Wartość argumentu planId oznaczająca tworzenie nowego planu. */
    const val NEW_PLAN_ID = "new"

    fun exerciseDetail(exerciseId: String): String = "exercises/${Uri.encode(exerciseId)}"

    /** null → edytor w trybie nowego planu. */
    fun planEditor(planId: String?): String = "plans/editor/${Uri.encode(planId ?: NEW_PLAN_ID)}"

    fun workout(planId: String, dayIndex: Int, scheduleEntryId: String? = null): String {
        val base = "workout/${Uri.encode(planId)}/$dayIndex"
        return if (scheduleEntryId == null) base
        else "$base?scheduleEntryId=${Uri.encode(scheduleEntryId)}"
    }

    fun exerciseProgress(exerciseId: String): String =
        "progress/exercise/${Uri.encode(exerciseId)}"
}

/**
 * Zakładka dolnej nawigacji. [label] nie jest rysowana (pasek ma SAME ikony) —
 * idzie do `contentDescription` dla czytnika ekranu.
 */
private data class BottomTab(val route: String, val label: String, val icon: ImageVector)

/**
 * Pięć zakładek w kolejności z mocków (`pack-dzis-plany.html`, `.nav`).
 *
 * Ikony bierzemy tu WPROST z `Icons.Rounded.*`, a nie ze [StronkIcons]: pasek
 * mówi „gdzie jestem", a ekrany mówią „co to jest", więc te same pojęcia mają
 * w nawigacji inne piktogramy niż w treści (Dziś = dom, nie kartka kalendarza;
 * Baza = hantel, nie lupa).
 */
private val bottomTabs = listOf(
    BottomTab(Routes.HOME, "Dziś", Icons.Rounded.Home),
    BottomTab(Routes.SCHEDULE, "Tydzień", Icons.Rounded.CalendarMonth),
    BottomTab(Routes.PLANS, "Plany", Icons.AutoMirrored.Rounded.ListAlt),
    BottomTab(Routes.PROGRESS, "Progres", Icons.AutoMirrored.Rounded.TrendingUp),
    BottomTab(Routes.EXERCISE_LIST, "Baza", Icons.Rounded.FitnessCenter),
)

@Composable
fun StronkNavHost() {
    val navController = rememberNavController()
    val app = LocalContext.current.applicationContext as StronkApplication
    // Decyzja raz na start: brak kodu dostępu = pierwsze uruchomienie → ekran kodu.
    val startDestination = remember {
        if (app.accessCodeStore.getCode() == null) Routes.ACCESS_CODE else Routes.HOME
    }

    // Przełączanie zakładek: jeden egzemplarz na szczycie, stan zakładki zachowany.
    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(Routes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    // Pasek tylko na ekranach-zakładkach; ekrany szczegółowe (trening, edytor,
    // szczegóły ćwiczenia…) są pełnoekranowe.
    val showBottomBar = bottomTabs.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                StronkNavigationBar {
                    bottomTabs.forEach { tab ->
                        StronkNavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { navigateToTab(tab.route) },
                            icon = tab.icon,
                            label = tab.label,
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.ACCESS_CODE) {
                AccessCodeScreen(
                    onCodeReady = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.ACCESS_CODE) { inclusive = true }
                        }
                    },
                )
            }

            composable(Routes.HOME) {
                HomeScreen(
                    onStartWorkout = { planId, dayIndex, scheduleEntryId ->
                        navController.navigate(Routes.workout(planId, dayIndex, scheduleEntryId))
                    },
                    onOpenSchedule = { navigateToTab(Routes.SCHEDULE) },
                    onOpenPlans = { navigateToTab(Routes.PLANS) },
                    onNewPlan = { navController.navigate(Routes.planEditor(null)) },
                    onOpenProfile = { navController.navigate(Routes.PROFILE) },
                    onExerciseClick = { id -> navController.navigate(Routes.exerciseDetail(id)) },
                )
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.PLANS) {
                PlansScreen(
                    onPlanClick = { planId -> navController.navigate(Routes.planEditor(planId)) },
                    onNewPlan = { navController.navigate(Routes.planEditor(null)) },
                )
            }

            composable(Routes.PLAN_EDITOR) { entry ->
                val rawPlanId = entry.arguments?.getString("planId").orEmpty()
                PlanEditorScreen(
                    planId = rawPlanId.takeUnless { it == Routes.NEW_PLAN_ID },
                    onBack = { navController.popBackStack() },
                    onExerciseClick = { id -> navController.navigate(Routes.exerciseDetail(id)) },
                )
            }

            composable(Routes.SCHEDULE) {
                ScheduleScreen(
                    onStartWorkout = { planId, dayIndex, scheduleEntryId ->
                        navController.navigate(Routes.workout(planId, dayIndex, scheduleEntryId))
                    },
                    onPlanClick = { planId -> navController.navigate(Routes.planEditor(planId)) },
                    onNewPlan = { navController.navigate(Routes.planEditor(null)) },
                    onExerciseClick = { id -> navController.navigate(Routes.exerciseDetail(id)) },
                )
            }

            composable(
                route = Routes.WORKOUT,
                arguments = listOf(
                    navArgument("scheduleEntryId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { entry ->
                WorkoutScreen(
                    planId = entry.arguments?.getString("planId").orEmpty(),
                    dayIndex = entry.arguments?.getString("dayIndex")?.toIntOrNull() ?: 0,
                    scheduleEntryId = entry.arguments?.getString("scheduleEntryId"),
                    onFinished = { navController.popBackStack() },
                    onExit = { navController.popBackStack() },
                    onExerciseClick = { id -> navController.navigate(Routes.exerciseDetail(id)) },
                )
            }

            composable(Routes.PROGRESS) {
                ProgressScreen(
                    onExerciseClick = { id -> navController.navigate(Routes.exerciseProgress(id)) },
                )
            }

            composable(Routes.EXERCISE_PROGRESS) { entry ->
                val exerciseId = entry.arguments?.getString("exerciseId").orEmpty()
                ExerciseProgressScreen(
                    exerciseId = exerciseId,
                    onBack = { navController.popBackStack() },
                    onExerciseDetailClick = { id ->
                        navController.navigate(Routes.exerciseDetail(id))
                    },
                )
            }

            composable(Routes.EXERCISE_LIST) {
                ExerciseListScreen(
                    onExerciseClick = { id -> navController.navigate(Routes.exerciseDetail(id)) },
                )
            }

            composable(Routes.EXERCISE_DETAIL) { entry ->
                val exerciseId = entry.arguments?.getString("exerciseId").orEmpty()
                ExerciseDetailScreen(
                    exerciseId = exerciseId,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
