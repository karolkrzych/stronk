package com.stronk.ui.progress

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Progres (moduł 6 CONCEPT): dziennik treningów z rozwijanymi szczegółami,
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

    Scaffold(
        topBar = { TopAppBar(title = { Text("Progres") }) },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Historia") },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Ćwiczenia") },
                )
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

// --- Zakładka: dziennik treningów ---

@Composable
private fun HistoryTab(
    state: ProgressUiState,
    onToggleExpanded: (workoutId: String) -> Unit,
    onExerciseClick: (exerciseId: String) -> Unit,
) {
    if (state.history.isEmpty()) {
        EmptyState(
            title = "Brak treningów",
            message = "Dziennik i rekordy pojawią się po pierwszym zapisanym treningu.",
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.celebrationLabels.isNotEmpty()) {
            item(key = "celebration") { CelebrationBanner(state.celebrationLabels) }
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

/** Baner celebracji nowych rekordów z ostatniego treningu. */
@Composable
private fun CelebrationBanner(labels: List<String>) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(14.dp)) {
            Icon(
                Icons.Filled.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = if (labels.size == 1) "Nowy rekord!" else "Nowe rekordy!",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                labels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
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
    Card(
        onClick = onToggle,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.dateLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (item.prLabels.isNotEmpty()) {
                    PrBadge()
                    Spacer(Modifier.width(8.dp))
                }
                Icon(
                    imageVector = if (expanded) {
                        Icons.Filled.KeyboardArrowUp
                    } else {
                        Icons.Filled.KeyboardArrowDown
                    },
                    contentDescription = if (expanded) "Zwiń" else "Rozwiń",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.summaryLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (expanded) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                item.exercises.forEach { exercise ->
                    WorkoutExerciseDetail(
                        exercise = exercise,
                        onClick = { onExerciseClick(exercise.exerciseId) },
                    )
                }
            }
        }
    }
}

/** Rozwinięcie: serie jednego ćwiczenia; tap na nazwę → wykres progresu. */
@Composable
private fun WorkoutExerciseDetail(
    exercise: WorkoutExerciseUi,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = exercise.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
        exercise.sets.forEach { set ->
            Text(
                text = if (set.isWarmup) "rozgrzewka · ${set.label}" else set.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// --- Zakładka: ćwiczenia z historią ---

@Composable
private fun ExercisesTab(
    state: ProgressUiState,
    onExerciseClick: (exerciseId: String) -> Unit,
) {
    if (state.exercises.isEmpty()) {
        EmptyState(
            title = "Brak danych",
            message = "Wykresy per ćwiczenie pojawią się po pierwszym zapisanym treningu.",
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(state.exercises, key = { it.exerciseId }) { item ->
            ListItem(
                modifier = Modifier.clickable { onExerciseClick(item.exerciseId) },
                headlineContent = { Text(item.name) },
                supportingContent = {
                    Column {
                        Text(item.subtitleLabel)
                        item.bestLabel?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                },
                trailingContent = { if (item.hasNewPr) PrBadge() },
            )
        }
    }
}

/** Mały badge "PR" — sygnał nowego rekordu z ostatniego treningu. */
@Composable
private fun PrBadge() {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "PR",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

@Composable
private fun EmptyState(title: String, message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
