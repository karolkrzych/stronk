package com.stronk.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stronk.ui.theme.StronkSizes
import com.stronk.ui.theme.StronkTheme

/**
 * Pierścień odliczania przerwy (mocki `pack-trening.html`, ekran 2) — 280 dp
 * średnicy, tor `--s2` o grubości 12 dp, limonkowy pasek pokazujący POZOSTAŁY
 * czas, start od godziny 12, zaokrąglone końce.
 *
 * Środek pierścienia to slot: wstaw tam kapitalik „PRZERWA", countdown
 * (`StronkTextStyles.hero`) i wygaszoną linijkę „z 1:15”. W przerwie NIE MA
 * zaliczania serii (ADR-005) — pod pierścieniem idzie tylko „Pomiń przerwę"
 * (ghost z akcentem) i „+30 s" (zwykły ghost).
 *
 * @param progress ułamek POZOSTAŁEGO czasu (1 = pełny pierścień, 0 = koniec);
 *        wartości spoza 0..1 są przycinane
 * @param content treść w środku pierścienia (wyśrodkowana w obu osiach)
 */
@Composable
fun StronkRingTimer(
    progress: Float,
    modifier: Modifier = Modifier,
    diameter: Dp = StronkSizes.ring,
    strokeWidth: Dp = StronkSizes.ringStroke,
    trackColor: Color = StronkTheme.colors.surfaceTile,
    barColor: Color = StronkTheme.colors.lime,
    content: @Composable BoxScope.() -> Unit,
) {
    val fraction = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier.size(diameter),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke),
            )
            if (fraction > 0f) {
                drawArc(
                    color = barColor,
                    startAngle = -90f,
                    sweepAngle = 360f * fraction,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}

/**
 * Wariant pierścienia dla mniejszych kontekstów (np. podgląd przerwy w karcie).
 * Ta sama mechanika, mniejsze wymiary — nie używaj go jako dominanty ekranu.
 */
@Composable
fun StronkRingTimerSmall(
    progress: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) = StronkRingTimer(
    progress = progress,
    modifier = modifier,
    diameter = 128.dp,
    strokeWidth = 8.dp,
    content = content,
)
