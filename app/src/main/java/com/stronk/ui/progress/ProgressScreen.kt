package com.stronk.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stronk.ui.components.MuscleIcons
import com.stronk.ui.components.StronkBadge
import com.stronk.ui.components.StronkCard
import com.stronk.ui.components.StronkChoiceChip
import com.stronk.ui.components.StronkEmptyState
import com.stronk.ui.components.StronkIconBadge
import com.stronk.ui.components.StronkIconBadgeSize
import com.stronk.ui.components.StronkIcons
import com.stronk.ui.components.StronkListRow
import com.stronk.ui.components.StronkScreenHeader
import com.stronk.ui.components.StronkSectionHeader
import com.stronk.ui.components.StronkStat
import com.stronk.ui.components.StronkStatRow
import com.stronk.ui.components.StronkTone
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTheme

/**
 * Progres (moduł 6 CONCEPT): dziennik treningów z rozwijanymi kartami,
 * lista ćwiczeń z historią (wejście w wykres) i rekordy osobiste z celebracją
 * nowych PR — bez gamifikacji ponad to.
 *
 * @param onExerciseClick otwiera wykres progresu ćwiczenia
 *   (trasa progress/exercise/{exerciseId}).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    onExerciseClick: (exerciseId: String) -> Unit,
    viewModel: ProgressViewModel = viewModel(factory = ProgressViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Column(modifier = Modifier.padding(horizontal = StronkSpacing.screen)) {
                Spacer(Modifier.height(StronkSpacing.sm))
                StronkScreenHeader(title = "Progres")
                Spacer(Modifier.height(StronkSpacing.sm))
                ProgressTabRow(selectedTab = selectedTab, onSelect = { selectedTab = it })
                Spacer(Modifier.height(StronkSpacing.xs))
            }
            when {
                state.loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                selectedTab == 0 -> HistoryTab(
                    state = state,
                    onToggleExpanded = viewModel::toggleExpanded,
                    onExerciseClick = onExerciseClick,
                )

                else -> ExercisesTab(state = state, onExerciseClick = onExerciseClick)
            }
        }
    }
}

@Composable
private fun ProgressTabRow(selectedTab: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(StronkSpacing.xs)) {
        StronkChoiceChip(label = "Historia", selected = selectedTab == 0, onClick = { onSelect(0) })
        StronkChoiceChip(label = "Ćwiczenia", selected = selectedTab == 1, onClick = { onSelect(1) })
    }
}

// --- Zakładka: dziennik treningów ---

@Composable
private fun HistoryTab(
    state: ProgressUiState,
    onToggleExpanded: (workoutId: String) -> Unit,
    onExerciseClick: (exerciseId: String) -> Unit,
) {
    if (state.history.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            StronkEmptyState(
                icon = StronkIcons.progress,
                title = "Brak treningów",
                description = "Dziennik i rekordy pojawią się po pierwszym zapisanym treningu.",
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = StronkSpacing.screen,
            end = StronkSpacing.screen,
            top = StronkSpacing.xs,
            bottom = StronkSpacing.xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(StronkSpacing.section),
    ) {
        if (state.celebrationLabels.isNotEmpty()) {
            item(key = "celebration") { CelebrationCard(state.celebrationLabels) }
        }
        items(state.history, key = { it.workoutId }) { item ->
            WorkoutHistoryCard(
                item = item,
                expanded = item.workoutId in state.expandedWorkoutIds,
                onToggle = { onToggleExpanded(item.workoutId) },
                onExerciseClick = onExerciseClick,
            )
        }
    }
}

/** Baner celebracji nowych rekordów z ostatniego treningu (focal point ekranu, gdy obecny). */
@Composable
private fun CelebrationCard(labels: List<String>) {
    StronkCard {
        Row {
            StronkIconBadge(icon = StronkIcons.record, tone = StronkTone.SUCCESS)
            Column(modifier = Modifier.weight(1f).padding(start = StronkSpacing.sm)) {
                Text(
                    text = if (labels.size == 1) "Nowy rekord!" else "Nowe rekordy!",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                labels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = StronkSpacing.xxs),
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutHistoryCard(
    item: WorkoutHistoryUi,
    expanded: Boolean,
    onToggle: () -> Unit,
    onExerciseClick: (exerciseId: String) -> Unit,
) {
    StronkCard(onClick = onToggle) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.dateLabel.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = StronkTheme.colors.textDim,
                )
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (item.prLabels.isNotEmpty()) {
                StronkBadge(
                    text = "PR",
                    tone = StronkTone.SUCCESS,
                    icon = StronkIcons.record,
                    modifier = Modifier.padding(end = StronkSpacing.xs),
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = if (expanded) "Zwiń" else "Rozwiń",
                tint = StronkTheme.colors.textDim,
            )
        }
        StronkStatRow(modifier = Modifier.padding(top = StronkSpacing.md)) {
            StronkStat(
                label = "ćwiczenia",
                value = item.exercisesCount.toString(),
                modifier = Modifier.weight(1f),
            )
            StronkStat(
                label = "serie",
                value = item.workingSetsCount.toString(),
                modifier = Modifier.weight(1f),
            )
            if (item.volumeKg > 0) {
                StronkStat(
                    label = "objętość",
                    value = ProgressFormat.volume(item.volumeKg),
                    valueColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1.4f),
                )
            }
        }
        if (expanded) {
            Column(
                modifier = Modifier.padding(top = StronkSpacing.md),
                verticalArrangement = Arrangement.spacedBy(StronkSpacing.row),
            ) {
                item.exercises.forEach { exercise ->
                    WorkoutExerciseRow(exercise = exercise, onClick = { onExerciseClick(exercise.exerciseId) })
                }
            }
        }
    }
}

/** Jeden wiersz ćwiczenia w rozwinięciu treningu; tap → wykres progresu. */
@Composable
private fun WorkoutExerciseRow(exercise: WorkoutExerciseUi, onClick: () -> Unit) {
    val setsSubtitle = exercise.sets.joinToString(" · ") { set ->
        if (set.isWarmup) "rozgrzewka · ${set.label}" else set.label
    }
    StronkListRow(
        title = exercise.name,
        icon = MuscleIcons.forMuscle(exercise.primaryMuscle),
        iconLabel = MuscleIcons.groupLabel(exercise.primaryMuscle),
        subtitle = setsSubtitle,
        inset = true,
        onClick = onClick,
    )
}

// --- Zakładka: ćwiczenia z historią ---

@Composable
private fun ExercisesTab(
    state: ProgressUiState,
    onExerciseClick: (exerciseId: String) -> Unit,
) {
    if (state.exercises.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            StronkEmptyState(
                icon = StronkIcons.database,
                title = "Brak danych",
                description = "Wykresy per ćwiczenie pojawią się po pierwszym zapisanym treningu.",
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = StronkSpacing.screen,
            end = StronkSpacing.screen,
            top = StronkSpacing.xs,
            bottom = StronkSpacing.xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(StronkSpacing.row),
    ) {
        item(key = "kicker") {
            StronkSectionHeader(
                title = "Ćwiczenia z historią",
                icon = StronkIcons.progress,
                modifier = Modifier.padding(bottom = StronkSpacing.xxs),
            )
        }
        items(state.exercises, key = { it.exerciseId }) { item ->
            val subtitle = listOfNotNull(item.subtitleLabel, item.bestLabel).joinToString(" · ")
            StronkListRow(
                title = item.name,
                icon = MuscleIcons.forMuscle(item.primaryMuscle),
                iconLabel = MuscleIcons.groupLabel(item.primaryMuscle),
                subtitle = subtitle,
                trailingContent = if (item.hasNewPr) {
                    { StronkIconBadge(icon = StronkIcons.record, size = StronkIconBadgeSize.SMALL, tone = StronkTone.SUCCESS) }
                } else {
                    null
                },
                onClick = { onExerciseClick(item.exerciseId) },
            )
        }
    }
}
