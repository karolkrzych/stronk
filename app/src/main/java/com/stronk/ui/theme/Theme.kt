package com.stronk.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Motyw aplikacji — domyślne schematy Material 3, ciemny wg ustawień systemu.
 * Charakter wizualny (kolory, typografia) będzie osobną rundą — nie kombinujemy.
 */
@Composable
fun StronkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme(),
        content = content,
    )
}
