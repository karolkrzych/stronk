package com.stronk.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.stronk.ui.theme.StronkSizes
import com.stronk.ui.theme.StronkTheme

/**
 * Pierścień postępu przerwy (mocki: `.ring`) — focal point ekranu odpoczynku.
 * Tor w tle w [MaterialTheme.colorScheme.surfaceVariant], łuk postępu w
 * [MaterialTheme.colorScheme.primary], start na godzinie 12 (-90°), zgodnie
 * z ruchem wskazówek zegara.
 *
 * @param progress 0f (start przerwy) .. 1f (koniec); wartości spoza zakresu są przycinane
 * @param content wnętrze pierścienia (np. duży czas + podpis) — wyśrodkowane w kolumnie
 */
@Composable
fun StronkRingTimer(
    progress: Float,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val clamped = progress.coerceIn(0f, 1f)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = MaterialTheme.colorScheme.primary
    val strokeWidth = 13.dp

    Box(modifier = modifier.size(264.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(264.dp)) {
            val strokePx = strokeWidth.toPx()
            val stroke = Stroke(width = strokePx, cap = StrokeCap.Butt)
            val diameter = size.minDimension - strokePx
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * clamped,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
            content = content,
        )
    }
}

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
