package com.stronk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stronk.ui.theme.StronkTextStyles
import com.stronk.ui.theme.StronkTheme

/**
 * Rozmiar stat-bloku — 1:1 z mockami.
 *
 * @property valueStyle styl liczby (`--fs-hero` 62 / `--fs-big` 40 / `--fs-title` 27)
 * @property unitStyle styl sufiksu jednostki obok liczby
 * @property capGap odstęp kapitalik → liczba
 * @property bottomInset dosunięcie w dół, żeby w [StronkStatRow] mniejsza liczba
 *           siedziała optycznie na tej samej linii co HERO (mock: `padding-bottom:9px`)
 */
enum class StronkStatSize(
    internal val valueStyle: TextStyle,
    internal val unitStyle: TextStyle,
    internal val capGap: Dp,
    internal val bottomInset: Dp,
) {
    /** Dominanta ekranu — liczba ciężaru w treningu. Dokładnie jedna na ekran. */
    HERO(StronkTextStyles.hero, StronkTextStyles.unitHero, 6.dp, 0.dp),

    /** Druga liczba pary (powtórzenia), liczba na karcie rekordu. */
    BIG(StronkTextStyles.big, StronkTextStyles.unitBig, 6.dp, 9.dp),

    /** Stat w gęstszym kontekście — wiersz karty, podsumowanie sekcji. */
    TITLE(StronkTextStyles.title, StronkTextStyles.unitBig, 5.dp, 4.dp),
}

/**
 * **Podstawowy klocek całej apki.** Kapitalik (`--fs-cap` 11 / tracking .14em,
 * `--text-3`) nad liczbą (`--text`), jednostka jako mały sufiks obok liczby.
 *
 * Zasada Karola (twarda): wartość z jednostką to OSOBNY byt. Nigdy nie sklejaj
 * frazy „32,5 kg × 12 powt." — zawsze dwa stat-bloki obok siebie, rozdzielone
 * [StronkStatDivider]:
 *
 * ```
 * StronkStatRow {
 *     StronkStatBlock("Ciężar", "32,5", unit = "kg", size = StronkStatSize.HERO, modifier = Modifier.weight(1f))
 *     StronkStatDivider()
 *     StronkStatBlock("Powtórzenia", "12", modifier = Modifier.weight(1f))
 * }
 * ```
 *
 * @param label kapitalik podany normalnie — wersaliki robi komponent
 * @param value sama liczba, np. "32,5"; bez jednostki i bez „×"
 * @param unit sufiks jednostki, np. "kg"; null = liczba bez jednostki
 * @param size [StronkStatSize.HERO] dla dominanty, [StronkStatSize.BIG] dla reszty
 * @param valueColor domyślnie `--text`; `StronkTheme.colors.lime` na liczbie rekordu
 * @param stretch siatka mocka (`.stats`): blok wypełnia wysokość wiersza, więc
 *        kapitaliki sąsiednich statów stoją w JEDNYM rzędzie, a liczby o różnej
 *        wielkości siadają na wspólnej linii u dołu. Wymaga [StronkStatRow].
 */
@Composable
fun StronkStatBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    size: StronkStatSize = StronkStatSize.BIG,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    labelColor: Color = StronkTheme.colors.textDim,
    stretch: Boolean = false,
    trailing: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .padding(bottom = size.bottomInset)
            .then(if (stretch) Modifier.fillMaxHeight() else Modifier),
        verticalArrangement = if (stretch) Arrangement.SpaceBetween else Arrangement.Top,
    ) {
        Text(
            text = label.uppercase(),
            style = StronkTextStyles.cap,
            color = labelColor,
            maxLines = 1,
        )
        Row(
            modifier = Modifier.padding(top = size.capGap),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(text = value, style = size.valueStyle, color = valueColor, maxLines = 1)
            if (unit != null) {
                Text(
                    text = unit,
                    style = size.unitStyle,
                    color = StronkTheme.colors.textDim,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 5.dp, bottom = 2.dp),
                )
            }
            trailing?.invoke()
        }
    }
}

/**
 * Pionowa kreska 1 px między stat-blokami (mocki: `.stat-div`) — margines 20 dp
 * po każdej stronie, wysokość rozciągana na cały wiersz.
 *
 * Działa tylko wewnątrz [StronkStatRow] (albo innego `Row` z
 * `Modifier.height(IntrinsicSize.Min)`).
 */
@Composable
fun StronkStatDivider(
    modifier: Modifier = Modifier,
    horizontalMargin: Dp = 20.dp,
) {
    Box(
        modifier = modifier
            .padding(horizontal = horizontalMargin)
            .width(1.dp)
            .fillMaxHeight()
            .background(StronkTheme.colors.line),
    )
}
