package com.stronk.ui.schedule

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * Czyste funkcje kalendarza tygodnia i generacji wpisów harmonogramu
 * (zero Androida — testowalne na JVM, wzorzec jak ExerciseFilter/SubstituteFinder).
 * Daty jako [LocalDate] (bez stref — trening to dzień, nie chwila);
 * na wire konwertuje wołający przez `toString()` = "YYYY-MM-DD".
 */

/** Zaplanowany slot treningu: data + indeks dnia planu. */
data class PlannedSlot(val date: LocalDate, val dayIndex: Int)

/**
 * Dzień zajęty przez wpis harmonogramu (PLANNED/DONE) — pod walidację kolizji
 * planów w [AssignPlanDialog] i [ScheduleViewModel.onAssignPlan]. [planName]
 * to nazwa planu w chwili budowy stanu (nie referencja — plan może zniknąć).
 */
data class OccupiedEntry(val date: LocalDate, val planId: String, val planName: String)

/**
 * Okno tygodni bloku pokazywane w siatce kwadratów: [startWeek] to 0-based
 * indeks pierwszego rzędu w bloku, [rows] to liczba rzędów.
 */
data class BlockWindow(val startWeek: Int, val rows: Int)

private val polishLocale = Locale.forLanguageTag("pl")
private val dayMonthFormatter = DateTimeFormatter.ofPattern("d MMMM", polishLocale)
private val dayMonthYearFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", polishLocale)

/** Poniedziałek tygodnia (ISO), w którym leży [date]. */
fun weekStartOf(date: LocalDate): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/**
 * Etykieta zakresu tygodnia od [weekStart], np. "10–16 sierpnia" (jeden miesiąc)
 * albo "28 lipca – 3 sierpnia" (przełom miesięcy). Rok dopisywany tylko wtedy,
 * gdy różni się od roku [today] — bez szumu w typowym widoku.
 */
fun weekLabel(weekStart: LocalDate, today: LocalDate): String {
    val weekEnd = weekStart.plusDays((ScheduleConstants.DAYS_IN_WEEK - 1).toLong())
    val endLabel =
        if (weekEnd.year == today.year) dayMonthFormatter.format(weekEnd)
        else dayMonthYearFormatter.format(weekEnd)
    return when {
        weekStart.month == weekEnd.month && weekStart.year == weekEnd.year ->
            "${weekStart.dayOfMonth}–$endLabel"

        weekStart.year == weekEnd.year ->
            "${dayMonthFormatter.format(weekStart)} – $endLabel"

        else -> "${dayMonthYearFormatter.format(weekStart)} – $endLabel"
    }
}

/**
 * Które tygodnie bloku trafiają do siatki kwadratów.
 *
 * Blok mieszczący się w limicie pokazujemy w całości od tygodnia 0 (typowy
 * przypadek: 5 tygodni pracy + 1 lekki = 6 rzędów, dokładnie jak mock).
 * Blok krótszy niż [minRows] dopełniamy kolejnymi tygodniami kalendarza
 * (siatka ma być dominantą ekranu, a nie dwoma rzędami), a blok dłuższy niż
 * [maxRows] pokazujemy oknem wyśrodkowanym na bieżącym tygodniu.
 *
 * @param weekIndexInBlock 0-based pozycja bieżącego tygodnia w bloku
 *   (z `ProgressionEngine.weekIndexInBlock` — silnik jest źródłem prawdy)
 * @param blockLengthWeeks pełna długość bloku (praca + tydzień lekki)
 */
fun blockGridWindow(
    weekIndexInBlock: Int,
    blockLengthWeeks: Int,
    minRows: Int = ScheduleConstants.GRID_WEEKS_MIN,
    maxRows: Int = ScheduleConstants.GRID_WEEKS_MAX,
): BlockWindow {
    val length = blockLengthWeeks.coerceAtLeast(1)
    val rows = length.coerceIn(minRows.coerceAtMost(maxRows), maxRows)
    if (rows >= length) return BlockWindow(startWeek = 0, rows = rows)
    val centered = weekIndexInBlock - (rows - 1) / 2
    return BlockWindow(startWeek = centered.coerceIn(0, length - rows), rows = rows)
}

/**
 * Okno siatki dla planu BEZ bloku (`Plan.blockLengthWeeks == null`): stałe
 * [rows] tygodni wokół bieżącego, z [past] tygodniami przeszłości. Bloku nie ma
 * czego domykać, więc siatka po prostu jedzie razem z planem w nieskończoność.
 *
 * @param weekIndex liniowy numer tygodnia planu, 0-based
 *   (z `ProgressionEngine.weekIndexForBlock` dla planu bez bloku)
 */
fun continuousGridWindow(
    weekIndex: Int,
    rows: Int = ScheduleConstants.GRID_WEEKS_CONTINUOUS,
    past: Int = ScheduleConstants.GRID_WEEKS_CONTINUOUS_PAST,
): BlockWindow = BlockWindow(
    startWeek = (weekIndex - past).coerceAtLeast(0),
    rows = rows.coerceAtLeast(1),
)

