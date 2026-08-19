package com.stronk.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stronk.ui.components.StronkBar
import com.stronk.ui.components.StronkBarChart
import com.stronk.ui.components.StronkEmptyState
import com.stronk.ui.components.StronkIcons
import com.stronk.ui.components.StronkSectionHeader
import com.stronk.ui.components.StronkStatHeadline
import com.stronk.ui.components.StronkStatItem
import com.stronk.ui.components.StronkStatSize
import com.stronk.ui.theme.StronkSizes
import com.stronk.ui.theme.StronkTextStyles
import com.stronk.ui.theme.StronkTheme

/**
 * Zakładka „Historia" ćwiczenia — JEDYNY widok historii w apce (mock
 * `pack-historia-profil.html`, ramka 1). Używa go szczegół ćwiczenia
 * (`ui/detail`) i wejście z Progresu, więc historia wygląda tak samo
 * niezależnie od tego, skąd się w nią weszło.
 *
 * Trzy warstwy, w tej kolejności: REKORD jako goły stat (dominanta, wariant A —
 * bez karty) → wykres słupkowy z przyrostem → tabela sesji. Nigdzie frazy
 * „kg × powt.": jednostki stoją RAZ w nagłówku, w komórkach są same liczby.
 */
@Composable
fun ExerciseHistorySection(
    state: ExerciseProgressUiState,
    modifier: Modifier = Modifier,
) {
    // Dopóki lecą pierwsze snapshoty, nie migamy „Brak historii" — pusto jest uczciwsze.
    if (state.loading) return
    if (!state.hasHistory) {
        StronkEmptyState(
            icon = StronkIcons.progress,
            title = "Brak historii",
            description = "Rekord i wykres pojawią się po pierwszym treningu z tym ćwiczeniem.",
            modifier = modifier,
        )
        return
    }
    Column(modifier = modifier.fillMaxWidth()) {
        state.record?.let { RecordHeadline(it) }
        if (state.bars.isNotEmpty()) {
            ChartHead(caption = state.chartCaption, delta = state.delta)
            Spacer(Modifier.height(10.dp))
            StronkBarChart(
                bars = state.bars.map { StronkBar(value = it.value, label = it.label, highlight = it.isRecord) },
                height = StronkSizes.chart,
            )
        }
        if (state.sessions.isNotEmpty()) {
            SessionTable(
                sessions = state.sessions,
                rail = state.railLabels,
                columnCount = state.columnCount,
                modifier = Modifier.padding(top = 18.dp),
            )
        }
    }
}

/**
 * Rekord jako GOŁY STAT (wariant A) — żadnej karty i żadnego tła: glif trofeum
 * z kapitalikiem, pod nim CIĘŻAR (hero, w limonce) i POWTÓRZENIA (big), a fakty
 * poboczne (data, szac. 1RM) w rzędzie chipów.
 */
@Composable
private fun RecordHeadline(record: ExerciseRecordUi) {
    StronkStatHeadline(
        label = "Rekord",
        icon = StronkIcons.record,
        stats = recordStats(record.primary, record.secondary),
        chips = record.chips,
        modifier = Modifier.padding(top = 18.dp),
    )
}

/**
 * Para statów rekordu: liczba wiodąca HERO w limonce, druga BIG w `--text`.
 * Wspólna dla Historii i Progresu, żeby rekord wyglądał identycznie w obu.
 */
internal fun recordStats(primary: StatValueUi, secondary: StatValueUi?): List<StronkStatItem> =
    listOfNotNull(
        StronkStatItem(
            label = primary.label,
            value = primary.value,
            unit = primary.unit,
            size = StronkStatSize.HERO,
            accent = true,
        ),
        secondary?.let {
            StronkStatItem(label = it.label, value = it.value, unit = it.unit)
        },
    )

/** Nagłówek wykresu (mock `.chart-head`): kapitalik metryki + mini-stat przyrostu. */
@Composable
private fun ChartHead(caption: String, delta: StatValueUi?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 22.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        StronkSectionHeader(title = caption)
        if (delta != null) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(
                    text = delta.label.uppercase(),
                    style = StronkTextStyles.cap,
                    color = StronkTheme.colors.textDim,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = delta.value,
                        style = StronkTextStyles.h2,
                        color = StronkTheme.colors.limeDeep,
                    )
                    delta.unit?.let {
                        Text(
                            text = it,
                            style = StronkTextStyles.meta,
                            color = StronkTheme.colors.textDim,
                            modifier = Modifier.padding(start = 3.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Szerokość lewej szyny tabeli sesji (mock: `grid-template-columns:66px …`). */
private val SessionRailWidth = 66.dp

/**
 * Tabela sesji (mock `.sess`): kapitaliki KG / POWT. RAZ w lewej szynie
 * nagłówka, kolumny SERIA 1..N, wiersz z rekordem świeci limonką.
 */
@Composable
private fun SessionTable(
    sessions: List<ExerciseSessionUi>,
    rail: SessionRailLabels,
    columnCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 9.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(
                modifier = Modifier.width(SessionRailWidth),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = rail.main.uppercase(),
                    style = StronkTextStyles.cap,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                rail.sub?.let {
                    Text(
                        text = it.uppercase(),
                        style = StronkTextStyles.cap,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            repeat(columnCount) { index ->
                Text(
                    text = "Seria ${index + 1}".uppercase(),
                    style = StronkTextStyles.cap,
                    color = StronkTheme.colors.textDim,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        HorizontalDivider(thickness = StronkSizes.hairline, color = StronkTheme.colors.line)
        sessions.forEachIndexed { index, session ->
            SessionRow(session)
            if (index != sessions.lastIndex) {
                HorizontalDivider(thickness = StronkSizes.hairline, color = StronkTheme.colors.lineSoft)
            }
        }
    }
}

@Composable
private fun SessionRow(session: ExerciseSessionUi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.width(SessionRailWidth),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = session.dateLabel,
                style = StronkTextStyles.meta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (session.hasPr) {
                Text(
                    text = "PR",
                    // mock `.srow .tag`: 9 px, tracking kapitalika, limonka
                    style = StronkTextStyles.cap.copy(fontSize = 9.sp),
                    color = StronkTheme.colors.lime,
                )
            }
        }
        session.cells.forEach { cell ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    // mock `.srow .c .kg`: 19 px / 600
                    text = cell.main.orEmpty(),
                    style = StronkTextStyles.h2.copy(fontSize = 19.sp, lineHeight = 21.sp),
                    color = if (session.hasPr) {
                        StronkTheme.colors.lime
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                )
                cell.sub?.let {
                    Text(
                        text = it,
                        style = StronkTextStyles.meta,
                        color = StronkTheme.colors.textDim,
                        modifier = Modifier.padding(top = 1.dp),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
