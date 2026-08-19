package com.stronk.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Rowing
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.SportsHandball
import androidx.compose.material.icons.rounded.SportsMartialArts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stronk.data.StressLevel
import com.stronk.ui.components.StronkEmptyState
import com.stronk.ui.components.StronkGhostButton
import com.stronk.ui.components.StronkIconBadge
import com.stronk.ui.components.StronkIcons
import com.stronk.ui.components.StronkListRow
import com.stronk.ui.components.StronkSectionHeader
import com.stronk.ui.components.StronkTextAction
import com.stronk.ui.components.StronkTone
import com.stronk.ui.theme.StronkRadius
import com.stronk.ui.theme.StronkSizes
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTextStyles
import com.stronk.ui.theme.StronkTheme
import kotlinx.coroutines.launch

/** Wymiary wiersza ograniczenia — 1:1 z `.limit` w mocku `pack-historia-profil.html`. */
private val RowPaddingHorizontal = 16.dp
private val RowPaddingVertical = 14.dp
private val RowGap = 13.dp
private val RowsGap = 10.dp
private val DotSize = 8.dp
private val DotGap = 5.dp

/** Kapitalik „LIMIT” stoi nad kropkami, nie nad krzyżykiem (mock: `margin-right:32px`). */
private val LimitCapInset = 32.dp
private val ActionsBottomGap = 26.dp

/** Krok arkusza dodawania/edycji ograniczenia; null = arkusz zamknięty. */
private sealed interface ConstraintSheet {
    /** Wybór stawu spośród jeszcze niedodanych. */
    data object PickJoint : ConstraintSheet

    /** Wybór limitu; [current] null = wpis dopiero powstaje. */
    data class PickSeverity(val joint: String, val current: StressLevel?) : ConstraintSheet
}

/**
 * Zakładka „Kontuzje” (mock: ekran 2) — lista tego, co realnie oszczędzamy:
 * ikona stawu, nazwa, limit jako kropki i dyskretny krzyżyk. Stawy bez
 * ograniczenia nie zajmują miejsca: dochodzą przez „Dodaj ograniczenie”.
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

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = StronkSpacing.screen),
        ) {
            if (added.isEmpty()) {
                StronkEmptyState(
                    icon = StronkIcons.injury,
                    title = "Nic nie oszczędzamy",
                    description = "Dodaj miejsce po kontuzji — podmienimy ryzykowne ćwiczenia.",
                )
            } else {
                StronkSectionHeader(
                    title = "Oszczędzamy",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = StronkSpacing.xl),
                    trailing = {
                        Text(
                            text = "Limit".uppercase(),
                            style = StronkTextStyles.cap,
                            color = StronkTheme.colors.textDim,
                            modifier = Modifier.padding(end = LimitCapInset),
                        )
                    },
                )
                Column(
                    modifier = Modifier.padding(top = StronkSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(RowsGap),
                ) {
                    added.forEach { joint ->
                        val level = constraints.getValue(joint)
                        ConstraintRow(
                            joint = joint,
                            level = level,
                            onClick = { sheet = ConstraintSheet.PickSeverity(joint, level) },
                            onRemove = { onConstraintChange(joint, null) },
                        )
                    }
                }
            }
        }

        StronkGhostButton(
            text = "Dodaj ograniczenie",
            onClick = { sheet = ConstraintSheet.PickJoint },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = StronkSpacing.screen)
                .padding(bottom = ActionsBottomGap),
            icon = StronkIcons.add,
            enabled = available.isNotEmpty(),
            height = StronkSizes.ctaSmall,
        )
    }

    val current = sheet
    if (current != null) {
        ModalBottomSheet(
            onDismissRequest = { sheet = null },
            sheetState = sheetState,
            containerColor = StronkTheme.colors.surfaceCard,
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
                        SheetTitle("Co oszczędzamy?")
                        Column {
                            available.forEachIndexed { index, joint ->
                                StronkListRow(
                                    title = ProfileTexts.jointTitle(joint),
                                    icon = jointIcon(joint),
                                    chevron = true,
                                    divider = index != available.lastIndex,
                                    onClick = {
                                        sheet = ConstraintSheet.PickSeverity(joint, null)
                                    },
                                )
                            }
                        }
                    }

                    is ConstraintSheet.PickSeverity -> {
                        SheetTitle(ProfileTexts.jointTitle(current.joint))
                        Column(verticalArrangement = Arrangement.spacedBy(RowsGap)) {
                            ProfileTexts.SEVERITY_OPTIONS.forEach { level ->
                                SeverityOption(
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

/**
 * Wiersz ograniczenia (mock: `.limit`) — kafelek ikony, nazwa stawu, limit jako
 * kropki i krzyżyk. Kliknięcie wiersza zmienia limit, krzyżyk usuwa ograniczenie.
 */
