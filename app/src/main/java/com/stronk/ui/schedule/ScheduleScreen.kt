package com.stronk.ui.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stronk.data.ScheduleStatus
import com.stronk.ui.components.MuscleIcons
import com.stronk.ui.components.StronkBadge
import com.stronk.ui.components.StronkCard
import com.stronk.ui.components.StronkEmptyState
import com.stronk.ui.components.StronkFooterActions
import com.stronk.ui.components.StronkIconBadge
import com.stronk.ui.components.StronkIcons
import com.stronk.ui.components.StronkListRow
import com.stronk.ui.components.StronkMetaChip
import com.stronk.ui.components.StronkPrimaryButton
import com.stronk.ui.components.StronkScreenHeader
import com.stronk.ui.components.StronkSectionHeader
import com.stronk.ui.components.StronkTextAction
import com.stronk.ui.components.StronkTone
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTheme
import java.time.LocalDate

/**
 * Harmonogram — widok tygodnia (moduł 4 CONCEPT, mock "Ekran 2").
 * Siatka poniedziałek–niedziela z nawigacją tygodni, karta(-y) wybranego dnia
 * (lista ćwiczeń + CTA "Zacznij trening"), przesunięcie/odwołanie wpisu
 * oraz przypisanie planu do dni tygodnia (generacja wpisów PLANNED).
 * Przypomnienia-notyfikacje świadomie poza tym zakresem (backlog).
 *
 * @param onStartWorkout start treningu dnia planu; scheduleEntryId wpisu,
 *   żeby tryb treningu mógł go po zakończeniu oznaczyć jako DONE.
 * @param onPlanClick otwiera edytor planu (np. z karty dnia).
 * @param onNewPlan otwiera edytor nowego planu (empty state bez planów).
 * @param onExerciseClick podgląd ćwiczenia w bazie (wiersz w karcie dnia).
 */
