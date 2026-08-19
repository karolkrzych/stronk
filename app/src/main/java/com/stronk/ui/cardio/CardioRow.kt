package com.stronk.ui.cardio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stronk.data.CardioType
import com.stronk.ui.components.StronkIconBadge
import com.stronk.ui.components.StronkSectionHeader
import com.stronk.ui.components.StronkTone
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTextStyles
import com.stronk.ui.theme.StronkTheme

/**
 * Wpis cardio w liście dnia (mock `round4/cardio-l1.html`, `.crow`).
 * Trzyma wartości surowe — formatowanie i etykiety robi [CardioTexts], a sheet
 * edycji dostaje z tego prefill.
 */
data class CardioRowUi(
    val id: String,
    val type: CardioType,
    val durationMin: Int,
    val distanceKm: Double?,
)

/** Piktogram typu cardio (mock: rower / biegacz / spacerowicz / zegar). */
internal fun cardioIcon(type: CardioType): ImageVector = when (type) {
    CardioType.BIKE -> Icons.AutoMirrored.Rounded.DirectionsBike
    CardioType.RUN -> Icons.AutoMirrored.Rounded.DirectionsRun
    CardioType.WALK -> Icons.AutoMirrored.Rounded.DirectionsWalk
    CardioType.OTHER -> Icons.Rounded.Timer
}

/**
 * Sekcja CARDIO pod listą ćwiczeń (mock: kapitalik `.sechd` + `.crow` +
 * `.addcardio`). Sekcja jest drugorzędna: dominantą ekranu zostaje karta dnia.
 *
 * @param onAdd null = sekcja tylko do czytania (Tydzień) — bez ghost-wiersza
 * @param onRowClick null = wiersze nieklikalne (Tydzień); edycja żyje w „Dziś"
 */
@Composable
fun CardioSection(
    rows: List<CardioRowUi>,
    modifier: Modifier = Modifier,
    onAdd: (() -> Unit)? = null,
    onRowClick: ((CardioRowUi) -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth()) {
        StronkSectionHeader(
            title = CardioTexts.SECTION_CARDIO,
            modifier = Modifier.fillMaxWidth(),
        )
        rows.forEach { row ->
            CardioRow(row = row, onClick = onRowClick?.let { click -> { click(row) } })
        }
        if (onAdd != null) {
            AddCardioRow(onClick = onAdd)
        }
    }
}

/**
 * Wiersz wpisu (mock: `.crow`) — piktogram, nazwa typu i STATY: CZAS oraz
 * (tylko gdy podany) DYSTANS. Każdy stat to osobny byt z własnym kapitalikiem;
 * fraza „42 min · 14,2 km" nie ma prawa tu powstać. Liczby w `--lime-deep`,
 * bo cardio na liście to fakt z przeszłości, nie akcja.
 */
@Composable
private fun CardioRow(row: CardioRowUi, onClick: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        StronkIconBadge(icon = cardioIcon(row.type), tone = StronkTone.SUCCESS)
        Text(
            text = CardioTexts.typeLabel(row.type),
            style = StronkTextStyles.h2,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            CardioStat(
                label = CardioTexts.LABEL_TIME,
                value = CardioTexts.minutesValue(row.durationMin),
                unit = CardioTexts.UNIT_MINUTES,
            )
            row.distanceKm?.let { km ->
                CardioStat(
                    label = CardioTexts.LABEL_DISTANCE,
                    value = CardioTexts.distanceValue(km),
                    unit = CardioTexts.UNIT_KILOMETERS,
                )
            }
        }
    }
}

/**
 * Kompaktowy stat wiersza (mock: `.cs`) — KAPITALIK nad liczbą, jednostka jako
 * mały sufiks, wszystko wyrównane do prawej. To ta sama zasada co
 * `StronkStatBlock`, tylko w rozmiarze wiersza listy (`--fs-h2` 17).
 */
@Composable
private fun CardioStat(label: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = label.uppercase(),
            style = StronkTextStyles.cap,
            color = StronkTheme.colors.textDim,
            maxLines = 1,
            textAlign = TextAlign.End,
        )
        Row(
            modifier = Modifier.padding(top = 4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = value,
                style = StronkTextStyles.h2,
                color = StronkTheme.colors.limeDeep,
                maxLines = 1,
            )
            Text(
                text = unit,
                style = StronkTextStyles.meta,
                color = StronkTheme.colors.textDim,
                maxLines = 1,
                modifier = Modifier.padding(start = 3.dp),
            )
        }
    }
}

/**
 * Ghost-wiersz „+ Dodaj cardio" (mock: `.addcardio`) — kreskowana linia u góry
 * i kreskowane kółko z plusem. Nic tu nie krzyczy: to zaproszenie, nie CTA.
 */
@Composable
private fun AddCardioRow(onClick: () -> Unit) {
    val line = StronkTheme.colors.lineSoft
    val outline = StronkTheme.colors.line
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = line,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(4.dp.toPx(), 4.dp.toPx()),
                    ),
                )
            }
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(StronkSpacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .drawBehind {
                    drawCircle(
                        color = outline,
                        radius = size.minDimension / 2f - 0.75.dp.toPx(),
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(3.dp.toPx(), 3.dp.toPx()),
                            ),
                        ),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                tint = StronkTheme.colors.textDim,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = CardioTexts.ADD_ROW,
            style = StronkTextStyles.bodyStrong,
            color = StronkTheme.colors.textDim,
            maxLines = 1,
        )
    }
}
