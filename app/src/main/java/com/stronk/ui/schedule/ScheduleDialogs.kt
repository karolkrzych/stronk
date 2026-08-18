package com.stronk.ui.schedule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val polishLocale = Locale.forLanguageTag("pl")
private val startDateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", polishLocale)

// DatePicker liczy w millisach UTC — konwersja tam i z powrotem bez strefy lokalnej.
private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun utcMillisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

/**
 * Wybór daty (M3 DatePicker) — używany do przesuwania wpisu
 * i do daty startu przy przypisaniu planu.
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
        confirmButton = {
            TextButton(
                enabled = pickerState.selectedDateMillis != null,
                onClick = {
                    pickerState.selectedDateMillis
                        ?.let { millis -> onConfirm(utcMillisToLocalDate(millis)) }
                },
            ) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anuluj") }
        },
    ) {
        DatePicker(
            state = pickerState,
            title = {
                Text(
                    text = title,
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                )
            },
            showModeToggle = false,
        )
    }
}

/**
 * Przypisanie planu do tygodnia: wybór planu, daty startu i mapowania
 * dni planu na dni tygodnia; generacja wpisów robi
 * [ScheduleViewModel.onAssignPlan] po potwierdzeniu.
 */
@Composable
fun AssignPlanDialog(
    plans: List<PlanOption>,
    onConfirm: (planId: String, assignments: Map<DayOfWeek, Int>, startDate: LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    // Jedyny plan od razu wybrany — najczęstszy przypadek bez zbędnego kliknięcia.
    var selectedPlanId by remember { mutableStateOf(plans.singleOrNull()?.id) }
    val selectedPlan = plans.firstOrNull { it.id == selectedPlanId }
    var assignments by remember(selectedPlanId) {
        mutableStateOf(defaultAssignments(selectedPlan?.dayNames?.size ?: 0))
    }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var showStartDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Zaplanuj tydzień") },
        confirmButton = {
            TextButton(
                enabled = selectedPlan != null && assignments.isNotEmpty(),
                onClick = {
                    selectedPlan?.let { onConfirm(it.id, assignments, startDate) }
                },
            ) { Text("Zaplanuj ${ScheduleConstants.GENERATION_WEEKS} tyg.") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anuluj") }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                DialogFieldLabel("Plan")
                PlanDropdown(
                    plans = plans,
                    selectedPlan = selectedPlan,
                    onSelect = { selectedPlanId = it.id },
                )

                DialogFieldLabel("Start", topPadding = 16.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showStartDatePicker = true },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = startDateFormatter.format(startDate),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { showStartDatePicker = true }) { Text("Zmień") }
                }

                if (selectedPlan != null) {
                    DialogFieldLabel("Dni tygodnia", topPadding = 16.dp)
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
                    Text(
                        text = "Wpisy powstaną na najbliższe " +
                            "${ScheduleConstants.GENERATION_WEEKS} tygodnie; " +
                            "zajęte dni zostaną pominięte.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
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

@Composable
private fun DialogFieldLabel(text: String, topPadding: Dp = 0.dp) {
    Text(
        text = text.uppercase(polishLocale),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = topPadding, bottom = 4.dp),
    )
}

@Composable
private fun PlanDropdown(
    plans: List<PlanOption>,
    selectedPlan: PlanOption?,
    onSelect: (PlanOption) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = selectedPlan?.name ?: "Wybierz plan…",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp),
            )
            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            plans.forEach { plan ->
                DropdownMenuItem(
                    text = { Text("${plan.name} · ${plan.dayNames.size} dni") },
                    onClick = {
                        expanded = false
                        onSelect(plan)
                    },
                )
            }
        }
    }
}

/** Wiersz "poniedziałek → [dzień planu / wolne]" z rozwijanym wyborem. */
@Composable
private fun WeekdayAssignmentRow(
    dayOfWeek: DayOfWeek,
    dayNames: List<String>,
    assignedDayIndex: Int?,
    onAssign: (Int?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = ScheduleConstants.DAY_NAMES.getValue(dayOfWeek),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Box {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val assignedName = assignedDayIndex?.let { dayNames.getOrNull(it) }
                Text(
                    text = assignedName ?: "wolne",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (assignedName != null) FontWeight.Bold else FontWeight.Normal,
                    color = if (assignedName != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("wolne") },
                    onClick = {
                        expanded = false
                        onAssign(null)
                    },
                )
                dayNames.forEachIndexed { index, name ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            expanded = false
                            onAssign(index)
                        },
                    )
                }
            }
        }
    }
}
