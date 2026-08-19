package com.stronk.ui.progress

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stronk.ui.components.StronkEmptyState
import com.stronk.ui.components.StronkExerciseRow
import com.stronk.ui.components.StronkIcons
import com.stronk.ui.components.StronkScreenHeader
import com.stronk.ui.components.StronkSectionHeader
import com.stronk.ui.components.StronkSparkline
import com.stronk.ui.components.StronkStatHeadline
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTextStyles
import com.stronk.ui.theme.StronkTheme

/**
 * Progres (mock `pack-progres-baza.html`, ekran 1) — dwa byty i koniec:
 * OSTATNI REKORD jako goły stat (dominanta, bez karty) i lista MOJE ĆWICZENIA
 * z mini-trendem.
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
                LastRecordHeadline(record = record, onClick = { onExerciseClick(record.exerciseId) })
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
 * Ostatni rekord jako GOŁY STAT (wariant A) — bez karty i bez limonkowego tła:
 * glif trofeum z kapitalikiem, nazwa ćwiczenia, para statów CIĘŻAR (hero,
 * w limonce) / POWTÓRZENIA, a data i szac. 1RM w chipach pod spodem.
 */
@Composable
private fun LastRecordHeadline(record: LastRecordUi, onClick: () -> Unit) {
    StronkStatHeadline(
        label = "Ostatni rekord",
        icon = StronkIcons.record,
        stats = recordStats(record.primary, record.secondary),
        title = record.name,
        chips = record.chips,
        onClick = onClick,
    )
}
