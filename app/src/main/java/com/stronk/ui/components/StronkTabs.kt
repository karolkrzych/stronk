package com.stronk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stronk.ui.theme.StronkRadius
import com.stronk.ui.theme.StronkSizes
import com.stronk.ui.theme.StronkTextStyles
import com.stronk.ui.theme.StronkTheme

/**
 * Taby segmentowe (mocki: `.seg`, np. „Opis | Historia") — kontener `--s1`
 * w pigułce z paddingiem 4 dp, aktywny segment `--s3` z tekstem `--text`,
 * nieaktywne `--text-3`. Bez limonki: to nawigacja, nie akcja.
 *
 * Maks 4 segmenty; przy większej liczbie tekst przestaje się mieścić — wtedy
 * to już jest lista, nie przełącznik.
 *
 * @param labels etykiety segmentów w kolejności
 * @param selectedIndex indeks aktywnego segmentu
 */
@Composable
fun StronkSegmentedTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (labels.isEmpty()) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(StronkTheme.colors.surfaceCard, StronkRadius.pill)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(StronkSizes.segment)
                    .background(
                        if (selected) StronkTheme.colors.surfaceMuted else Color.Transparent,
                        StronkRadius.pill,
                    )
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = if (labels.size > 2) StronkTextStyles.meta else StronkTextStyles.bodyStrong,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        StronkTheme.colors.textDim
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
