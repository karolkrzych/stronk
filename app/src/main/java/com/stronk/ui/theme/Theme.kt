package com.stronk.ui.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Motyw aplikacji — DARK ONLY, niezależnie od ustawienia systemu.
 * Wygląd jest wzorowany 1:1 na `mocks/alpha-screens.html` (zaakceptowane mocki):
 * jedna rodzina granatu + jeden akcent indygo, zieleń/bursztyn tylko jako semantyka.
 *
 * Role spoza Material 3 (tekst wygaszony, sukces, ostrzeżenie, badge piktogramu)
 * są w [StronkTheme.colors].
 */
@Composable
fun StronkTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalStronkColors provides StronkColorsDark) {
        MaterialTheme(
            colorScheme = StronkColorScheme,
            typography = StronkTypography,
            shapes = StronkShapes,
        ) {
            // Domyślny styl tekstu to tekst wspierający — duże napisy zawsze deklaruj
            // jawnie. Bez koloru: `Text` dziedziczy LocalContentColor powierzchni.
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.bodyMedium,
                content = content,
            )
        }
    }
}

/** Dostęp do ról koloru spoza Material 3: `StronkTheme.colors.warning` itd. */
object StronkTheme {
    val colors: StronkColors
        @Composable
        @ReadOnlyComposable
        get() = LocalStronkColors.current
}
