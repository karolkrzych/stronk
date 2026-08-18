package com.stronk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.stronk.ui.theme.StronkSizes
import com.stronk.ui.theme.StronkTheme

/**
 * Segmentowy pasek postępu (mocki: `.seg-track`, `.wiz-track`) — ile kroków za
 * nami, gdzie jesteśmy. Czytelny jednym rzutem oka, bez procentów i tekstu.
 *
 * @param total liczba segmentów (ćwiczeń w treningu, kroków kreatora)
 * @param currentIndex indeks bieżącego segmentu liczony od 0; wcześniejsze = zrobione
 */
@Composable
fun StronkSegmentedProgress(
    total: Int,
    currentIndex: Int,
    modifier: Modifier = Modifier,
) {
    if (total <= 0) return
    val done = StronkTheme.colors.fillDim
    val current = MaterialTheme.colorScheme.onSurfaceVariant
    val rest = MaterialTheme.colorScheme.surfaceVariant
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        repeat(total) { index ->
            val color = when {
                index < currentIndex -> done
                index == currentIndex -> current
                else -> rest
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(StronkSizes.progressBar)
                    .background(color, RoundedCornerShape(2.dp)),
            )
        }
    }
}

/**
 * Kropki serii (mocki: `.dots`) — zaliczona (zieleń), bieżąca (obrys), przyszła.
 * Stan treningu ma być widoczny bez czytania: kropki zamiast "2 z 3 wykonane".
 */
@Composable
fun StronkSeriesDots(
    total: Int,
    currentIndex: Int,
    modifier: Modifier = Modifier,
) {
    val success = StronkTheme.colors.success
    val outline = MaterialTheme.colorScheme.outline
    val onSurface = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { index ->
            when {
                index < currentIndex -> Box(
                    Modifier
                        .size(8.dp)
                        .background(success, CircleShape),
                )

                index == currentIndex -> Box(
                    Modifier
                        .size(10.dp)
                        .border(2.5.dp, onSurface, CircleShape),
                )

                else -> Box(
                    Modifier
                        .size(8.dp)
                        .border(1.5.dp, outline, CircleShape),
                )
            }
        }
    }
}
