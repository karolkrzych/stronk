package com.stronk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.stronk.StronkApplication
import com.stronk.data.ExerciseRepository
import com.stronk.ui.theme.StronkSizes
import com.stronk.ui.theme.StronkTextStyles
import com.stronk.ui.theme.StronkTheme

/**
 * Miniatura ćwiczenia — kafelek `--s2` z PIERWSZYM obrazkiem ćwiczenia z assets
 * (te same pliki, które szczegół ćwiczenia pokazuje jako „start / koniec”).
 *
 * Zastępuje piktogramy partii wszędzie tam, gdzie wiersz mówi o KONKRETNYM
 * ćwiczeniu: Dziś, Baza, Progres, „następne” w treningu. Ikona partii zostaje
 * tylko jako fallback — dla ćwiczeń bez obrazka w datasecie.
 *
 * Kadr: [ContentScale.Crop], więc kafelek jest zawsze wypełniony niezależnie od
 * proporcji zdjęcia (dataset ma 4:3).
 *
 * @param exerciseId id z datasetu (`exercises.json`) — po nim lecą obrazki i ikona
 * @param size bok kwadratu; z mocków: 38 na listach, 62 w Bazie, 34 w „następne”
 * @param cornerRadius promień kafelka (mocki: 12 na listach, `--r-tile` 14 w Bazie)
 */
@Composable
fun StronkExerciseThumb(
    exerciseId: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    cornerRadius: Dp = 12.dp,
) {
    val application = LocalContext.current.applicationContext as? StronkApplication
    val info by produceState<ExerciseThumbInfo?>(initialValue = null, exerciseId, application) {
        value = application?.let { ExerciseThumbs.of(it.exerciseRepository, exerciseId) }
    }
    // Dataset ładuje się raz na proces; do tego czasu kafelek jest po prostu
    // pusty — mignięcie ikony, która zaraz znika, byłoby gorsze niż nic.
    val loading = application != null && info == null
    val imageModel = info?.imageModel

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(StronkTheme.colors.surfaceTile),
        contentAlignment = Alignment.Center,
    ) {
        when {
            imageModel != null -> AsyncImage(
                model = imageModel,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            loading -> Unit

            else -> Icon(
                imageVector = info?.icon ?: StronkIcons.start,
                contentDescription = null,
                tint = StronkTheme.colors.textDim,
                modifier = Modifier.size(size * FALLBACK_GLYPH_RATIO),
            )
        }
    }
}

/**
 * Wiersz listy ćwiczeń z miniaturą (mocki: `.exrow`, `.row`) — to samo, co
 * [StronkListRow], tylko zamiast piktogramu partii stoi zdjęcie ćwiczenia.
 *
 * @param caption KAPITALIK pod nazwą (np. partia mięśniowa)
 * @param trailing krótki tekst po prawej jako chip — NIGDY fraza typu „40×10”
 * @param trailingContent slot po prawej zamiast [trailing] (sparkline, ikona)
 */
@Composable
fun StronkExerciseRow(
    exerciseId: String,
    title: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    trailing: String? = null,
    trailingContent: @Composable (RowScope.() -> Unit)? = null,
    thumbSize: Dp = StronkSizes.iconTile,
    thumbCorner: Dp = 12.dp,
    chevron: Boolean = false,
    divider: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            StronkExerciseThumb(
                exerciseId = exerciseId,
                size = thumbSize,
                cornerRadius = thumbCorner,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = StronkTextStyles.h2,
                    color = MaterialTheme.colorScheme.onSurface,
                    // Mocki (`verify-c` ramka 2, `verify-sheet-2` ramka 2) łamią długie
                    // nazwy na DWIE linie. Przy jednej linii „Wyciskanie na maszynie
                    // siedząc" i „Wyciskanie nogami na suwnicy" ucinały się do
                    // nierozróżnialnego „Wycisk…".
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (caption != null) {
                    Text(
                        text = caption.uppercase(),
                        style = StronkTextStyles.cap,
                        color = StronkTheme.colors.textDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                }
            }
            if (trailing != null) {
                StronkChip(label = trailing)
            }
            trailingContent?.invoke(this)
            if (chevron) {
                Icon(
                    imageVector = StronkIcons.chevron,
                    contentDescription = null,
                    tint = StronkTheme.colors.textDim,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        if (divider) {
            HorizontalDivider(
                thickness = StronkSizes.hairline,
                color = StronkTheme.colors.lineSoft,
            )
        }
    }
}

/** Ile kafelka zajmuje ikona fallbacku — tyle, co glif w [StronkIconBadge]. */
private const val FALLBACK_GLYPH_RATIO = 0.45f

/** Co miniatura potrzebuje wiedzieć o ćwiczeniu: obrazek albo ikona zastępcza. */
private data class ExerciseThumbInfo(val imageModel: String?, val icon: ImageVector)

/**
 * Indeks miniatur budowany RAZ z datasetu (873 ćwiczenia) — bez tego każdy
 * wiersz listy przeszukiwałby liniowo całą bazę przy każdym przewinięciu.
 * Wyścig przy pierwszym budowaniu jest nieszkodliwy: obie mapy są identyczne.
 */
private object ExerciseThumbs {

    @Volatile
    private var index: Map<String, ExerciseThumbInfo>? = null

    suspend fun of(repository: ExerciseRepository, exerciseId: String): ExerciseThumbInfo? {
        val map = index ?: repository.getAll().associate { exercise ->
            exercise.id to ExerciseThumbInfo(
                imageModel = exercise.images.firstOrNull()
                    ?.let { ExerciseRepository.IMAGES_BASE_URI + it },
                icon = MuscleIcons.forExercise(exercise),
            )
        }.also { index = it }
        return map[exerciseId]
    }
}
