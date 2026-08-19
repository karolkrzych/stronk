package com.stronk.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stronk.ui.theme.StronkRadius
import com.stronk.ui.theme.StronkSizes
import com.stronk.ui.theme.StronkTheme

/**
 * Jeden słupek wykresu trendu.
 *
 * @param value wartość liczbowa (np. ciężar) — steruje wysokością słupka
 * @param label tekst rysowany nad słupkiem; podawaj TYLKO przy pierwszym
 *        i ostatnim (tak jest w mocku) — reszta zostaje niema
 * @param highlight rekord (PR) — słupek dostaje jasną limonkę
 */
data class StronkBar(
    val value: Float,
    val label: String? = null,
    val highlight: Boolean = false,
)

/**
 * Wykres trendu jako SCHODKI (mocki `pack-historia-profil.html`, `.chart`) —
 * słupki z zaokrąglonym rogiem, dyskretna linia bazowa, wartości liczbowe nad
 * pierwszym i ostatnim słupkiem. Słupek rekordu jasną limonką, ostatnie sesje
 * `--lime-deep`, starsze `--s3`.
 *
 * Goła linia bez osi została ODRZUCONA przez Karola — nie wracaj do niej.
 *
 * Najniższy słupek ma połowę wysokości najwyższego (jak w mocku), więc wykres
 * czyta się jako przyrost, a nie jako „coś prawie zerowego".
 *
 * @param bars od najstarszej do najnowszej; pusta lista nic nie rysuje
 * @param recentCount ile ostatnich słupków dostaje `--lime-deep` (przeszłość
 *        bliska); wcześniejsze są `--s3`
 */
@Composable
fun StronkBarChart(
    bars: List<StronkBar>,
    modifier: Modifier = Modifier,
    height: Dp = StronkSizes.chart,
    recentCount: Int = 4,
) {
    if (bars.isEmpty()) return
    val measurer = rememberTextMeasurer()
    val colorOld = StronkTheme.colors.surfaceMuted
    val colorRecent = StronkTheme.colors.limeDeep
    val colorTop = StronkTheme.colors.lime
    val colorLine = StronkTheme.colors.line
    val colorLabel = StronkTheme.colors.textDim
    val labelStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(height),
    ) {
        val labelRow = 16.dp.toPx()
        val baselineY = size.height - 1.5f.dp.toPx()
        val barBottom = size.height - 4.dp.toPx()
        val maxBarHeight = (barBottom - labelRow).coerceAtLeast(1f)
        val minBarHeight = maxBarHeight / 2f
        val corner = CornerRadius(StronkRadius.bar.toPx(), StronkRadius.bar.toPx())

        // linia bazowa — ledwie widoczna, ale trzyma słupki na ziemi
        drawLine(
            color = colorLine,
            start = Offset(0f, baselineY),
            end = Offset(size.width, baselineY),
            strokeWidth = 1.dp.toPx(),
        )

        val gap = 10.dp.toPx()
        val pitch = (size.width + gap) / bars.size
        val barWidth = (pitch - gap).coerceAtLeast(2f)

        val min = bars.minOf { it.value }
        val max = bars.maxOf { it.value }
        val span = (max - min).takeIf { it > 0f }

        bars.forEachIndexed { index, bar ->
            val ratio = if (span == null) 1f else (bar.value - min) / span
            val barHeight = minBarHeight + ratio * (maxBarHeight - minBarHeight)
            val left = index * pitch
            val top = barBottom - barHeight
            val color: Color = when {
                bar.highlight -> colorTop
                index >= bars.size - recentCount -> colorRecent
                else -> colorOld
            }
            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = corner,
            )
            val label = bar.label
            if (label != null) {
                val layout = measurer.measure(
                    text = label,
                    style = labelStyle.copy(
                        color = if (bar.highlight) colorTop else colorLabel,
                        fontWeight = if (bar.highlight) FontWeight.Bold else FontWeight.SemiBold,
                    ),
                )
                val maxTextX = (size.width - layout.size.width).coerceAtLeast(0f)
                val textX = (left + barWidth / 2f - layout.size.width / 2f)
                    .coerceIn(0f, maxTextX)
                val textY = (top - layout.size.height - 4.dp.toPx()).coerceAtLeast(0f)
                drawText(layout, topLeft = Offset(textX, textY))
            }
        }
    }
}

/**
 * Mini-trend w wierszu listy (mocki: `.spark`) — kilka wąskich słupków, ostatni
 * limonkowy. Bez liczb i bez osi: to sygnał „rośnie/stoi", nie wykres.
 *
 * @param values od najstarszej do najnowszej, maks ~6 wartości
 */
@Composable
fun StronkSparkline(
    values: List<Float>,
    modifier: Modifier = Modifier,
    height: Dp = 26.dp,
    barWidth: Dp = 4.dp,
    gap: Dp = 3.dp,
) {
    if (values.isEmpty()) return
    val colorRest = StronkTheme.colors.surfaceMuted
    val colorLast = StronkTheme.colors.lime
    Canvas(
        modifier.height(height).width(barWidth * values.size + gap * (values.size - 1)),
    ) {
        val w = barWidth.toPx()
        val g = gap.toPx()
        val min = values.min()
        val max = values.max()
        val span = (max - min).takeIf { it > 0f }
        val corner = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        values.forEachIndexed { index, value ->
            val ratio = if (span == null) 1f else (value - min) / span
            val h = (size.height / 2f) + ratio * (size.height / 2f)
            drawRoundRect(
                color = if (index == values.lastIndex) colorLast else colorRest,
                topLeft = Offset(index * (w + g), size.height - h),
                size = Size(w, h),
                cornerRadius = corner,
            )
        }
    }
}
