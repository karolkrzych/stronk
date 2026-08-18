package com.stronk.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/**
 * Wykres liniowy progresu rysowany ręcznie na Canvas (zero bibliotek —
 * konwencja projektu). Kolory z MaterialTheme, więc działa w dark i light.
 * Jeden punkt = jeden trening; oś X to czas, etykiety dat pod osią.
 * Pusty stan obsługuje wołający — tu zakładamy co najmniej jeden punkt.
 */
@Composable
internal fun ProgressLineChart(
    points: List<ChartPoint>,
    valueFormatter: (Double) -> String,
    modifier: Modifier = Modifier,
) {
    if (points.isEmpty()) return
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = MaterialTheme.typography.labelSmall
        .copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val lastValueStyle = MaterialTheme.typography.labelMedium
        .copy(color = lineColor, fontWeight = FontWeight.SemiBold)
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        // --- Zakres wartości z marginesem, bez schodzenia poniżej zera ---
        val minValue = points.minOf { it.value }
        val maxValue = points.maxOf { it.value }
        val span = (maxValue - minValue).takeIf { it > 1e-9 }
            ?: maxOf(abs(maxValue) * FLAT_SPAN_FRACTION, 1.0)
        var yMin = minValue - span * RANGE_PAD_FRACTION
        if (minValue >= 0 && yMin < 0) yMin = 0.0
        val yMax = maxValue + span * RANGE_PAD_FRACTION

        // --- Marginesy pod etykiety osi ---
        val ticks = niceTicks(yMin, yMax)
        val tickLayouts = ticks.map {
            textMeasurer.measure(AnnotatedString(valueFormatter(it)), labelStyle)
        }
        val axisGap = 6.dp.toPx()
        val plotLeft = (tickLayouts.maxOfOrNull { it.size.width } ?: 0) + axisGap
        val firstDateLayout =
            textMeasurer.measure(AnnotatedString(ProgressFormat.axisDate(points.first().startedAt)), labelStyle)
        val plotBottom = size.height - firstDateLayout.size.height - axisGap
        val plotTop = 10.dp.toPx()
        val plotRight = size.width - 6.dp.toPx()
        val plotWidth = plotRight - plotLeft
        val plotHeight = plotBottom - plotTop
        if (plotWidth <= 0 || plotHeight <= 0) return@Canvas

        fun yPx(value: Double): Float =
            (plotBottom - (value - yMin) / (yMax - yMin) * plotHeight).toFloat()

        val t0 = points.first().startedAt
        val t1 = points.last().startedAt
        fun xPx(timestamp: Long): Float =
            if (t1 == t0) (plotLeft + plotRight) / 2f
            else (plotLeft + (timestamp - t0).toDouble() / (t1 - t0).toDouble() * plotWidth).toFloat()

        // --- Siatka pozioma + etykiety wartości ---
        ticks.forEachIndexed { index, tick ->
            val y = yPx(tick)
            drawLine(
                color = gridColor,
                start = Offset(plotLeft, y),
                end = Offset(plotRight, y),
                strokeWidth = 1.dp.toPx(),
            )
            val layout = tickLayouts[index]
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    plotLeft - axisGap - layout.size.width,
                    (y - layout.size.height / 2f)
                        .coerceAtMost(size.height - layout.size.height)
                        .coerceAtLeast(0f),
                ),
            )
        }

        // --- Etykiety dat na osi X (pierwsza i ostatnia; jedna gdy jeden trening) ---
        val dateY = plotBottom + axisGap / 2f
        if (t1 == t0) {
            drawText(
                textLayoutResult = firstDateLayout,
                topLeft = Offset(
                    ((plotLeft + plotRight) / 2f - firstDateLayout.size.width / 2f)
                        .coerceAtLeast(plotLeft),
                    dateY,
                ),
            )
        } else {
            drawText(textLayoutResult = firstDateLayout, topLeft = Offset(plotLeft, dateY))
            val lastDateLayout =
                textMeasurer.measure(AnnotatedString(ProgressFormat.axisDate(t1)), labelStyle)
            drawText(
                textLayoutResult = lastDateLayout,
                topLeft = Offset(plotRight - lastDateLayout.size.width, dateY),
            )
        }

        // --- Linia, wypełnienie pod nią i punkty ---
        val offsets = points.map { Offset(xPx(it.startedAt), yPx(it.value)) }
        if (offsets.size > 1) {
            val linePath = Path().apply {
                moveTo(offsets.first().x, offsets.first().y)
                offsets.drop(1).forEach { lineTo(it.x, it.y) }
            }
            val fillPath = Path().apply {
                addPath(linePath)
                lineTo(offsets.last().x, plotBottom)
                lineTo(offsets.first().x, plotBottom)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = FILL_ALPHA), lineColor.copy(alpha = 0f)),
                    startY = plotTop,
                    endY = plotBottom,
                ),
            )
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }
        offsets.forEach { drawCircle(color = lineColor, radius = 3.dp.toPx(), center = it) }

        // --- Wyróżnienie ostatniego treningu: większy punkt + wartość ---
        val last = offsets.last()
        drawCircle(color = lineColor, radius = 5.dp.toPx(), center = last)
        val lastValueLayout =
            textMeasurer.measure(AnnotatedString(valueFormatter(points.last().value)), lastValueStyle)
        val labelX = (last.x - lastValueLayout.size.width / 2f)
            .coerceAtMost(plotRight - lastValueLayout.size.width)
            .coerceAtLeast(plotLeft)
        val labelAbove = last.y - lastValueLayout.size.height - 6.dp.toPx()
        drawText(
            textLayoutResult = lastValueLayout,
            topLeft = Offset(labelX, if (labelAbove >= 0f) labelAbove else last.y + 8.dp.toPx()),
        )
    }
}

/** Stałe layoutu wykresu — jedno miejsce zmian. */
private const val RANGE_PAD_FRACTION = 0.15
private const val FLAT_SPAN_FRACTION = 0.2
private const val FILL_ALPHA = 0.20f
private const val MAX_Y_TICKS = 4

/**
 * "Ładne" podziałki osi Y: krok 1/2/2,5/5 × 10^n dobrany tak,
 * żeby zmieściło się maksymalnie [MAX_Y_TICKS] linii.
 */
private fun niceTicks(min: Double, max: Double): List<Double> {
    val span = max - min
    if (span <= 0) return listOf(min)
    val rawStep = span / MAX_Y_TICKS
    val magnitude = 10.0.pow(floor(log10(rawStep)))
    val residual = rawStep / magnitude
    val step = magnitude * when {
        residual > 5 -> 10.0
        residual > 2.5 -> 5.0
        residual > 2 -> 2.5
        residual > 1 -> 2.0
        else -> 1.0
    }
    val ticks = mutableListOf<Double>()
    var value = ceil(min / step) * step
    while (value <= max + step * 1e-3) {
        ticks.add(value)
        value += step
    }
    return ticks
}
