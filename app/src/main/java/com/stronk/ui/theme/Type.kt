package com.stronk.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.stronk.R

/**
 * HYBRYDA H2 (wybór Karola, runda 5) — DWIE rodziny, zero Intera:
 * - TYTUŁY (`title`, `h1`, `h1Small` — nazwa ćwiczenia, nagłówek ekranu, nazwa dnia)
 *   → **Barlow Semi Condensed SemiBold 600**. Wąski, „sportowy", oszczędza szerokość
 *   przy długich polskich nazwach ćwiczeń.
 * - LICZBY hero/big (`hero`, `big` — ciężar, countdown, staty rekordu)
 *   → **Figtree 800 z `tnum`** (cyfry tabelaryczne: liczba nie „skacze" przy zmianie).
 * - BODY / META / KAPITALIKI i cała reszta → **Figtree** 400/500/600/700.
 *
 * Wzorzec: mock rundy 4 `typografia-hybryda.html`, wariant H2 (`.hyb-h2`).
 * Oba kroje na licencji OFL, bundlowane w `res/font` (patrz `docs/design-system.md`).
 */
private val Barlow = FontFamily(
    Font(R.font.barlow_semi_condensed_semibold, FontWeight.SemiBold),
)

/**
 * Figtree to font zmienny (oś `wght` 300–900) — jeden plik TTF obsługuje wszystkie
 * grubości przez [FontVariation]. Ustawienia osi działają od API 26, `minSdk` = 29.
 *
 * `variationSettings` jest jeszcze `@ExperimentalTextApi` (Compose BOM 2025.06) —
 * API jest stabilne w praktyce, a alternatywa to bundlowanie 5 statycznych TTF-ów.
 */
@OptIn(ExperimentalTextApi::class)
private fun figtree(weight: FontWeight) = Font(
    resId = R.font.figtree_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

private val Figtree = FontFamily(
    figtree(FontWeight.Normal),     // 400 — body, hint
    figtree(FontWeight.Medium),     // 500 — meta
    figtree(FontWeight.SemiBold),   // 600 — h2, bodyStrong, jednostki, chipy
    figtree(FontWeight.Bold),       // 700 — CTA, kapitaliki
    figtree(FontWeight.ExtraBold),  // 800 — liczby hero/big
)

/** Liczby w tej apce są danymi — zawsze ciasno i bez „skakania” wysokości wiersza. */
private val Tight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

/** Cyfry tabelaryczne — stała szerokość glifu, kolumny liczb się nie rozjeżdżają. */
private const val TABULAR = "tnum"

/**
 * Ten sam styl, ale z cyframi tabelarycznymi — odpowiednik klasy `.num` z mocków.
 * [StronkTextStyles.hero] i `.big` mają `tnum` na stałe (to zawsze liczby); mniejsze
 * style są mieszane, więc liczbę oznaczamy wprost: `StronkTextStyles.cap.tabularNums()`.
 */
fun TextStyle.tabularNums(): TextStyle = copy(fontFeatureSettings = TABULAR)

/**
 * Skala typograficzna „Limonka" — 1:1 ze zmiennymi `--fs-*` mocków:
 * hero 62 / big 40 / title 27 / h1 24 / h2 17 / body 15 / meta 13 / cap 11.
 * Kapitaliki (cap) mają tracking `.14em`.
 *
 * **Kompensacja optyczna** (z wariantu H2, tokeny `--sc-head` / `--sc-num`): Barlow
 * Semi Condensed jest wąski, więc tytuł 27 rysujemy jako 27 × 1,04 ≈ 28 sp; Figtree
 * 800 jest szerokie i ciężkie, więc hero 62 × 0,97 ≈ 60 sp i big 40 × 0,97 ≈ 39 sp.
 * Tokeny w design systemie zostają 62/40/27 — mnożnik to poprawka pod krój.
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

    /**
     * `--fs-hero` 62 — wielka liczba (ciężar, countdown). Dokładnie jedna na ekran.
     * Figtree 800 + `tnum`, 62 × 0,97 ≈ 60 sp.
     */
    val hero = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 60.sp,
        lineHeight = 55.sp,
        letterSpacing = (-1.8).sp,
        fontFeatureSettings = TABULAR,
        lineHeightStyle = Tight,
    )

    /** `--fs-big` 40 — duża liczba statu, wordmark. Figtree 800 + `tnum`, 40 × 0,97 ≈ 39 sp. */
    val big = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 39.sp,
        lineHeight = 41.sp,
        letterSpacing = (-0.59).sp,
        fontFeatureSettings = TABULAR,
        lineHeightStyle = Tight,
    )

    /** `--fs-title` 27 — nazwa ćwiczenia (dominanta karty/ekranu). Barlow 600, 27 × 1,04 ≈ 28 sp. */
    val title = TextStyle(
        fontFamily = Barlow,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.06.sp,
    )

    /** `--fs-h1` 24 — nagłówek ekranu. Barlow 600. */
    val h1 = TextStyle(
        fontFamily = Barlow,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.24).sp,
    )

    /** 21 — tytuł karty dnia, nazwa w sekcji „następnie". Barlow 600. */
    val h1Small = TextStyle(
        fontFamily = Barlow,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.21).sp,
    )

    /** 18 — nazwa ćwiczenia w wierszu-karcie edytora planu (mock W1: 17px). Barlow 600, 17 × 1,04 ≈ 18 sp. */
    val h1Tiny = TextStyle(
        fontFamily = Barlow,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.18).sp,
    )

    /** 19 — tekst CTA. */
    val cta = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 23.sp,
        letterSpacing = (-0.19).sp,
    )

    /** `--fs-h2` 17 — tytuł wiersza listy, nazwa ćwiczenia w kolejce. */
    val h2 = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 21.sp,
        letterSpacing = (-0.17).sp,
    )

    /** `--fs-body` 15 mocniejszy — segment tabów, treść z naciskiem. */
    val bodyStrong = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.08).sp,
    )

    /** `--fs-body` 15 — zwykły tekst opisowy. */
    val body = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    )

    /** `--fs-meta` 13 — meta, chip, podpis pod liczbą. */
    val meta = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    )

    /** 12,5 — jedna linijka podpowiedzi (`.hint` w mockach). */
    val hint = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5f.sp,
        lineHeight = 18.sp,
    )

    /**
     * `--fs-cap` 11 z trackingiem `.14em` — KAPITALIK nad liczbą i nad sekcją.
     * Tekst podawaj normalnie; wersaliki robi komponent (`label.uppercase()`).
     */
    val cap = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.54.sp,
    )

    /** Jednostka przy liczbie HERO (`.stat .u` = 19 px). */
    val unitHero = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 22.sp,
    )

    /** Jednostka przy liczbie BIG (`.record .stat .u` = 15 px). */
    val unitBig = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 18.sp,
    )
}
