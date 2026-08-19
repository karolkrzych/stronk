package com.stronk.ui.accesscode

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stronk.data.AccessCodeGenerator
import com.stronk.ui.theme.StronkTheme

/** `--code-box` 36 × 46, `--code-gap` 8, promień 10 — 1:1 z mockiem kodu. */
private val BoxHeight = 46.dp
private val BoxGap = 8.dp
private val BoxCorner = 10.dp

/** `--code-fs` 21, krój maszynowy: znaki kodu muszą mieć jednakową szerokość. */
private val CodeGlyph = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.SemiBold,
    fontSize = 21.sp,
    lineHeight = 24.sp,
)

/**
 * Kod dostępu w kratkach (mock `pack-historia-profil.html`, ekran 3: `.code i`) —
 * osiem osobnych pól `--s1`, znak w każdym. Kod nie jest zdaniem, tylko ciągiem
 * znaków do przepisania, więc każdy znak dostaje własne pudełko.
 *
 * Kratki dzielą szerokość po równo, żeby rząd nie wystawał poza ekran przy
 * większej czcionce systemowej.
 *
 * @param code znaki do pokazania; krótszy niż [length] zostawia puste kratki
 * @param activeIndex kratka podświetlona limonkowym obrysem (kursor przy wpisywaniu)
 */
@Composable
fun AccessCodeBoxes(
    code: String,
    modifier: Modifier = Modifier,
    length: Int = AccessCodeGenerator.CODE_LENGTH,
    activeIndex: Int? = null,
) {
    val glyphs = code.take(length)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = if (glyphs.isEmpty()) "Brak kodu" else glyphs },
        horizontalArrangement = Arrangement.spacedBy(BoxGap),
    ) {
        repeat(length) { index ->
            val active = index == activeIndex
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(BoxHeight),
                shape = RoundedCornerShape(BoxCorner),
                color = StronkTheme.colors.surfaceCard,
                border = if (active) BorderStroke(1.dp, StronkTheme.colors.limeLine) else null,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = glyphs.getOrNull(index)?.toString().orEmpty(),
                        style = CodeGlyph,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
