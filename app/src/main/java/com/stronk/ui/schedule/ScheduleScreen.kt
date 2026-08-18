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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stronk.data.ScheduleStatus
import java.time.LocalDate

/**
 * Harmonogram — widok tygodnia (moduł 4 CONCEPT, mock "Ekran 2").
 * Siatka poniedziałek–niedziela z nawigacją tygodni, karta wybranego dnia
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
@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Twój tydzień")
                        if (state.weekLabel.isNotEmpty()) {
                            Text(
                                text = state.weekLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    if (!state.isCurrentWeek) {
                        TextButton(onClick = viewModel::onBackToToday) { Text("Dziś") }
                    }
                    if (state.planOptions.isNotEmpty()) {
                        TextButton(onClick = { showAssignDialog = true }) { Text("Zaplanuj") }
                    }
                },
            )
        },
    ) { innerPadding ->
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            WeekGrid(
                days = state.days,
                onPreviousWeek = viewModel::onPreviousWeek,
                onNextWeek = viewModel::onNextWeek,
                onSelectDay = viewModel::onSelectDay,
            )
            Spacer(Modifier.height(16.dp))
            if (state.scheduleEmpty) {
                EmptyScheduleCard(
                    hasPlans = state.planOptions.isNotEmpty(),
                    onAssignPlan = { showAssignDialog = true },
                    onNewPlan = onNewPlan,
                )
            } else {
                SelectedDayCard(
                    dayLabel = state.selectedDayLabel,
                    entries = state.selectedEntries,
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
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Poprzedni tydzień",
            )
        }
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            days.forEach { day ->
                DayCell(
                    day = day,
                    onClick = { onSelectDay(day.date) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        IconButton(onClick = onNextWeek) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Następny tydzień",
            )
        }
    }
}

@Composable
private fun DayCell(
    day: ScheduleDayUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = when {
        day.isToday -> MaterialTheme.colorScheme.secondaryContainer
        day.badge == DayBadge.NONE -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (day.isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .padding(vertical = 8.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = day.abbrev,
                style = MaterialTheme.typography.labelSmall,
                color = if (day.isToday) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Text(
                text = "${day.dayOfMonth}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            DayCellBadge(day)
        }
    }
}

/** Dolna linijka komórki: nazwa dnia planu w stylu zależnym od statusu. */
@Composable
private fun DayCellBadge(day: ScheduleDayUi) {
    when (day.badge) {
        DayBadge.NONE -> Text(
            text = "—",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )

        DayBadge.PLANNED -> DayCellLabel(day.label, MaterialTheme.colorScheme.onSurface)

        DayBadge.DONE -> {
            Text(
                text = "✓",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            DayCellLabel(day.label, MaterialTheme.colorScheme.onSurfaceVariant)
        }

        DayBadge.SKIPPED -> DayCellLabel(
            text = day.label,
            color = MaterialTheme.colorScheme.outline,
            decoration = TextDecoration.LineThrough,
        )

        DayBadge.MOVED -> DayCellLabel(
            text = "→ ${day.label}",
            color = MaterialTheme.colorScheme.outline,
        )
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
        fontWeight = FontWeight.SemiBold,
        color = color,
        textDecoration = decoration,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
    )
}

/** Karta wybranego dnia: wpisy albo "dzień odpoczynku" (jak day-card z mocka). */
@Composable
private fun SelectedDayCard(
    dayLabel: String,
    entries: List<ScheduleEntryUi>,
    onStartWorkout: (planId: String, dayIndex: Int, scheduleEntryId: String?) -> Unit,
    onPlanClick: (planId: String) -> Unit,
    onExerciseClick: (exerciseId: String) -> Unit,
    onMove: (entryId: String) -> Unit,
    onCancel: (entryId: String) -> Unit,
    onRestore: (entryId: String) -> Unit,
) {
    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = dayLabel,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            if (entries.isEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Dzień odpoczynku",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                entries.forEachIndexed { index, entry ->
                    if (index > 0) {
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    Spacer(Modifier.height(12.dp))
                    EntrySection(
                        entry = entry,
                        onStartWorkout = onStartWorkout,
                        onPlanClick = onPlanClick,
                        onExerciseClick = onExerciseClick,
                        onMove = onMove,
                        onCancel = onCancel,
                        onRestore = onRestore,
                    )
                }
            }
        }
    }
}

@Composable
private fun EntrySection(
    entry: ScheduleEntryUi,
    onStartWorkout: (planId: String, dayIndex: Int, scheduleEntryId: String?) -> Unit,
    onPlanClick: (planId: String) -> Unit,
    onExerciseClick: (exerciseId: String) -> Unit,
    onMove: (entryId: String) -> Unit,
    onCancel: (entryId: String) -> Unit,
    onRestore: (entryId: String) -> Unit,
) {
    // Nagłówek wpisu: nazwa dnia planu + plan (tap = edytor planu).
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.dayName ?: "Plan usunięty",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { onPlanClick(entry.planId) },
                )
            }
        }
        EntryStatusLabel(entry)
    }

    if (entry.exercises.isNotEmpty() && entry.status != ScheduleStatus.MOVED) {
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        entry.exercises.forEach { row ->
            ExerciseRow(row = row, onClick = { onExerciseClick(row.exerciseId) })
        }
    }

    when (entry.status) {
        ScheduleStatus.PLANNED -> {
            Spacer(Modifier.height(12.dp))
            if (entry.canStart) {
                Button(
                    onClick = { onStartWorkout(entry.planId, entry.dayIndex, entry.entryId) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("Zacznij trening", style = MaterialTheme.typography.titleMedium)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                TextButton(onClick = { onMove(entry.entryId) }) { Text("Przesuń") }
                TextButton(onClick = { onCancel(entry.entryId) }) { Text("Odwołaj") }
            }
        }

        ScheduleStatus.SKIPPED -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                TextButton(onClick = { onRestore(entry.entryId) }) { Text("Przywróć") }
            }
        }

        ScheduleStatus.MOVED -> {
            if (entry.movedToLabel != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Przeniesiony na ${entry.movedToLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        ScheduleStatus.DONE -> Unit // status widoczny w etykiecie, bez akcji
    }
}

@Composable
private fun EntryStatusLabel(entry: ScheduleEntryUi) {
    when (entry.status) {
        ScheduleStatus.DONE -> Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "zaliczony",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        ScheduleStatus.SKIPPED -> Text(
            text = "odwołany",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
            textDecoration = TextDecoration.LineThrough,
        )

        ScheduleStatus.MOVED -> Text(
            text = "przesunięty",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
        )

        ScheduleStatus.PLANNED -> Text(
            text = "${entry.exercises.size} ćw.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ExerciseRow(row: ScheduleExerciseRow, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            if (row.muscleLabel.isNotEmpty()) {
                Text(
                    text = row.muscleLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = row.targetLabel,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Pusty stan harmonogramu — zachęta do przypisania planu do tygodnia. */
@Composable
private fun EmptyScheduleCard(
    hasPlans: Boolean,
    onAssignPlan: () -> Unit,
    onNewPlan: () -> Unit,
) {
    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Pusty tydzień",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (hasPlans) {
                    "Przypisz plan do dni tygodnia — wpisy na najbliższe tygodnie " +
                        "wygenerują się same."
                } else {
                    "Najpierw złóż plan treningowy, potem przypiszesz go do dni tygodnia."
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = if (hasPlans) onAssignPlan else onNewPlan,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = if (hasPlans) "Przypisz plan do tygodnia" else "Stwórz plan",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}
