package com.stronk.ui.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stronk.data.ScheduleStatus
import com.stronk.ui.components.MuscleIcons
import com.stronk.ui.components.StronkBadge
import com.stronk.ui.components.StronkCard
import com.stronk.ui.components.StronkEmptyState
import com.stronk.ui.components.StronkIconBadge
import com.stronk.ui.components.StronkIcons
import com.stronk.ui.components.StronkListRow
import com.stronk.ui.components.StronkPrimaryButton
import com.stronk.ui.components.StronkScreenHeader
import com.stronk.ui.components.StronkTextAction
import com.stronk.ui.components.StronkTone
import com.stronk.ui.components.stronkDashedBorder
import com.stronk.ui.progress.ProgressFormat
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTheme
import java.time.LocalDate

/**
 * Harmonogram — widok tygodnia (moduł 4 CONCEPT, mock "Ekran 2").
 * Nagłówek + siatka dni są STAŁE, karta wybranego dnia wypełnia resztę
 * ekranu (przewijanie ćwiczeń jest wewnątrz niej), CTA "Zacznij trening"
 * jest przyklejone do dołu karty. Przypomnienia-notyfikacje świadomie
 * poza tym zakresem (backlog).
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
                .padding(horizontal = StronkSpacing.screen)
                .padding(top = StronkSpacing.sm, bottom = StronkSpacing.lg),
        ) {
            StronkScreenHeader(
                title = "Twój tydzień",
                subtitle = state.weekLabel.ifEmpty { null },
                meta = state.weekInBlock?.let { w -> state.blockLengthWeeks?.let { l -> "tydzień $w/$l" } },
                actions = {
                    WeekNavArrow(icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft, description = "Poprzedni tydzień", onClick = viewModel::onPreviousWeek)
                    WeekNavArrow(icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight, description = "Następny tydzień", onClick = viewModel::onNextWeek)
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

            Spacer(Modifier.height(StronkSpacing.md))

            WeekGrid(days = state.days, onSelectDay = viewModel::onSelectDay)

            Spacer(Modifier.height(StronkSpacing.md))

            if (state.scheduleEmpty) {
                val hasPlans = state.planOptions.isNotEmpty()
                val onEmptyAction: () -> Unit = if (hasPlans) {
                    { showAssignDialog = true }
                } else {
                    onNewPlan
                }
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
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
                }
            } else {
                DayCardArea(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    selectedDate = state.selectedDate,
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

/** Mała strzałka nawigacji tygodni w akcjach nagłówka (16dp ikony). */
@Composable
private fun WeekNavArrow(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}

/** Siatka 7 równych kolumn na pełną szerokość (jak week-grid z mocka), bez strzałek. */
@Composable
private fun WeekGrid(
    days: List<ScheduleDayUi>,
    onSelectDay: (LocalDate) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        days.forEach { day ->
            WeekDayCell(
                day = day,
                onClick = { onSelectDay(day.date) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private val dayCellShape = RoundedCornerShape(13.dp)
private val dayAbbrevStyle = TextStyle(fontSize = 9.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.55.sp)
private val dayNumberStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
private val dayLabelStyle = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold)
private val dayDoneMarkStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)

@Composable
private fun WeekDayCell(
    day: ScheduleDayUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (day.badge == DayBadge.NONE) {
        Surface(
            onClick = onClick,
            modifier = modifier.heightIn(min = 82.dp).stronkDashedBorder(
                color = MaterialTheme.colorScheme.outlineVariant,
                cornerRadius = 13.dp,
            ),
            shape = dayCellShape,
            color = Color.Transparent,
        ) {
            WeekDayCellContent(day)
        }
        return
    }

    val container = if (day.isToday) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val (borderWidth, borderColor) = when {
        day.isToday -> 1.5.dp to MaterialTheme.colorScheme.primary
        day.isSelected -> 1.dp to MaterialTheme.colorScheme.onSurfaceVariant
        else -> 1.dp to MaterialTheme.colorScheme.outlineVariant
    }
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 82.dp),
        shape = dayCellShape,
        color = container,
        border = BorderStroke(borderWidth, borderColor),
    ) {
        WeekDayCellContent(day)
    }
}

@Composable
private fun WeekDayCellContent(day: ScheduleDayUi) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 9.dp, bottom = 10.dp, start = 2.dp, end = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = day.abbrev,
            style = dayAbbrevStyle,
            color = if (day.isToday) MaterialTheme.colorScheme.primary else StronkTheme.colors.textDim,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = "${day.dayOfMonth}",
            style = dayNumberStyle,
            color = if (day.badge == DayBadge.NONE) StronkTheme.colors.textDim else MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.weight(1f))
        DayCellStatus(day)
    }
}

