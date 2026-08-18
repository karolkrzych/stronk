package com.stronk.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.stronk.data.GoalDefaults
import com.stronk.data.TrainingGoal
import com.stronk.progression.ProgressionConstants
import com.stronk.ui.components.StronkCard
import com.stronk.ui.components.StronkIconBadge
import com.stronk.ui.components.StronkIcons
import com.stronk.ui.components.StronkMetaChip
import com.stronk.ui.components.StronkSectionHeader
import com.stronk.ui.components.StronkStat
import com.stronk.ui.components.StronkStatRow
import com.stronk.ui.components.StronkTone
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTheme
import kotlin.math.roundToInt

/**
 * Zakładka „Cel” — wybór celu treningu z liczbami, które ten wybór realnie
 * zmienia (zakres powtórzeń, serie, przerwa), plus tryb powrotu po przerwie.
 */
@Composable
internal fun ProfileGoalTab(
    goal: TrainingGoal?,
    returningFromBreak: Boolean,
    onGoalChange: (TrainingGoal) -> Unit,
    onReturningFromBreakChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = StronkSpacing.screen)
            .padding(top = StronkSpacing.lg, bottom = StronkSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(StronkSpacing.section),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(StronkSpacing.sm)) {
            StronkSectionHeader(title = "Cel treningu", icon = StronkIcons.record)
            TrainingGoal.entries.forEach { option ->
                GoalCard(
                    goal = option,
                    selected = goal == option,
                    onClick = { onGoalChange(option) },
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(StronkSpacing.sm)) {
            StronkSectionHeader(title = "Powrót po przerwie", icon = StronkIcons.restDay)
            ReturningFromBreakCard(returningFromBreak, onReturningFromBreakChange)
        }
    }
}

/**
 * Karta celu: wybrany pokazuje parametry jako duże liczby (focal point zakładki),
 * pozostałe te same wartości jako chipy — wybór jest namacalny, a nie deklaratywny.
 */
@Composable
private fun GoalCard(goal: TrainingGoal, selected: Boolean, onClick: () -> Unit) {
    val params = GoalDefaults.forGoal(goal)
    StronkCard(onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(StronkSpacing.sm),
        ) {
            StronkIconBadge(
                icon = goalIcon(goal),
                tone = if (selected) StronkTone.ACCENT else StronkTone.NEUTRAL,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = GoalDefaults.label(goal),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = GoalDefaults.description(goal),
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
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
                modifier = Modifier.size(22.dp),
            )
        }

        if (selected) {
            StronkStatRow(Modifier.padding(top = StronkSpacing.md)) {
                StronkStat(
                    label = "powt.",
                    value = GoalDefaults.repRangeLabel(goal),
                    modifier = Modifier.weight(1.2f),
                )
                StronkStat(
                    label = "serie",
                    value = params.defaultSets.toString(),
                    modifier = Modifier.weight(0.8f),
                )
                StronkStat(
                    label = "przerwa",
                    value = ProfileTexts.restValue(params.restSeconds),
                    modifier = Modifier.weight(1f),
                    unit = ProfileTexts.restUnit(params.restSeconds),
                )
            }
        } else {
            FlowRow(
                modifier = Modifier.padding(top = StronkSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(StronkSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(StronkSpacing.xs),
            ) {
                StronkMetaChip("${GoalDefaults.repRangeLabel(goal)} powt.")
                StronkMetaChip(ProfileTexts.setsChip(params.defaultSets))
                StronkMetaChip(ProfileTexts.restChip(params.restSeconds))
            }
        }
    }
}

@Composable
private fun ReturningFromBreakCard(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val startPercent = (ProgressionConstants.RAMP_UP_START_FACTOR * 100).roundToInt()
    StronkCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(StronkSpacing.sm),
        ) {
            StronkIconBadge(
                icon = StronkIcons.injury,
                tone = if (checked) StronkTone.ACCENT else StronkTone.NEUTRAL,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Wracam po przerwie",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Start od ok. $startPercent% docelowych ciężarów, " +
                        "potem szybsza progresja, aż dogonisz plan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StronkTheme.colors.textDim,
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

private fun goalIcon(goal: TrainingGoal): ImageVector = when (goal) {
    TrainingGoal.STRENGTH -> Icons.Rounded.Bolt
    TrainingGoal.MASS -> Icons.Rounded.FitnessCenter
    TrainingGoal.RETURN_TO_FORM -> Icons.Rounded.SelfImprovement
}
