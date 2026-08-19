package com.stronk.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.stronk.ui.PlLabels
import com.stronk.ui.components.StronkChoiceChip
import com.stronk.ui.components.StronkEmptyState
import com.stronk.ui.components.StronkIcons
import com.stronk.ui.components.StronkNoteCard
import com.stronk.ui.components.StronkSectionHeader
import com.stronk.ui.components.StronkTone
import com.stronk.ui.theme.StronkSpacing

/**
 * Zakładka „Sprzęt” — chipy pogrupowane w sekcje. Zaznaczenie zmienia wyłącznie
 * kolory (obrys jest rysowany zawsze), więc żaden chip nie zmienia rozmiaru
 * i nic nie ucieka spod palca — to była naprawa reflow z alfy.
 */
@Composable
internal fun ProfileEquipmentTab(
    options: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val groups = ProfileEquipment.groupsOf(options)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = StronkSpacing.screen)
            .padding(top = StronkSpacing.xl, bottom = StronkSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(StronkSpacing.section),
    ) {
        if (groups.isEmpty()) {
            StronkEmptyState(
                icon = StronkIcons.start,
                title = "Wczytujemy bazę ćwiczeń",
                description = "Lista sprzętu pojawi się za chwilę.",
            )
            return@Column
        }

        StronkNoteCard(
            text = ProfileTexts.equipmentHint(selected.size),
            modifier = Modifier.fillMaxWidth(),
            tone = if (selected.isEmpty()) StronkTone.NEUTRAL else StronkTone.ACCENT,
        )

        groups.forEach { group ->
            Column(verticalArrangement = Arrangement.spacedBy(StronkSpacing.sm)) {
                StronkSectionHeader(title = group.title)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(StronkSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(StronkSpacing.xs),
                ) {
                    group.items.forEach { item ->
                        StronkChoiceChip(
                            label = PlLabels.equipment(item),
                            selected = item in selected,
                            onClick = { onToggle(item) },
                        )
                    }
                }
            }
        }
    }
}
