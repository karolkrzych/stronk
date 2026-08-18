package com.stronk.ui.home

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Ekran startowy "Dziś": dzisiejszy (albo najbliższy) zaplanowany trening
 * z CTA "Zacznij trening", a bez harmonogramu/planów — zachęta do działania.
 * Układ wzorowany na karcie dnia z mocków (mock "Ekran 2").
 */
@Composable
fun HomeScreen(
    onStartWorkout: (planId: String, dayIndex: Int, scheduleEntryId: String?) -> Unit,
    onOpenSchedule: () -> Unit,
    onOpenPlans: () -> Unit,
    onNewPlan: () -> Unit,
    onOpenProfile: () -> Unit,
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
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Header(
                displayName = state.displayName,
                todayLabel = state.todayLabel,
                onOpenProfile = onOpenProfile,
            )
            Spacer(Modifier.height(16.dp))
            if (state.todayDone) {
                TodayDoneBanner()
                Spacer(Modifier.height(12.dp))
            }
            when (val content = state.content) {
                is HomeContent.TodayWorkout -> {
                    WorkoutCard(
                        heading = "Dziś",
                        workout = content.workout,
                        ctaLabel = "Zacznij trening",
                        onStartWorkout = onStartWorkout,
                    )
                    SeeWeekLink(onOpenSchedule)
                }

                is HomeContent.UpcomingWorkout -> {
                    if (!state.todayDone) {
                        Text(
                            text = "Dziś dzień odpoczynku",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    WorkoutCard(
                        heading = "Najbliższy trening · ${content.workout.dateLabel}",
                        workout = content.workout,
                        ctaLabel = "Zacznij teraz",
                        onStartWorkout = onStartWorkout,
                    )
                    SeeWeekLink(onOpenSchedule)
                }

                HomeContent.NoSchedule -> EmptyStateCard(
                    title = "Pusty tydzień",
                    message = "Masz już plan — zaplanuj z niego treningi na najbliższe dni.",
                    buttonLabel = "Zaplanuj tydzień",
                    onButtonClick = onOpenSchedule,
                    secondaryLabel = "Przejrzyj plany",
                    onSecondaryClick = onOpenPlans,
                )

                HomeContent.NoPlans -> EmptyStateCard(
                    title = "Zacznij od planu",
                    message = "Złóż plan treningowy z bazy ćwiczeń — apka dopasuje go " +
                        "do Twojego sprzętu i ograniczeń.",
                    buttonLabel = "Stwórz plan",
                    onButtonClick = onNewPlan,
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
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (displayName.isNullOrBlank()) "stronk" else "Cześć, $displayName",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = todayLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onOpenProfile) {
            Icon(Icons.Filled.Person, contentDescription = "Profil")
        }
    }
}

@Composable
private fun TodayDoneBanner() {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Dzisiejszy trening zaliczony",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

/** Karta treningu jak karta dnia z mocków: nagłówek, lista ćwiczeń, CTA. */
@Composable
private fun WorkoutCard(
    heading: String,
    workout: ScheduledWorkoutUi,
    ctaLabel: String,
    onStartWorkout: (planId: String, dayIndex: Int, scheduleEntryId: String?) -> Unit,
) {
    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = heading.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = workout.dayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${workout.exercises.size} ćw.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = workout.planName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            workout.exercises.forEach { row -> ExerciseRow(row) }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    onStartWorkout(workout.planId, workout.dayIndex, workout.scheduleEntryId)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(ctaLabel, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun ExerciseRow(row: HomeExerciseRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            if (row.muscleLabel.isNotEmpty()) {
                Text(
                    text = row.muscleLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = row.targetLabel,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SeeWeekLink(onOpenSchedule: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        TextButton(onClick = onOpenSchedule) { Text("Zobacz cały tydzień") }
    }
}

@Composable
private fun EmptyStateCard(
    title: String,
    message: String,
    buttonLabel: String,
    onButtonClick: () -> Unit,
    secondaryLabel: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
) {
    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onButtonClick,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(buttonLabel, style = MaterialTheme.typography.titleMedium)
            }
            if (secondaryLabel != null && onSecondaryClick != null) {
                TextButton(onClick = onSecondaryClick) { Text(secondaryLabel) }
            }
        }
    }
}
