package com.stronk.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Tokeny kolorów 1:1 z mockami (`mocks/alpha-screens.html`, sekcja `:root`).
 * Jedna rodzina granatu (hue ~224) + jeden mocny akcent indygo; zieleń i bursztyn
 * WYŁĄCZNIE jako semantyka danych (zaliczone / ostrzeżenie).
 *
 * Nazwy odpowiadają zmiennym CSS z mocków, żeby dało się je porównać wzrokiem.
 */
internal object StronkTokens {
    /** `--page` — najciemniejsze tło (za ekranem, paski systemowe). */
    val Page = Color.hsl(224f, 0.30f, 0.04f)

    /** `--s0` — tło ekranu. */
    val S0 = Color.hsl(224f, 0.22f, 0.08f)

    /** Poziom pośredni między tłem a kartą (nie ma w mockach, potrzebny M3). */
    val S05 = Color.hsl(224f, 0.21f, 0.11f)

    /** `--s1` — karta. */
    val S1 = Color.hsl(224f, 0.20f, 0.14f)

    /** Poziom pośredni między kartą a elementem na karcie. */
    val S15 = Color.hsl(224f, 0.19f, 0.17f)

    /** `--s2` — element na karcie (wiersz, kafelek statystyki, chip). */
    val S2 = Color.hsl(223f, 0.18f, 0.21f)

    /** Najjaśniejsza powierzchnia rodziny (rzadko — np. stan wciśnięcia). */
    val S3 = Color.hsl(223f, 0.18f, 0.24f)

    /** `--line` — obrys wyraźny. */
    val Line = Color.hsl(224f, 0.16f, 0.26f)

    /** `--line-soft` — obrys kart i wierszy. */
    val LineSoft = Color.hsl(224f, 0.18f, 0.19f)

    /** `--text` — tekst główny. */
    val Text = Color.hsl(224f, 0.40f, 0.95f)

    /** `--text-2` — tekst wspierający. */
    val Text2 = Color.hsl(224f, 0.16f, 0.70f)

    /** `--text-3` — tekst wygaszony (podpisy, kickery sekcji). */
    val Text3 = Color.hsl(224f, 0.12f, 0.49f)

    /** `--accent` — jedyny mocny akcent, focal point ekranu. */
    val Accent = Color.hsl(226f, 0.90f, 0.64f)

    /** `--accent-deep` — akcent przygaszony (stan wciśnięcia, obrysy). */
    val AccentDeep = Color.hsl(226f, 0.70f, 0.55f)

    /** Kontener w rodzinie akcentu (tło pod treścią akcentowaną). */
    val AccentContainer = Color.hsl(226f, 0.48f, 0.20f)

    /** Tekst/ikona na [AccentContainer]. */
    val OnAccentContainer = Color.hsl(226f, 0.80f, 0.88f)

    /** `--fill-dim` — neutralne wypełnienia progresu (wciąż rodzina granatu). */
    val FillDim = Color.hsl(224f, 0.30f, 0.38f)

    /** `--ico` — piktogramy partii mięśniowych. */
    val Ico = Color.hsl(226f, 0.65f, 0.76f)

    /** `--ico-bg` — kwadrat pod piktogramem (odpowiednik hsla ~12% alfa, spłaszczony). */
    val IcoBg = Color.hsl(226f, 0.42f, 0.17f)

    /** `--ico-line` — obrys kwadratu piktogramu. */
    val IcoLine = Color.hsl(226f, 0.38f, 0.28f)

    /** `--ok` — semantyka „zaliczone”. */
    val Ok = Color.hsl(155f, 0.60f, 0.47f)

    /** Tło kontenera sukcesu. */
    val OkContainer = Color.hsl(155f, 0.45f, 0.15f)

    /** Tekst/ikona na [OkContainer]. */
    val OnOkContainer = Color.hsl(155f, 0.55f, 0.78f)

    /** `--warn` — semantyka ostrzeżenia (kontuzje, ograniczenia). */
    val Warn = Color.hsl(35f, 0.78f, 0.57f)

    /** Tło kontenera ostrzeżenia. */
    val WarnContainer = Color.hsl(35f, 0.55f, 0.16f)

    /** Tekst/ikona na [WarnContainer]. */
    val OnWarnContainer = Color.hsl(35f, 0.72f, 0.80f)

