package com.stronk.ui.progress

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
 * Wykres progresu jednego ćwiczenia (moduł 6 CONCEPT): linia ciężaru
 * roboczego po treningach (przełącznik na objętość dla WEIGHT_REPS; inne
 * typy — powtórzenia/czas/dystans), rekordy osobiste i lista sesji.
 * Wykres ręcznie na Canvas — zero nowych zależności.
 *
 * @param exerciseId id ćwiczenia z bundlowanej bazy.
 * @param onBack powrót do listy progresu.
 * @param onExerciseDetailClick podgląd ćwiczenia w bazie (instrukcje/obrazki).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseProgressScreen(
    exerciseId: String,
    onBack: () -> Unit,
    onExerciseDetailClick: (exerciseId: String) -> Unit,
    viewModel: ExerciseProgressViewModel =
        viewModel(factory = ExerciseProgressViewModel.factory(exerciseId)),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.exerciseName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
                    }
                },
                actions = {
                    IconButton(onClick = { onExerciseDetailClick(exerciseId) }) {
                        Icon(Icons.Filled.Info, contentDescription = "Szczegóły ćwiczenia")
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.loading -> Box(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            !state.hasHistory -> Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(text = "Brak historii", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Wykres pojawi się po pierwszym treningu z tym ćwiczeniem.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> ExerciseProgressContent(
                state = state,
                onMetricSelect = viewModel::onMetricSelect,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun ExerciseProgressContent(
    state: ExerciseProgressUiState,
    onMetricSelect: (ChartMetric) -> Unit,
    modifier: Modifier = Modifier,
) {
    val metric = state.selectedMetric
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
    ) {
        if (state.availableMetrics.size > 1) {
            item(key = "metrics") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.availableMetrics) { candidate ->
                        FilterChip(
                            selected = candidate == metric,
                            onClick = { onMetricSelect(candidate) },
                            label = { Text(ProgressFormat.metricLabel(candidate)) },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
        if (metric != null && state.points.isNotEmpty()) {
            item(key = "chart") {
                ChartCard(points = state.points, metric = metric)
                Spacer(Modifier.height(16.dp))
            }
        }
        if (state.prLabels.isNotEmpty()) {
            item(key = "pr-title") { SectionTitle("Rekordy") }
            items(state.prLabels) { label -> PrRow(label) }
            item(key = "pr-gap") { Spacer(Modifier.height(16.dp)) }
        }
        item(key = "history-title") { SectionTitle("Historia") }
        items(state.sessions, key = { it.workoutId }) { session ->
            SessionRow(session)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun ChartCard(points: List<ChartPoint>, metric: ChartMetric) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = ProgressFormat.metricLabel(metric).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            ProgressLineChart(
                points = points,
                valueFormatter = { ProgressFormat.metricValue(metric, it) },
                modifier = Modifier.fillMaxWidth().height(220.dp),
            )
            if (points.size == 1) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Trend pokaże się od drugiego treningu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun PrRow(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Star,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SessionRow(session: ExerciseSessionUi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.dateLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = session.setsLabel, style = MaterialTheme.typography.bodyMedium)
        }
        if (session.hasPr) {
            Icon(
                Icons.Filled.Star,
                contentDescription = "Rekord w tej sesji",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
