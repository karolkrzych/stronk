package com.stronk.ui.home

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.stronk.ui.components.StronkCard
import com.stronk.ui.components.StronkEmptyState
import com.stronk.ui.components.StronkIconBadge
import com.stronk.ui.components.StronkIconBadgeSize
import com.stronk.ui.components.StronkIcons
import com.stronk.ui.components.StronkMetaChip
import com.stronk.ui.components.StronkPrimaryButton
import com.stronk.ui.components.StronkScreenHeader
import com.stronk.ui.components.StronkSectionHeader
import com.stronk.ui.components.StronkTextAction
import com.stronk.ui.components.StronkTone
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTheme

/**
 * Ekran startowy "Dziś": dzisiejszy (albo najbliższy) zaplanowany trening
 * z CTA "Zacznij trening", a bez harmonogramu/planów — zachęta do działania.
 * Karta treningu jest jedynym focal pointem ekranu (mocki: język "day-card").
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
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = StronkSpacing.screen, vertical = StronkSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(StronkSpacing.section),
        ) {
            Header(
                displayName = state.displayName,
                todayLabel = state.todayLabel,
                onOpenProfile = onOpenProfile,
            )

            // Trening w toku (sesja przeżyła ubicie aktywności) — powrót
            // jednym tapnięciem, zanim user zacznie cokolwiek innego.
            state.activeWorkout?.let { active ->
                ActiveWorkoutCard(
                    active = active,
                    onResume = {
                        onStartWorkout(active.planId, active.dayIndex, active.scheduleEntryId)
                    },
                )
            }

            if (state.todayDone) {
                TodayDoneCard()
            }

            when (val content = state.content) {
                is HomeContent.TodayWorkout -> {
                    WorkoutCard(
                        kicker = "Dziś",
                        workout = content.workout,
                        ctaLabel = "Zacznij trening",
                        onStartWorkout = onStartWorkout,
                        onExerciseClick = onExerciseClick,
                    )
                    SeeWeekLink(onOpenSchedule)
                }

                is HomeContent.UpcomingWorkout -> {
                    if (!state.todayDone) {
                        Text(
                            text = "Dziś dzień odpoczynku",
                            style = MaterialTheme.typography.bodyMedium,
                            color = StronkTheme.colors.textDim,
                        )
                    }
                    WorkoutCard(
                        kicker = "Najbliższy trening · ${content.workout.dateLabel}",
                        workout = content.workout,
                        ctaLabel = "Zacznij teraz",
                        onStartWorkout = onStartWorkout,
                        onExerciseClick = onExerciseClick,
                    )
                    SeeWeekLink(onOpenSchedule)
                }

                HomeContent.NoSchedule -> Column(
                    verticalArrangement = Arrangement.spacedBy(StronkSpacing.sm),
                ) {
                    StronkEmptyState(
                        icon = StronkIcons.week,
                        title = "Pusty tydzień",
                        description = "Masz już plan — zaplanuj z niego treningi na najbliższe dni.",
                        actionLabel = "Zaplanuj tydzień",
                        onAction = onOpenSchedule,
                    )
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        StronkTextAction("Przejrzyj plany", onClick = onOpenPlans, icon = StronkIcons.plans)
                    }
                }

                HomeContent.NoPlans -> StronkEmptyState(
                    icon = StronkIcons.plans,
                    title = "Zacznij od planu",
                    description = "Złóż plan treningowy z bazy ćwiczeń — apka dopasuje go " +
                        "do Twojego sprzętu i ograniczeń.",
                    actionLabel = "Stwórz plan",
                    onAction = onNewPlan,
                )
            }
        }
    }
}

@Composable
private fun Header(
    displayName: String?,
    todayLabel: String,
    onOpenProfile: () -> Unit,
) {
    StronkScreenHeader(
        title = if (displayName.isNullOrBlank()) "stronk" else "Cześć, $displayName",
        subtitle = todayLabel,
        actions = {
            IconButton(onClick = onOpenProfile) {
                Icon(
                    StronkIcons.profile,
                    contentDescription = "Profil",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

/** Baner "trening w toku" — tap wraca do trwającej sesji (nic nie przepada). */
@Composable
private fun ActiveWorkoutCard(
    active: ActiveWorkoutUi,
    onResume: () -> Unit,
) {
    StronkCard(onClick = onResume) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StronkIconBadge(icon = StronkIcons.start, tone = StronkTone.ACCENT)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = StronkSpacing.sm),
            ) {
                Text(
                    text = "Trening w toku: ${active.dayName}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${active.completedSets} z ${active.totalSets} serii — wróć do treningu",
                    style = MaterialTheme.typography.bodySmall,
                    color = StronkTheme.colors.textDim,
                )
            }
        }
    }
}

@Composable
private fun TodayDoneCard() {
    StronkCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StronkIconBadge(icon = StronkIcons.done, tone = StronkTone.SUCCESS)
            Text(
                text = "Dzisiejszy trening zaliczony",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = StronkSpacing.sm),
            )
        }
    }
}

/** Karta treningu — jedyny focal point ekranu: kicker, nazwa dnia, lista, CTA. */
@Composable
private fun WorkoutCard(
    kicker: String,
    workout: ScheduledWorkoutUi,
    ctaLabel: String,
    onStartWorkout: (planId: String, dayIndex: Int, scheduleEntryId: String?) -> Unit,
    onExerciseClick: (exerciseId: String) -> Unit,
) {
    StronkCard {
        StronkSectionHeader(
            title = kicker,
            icon = StronkIcons.today,
            trailing = { StronkMetaChip("${workout.exercises.size} ćw.") },
        )
        Spacer(Modifier.height(StronkSpacing.sm))
        Text(
            text = workout.dayName,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = workout.planName,
            style = MaterialTheme.typography.bodySmall,
            color = StronkTheme.colors.textDim,
        )
        Spacer(Modifier.height(StronkSpacing.md))
        Column(verticalArrangement = Arrangement.spacedBy(StronkSpacing.row)) {
            workout.exercises.forEach { row ->
                ExerciseThumbnailRow(row = row, onClick = { onExerciseClick(row.exerciseId) })
            }
        }
        Spacer(Modifier.height(StronkSpacing.md))
        StronkPrimaryButton(
            text = ctaLabel,
            icon = StronkIcons.start,
            onClick = {
                onStartWorkout(workout.planId, workout.dayIndex, workout.scheduleEntryId)
            },
        )
    }
}

/**
 * Wiersz ćwiczenia z miniaturką zdjęcia zamiast piktogramu partii — tu, w
 * przeciwieństwie do [com.stronk.ui.components.StronkListRow], badge to realne
 * zdjęcie ćwiczenia (Karol: "nie da się kliknąć i zobaczyć jak wygląda").
 */
@Composable
private fun ExerciseThumbnailRow(row: HomeExerciseRow, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = StronkSpacing.xs,
                    top = StronkSpacing.xs,
                    end = 14.dp,
                    bottom = StronkSpacing.xs,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(StronkSpacing.sm),
        ) {
            AsyncImage(
                model = row.imageUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(StronkIconBadgeSize.MEDIUM.box)
                    .clip(RoundedCornerShape(StronkIconBadgeSize.MEDIUM.corner)),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = row.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (row.muscleLabel.isNotEmpty()) {
                    Text(
                        text = row.muscleLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = StronkTheme.colors.textDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = row.targetLabel,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SeeWeekLink(onOpenSchedule: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        StronkTextAction(
            text = "Zobacz cały tydzień",
            onClick = onOpenSchedule,
            icon = StronkIcons.week,
        )
    }
}
