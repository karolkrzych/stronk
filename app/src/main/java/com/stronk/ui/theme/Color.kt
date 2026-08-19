package com.stronk.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Tokeny kolorów „Limonka" — 1:1 z sekcją `:root` mocków w `mocks/limonka/`
 * (`wariant-c2-limonka.html` + `pack-*.html`).
 *
 * Jedna rodzina powierzchni: czerń o hue 80 z minimalną saturacją. JEDEN akcent:
 * stonowana limonka, maks. ~10% powierzchni ekranu — jasna [Lime] to akcja /
 * teraz / dziś / PR, przygaszona [LimeDeep] to fakty z przeszłości.
 *
 * Zero indygo, zero fioletu, zero drugiego hue „na semantykę" — ostrzeżenia
 * i ryzyko niosą IKONY i słowa, nie kolor (tak jak w zaakceptowanych mockach).
 * Jedyny wyjątek to [Danger] dla akcji nieodwracalnych.
 *
 * Nazwy odpowiadają zmiennym CSS z mocków, żeby dało się je porównać wzrokiem.
 */
internal object StronkTokens {
    /** `--page` — najciemniejsze tło (za ekranem, paski systemowe). */
    val Page = Color.hsl(80f, 0.04f, 0.04f)

    /** `--s0` — tło ekranu i dolnej nawigacji. */
    val S0 = Color.hsl(80f, 0.04f, 0.06f)

    /** Poziom pośredni między tłem a kartą (nie ma w mockach, potrzebny M3). */
    val S05 = Color.hsl(80f, 0.04f, 0.08f)

    /** `--s1` — karta, pole wyszukiwania, kontener tabów. */
    val S1 = Color.hsl(80f, 0.04f, 0.10f)

    /** Poziom pośredni między kartą a elementem na karcie. */
    val S15 = Color.hsl(78f, 0.04f, 0.12f)

    /** `--s2` — element na karcie: kafelek ikony, chip, tor pierścienia. */
    val S2 = Color.hsl(75f, 0.04f, 0.14f)

    /** `--s3` — placeholder, tor paska, pusty dzień, aktywny segment tabów. */
    val S3 = Color.hsl(75f, 0.04f, 0.19f)

    /** `--line` — obrys wyraźny (ghost-przycisk, dzielnik statów, dzień „plan"). */
    val Line = Color.hsl(75f, 0.04f, 0.17f)

    /** `--line-soft` — dzielnik wierszy i linia nad dolną nawigacją. */
    val LineSoft = Color.hsl(75f, 0.04f, 0.12f)

    /** `--text` — tekst główny i liczby. */
    val Text = Color.hsl(70f, 0.08f, 0.96f)

    /** `--text-2` — tekst wspierający (nazwy na listach, ghost-przyciski). */
    val Text2 = Color.hsl(70f, 0.05f, 0.70f)

    /** `--text-3` — tekst wygaszony: KAPITALIKI statów, meta, ikony w kafelkach. */
    val Text3 = Color.hsl(70f, 0.04f, 0.46f)

    /** `--lime` — jedyny akcent: akcja / teraz / dziś / PR. */
    val Lime = Color.hsl(75f, 0.70f, 0.52f)

    /** `--lime-deep` — limonka przyciszona: fakty z przeszłości (zrobione dni). */
    val LimeDeep = Color.hsl(75f, 0.55f, 0.40f)

    /** `--lime-ink` — tekst NA limonce (CTA, wypełniony dzień, badge PR). */
    val LimeInk = Color.hsl(80f, 0.60f, 0.07f)

    /** `--lime-dim` — tint 13%: tło karty rekordu, zaznaczony chip, ghost-accent. */
    val LimeDim = Lime.copy(alpha = 0.13f)

    /** `--lime-line` — obrys 30%: karta rekordu, ghost-accent, zaznaczony chip. */
    val LimeLine = Lime.copy(alpha = 0.30f)

    /** Błąd / akcja nieodwracalna — jedyny kolor spoza rodziny, celowo stonowany. */
    val Danger = Color.hsl(6f, 0.55f, 0.58f)

    val DangerContainer = Color.hsl(6f, 0.30f, 0.16f)

    val OnDangerContainer = Color.hsl(6f, 0.50f, 0.82f)

    val White = Color.hsl(0f, 0f, 1f)

    val Black = Color.hsl(0f, 0f, 0f)

    // ---------------------------------------------------------------------
    // Aliasy starych nazw (paleta granat+indygo). Wskazują nowe wartości, żeby
    // kod sprzed rundy „Limonka" dalej się kompilował. NIE używaj ich w nowym
    // kodzie — mają zniknąć razem z ostatnim ekranem, który je woła.
    // ---------------------------------------------------------------------

    /** @deprecated alias [Lime]. */
    val Accent = Lime

    /** @deprecated alias [LimeDeep]. */
    val AccentDeep = LimeDeep

    /** @deprecated alias [LimeDim]. */
    val AccentContainer = LimeDim

