package com.stronk.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stronk.ui.components.StronkAnchoredDropdownMenu
import com.stronk.ui.components.StronkChoiceChip
import com.stronk.ui.components.StronkIcons
import com.stronk.ui.components.StronkNoteCard
import com.stronk.ui.components.StronkPrimaryButton
import com.stronk.ui.components.StronkSectionHeader
import com.stronk.ui.components.StronkTextAction
import com.stronk.ui.components.StronkTone
import com.stronk.ui.theme.StronkRadius
import com.stronk.ui.theme.StronkSizes
import com.stronk.ui.theme.StronkSpacing
import com.stronk.ui.theme.StronkTextStyles
import com.stronk.ui.theme.StronkTheme
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

private val polishLocale = Locale.forLanguageTag("pl")

// DatePicker liczy w millisach UTC — konwersja tam i z powrotem bez strefy lokalnej.
private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun utcMillisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

/**
 * Wybór daty (kalendarz M3 w skórze „Limonka") — przesunięcie wpisu i data
 * startu przy przypisaniu planu. Sam kalendarz zostaje materiałowy (to kontrolka
 * systemowa), ale ramka i akcje są już komponentami Stronk.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleDatePickerDialog(
    title: String,
    initialDate: LocalDate,
    /**
     * Najwcześniejsza wybieralna data; `null` = bez ograniczenia (wzorzec
     * „Przesuń trening" w [ScheduleScreen]). Data startu przy przypisaniu
     * planu ([AssignPlanDialog]) przekazuje tu dzisiaj — przeplanowanie nie
     * ma prawa kasować przeszłych, niezaliczonych wpisów PLANNED.
     */
    minSelectableDate: LocalDate? = null,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val selectableDates = remember(minSelectableDate) {
        val minDate = minSelectableDate
        if (minDate == null) {
            DatePickerDefaults.AllDates
        } else {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    !utcMillisToLocalDate(utcTimeMillis).isBefore(minDate)

                override fun isSelectableYear(year: Int): Boolean = year >= minDate.year
            }
        }
    }
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toUtcMillis(),
        selectableDates = selectableDates,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        shape = StronkRadius.cardShape,
        confirmButton = {
            StronkTextAction(
                text = "Wybierz",
                tone = StronkTone.ACCENT,
                enabled = pickerState.selectedDateMillis != null,
                onClick = {
                    pickerState.selectedDateMillis
                        ?.let { millis -> onConfirm(utcMillisToLocalDate(millis)) }
                },
            )
        },
        dismissButton = {
            StronkTextAction(text = "Anuluj", onClick = onDismiss)
        },
    ) {
        DatePicker(
            state = pickerState,
            title = {
                Text(
                    text = title,
                    style = StronkTextStyles.h1Small,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(
                        start = StronkSpacing.xl,
                        end = StronkSpacing.sm,
                        top = StronkSpacing.md,
                    ),
                )
            },
            showModeToggle = false,
        )
    }
}

/**
 * Planer tygodnia całego życia planu — wybór planu (chipy), daty startu i
 * mapowania dni. Zero rozwijanych menu: dzień tygodnia przełącza się TAPEM
 * chipa, który krąży „wolne → dzień 1 → dzień 2 → … → wolne". Zatwierdzenie
 * zapisuje wzorzec w planie i materializuje harmonogram na jego podstawie
 * ([ScheduleViewModel.onAssignPlan]) — dopóki user niczego nie zmieni względem
 * [WeekPlanner.weekPlanBaseline], CTA zostaje wyszarzone (nic by się nie stało).
 */
