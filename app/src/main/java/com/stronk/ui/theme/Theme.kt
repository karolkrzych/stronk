package com.stronk.ui.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Motyw aplikacji — DARK ONLY, niezależnie od ustawienia systemu.
 * Wygląd jest wzorowany 1:1 na mockach „Limonka" (`mocks/limonka/`, sekcje
 * `:root`): jedna rodzina NEUTRALNEJ czerni (saturacja 0, tło „plain ciemne")
 * + JEDEN akcent — stonowana limonka, maks. ~10% powierzchni ekranu. Zero
 * indygo, zero drugiego hue „na semantykę", zero podtonu na powierzchniach.
 *
 * Role spoza Material 3 (limonka i jej odcienie, powierzchnie s1/s2/s3, linie,
 * tekst wygaszony) są w [StronkTheme.colors]. Skala typograficzna z nazwami
 * mocków jest w `StronkTextStyles`, promienie w `StronkRadius`.
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
