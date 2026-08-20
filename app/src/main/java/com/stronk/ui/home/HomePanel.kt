package com.stronk.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stronk.data.CardioType
import com.stronk.ui.cardio.CardioRowUi
import com.stronk.ui.cardio.CardioTexts
import com.stronk.ui.cardio.cardioIcon
import com.stronk.ui.components.StronkIcons
import com.stronk.ui.theme.StronkRadius
import com.stronk.ui.theme.StronkSizes
import com.stronk.ui.theme.StronkTextStyles
import com.stronk.ui.theme.StronkTheme

/** Kafelek piktogramu wiersza panelu (mock: `.prow .picon` 40 × 40, `--r-tile`). */
private val PanelIconTile = 40.dp

/** Kółko „+" przy cardio (mock: `.paddbtn` 34, `.paddbtn.small` 28 przy wpisie). */
private val AddButtonSize = 34.dp
private val AddButtonSizeSmall = 28.dp

/** Duża liczba wiersza (mock: `.pnum` 22/800) — Figtree 800 z cyframi tabelarycznymi. */
private val PanelNumber = StronkTextStyles.big.copy(
    fontSize = 22.sp,
    lineHeight = 26.sp,
    letterSpacing = (-0.44).sp,
)

/** Wartość tekstowa wiersza (mock: `.pval` 17/700). */
private val PanelValue = StronkTextStyles.h2.copy(fontWeight = FontWeight.Bold)

/** KAPITALIK mini-statu cardio (mock: `.pstat .cap` 9,5 z tym samym trackingiem). */
private val PanelStatCap = StronkTextStyles.cap.copy(fontSize = 9.5f.sp, letterSpacing = 1.33.sp)

/** Liczba mini-statu cardio (mock: `.pstat .v` 15/700 w `--lime-deep`). */
private val PanelStatValue = StronkTextStyles.bodyStrong.copy(fontWeight = FontWeight.Bold)

/**
 * Panel dolny ekranu „Dziś" (mock rundy 5 `wariant-c-strefy.html`, `.panel`) —
 * karta `--s1` przypięta nad dolną nawigacją, w niej wiersze ĆWICZENIA i CARDIO.
 *
 * Cała reszta ekranu nad panelem zostaje PUSTA: strefa treningu u góry, panel na
 * dole, oddech w środku. To jedyne miejsce, gdzie ekran mówi o liczbach — same
 * ćwiczenia są za chevronem (bottom sheet), nie na ekranie.
 *
 * @param exerciseCount null = nie ma treningu (pusty tydzień / brak planów), więc
 *        wiersza ĆWICZENIA w ogóle nie ma; CARDIO zostaje ZAWSZE — cardio nie
 *        zależy od siłowni
 * @param onAddCardio JEDYNY punkt wejścia do dodawania cardio w całej apce
 * @param onCardioClick tap we wpis → sheet edycji/usuwania
 */