@Composable
fun AssignPlanDialog(
    plans: List<PlanOption>,
    /** Zajęte dni ze wszystkich planów — wejście do walidacji kolizji okna. */
    occupiedEntries: List<OccupiedEntry>,
    /** Aktualny wzorzec PLANNED per plan — fallback baseline dla planów bez zapisanego wzorca. */
    plannedSlotsByPlan: Map<String, List<PlannedSlot>>,
    onConfirm: (planId: String, assignments: Map<DayOfWeek, Int>, startDate: LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    // Jedyny plan od razu wybrany — najczęstszy przypadek bez zbędnego tapnięcia.
    var selectedPlanId by remember { mutableStateOf(plans.firstOrNull()?.id) }
    val selectedPlan = plans.firstOrNull { it.id == selectedPlanId }
    // Baseline = punkt odniesienia do prefillu I do detekcji zmian (CTA).
    // Zamrożony na zmianę planu (nie dryfuje przy tle odświeżającym się stanie).
    val baseline = remember(selectedPlanId) {
        weekPlanBaseline(
            selectedPlan?.weekdayAssignments,
            plannedSlotsByPlan[selectedPlanId].orEmpty(),
            selectedPlan?.dayNames?.size ?: 0,
        )
    }
    var assignments by remember(selectedPlanId) { mutableStateOf(baseline) }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var showStartDatePicker by remember { mutableStateOf(false) }

    // Kolizja z INNYM planem w całym oknie generacji — blokuje CTA („jeden okres,
    // jeden plan"). Zajęcie przez TEN SAM plan nie blokuje — onConfirm po prostu
    // pominie zajęte dni (albo pokaże komunikat, gdy nic nowego nie powstanie).
    val conflict = remember(selectedPlanId, startDate, occupiedEntries) {
        selectedPlanId?.let { planId -> conflictingOtherPlanEntry(occupiedEntries, planId, startDate) }
    }
    // Jedyny warunek, kiedy CTA "Zapisz" ma sens: coś się realnie zmieniło
    // względem baseline. Sama zmiana daty startu bez zmiany przypisań NIE
    // zmienia wyniku materializacji tego samego wzorca — nie liczy się.
    val dirty = isWeekPlanDirty(baseline, assignments)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = StronkRadius.cardShape,
        containerColor = StronkTheme.colors.surfaceCard,
        title = {
            Text(
                text = "Zaplanuj tydzień",
                style = StronkTextStyles.h1Small,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        // Akcje żyją w treści: CTA na pełną szerokość + link „Anuluj" pod nim.
        confirmButton = {},
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (plans.size > 1) {
                    StronkSectionHeader(title = "Plan")
                    FlowRow(
                        modifier = Modifier.padding(top = StronkSpacing.xs),
                        horizontalArrangement = Arrangement.spacedBy(StronkSpacing.xs),
                        verticalArrangement = Arrangement.spacedBy(StronkSpacing.xs),
                    ) {
                        plans.forEach { plan ->
                            StronkChoiceChip(
                                label = plan.name,
                                selected = plan.id == selectedPlanId,
                                onClick = { selectedPlanId = plan.id },
                            )
                        }
                    }
                    Spacer(Modifier.height(StronkSpacing.md))
                }

                StronkSectionHeader(title = "Start")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = StronkSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = ScheduleTexts.startDateLabel(startDate),
                        style = StronkTextStyles.bodyStrong,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    ScheduleLinkAction(
                        text = "Zmień",
                        onClick = { showStartDatePicker = true },
                    )
                }

                if (selectedPlan != null) {
                    Spacer(Modifier.height(StronkSpacing.md))
                    StronkSectionHeader(
                        title = "Dni tygodnia",
                        trailing = {
                            Text(
                                text = "tapnij, by wybrać",
                                style = StronkTextStyles.meta,
                                color = StronkTheme.colors.textDim,
                            )
                        },
                    )
                    Spacer(Modifier.height(StronkSpacing.xs))
                    ScheduleConstants.DAY_ABBREVIATIONS.keys.forEach { dayOfWeek ->
                        WeekdayAssignmentRow(
                            dayOfWeek = dayOfWeek,
                            dayNames = selectedPlan.dayNames,
                            assignedDayIndex = assignments[dayOfWeek],
                            onAssign = { dayIndex ->
                                assignments =
                                    if (dayIndex == null) assignments - dayOfWeek
                                    else assignments + (dayOfWeek to dayIndex)
                            },
                        )
                    }
                    Spacer(Modifier.height(StronkSpacing.sm))
                    if (conflict != null) {
                        StronkNoteCard(
                            text = ScheduleTexts.periodConflictNote(conflict.planName),
                            tone = StronkTone.WARNING,
                            icon = StronkIcons.info,
                        )
                    } else {
                        StronkNoteCard(
                            text = ScheduleTexts.assignPlanNote(selectedPlan.fullBlockWeeks),
                            icon = StronkIcons.info,
                        )
                    }
                }

                Spacer(Modifier.height(StronkSpacing.lg))
                StronkPrimaryButton(
                    text = ScheduleTexts.ASSIGN_PLAN_CTA,
                    height = StronkSizes.ctaSmall,
                    enabled = selectedPlan != null && dirty && conflict == null,
                    onClick = { selectedPlan?.let { onConfirm(it.id, assignments, startDate) } },
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = StronkSpacing.xs),
                    contentAlignment = Alignment.Center,
                ) {
                    ScheduleLinkAction(text = "Anuluj", onClick = onDismiss)
                }
            }
        },
    )

    if (showStartDatePicker) {
        ScheduleDatePickerDialog(
            title = "Data startu",
            initialDate = startDate,
            minSelectableDate = LocalDate.now(),
            onConfirm = { date ->
                startDate = date
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false },
        )
    }
}

