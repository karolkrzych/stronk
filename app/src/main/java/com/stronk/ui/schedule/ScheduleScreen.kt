package com.stronk.ui.schedule

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stronk.data.ScheduleStatus
import com.stronk.ui.cardio.CardioSection
import com.stronk.ui.cardio.CardioTexts
import com.stronk.ui.components.MuscleIcons
import com.stronk.ui.components.StronkBadge
import com.stronk.ui.components.StronkCard
import com.stronk.ui.components.StronkDayLegend
import com.stronk.ui.components.StronkDaySquare
import com.stronk.ui.components.StronkDayState
import com.stronk.ui.components.StronkEmptyState
import com.stronk.ui.components.StronkIconBadge
import com.stronk.ui.components.StronkIcons
import com.stronk.ui.components.StronkListRow
import com.stronk.ui.components.StronkNoteCard
import com.stronk.ui.components.StronkPrimaryButton
import com.stronk.ui.components.StronkScreenHeader
import com.stronk.ui.components.StronkTone
import com.stronk.ui.components.StronkWeekdayHeader
import com.stronk.ui.theme.StronkSizes
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTextStyles
import com.stronk.ui.theme.StronkTheme
import java.time.LocalDate

/** Odstęp między kwadratami siatki (mock: `.cal .grid { gap: 8px }`). */
private val DaySquareGap = 8.dp

/** Nagłówek → siatka i siatka → karta dnia (mock: `.cal`/`.daycard` margin-top 18). */
private val BlockGap = 18.dp

/** Siatka → legenda (mock: `.legend { margin-top: 14px }`). */
private val LegendGap = 14.dp

/** Wewnętrzny padding karty dnia (mock: `.daycard { padding: 18px }`). */
private val DayCardPadding = 18.dp

/**
 * Harmonogram — ekran „Tydzień" (mock `wariant-c2-limonka.html`, ekran 2).
 *
 * Dominanta ekranu to SIATKA KWADRATÓW: 7 kolumn × tygodnie bieżącego bloku
 * w dół, numer dnia w kwadracie, stan niesiony wypełnieniem (zrobione), obrysem
 * (plan) albo brakiem obu (dzień wolny); „dziś" to limonkowy ring. Legenda ma
 * maks 2 pozycje — trzecia byłaby znakiem, że siatka przestała być czytelna.
 *
 * Pod siatką jest jedna karta wybranego dnia: „Środa · Full body B", CTA
 * „Zacznij trening" i prosta lista ćwiczeń (ikona + nazwa + chip „3 serie").
 * Akcje drugorzędne (przesuń/odwołaj) to podkreślone linki, nie przyciski.
 * Dolna nawigacja żyje w [com.stronk.ui.StronkNavHost] — ten ekran jej nie rysuje.
 *
 * @param onStartWorkout start treningu dnia planu; scheduleEntryId wpisu,
 *   żeby tryb treningu mógł go po zakończeniu oznaczyć jako DONE.
 * @param onPlanClick otwiera edytor planu (nazwa planu w karcie dnia).
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
    val assignmentMessage by viewModel.assignmentMessage.collectAsState()
    var showAssignDialog by remember { mutableStateOf(false) }
    var moveEntryId by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    // Komunikat po nieudanej próbie przypisania planu (np. „ten okres jest już
    // zaplanowany") — StronkNoteCard w slocie snackbara, nie systemowy Toast.
    LaunchedEffect(assignmentMessage) {
        assignmentMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onAssignmentMessageShown()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                StronkNoteCard(
                    text = data.visuals.message,
                    icon = StronkIcons.info,
                    modifier = Modifier.padding(StronkSpacing.screen),
                )
            }
        },
    ) { innerPadding ->
        if (state.loading) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = StronkSpacing.screen)
                .padding(top = StronkSpacing.sm, bottom = StronkSpacing.lg),
        ) {
            StronkScreenHeader(
                title = state.blockLabel.ifEmpty { "Tydzień" },
                subtitle = state.monthLabel.ifEmpty { null },
                actions = {
                    if (!state.todaySelected) {
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
                                tint = StronkTheme.colors.lime,
                            )
                        }
                    }
                },
            )

            Spacer(Modifier.height(BlockGap))
            BlockCalendar(weeks = state.weeks, onSelectDay = viewModel::onSelectDay)

            Spacer(Modifier.height(LegendGap))
            // Trzecia pozycja legendy pojawia się tylko wtedy, gdy w siatce
            // faktycznie jest cardio — inaczej legenda puchnie bez powodu.
            val anyCardio = state.weeks.any { week -> week.days.any { it.hasCardio } }
            StronkDayLegend(cardioLabel = if (anyCardio) CardioTexts.SECTION_CARDIO else null)

            Spacer(Modifier.height(BlockGap))
            if (state.scheduleEmpty) {
                EmptySchedule(
                    hasPlans = state.planOptions.isNotEmpty(),
                    onAssign = { showAssignDialog = true },
                    onNewPlan = onNewPlan,
                )
            } else if (state.selectedEntries.isEmpty()) {
                FreeDayCard(state.selectedDayLabel)
            } else {
                state.selectedEntries.forEachIndexed { index, entry ->
                    if (index > 0) Spacer(Modifier.height(StronkSpacing.sm))
                    DayCard(
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

            // Cardio wybranego dnia — także w dniach przeszłych i przyszłych.
            // Tydzień pokazuje fakty; wpisuje się je w „Dziś", więc tu wiersze
            // są bez ghost-wiersza i bez edycji.
            if (state.selectedCardio.isNotEmpty()) {
                StronkCard(
                    modifier = Modifier.padding(top = StronkSpacing.sm),
                    contentPadding = PaddingValues(DayCardPadding),
                ) {
                    CardioSection(rows = state.selectedCardio)
                }
            }
        }
    }

    if (showAssignDialog) {
        AssignPlanDialog(
            plans = state.planOptions,
            occupiedEntries = state.occupiedEntries,
            onConfirm = { planId, assignments, startDate ->
                viewModel.onAssignPlan(planId, assignments, startDate)
                showAssignDialog = false
            },
            onDismiss = { showAssignDialog = false },
        )
    }

    moveEntryId?.let { entryId ->
        ScheduleDatePickerDialog(
            title = "Przesuń trening",
            initialDate = state.selectedDate,
            onConfirm = { newDate ->
                viewModel.onMoveEntry(entryId, newDate)
                moveEntryId = null
            },
            onDismiss = { moveEntryId = null },
        )
    }
}

/**
 * Siatka kwadratów bloku (mock: `.cal`) — nagłówek liter dni + rzędy tygodni.
 * Ring „dziś" rysuje się POZA kwadratem, więc odstęp 8 dp musi zostać.
 */