/**
 * Okno siatki dla planu z blokiem ALBO bez niego — jedno wejście dla ekranu:
 * [fullBlockWeeks] null = plan bez bloku ([continuousGridWindow]), w przeciwnym
 * razie zwykłe okno bloku ([blockGridWindow]).
 */
fun gridWindow(weekIndex: Int, fullBlockWeeks: Int?): BlockWindow =
    if (fullBlockWeeks == null) {
        continuousGridWindow(weekIndex)
    } else {
        blockGridWindow(weekIndex, fullBlockWeeks)
    }

/**
 * Poniedziałki rzędów siatki. Kotwicą jest poniedziałek tygodnia [today]
 * cofnięty o [weekIndexInBlock] tygodni — silnik liczy tygodnie bloku jako
 * pełne 7-dniowe okna od `plan.createdAt`, więc w kalendarzu (który zaczyna
 * tydzień w poniedziałek) blok kotwiczymy przez bieżący tydzień, nie przez
 * dokładną datę utworzenia planu.
 */
fun blockWeekMondays(
    today: LocalDate,
    weekIndexInBlock: Int,
    window: BlockWindow,
): List<LocalDate> {
    val blockFirstMonday = weekStartOf(today).minusWeeks(weekIndexInBlock.toLong())
    return (0 until window.rows).map { row ->
        blockFirstMonday.plusWeeks((window.startWeek + row).toLong())
    }
}

/**
 * Domyślne przypisanie dni planu do dni tygodnia (dzień tygodnia → indeks dnia
 * planu) wg [ScheduleConstants.DEFAULT_TRAINING_DAYS]. Plan dłuższy niż 7 dni
 * dostaje przypisane pierwsze 7 — resztę user rozkłada ręcznie.
 */
fun defaultAssignments(planDayCount: Int): Map<DayOfWeek, Int> {
    if (planDayCount <= 0) return emptyMap()
    val weekdays = ScheduleConstants.DEFAULT_TRAINING_DAYS
        .getValue(planDayCount.coerceAtMost(ScheduleConstants.DAYS_IN_WEEK))
    return weekdays.mapIndexed { index, dayOfWeek -> dayOfWeek to index }.toMap()
}

/**
 * Sloty "planned" wygenerowane z przypisania dni planu do dni tygodnia.
 *
 * [assignments]: dzień tygodnia → indeks dnia planu; jeden dzień planu może
 * wystąpić w kilku dniach tygodnia (np. full body 3×/tydz.), ale dzień
 * tygodnia ma najwyżej jeden trening.
 *
 * Okno generacji to [startDate] włącznie + [weeks] pełnych tygodni
 * ("najbliższe N tygodni od daty startu", nie N tygodni kalendarzowych).
 * Daty z [occupiedDates] (dni z już aktywnym wpisem) są pomijane —
 * nie dublujemy treningów na zajętym dniu.
 */
fun generatePlannedSlots(
    assignments: Map<DayOfWeek, Int>,
    startDate: LocalDate,
    weeks: Int = ScheduleConstants.GENERATION_WEEKS,
    occupiedDates: Set<LocalDate> = emptySet(),
): List<PlannedSlot> {
    if (weeks <= 0 || assignments.isEmpty()) return emptyList()
    val firstMonday = weekStartOf(startDate)
    val endExclusive = startDate.plusWeeks(weeks.toLong())
    // 0..weeks (włącznie): okno kroczące może zahaczać o tydzień kalendarzowy
    // za ostatnim pełnym — filtr dat przycina nadmiar.
    return (0..weeks).flatMap { week ->
        val monday = firstMonday.plusWeeks(week.toLong())
        assignments.map { (dayOfWeek, dayIndex) -> PlannedSlot(monday.with(dayOfWeek), dayIndex) }
    }
        .filter { it.date >= startDate && it.date < endExclusive && it.date !in occupiedDates }
        .sortedBy { it.date }
}

// ---------- rolling generation (plan bez bloku) ----------

/**
 * Czy plan bez bloku potrzebuje dogenerowania kolejnych tygodni: najpóźniejszy
 * zaplanowany wpis [lastPlannedDate] jest bliżej niż [thresholdWeeks] tygodni od
 * [today]. Ściśle „bliżej niż" — dokładnie [thresholdWeeks] tygodni od dziś to
 * jeszcze nie powód (zapas się nie skończył).
 */
fun needsRollingExtension(
    lastPlannedDate: LocalDate,
    today: LocalDate,
    thresholdWeeks: Int = ScheduleConstants.ROLLING_THRESHOLD_WEEKS,
): Boolean = lastPlannedDate.isBefore(today.plusWeeks(thresholdWeeks.toLong()))

