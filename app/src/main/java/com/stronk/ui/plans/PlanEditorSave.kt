package com.stronk.ui.plans

import com.stronk.data.Plan
import com.stronk.data.PlanDay

/**
 * Buduje dokument [Plan] do zapisu z roboczego stanu edytora
 * ([PlanEditorViewModel.save]) — czysta funkcja pod test, wydzielona zeby
 * regresja pol NIEEDYTOWANYCH w edytorze byla widoczna od razu w testach
 * jednostkowych, nie dopiero na Firestore.
 *
 * Pola planu, ktorych edytor nie edytuje bezposrednio (m.in.
 * [Plan.weekdayAssignments] — wzorzec dni tygodnia zapisywany przez dialog
 * planowania tygodnia, patrz KDoc pola) MUSZA przetrwac zapis nietkniete —
 * lecą z [base], dokladnie jak w [PlanEditorViewModel.setArchived]
 * (`base.copy(...)`). Nowy plan ([base] `== null`) dostaje wartosci domyslne:
 * swieze id z [newId], czas utworzenia z [now], `archived = false`, brak
 * wzorca dni tygodnia (nigdy jeszcze nie zapisany).
 */
fun buildPlanForSave(
    base: Plan?,
    name: String,
    blockLengthWeeks: Int?,
    days: List<PlanDay>,
    newId: () -> String,
    now: () -> Long = System::currentTimeMillis,
): Plan = Plan(
    id = base?.id ?: newId(),
    name = name.trim(),
    createdAt = base?.createdAt ?: now(),
    archived = base?.archived ?: false,
    blockLengthWeeks = blockLengthWeeks,
    weekdayAssignments = base?.weekdayAssignments,
    days = days.mapIndexed { index, day ->
        day.copy(name = day.name.trim().ifEmpty { dayName(index) })
    },
)

/** Domyślna nazwa dnia: "Dzień A", "Dzień B", … (wzorem [PlanEditorViewModel]). */
internal fun dayName(index: Int): String =
    if (index < 26) "Dzień ${'A' + index}" else "Dzień ${index + 1}"
