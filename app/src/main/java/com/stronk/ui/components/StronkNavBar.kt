package com.stronk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stronk.ui.theme.StronkSizes
import com.stronk.ui.theme.StronkTheme

/**
 * Dolna nawigacja „Limonka" (mocki: `.nav`) — 64 dp, tło `--s0` (to samo co tło
 * ekranu), cienka linia `--line-soft` u góry, SAME IKONY bez etykiet, aktywna
 * limonkowa, reszta `--text-3`. Bez pigułki-wskaźnika pod ikoną (indicator jest
 * przezroczysty) — ma być „delikatne, nie zbyt agresywne".
 *
 * Tylko na ekranach zakładek: Dziś / Tydzień / Plany / Progres / Baza.
 */
@Composable
fun StronkNavigationBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding(),
    ) {
        HorizontalDivider(
            thickness = StronkSizes.hairline,
            color = StronkTheme.colors.lineSoft,
        )
        NavigationBar(
            modifier = Modifier.height(StronkSizes.navBar),
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = StronkTheme.colors.textDim,
            tonalElevation = Dp.Hairline,
            windowInsets = WindowInsets(0, 0, 0, 0),
            content = content,
        )
    }
}

/**
 * Pozycja dolnej nawigacji — SAMA ikona (stroke 2 px, `Icons.Rounded.*`).
 * [label] nie jest rysowana: służy jako opis dla czytnika ekranu. Tekst pod
 * ikoną został wycięty w rundzie „Limonka" i nie wraca.
 */
@Composable
fun RowScope.StronkNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp),
            )
        },
        label = null,
        alwaysShowLabel = false,
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = StronkTheme.colors.lime,
            selectedTextColor = StronkTheme.colors.lime,
            indicatorColor = Color.Transparent,
            unselectedIconColor = StronkTheme.colors.textDim,
            unselectedTextColor = StronkTheme.colors.textDim,
        ),
    )
}
