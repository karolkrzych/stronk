package com.stronk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stronk.ui.theme.StronkRadius
import com.stronk.ui.theme.StronkTextStyles
import com.stronk.ui.theme.StronkTheme

/** Stan kwadratu dnia w kalendarzu (mocki: `.day`, `.day.done/.plan/.today/.off`). */
enum class StronkDayState {
    /** Trening zrobiony — wypełnienie `--lime-deep`, liczba w `--lime-ink`. */
    DONE,

    /** Trening zaplanowany — przezroczyste tło, obrys `--line`. */
    PLANNED,

    /** Dzień wolny — powierzchnia `--s1`, liczba ledwie widoczna. */
    OFF,
}

/**
 * Kwadrat dnia (mocki: `.day`) — promień `--r-day` 7, proporcja 1:1, liczba dnia
 * w `--fs-meta`. To on jest dominantą ekranu Tydzień: siatka 7 w rzędzie z
 * odstępem 8 dp, nic więcej.
 *
 * „Dziś" to nie osobny stan tylko [today] = true — kwadrat zachowuje swój stan
 * (plan/zrobione/wolny) i dostaje dodatkowo limonkowy ring dookoła (mock:
 * `box-shadow: 0 0 0 2px var(--s0), 0 0 0 4px var(--lime)`). Ring rysuje się POZA
 * kwadratem, więc nie zmienia siatki — nie opakowuj go w dodatkowy padding.
 *
 * @param day liczba dnia miesiąca jako tekst, np. "6"
 * @param state stan dnia; [today] dokłada ring, nie zastępuje stanu
 */
@Composable
fun StronkDaySquare(
    day: String,
    state: StronkDayState,
    modifier: Modifier = Modifier,
    today: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val ringColor = StronkTheme.colors.lime
    val gapColor = MaterialTheme.colorScheme.background
    val background = when (state) {
        StronkDayState.DONE -> StronkTheme.colors.limeDeep
        StronkDayState.PLANNED -> Color.Transparent
        StronkDayState.OFF -> StronkTheme.colors.surfaceCard
    }
    val textColor = when (state) {
        StronkDayState.DONE -> StronkTheme.colors.limeInk
        StronkDayState.PLANNED -> MaterialTheme.colorScheme.onSurfaceVariant
        StronkDayState.OFF -> StronkTheme.colors.surfaceMuted
    }
    var box = modifier
        .aspectRatio(1f)
        .then(
            if (today) {
                Modifier.drawBehind {
                    val gap = 2.dp.toPx()
                    val radius = StronkRadius.day.toPx()
                    // pierścień „odstępu" w kolorze tła, tuż przy kwadracie
                    drawRing(gapColor, inset = gap / 2f, stroke = gap, radius = radius)
                    // limonkowy ring dziś — 2 dp dalej
                    drawRing(ringColor, inset = gap * 1.5f, stroke = gap, radius = radius)
                }
            } else {
                Modifier
            },
        )
        .background(background, StronkRadius.dayShape)
    if (state == StronkDayState.PLANNED) {
        box = box.border(1.5.dp, StronkTheme.colors.line, StronkRadius.dayShape)
    }
    if (onClick != null) {
        box = box.clickable(onClick = onClick)
    }
    Box(box, contentAlignment = Alignment.Center) {
        Text(
            text = day,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (state == StronkDayState.DONE) FontWeight.Bold else FontWeight.SemiBold,
            ),
            color = textColor,
            maxLines = 1,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRing(
    color: Color,
    inset: Float,
    stroke: Float,
    radius: Float,
) {
    drawRoundRect(
        color = color,
        topLeft = Offset(-inset, -inset),
        size = Size(size.width + inset * 2, size.height + inset * 2),
        cornerRadius = CornerRadius(radius + inset, radius + inset),
        style = Stroke(width = stroke),
    )
}

/** Nagłówek siatki — inicjały dni tygodnia w KAPITALIKACH (mocki: `.cal .wd`). */
@Composable
fun StronkWeekdayHeader(
    modifier: Modifier = Modifier,
    labels: List<String> = listOf("P", "W", "Ś", "C", "P", "S", "N"),
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        labels.forEach { label ->
            Text(
                text = label.uppercase(),
                style = StronkTextStyles.cap,
                color = StronkTheme.colors.textDim,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Legenda kalendarza (mocki: `.legend`) — MAKS 2 pozycje: „Zrobione" i „Plan".
 * Trzecia pozycja to znak, że siatka przestała być czytelna sama z siebie.
 *
 * Znaczniki to KWADRACIKI 12 dp o promieniu 4 dp (miniatury kwadratu dnia), nie
 * kółka: „zrobione" wypełnione `--lime-deep` — dokładnie tym, czym wypełnia się
 * kwadrat dnia — „plan" tylko obrysem `--line`.
 */
@Composable
fun StronkDayLegend(
    modifier: Modifier = Modifier,
    doneLabel: String = "Zrobione",
    plannedLabel: String = "Plan",
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendItem(doneLabel) {
            Box(
                Modifier
                    .size(12.dp)
                    .background(StronkTheme.colors.limeDeep, StronkRadius.swatchShape),
            )
        }
        LegendItem(plannedLabel) {
            Box(
                Modifier
                    .size(12.dp)
                    .border(1.5.dp, StronkTheme.colors.line, StronkRadius.swatchShape),
            )
        }
    }
}

@Composable
private fun LegendItem(label: String, swatch: @Composable () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        swatch()
        Text(
            text = label.uppercase(),
            style = StronkTextStyles.cap,
            color = StronkTheme.colors.textDim,
            maxLines = 1,
        )
    }
}
