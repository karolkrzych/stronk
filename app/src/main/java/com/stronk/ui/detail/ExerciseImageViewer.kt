package com.stronk.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.stronk.data.ExerciseRepository
import com.stronk.ui.components.StronkIcons
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTextStyles
import com.stronk.ui.theme.StronkTheme
import kotlin.math.abs

/** Maksymalne przybliżenie w podglądzie — dalej obrazki z datasetu i tak się rozpadają. */
private const val MAX_ZOOM = 4f

/** Przybliżenie po dwukliku — tyle, żeby zobaczyć szczegół chwytu bez celowania szczypcami. */
private const val DOUBLE_TAP_ZOOM = 2.5f

/** Ile trzeba przeciągnąć w bok (px), żeby przeskoczyć na sąsiedni obrazek przy zoomie 1x. */
private const val SWIPE_THRESHOLD_PX = 160f

/**
 * Pełnoekranowy podgląd obrazków ćwiczenia — na telefonie kafelki 4:3 w opisie są
 * za małe, żeby cokolwiek z nich wyczytać.
 *
 * Zachowanie: pinch-zoom 1x–[MAX_ZOOM] z panningiem przyciętym do krawędzi obrazka,
 * dwuklik jako szybki zoom/reset, przeciągnięcie w bok przy zoomie 1x przełącza
 * start ↔ koniec (kropki na dole pokazują, gdzie jesteśmy), a pojedynczy tap w tło
 * przy zoomie 1x zamyka. Przy powiększeniu tap nic nie robi — inaczej podgląd
 * zamykałby się w trakcie oglądania.
 *
 * @param images ścieżki względne z datasetu (jak w [com.stronk.data.Exercise.images])
 * @param startIndex obrazek, w który użytkownik kliknął
 */
@Composable
fun ExerciseImageViewer(
    images: List<String>,
    startIndex: Int,
    onDismiss: () -> Unit,
) {
    if (images.isEmpty()) return
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
        ),
    ) {
        var index by remember { mutableIntStateOf(startIndex.coerceIn(images.indices)) }
        var scale by remember { mutableFloatStateOf(1f) }
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }
        var swipeAccum by remember { mutableFloatStateOf(0f) }
        var boxSize by remember { mutableStateOf(IntSize.Zero) }

        fun reset() {
            scale = 1f
            offsetX = 0f
            offsetY = 0f
            swipeAccum = 0f
        }

        fun clampOffsets() {
            val maxX = (boxSize.width * (scale - 1f)) / 2f
            val maxY = (boxSize.height * (scale - 1f)) / 2f
            offsetX = offsetX.coerceIn(-maxX, maxX)
            offsetY = offsetY.coerceIn(-maxY, maxY)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim)
                .onSizeChanged { boxSize = it },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(index) {
                        detectTapGestures(
                            onTap = { if (scale <= 1f) onDismiss() },
                            onDoubleTap = {
                                if (scale > 1f) {
                                    reset()
                                } else {
                                    scale = DOUBLE_TAP_ZOOM
                                }
                            },
                        )
                    }
                    .pointerInput(index, images.size) {
                        detectTransformGestures { _, pan, gestureZoom, _ ->
                            val newScale = (scale * gestureZoom).coerceIn(1f, MAX_ZOOM)
                            if (newScale != scale) {
                                scale = newScale
                                if (scale <= 1f) {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                            if (scale > 1f) {
                                swipeAccum = 0f
                                offsetX += pan.x
                                offsetY += pan.y
                                clampOffsets()
                            } else if (images.size > 1) {
                                swipeAccum += pan.x
                                if (abs(swipeAccum) > SWIPE_THRESHOLD_PX) {
                                    val step = if (swipeAccum > 0) -1 else 1
                                    index = (index + step).coerceIn(images.indices)
                                    reset()
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = ExerciseRepository.IMAGES_BASE_URI + images[index],
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY,
                        ),
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(StronkSpacing.xs),
            ) {
                Icon(
                    imageVector = StronkIcons.close,
                    contentDescription = "Zamknij podgląd",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            if (images.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    images.forEachIndexed { dot, _ ->
                        Box(
                            modifier = Modifier
                                .size(if (dot == index) 9.dp else 7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (dot == index) {
                                        StronkTheme.colors.lime
                                    } else {
                                        StronkTheme.colors.surfaceMuted
                                    },
                                ),
                        )
                    }
                }
                Text(
                    text = (if (index == 0) "Start" else "Koniec").uppercase(),
                    style = StronkTextStyles.cap,
                    color = StronkTheme.colors.textDim,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 56.dp),
                )
            }
        }
    }
}
