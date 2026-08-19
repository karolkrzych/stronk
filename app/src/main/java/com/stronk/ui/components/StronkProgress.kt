package com.stronk.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import com.stronk.ui.theme.StronkSizes
import com.stronk.ui.theme.StronkTheme

/**
 * Segmentowy pasek postępu — ile kroków za nami, gdzie jesteśmy. Czytelny jednym
 * rzutem oka, bez procentów i tekstu. Tor `--s3`, zrobione `--lime-deep`,
 * bieżący `--lime`.
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
    val done = StronkTheme.colors.limeDeep
    val current = StronkTheme.colors.lime
    val rest = StronkTheme.colors.surfaceMuted
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
 * Kropki serii (mocki: `.dots`) — 9 dp, tor `--s3`, bieżąca limonkowa z miękką
 * poświatą `--lime-dim` (mock: `box-shadow: 0 0 0 5px`). Zrobione dostają
 * `--lime-deep`: przeszłość jest przygaszona, „teraz" świeci.
 *
 * Stan treningu ma być widoczny bez czytania — kropki zamiast „2 z 3 wykonane".
 */
@Composable
fun StronkSeriesDots(
    total: Int,
    currentIndex: Int,
    modifier: Modifier = Modifier,
) {
    if (total <= 0) return
    val done = StronkTheme.colors.limeDeep
    val current = StronkTheme.colors.lime
    val glow = StronkTheme.colors.limeDim
    val rest = StronkTheme.colors.surfaceMuted
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { index ->
            val isCurrent = index == currentIndex
            val color = when {
                index < currentIndex -> done
                isCurrent -> current
                else -> rest
            }
            Box(
                Modifier
                    .size(StronkSizes.seriesDot)
                    .then(
                        if (isCurrent) {
                            Modifier.drawBehind {
                                drawCircle(
                                    color = glow,
                                    radius = size.minDimension / 2f + 5.dp.toPx(),
                                )
                            }
                        } else {
                            Modifier
                        },
                    )
                    .background(color, CircleShape),
            )
        }
    }
}
