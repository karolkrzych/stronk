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
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate.toUtcMillis())
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
 * Przypisanie planu do dni tygodnia — wybór planu (chipy), daty startu i
 * mapowania dni. Zero rozwijanych menu: dzień tygodnia przełącza się TAPEM
 * chipa, który krąży „wolne → dzień 1 → dzień 2 → … → wolne".
 * Generację wpisów robi [ScheduleViewModel.onAssignPlan] po potwierdzeniu.
 */
@Composable
fun AssignPlanDialog(
    plans: List<PlanOption>,
    onConfirm: (planId: String, assignments: Map<DayOfWeek, Int>, startDate: LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    // Jedyny plan od razu wybrany — najczęstszy przypadek bez zbędnego tapnięcia.
    var selectedPlanId by remember { mutableStateOf(plans.firstOrNull()?.id) }
    val selectedPlan = plans.firstOrNull { it.id == selectedPlanId }
    var assignments by remember(selectedPlanId) {
        mutableStateOf(defaultAssignments(selectedPlan?.dayNames?.size ?: 0))
    }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var showStartDatePicker by remember { mutableStateOf(false) }

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
                                text = "tapnij, by zmienić",
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
                    StronkNoteCard(
                        text = "Wpisy na ${ScheduleConstants.GENERATION_WEEKS} tygodnie; " +
                            "zajęte dni pomijamy.",
                        icon = StronkIcons.info,
                    )
                }

                Spacer(Modifier.height(StronkSpacing.lg))
                StronkPrimaryButton(
                    text = "Zaplanuj ${ScheduleConstants.GENERATION_WEEKS} tyg.",
                    height = StronkSizes.ctaSmall,
                    enabled = selectedPlan != null && assignments.isNotEmpty(),
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
            onConfirm = { date ->
                startDate = date
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false },
        )
    }
}

/** Wiersz „poniedziałek → [dzień planu / wolne]"; tap krąży po dniach planu. */
@Composable
private fun WeekdayAssignmentRow(
    dayOfWeek: DayOfWeek,
    dayNames: List<String>,
    assignedDayIndex: Int?,
    onAssign: (Int?) -> Unit,
) {
    val assignedName = assignedDayIndex?.let { dayNames.getOrNull(it) }
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
            onClick = { onAssign(nextAssignment(assignedDayIndex, dayNames.size)) },
        )
    }
}

/** Kolejny stan chipa dnia: null → 0 → 1 → … → ostatni → null (wolne). */
private fun nextAssignment(current: Int?, dayCount: Int): Int? = when {
    dayCount <= 0 -> null
    current == null -> 0
    current + 1 < dayCount -> current + 1
    else -> null
}
