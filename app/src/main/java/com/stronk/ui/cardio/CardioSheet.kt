package com.stronk.ui.cardio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stronk.data.CardioType
import com.stronk.ui.components.StronkChoiceChip
import com.stronk.ui.components.StronkGhostButton
import com.stronk.ui.components.StronkIcons
import com.stronk.ui.components.StronkPrimaryButton
import com.stronk.ui.theme.StronkRadius
import com.stronk.ui.theme.StronkSizes
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTextStyles
import com.stronk.ui.theme.StronkTheme

/** Pole dystansu (mock: `.distfield` 128×46). */
private val DistanceFieldWidth = 128.dp
private val DistanceFieldHeight = 46.dp

/**
 * Bottom sheet dodawania / zmiany cardio (mock `round4/cardio-l1.html`, ekran 2)
 * z JEDNĄ zmianą zamówioną przez Karola: minuty to DUŻE POLE LICZBOWE w stylu
 * stat-bloku z klawiaturą numeryczną — slidera nie ma.
 *
 * Trzy rzeczy na ekranie, w tej kolejności: typ (chipy), czas (dominanta),
 * dystans (wyraźnie opcjonalny). CTA „Zapisz" jest nieaktywne, dopóki minuty
 * są puste albo zerowe ([CardioTexts.canSave]).
 *
 * @param initial null = nowy wpis; niepuste = edycja (prefill + ghost „Usuń")
 * @param onSave typ, minuty i dystans (null = nie podano) — zapis robi ViewModel
 * @param onDelete kasowanie edytowanego wpisu; null = brak akcji kasowania
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardioSheet(
    initial: CardioRowUi?,
    onDismiss: () -> Unit,
    onSave: (type: CardioType, durationMin: Int, distanceKm: Double?) -> Unit,
    onDelete: ((entryId: String) -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var type by remember(initial?.id) { mutableStateOf(initial?.type ?: CardioType.BIKE) }
    var minutesText by remember(initial?.id) {
        mutableStateOf(initial?.let { CardioTexts.minutesInput(it.durationMin) }.orEmpty())
    }
    var distanceText by remember(initial?.id) {
        mutableStateOf(CardioTexts.distanceInput(initial?.distanceKm))
    }
    val minutesFocus = remember { FocusRequester() }

    // Klawiatura numeryczna od razu na polu minut — to jedyna wartość wymagana.
    // runCatching, bo przy szybkim zamknięciu sheetu requester bywa już odpięty.
    LaunchedEffect(initial?.id) { runCatching { minutesFocus.requestFocus() } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StronkTheme.colors.surfaceCard,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = StronkSpacing.screen)
                .padding(bottom = StronkSpacing.xxl),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (initial == null) CardioTexts.SHEET_TITLE_ADD else CardioTexts.SHEET_TITLE_EDIT,
                    style = StronkTextStyles.h1,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = StronkIcons.close,
                        contentDescription = "Zamknij",
                        tint = StronkTheme.colors.textDim,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            SheetCaption(CardioTexts.LABEL_TYPE, top = StronkSpacing.md)
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 11.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                CardioType.entries.forEach { option ->
                    StronkChoiceChip(
                        label = CardioTexts.typeLabel(option),
                        selected = option == type,
                        onClick = { type = option },
                        icon = cardioIcon(option),
                    )
                }
            }

            // CZAS — dominanta sheetu: stat-blok, w którym liczba jest polem.
            SheetCaption(CardioTexts.LABEL_TIME, top = 26.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                // Pole ma szerokość SWOJEJ liczby (miarka-duch pod spodem), żeby
                // „min" stało tuż przy wartości jak w mocku, a nie odpłynęło do
                // prawej krawędzi — BasicTextField sam z siebie bierze całą szerokość.
                Box {
                    Text(
                        text = minutesText.ifEmpty { CardioTexts.MINUTES_PLACEHOLDER },
                        style = StronkTextStyles.hero,
                        color = Color.Transparent,
                        maxLines = 1,
                    )
                    BasicTextField(
                        value = minutesText,
                        onValueChange = { minutesText = CardioTexts.sanitizeMinutes(it) },
                        modifier = Modifier
                            .matchParentSize()
                            .focusRequester(minutesFocus),
                        textStyle = StronkTextStyles.hero.copy(color = MaterialTheme.colorScheme.onSurface),
                        singleLine = true,
                        cursorBrush = SolidColor(StronkTheme.colors.lime),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        decorationBox = { field ->
                            Box {
                                if (minutesText.isEmpty()) {
                                    Text(
                                        text = CardioTexts.MINUTES_PLACEHOLDER,
                                        style = StronkTextStyles.hero,
                                        color = StronkTheme.colors.surfaceMuted,
                                        maxLines = 1,
                                    )
                                }
                                field()
                            }
                        },
                    )
                }
                Text(
                    text = CardioTexts.UNIT_MINUTES,
                    style = StronkTextStyles.unitHero,
                    color = StronkTheme.colors.textDim,
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
                )
            }

            // DYSTANS — pole opcjonalne: etykieta mówi to wprost, a puste pole
            // jest poprawnym stanem (CTA nie zależy od dystansu).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = CardioTexts.LABEL_DISTANCE,
                        style = StronkTextStyles.bodyStrong,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = CardioTexts.OPTIONAL,
                        style = StronkTextStyles.hint,
                        color = StronkTheme.colors.textDim,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                DistanceField(
                    value = distanceText,
                    onValueChange = { distanceText = CardioTexts.sanitizeDistance(it) },
                )
            }

            StronkPrimaryButton(
                text = CardioTexts.SAVE,
                onClick = {
                    val minutes = CardioTexts.parseMinutes(minutesText) ?: return@StronkPrimaryButton
                    onSave(type, minutes, CardioTexts.parseDistance(distanceText))
                },
                modifier = Modifier.padding(top = 28.dp),
                icon = StronkIcons.done,
                enabled = CardioTexts.canSave(minutesText),
            )

            if (initial != null && onDelete != null) {
                StronkGhostButton(
                    text = CardioTexts.DELETE,
                    onClick = { onDelete(initial.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = StronkSpacing.sm),
                    icon = StronkIcons.delete,
                    height = StronkSizes.ctaSmall,
                )
            }
        }
    }
}

/** KAPITALIK nad kontrolką sheetu (mock: `.cap`). */
@Composable
private fun SheetCaption(text: String, top: Dp) {
    Text(
        text = text.uppercase(),
        style = StronkTextStyles.cap,
        color = StronkTheme.colors.textDim,
        modifier = Modifier.padding(top = top),
    )
}

/** Pole dystansu (mock: `.distfield`) — liczba do prawej, „km" jako sufiks. */
@Composable
private fun DistanceField(value: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .width(DistanceFieldWidth)
            .height(DistanceFieldHeight)
            .background(StronkTheme.colors.surfaceTile, StronkRadius.innerShape)
            .border(1.dp, StronkTheme.colors.lineSoft, StronkRadius.innerShape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            textStyle = StronkTextStyles.h2.copy(
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
            ),
            singleLine = true,
            cursorBrush = SolidColor(StronkTheme.colors.lime),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            decorationBox = { field ->
                Box(contentAlignment = Alignment.CenterEnd) {
                    if (value.isEmpty()) {
                        Text(
                            text = CardioTexts.DISTANCE_PLACEHOLDER,
                            style = StronkTextStyles.h2,
                            color = StronkTheme.colors.textDim,
                            maxLines = 1,
                        )
                    }
                    field()
                }
            },
        )
        Text(
            text = CardioTexts.UNIT_KILOMETERS,
            style = StronkTextStyles.meta,
            color = StronkTheme.colors.textDim,
            modifier = Modifier.padding(start = 5.dp),
        )
    }
}
