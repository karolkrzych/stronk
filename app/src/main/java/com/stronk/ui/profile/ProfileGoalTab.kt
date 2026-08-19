package com.stronk.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.stronk.data.GoalDefaults
import com.stronk.data.TrainingGoal
import com.stronk.progression.ProgressionConstants
import com.stronk.ui.components.StronkAccentCard
import com.stronk.ui.components.StronkCard
import com.stronk.ui.components.StronkSectionHeader
import com.stronk.ui.components.StronkStatBlock
import com.stronk.ui.components.StronkStatRow
import com.stronk.ui.components.StronkStatSize
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTextStyles
import com.stronk.ui.theme.StronkTheme
import kotlin.math.roundToInt

/** Odstęp między kartami celu — karty stoją gęściej niż sekcje ekranu. */
private val CardsGap = 10.dp

/**
 * Zakładka „Cel” — wybór celu treningu z liczbami, które ten wybór realnie
 * zmienia (powtórzenia, serie, przerwa), plus tryb powrotu po przerwie.
 *
 * Liczby są w stat-blokach z kapitalikowymi nagłówkami: żadnego sklejania
 * „8–12 powt. × 3 serie” w zdanie.
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
            .padding(top = StronkSpacing.xl, bottom = StronkSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(StronkSpacing.section),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(StronkSpacing.sm)) {
            StronkSectionHeader(title = "Cel treningu")
            Column(verticalArrangement = Arrangement.spacedBy(CardsGap)) {
                TrainingGoal.entries.forEach { option ->
                    GoalCard(
                        goal = option,
                        selected = goal == option,
                        onClick = { onGoalChange(option) },
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(StronkSpacing.sm)) {
            StronkSectionHeader(title = "Powrót po przerwie")
            ReturningFromBreakCard(returningFromBreak, onReturningFromBreakChange)
        }
    }
}

/**
 * Karta celu — nazwa, jedna linijka opisu i trzy staty z nagłówkami. Wybrany cel
 * dostaje limonkowy tint (jedyna akcentowana karta zakładki), więc widać go
 * z drugiego końca pokoju.
 */
@Composable
private fun GoalCard(goal: TrainingGoal, selected: Boolean, onClick: () -> Unit) {
    val params = GoalDefaults.forGoal(goal)
    val body: @Composable ColumnScope.() -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = GoalDefaults.label(goal),
                    style = StronkTextStyles.h1Small,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = GoalDefaults.description(goal),
                    style = StronkTextStyles.meta,
                    color = StronkTheme.colors.textDim,
                    modifier = Modifier.padding(top = 3.dp),
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
                    StronkTheme.colors.lime
                } else {
                    StronkTheme.colors.line
                },
                modifier = Modifier
                    .padding(start = StronkSpacing.sm)
                    .size(22.dp),
            )
        }

        val valueColor = if (selected) {
            StronkTheme.colors.lime
        } else {
            MaterialTheme.colorScheme.onSurface
        }
        StronkStatRow(Modifier.padding(top = StronkSpacing.md)) {
            StronkStatBlock(
                label = "Powtórzenia",
                value = GoalDefaults.repRangeLabel(goal),
                size = StronkStatSize.TITLE,
                valueColor = valueColor,
                weight = 1.15f,
            )
            StronkStatBlock(
                label = "Serie",
                value = params.defaultSets.toString(),
                size = StronkStatSize.TITLE,
                valueColor = valueColor,
                weight = 0.85f,
            )
            StronkStatBlock(
                label = "Przerwa",
                value = ProfileTexts.restValue(params.restSeconds),
                unit = ProfileTexts.restUnit(params.restSeconds),
                size = StronkStatSize.TITLE,
                valueColor = valueColor,
            )
        }
    }

    if (selected) {
        StronkAccentCard(modifier = Modifier.fillMaxWidth(), onClick = onClick, content = body)
    } else {
        StronkCard(modifier = Modifier.fillMaxWidth(), onClick = onClick, content = body)
    }
}

@Composable
private fun ReturningFromBreakCard(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val startPercent = (ProgressionConstants.RAMP_UP_START_FACTOR * 100).roundToInt()
    StronkCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(StronkSpacing.sm),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Wracam po przerwie",
                    style = StronkTextStyles.h2,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = ProfileTexts.returningFromBreakHint(startPercent),
                    style = StronkTextStyles.meta,
                    color = StronkTheme.colors.textDim,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = StronkTheme.colors.limeInk,
                    checkedTrackColor = StronkTheme.colors.lime,
                    checkedBorderColor = StronkTheme.colors.lime,
                    uncheckedThumbColor = StronkTheme.colors.textDim,
                    uncheckedTrackColor = Color.Transparent,
                    uncheckedBorderColor = StronkTheme.colors.line,
                ),
            )
        }
    }
}