@Composable
fun HomeBottomPanel(
    exerciseCount: Int?,
    cardio: List<CardioRowUi>,
    onExercisesClick: () -> Unit,
    onAddCardio: () -> Unit,
    onCardioClick: (CardioRowUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = StronkRadius.cardShape,
        color = StronkTheme.colors.surfaceCard,
    ) {
        Column {
            if (exerciseCount != null) {
                PanelRow(icon = StronkIcons.start, onClick = onExercisesClick) {
                    PanelLabel(
                        caption = HomeTexts.SECTION_EXERCISES,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = exerciseCount.toString(),
                            style = PanelNumber,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Icon(
                        imageVector = StronkIcons.chevron,
                        contentDescription = null,
                        tint = StronkTheme.colors.textDim,
                        modifier = Modifier.size(18.dp),
                    )
                }
                PanelDivider()
            }

            if (cardio.isEmpty()) {
                EmptyCardioRow(onAddCardio)
            } else {
                cardio.forEachIndexed { index, row ->
                    if (index > 0) PanelDivider()
                    CardioEntryRow(
                        row = row,
                        // KAPITALIK stoi raz nad pierwszym wpisem, a „+" raz pod
                        // ostatnim: powtórzony przy każdym wierszu byłby szumem
                        // i drugim punktem wejścia do tej samej akcji.
                        showCaption = index == 0,
                        onClick = { onCardioClick(row) },
                        onAdd = if (index == cardio.lastIndex) onAddCardio else null,
                    )
                }
            }
        }
    }
}

/** Pusty stan cardio — zaproszenie i plusik w jednym wierszu (mock: ramka 1). */
@Composable
private fun EmptyCardioRow(onAddCardio: () -> Unit) {
    PanelRow(
        icon = cardioIcon(CardioType.BIKE),
        iconTint = StronkTheme.colors.limeDeep,
        onClick = onAddCardio,
    ) {
        PanelLabel(caption = HomeTexts.SECTION_CARDIO, modifier = Modifier.weight(1f)) {
            Text(
                text = HomeTexts.ADD_CARDIO,
                style = StronkTextStyles.h2,
                color = StronkTheme.colors.textDim,
            )
        }
        AddButton(size = AddButtonSize, onClick = onAddCardio)
    }
}

/**
 * Wpis cardio (mock: ramka 4) — nazwa typu i STATY CZAS / DYSTANS, każdy z
 * własnym kapitalikiem. Liczby w `--lime-deep`: to fakt z przeszłości, nie akcja.
 */
@Composable
private fun CardioEntryRow(
    row: CardioRowUi,
    showCaption: Boolean,
    onClick: () -> Unit,
    onAdd: (() -> Unit)?,
) {
    PanelRow(icon = cardioIcon(row.type), iconTint = StronkTheme.colors.limeDeep, onClick = onClick) {
        PanelLabel(
            caption = HomeTexts.SECTION_CARDIO.takeIf { showCaption },
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = CardioTexts.typeLabel(row.type),
                style = PanelValue,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            PanelStat(
                label = CardioTexts.LABEL_TIME,
                value = CardioTexts.minutesValue(row.durationMin),
                unit = CardioTexts.UNIT_MINUTES,
            )
            row.distanceKm?.let { km ->
                PanelStatDivider()
                PanelStat(
                    label = CardioTexts.LABEL_DISTANCE,
                    value = CardioTexts.distanceValue(km),
                    unit = CardioTexts.UNIT_KILOMETERS,
                )
            }
        }
        if (onAdd != null) {
            AddButton(
                size = AddButtonSizeSmall,
                onClick = onAdd,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
    }
}

/** Szkielet wiersza panelu (mock: `.prow`) — kafelek ikony, treść, akcja z prawej. */
@Composable
private fun PanelRow(
    icon: ImageVector,
    onClick: (() -> Unit)?,
    iconTint: Color? = null,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier.size(PanelIconTile),
            shape = StronkRadius.tileShape,
            color = StronkTheme.colors.surfaceTile,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        content()
    }
}

/** KAPITALIK nad wartością wiersza (mock: `.pcap` + `.pnum` / `.pval`). */
@Composable
private fun PanelLabel(
    caption: String?,
    modifier: Modifier = Modifier,
    value: @Composable () -> Unit,
) {
    Column(modifier) {
        if (caption != null) {
            Text(
                text = caption.uppercase(),
                style = StronkTextStyles.cap,
                color = StronkTheme.colors.textDim,
                maxLines = 1,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
        value()
    }
}

/** Mini-stat po prawej stronie wiersza cardio (mock: `.pstat`). */
@Composable
private fun PanelStat(label: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = label.uppercase(),
            style = PanelStatCap,
            color = StronkTheme.colors.textDim,
            maxLines = 1,
            textAlign = TextAlign.End,
        )
        Row(
            modifier = Modifier.padding(top = 3.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(text = value, style = PanelStatValue, color = StronkTheme.colors.limeDeep, maxLines = 1)
            Text(
                text = unit,
                style = StronkTextStyles.hint,
                color = StronkTheme.colors.textDim,
                maxLines = 1,
                modifier = Modifier.padding(start = 3.dp),
            )
        }
    }
}

/** Pionowa kreska między statami cardio (mock: `.pstat-div`). */
@Composable
private fun PanelStatDivider() = Box(
    Modifier
        .padding(bottom = 2.dp)
        .width(StronkSizes.hairline)
        .height(24.dp)
        .background(StronkTheme.colors.line),
)

/** Dzielnik wierszy panelu (mock: `.prow + .prow` z `--line-soft`). */
@Composable
private fun PanelDivider() = HorizontalDivider(
    thickness = StronkSizes.hairline,
    color = StronkTheme.colors.lineSoft,
)

/**
 * Kółko „+" (mock: `.paddbtn`) — obrys `--lime-line`, znak `--lime`, bez
 * wypełnienia. Limonka na tak małej powierzchni mieści się w budżecie ~10%,
 * a to jedyne wejście do dodawania cardio.
 */
@Composable
private fun AddButton(size: Dp, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(size),
        shape = StronkRadius.pill,
        color = Color.Transparent,
        border = BorderStroke(StronkSizes.hairline, StronkTheme.colors.limeLine),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = StronkIcons.add,
                contentDescription = HomeTexts.ADD_CARDIO,
                tint = StronkTheme.colors.lime,
                modifier = Modifier.size(size * 0.47f),
            )
        }
    }
}