/**
 * Wiersz „poniedziałek → [dzień planu / wolne]"; tap w chip otwiera menu ze
 * WSZYSTKIMI dostępnymi opcjami naraz (Wolne + każdy dzień planu, aktualna
 * zaznaczona check-markiem) — wybór jednym tapem, bez przeklikiwania się po
 * kolei ([nextAssignment] cyklem null→0→1→…→null to poprzedni, odrzucony
 * wzorzec). Wizualnie to ten sam skórowany [StronkAnchoredDropdownMenu], co
 * filtr sprzętu w zamiennikach — menu na szerokość chipa-kotwicy.
 */
@Composable
private fun WeekdayAssignmentRow(
    dayOfWeek: DayOfWeek,
    dayNames: List<String>,
    assignedDayIndex: Int?,
    onAssign: (Int?) -> Unit,
) {
    val assignedName = assignedDayIndex?.let { dayNames.getOrNull(it) }
    var expanded by remember { mutableStateOf(false) }
    var anchorWidthPx by remember { mutableIntStateOf(0) }
    // Kotwiczymy menu na CAŁYM wierszu dnia (nie na wąskim chipie) — inaczej
    // menu dziedziczy szerokość chipa i długie opcje ("Full body A") łamią się
    // litera po literze. Ten sam wzorzec co StronkEquipmentFilterButton: Box
    // mierzy swoją szerokość przez onSizeChanged, DropdownMenu wewnątrz niego
    // dostaje tę szerokość — tu Box owija cały Row, więc menu jest pełnej
    // szerokości wiersza, chip zostaje wizualnie po prawej.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { anchorWidthPx = it.width },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = ScheduleConstants.DAY_NAMES.getValue(dayOfWeek)
                    .replaceFirstChar { it.titlecase(polishLocale) },
                style = StronkTextStyles.bodyStrong,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            StronkChoiceChip(
                label = assignedName ?: "wolne",
                selected = assignedName != null,
                onClick = { expanded = true },
            )
        }
        StronkAnchoredDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            anchorWidthPx = anchorWidthPx,
        ) {
                DropdownMenuItem(
                    text = { Text("wolne") },
                    onClick = {
                        onAssign(null)
                        expanded = false
                    },
                    trailingIcon = if (assignedDayIndex == null) {
                        {
                            Icon(
                                imageVector = StronkIcons.done,
                                contentDescription = null,
                                tint = StronkTheme.colors.lime,
                            )
                        }
                    } else {
                        null
                    },
                )
                dayNames.forEachIndexed { index, dayName ->
                    DropdownMenuItem(
                        text = { Text(dayName) },
                        onClick = {
                            onAssign(index)
                            expanded = false
                        },
                        trailingIcon = if (assignedDayIndex == index) {
                            {
                                Icon(
                                    imageVector = StronkIcons.done,
                                    contentDescription = null,
                                    tint = StronkTheme.colors.lime,
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
