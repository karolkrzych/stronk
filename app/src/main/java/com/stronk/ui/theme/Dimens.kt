package com.stronk.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Siatka odstępów z mocków: 4 / 8 / 12 / 16 / 20 / 24 / 32,
 * `--pad-screen: 22` i `--pad-card: 20`.
 * Boczny padding każdego ekranu = [screen] (22 dp) — bez wyjątków.
 */
object StronkSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp

    /** `--pad-screen` — boczny padding ekranu. */
    val screen = 22.dp

    /** `--pad-card` — wewnętrzny padding karty. */
    val card = 20.dp

    /** Odstęp między kartami/sekcjami w pionie. */
    val section = 22.dp

    /** Odstęp między wierszami listy (wiersze mają własny padding + dzielnik). */
    val row = 8.dp
}

/**
 * Promienie z mocków — nazwane tak jak zmienne CSS, żeby dało się je porównać:
 * `--r-card: 24`, `--r-inner: 18`, `--r-tile: 14`, `--r-day: 7`, `--r-pill: 999`.
 */
object StronkRadius {
    /** `--r-card` — karta sekcji, karta rekordu, arkusz. */
    val card = 24.dp

    /** `--r-inner` — CTA, ghost, element wewnątrz karty, obrazek. */
    val inner = 18.dp

    /** `--r-tile` — kafelek ikony, miniatura, pole wyszukiwania. */
    val tile = 14.dp

    /** Mniejszy kafelek ikony w wierszu listy (`.row .ico`, `.exrow .ico`). */
    val tileSmall = 12.dp

    /** `--r-day` — kwadrat dnia w kalendarzu. */
    val day = 7.dp

    /** Znacznik legendy kalendarza — mały kwadracik 12 dp (mocki: `.legend i`). */
    val swatch = 4.dp

    /** `--r-pill` — chip, badge, segment tabów. */
    val pill = RoundedCornerShape(percent = 50)

    /** Zaokrąglenie słupka wykresu (`rx=3`). */
    val bar = 3.dp

    val cardShape: Shape = RoundedCornerShape(card)
    val innerShape: Shape = RoundedCornerShape(inner)
    val tileShape: Shape = RoundedCornerShape(tile)
    val tileSmallShape: Shape = RoundedCornerShape(tileSmall)
    val dayShape: Shape = RoundedCornerShape(day)
    val swatchShape: Shape = RoundedCornerShape(swatch)
}

/**
 * Role kształtów Material 3 wpięte w promienie mocków:
 * extraSmall = dzień 7, small = kafelek 14, medium = inner 18, large = karta 24.
 * `extraLarge` zostaje 24 — w „Limonce" nic nie jest bardziej okrągłe od karty.
 */
internal val StronkShapes = Shapes(
    extraSmall = RoundedCornerShape(StronkRadius.day),
    small = RoundedCornerShape(StronkRadius.tile),
    medium = RoundedCornerShape(StronkRadius.inner),
    large = RoundedCornerShape(StronkRadius.card),
    extraLarge = RoundedCornerShape(StronkRadius.card),
)

/** Wysokości i rozmiary elementów — spójne w całej apce, wprost z mocków. */
object StronkSizes {
    /** `.cta` — główny przycisk ekranu: 66 dp. */
    val cta = 66.dp

    /** `.cta.sm` — CTA wewnątrz karty: 54 dp. */
    val ctaSmall = 54.dp

    /** `--h-ghost` — przycisk drugorzędny: 56 dp. */
    val ghost = 56.dp

    /** Alias zgodności: „standardowy przycisk" = CTA. */
    val button = cta

    /** Wielki przycisk kciukowy (stan sprzed „Limonki" — CTA ma dziś 66 dp). */
    val bigButton = 108.dp

    /** `--nav-h` — dolna nawigacja. */
    val navBar = 64.dp

    /** Wysokość paska nawigacji ekranu (chevron wstecz + „i"). */
    val topBar = 44.dp

    /** `--ring-size` — pierścień odliczania przerwy. */
    val ring = 280.dp

    /** `--ring-stroke` — grubość toru i paska pierścienia. */
    val ringStroke = 12.dp

    /** `--ico-tile` — kafelek ikony w wierszu listy. */
    val iconTile = 38.dp

    /** Kafelek ikony w sekcji „następne" (`.next .ico`). */
    val iconTileSmall = 34.dp

    /** `--thumb` — miniatura ćwiczenia w bazie. */
    val thumb = 62.dp

    /** `--row-h` — wiersz listy ćwiczeń. */
    val listRow = 62.dp

    /** Wysokość chipa (`.chip`). */
    val chip = 30.dp

    /** `--seg-h` — segment przełącznika Opis|Historia. */
    val segment = 38.dp

    /** `--search-h` — pole wyszukiwania. */
    val search = 46.dp

    /** Kropka serii (`.dots i`). */
    val seriesDot = 9.dp

    /** Grubość paska/dzielnika. */
    val hairline = 1.dp

    /** Grubość segmentu paska postępu. */
    val progressBar = 4.dp

    /** `--chart-h` — wysokość wykresu słupkowego. */
    val chart = 100.dp
}
