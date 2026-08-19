package com.stronk.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTextStyles
import com.stronk.ui.theme.StronkTheme

/**
 * Pusty stan — ikona, jedno zdanie i (opcjonalnie) jedna akcja. Nigdy tłumaczenie
 * na trzy akapity: użytkownik ma wiedzieć, co kliknąć, nie co się stało.
 *
 * @param icon ikona z material-icons-extended pasująca do braku (np. kalendarz)
 * @param title krótkie zdanie, np. "Nie masz jeszcze planu"
 * @param description jedno zdanie wsparcia; null gdy tytuł wystarcza
 * @param actionLabel etykieta CTA; null = pusty stan bez akcji
 */
@Composable
fun StronkEmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = StronkSpacing.lg, vertical = StronkSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StronkIconBadge(icon = icon, size = StronkIconBadgeSize.LARGE)
        Text(
            text = title,
            style = StronkTextStyles.h1Small,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = StronkSpacing.md),
        )
        if (description != null) {
            Text(
                text = description,
                style = StronkTextStyles.meta,
                color = StronkTheme.colors.textDim,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = StronkSpacing.xs)
                    .widthIn(max = 280.dp),
            )
        }
        if (actionLabel != null && onAction != null) {
            StronkPrimaryButton(
                text = actionLabel,
                onClick = onAction,
                modifier = Modifier
                    .padding(top = StronkSpacing.xl)
                    .widthIn(max = 300.dp),
            )
        }
    }
}
