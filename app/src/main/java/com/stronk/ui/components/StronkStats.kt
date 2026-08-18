package com.stronk.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTheme

/**
 * Wyeksponowana statystyka (mocki: `.nb-stat`) — mała, wygaszona etykieta u góry,
 * pod nią DUŻA liczba. Zasada: liczba jest bohaterem, etykieta tylko ją nazywa.
 *
 * Dwie lub trzy takie obok siebie robią czytelny wiersz danych bez ściany tekstu:
 * `StronkStatRow { StronkStat(...); StronkStat(...) }`.
 *
 * @param label krótka etykieta, np. "seria", "objętość" (komponent robi wersaliki)
 * @param value sama wartość, np. "42,5" albo "3/3"
 * @param unit jednostka pisana mniejszą czcionką obok wartości, np. "kg"
 * @param valueColor kolor liczby — domyślnie `onSurface`; dla wyróżnienia użyj
 *        `MaterialTheme.colorScheme.primary` albo `StronkTheme.colors.success`
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
    StronkInsetCard(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = StronkSpacing.sm),
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = align) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 1.0.sp),
                color = StronkTheme.colors.textDim,
                textAlign = if (align == Alignment.CenterHorizontally) TextAlign.Center else TextAlign.Start,
            )
            Row(
                modifier = Modifier.padding(top = 6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(text = value, style = valueStyle, color = valueColor, maxLines = 1)
                if (unit != null) {
                    Text(
                        text = unit,
                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 3.dp, bottom = 2.dp),
                    )
                }
            }
        }
    }
}

/** Wiersz statystyk z równym odstępem — dzieci dostają `Modifier.weight(1f)` same. */
@Composable
fun StronkStatRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(StronkSpacing.xs),
        verticalAlignment = Alignment.Top,
        content = content,
    )
}

/**
 * Hero-liczba bez kafelka (mocki: `.value`, `.rest-time`) — focal point ekranu.
 * Używaj DOKŁADNIE raz na ekran; dwie hero-liczby to brak hierarchii.
 *
 * @param value liczba jako tekst, np. "42,5" albo "01:24"
 * @param unit jednostka obok liczby, np. "kg"
 * @param caption jeden mały, wygaszony wiersz pod liczbą, np. "ostatnio: 40 kg × 8"
 */
@Composable
fun StronkHeroNumber(
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    caption: String? = null,
    valueStyle: TextStyle = MaterialTheme.typography.displayMedium,
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
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = StronkSpacing.xxs, bottom = 4.dp),
                )
            }
        }
        if (caption != null) {
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = StronkTheme.colors.textDim,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = StronkSpacing.sm),
            )
        }
    }
}
