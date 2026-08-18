package com.stronk.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.stronk.ui.theme.StronkTheme

/**
 * Semantyka koloru dla badge'y, chipów i komunikatów. Kolor NIGDY nie jest
 * dekoracją — niesie znaczenie danych (mocki: zieleń = zaliczone, bursztyn =
 * ostrzeżenie, indygo = focal point, neutral = zwykła informacja).
 */
enum class StronkTone {
    /** Zwykła informacja — szarość rodziny granatu. */
    NEUTRAL,

    /** Wyróżnienie / stan wybrany — akcent indygo. */
    ACCENT,

    /** Zaliczone, PR, sukces — zieleń. */
    SUCCESS,

    /** Ostrzeżenie: kontuzja, ograniczenie, ryzyko — bursztyn. */
    WARNING,

    /** Błąd lub akcja destrukcyjna — czerwień. */
    DANGER,
}

/** Mocny kolor tonu (tekst na tle kontenera, obrys, pasek). */
@Composable
@ReadOnlyComposable
fun StronkTone.accentColor(): Color = when (this) {
    StronkTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
    StronkTone.ACCENT -> MaterialTheme.colorScheme.primary
    StronkTone.SUCCESS -> StronkTheme.colors.success
    StronkTone.WARNING -> StronkTheme.colors.warning
    StronkTone.DANGER -> MaterialTheme.colorScheme.error
}

/** Tło kontenera tonu (badge, chip zaznaczony). */
@Composable
@ReadOnlyComposable
fun StronkTone.containerColor(): Color = when (this) {
    StronkTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant
    StronkTone.ACCENT -> MaterialTheme.colorScheme.primaryContainer
    StronkTone.SUCCESS -> StronkTheme.colors.successContainer
    StronkTone.WARNING -> StronkTheme.colors.warningContainer
    StronkTone.DANGER -> MaterialTheme.colorScheme.errorContainer
}

/** Kolor treści na [containerColor]. */
@Composable
@ReadOnlyComposable
fun StronkTone.onContainerColor(): Color = when (this) {
    StronkTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
    StronkTone.ACCENT -> MaterialTheme.colorScheme.onPrimaryContainer
    StronkTone.SUCCESS -> StronkTheme.colors.onSuccessContainer
    StronkTone.WARNING -> StronkTheme.colors.onWarningContainer
    StronkTone.DANGER -> MaterialTheme.colorScheme.onErrorContainer
}
