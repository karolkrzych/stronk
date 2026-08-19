package com.stronk.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stronk.ui.theme.StronkRadius
import com.stronk.ui.theme.StronkSizes
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTextStyles
import com.stronk.ui.theme.StronkTheme

/** CTA nie ma pionowego paddingu Materiala — wysokość robi `height`, nie treść. */
private val CtaContentPadding = PaddingValues(horizontal = StronkSpacing.md)

/**
 * Ghost bywa wąski (rząd 4:1 w przerwie: ~67 dp na „+30 s"), więc domyślne
 * 24 dp Materiala z każdej strony nie ma prawa bytu — tekst zawijał się wtedy
 * na „+3" / „0 s".
 */
private val GhostContentPadding = PaddingValues(horizontal = StronkSpacing.xs)

/**
 * Główne CTA ekranu (mocki: `.cta`) — pełna szerokość, limonka, tekst
 * `--lime-ink`, promień `--r-inner` 18, wysokość 66 dp, tekst 19/700.
 *
 * Na ekranie jest DOKŁADNIE jedno takie — to jedyna duża plama limonki
 * w budżecie ~10%. Wszystko inne to [StronkGhostButton] albo [StronkTextAction].
 *
 * @param height [StronkSizes.ctaSmall] (54 dp) dla CTA wewnątrz karty (`.cta.sm`)
 */
@Composable
fun StronkPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = StronkSizes.cta,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        enabled = enabled,
        shape = StronkRadius.innerShape,
        contentPadding = CtaContentPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = StronkTheme.colors.lime,
            contentColor = StronkTheme.colors.limeInk,
            disabledContainerColor = StronkTheme.colors.surfaceTile,
            disabledContentColor = StronkTheme.colors.textDim,
        ),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
        }
        Text(
            text = text,
            style = StronkTextStyles.cta,
            modifier = if (icon != null) Modifier.padding(start = 10.dp) else Modifier,
        )
    }
}

/**
 * Akcja drugorzędna (mocki: `.ghost`) — przezroczyste tło, obrys `--line`,
 * tekst `--text-2`, wysokość 56 dp, promień 18.
 *
 * @param accent wariant `.ghost.accent` — obrys `--lime-line`, tło `--lime-dim`,
 *        tekst `--lime`. Dla akcji, która jest „prawie główna" (np. „Pomiń
 *        przerwę" w proporcji 4:1 obok „+30 s"). Nigdy dwa akcentowane obok siebie.
 *
 * Etykieta NIGDY się nie zawija: w rzędzie 4:1 wąski ghost ma ~67 dp i domyślny
 * padding Materiala (2×24 dp) łamał „+30 s" na dwie linie. Stąd własny, ciasny
 * [GhostContentPadding] i `softWrap = false` — mock ma tu jedną linię.
 */
@Composable
fun StronkGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    accent: Boolean = false,
    height: Dp = StronkSizes.ghost,
) {
    val content = if (accent) StronkTheme.colors.lime else MaterialTheme.colorScheme.onSurfaceVariant
    val border = if (accent) StronkTheme.colors.limeLine else StronkTheme.colors.line
    val container = if (accent) StronkTheme.colors.limeDim else Color.Transparent
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(height),
        enabled = enabled,
        shape = StronkRadius.innerShape,
        border = BorderStroke(1.dp, border),
        contentPadding = GhostContentPadding,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = StronkTheme.colors.textDim,
        ),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        Text(
            text = text,
            style = StronkTextStyles.h2,
            maxLines = 1,
            softWrap = false,
            modifier = if (icon != null) Modifier.padding(start = 9.dp) else Modifier,
        )
    }
}

/**
 * Akcja trzeciorzędna (mocki: `.more`) — mały tekst bez obwódki. Dla „pokaż
 * wszystkie / przesuń / odwołaj", nigdy dla akcji głównej.
 *
 * @param tone [StronkTone.ACCENT] daje limonkowy tekst (mock `.more`);
 *        [StronkTone.NEUTRAL] wygaszony `--text-3`
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
    val color = if (tone == StronkTone.NEUTRAL) StronkTheme.colors.textDim else tone.accentColor()
    TextButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = if (icon != null) Modifier.padding(start = 6.dp) else Modifier,
        )
    }
}

/**
 * Wielki przycisk kciukowy treningu — 108 dp, ogromny znak + KAPITALIK pod nim.
 * Zasada nr 1 apki: jeden tap na serię, trafialny bez patrzenia.
 *
 * W „Limonce" domyślnym CTA treningu jest 66-dp [StronkPrimaryButton] z mocka;
 * ten wariant zostaje dla ekranów, które świadomie chcą większy cel dotykowy.
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
            .height(StronkSizes.bigButton),
        enabled = enabled,
        shape = StronkRadius.innerShape,
        color = if (enabled) StronkTheme.colors.lime else StronkTheme.colors.surfaceTile,
        contentColor = if (enabled) StronkTheme.colors.limeInk else StronkTheme.colors.textDim,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(mark, contentDescription = null, modifier = Modifier.size(42.dp))
                Text(text = label.uppercase(), style = StronkTextStyles.cap)
            }
        }
    }
}

/**
 * Rząd akcji (mocki: `.btn-row`) — proporcje ustawiasz `Modifier.weight(...)`
 * na dzieciach. W przerwie to 4:1 („Pomiń przerwę" : „+30 s").
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
