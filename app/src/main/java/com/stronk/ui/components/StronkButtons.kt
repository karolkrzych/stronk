package com.stronk.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stronk.ui.theme.StronkButtonShape
import com.stronk.ui.theme.StronkSizes
import com.stronk.ui.theme.StronkTheme

/**
 * Główne CTA ekranu (mocki: `.cta`) — akcent indygo, wysokość 56 dp, gruby tekst.
 * Na ekranie jest DOKŁADNIE jedno takie; wszystko inne to [StronkGhostButton]
 * albo [StronkTextAction].
 */
@Composable
fun StronkPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(StronkSizes.button)
            .shadow(
                elevation = 12.dp,
                shape = StronkButtonShape,
                ambientColor = MaterialTheme.colorScheme.primary,
                spotColor = MaterialTheme.colorScheme.primary,
                clip = false,
            ),
        enabled = enabled,
        shape = StronkButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = StronkTheme.colors.textDim,
        ),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = if (icon != null) Modifier.padding(start = 10.dp) else Modifier,
        )
    }
}

/** Akcja drugorzędna (mocki: `.btn-ghost`) — powierzchnia karty + obrys, bez akcentu. */
@Composable
fun StronkGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(StronkSizes.button),
        enabled = enabled,
        shape = StronkButtonShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            disabledContentColor = StronkTheme.colors.textDim,
        ),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            modifier = if (icon != null) Modifier.padding(start = 8.dp) else Modifier,
        )
    }
}

/**
 * Akcja trzeciorzędna (mocki: `.sec-actions u`, `.wiz-skip`) — mały, wygaszony
 * tekst. Dla „przesuń / odwołaj / pomiń”, nigdy dla akcji głównej.
 */
@Composable
fun StronkTextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: StronkTone = StronkTone.NEUTRAL,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    TextButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = tone.accentColor(), modifier = Modifier.size(16.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (tone == StronkTone.NEUTRAL) StronkTheme.colors.textDim else tone.accentColor(),
            modifier = if (icon != null) Modifier.padding(start = 6.dp) else Modifier,
        )
    }
}

/**
 * [StronkTextAction] z podkreśleniem (mocki: `.sec-actions u`, `.wiz-skip` —
 * `border-bottom`). Dla akcji trzeciorzędnych, które w mocku mają widoczną
 * kreskę pod tekstem: „przesuń” / „odwołaj” w harmonogramie, „pomiń ten krok”
 * w kreatorze planu.
 */
@Composable
fun StronkUnderlinedTextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.outline
    StronkTextAction(
        text = text,
        onClick = onClick,
        tone = StronkTone.NEUTRAL,
        modifier = modifier.drawBehind {
            val strokeWidth = 1.dp.toPx()
            val y = size.height - strokeWidth
            drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth)
        },
    )
}

/**
 * Wielki przycisk kciukowy treningu (mocki: `.cta-big`) — 108 dp wysokości,
 * ogromny znak + mały podpis WERSALIKAMI. Zasada nr 1 apki: jeden tap na serię,
 * trafialny bez patrzenia.
 *
 * @param mark ikona-znak, np. `Icons.Rounded.Check`
 * @param label podpis pod znakiem, np. "zalicz serię" (komponent robi wersaliki)
 */
@Composable
fun StronkBigActionButton(
    mark: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(StronkSizes.bigButton)
            .shadow(
                elevation = 18.dp,
                shape = MaterialTheme.shapes.extraLarge,
                ambientColor = MaterialTheme.colorScheme.primary,
                spotColor = MaterialTheme.colorScheme.primary,
                clip = false,
            ),
        enabled = enabled,
        shape = MaterialTheme.shapes.extraLarge,
        color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (enabled) MaterialTheme.colorScheme.onPrimary else StronkTheme.colors.textDim,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(mark, contentDescription = null, modifier = Modifier.size(46.dp))
                Text(text = label.uppercase(), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/**
 * Stopka z akcjami (mocki: `.wiz-nav`, `.rest-actions`) — ghost po lewej, CTA po
 * prawej. Proporcje ustawiasz `Modifier.weight(...)` na dzieciach (mocki: 1 / 1.7).
 */
@Composable
fun StronkFooterActions(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/**
 * Mała pigułkowa akcja (mocki: `.swap`) — tło przezroczyste, cienki obrys, tekst
 * wygaszony. Dla drugorzędnych akcji WEWNĄTRZ karty (np. „zamień” przy ćwiczeniu),
 * nie na stopce ekranu — tam jest [StronkGhostButton]/[StronkPrimaryButton].
 */
@Composable
fun StronkPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = StronkTheme.colors.textDim,
                    modifier = Modifier.size(14.dp),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                color = StronkTheme.colors.textDim,
            )
        }
    }
}
