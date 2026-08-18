package com.stronk.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stronk.ui.components.StronkCard
import com.stronk.ui.components.StronkChoiceChip
import com.stronk.ui.components.StronkEmptyState
import com.stronk.ui.components.StronkIconBadge
import com.stronk.ui.components.StronkIconBadgeSize
import com.stronk.ui.components.StronkIcons
import com.stronk.ui.components.StronkListRow
import com.stronk.ui.components.StronkScreenHeader
import com.stronk.ui.components.StronkSectionHeader
import com.stronk.ui.components.StronkTone
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTheme

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

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Column(modifier = Modifier.padding(horizontal = StronkSpacing.screen)) {
                Spacer(Modifier.height(StronkSpacing.sm))
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Wstecz",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StronkScreenHeader(
                    title = state.exerciseName,
                    actions = {
                        IconButton(onClick = { onExerciseDetailClick(exerciseId) }) {
                            Icon(
                                Icons.Rounded.Info,
                                contentDescription = "Szczegóły ćwiczenia",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                )
                Spacer(Modifier.height(StronkSpacing.xs))
            }
            when {
                state.loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                !state.hasHistory -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    StronkEmptyState(
                        icon = StronkIcons.progress,
                        title = "Brak historii",
                        description = "Wykres pojawi się po pierwszym treningu z tym ćwiczeniem.",
                    )
                }

                else -> ExerciseProgressContent(
                    state = state,
                    onMetricSelect = viewModel::onMetricSelect,
                )
            }
        }
    }
}

@Composable
private fun ExerciseProgressContent(
    state: ExerciseProgressUiState,
    onMetricSelect: (ChartMetric) -> Unit,
) {
    val metric = state.selectedMetric
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
        if (state.availableMetrics.size > 1) {
            item(key = "metrics") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(StronkSpacing.xs)) {
                    items(state.availableMetrics) { candidate ->
                        StronkChoiceChip(
                            label = ProgressFormat.metricLabel(candidate),
                            selected = candidate == metric,
                            onClick = { onMetricSelect(candidate) },
                        )
                    }
                }
            }
        }
        if (metric != null && state.points.isNotEmpty()) {
            item(key = "chart") { ChartCard(points = state.points, metric = metric) }
        }
        if (state.prRows.isNotEmpty()) {
            item(key = "pr-title") {
                StronkSectionHeader(
                    title = "Rekordy",
                    icon = StronkIcons.record,
                    modifier = Modifier.padding(bottom = StronkSpacing.xs),
                )
            }
            item(key = "pr-list") {
                Column(verticalArrangement = Arrangement.spacedBy(StronkSpacing.row)) {
                    state.prRows.forEach { row -> PrRow(row) }
                }
            }
        }
        item(key = "history-title") {
            StronkSectionHeader(
                title = "Historia",
                icon = StronkIcons.week,
                modifier = Modifier.padding(bottom = StronkSpacing.xs),
            )
        }
        items(state.sessions, key = { it.workoutId }) { session ->
            SessionRow(session, modifier = Modifier.padding(bottom = StronkSpacing.row))
        }
    }
}

@Composable
private fun ChartCard(points: List<ChartPoint>, metric: ChartMetric) {
    StronkCard {
        StronkSectionHeader(title = ProgressFormat.metricLabel(metric), icon = StronkIcons.progress)
        Spacer(Modifier.height(StronkSpacing.sm))
        ProgressLineChart(
            points = points,
            valueFormatter = { ProgressFormat.metricValue(metric, it) },
            modifier = Modifier.fillMaxWidth().height(220.dp),
        )
        if (points.size == 1) {
            Spacer(Modifier.height(StronkSpacing.xs))
            Text(
                text = "Trend pokaże się od drugiego treningu.",
                style = MaterialTheme.typography.bodySmall,
                color = StronkTheme.colors.textDim,
            )
        }
    }
}

@Composable
private fun PrRow(row: ExercisePrRowUi) {
    StronkListRow(
        title = "${row.kindLabel}: ${row.valueLabel}",
        subtitle = row.dateLabel,
        icon = StronkIcons.record,
        tone = StronkTone.SUCCESS,
    )
}

@Composable
private fun SessionRow(session: ExerciseSessionUi, modifier: Modifier = Modifier) {
    StronkListRow(
        title = session.setsLabel,
        subtitle = session.dateLabel,
        trailingContent = if (session.hasPr) {
            {
                StronkIconBadge(
                    icon = StronkIcons.record,
                    size = StronkIconBadgeSize.SMALL,
                    tone = StronkTone.SUCCESS,
                    contentDescription = "Rekord w tej sesji",
                )
            }
        } else {
            null
        },
        modifier = modifier,
    )
}
