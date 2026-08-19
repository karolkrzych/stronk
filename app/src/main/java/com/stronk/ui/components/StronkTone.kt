package com.stronk.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.stronk.ui.theme.StronkTheme

/**
 * Semantyka koloru dla chipów, badge'y i notek — w „Limonce" jest jej celowo
 * mało: cała apka ma JEDNĄ rodzinę barw, a znaczenie niosą ikona i słowo,
 * nie tęcza kolorów.
 *
 * - jasna limonka = akcja / teraz / dziś / PR
 * - limonka przygaszona = fakt z przeszłości (zrobione)
 * - reszta = szarości rodziny hue 80
 */
enum class StronkTone {
    /** Zwykła informacja — `--s2` + `--text-2`. */
    NEUTRAL,

    /** Wyróżnienie / stan wybrany / teraz — limonka. */
    ACCENT,

    /** Zrobione, PR, fakt z przeszłości — limonka przygaszona. */
    SUCCESS,

    /**
     * Ostrzeżenie: kontuzja, ograniczenie, ryzyko. W mockach „Limonka" NIE ma
     * własnego koloru — jest neutralne, a uwagę przyciąga ikona ostrzeżenia
     * i treść. Nie dokładaj tu bursztynu: to złamałoby jedną rodzinę barw.
     */
    WARNING,

    /** Błąd lub akcja nieodwracalna — jedyny kolor spoza rodziny. */
    DANGER,
}

/** Mocny kolor tonu (tekst na tle kontenera, obrys, krecha, ikona). */
@Composable
@ReadOnlyComposable
fun StronkTone.accentColor(): Color = when (this) {
    StronkTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
    StronkTone.ACCENT -> StronkTheme.colors.lime
    StronkTone.SUCCESS -> StronkTheme.colors.limeDeep
    StronkTone.WARNING -> MaterialTheme.colorScheme.onSurfaceVariant
    StronkTone.DANGER -> MaterialTheme.colorScheme.error
}

/** Tło kontenera tonu (badge, chip zaznaczony, karta rekordu). */
@Composable
@ReadOnlyComposable
fun StronkTone.containerColor(): Color = when (this) {
    StronkTone.NEUTRAL -> StronkTheme.colors.surfaceTile
    StronkTone.ACCENT, StronkTone.SUCCESS -> StronkTheme.colors.limeDim
    StronkTone.WARNING -> StronkTheme.colors.surfaceTile
    StronkTone.DANGER -> MaterialTheme.colorScheme.errorContainer
}

/** Kolor treści na [containerColor]. */
@Composable
@ReadOnlyComposable
fun StronkTone.onContainerColor(): Color = when (this) {
    StronkTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
    StronkTone.ACCENT, StronkTone.SUCCESS -> StronkTheme.colors.lime
    StronkTone.WARNING -> MaterialTheme.colorScheme.onSurfaceVariant
    StronkTone.DANGER -> MaterialTheme.colorScheme.onErrorContainer
}

/** Obrys elementu w tym tonie (mocki: `.chip.on` ma `--lime-line`). */
@Composable
@ReadOnlyComposable
fun StronkTone.outlineColor(): Color = when (this) {
    StronkTone.NEUTRAL -> StronkTheme.colors.line
    StronkTone.ACCENT, StronkTone.SUCCESS -> StronkTheme.colors.limeLine
    StronkTone.WARNING -> StronkTheme.colors.line
    StronkTone.DANGER -> MaterialTheme.colorScheme.error
}
