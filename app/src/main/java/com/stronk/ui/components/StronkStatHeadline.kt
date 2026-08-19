package com.stronk.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stronk.ui.theme.StronkRadius
import com.stronk.ui.theme.StronkTextStyles
import com.stronk.ui.theme.StronkTheme

/**
 * Jedna liczba w [StronkStatHeadline] — dane, nie composable, żeby wszystkie
 * trzy miejsca („Rekord", „Ostatni rekord", „Kalibracja") miały gwarantowanie
 * ten sam układ.
 *
 * @param label kapitalik podany normalnie, np. "Ciężar"
 * @param value sama liczba, np. "40"; bez jednostki i bez „×"
 * @param unit sufiks jednostki, np. "kg"; null = liczba bez jednostki
 * @param size wielkość liczby — [StronkStatSize.HERO] tylko dla dominanty ekranu
 * @param accent liczba w limonce; dokładnie JEDNA na sekcję (ta, o którą chodzi)
 */
data class StronkStatItem(
    val label: String,
    val value: String,
    val unit: String? = null,
    val size: StronkStatSize = StronkStatSize.BIG,
    val accent: Boolean = false,
)

/**
 * **GOŁY STAT (wariant A)** — kanoniczna prezentacja rekordu i wyniku
 * kalibracji: BEZ karty, bez tła i bez obrysu. Siła idzie z typografii, limonka
 * zostaje tylko na glifie i na jednej liczbie.
 *
 * Układ (mock `mocks/limonka/record-card-variants.html`, kolumna A):
 * 1. wiersz nagłówka — glif 16 dp w limonce + KAPITALIK w `--text-3`,
 * 2. opcjonalna nazwa (np. ćwiczenia) 21 sp,
 * 3. staty w siatce mocka: kapitaliki w jednym rzędzie, liczby na wspólnej
 *    linii u dołu, między nimi [StronkStatDivider],
 * 4. rząd chipów z faktami pobocznymi (data, szac. 1RM, seria testowa).
 *
 * Fakty poboczne idą CHIPAMI, nigdy linijką tekstu w stylu
 * „16.08 · szac. 1RM 53,3 kg" — taka linijka jest enigmatyczna i rozjeżdża się
 * po ekranie. Jeden fakt = jeden chip; [StronkChip] sam kapitalizuje pierwszą
 * literę.
 *
 * ODRZUCONE (Karol, 2026-08-19): rekord na [StronkAccentCard] — limonkowy tint
 * tła z obrysem („blady zielony, tekst rozjebany, brzydkie").
 *
 * @param label KAPITALIK sekcji podany normalnie, np. "Rekord" (wersaliki robi komponent)
 * @param icon glif `Icons.Rounded.*` 16 dp w limonce — jedyny kolor obok liczby
 * @param stats para statów (rzadziej jedna liczba); kolejność = ważność
 * @param title opcjonalna nazwa nad statami (np. ćwiczenia w Progresie)
 * @param chips fakty poboczne jako pigułki, np. "16.08", "1RM · 53,3 kg"
 * @param onClick całą sekcję da się tapnąć (wejście w szczegóły); null = sam widok
 */
@Composable
fun StronkStatHeadline(
    label: String,
    icon: ImageVector,
    stats: List<StronkStatItem>,
    modifier: Modifier = Modifier,
    title: String? = null,
    chips: List<String> = emptyList(),
    onClick: (() -> Unit)? = null,
) {
    val clickable = if (onClick == null) {
        Modifier
    } else {
        Modifier
            .clip(StronkRadius.tileShape)
            .clickable(onClick = onClick)
    }
    Column(modifier = modifier.then(clickable).fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = StronkTheme.colors.lime,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = label.uppercase(),
                style = StronkTextStyles.cap,
                color = StronkTheme.colors.textDim,
                maxLines = 1,
            )
        }
        if (title != null) {
            Text(
                text = title,
                style = StronkTextStyles.h1Small,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        StronkStatRow(modifier = Modifier.padding(top = 10.dp)) {
            stats.forEachIndexed { index, stat ->
                if (index > 0) StronkStatDivider(horizontalMargin = StatGap)
                StronkStatBlock(
                    label = stat.label,
                    value = stat.value,
                    unit = stat.unit,
                    size = stat.size,
                    valueColor = if (stat.accent) {
                        StronkTheme.colors.lime
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    stretch = true,
                )
            }
        }
        if (chips.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                chips.forEach { StronkChip(label = it) }
            }
        }
    }
}

/** `column-gap:24px` z mocka — odstęp od kreski dzielącej staty. */
private val StatGap = 24.dp
