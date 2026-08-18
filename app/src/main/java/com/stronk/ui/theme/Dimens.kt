package com.stronk.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Siatka odstępów z mocków: 4 / 8 / 12 / 16 / 20 / 24 / 32.
 * Boczny padding każdego ekranu = [screen] (20 dp) — bez wyjątków.
 */
object StronkSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp

    /** Boczny padding ekranu. */
    val screen = 20.dp

    /** Domyślny wewnętrzny padding karty. */
    val card = 20.dp

    /** Odstęp między kartami/sekcjami w pionie. */
    val section = 20.dp

    /** Odstęp między wierszami listy. */
    val row = 8.dp
}

/**
 * Promienie z mocków: badge 9–16, wiersz 14, karta 20–22, CTA 18–28.
 * `extraLarge` zarezerwowany dla wielkiego CTA i bottom sheetów.
 */
internal val StronkShapes = Shapes(
    extraSmall = RoundedCornerShape(9.dp),
    small = RoundedCornerShape(13.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Promień karty ekranowej (mocki: `.ex-card`/`.next-big`) — domyślny `shape` [StronkCard]. */
val StronkCardShape = RoundedCornerShape(22.dp)

/** Promień głównego CTA (mocki: `.cta`). */
val StronkButtonShape = RoundedCornerShape(18.dp)

/** Wysokości elementów interaktywnych — spójne w całej apce. */
object StronkSizes {
    /** Standardowy przycisk (CTA i ghost). */
    val button = 56.dp

    /** Wielki przycisk kciukowy w treningu. */
    val bigButton = 108.dp

    /** Grubość segmentu paska postępu. */
    val progressBar = 4.dp
}