/** Dolna linijka komórki: status ikoną/znakiem, nazwa dnia planu pod spodem. */
@Composable
private fun DayCellStatus(day: ScheduleDayUi) {
    when (day.badge) {
        DayBadge.NONE -> Text(
            text = "–",
            style = dayLabelStyle.copy(fontWeight = FontWeight.SemiBold),
            color = StronkTheme.colors.textDim,
        )

        DayBadge.PLANNED -> DayCellLabel(day.label, MaterialTheme.colorScheme.onSurfaceVariant)

        DayBadge.DONE -> {
            Text(text = "✓", style = dayDoneMarkStyle, color = StronkTheme.colors.success)
            DayCellLabel(day.label, StronkTheme.colors.textDim)
        }

        DayBadge.SKIPPED -> {
            Icon(
                StronkIcons.close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(11.dp),
            )
            DayCellLabel(day.label, StronkTheme.colors.textDim, decoration = TextDecoration.LineThrough)
        }

        DayBadge.MOVED -> {
            Icon(
                StronkIcons.swap,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(11.dp),
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
        style = dayLabelStyle,
        color = color,
        textDecoration = decoration,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Karta wybranego dnia (day-card z mocka) — wypełnia resztę ekranu, CTA przyklejone do dołu. */
@Composable
private fun DayCardArea(
    modifier: Modifier,
    selectedDate: LocalDate,
    entries: List<ScheduleEntryUi>,
    onStartWorkout: (planId: String, dayIndex: Int, scheduleEntryId: String?) -> Unit,
    onPlanClick: (planId: String) -> Unit,
    onExerciseClick: (exerciseId: String) -> Unit,
    onMove: (entryId: String) -> Unit,
    onCancel: (entryId: String) -> Unit,
    onRestore: (entryId: String) -> Unit,
) {
    val isToday = selectedDate == LocalDate.now()
    val titlePrefix = if (isToday) {
        "Dziś"
    } else {
        "${ScheduleConstants.DAY_ABBREVIATIONS.getValue(selectedDate.dayOfWeek)} ${selectedDate.dayOfMonth}"
    }

    StronkCard(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        contentPadding = PaddingValues(18.dp),
    ) {
        when {
            entries.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                RestDayContent()
            }

            entries.size == 1 -> EntryCardBody(
                modifier = Modifier.weight(1f),
                fillHeight = true,
                entry = entries.first(),
                titlePrefix = titlePrefix,
                onStartWorkout = onStartWorkout,
                onPlanClick = onPlanClick,
                onExerciseClick = onExerciseClick,
                onMove = onMove,
                onCancel = onCancel,
                onRestore = onRestore,
            )

            else -> Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(StronkSpacing.section),
            ) {
                entries.forEach { entry ->
                    EntryCardBody(
                        modifier = Modifier,
                        fillHeight = false,
                        entry = entry,
                        titlePrefix = titlePrefix,
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

/** Dzień bez wpisu — mały komunikat zamiast pustej karty. */
@Composable
private fun RestDayContent() {
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

/** Treść karty jednego wpisu: głowa (dc-head), lista ćwiczeń i akcje statusu. */
@Composable
private fun EntryCardBody(
    modifier: Modifier,
    fillHeight: Boolean,
    entry: ScheduleEntryUi,
    titlePrefix: String,
    onStartWorkout: (planId: String, dayIndex: Int, scheduleEntryId: String?) -> Unit,
    onPlanClick: (planId: String) -> Unit,
    onExerciseClick: (exerciseId: String) -> Unit,
    onMove: (entryId: String) -> Unit,
    onCancel: (entryId: String) -> Unit,
    onRestore: (entryId: String) -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        val titleColor = if (entry.dayName == null) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurface
        }
        val dimColor = StronkTheme.colors.textDim

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = titleColor)) { append(titlePrefix) }
                    append(" ")
                    withStyle(SpanStyle(color = dimColor)) { append("·") }
                    append(" ")
                    withStyle(SpanStyle(color = titleColor)) {
                        append(entry.dayName ?: "Plan usunięty")
                    }
                },
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.2).sp),
                modifier = Modifier.weight(1f).let {
                    if (entry.planName != null) it.clickable { onPlanClick(entry.planId) } else it
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (entry.status == ScheduleStatus.PLANNED) {
                Text(
                    text = "~${entry.estimatedMinutes} min · ${ProgressFormat.exercisesCount(entry.exercises.size)}",
                    style = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
                    color = dimColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                EntryStatus(entry)
            }
        }

        if (entry.exercises.isNotEmpty() && entry.status != ScheduleStatus.MOVED) {
            Spacer(Modifier.height(14.dp))
            val listModifier = if (fillHeight) {
                Modifier.weight(1f).verticalScroll(rememberScrollState())
            } else {
                Modifier
            }
            Column(
                modifier = listModifier,
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
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
        } else if (fillHeight) {
            Spacer(Modifier.weight(1f))
        }

        when (entry.status) {
            ScheduleStatus.PLANNED -> {
                if (entry.canStart) {
                    Spacer(Modifier.height(StronkSpacing.md))
                    StronkPrimaryButton(
                        text = "Zacznij trening",
                        onClick = { onStartWorkout(entry.planId, entry.dayIndex, entry.entryId) },
                    )
                }
                Spacer(Modifier.height(12.dp))
                SecondaryActionsRow(
                    onMove = { onMove(entry.entryId) },
                    onCancel = { onCancel(entry.entryId) },
                )
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
                        color = dimColor,
                    )
                }
            }

            ScheduleStatus.DONE -> Unit // status widoczny w badge'u, bez akcji
        }
    }
}

/** Wiersz "przesuń · odwołaj" wyśrodkowany pod CTA (mock: `.sec-actions`), bez ikon, bez czerwieni. */
@Composable
private fun SecondaryActionsRow(
    onMove: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UnderlinedTextAction(text = "przesuń", onClick = onMove)
            Text(
                text = "·",
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                color = StronkTheme.colors.textDim,
            )
            UnderlinedTextAction(text = "odwołaj", onClick = onCancel)
        }
    }
}

@Composable
private fun UnderlinedTextAction(text: String, onClick: () -> Unit) {
    val lineColor = MaterialTheme.colorScheme.outline
    StronkTextAction(
        text = text,
        onClick = onClick,
        tone = StronkTone.NEUTRAL,
        modifier = Modifier.drawBehind {
            val strokeWidth = 1.dp.toPx()
            val y = size.height - strokeWidth
            drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth)
        },
    )
}

/** Status wpisu jako badge (ikona + kolor semantyczny) — wyłącznie DONE/SKIPPED/MOVED. */
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

        // PLANNED nie trafia tutaj — dc-head pokazuje wtedy szacowany czas (patrz EntryCardBody).
        ScheduleStatus.PLANNED -> Unit
    }
}