@Composable
fun ScheduleScreen(
    onStartWorkout: (planId: String, dayIndex: Int, scheduleEntryId: String?) -> Unit,
    onPlanClick: (planId: String) -> Unit,
    onNewPlan: () -> Unit,
    onExerciseClick: (exerciseId: String) -> Unit,
    viewModel: ScheduleViewModel = viewModel(factory = ScheduleViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsState()
    var showAssignDialog by remember { mutableStateOf(false) }
    var moveEntryId by remember { mutableStateOf<String?>(null) }

    Scaffold { innerPadding ->
        if (state.loading) {
            Box(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = StronkSpacing.screen, vertical = StronkSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(StronkSpacing.section),
        ) {
            StronkScreenHeader(
                title = "Twój tydzień",
                subtitle = state.weekLabel.ifEmpty { null },
                actions = {
                    if (!state.isCurrentWeek) {
                        IconButton(onClick = viewModel::onBackToToday) {
                            Icon(
                                StronkIcons.today,
                                contentDescription = "Dziś",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (state.planOptions.isNotEmpty()) {
                        IconButton(onClick = { showAssignDialog = true }) {
                            Icon(
                                StronkIcons.add,
                                contentDescription = "Zaplanuj tydzień",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
            )

            WeekGrid(
                days = state.days,
                onPreviousWeek = viewModel::onPreviousWeek,
                onNextWeek = viewModel::onNextWeek,
                onSelectDay = viewModel::onSelectDay,
            )

            if (state.scheduleEmpty) {
                val hasPlans = state.planOptions.isNotEmpty()
                val onEmptyAction: () -> Unit = if (hasPlans) {
                    { showAssignDialog = true }
                } else {
                    onNewPlan
                }
                StronkEmptyState(
                    icon = StronkIcons.week,
                    title = "Pusty tydzień",
                    description = if (hasPlans) {
                        "Przypisz plan do dni tygodnia — wpisy na najbliższe tygodnie " +
                            "wygenerują się same."
                    } else {
                        "Najpierw złóż plan treningowy, potem przypiszesz go do dni tygodnia."
                    },
                    actionLabel = if (hasPlans) "Przypisz plan do tygodnia" else "Stwórz plan",
                    onAction = onEmptyAction,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(StronkSpacing.sm)) {
                    StronkSectionHeader(title = state.selectedDayLabel, icon = StronkIcons.today)
                    if (state.selectedEntries.isEmpty()) {
                        RestDayCard()
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(StronkSpacing.section)) {
                            state.selectedEntries.forEach { entry ->
                                EntryCard(
                                    entry = entry,
                                    onStartWorkout = onStartWorkout,
                                    onPlanClick = onPlanClick,
                                    onExerciseClick = onExerciseClick,
                                    onMove = { moveEntryId = it },
                                    onCancel = viewModel::onCancelEntry,
                                    onRestore = viewModel::onRestoreEntry,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAssignDialog) {
        AssignPlanDialog(
            plans = state.planOptions,
            onConfirm = { planId, assignments, startDate ->
                viewModel.onAssignPlan(planId, assignments, startDate)
                showAssignDialog = false
            },
            onDismiss = { showAssignDialog = false },
        )
    }

    moveEntryId?.let { entryId ->
        ScheduleDatePickerDialog(
            title = "Przesuń trening na…",
            initialDate = state.selectedDate,
            onConfirm = { newDate ->
                viewModel.onMoveEntry(entryId, newDate)
                moveEntryId = null
            },
            onDismiss = { moveEntryId = null },
        )
    }
}

/** Strzałki tygodnia + 7 komórek dni (jak week-grid z mocka). */
@Composable
private fun WeekGrid(
    days: List<ScheduleDayUi>,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onSelectDay: (LocalDate) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPreviousWeek) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = "Poprzedni tydzień",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(StronkSpacing.xxs),
        ) {
            days.forEach { day ->
                WeekDayCell(
                    day = day,
                    onClick = { onSelectDay(day.date) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        IconButton(onClick = onNextWeek) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "Następny tydzień",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WeekDayCell(
    day: ScheduleDayUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val highlighted = day.isToday || day.isSelected
    val container = when {
        day.isToday -> MaterialTheme.colorScheme.surfaceVariant
        day.badge == DayBadge.NONE -> MaterialTheme.colorScheme.background
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 82.dp),
        shape = MaterialTheme.shapes.small,
        color = container,
        border = BorderStroke(
            if (highlighted) 1.5.dp else 1.dp,
            if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = StronkSpacing.xs, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = day.abbrev,
                style = MaterialTheme.typography.labelSmall,
                color = if (day.isToday) MaterialTheme.colorScheme.primary else StronkTheme.colors.textDim,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "${day.dayOfMonth}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            DayCellStatus(day)
        }
    }
}

/** Dolna linijka komórki: status ikoną+kolorem, nazwa dnia planu pod spodem. */
@Composable
private fun DayCellStatus(day: ScheduleDayUi) {
    when (day.badge) {
        DayBadge.NONE -> Text(
            text = "–",
            style = MaterialTheme.typography.labelSmall,
            color = StronkTheme.colors.textDim,
        )

        DayBadge.PLANNED -> DayCellLabel(day.label, MaterialTheme.colorScheme.onSurfaceVariant)

        DayBadge.DONE -> {
            Icon(
                StronkIcons.done,
                contentDescription = null,
                tint = StronkTheme.colors.success,
                modifier = Modifier.size(12.dp),
            )
            DayCellLabel(day.label, StronkTheme.colors.textDim)
        }

        DayBadge.SKIPPED -> {
            Icon(
                StronkIcons.close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(12.dp),
            )
            DayCellLabel(day.label, StronkTheme.colors.textDim, decoration = TextDecoration.LineThrough)
        }

        DayBadge.MOVED -> {
            Icon(
                StronkIcons.swap,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(12.dp),
            )
            DayCellLabel(day.label, StronkTheme.colors.textDim)
        }
    }
}

@Composable
private fun DayCellLabel(
    text: String,
    color: Color,
    decoration: TextDecoration? = null,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        textDecoration = decoration,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Dzień bez wpisu — mały komunikat zamiast pustej karty. */
@Composable
private fun RestDayCard() {
    StronkCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StronkIconBadge(icon = StronkIcons.restDay)
            Text(
                text = "Dzień odpoczynku",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = StronkSpacing.sm),
            )
        }
    }
}

/** Karta jednego wpisu harmonogramu wybranego dnia (day-card z mocka). */
@Composable
private fun EntryCard(
    entry: ScheduleEntryUi,
    onStartWorkout: (planId: String, dayIndex: Int, scheduleEntryId: String?) -> Unit,
    onPlanClick: (planId: String) -> Unit,
    onExerciseClick: (exerciseId: String) -> Unit,
    onMove: (entryId: String) -> Unit,
    onCancel: (entryId: String) -> Unit,
    onRestore: (entryId: String) -> Unit,
) {
    StronkCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.dayName ?: "Plan usunięty",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (entry.dayName == null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                if (entry.planName != null) {
                    Text(
                        text = entry.planName,
                        style = MaterialTheme.typography.bodySmall,
                        color = StronkTheme.colors.textDim,
                        modifier = Modifier.clickable { onPlanClick(entry.planId) },
                    )
                }
            }
            EntryStatus(entry)
        }

        if (entry.exercises.isNotEmpty() && entry.status != ScheduleStatus.MOVED) {
            Spacer(Modifier.height(StronkSpacing.md))
            Column(verticalArrangement = Arrangement.spacedBy(StronkSpacing.row)) {
                entry.exercises.forEach { row ->
                    StronkListRow(
                        title = row.name,
                        icon = MuscleIcons.forMuscle(row.muscleKey),
                        iconLabel = MuscleIcons.groupLabel(row.muscleKey),
                        trailing = row.targetLabel,
                        inset = true,
                        onClick = { onExerciseClick(row.exerciseId) },
                    )
                }
            }
        }

        when (entry.status) {
            ScheduleStatus.PLANNED -> {
                if (entry.canStart) {
                    Spacer(Modifier.height(StronkSpacing.md))
                    StronkPrimaryButton(
                        text = "Zacznij trening",
                        icon = StronkIcons.start,
                        onClick = { onStartWorkout(entry.planId, entry.dayIndex, entry.entryId) },
                    )
                }
                Spacer(Modifier.height(StronkSpacing.xs))
                StronkFooterActions {
                    StronkTextAction(
                        text = "Przesuń",
                        icon = StronkIcons.swap,
                        onClick = { onMove(entry.entryId) },
                        modifier = Modifier.weight(1f),
                    )
                    StronkTextAction(
                        text = "Odwołaj",
                        icon = StronkIcons.close,
                        tone = StronkTone.DANGER,
                        onClick = { onCancel(entry.entryId) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            ScheduleStatus.SKIPPED -> {
                Spacer(Modifier.height(StronkSpacing.sm))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    StronkTextAction(
                        text = "Przywróć",
                        icon = StronkIcons.add,
                        onClick = { onRestore(entry.entryId) },
                    )
                }
            }

            ScheduleStatus.MOVED -> {
                if (entry.movedToLabel != null) {
                    Spacer(Modifier.height(StronkSpacing.xxs))
                    Text(
                        text = "Przeniesiony na ${entry.movedToLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = StronkTheme.colors.textDim,
                    )
                }
            }

            ScheduleStatus.DONE -> Unit // status widoczny w badge'u, bez akcji
        }
    }
}

/** Status wpisu jako badge (ikona + kolor semantyczny) zamiast samego tekstu. */
@Composable
private fun EntryStatus(entry: ScheduleEntryUi) {
    when (entry.status) {
        ScheduleStatus.DONE -> StronkBadge(
            text = "zaliczony",
            tone = StronkTone.SUCCESS,
            icon = StronkIcons.done,
        )

        ScheduleStatus.SKIPPED -> StronkBadge(
            text = "odwołany",
            tone = StronkTone.NEUTRAL,
            icon = StronkIcons.close,
        )

        ScheduleStatus.MOVED -> StronkBadge(
            text = "przesunięty",
            tone = StronkTone.NEUTRAL,
            icon = StronkIcons.swap,
        )

        ScheduleStatus.PLANNED -> StronkMetaChip("${entry.exercises.size} ćw.")
    }
}
