package com.stronk.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.stronk.ui.theme.StronkRadius
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTextStyles
import com.stronk.ui.theme.StronkTheme

/**
 * Karta sekcji (mocki: `.daycard`) — powierzchnia `--s1`, promień `--r-card` 24,
 * padding `--pad-card` 20. BEZ obrysu: w „Limonce" karta odcina się samą jasnością.
 *
 * Karty układaj w `Column` z odstępem [StronkSpacing.section]; nigdy nie sklejaj
 * ich w gęstą listę wierszy bez oddechu.
 */
@Composable
fun StronkCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(StronkSpacing.card),
    shape: Shape = StronkRadius.cardShape,
    content: @Composable ColumnScope.() -> Unit,
) {
    val color = StronkTheme.colors.surfaceCard
    if (onClick == null) {
        Surface(modifier = modifier, shape = shape, color = color) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    } else {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = color) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    }
}

/**
 * Karta akcentowana (mocki: `.record`) — tło `--lime-dim`, obrys `--lime-line`,
 * promień `--r-card`. Zarezerwowana dla REKORDU / PR-a: dokładnie jedna na ekran,
 * inaczej limonka przekracza budżet ~10% powierzchni.
 *
 * Liczby w środku podawaj przez [StronkStatBlock] z `valueColor = colors.lime`.
 */
@Composable
fun StronkAccentCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(StronkSpacing.card),
    shape: Shape = StronkRadius.cardShape,
    content: @Composable ColumnScope.() -> Unit,
) {
    val color = StronkTheme.colors.limeDim
    val border = BorderStroke(1.dp, StronkTheme.colors.limeLine)
    if (onClick == null) {
        Surface(modifier = modifier, shape = shape, color = color, border = border) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    } else {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = color, border = border) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    }
}

/**
 * Kafelek/element WEWNĄTRZ karty (mocki: `.ico`, `.thumb`, `.chip`-owe tło) —
 * powierzchnia `--s2`, promień `--r-inner` 18. Samodzielnie na tle ekranu wygląda
 * jak zgubiony element, więc trzymaj go w [StronkCard].
 */
@Composable
fun StronkInsetCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(StronkSpacing.sm),
    shape: Shape = StronkRadius.innerShape,
    content: @Composable ColumnScope.() -> Unit,
) {
    val color = StronkTheme.colors.surfaceTile
    if (onClick == null) {
        Surface(modifier = modifier, shape = shape, color = color) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    } else {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = color) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    }
}

/**
 * Notka (mocki: `.note`) — karta `--s1` z lewą krechą 3 dp i JEDNĄ linijką
 * treści. Krecha limonkowa dla akcentu, `--s3` dla zwykłej uwagi.
 *
 * Jeśli tekst nie mieści się w linijce–dwóch, to nie jest notka tylko akapit —
 * schowaj go za chevronem albo ikoną „i".
 *
 * @param text jedno–dwa zdania, nie akapit
 * @param tone [StronkTone.ACCENT] = krecha limonkowa; reszta = krecha neutralna
 * @param label opcjonalny KAPITALIK nad tekstem (komponent robi wersaliki)
 * @param icon opcjonalna ikona przed tekstem (`Icons.Rounded.*`)
 */
@Composable
fun StronkNoteCard(
    text: String,
    modifier: Modifier = Modifier,
    tone: StronkTone = StronkTone.NEUTRAL,
    label: String? = null,
    icon: ImageVector? = null,
) {
    val stripe = when (tone) {
        StronkTone.ACCENT, StronkTone.SUCCESS -> StronkTheme.colors.lime
        else -> StronkTheme.colors.surfaceMuted
    }
    Surface(
        modifier = modifier,
        shape = StronkRadius.innerShape,
        color = StronkTheme.colors.surfaceCard,
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Surface(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(),
                color = stripe,
                content = {},
            )
            Row(
                modifier = Modifier.padding(
                    start = 14.dp,
                    end = StronkSpacing.md,
                    top = StronkSpacing.sm,
                    bottom = StronkSpacing.sm,
                ),
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = StronkTheme.colors.textDim,
                        modifier = Modifier
                            .padding(end = 11.dp, top = 2.dp)
                            .size(16.dp),
                    )
                }
                Column {
                    if (label != null) {
                        Text(
                            text = label.uppercase(),
                            style = StronkTextStyles.cap,
                            color = StronkTheme.colors.textDim,
                            modifier = Modifier.padding(bottom = 5.dp),
                        )
                    }
                    Text(
                        text = text,
                        style = StronkTextStyles.meta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
