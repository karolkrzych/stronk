package com.stronk.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTheme

/**
 * Badge statusu (mocki: `.chip.on`, oznaczenia „zaliczone”/„uwaga”) — krótkie
 * słowo lub sama ikona, kolor niosący znaczenie. Zamiast zdania „ćwiczenie mocno
 * obciąża kolano” daj badge WARNING z tekstem "kolano".
 *
 * @param text 1–2 słowa; dłuższy tekst to znak, że potrzebujesz [StronkNoteCard]
 * @param tone semantyka: NEUTRAL / ACCENT / SUCCESS / WARNING / DANGER
 * @param icon opcjonalna ikona przed tekstem (`Icons.Rounded.*`)
 */
@Composable
fun StronkBadge(
    text: String,
    modifier: Modifier = Modifier,
    tone: StronkTone = StronkTone.NEUTRAL,
    icon: ImageVector? = null,
) {
    val content = tone.onContainerColor()
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = tone.containerColor(),
        border = BorderStroke(1.dp, if (tone == StronkTone.NEUTRAL) MaterialTheme.colorScheme.outline else tone.accentColor()),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = StronkSpacing.sm, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = tone.accentColor(), modifier = Modifier.size(14.dp))
            }
            Text(text = text, style = MaterialTheme.typography.labelMedium, color = content, maxLines = 1)
        }
    }
}

/**
 * Chip wyboru (mocki: `.chip` / `.chip.on`) — element klikalny ze stanem.
 * Zaznaczony dostaje kolor [tone]; niezaznaczony jest neutralny i wygaszony.
 *
 * Uwaga UX (znany problem alfy): przy dużej liczbie chipów układaj je tak, żeby
 * zaznaczenie nie zmieniało rozmiaru chipa — obrys i tło tak, szerokość nie.
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
) {
    val accent = tone.accentColor()
    val border = if (selected) accent else MaterialTheme.colorScheme.outline
    val background = if (selected) accent.copy(alpha = 0.13f) else MaterialTheme.colorScheme.surfaceVariant
    val content = when {
        !enabled -> StronkTheme.colors.textDim
        selected -> tone.onContainerColor()
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val ringModifier = if (selected) {
        Modifier.border(2.dp, accent.copy(alpha = 0.35f), CircleShape)
    } else {
        Modifier
    }
    Surface(
        onClick = onClick,
        modifier = modifier.then(ringModifier),
        enabled = enabled,
        shape = CircleShape,
        color = background,
        border = BorderStroke(1.dp, border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = StronkSpacing.md, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) accent else content,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 13.5.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                ),
                color = content,
                maxLines = 1,
            )
        }
    }
}

/** Rozmiary kwadratowego badge'a z piktogramem (mocki: `.mbadge.s30/.s44/.s56`). */
enum class StronkIconBadgeSize(internal val box: Dp, internal val glyph: Dp, internal val corner: Dp) {
    SMALL(30.dp, 18.dp, 9.dp),
    MEDIUM(44.dp, 26.dp, 13.dp),
    LARGE(56.dp, 32.dp, 16.dp),
}

/**
 * Kwadratowy badge z piktogramem (mocki: `.mbadge`) — wizytówka ćwiczenia,
 * partii mięśniowej albo sekcji. To on odróżnia listę „stronk” od listy tekstu:
 * KAŻDY wiersz ćwiczenia i KAŻDA karta ćwiczenia ma taki badge.
 *
 * @param icon ikona z material-icons-extended, dobrana do partii/roli
 * @param tone domyślnie indygo (paleta piktogramów); WARNING dla ćwiczeń
 *        naruszających ograniczenia, SUCCESS dla zaliczonych
 */
@Composable
fun StronkIconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: StronkIconBadgeSize = StronkIconBadgeSize.MEDIUM,
    tone: StronkTone? = null,
    contentDescription: String? = null,
) {
    val background = tone?.containerColor() ?: StronkTheme.colors.iconBadgeBackground
    val border = tone?.accentColor() ?: StronkTheme.colors.iconBadgeBorder
    val content = tone?.accentColor() ?: StronkTheme.colors.iconBadgeContent
    Surface(
        modifier = modifier.size(size.box),
        shape = RoundedCornerShape(size.corner),
        color = background,
        border = BorderStroke(1.dp, border),
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