@Composable
private fun BlockCalendar(
    weeks: List<ScheduleWeekUi>,
    onSelectDay: (LocalDate) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(DaySquareGap)) {
        StronkWeekdayHeader(modifier = Modifier.padding(bottom = 2.dp))
        weeks.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DaySquareGap),
            ) {
                week.days.forEach { day ->
                    val marker = CalendarMarkers.marker(day.status, day.hasCardio)
                    StronkDaySquare(
                        day = day.dayOfMonth.toString(),
                        state = marker.toSquareState(),
                        // Ring = punkt odniesienia: dziś (mock) i dzień, którego
                        // kartę widać pod siatką. Domyślnie to ten sam kwadrat.
                        today = day.isToday || day.isSelected,
                        cardio = marker == DayMarker.CARDIO || marker == DayMarker.DONE_WITH_CARDIO,
                        onClick = { onSelectDay(day.date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * Znacznik → wygląd kwadratu. Zaplanowany dzień w przeszłości bez zaliczenia
 * rysuje się jak plan (pusty obrys w przeszłości mówi sam za siebie), a cardio
 * niesie własny obrys — stąd stan bazowy [StronkDayState.OFF] plus flaga
 * `cardio` w [StronkDaySquare].
 */
private fun DayMarker.toSquareState(): StronkDayState = when (this) {
    DayMarker.DONE, DayMarker.DONE_WITH_CARDIO -> StronkDayState.DONE
    DayMarker.PLANNED -> StronkDayState.PLANNED
    DayMarker.CARDIO, DayMarker.FREE -> StronkDayState.OFF
}

/** Karta wybranego dnia (mock: `.daycard`). */
@Composable
private fun DayCard(
    entry: ScheduleEntryUi,
    onStartWorkout: (planId: String, dayIndex: Int, scheduleEntryId: String?) -> Unit,
    onPlanClick: (planId: String) -> Unit,
    onExerciseClick: (exerciseId: String) -> Unit,
    onMove: (entryId: String) -> Unit,
    onCancel: (entryId: String) -> Unit,
    onRestore: (entryId: String) -> Unit,
) {
    StronkCard(contentPadding = PaddingValues(DayCardPadding)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    style = StronkTextStyles.h1Small,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (entry.planMissing) {
                    Text(
                        text = "Plan usunięty",
                        style = StronkTextStyles.meta,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = StronkSpacing.xxs),
                    )
                } else if (entry.planName != null) {
                    Text(
                        text = entry.planName,
                        style = StronkTextStyles.meta,
                        color = StronkTheme.colors.textDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(top = StronkSpacing.xxs)
                            .clickable { onPlanClick(entry.planId) },
                    )
                }
            }
            EntryStatusBadge(entry.status)
        }

        if (entry.canStart) {
            Spacer(Modifier.height(StronkSpacing.lg))
            StronkPrimaryButton(
                text = "Zacznij trening",
                icon = StronkIcons.start,
                height = StronkSizes.ctaSmall,
                onClick = { onStartWorkout(entry.planId, entry.dayIndex, entry.entryId) },
            )
        }

        if (entry.status == ScheduleStatus.MOVED && entry.movedToLabel != null) {
            Spacer(Modifier.height(StronkSpacing.sm))
            Text(
                text = "Przeniesiony na ${entry.movedToLabel}",
                style = StronkTextStyles.meta,
                color = StronkTheme.colors.textDim,
            )
        }

        if (entry.exercises.isNotEmpty() && entry.status != ScheduleStatus.MOVED) {
            Spacer(Modifier.height(6.dp))
            entry.exercises.forEachIndexed { index, row ->
                StronkListRow(
                    title = row.name,
                    icon = MuscleIcons.forMuscle(row.muscleKey),
                    trailing = row.setsLabel,
                    divider = index < entry.exercises.lastIndex,
                    onClick = { onExerciseClick(row.exerciseId) },
                )
            }
        }

        when (entry.status) {
            ScheduleStatus.PLANNED -> {
                Spacer(Modifier.height(BlockGap))
                DayCardLinks {
                    ScheduleLinkAction(text = "Przesuń", onClick = { onMove(entry.entryId) })
                    ScheduleLinkAction(text = "Odwołaj", onClick = { onCancel(entry.entryId) })
                }
            }

            ScheduleStatus.SKIPPED -> {
                Spacer(Modifier.height(BlockGap))
                DayCardLinks {
                    ScheduleLinkAction(text = "Przywróć", onClick = { onRestore(entry.entryId) })
                }
            }

            ScheduleStatus.DONE, ScheduleStatus.MOVED -> Unit
        }
    }
}

/** Rząd podkreślonych linków pod kartą dnia (mock: `.skip u`). */
@Composable
private fun DayCardLinks(content: @Composable () -> Unit) {
    val centered = Arrangement.spacedBy(StronkSpacing.md, Alignment.CenterHorizontally)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = centered,
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

/**
 * Akcja drugorzędna jako PODKREŚLONY LINK (mock: `.skip u`, `.weeklink u`) —
 * 13/600 w `--text-3`. Nie przycisk: przycisk konkurowałby wagą z CTA.
 */
@Composable
internal fun ScheduleLinkAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = StronkTextStyles.meta.copy(
            fontWeight = FontWeight.SemiBold,
            textDecoration = TextDecoration.Underline,
        ),
        color = StronkTheme.colors.textDim,
        maxLines = 1,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = StronkSpacing.sm, vertical = StronkSpacing.xs),
    )
}

