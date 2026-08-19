package com.stronk.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTextStyles
import com.stronk.ui.theme.StronkTheme

/**
 * Stat w gęstszym kontekście — cieńszy wariant [StronkStatBlock] bez kafelka
 * (mocki „Limonka" nie mają tła pod statem: liczba stoi wprost na karcie).
 *
 * Nowy kod pisz na [StronkStatBlock] — ta funkcja istnieje dla ekranów sprzed
 * rundy „Limonka" i dla statów, które muszą być wyśrodkowane.
 *
 * @param label kapitalik podany normalnie (komponent robi wersaliki)
 * @param value sama liczba, np. "42,5" albo "3/3" — nigdy z jednostką w środku
 * @param unit jednostka jako mały sufiks, np. "kg"
 */
@Composable
fun StronkStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    valueStyle: TextStyle = MaterialTheme.typography.displaySmall,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    align: Alignment.Horizontal = Alignment.Start,
) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = align) {
        Text(
            text = label.uppercase(),
            style = StronkTextStyles.cap,
            color = StronkTheme.colors.textDim,
            textAlign = if (align == Alignment.CenterHorizontally) TextAlign.Center else TextAlign.Start,
            maxLines = 1,
        )
        Row(
            modifier = Modifier.padding(top = 6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(text = value, style = valueStyle, color = valueColor, maxLines = 1)
            if (unit != null) {
                Text(
                    text = unit,
                    style = StronkTextStyles.unitBig,
                    color = StronkTheme.colors.textDim,
                    modifier = Modifier.padding(start = 5.dp, bottom = 2.dp),
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * Wiersz stat-bloków wyrównanych do dołu (mocki: `.stats`). Dzieci dostają
 * `Modifier.weight(1f)` same, a między nie wstawiasz [StronkStatDivider].
 *
 * Wiersz ma wysokość `IntrinsicSize.Min`, żeby kreska dzieląca mogła się
 * rozciągnąć na pełną wysokość statów.
 */
@Composable
fun StronkStatRow(
    modifier: Modifier = Modifier,
    verticalAlignment: Alignment.Vertical = Alignment.Bottom,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = verticalAlignment,
        content = content,
    )
}

/**
 * Hero-liczba bez etykiety (mocki: `.rest-time`) — countdown przerwy w środku
 * pierścienia albo pojedyncza wielka wartość. Dokładnie raz na ekran.
 *
 * Gdy liczba ma nazwę („CIĘŻAR", „POWTÓRZENIA") — użyj [StronkStatBlock]
 * z `size = StronkStatSize.HERO`, nie tej funkcji.
 *
 * @param value liczba jako tekst, np. "1:12" albo "42,5"
 * @param unit jednostka obok liczby, np. "kg"
 * @param caption jedna wygaszona linijka pod liczbą, np. "z 1:15"
 */
@Composable
fun StronkHeroNumber(
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    caption: String? = null,
    valueStyle: TextStyle = StronkTextStyles.hero,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = value, style = valueStyle, color = valueColor, maxLines = 1)
            if (unit != null) {
                Text(
                    text = unit,
                    style = StronkTextStyles.unitHero,
                    color = StronkTheme.colors.textDim,
                    modifier = Modifier.padding(start = 5.dp, bottom = 4.dp),
                    maxLines = 1,
                )
            }
        }
        if (caption != null) {
            Text(
                text = caption,
                style = StronkTextStyles.meta,
                color = StronkTheme.colors.textDim,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = StronkSpacing.xs),
            )
        }
    }
}