/**
 * Przypisanie dzień tygodnia → indeks dnia planu wyprowadzone z ISTNIEJĄCYCH
 * wpisów (rolling generation nie ma dostępu do assignments z dialogu — te żyją
 * tylko lokalnie w [AssignPlanDialog], nigdzie nie są trwałe).
 *
 * Bierzemy ostatni tydzień (najpóźniejszy poniedziałek) z wpisów w [entries] —
 * generacja produkuje identyczny wzorzec dnia tygodnia w każdym tygodniu, więc
 * jeden tydzień wystarczy jako źródło prawdy. Puste [entries] dają pustą mapę.
 */
fun deriveWeekAssignments(entries: List<PlannedSlot>): Map<DayOfWeek, Int> {
    if (entries.isEmpty()) return emptyMap()
    val byWeek = entries.groupBy { weekStartOf(it.date) }
    val lastWeekStart = byWeek.keys.max()
    return byWeek.getValue(lastWeekStart).associate { it.date.dayOfWeek to it.dayIndex }
}

// ---------- przeplanowanie wybranego planu ----------

/**
 * Rodzaj wpisu istotny dla przeplanowania: [PLANNED] wybranego planu jest
 * kasowany i zastępowany, [DONE] (dowolnego planu, także tego samego —
 * historia treningu) blokuje datę, [OTHER] (SKIPPED/MOVED) jest neutralny —
 * nie blokuje i nie jest kasowany (obecne traktowanie occupied, zachowane).
 */
enum class ScheduleEntryKind { PLANNED, DONE, OTHER }

/**
 * Minimalny widok wpisu harmonogramu pod [planReplacement] — zero zależności
 * od modelu Firestore, wzorem [PlannedSlot]/[OccupiedEntry].
 */
data class ScheduleEntryRef(val id: String, val date: LocalDate, val planId: String, val kind: ScheduleEntryKind)

/**
 * Wynik przeplanowania: id-ki starych wpisów PLANNED wybranego planu do
 * skasowania + nowe sloty do zapisania w ich miejsce.
 */
data class ReplanResult(val idsToDelete: List<String>, val slots: List<PlannedSlot>)

/**
 * Przeplanowanie [selectedPlanId]: WSZYSTKIE jego przyszłe (`date >=
 * [startDate]`) wpisy PLANNED lecą do kasacji — rolling generation mógł je
 * nagenerować dalej niż jedno okno [weeks], stąd „wszystkie", nie tylko okno
 * generacji — a w ich miejsce powstają nowe sloty wg [assignments].
 *
 * Nietykalne: wpisy DONE (dowolnego planu — historia treningu) i PLANNED
 * INNEGO planu blokują datę (nowy slot tam nie powstaje), tak jak
 * [conflictingOtherPlanEntry] blokuje CTA w dialogu. SKIPPED/MOVED
 * ([ScheduleEntryKind.OTHER]) nie blokują i nie są kasowane. Stare wpisy
 * PLANNED TEGO SAMEGO planu nie blokują nowych slotów — i tak trafiają do
 * [ReplanResult.idsToDelete].
 */
fun planReplacement(
    currentEntries: List<ScheduleEntryRef>,
    selectedPlanId: String,
    assignments: Map<DayOfWeek, Int>,
    startDate: LocalDate,
    weeks: Int = ScheduleConstants.GENERATION_WEEKS,
): ReplanResult {
    val idsToDelete = currentEntries
        .filter { it.planId == selectedPlanId && it.kind == ScheduleEntryKind.PLANNED && it.date >= startDate }
        .map { it.id }
    val occupied = currentEntries
        .filter { it.kind == ScheduleEntryKind.DONE || (it.kind == ScheduleEntryKind.PLANNED && it.planId != selectedPlanId) }
        .map { it.date }
        .toSet()
    val slots = generatePlannedSlots(assignments, startDate, weeks, occupied)
    return ReplanResult(idsToDelete, slots)
}

// ---------- walidacja kolizji planów ----------

/**
 * Pierwszy (najwcześniejszy) wpis INNEGO planu w oknie [startDate, startDate +
 * [weeks]) — sygnał blokujący CTA w [AssignPlanDialog]. Cel: na jeden okres tylko
 * jeden plan, więc sprawdzamy CAŁE okno, nie tylko konkretne dni przypisania.
 *
 * `null` = okno wolne od kolizji z innym planem (może być częściowo zajęte przez
 * TEN SAM plan — to nie blokuje CTA, patrz [ScheduleViewModel.onAssignPlan]).
 */
fun conflictingOtherPlanEntry(
    occupied: List<OccupiedEntry>,
    selectedPlanId: String,
    startDate: LocalDate,
    weeks: Int = ScheduleConstants.GENERATION_WEEKS,
): OccupiedEntry? {
    val endExclusive = startDate.plusWeeks(weeks.toLong())
    return occupied
        .filter { it.planId != selectedPlanId && it.date >= startDate && it.date < endExclusive }
        .minByOrNull { it.date }
}