/** Status wpisu jako badge — tylko wtedy, gdy nie jest zwykłym „zaplanowany". */
@Composable
private fun EntryStatusBadge(status: ScheduleStatus) {
    when (status) {
        ScheduleStatus.DONE -> StronkBadge(
            text = "zaliczony",
            tone = StronkTone.SUCCESS,
            icon = StronkIcons.done,
        )

        ScheduleStatus.SKIPPED -> StronkBadge(text = "odwołany", icon = StronkIcons.close)

        ScheduleStatus.MOVED -> StronkBadge(text = "przesunięty", icon = StronkIcons.swap)

        ScheduleStatus.PLANNED -> Unit
    }
}

/** Dzień bez treningu — jedna linijka, żeby ekran nie puchł. */
@Composable
private fun FreeDayCard(dayLabel: String) {
    StronkCard(contentPadding = PaddingValues(DayCardPadding)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StronkIconBadge(icon = StronkIcons.restDay)
            Column(Modifier.padding(start = StronkSpacing.sm)) {
                Text(
                    text = "Dzień wolny",
                    style = StronkTextStyles.h2,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = dayLabel,
                    style = StronkTextStyles.meta,
                    color = StronkTheme.colors.textDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Pusty harmonogram — jedna zachęta, zero tłumaczeń na trzy akapity. */
@Composable
private fun EmptySchedule(
    hasPlans: Boolean,
    onAssign: () -> Unit,
    onNewPlan: () -> Unit,
) {
    StronkEmptyState(
        icon = StronkIcons.week,
        title = "Pusty tydzień",
        description = if (hasPlans) {
            "Przypisz plan do dni tygodnia — wpisy wygenerują się same."
        } else {
            "Najpierw złóż plan, potem przypiszesz go do dni tygodnia."
        },
        actionLabel = if (hasPlans) "Zaplanuj tydzień" else "Stwórz plan",
        onAction = if (hasPlans) onAssign else onNewPlan,
    )
}
