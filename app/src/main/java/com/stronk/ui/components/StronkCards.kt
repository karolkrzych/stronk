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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.stronk.ui.theme.StronkSpacing

/**
 * Karta sekcji — podstawowy budulec każdego ekranu (mocki: `.ex-card`, `.day-card`,
 * `.limit-panel`). Powierzchnia o stopień jaśniejsza od tła, miękki obrys, duży
 * promień i realny oddech w środku.
 *
 * Karty układaj w `Column` z odstępem [StronkSpacing.section]; nigdy nie sklejaj
 * ich w gęstą listę wierszy bez oddechu.
 */
@Composable
fun StronkCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(StronkSpacing.card),
    shape: Shape = MaterialTheme.shapes.large,
    content: @Composable ColumnScope.() -> Unit,
) {
    val color = MaterialTheme.colorScheme.surfaceContainer
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
 * Kafelek/wiersz WEWNĄTRZ karty (mocki: `.ex-row`, `.nb-stat`) — powierzchnia
 * o stopień jaśniejsza od karty. Samodzielnie na tle ekranu wygląda jak zgubiony
 * element, więc trzymaj go w [StronkCard].
 */
@Composable
fun StronkInsetCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(StronkSpacing.sm),
    shape: Shape = MaterialTheme.shapes.medium,
    content: @Composable ColumnScope.() -> Unit,
) {
    val color = MaterialTheme.colorScheme.surfaceVariant
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
 * Komunikat kontekstowy z akcentowanym paskiem po lewej (mocki: `.wiz-info`).
 * Jedno–dwa zdania, nie akapit: to podpowiedź, nie dokumentacja.
 */
@Composable
fun StronkNoteCard(
    text: String,
    modifier: Modifier = Modifier,
    tone: StronkTone = StronkTone.WARNING,
) {
    val stripe = tone.accentColor()
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Surface(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(),
                color = stripe,
                content = {},
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = StronkSpacing.md, vertical = StronkSpacing.sm),
            )
        }
    }
}
