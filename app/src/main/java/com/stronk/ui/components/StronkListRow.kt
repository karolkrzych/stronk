package com.stronk.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTheme

/**
 * Wiersz listy (mocki: `.ex-row`) — kwadratowy piktogram, nazwa, po prawej
 * zwięzła wartość. To jest DOMYŚLNY sposób pokazywania listy ćwiczeń/planów;
 * gołe `Text` jeden pod drugim = ściana tekstu i zostanie odrzucone.
 *
 * @param title nazwa pozycji (1 wiersz, ucinana wielokropkiem)
 * @param icon piktogram do badge'a; null = wiersz bez badge'a (rzadko)
 * @param iconLabel malutki podpis pod badge'em, np. partia mięśniowa
 * @param subtitle drugi wiersz pod nazwą, wygaszony; null = jednowierszowy
 * @param trailing tekst po prawej, np. "3×8"
 * @param trailingContent slot po prawej zamiast [trailing] (ikona, badge, checkbox)
 * @param tone ton badge'a — WARNING gdy ćwiczenie łamie ograniczenie, SUCCESS gdy zaliczone
 * @param inset true = wiersz stoi WEWNĄTRZ karty (jaśniejsze tło); false = na tle ekranu
 */
@Composable
fun StronkListRow(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconLabel: String? = null,
    subtitle: String? = null,
    trailing: String? = null,
    trailingContent: @Composable (RowScope.() -> Unit)? = null,
    tone: StronkTone? = null,
    inset: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val container =
        if (inset) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceContainer
    val body: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaddingValues(start = StronkSpacing.xs, top = StronkSpacing.xs, end = 14.dp, bottom = StronkSpacing.xs)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(StronkSpacing.sm),
        ) {
            if (icon != null) {
                Column(
                    modifier = Modifier.width(46.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    StronkIconBadge(icon = icon, size = StronkIconBadgeSize.SMALL, tone = tone)
                    if (iconLabel != null) {
                        Text(
                            text = iconLabel.uppercase(),
                            style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp, lineHeight = 10.sp),
                            color = StronkTheme.colors.textDim,
                            maxLines = 1,
                        )
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = StronkTheme.colors.textDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (trailing != null) {
                Text(
                    text = trailing,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            trailingContent?.invoke(this)
        }
    }
    val shape = MaterialTheme.shapes.medium
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    if (onClick == null) {
        Surface(modifier = modifier, shape = shape, color = container, border = border) { body() }
    } else {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = container, border = border) { body() }
    }
}
