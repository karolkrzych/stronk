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

/**
 * Mapa STARY indeks dnia (w [base] planu, przed edycją) → NOWY indeks (w
 * drafcie, przy zapisie) — pod przemapowanie [com.stronk.data.Plan.weekdayAssignments]
 * i decyzję, czy przepisać harmonogram ([dayIdentityChanged]).
 *
 * [draftBaseDayIndices] to `baseDayIndex` każdego dnia draftu W KOLEJNOŚCI
 * draftu (patrz `PlanEditorViewModel.DraftDay`): `null` dla dnia bez
 * odpowiednika w [base] (nowo dodany w tej sesji edycji — nie ma jeszcze
 * miejsca w mapie, bo nie było go czym mapować). Dzień z [base] USUNIĘTY w
 * drafcie po prostu nie występuje w żadnym elemencie listy — jego stary
 * indeks nie dostaje wpisu w wyniku (nie da się dojść do niego z draftu).
 */
fun dayIndexRemap(draftBaseDayIndices: List<Int?>): Map<Int, Int> =
    draftBaseDayIndices.mapIndexedNotNull { newIndex, baseIndex ->
        baseIndex?.let { it to newIndex }
    }.toMap()

/**
 * Czy [remap] (z [dayIndexRemap]) zmienia TOŻSAMOŚĆ któregokolwiek z
 * [baseDayCount] dni, które plan miał PRZED edycją — usunięcie dnia (brak
 * klucza w [remap] dla tego indeksu) albo przestawienie (klucz jest, ale
 * wskazuje inny indeks niż wcześniej).
 *
 * SAMO dodanie nowego dnia na końcu NIE zmienia niczego tutaj: wszystkie stare
 * indeksy dostają identity remap (`oldIndex -> oldIndex`), więc funkcja zwraca
 * `false` — nowy dzień jeszcze nigdy nie był w harmonogramie, nie ma go czego
 * przemapowywać (patrz zasada „dodanie dnia nie dotyka harmonogramu" w save()).
 */
fun dayIdentityChanged(remap: Map<Int, Int>, baseDayCount: Int): Boolean =
    (0 until baseDayCount).any { oldIndex -> remap[oldIndex] != oldIndex }