    /** Błąd / akcja destrukcyjna — jedyny czerwony w apce, poza paletą mocków. */
    val Danger = Color.hsl(356f, 0.70f, 0.63f)

    val DangerContainer = Color.hsl(356f, 0.45f, 0.17f)

    val OnDangerContainer = Color.hsl(356f, 0.70f, 0.85f)

    val White = Color.hsl(0f, 0f, 1f)

    val Black = Color.hsl(0f, 0f, 0f)
}

/**
 * Ciemny (i jedyny) schemat kolorów aplikacji — mapowanie tokenów mocków na role
 * Material 3. Ekrany nie mają prawa do własnych `Color(0xFF…)`.
 */
internal val StronkColorScheme = darkColorScheme(
    primary = StronkTokens.Accent,
    onPrimary = StronkTokens.White,
    primaryContainer = StronkTokens.AccentContainer,
    onPrimaryContainer = StronkTokens.OnAccentContainer,
    inversePrimary = StronkTokens.AccentDeep,

    secondary = StronkTokens.Ico,
    onSecondary = StronkTokens.Page,
    secondaryContainer = StronkTokens.IcoBg,
    onSecondaryContainer = StronkTokens.Ico,

    // tertiary = semantyka sukcesu (zaliczone serie, PR-y, dni odhaczone)
    tertiary = StronkTokens.Ok,
    onTertiary = StronkTokens.Page,
    tertiaryContainer = StronkTokens.OkContainer,
    onTertiaryContainer = StronkTokens.OnOkContainer,

    background = StronkTokens.S0,
    onBackground = StronkTokens.Text,

    surface = StronkTokens.S0,
    onSurface = StronkTokens.Text,
    surfaceVariant = StronkTokens.S2,
    onSurfaceVariant = StronkTokens.Text2,
    surfaceTint = StronkTokens.Accent,

    surfaceDim = StronkTokens.Page,
    surfaceBright = StronkTokens.S3,
    surfaceContainerLowest = StronkTokens.Page,
    surfaceContainerLow = StronkTokens.S05,
    surfaceContainer = StronkTokens.S1,
    surfaceContainerHigh = StronkTokens.S15,
    surfaceContainerHighest = StronkTokens.S2,

    inverseSurface = StronkTokens.Text,
    inverseOnSurface = StronkTokens.Page,

    outline = StronkTokens.Line,
    outlineVariant = StronkTokens.LineSoft,

    error = StronkTokens.Danger,
    onError = StronkTokens.White,
    errorContainer = StronkTokens.DangerContainer,
    onErrorContainer = StronkTokens.OnDangerContainer,

    scrim = StronkTokens.Black,
)

/**
 * Role, których Material 3 nie ma, a mocki wymagają. Dostęp: `StronkTheme.colors`.
 * Nadal jeden motyw — to nie jest furtka do własnych kolorów w ekranach.
 */
@Immutable
data class StronkColors(
    /** Tekst wygaszony (`--text-3`): podpisy, kickery sekcji, meta. */
    val textDim: Color,
    /** Neutralne wypełnienie progresu (`--fill-dim`) — pasek, segmenty, tło wykresu. */
    val fillDim: Color,
    /** Kolor piktogramu w kwadratowym badge'u (`--ico`). */
    val iconBadgeContent: Color,
    /** Tło kwadratowego badge'a (`--ico-bg`). */
    val iconBadgeBackground: Color,
    /** Obrys kwadratowego badge'a (`--ico-line`). */
    val iconBadgeBorder: Color,
    /** Sukces — zaliczone, PR, dzień odhaczony (`--ok`). */
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    /** Ostrzeżenie — kontuzje, ograniczenia, ryzyko (`--warn`). */
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
)

internal val StronkColorsDark = StronkColors(
    textDim = StronkTokens.Text3,
    fillDim = StronkTokens.FillDim,
    iconBadgeContent = StronkTokens.Ico,
    iconBadgeBackground = StronkTokens.IcoBg,
    iconBadgeBorder = StronkTokens.IcoLine,
    success = StronkTokens.Ok,
    successContainer = StronkTokens.OkContainer,
    onSuccessContainer = StronkTokens.OnOkContainer,
    warning = StronkTokens.Warn,
    warningContainer = StronkTokens.WarnContainer,
    onWarningContainer = StronkTokens.OnWarnContainer,
)

internal val LocalStronkColors = staticCompositionLocalOf { StronkColorsDark }
