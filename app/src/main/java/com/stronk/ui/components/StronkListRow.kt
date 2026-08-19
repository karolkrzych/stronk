package com.stronk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stronk.ui.theme.StronkSizes
import com.stronk.ui.theme.StronkTextStyles
import com.stronk.ui.theme.StronkTheme

/**
 * Wiersz listy (mocki: `.exrow`, `.row`, `.libitem`) — kafelek z piktogramem,
 * nazwa `--fs-h2` 17, po prawej jeden chip albo chevron, pod spodem cienki
 * dzielnik `--line-soft`.
 *
 * W „Limonce" wiersz NIE jest kartą: nie ma własnego tła ani obrysu. Listę
 * wierszy wkładasz do [StronkCard] albo kładziesz wprost na tle ekranu.
 *
 * @param title nazwa pozycji (1 wiersz, ucinana wielokropkiem)
 * @param icon piktogram do kafelka; null = wiersz bez kafelka (rzadko)
 * @param iconLabel KAPITALIK pod nazwą, np. partia mięśniowa
 * @param subtitle drugi wiersz pod nazwą, wygaszony; wyklucza się z [iconLabel]
 * @param trailing krótki tekst po prawej — renderowany jako chip. NIGDY nie
 *        wkładaj tu frazy typu „40×10": liczby idą w kolumny albo w stat-bloki
 * @param trailingContent slot po prawej zamiast [trailing] (chip, ikona, checkbox)
 * @param chevron dokłada strzałkę „w szczegóły" na końcu wiersza
 * @param divider cienka linia pod wierszem; ostatni wiersz sekcji dostaje `false`
 * @param tone ton kafelka — ACCENT/SUCCESS podbija ikonę limonką
 * @param inset wiersz stoi WEWNĄTRZ karty (zostawione dla zgodności; w „Limonce"
 *        wiersz wygląda tak samo w obu miejscach)
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
    chevron: Boolean = false,
    divider: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            if (icon != null) {
                StronkIconBadge(icon = icon, size = StronkIconBadgeSize.MEDIUM, tone = tone)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = StronkTextStyles.h2,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                when {
                    subtitle != null -> Text(
                        text = subtitle,
                        style = StronkTextStyles.meta,
                        color = StronkTheme.colors.textDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp),
                    )

                    iconLabel != null -> Text(
                        text = iconLabel.uppercase(),
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

/**
 * Wiersz „następne w kolejce" (mocki: `.next .row`) — mniejszy kafelek 34 dp,
 * nazwa w `--text-2`, chevron na końcu. Pod kapitalikiem „NASTĘPNE", nad
 * dolną krawędzią ekranu treningu.
 */
@Composable
fun StronkNextRow(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) {
            StronkIconBadge(
                icon = icon,
                size = StronkIconBadgeSize.SMALL,
                modifier = Modifier.size(StronkSizes.iconTileSmall),
            )
        }
        Text(
            text = title,
            style = StronkTextStyles.h2,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = StronkIcons.chevron,
            contentDescription = null,
            tint = StronkTheme.colors.textDim,
            modifier = Modifier.size(18.dp),
        )
    }
}
