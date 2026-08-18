package com.stronk.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTheme

/**
 * Nagłówek ekranu (mocki: `.app-head`) — jeden duży tytuł, wygaszony podtytuł
 * i opcjonalny chip meta po prawej. To jedyne miejsce na tytuł ekranu; nie
 * dokładaj `TopAppBar`.
 *
 * @param title tytuł ekranu, krótki (1–3 słowa), np. "Twój tydzień"
 * @param subtitle drugi wiersz, wygaszony — daty, licznik tygodnia itd.
 * @param meta tekst chipa po prawej, np. "tydzień 2/5"; null = brak chipa
 * @param actions slot na ikony akcji po prawej (zamiast/obok chipa meta)
 */
@Composable
fun StronkScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    meta: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(StronkSpacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                    color = StronkTheme.colors.textDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
        if (meta != null) {
            StronkMetaChip(meta, Modifier.padding(top = StronkSpacing.xxs))
        }
        actions()
    }
}

/** Mały chip meta w nagłówku (mocki: `.chip-meta`) — licznik, kontekst, nic więcej. */
@Composable
fun StronkMetaChip(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = StronkSpacing.sm, vertical = 6.dp),
            maxLines = 1,
        )
    }
}

/**
 * Nagłówek sekcji (mocki: `.sec-k`) — mały, rozstrzelony kicker WERSALIKAMI.
 * Rytm sekcji jest ten sam na każdym ekranie: kicker → odstęp 12 dp → treść.
 *
 * @param title tekst kickera podany normalnie; komponent sam robi wersaliki
 * @param icon ikona z `Icons.Rounded.*` (material-icons-extended); null = bez ikony
 * @param trailing slot po prawej — licznik ("zaznaczone: 2"), akcja tekstowa itd.
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
        horizontalArrangement = Arrangement.spacedBy(StronkSpacing.xs),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = StronkTheme.colors.textDim,
                modifier = Modifier.size(15.dp),
            )
        }
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = StronkTheme.colors.textDim,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (trailing != null) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) { trailing() }
        }
    }
}
