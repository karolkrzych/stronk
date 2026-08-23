package com.stronk.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import com.stronk.ui.profile.ProfileEquipment
import com.stronk.ui.theme.StronkRadius
import com.stronk.ui.theme.StronkSizes
import com.stronk.ui.theme.StronkTheme

/**
 * Menu rozwijane na dokładną szerokość kotwicy, w skórze „Limonka" (tło
 * `surfaceMuted` odcięte od karty + hairline obrys) — wspólny rdzeń wizualny
 * wyciągnięty z [StronkEquipmentFilterButton] (patrz jego doc dla historii
 * pomiarów). Każdy trigger (ghost button, chip, cokolwiek) mierzy własną
 * szerokość przez `Modifier.onSizeChanged` na opakowującym [Box] i przekazuje
 * ją tu jako [anchorWidthPx] — reużywalne niezależnie od kształtu kotwicy.
 */
@Composable
fun StronkAnchoredDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    anchorWidthPx: Int,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val density = LocalDensity.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.width(with(density) { anchorWidthPx.toDp() }),
        shape = StronkRadius.innerShape,
        containerColor = StronkTheme.colors.surfaceMuted,
        border = BorderStroke(StronkSizes.hairline, StronkTheme.colors.line),
        content = content,
    )
}

/**
 * Full-width „Filtruj" pod tytułem arkusza zamienników + Material3
 * [DropdownMenu] w kolorach theme. Współdzielona przez arkusz zamienników
 * treningu ([com.stronk.ui.workout]) i edytora planu ([com.stronk.ui.plans]) —
 * wygląd i zachowanie MUSZĄ zostać identyczne, więc żyje tu jeden raz.
 *
 * Domyślny `containerColor` menu (`surfaceContainer` = `--s1`) jest IDENTYCZNY
 * z tłem arkusza (`surfaceCard` = `--s1`) — zero odcięcia, menu znikało w tle.
 * Naprawione jawnym `containerColor = surfaceMuted` (`--s3`, wyraźnie jaśniejsze
 * niż karta) + hairline obrys `line`. Szerokość i pozycja: mierzymy szerokość
 * przycisku (`onSizeChanged` na kontenerze-Box, px→dp przez [LocalDensity])
 * i wymuszamy tę samą szerokość na menu — bez tego menu miało intrinsic
 * szerokość dopasowaną do treści (węższą, przyklejoną do lewej krawędzi
 * pełnoszerokiego przycisku).
 *
 * Multi-select: tap pozycji dopisuje/zdejmuje grupę, menu zostaje otwarte, żeby
 * dało się zaznaczyć kilka naraz; zamyka je dopiero tap poza nim.
 *
 * Aktywny filtr sygnalizuje limonkowy akcent [StronkGhostButton] (`accent = true`)
 * + licznik zaznaczonych grup w etykiecie — bez dodatkowych kolorów czy odznak.
 */
@Composable
fun StronkEquipmentFilterButton(
    groups: List<String>,
    selected: Set<String>,
    onToggle: (groupId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var anchorWidthPx by remember { mutableIntStateOf(0) }
    Box(
        modifier = modifier.onSizeChanged { anchorWidthPx = it.width },
    ) {
        StronkGhostButton(
            text = if (selected.isEmpty()) "Filtruj" else "Filtruj (${selected.size})",
            onClick = { expanded = true },
            icon = StronkIcons.filter,
            accent = selected.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        )
        StronkAnchoredDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            anchorWidthPx = anchorWidthPx,
        ) {
            groups.forEach { groupId ->
                val isSelected = groupId in selected
                DropdownMenuItem(
                    text = { Text(ProfileEquipment.titleOf(groupId)) },
                    onClick = { onToggle(groupId) },
                    trailingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = StronkIcons.done,
                                contentDescription = null,
                                tint = StronkTheme.colors.lime,
                            )
                        }
                    } else {
                        null
                    },
                )
            }
        }
    }
}