    /** @deprecated alias [Lime]. */
    val OnAccentContainer = Lime

    /** @deprecated alias [S3] — tor paska postępu. */
    val FillDim = S3

    /** @deprecated alias [Text3] — ikona w kafelku. */
    val Ico = Text3

    /** @deprecated alias [S2] — kafelek ikony. */
    val IcoBg = S2

    /** @deprecated alias [S2] — kafelek ikony nie ma w mockach obrysu. */
    val IcoLine = S2

    /** @deprecated „zaliczone" to teraz przeszłość = [LimeDeep], nie zieleń. */
    val Ok = LimeDeep

    /** @deprecated alias [LimeDim]. */
    val OkContainer = LimeDim

    /** @deprecated alias [Lime]. */
    val OnOkContainer = Lime

    /** @deprecated ostrzeżenie jest neutralne (ikona + słowo), nie bursztynowe. */
    val Warn = Text2

    /** @deprecated alias [S2]. */
    val WarnContainer = S2

    /** @deprecated alias [Text]. */
    val OnWarnContainer = Text
}

/**
 * Ciemny (i jedyny) schemat kolorów aplikacji — mapowanie tokenów mocków na role
 * Material 3. Ekrany nie mają prawa do własnych `Color(0xFF…)`.
 */
internal val StronkColorScheme = darkColorScheme(
    primary = StronkTokens.Lime,
    onPrimary = StronkTokens.LimeInk,
    primaryContainer = StronkTokens.LimeDim,
    onPrimaryContainer = StronkTokens.Lime,
    inversePrimary = StronkTokens.LimeDeep,

    // secondary = limonka przygaszona: to, co już się wydarzyło
    secondary = StronkTokens.LimeDeep,
    onSecondary = StronkTokens.LimeInk,
    secondaryContainer = StronkTokens.S2,
    onSecondaryContainer = StronkTokens.Text2,

    // tertiary = neutralny kafelek ikony (mocki: `.ico`, `.thumb`)
    tertiary = StronkTokens.Text3,
    onTertiary = StronkTokens.S0,
    tertiaryContainer = StronkTokens.S2,
    onTertiaryContainer = StronkTokens.Text3,

    background = StronkTokens.S0,
    onBackground = StronkTokens.Text,

    surface = StronkTokens.S0,
    onSurface = StronkTokens.Text,
    surfaceVariant = StronkTokens.S2,
    onSurfaceVariant = StronkTokens.Text2,
    surfaceTint = StronkTokens.Lime,

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
    /** `--text-3`: KAPITALIKI statów, meta, ikony w kafelkach, podpisy. */
    val textDim: Color,
    /** `--lime`: akcja / teraz / dziś / PR. Maks ~10% powierzchni ekranu. */
    val lime: Color,
    /** `--lime-deep`: fakty z przeszłości — zrobione dni, starsze słupki wykresu. */
    val limeDeep: Color,
    /** `--lime-ink`: tekst i ikony NA limonce. */
    val limeInk: Color,
    /** `--lime-dim`: tint 13% — karta rekordu, zaznaczony chip, ghost-accent. */
    val limeDim: Color,
    /** `--lime-line`: obrys 30% w parze z [limeDim]. */
    val limeLine: Color,
    /** `--s1`: powierzchnia karty. */
    val surfaceCard: Color,
    /** `--s2`: element na karcie — kafelek, chip, tor pierścienia. */
    val surfaceTile: Color,
    /** `--s3`: placeholder, tor paska, pusty dzień, aktywny segment tabów. */
    val surfaceMuted: Color,
    /** `--line`: obrys wyraźny. */
    val line: Color,
    /** `--line-soft`: dzielnik wierszy, linia nad dolną nawigacją. */
    val lineSoft: Color,

    // --- aliasy zgodności ze starą paletą (patrz StronkTokens) ---
    /** @deprecated tor paska postępu — dziś `--s3`. */
    val fillDim: Color,
    /** @deprecated ikona w kafelku — dziś `--text-3`. */
    val iconBadgeContent: Color,
    /** @deprecated kafelek ikony — dziś `--s2`. */
    val iconBadgeBackground: Color,
    /** @deprecated kafelek ikony nie ma obrysu — dziś `--s2`. */
    val iconBadgeBorder: Color,
    /** @deprecated „zrobione" — dziś `--lime-deep`. */
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    /** @deprecated ostrzeżenie jest neutralne — dziś `--text-2` + ikona. */
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
)

internal val StronkColorsDark = StronkColors(
    textDim = StronkTokens.Text3,
    lime = StronkTokens.Lime,
    limeDeep = StronkTokens.LimeDeep,
    limeInk = StronkTokens.LimeInk,
    limeDim = StronkTokens.LimeDim,
    limeLine = StronkTokens.LimeLine,
    surfaceCard = StronkTokens.S1,
    surfaceTile = StronkTokens.S2,
    surfaceMuted = StronkTokens.S3,
    line = StronkTokens.Line,
    lineSoft = StronkTokens.LineSoft,
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