@Composable
private fun ConstraintRow(
    joint: String,
    level: StressLevel,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = StronkRadius.innerShape,
        color = StronkTheme.colors.surfaceCard,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = RowPaddingHorizontal,
                vertical = RowPaddingVertical,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RowGap),
        ) {
            StronkIconBadge(icon = jointIcon(joint), tone = StronkTone.NEUTRAL)
            Text(
                text = ProfileTexts.jointTitle(joint),
                style = StronkTextStyles.h2,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            SeverityDots(
                level = level,
                modifier = Modifier.semantics {
                    contentDescription = ProfileTexts.severityRowText(level)
                },
            )
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = StronkIcons.close,
                    contentDescription = "Usuń ograniczenie",
                    tint = StronkTheme.colors.textDim,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}

/**
 * Limit jako miarka kropek (mock: `.dotmeter`) — im więcej zapalonych, tym
 * mocniej oszczędzamy staw. Kropki są faktem z przeszłości/stanem, nie akcją,
 * więc świecą przygaszoną limonką.
 */
@Composable
private fun SeverityDots(level: StressLevel, modifier: Modifier = Modifier) {
    val lit = ProfileTexts.severityDots(level)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(DotGap),
    ) {
        repeat(ProfileTexts.SEVERITY_DOTS) { index ->
            Box(
                modifier = Modifier
                    .size(DotSize)
                    .background(
                        color = if (index < lit) {
                            StronkTheme.colors.limeDeep
                        } else {
                            StronkTheme.colors.surfaceMuted
                        },
                        shape = CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun SheetTitle(title: String) {
    Text(
        text = title,
        style = StronkTextStyles.h1Small,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = StronkSpacing.xs),
    )
}

/** Opcja limitu w arkuszu — nazwa, jedno zdanie i ta sama miarka kropek co w liście. */
@Composable
private fun SeverityOption(level: StressLevel, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = StronkRadius.innerShape,
        color = if (selected) StronkTheme.colors.limeDim else StronkTheme.colors.surfaceTile,
        border = if (selected) BorderStroke(1.dp, StronkTheme.colors.limeLine) else null,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = RowPaddingHorizontal,
                vertical = RowPaddingVertical,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RowGap),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = ProfileTexts.severityTitle(level),
                    style = StronkTextStyles.h2,
                    color = if (selected) {
                        StronkTheme.colors.lime
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                Text(
                    text = ProfileTexts.severityDescription(level),
                    style = StronkTextStyles.meta,
                    color = StronkTheme.colors.textDim,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            SeverityDots(level)
        }
    }
}

/**
 * Piktogram stawu do kafelka (mock ma własne SVG kręgosłupa/kolana/barku) —
 * najbliższe odpowiedniki z material-icons-extended Rounded.
 */
private fun jointIcon(joint: String): ImageVector = when (joint) {
    "lowBack" -> Icons.Rounded.Rowing
    "knee" -> Icons.AutoMirrored.Rounded.DirectionsRun
    "shoulder" -> Icons.Rounded.SportsHandball
    "hip" -> Icons.Rounded.SelfImprovement
    "elbow" -> Icons.Rounded.FitnessCenter
    "wrist" -> Icons.Rounded.SportsMartialArts
    "neck" -> Icons.Rounded.Accessibility
    else -> StronkIcons.injury
}
