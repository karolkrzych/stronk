package com.stronk.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stronk.ui.theme.StronkRadius
import com.stronk.ui.theme.StronkSizes
import com.stronk.ui.theme.StronkTextStyles
import com.stronk.ui.theme.StronkTheme

/**
 * Chip / pigułka (mocki: `.chip`) — 30 dp wysokości, promień `--r-pill`,
 * tło `--s2`, tekst `--fs-meta` 13 w `--text-2`.
 *
 * Wariant limonkowy ([selected] = true, mock `.chip.on` / `.chip.lime`) to TINT,
 * nie plama: tło `--lime-dim`, obrys `--lime-line`, tekst `--lime`. Rozmiar chipa
 * nie zmienia się przy zaznaczeniu — obrys jest zawsze rysowany.
 *
 * Pierwszą literę etykiety chip pokazuje ZAWSZE wielką (jak w mockach) — ekrany
 * nie muszą o tym pamiętać i mogą podawać etykiety z danych („pośladki").
 *
 * @param label 1–2 słowa, np. "Pośladki", "3 serie"
 * @param onClick null = chip jest etykietą, nie kontrolką
 */
@Composable
fun StronkChip(
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val content = when {
        !enabled -> StronkTheme.colors.textDim
        selected -> StronkTheme.colors.lime
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val container = if (selected) StronkTheme.colors.limeDim else StronkTheme.colors.surfaceTile
    val border = BorderStroke(
        1.dp,
        if (selected) StronkTheme.colors.limeLine else StronkTheme.colors.surfaceTile,
    )
    val body: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = StronkSizes.chip)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
            }
            Text(
                text = label.replaceFirstChar { it.uppercaseChar() },
                style = MaterialTheme.typography.labelMedium,
                color = content,
                maxLines = 1,
            )
        }
    }
    if (onClick == null) {
        Surface(modifier = modifier, shape = StronkRadius.pill, color = container, border = border) { body() }
    } else {
        Surface(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = StronkRadius.pill,
            color = container,
            border = border,
        ) { body() }
    }
}

/**
 * Badge statusu — chip niosący znaczenie (mocki: chip partii, „3 serie",
 * oznaczenie ćwiczenia). Zamiast zdania „ćwiczenie mocno obciąża kolano" daj
 * badge z ikoną ostrzeżenia i słowem "kolano".
 *
 * @param text 1–2 słowa; dłuższy tekst to znak, że potrzebujesz [StronkNoteCard]
 * @param tone ACCENT/SUCCESS = limonkowy tint; reszta neutralna (patrz [StronkTone])
 */
@Composable
fun StronkBadge(
    text: String,
    modifier: Modifier = Modifier,
    tone: StronkTone = StronkTone.NEUTRAL,
    icon: ImageVector? = null,
) = StronkChip(
    label = text,
    modifier = modifier,
    selected = tone == StronkTone.ACCENT || tone == StronkTone.SUCCESS,
    icon = icon,
)

/**
 * Mocny badge na limonce (mocki: `.record .badge`) — pełne tło `--lime`, tekst
 * `--lime-ink` KAPITALIKAMI. Maksymalnie jeden na ekran: to jedyna rzecz, która
 * krzyczy.
 *
 * Dziś BEZ UŻYĆ — rekord po przejściu na goły stat ([StronkStatHeadline]) nie
 * ma już plakietki „PR"; komponent zostaje w słowniku na wypadek realnego
 * krzyku (np. pierwszy rekord w bloku).
 */
@Composable
fun StronkAccentBadge(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Surface(modifier = modifier, shape = StronkRadius.pill, color = StronkTheme.colors.lime) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 24.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = StronkTheme.colors.limeInk,
                    modifier = Modifier.size(13.dp),
                )
            }
            Text(
                text = text.uppercase(),
                style = StronkTextStyles.cap,
                color = StronkTheme.colors.limeInk,
                maxLines = 1,
            )
        }
    }
}

/**
 * Chip wyboru ze stanem (mocki: `.chip` / `.chip.on`). Zaznaczony dostaje
 * limonkowy tint; niezaznaczony jest neutralny.
 *
 * Uwaga UX (znany problem alfy): układaj chipy tak, żeby zaznaczenie nie
 * zmieniało ich rozmiaru — tu obrys jest rysowany zawsze, więc szerokość stoi.
 */
@Composable
fun StronkChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: StronkTone = StronkTone.ACCENT,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) = StronkChip(
    label = label,
    modifier = modifier,
    selected = selected && tone != StronkTone.DANGER,
    onClick = onClick,
    icon = icon,
    enabled = enabled,
)

/** Rozmiary kafelka ikony (mocki: `.next .ico` 34, `.row .ico` 38, `--thumb` 62). */
enum class StronkIconBadgeSize(internal val box: Dp, internal val glyph: Dp, internal val corner: Dp) {
    SMALL(StronkSizes.iconTileSmall, 18.dp, 10.dp),
    MEDIUM(StronkSizes.iconTile, 18.dp, StronkRadius.tileSmall),
    LARGE(StronkSizes.thumb, 26.dp, StronkRadius.tile),
}

/**
 * Kafelek z piktogramem (mocki: `.ico`, `.thumb`) — kwadrat `--s2` z zaokrąglonym
 * rogiem i wygaszoną ikoną `--text-3`. BEZ obrysu i bez koloru: to podkładka pod
 * nazwę ćwiczenia, nie ozdoba. KAŻDY wiersz ćwiczenia ma taki kafelek.
 *
 * @param tone ACCENT/SUCCESS podbija ikonę limonką — używaj oszczędnie
 */
@Composable
fun StronkIconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: StronkIconBadgeSize = StronkIconBadgeSize.MEDIUM,
    tone: StronkTone? = null,
    contentDescription: String? = null,
) {
    val content = when (tone) {
        StronkTone.ACCENT -> StronkTheme.colors.lime
        StronkTone.SUCCESS -> StronkTheme.colors.limeDeep
        StronkTone.DANGER -> MaterialTheme.colorScheme.error
        else -> StronkTheme.colors.textDim
    }
    Surface(
        modifier = modifier.size(size.box),
        shape = RoundedCornerShape(size.corner),
        color = StronkTheme.colors.surfaceTile,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = content,
                modifier = Modifier.size(size.glyph),
            )
        }
    }
}
