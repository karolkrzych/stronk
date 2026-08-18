package com.stronk.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.rounded.PrecisionManufacturing
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.stronk.ui.PlLabels
import com.stronk.ui.components.MuscleIcons
import com.stronk.ui.components.StronkEmptyState
import com.stronk.ui.components.StronkIcons
import com.stronk.ui.components.StronkListRow
import com.stronk.ui.components.StronkNoteCard
import com.stronk.ui.components.StronkSectionHeader
import com.stronk.ui.components.StronkTone
import com.stronk.ui.theme.StronkSpacing

/**
 * Zakładka „Sprzęt” — pełna lista wartości z datasetu, ale pogrupowana w sekcje
 * i renderowana jako wiersze pełnej szerokości. Zaznaczenie zmienia tylko kolory,
 * nigdy szerokość, więc nic nie ucieka spod palca (naprawa reflow chipów z alfy).
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
            .padding(top = StronkSpacing.lg, bottom = StronkSpacing.xxl),
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
            tone = if (selected.isEmpty()) StronkTone.NEUTRAL else StronkTone.ACCENT,
        )

        groups.forEach { group ->
            Column(verticalArrangement = Arrangement.spacedBy(StronkSpacing.sm)) {
                StronkSectionHeader(title = group.title, icon = groupIcon(group.id))
                Column(verticalArrangement = Arrangement.spacedBy(StronkSpacing.row)) {
                    group.items.forEach { item ->
                        val isOn = item in selected
                        StronkListRow(
                            title = PlLabels.equipment(item),
                            icon = MuscleIcons.forEquipment(item),
                            trailingContent = { EquipmentCheck(isOn) },
                            tone = if (isOn) StronkTone.ACCENT else StronkTone.NEUTRAL,
                            onClick = { onToggle(item) },
                        )
                    }
                }
            }
        }
    }
}

/** Znacznik zaznaczenia o stałym rozmiarze — zmienia się kolor, nie układ. */
@Composable
private fun EquipmentCheck(selected: Boolean) {
    Icon(
        imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
        contentDescription = null,
        tint = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outline
        },
        modifier = Modifier.size(22.dp),
    )
}

private fun groupIcon(groupId: String): ImageVector = when (groupId) {
    ProfileEquipment.FREE_WEIGHTS -> Icons.Rounded.FitnessCenter
    ProfileEquipment.MACHINES -> Icons.Rounded.PrecisionManufacturing
    ProfileEquipment.ACCESSORIES -> Icons.Rounded.Waves
    ProfileEquipment.BODYWEIGHT -> Icons.Rounded.Accessibility
    else -> Icons.Rounded.Handyman
}
