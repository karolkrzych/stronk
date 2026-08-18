package com.stronk.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import com.stronk.ui.theme.StronkTheme

/**
 * Dolna nawigacja w języku mocków — powierzchnia karty, wybrana pozycja
 * podświetlona akcentem indygo, reszta wygaszona. Ikony bierz z `Icons.Rounded.*`.
 */
@Composable
fun StronkNavigationBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = Dp.Hairline,
        content = content,
    )
}

/** Pozycja dolnej nawigacji: ikona + krótka etykieta (1 słowo). */
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
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
            unselectedIconColor = StronkTheme.colors.textDim,
            unselectedTextColor = StronkTheme.colors.textDim,
        ),
    )
}
