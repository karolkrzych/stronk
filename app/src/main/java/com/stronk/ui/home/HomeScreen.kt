package com.stronk.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stronk.ui.components.MuscleIcons
import com.stronk.ui.components.StronkCard
import com.stronk.ui.components.StronkEmptyState
import com.stronk.ui.components.StronkIcons
import com.stronk.ui.components.StronkListRow
import com.stronk.ui.components.StronkMetaChip
import com.stronk.ui.components.StronkNoteCard
import com.stronk.ui.components.StronkPrimaryButton
import com.stronk.ui.components.StronkScreenHeader
import com.stronk.ui.components.StronkSectionHeader
import com.stronk.ui.components.StronkTextAction
import com.stronk.ui.components.StronkTone
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTextStyles
import com.stronk.ui.theme.StronkTheme

/**
 * Ekran „Dziś" — 1:1 z mockiem `mocks/limonka/pack-dzis-plany.html` (ekran 1).
 *
 * Dominanta: karta dnia (data kapitalikami + chip bloku, nazwa dnia 27, nazwa
 * planu, CTA „Zacznij trening"). Pod nią sekcja ĆWICZENIA z licznikiem i
 * wierszami „piktogram + nazwa + chip serii" — bez ciężarów i bez fraz typu
 * „3×10": szczegóły są za tapnięciem w wiersz.
 *
 * Na dole ekranu, tuż nad dolną nawigacją, jedyny link ekranu: „Cały tydzień".
 */
@Composable
fun HomeScreen(
    onStartWorkout: (planId: String, dayIndex: Int, scheduleEntryId: String?) -> Unit,
    onOpenSchedule: () -> Unit,
    onOpenPlans: () -> Unit,
    onNewPlan: () -> Unit,
    onOpenProfile: () -> Unit,
    onExerciseClick: (exerciseId: String) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold { innerPadding ->
        if (state.loading) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
            return@Scaffold
        }

        val content = state.content
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = StronkSpacing.screen),
            ) {
                StronkScreenHeader(
                    title = "Dziś",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    actions = {
                        IconButton(onClick = onOpenProfile, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = StronkIcons.profile,
                                contentDescription = "Profil",
                                tint = StronkTheme.colors.textDim,
                                modifier = Modifier.size(21.dp),
                            )
                        }
                    },
                )

                // Trening w toku (sesja przeżyła ubicie aktywności) — powrót
                // jednym tapnięciem, zanim user zacznie cokolwiek innego.
                state.activeWorkout?.let { active ->
                    StronkNoteCard(
                        text = "${active.dayName} — zrobione serie: " +
                            "${active.completedSets} z ${active.totalSets}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = StronkSpacing.sm)
                            .clickable {
                                onStartWorkout(active.planId, active.dayIndex, active.scheduleEntryId)
                            },
                        tone = StronkTone.ACCENT,
                        label = "Trening w toku",
                        icon = StronkIcons.start,
                    )
                }

                if (state.todayDone) {
                    StronkNoteCard(
                        text = "Dzisiejszy trening zaliczony.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = StronkSpacing.sm),
                        tone = StronkTone.SUCCESS,
                        icon = StronkIcons.done,
                    )
                }

                when (content) {
                    is HomeContent.TodayWorkout -> WorkoutSection(
                        workout = content.workout,
                        ctaLabel = "Zacznij trening",
                        onStartWorkout = onStartWorkout,
                        onExerciseClick = onExerciseClick,
                    )

                    is HomeContent.UpcomingWorkout -> WorkoutSection(
                        workout = content.workout,
                        ctaLabel = "Zacznij teraz",
                        onStartWorkout = onStartWorkout,
                        onExerciseClick = onExerciseClick,
                    )

                    HomeContent.NoSchedule -> Column {
                        StronkEmptyState(
                            icon = StronkIcons.week,
                            title = "Pusty tydzień",
                            description = "Masz plan — zaplanuj z niego treningi na najbliższe dni.",
                            actionLabel = "Zaplanuj tydzień",
                            onAction = onOpenSchedule,
                        )
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            StronkTextAction("Przejrzyj plany", onClick = onOpenPlans)
                        }
                    }

                    HomeContent.NoPlans -> StronkEmptyState(
                        icon = StronkIcons.plans,
                        title = "Zacznij od planu",
                        description = "Złóż plan z bazy ćwiczeń — dopasujemy go do sprzętu " +
                            "i ograniczeń z profilu.",
                        actionLabel = "Stwórz plan",
                        onAction = onNewPlan,
                    )
                }

                Spacer(Modifier.height(StronkSpacing.lg))
            }

            if (content is HomeContent.TodayWorkout || content is HomeContent.UpcomingWorkout) {
                WeekLink(onOpenSchedule)
            }
        }
    }
}

/** Karta dnia + sekcja ćwiczeń — jedyna treść ekranu, gdy trening jest zaplanowany. */
@Composable
private fun WorkoutSection(
    workout: ScheduledWorkoutUi,
    ctaLabel: String,
    onStartWorkout: (planId: String, dayIndex: Int, scheduleEntryId: String?) -> Unit,
    onExerciseClick: (exerciseId: String) -> Unit,
) {
    StronkCard(modifier = Modifier.padding(top = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = workout.dateCaption.uppercase(),
                style = StronkTextStyles.cap,
                color = StronkTheme.colors.textDim,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            workout.weekChip?.let { StronkMetaChip(it) }
        }
        Text(
            text = workout.dayName,
            style = StronkTextStyles.title,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 14.dp),
        )
        Text(
            text = workout.planName,
            style = MaterialTheme.typography.labelMedium,
            color = StronkTheme.colors.textDim,
            modifier = Modifier.padding(top = 6.dp),
        )
        StronkPrimaryButton(
            text = ctaLabel,
            onClick = {
                onStartWorkout(workout.planId, workout.dayIndex, workout.scheduleEntryId)
            },
            icon = Icons.Rounded.PlayArrow,
            modifier = Modifier.padding(top = 18.dp),
        )
    }

    StronkSectionHeader(
        title = "Ćwiczenia",
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = StronkSpacing.xl),
        trailing = {
            Text(
                text = workout.exercises.size.toString(),
                style = StronkTextStyles.cap,
                color = StronkTheme.colors.textDim,
            )
        },
    )
    Column(Modifier.padding(top = 2.dp)) {
        workout.exercises.forEachIndexed { index, row ->
            StronkListRow(
                title = row.name,
                icon = MuscleIcons.forMuscle(row.muscleKey),
                trailing = row.setsChip,
                divider = index != workout.exercises.lastIndex,
                onClick = { onExerciseClick(row.exerciseId) },
            )
        }
    }
}

/**
 * Jedyny link ekranu (mock: `.weeklink`) — podkreślony tekst 13 w `--text-3`
 * z chevronem, wyśrodkowany tuż nad dolną nawigacją.
 */
@Composable
private fun WeekLink(onOpenSchedule: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenSchedule)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Cały tydzień",
            style = MaterialTheme.typography.labelMedium,
            color = StronkTheme.colors.textDim,
            textDecoration = TextDecoration.Underline,
        )
        Icon(
            imageVector = StronkIcons.chevron,
            contentDescription = null,
            tint = StronkTheme.colors.textDim,
            modifier = Modifier
                .padding(start = 7.dp)
                .size(16.dp),
        )
    }
}
