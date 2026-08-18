package com.stronk.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.stronk.data.StressLevel
import com.stronk.ui.components.StronkCard
import com.stronk.ui.components.StronkEmptyState
import com.stronk.ui.components.StronkIconBadge
import com.stronk.ui.components.StronkIcons
import com.stronk.ui.components.StronkListRow
import com.stronk.ui.components.StronkNoteCard
import com.stronk.ui.components.StronkPrimaryButton
import com.stronk.ui.components.StronkSectionHeader
import com.stronk.ui.components.StronkTextAction
import com.stronk.ui.components.StronkTone
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTheme
import kotlinx.coroutines.launch

/** Krok arkusza dodawania/edycji ograniczenia; null = arkusz zamknięty. */
private sealed interface ConstraintSheet {
    /** Wybór stawu spośród jeszcze niedodanych. */
    data object PickJoint : ConstraintSheet

    /** Wybór limitu; [current] null = wpis dopiero powstaje. */
    data class PickSeverity(val joint: String, val current: StressLevel?) : ConstraintSheet
}

/**
 * Zakładka „Kontuzje” — lista tego, co realnie oszczędzamy. Stawy bez ograniczenia
 * nie zajmują miejsca na ekranie: dochodzą przez arkusz „Dodaj ograniczenie”.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileConstraintsTab(
    constraints: Map<String, StressLevel>,
    onConstraintChange: (joint: String, maxAccepted: StressLevel?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val added = ProfileDefaults.JOINT_KEYS.filter { it in constraints }
    val available = ProfileDefaults.JOINT_KEYS.filterNot { it in constraints }
    var sheet by remember { mutableStateOf<ConstraintSheet?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val closeSheet: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { sheet = null }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = StronkSpacing.screen)
            .padding(top = StronkSpacing.lg, bottom = StronkSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(StronkSpacing.section),
    ) {
        if (added.isEmpty()) {
            StronkEmptyState(
                icon = StronkIcons.injury,
                title = "Nic nie oszczędzamy",
                description = "Dodaj miejsce po kontuzji — ćwiczenia, które je obciążają, " +
                    "oznaczymy i podmienimy na bezpieczniejsze.",
                actionLabel = "Dodaj ograniczenie",
                onAction = { sheet = ConstraintSheet.PickJoint },
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(StronkSpacing.sm)) {
                StronkSectionHeader(title = "Oszczędzamy", icon = StronkIcons.injury)
                Column(verticalArrangement = Arrangement.spacedBy(StronkSpacing.row)) {
                    added.forEach { joint ->
                        val level = constraints.getValue(joint)
                        StronkListRow(
                            title = ProfileTexts.jointTitle(joint),
                            icon = StronkIcons.injury,
                            subtitle = ProfileTexts.severityRowText(level),
                            trailingContent = {
                                Icon(
                                    imageVector = StronkIcons.edit,
                                    contentDescription = "Zmień",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            tone = StronkTone.WARNING,
                            onClick = { sheet = ConstraintSheet.PickSeverity(joint, level) },
                        )
                    }
                }
            }

            StronkNoteCard(
                text = "Ćwiczenia powyżej limitu oznaczymy i zaproponujemy zamiennik.",
            )

            StronkPrimaryButton(
                text = "Dodaj ograniczenie",
                onClick = { sheet = ConstraintSheet.PickJoint },
                icon = StronkIcons.add,
                enabled = available.isNotEmpty(),
            )
        }
    }

    val current = sheet
    if (current != null) {
        ModalBottomSheet(
            onDismissRequest = { sheet = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = StronkSpacing.screen)
                    .padding(bottom = StronkSpacing.xxl),
                verticalArrangement = Arrangement.spacedBy(StronkSpacing.lg),
            ) {
                when (current) {
                    ConstraintSheet.PickJoint -> {
                        SheetTitle(
                            title = "Co oszczędzamy?",
                            subtitle = "Wybierz miejsce, które ma być traktowane ostrożnie.",
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(StronkSpacing.row)) {
                            available.forEach { joint ->
                                StronkListRow(
                                    title = ProfileTexts.jointTitle(joint),
                                    icon = StronkIcons.injury,
                                    tone = StronkTone.NEUTRAL,
                                    onClick = {
                                        sheet = ConstraintSheet.PickSeverity(joint, null)
                                    },
                                )
                            }
                        }
                    }

                    is ConstraintSheet.PickSeverity -> {
                        SheetTitle(
                            title = ProfileTexts.jointTitle(current.joint),
                            subtitle = "Jak mocno oszczędzamy to miejsce?",
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(StronkSpacing.sm)) {
                            ProfileTexts.SEVERITY_OPTIONS.forEach { level ->
                                SeverityCard(
                                    level = level,
                                    selected = current.current == level,
                                    onClick = {
                                        onConstraintChange(current.joint, level)
                                        closeSheet()
                                    },
                                )
                            }
                        }
                        if (current.current != null) {
                            StronkTextAction(
                                text = "Usuń ograniczenie",
                                onClick = {
                                    onConstraintChange(current.joint, null)
                                    closeSheet()
                                },
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                tone = StronkTone.DANGER,
                                icon = StronkIcons.delete,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(StronkSpacing.xxs)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = StronkTheme.colors.textDim,
        )
    }
}

@Composable
private fun SeverityCard(level: StressLevel, selected: Boolean, onClick: () -> Unit) {
    StronkCard(onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(StronkSpacing.sm),
        ) {
            StronkIconBadge(
                icon = severityIcon(level),
                tone = if (selected) StronkTone.WARNING else StronkTone.NEUTRAL,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = ProfileTexts.severityTitle(level),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = ProfileTexts.severityDescription(level),
                    style = MaterialTheme.typography.bodySmall,
                    color = StronkTheme.colors.textDim,
                )
            }
            Icon(
                imageVector = if (selected) {
                    Icons.Rounded.CheckCircle
                } else {
                    Icons.Rounded.RadioButtonUnchecked
                },
                contentDescription = null,
                tint = if (selected) {
                    StronkTheme.colors.warning
                } else {
                    MaterialTheme.colorScheme.outline
                },
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

private fun severityIcon(level: StressLevel): ImageVector =
    if (level == StressLevel.MEDIUM) Icons.Rounded.Speed else Icons.Rounded.SelfImprovement
