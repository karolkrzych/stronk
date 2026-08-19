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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stronk.ui.components.StronkAccentBadge
import com.stronk.ui.components.StronkAccentCard
import com.stronk.ui.components.StronkEmptyState
import com.stronk.ui.components.StronkExerciseRow
import com.stronk.ui.components.StronkIcons
import com.stronk.ui.components.StronkScreenHeader
import com.stronk.ui.components.StronkSectionHeader
import com.stronk.ui.components.StronkSparkline
import com.stronk.ui.components.StronkStatBlock
import com.stronk.ui.components.StronkStatRow
import com.stronk.ui.components.StronkStatSize
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTextStyles
import com.stronk.ui.theme.StronkTheme

/**
 * Progres (mock `pack-progres-baza.html`, ekran 1) — dwa byty i koniec:
 * karta OSTATNI REKORD jako dominanta i lista MOJE ĆWICZENIA z mini-trendem.
 *
 * Wykres, przyrost i rozpisane serie siedzą O JEDEN TAP DALEJ — w zakładce
 * „Historia" ćwiczenia (chevron w wierszu). Ekran ma być prościuteńki.
 *
 * @param onExerciseClick otwiera historię ćwiczenia.
 */
@Composable
fun ProgressScreen(
    onExerciseClick: (exerciseId: String) -> Unit,
    viewModel: ProgressViewModel = viewModel(factory = ProgressViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = StronkSpacing.screen),
    ) {
        Spacer(Modifier.height(StronkSpacing.sm))
        StronkScreenHeader(title = "Progres")
        when {
            state.loading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            state.exercises.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                StronkEmptyState(
                    icon = StronkIcons.progress,
                    title = "Brak treningów",
                    description = "Rekordy i trendy pojawią się po pierwszym zapisanym treningu.",
                )
            }

            else -> ProgressContent(
                state = state,
                onExerciseClick = onExerciseClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ProgressContent(
    state: ProgressUiState,
    onExerciseClick: (exerciseId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(top = 14.dp, bottom = StronkSpacing.xxl),
    ) {
        state.lastRecord?.let { record ->
            item(key = "record") {
                LastRecordCard(record = record, onClick = { onExerciseClick(record.exerciseId) })
            }
        }
        item(key = "section") {
            StronkSectionHeader(
                title = "Moje ćwiczenia",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 26.dp, bottom = StronkSpacing.xxs),
                trailing = {
                    Text(
                        text = state.exercises.size.toString(),
                        style = StronkTextStyles.cap,
                        color = StronkTheme.colors.textDim,
                    )
                },
            )
        }
        items(state.exercises, key = { it.exerciseId }) { exercise ->
            StronkExerciseRow(
                exerciseId = exercise.exerciseId,
                title = exercise.name,
                trailingContent = {
                    StronkSparkline(
                        values = exercise.trend,
                        modifier = Modifier.padding(end = StronkSpacing.xxs),
                    )
                },
                chevron = true,
                divider = exercise.exerciseId != state.exercises.last().exerciseId,
                onClick = { onExerciseClick(exercise.exerciseId) },
            )
        }
    }
}

/**
 * Karta ostatniego rekordu (mock `.record`) — jedyna limonkowa plama ekranu:
 * kapitalik, badge PR, nazwa ćwiczenia i para statów CIĘŻAR / POWTÓRZENIA.
 */
@Composable
private fun LastRecordCard(record: LastRecordUi, onClick: () -> Unit) {
    StronkAccentCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StronkSectionHeader(title = "Ostatni rekord")
            StronkAccentBadge(text = "PR", icon = StronkIcons.record)
        }
        Text(
            text = record.name,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = StronkSpacing.sm),
        )
        StronkStatRow(modifier = Modifier.padding(top = 18.dp)) {
            StronkStatBlock(
                label = record.primary.label,
                value = record.primary.value,
                unit = record.primary.unit,
                size = StronkStatSize.BIG,
                valueColor = StronkTheme.colors.lime,
                modifier = Modifier.weight(1f),
            )
            record.secondary?.let { secondary ->
                StronkStatBlock(
                    label = secondary.label,
                    value = secondary.value,
                    unit = secondary.unit,
                    size = StronkStatSize.BIG,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
