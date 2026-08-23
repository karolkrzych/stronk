package com.stronk.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTextStyles
import com.stronk.ui.theme.StronkTheme

/**
 * Nagłówek ekranu (mocki: `.h1` w `.navbar`) — jeden tytuł `--fs-h1` 24 i
 * ewentualnie chip meta albo ikona po prawej. To jedyne miejsce na tytuł ekranu;
 * nie dokładaj `TopAppBar`.
 *
 * @param title tytuł ekranu, krótki (1–3 słowa), np. "Tydzień 1/6"
 * @param subtitle drugi wiersz, wygaszony — daty, kontekst
 * @param meta tekst chipa po prawej; null = brak chipa
 * @param titleTrailing mały slot tuż obok tytułu (ta sama linia), np. akcja
 *        "Zaplanuj" — dla akcji, która logicznie należy do tytułu, nie do
 *        grupy `actions` po prawej krawędzi. Gdy podany, tytuł dzieli z nim
 *        wiersz (maxLines = 1, kurczy się przed nim zamiast go spychać).
 * @param titleStyle styl tytułu; domyślnie `h1` (24). Węższe nagłówki
 *        (np. Tydzień z trzema IconButtonami + "Zaplanuj" obok tytułu) mogą
 *        podać `h1Small` (21), żeby najdłuższy wariant tytułu zmieścił się
 *        w jednej linii bez łamania layoutu.
 * @param actions slot na ikony akcji po prawej (np. dyskretne „i")
 */
@Composable
fun StronkScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    meta: String? = null,
    titleTrailing: (@Composable () -> Unit)? = null,
    titleStyle: androidx.compose.ui.text.TextStyle = StronkTextStyles.h1,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(StronkSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            if (titleTrailing != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = titleStyle,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(StronkSpacing.xs))
                    titleTrailing()
                }
            } else {
                Text(
                    text = title,
                    style = titleStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = StronkTextStyles.meta,
                    color = StronkTheme.colors.textDim,
                    modifier = Modifier.padding(top = StronkSpacing.xxs),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (meta != null) {
            StronkMetaChip(meta)
        }
        actions()
    }
}

/** Mały chip meta w nagłówku (mocki: `.chip`) — licznik, kontekst, nic więcej. */
@Composable
fun StronkMetaChip(text: String, modifier: Modifier = Modifier) =
    StronkChip(label = text, modifier = modifier)

/**
 * Kicker sekcji (mocki: `.cap`) — KAPITALIK 11 z trackingiem `.14em` w `--text-3`.
 * Rytm sekcji jest ten sam na każdym ekranie: kicker → 10–12 dp → treść.
 *
 * @param title tekst podany normalnie; wersaliki robi komponent
 * @param icon ikona z `Icons.Rounded.*`; np. [StronkIcons.info] przy „NASTĘPNE"
 * @param trailing slot po prawej — akcja tekstowa, licznik, mini-stat
 */
@Composable
fun StronkSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            text = title.uppercase(),
            style = StronkTextStyles.cap,
            color = StronkTheme.colors.textDim,
            maxLines = 1,
        )
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = StronkTheme.colors.textDim,
                modifier = Modifier.size(15.dp),
            )
        }
        if (trailing != null) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) { trailing() }
        }
    }
}
