package com.stronk.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * Font: systemowy (Roboto). Docelowy krój (kandydaci Inter / Figtree) jest TBD
 * po rundzie typografii — do tego czasu NIE bundlujemy niczego w `res/font`.
 */
private val Sans = FontFamily.Default

/** Liczby w tej apce są danymi — zawsze ciasno i bez „skakania” wysokości wiersza. */
private val Tight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

/**
 * Skala typograficzna „Limonka" — 1:1 ze zmiennymi `--fs-*` mocków:
 * hero 62 / big 40 / title 27 / h1 24 / h2 17 / body 15 / meta 13 / cap 11.
 * Kapitaliki (cap) mają tracking `.14em`; grubości to 600 i 700, nic więcej.
 *
 * Bezpośrednio (bez ról M3) sięgaj po [StronkTextStyles] — role poniżej istnieją
 * po to, żeby komponenty Material 3 domyślnie wyglądały jak mock.
 *
 * Mapowanie ról (obowiązuje wszystkie ekrany):
 * - `displayLarge`  — HERO 62: liczba ciężaru, countdown w pierścieniu
 * - `displayMedium` — BIG 40: druga liczba statu, wordmark
 * - `displaySmall`  — TITLE 27: nazwa ćwiczenia jako dominanta ekranu
 * - `headlineLarge` — TITLE 27: tytuł kroku kreatora
 * - `headlineMedium`— H1 24: nagłówek ekranu
 * - `headlineSmall` — 21: tytuł karty dnia / „następnie"
 * - `titleLarge`    — 19: tekst CTA
 * - `titleMedium`   — H2 17: nazwa pozycji na liście
 * - `titleSmall`    — BODY 15: mocniejszy wiersz treści, segment tabów
 * - `bodyLarge/Medium/Small` — 15 / 13 / 12,5: tekst opisowy
 * - `labelLarge`    — 19: etykieta przycisku
 * - `labelMedium`   — META 13: chip, meta, licznik
 * - `labelSmall`    — CAP 11: KAPITALIK sekcji/statu (tracking .14em, `colors.textDim`)
 */
internal val StronkTypography = Typography(
    displayLarge = StronkTextStyles.hero,
    displayMedium = StronkTextStyles.big,
    displaySmall = StronkTextStyles.title,
    headlineLarge = StronkTextStyles.title,
    headlineMedium = StronkTextStyles.h1,
    headlineSmall = StronkTextStyles.h1Small,
    titleLarge = StronkTextStyles.cta,
    titleMedium = StronkTextStyles.h2,
    titleSmall = StronkTextStyles.bodyStrong,
    bodyLarge = StronkTextStyles.body,
    bodyMedium = StronkTextStyles.meta,
    bodySmall = StronkTextStyles.hint,
    labelLarge = StronkTextStyles.cta,
    labelMedium = StronkTextStyles.meta.copy(fontWeight = FontWeight.SemiBold),
    labelSmall = StronkTextStyles.cap,
)

/**
 * Style nazwane jak w mockach — używaj ich wprost, gdy rola M3 myli
 * (np. `StronkTextStyles.hero` dla wielkiej liczby ciężaru).
 */
object StronkTextStyles {

    /** `--fs-hero` 62 — wielka liczba (ciężar, countdown). Dokładnie jedna na ekran. */
    val hero = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Bold,
        fontSize = 62.sp,
        lineHeight = 57.sp,
        letterSpacing = (-1.24).sp,
        lineHeightStyle = Tight,
    )

    /** `--fs-big` 40 — duża liczba statu, wordmark. */
    val big = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.8).sp,
        lineHeightStyle = Tight,
    )

    /** `--fs-title` 27 — nazwa ćwiczenia (dominanta karty/ekranu). */
    val title = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Bold,
        fontSize = 27.sp,
        lineHeight = 31.sp,
        letterSpacing = (-0.27).sp,
    )

    /** `--fs-h1` 24 — nagłówek ekranu. */
    val h1 = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.24).sp,
    )

    /** 21 — tytuł karty dnia, nazwa w sekcji „następnie". */
    val h1Small = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.21).sp,
    )

    /** 19 — tekst CTA. */
    val cta = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 23.sp,
        letterSpacing = (-0.19).sp,
    )

    /** `--fs-h2` 17 — tytuł wiersza listy, nazwa ćwiczenia w kolejce. */
    val h2 = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 21.sp,
        letterSpacing = (-0.17).sp,
    )

    /** `--fs-body` 15 mocniejszy — segment tabów, treść z naciskiem. */
    val bodyStrong = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.08).sp,
    )

    /** `--fs-body` 15 — zwykły tekst opisowy. */
    val body = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    )

    /** `--fs-meta` 13 — meta, chip, podpis pod liczbą. */
    val meta = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    )

    /** 12,5 — jedna linijka podpowiedzi (`.hint` w mockach). */
    val hint = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5f.sp,
        lineHeight = 18.sp,
    )

    /**
     * `--fs-cap` 11 z trackingiem `.14em` — KAPITALIK nad liczbą i nad sekcją.
     * Tekst podawaj normalnie; wersaliki robi komponent (`label.uppercase()`).
     */
    val cap = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.54.sp,
    )

    /** Jednostka przy liczbie HERO (`.stat .u` = 19 px). */
    val unitHero = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 22.sp,
    )

    /** Jednostka przy liczbie BIG (`.record .stat .u` = 15 px). */
    val unitBig = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 18.sp,
    )
}
