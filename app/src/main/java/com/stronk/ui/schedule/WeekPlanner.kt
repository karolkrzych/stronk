package com.stronk.ui.schedule

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
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
 * Poniedziałki rzędów KLASYCZNEJ siatki miesiąca [month]: od poniedziałku
 * tygodnia, w którym leży 1. dzień miesiąca, po poniedziałek tygodnia, w którym
 * leży ostatni dzień. Zawsze pełne tygodnie (kolumny = dni tygodnia się nie
 * rozjeżdżają) — dni spoza [month] renderują się jako PUSTE placeholdery, patrz
 * [ScheduleViewModel.buildState].
 *
 * Liczba rzędów wychodzi z kalendarza, nie z konfiguracji: 4 (luty roku
 * nieprzestępnego zaczynający się w poniedziałek) do 6 (długi miesiąc
 * zaczynający się pod koniec tygodnia).
 */
fun monthGridMondays(month: YearMonth): List<LocalDate> {
    val firstMonday = weekStartOf(month.atDay(1))
    val lastMonday = weekStartOf(month.atEndOfMonth())
    val rows = ChronoUnit.WEEKS.between(firstMonday, lastMonday).toInt() + 1
    return (0 until rows).map { row -> firstMonday.plusWeeks(row.toLong()) }
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
 * Czy plan kwalifikuje się do rolling generation
 * ([ScheduleViewModel.maybeExtendContinuousPlans]): musi być BEZ bloku
 * ([blockLengthWeeks] `== null` — plan z blokiem ma własny horyzont z
 * [blockReplanWeeks]) i NIE może być zarchiwizowany. Zarchiwizowany plan nie
 * ma dostawać nowych wpisów PLANNED — to on jest źródłem martwych wpisów,
 * które reszta tego pliku sprząta ([archivedPlanDeadEntryIds],
 * [planReplacement]), więc rolling nie ma prawa produkować kolejnych.
 */
fun isEligibleForRollingExtension(archived: Boolean, blockLengthWeeks: Int?): Boolean =
    !archived && blockLengthWeeks == null

/**
 * Id-ki przyszłych (`date >= [today]`) wpisów zarchiwizowanych planów
 * ([ScheduleEntryRef.archived]) — martwe wpisy do skasowania batchem. Dwa
 * miejsca użycia (ten sam mechanizm, jedna funkcja):
 * - [PlanEditorViewModel.setArchived]: [refs] ograniczone do JEDNEGO planu
 *   właśnie archiwizowanego (`archived = true` tylko dla jego wpisów) —
 *   sprząta świeżo martwe wpisy w chwili archiwizacji;
 * - sweep przy starcie ekranu Tydzień ([ScheduleViewModel]): [refs] ze
 *   WSZYSTKICH już zarchiwizowanych planów — samonaprawia stan sprzed
 *   istnienia czyszczenia w [PlanEditorViewModel.setArchived] (dokładnie
 *   przypadek usera z tego zgłoszenia), bez migracji danych.
 *
 * Martwy jest KAŻDY status poza DONE: PLANNED (trening, który się już nie
 * odbędzie), ale też MOVED i SKIPPED. Te dwa ostatnie to breadcrumby
 * („Przesunięty → czwartek", „Odwołany") sensowne tylko wtedy, gdy plan
 * jeszcze żyje — po archiwizacji nie ma czego opisywać i wiszą w karcie dnia
 * jako karty-widma (drugi artefakt z tego zgłoszenia: poniedziałek z kartą
 * „Przesunięty → czwartek" obok normalnego treningu INNEGO planu; kluczowanie
 * po (planId, data) w [shadowedEntryIds] takiej pary nie łapie, bo plany są
 * różne).
 *
 * DONE nigdy nie jest tu brane pod uwagę — [kind] filtruje wyłącznie
 * PLANNED/MOVED/SKIPPED, więc historia treningu jest nietykalna niezależnie od
 * tego, jak wołający zbudował [ScheduleEntryRef.archived] dla wpisów DONE.
 * Wpisy sprzed [today] też zostają — audit-trail (patrz [Plan] KDoc
 * archiwizacji); z oczu znikają natychmiast przez [archivedPlanGhostEntryIds].
 */
fun archivedPlanDeadEntryIds(refs: List<ScheduleEntryRef>, today: LocalDate = LocalDate.now()): List<String> =
    refs
        .filter { it.kind != ScheduleEntryKind.DONE && it.archived && it.date >= today }
        .map { it.id }

/**
 * Id-ki wpisów MOVED/SKIPPED zarchiwizowanych planów — karty-widma do UKRYCIA
 * w karcie dnia ([ScheduleViewModel.buildState]), wzorem [shadowedEntryIds]:
 * render nie czeka na sweep bazy, artefakt znika w tej samej klatce.
 *
 * Świadoma asymetria wobec [archivedPlanDeadEntryIds]: sweep kasuje z bazy
 * tylko wpisy od dziś w przód (przeszłość to audit-trail), a ten filtr ukrywa
 * je NIEZALEŻNIE od daty — zarchiwizowany plan nie ma prawa opowiadać w
 * kalendarzu o przesunięciach i odwołaniach, do których nigdy nie dojdzie.
 * Jedyne, co po nim zostaje widoczne, to fakty: wpisy DONE (i przeszłe
 * PLANNED — „zaplanowany, nie zrobiony" to też fakt).
 *
 * DONE nigdy nie wpada — filtr bierze wyłącznie MOVED i SKIPPED.
 */
fun archivedPlanGhostEntryIds(refs: List<ScheduleEntryRef>): List<String> =
    refs
        .filter { it.archived && (it.kind == ScheduleEntryKind.MOVED || it.kind == ScheduleEntryKind.SKIPPED) }
        .map { it.id }

/**
 * Przypisanie dzień tygodnia → indeks dnia planu wyprowadzone z ISTNIEJĄCYCH
 * wpisów (rolling generation nie ma dostępu do assignments z dialogu — te żyją
 * tylko lokalnie w [AssignPlanDialog], nigdzie nie są trwałe).
 *
 * Bierzemy ostatni tydzień (najpóźniejszy poniedziałek) z wpisów w [entries] —
 * generacja produkuje identyczny wzorzec dnia tygodnia w każdym tygodniu, więc
 * jeden tydzień wystarczy jako źródło prawdy. Puste [entries] dają pustą mapę.
 *
 * [dayCount] odfiltrowuje wpisy z `dayIndex` poza aktualną liczbą dni planu —
 * plan mógł stracić dni PO wygenerowaniu tych wpisów (edycja w edytorze planu
 * usuwa dzień, wpisy w `schedule` zostają ze starym indeksem). Bez tego filtra
 * martwy indeks kopiowałby się w każdy kolejny tydzień rolling generation
 * ([ScheduleViewModel.maybeExtendContinuousPlans]). Domyślnie bez ograniczenia
 * — wołający, których to nie dotyczy, dostają dotychczasowe zachowanie.
 */
fun deriveWeekAssignments(entries: List<PlannedSlot>, dayCount: Int = Int.MAX_VALUE): Map<DayOfWeek, Int> {
    if (entries.isEmpty()) return emptyMap()
    val byWeek = entries.groupBy { weekStartOf(it.date) }
    val lastWeekStart = byWeek.keys.max()
    return byWeek.getValue(lastWeekStart)
        .filter { it.dayIndex < dayCount }
        .associate { it.date.dayOfWeek to it.dayIndex }
}

// ---------- wzorzec tygodnia trwały w planie (Plan.weekdayAssignments) ----------

/**
 * [Plan.weekdayAssignments] (klucz = ISO dzień tygodnia 1..7) → [DayOfWeek],
 * format użyty w reszcie tego pliku i w [AssignPlanDialog]. Klucz spoza 1..7
 * jest pomijany (odporność wzorem [com.stronk.data.FirestoreMappers]).
 */
fun weekdayAssignmentsFromIso(raw: Map<Int, Int>): Map<DayOfWeek, Int> =
    raw.mapNotNull { (iso, dayIndex) ->
        DayOfWeek.entries.firstOrNull { it.value == iso }?.let { it to dayIndex }
    }.toMap()

/** Odwrotność [weekdayAssignmentsFromIso] — zapis do [Plan.weekdayAssignments]. */
fun weekdayAssignmentsToIso(assignments: Map<DayOfWeek, Int>): Map<Int, Int> =
    assignments.entries.associate { (dayOfWeek, dayIndex) -> dayOfWeek.value to dayIndex }

/**
 * Przemapowanie wzorca dnia tygodnia ([assignments], dzień tygodnia → indeks
 * dnia planu) po edycji STRUKTURY dni w edytorze planu
 * ([com.stronk.ui.plans.PlanEditorViewModel.save]): [remap] to stary indeks
 * dnia → nowy (z `PlanEditorSave.dayIndexRemap`, zbudowany ze śladu tożsamości
 * dni draftu). Przypisania wskazujące dzień USUNIĘTY (brak klucza w [remap])
 * wypadają — user skasował ten dzień w edytorze, więc nie ma już czego
 * przypisywać. Dzień PRZESTAWIONY dostaje swój nowy indeks; dzień
 * NIETKNIĘTY (identity remap) zostaje bez zmian.
 */
fun remapWeekdayAssignments(assignments: Map<DayOfWeek, Int>, remap: Map<Int, Int>): Map<DayOfWeek, Int> =
    assignments.mapNotNull { (dayOfWeek, oldDayIndex) -> remap[oldDayIndex]?.let { dayOfWeek to it } }.toMap()

/**
 * Wzorzec bazowy dla wybranego planu w [AssignPlanDialog] — punkt odniesienia
 * do prefillu chipów ORAZ do detekcji zmian ([isWeekPlanDirty]). Zapisany
 * wzorzec planu ([savedAssignments], już po [weekdayAssignmentsFromIso])
 * wygrywa zawsze, gdy istnieje — także pusty (user mógł świadomie wyzerować
 * wszystkie dni). Gdy plan nigdy nie miał zapisanego wzorca (`null` — stary
 * dokument sprzed tego pola albo plan jeszcze nigdy nie przypisany), spadamy
 * na wzorzec wyprowadzony z istniejących wpisów PLANNED ([deriveWeekAssignments]);
 * bez wpisów w ogóle — pusty wzorzec.
 *
 * [dayCount] odfiltrowuje przypisania (zarówno [savedAssignments], jak i
 * fallback z [deriveWeekAssignments]) z `dayIndex` poza aktualną liczbą dni
 * planu — [savedAssignments] to [Plan.weekdayAssignments] z bazy, który mógł
 * powstać PRZED usunięciem dni z planu (patrz [deriveWeekAssignments]).
 */
fun weekPlanBaseline(
    savedAssignments: Map<DayOfWeek, Int>?,
    existingEntries: List<PlannedSlot>,
    dayCount: Int = Int.MAX_VALUE,
): Map<DayOfWeek, Int> =
    savedAssignments?.filterValues { it < dayCount } ?: deriveWeekAssignments(existingEntries, dayCount)

/**
 * Czy CTA „Zapisz" w [AssignPlanDialog] ma być aktywne: [assignments] różnią
 * się od [baseline]. Prosta nierówność map pokrywa też przypadek „baseline
 * pusty, przypisania niepuste" (plan bez wcześniejszego wzorca, user dopiero
 * co coś przypisał) — to tylko szczególny przypadek różnicy, nie osobna reguła.
 */
fun isWeekPlanDirty(baseline: Map<DayOfWeek, Int>, assignments: Map<DayOfWeek, Int>): Boolean =
    assignments != baseline

// ---------- przeplanowanie wybranego planu ----------

/**
 * Rodzaj wpisu istotny dla przeplanowania: [PLANNED] wybranego planu jest
 * kasowany i zastępowany, [DONE] (dowolnego planu, także tego samego —
 * historia treningu) blokuje datę, [SKIPPED] jest neutralny — nie blokuje i
 * nie jest kasowany (user świadomie odwołał trening, dzień ma wrócić do puli).
 *
 * [MOVED] to NIE to samo co [SKIPPED], mimo że oba są „nieaktywne": trening z
 * tej daty nadal istnieje, tyle że pod INNĄ datą ([ScheduleEntryRef.movedTo]).
 * Data źródłowa aktywnego przesunięcia jest dla WŁASNEGO planu zajęta
 * (patrz [activeMovedSlots], [planReplacement]) — inaczej ponowna
 * materializacja tego samego wzorca tygodnia dokłada na nią DRUGI wpis
 * PLANNED i dzień jest naraz „przesunięty" i „zaplanowany".
 */
enum class ScheduleEntryKind { PLANNED, DONE, MOVED, SKIPPED }

/**
 * Minimalny widok wpisu harmonogramu pod [planReplacement] — zero zależności
 * od modelu Firestore, wzorem [PlannedSlot]/[OccupiedEntry].
 *
 * [archived]: plan-właściciel tego wpisu jest zarchiwizowany. Wołający ustawia
 * ją dla KAŻDEGO statusu poza [ScheduleEntryKind.DONE] — czyli dla PLANNED,
 * MOVED i SKIPPED. Taki wpis to „martwy" wpis (plan zarchiwizowany, ale wpis w
 * `schedule` przetrwał archiwizację): PLANNED nie ma prawa niczego blokować
 * (patrz [planReplacement] i [buildOccupiedEntries]), a MOVED/SKIPPED nie mają
 * prawa renderować się jako karty-widma (patrz [archivedPlanGhostEntryIds]);
 * wszystkie trzy idą do skasowania przez [archivedPlanDeadEntryIds].
 * [ScheduleEntryKind.DONE] MUSI zawsze mieć `archived = false` — historia
 * treningu blokuje datę niezależnie od tego, czy plan, pod którym trening
 * poszedł, jest dziś zarchiwizowany (wołający pilnuje tego przy budowie).
 *
 * [movedTo]: data, NA KTÓRĄ trening pojechał — wypełniona wyłącznie dla
 * [ScheduleEntryKind.MOVED] (`ScheduleEntry.movedTo`). Bez niej nie da się
 * odróżnić żywego przesunięcia (cel wciąż istnieje) od osieroconego wpisu po
 * skasowanym celu, patrz [activeMovedSlots].
 *
 * [dayIndex]: indeks dnia planu (`ScheduleEntry.dayIndex`) — jedyna tożsamość
 * TRENINGU, jaką niesie wpis. Potrzebna w [moveResolution]: bez niej okruch
 * „Full body A pojechał na środę" skleiłby się z przesunięciem „Full body B"
 * wychodzącym z tej samej środy (ten sam plan, ta sama data) i skasował cudzy,
 * całkiem poprawny breadcrumb. Domyślnie 0 — wołający, których sklejanie
 * łańcucha nie dotyczy, nie muszą go wypełniać.
 */
data class ScheduleEntryRef(
    val id: String,
    val date: LocalDate,
    val planId: String,
    val kind: ScheduleEntryKind,
    val archived: Boolean = false,
    val movedTo: LocalDate? = null,
    val dayIndex: Int = 0,
)

/** Aktywne przesunięcie: trening z dnia [from] leży pod datą [to]. */
data class MovedSlot(val from: LocalDate, val to: LocalDate)

/**
 * Aktywne przesunięcia [planId] o dacie ŹRÓDŁOWEJ od [since] w przód: wpisy
 * MOVED tego planu, których cel ([ScheduleEntryRef.movedTo]) NADAL trzyma żywy
 * wpis (PLANNED albo DONE) TEGO SAMEGO planu.
 *
 * Warunek „cel wciąż żyje" jest kluczowy: osierocony MOVED (cel skasowany albo
 * odwołany) nie ma prawa blokować swojej daty na zawsze — inaczej jedno
 * przesunięcie wykluczyłoby ten dzień tygodnia z planu do końca świata.
 * Osieroconym zajmuje się dopiero [shadowedEntryIds] (gdy jest zdublowany) albo
 * kolejne przeplanowanie.
 *
 * Wynik podaje OBIE daty pary: źródłowa jest dla tego planu zajęta (trening już
 * z niej wyszedł), docelowa też (trening tam leży) — patrz [planReplacement].
 */
fun activeMovedSlots(
    entries: List<ScheduleEntryRef>,
    planId: String,
    since: LocalDate,
): List<MovedSlot> {
    val liveDates = entries
        .filter {
            it.planId == planId &&
                (it.kind == ScheduleEntryKind.PLANNED || it.kind == ScheduleEntryKind.DONE)
        }
        .mapTo(HashSet()) { it.date }
    return entries
        .filter { it.kind == ScheduleEntryKind.MOVED && it.planId == planId && it.date >= since }
        .mapNotNull { entry ->
            entry.movedTo
                ?.takeIf { it in liveDates }
                ?.let { target -> MovedSlot(from = entry.date, to = target) }
        }
}

/**
 * Id-ki wpisów MOVED/SKIPPED „przykrytych" żywym wpisem: na TEJ SAMEJ dacie i w
 * TYM SAMYM planie leży wpis PLANNED albo DONE. Taki wpis nie niesie już żadnej
 * informacji — dzień ma realny trening, a etykieta „Przesunięty/Odwołany" obok
 * niego to czysta sprzeczność (dokładnie artefakt z tego zgłoszenia: poniedziałek
 * naraz „Przesunięty → czwartek" i z pełną listą ćwiczeń).
 *
 * Jedna reguła, dwa użycia (wzorem [archivedPlanDeadEntryIds]):
 * - [ScheduleViewModel.buildState] UKRYWA te wpisy w karcie dnia — natychmiastowa
 *   naprawa wyświetlania, także dla stanu, który dopiero co powstał;
 * - sweep przy starcie ekranu Tydzień KASUJE je z bazy — samonaprawa kont, na
 *   których duplikat już siedzi (bez ręcznej edycji Firestore).
 *
 * DONE nie jest tu nigdy kandydatem do ukrycia/kasacji — historia treningu jest
 * nietykalna, filtr bierze wyłącznie MOVED i SKIPPED.
 *
 * Klucz to para (planId, data), więc ta reguła z definicji NIE łapie widma
 * INNEGO planu niż ten, który trzyma żywy wpis na tej dacie — takie widma
 * (zwykle po planie zarchiwizowanym) sprząta [archivedPlanGhostEntryIds] i
 * [archivedPlanDeadEntryIds]. Dwie reguły, wspólny cel: jeden dzień w karcie
 * dnia nie może być naraz treningiem i notatką „Przesunięty/Odwołany".
 */
fun shadowedEntryIds(entries: List<ScheduleEntryRef>): List<String> {
    val liveKeys = entries
        .filter { it.kind == ScheduleEntryKind.PLANNED || it.kind == ScheduleEntryKind.DONE }
        .mapTo(HashSet()) { it.planId to it.date }
    return entries
        .filter { it.kind == ScheduleEntryKind.MOVED || it.kind == ScheduleEntryKind.SKIPPED }
        .filter { (it.planId to it.date) in liveKeys }
        .map { it.id }
}

// ---------- przesunięcie pojedynczego treningu (łańcuch okruchów) ----------

/** Okruch MOVED do przekierowania: wpis [id] ma odtąd wskazywać [movedTo]. */
data class MovedCrumbRedirect(val id: String, val movedTo: LocalDate)

/**
 * Co zrobić z okruchami MOVED przy przesunięciu jednego treningu — wynik
 * [moveResolution], wykonywany przez [ScheduleViewModel.onMoveEntry].
 *
 * [crumbIdsToDelete]: okruchy do skasowania (trening wrócił do punktu wyjścia).
 * [crumbsToRedirect]: okruchy, które mają wskazywać nową datę docelową.
 * [createCrumbAtSource]: czy na dacie ŹRÓDŁOWEJ ma powstać NOWY okruch
 * („Przesunięty → …"). `false` znaczy „data źródłowa była tylko przystankiem" —
 * wpis po prostu jedzie dalej pod nową datę, bez zostawiania śladu.
 */
data class MoveResolution(
    val crumbIdsToDelete: List<String>,
    val crumbsToRedirect: List<MovedCrumbRedirect>,
    val createCrumbAtSource: Boolean,
)

/**
 * Przesunięcie wpisu [movedEntry] z jego daty (dalej „X") na [newDate] („Y") —
 * reguła SKLEJANIA łańcucha okruchów. Bez niej każdy przystanek zostawiał
 * własną kartę „Przesunięty → …" i po odesłaniu treningu z powrotem zostawał
 * na kalendarzu śmieć: dzień, który nigdy nie był dniem wzorca, trzymał trwałą
 * notatkę o przesunięciu (zgłoszenie: pon → wt → pon zostawiało wtorek z kartą
 * „Przesunięty → poniedziałek", której nie łapie ani [shadowedEntryIds] — na
 * wtorku nie ma żywego wpisu — ani [activeMovedSlots], bo cel przesunięcia żyje).
 *
 * Reguła:
 * - X BYŁO PRZYSTANKIEM (istnieje okruch MOVED tego samego planu z
 *   `movedTo == X`, czyli trening przyszedł na X przesunięciem z jakiegoś W):
 *   ten okruch przejmuje nowy cel — `movedTo = Y` — a gdy Y jest jego WŁASNĄ
 *   datą (`W == Y`, trening wrócił do punktu wyjścia), okruch leci do kasacji.
 *   Na X nowy okruch NIE powstaje: przystanek nie zostawia śladu.
 * - X NIE było przystankiem (normalny dzień wzorca): powstaje zwykły okruch
 *   MOVED(X → Y), jak dotąd.
 *
 * Własności, które z tego wynikają (pokryte w `WeekPlannerTest`):
 * - X → Y → X kończy się ZEROWĄ liczbą okruchów (X znów ma PLANNED, Y jest puste);
 * - X → Y → Z kończy się JEDNYM okruchem MOVED(X → Z) (Y puste, Z ma PLANNED);
 * - pojedyncze X → Y zachowuje się dokładnie jak dotąd.
 *
 * Liczą się wyłącznie okruchy TEGO SAMEGO treningu: ten sam plan I ten sam
 * [ScheduleEntryRef.dayIndex]. Sam plan + data to za mało — na jednej dacie
 * potrafią spotkać się dwa różne dni tego samego planu („Full body A"
 * przesunięty na środę, na której od zawsze siedzi „Full body B"), a wtedy
 * przesunięcie B skleiłoby się z okruchem A i skasowało cudzy, poprawny
 * breadcrumb (złapane na emulatorze przy weryfikacji tej reguły).
 */
fun moveResolution(
    entries: List<ScheduleEntryRef>,
    movedEntry: ScheduleEntryRef,
    newDate: LocalDate,
): MoveResolution {
    val stopoverCrumbs = entries.filter {
        it.kind == ScheduleEntryKind.MOVED &&
            it.planId == movedEntry.planId &&
            it.dayIndex == movedEntry.dayIndex &&
            it.id != movedEntry.id &&
            it.movedTo == movedEntry.date
    }
    if (stopoverCrumbs.isEmpty()) {
        return MoveResolution(
            crumbIdsToDelete = emptyList(),
            crumbsToRedirect = emptyList(),
            createCrumbAtSource = true,
        )
    }
    val (returnedHome, redirected) = stopoverCrumbs.partition { it.date == newDate }
    return MoveResolution(
        crumbIdsToDelete = returnedHome.map { it.id },
        crumbsToRedirect = redirected.map { MovedCrumbRedirect(it.id, newDate) },
        createCrumbAtSource = false,
    )
}

/**
 * Wynik przeplanowania: id-ki starych wpisów PLANNED wybranego planu do
 * skasowania + nowe sloty do zapisania w ich miejsce.
 */
data class ReplanResult(val idsToDelete: List<String>, val slots: List<PlannedSlot>)

/**
 * Zabezpieczenie „pas i szelki" pod datę startu przeplanowania: nawet gdyby
 * UI ([ScheduleDatePickerDialog.minSelectableDate]) przepuściło datę sprzed
 * dziś, [planReplacement] nie ma prawa skasować przeszłych, niezaliczonych
 * (missed) wpisów PLANNED — clampujemy do `max(startDate, today)`.
 */
fun clampStartDateToToday(startDate: LocalDate, today: LocalDate = LocalDate.now()): LocalDate =
    if (startDate.isBefore(today)) today else startDate

/**
 * Horyzont materializacji (w tygodniach od [startDate]) dla planu Z BLOKIEM —
 * wejście `weeks` do [planReplacement]. [fullBlockWeeks] to PEŁNA długość
 * bloku (praca + tydzień lekki, `ProgressionEngine.fullBlockWeeks` na
 * `Plan.blockLengthWeeks` — ta funkcja przyjmuje już przeliczoną wartość, zero
 * zależności od modułu progresji tutaj).
 *
 * Pierwsze planowanie (plan bez ŻADNYCH wcześniejszych wpisów PLANNED,
 * [existingPlanDates] puste) dostaje dokładnie [fullBlockWeeks] tygodni.
 * Kolejne przeplanowanie NIE SKRACA horyzontu, który plan już miał: koniec
 * okna to `max(startDate + fullBlockWeeks, tydzień ostatniego istniejącego
 * wpisu planu)` — user mógł wcześniej zaplanować dalej, zmiana samego
 * wzorca dni nie ma prawa tego uciąć.
 */
fun blockReplanWeeks(
    startDate: LocalDate,
    fullBlockWeeks: Int,
    existingPlanDates: List<LocalDate>,
): Int {
    val minWeeks = fullBlockWeeks.coerceAtLeast(0)
    val lastEntryDate = existingPlanDates.maxOrNull() ?: return minWeeks
    // Koniec tygodnia (włącznie z niedzielą) ostatniego istniejącego wpisu,
    // jako granica wyłączna — poniedziałek tygodnia PO nim.
    val lastEntryWeekEndExclusive = weekStartOf(lastEntryDate).plusWeeks(1)
    val minEndExclusive = startDate.plusWeeks(minWeeks.toLong())
    val horizonEnd = maxOf(minEndExclusive, lastEntryWeekEndExclusive)
    val daysToHorizon = ChronoUnit.DAYS.between(startDate, horizonEnd)
    if (daysToHorizon <= 0) return minWeeks
    // Zaokrąglenie w górę do pełnych tygodni — [generatePlannedSlots] tnie
    // okno przez `endExclusive`, więc horyzont nie może wypaść za krótki.
    val weeksToHorizon = ((daysToHorizon + ScheduleConstants.DAYS_IN_WEEK - 1) / ScheduleConstants.DAYS_IN_WEEK).toInt()
    return maxOf(minWeeks, weeksToHorizon)
}

/**
 * Horyzont materializacji (w tygodniach od daty zapisu) przy zapisie edytora
 * planu ([com.stronk.ui.plans.PlanEditorViewModel.save]) — WEJŚCIE do
 * [planReplacement], celowo INNE niż [blockReplanWeeks] użyty w dialogu
 * planowania tygodnia.
 *
 * [blockReplanWeeks] nigdy nie SKRACA horyzontu, który plan już miał — słuszne
 * w dialogu, gdzie user zmienia tylko przypisania dni, nie samą długość bloku.
 * Zapis edytora jest inny: gdy user właśnie ZMIENIA `Plan.blockLengthWeeks`,
 * ta nowa wartość ma być autorytatywna — skrócenie bloku MUSI realnie uciąć
 * nadmiarowe wpisy poza nowym horyzontem (gate: blok 6→3 tyg. kończy wpisy na
 * 3 tyg.), a nie zostawić stary, dłuższy horyzont.
 *
 * [fullBlockWeeks] to już przeliczona pełna długość bloku
 * (`ProgressionEngine.fullBlockWeeks` na `Plan.blockLengthWeeks`) — `null` =
 * plan bez bloku, dostaje stałe [ScheduleConstants.GENERATION_WEEKS] jak przy
 * pierwszym przypisaniu planu bez bloku (rolling generation dociągnie resztę
 * samo, patrz [needsRollingExtension]).
 */
fun saveReplanWeeks(fullBlockWeeks: Int?): Int = fullBlockWeeks ?: ScheduleConstants.GENERATION_WEEKS

/**
 * Przeplanowanie [selectedPlanId]: WSZYSTKIE jego przyszłe (`date >=
 * [startDate]`, clampowanej do [today] — patrz [clampStartDateToToday]) wpisy
 * PLANNED lecą do kasacji — rolling generation mógł je nagenerować dalej niż
 * jedno okno [weeks], stąd „wszystkie", nie tylko okno generacji — a w ich
 * miejsce powstają nowe sloty wg [assignments].
 *
 * Nietykalne: wpisy DONE (dowolnego planu — historia treningu) i PLANNED
 * INNEGO, NIEZARCHIWIZOWANEGO planu blokują datę (nowy slot tam nie
 * powstaje), tak jak [conflictingOtherPlanEntry]/[buildOccupiedEntries]
 * blokują CTA w dialogu. SKIPPED nie blokuje i nie jest kasowany (odwołany
 * trening = dzień wraca do puli). Stare wpisy PLANNED TEGO SAMEGO planu nie
 * blokują nowych slotów — i tak trafiają do [ReplanResult.idsToDelete].
 *
 * AKTYWNE PRZESUNIĘCIA tego planu ([activeMovedSlots]) są respektowane w
 * całości — „przesunięty znaczy przesunięty":
 * - data ŹRÓDŁOWA jest zajęta → nie powstaje na niej nowy slot (bez tego na
 *   dacie z wpisem MOVED lądował DRUGI wpis PLANNED i dzień był naraz
 *   „Przesunięty na czwartek" i normalnym dniem treningowym);
 * - data DOCELOWA jest zajęta ORAZ leżący tam wpis PLANNED jest wyjęty z
 *   [ReplanResult.idsToDelete] — inaczej samo zablokowanie źródła zjadałoby
 *   trening w całości (źródło zablokowane, cel skasowany).
 *
 * MOVED INNEGO planu nie blokuje — to dzień wolny z punktu widzenia tego planu.
 *
 * PLANNED INNEGO, ZARCHIWIZOWANEGO planu ([ScheduleEntryRef.archived]) to
 * martwy wpis — nie blokuje daty (traktowany jak wolny dzień) I jeśli na jego
 * dacie powstaje nowy slot, wpis jest KASOWANY razem z resztą przeplanowania
 * (ta sama paczka [ReplanResult.idsToDelete]) — samonaprawa już zepsutego
 * stanu (user zarchiwizował plan, zanim istniało czyszczenie z [setArchived])
 * bez migracji danych. Martwy wpis, którego data NIE dostaje nowego slotu
 * (np. user wyzerował dany dzień tygodnia), zostaje nietknięty — posprząta go
 * ewentualna kolejna archiwizacja albo sweep przy starcie ekranu.
 */
fun planReplacement(
    currentEntries: List<ScheduleEntryRef>,
    selectedPlanId: String,
    assignments: Map<DayOfWeek, Int>,
    startDate: LocalDate,
    weeks: Int = ScheduleConstants.GENERATION_WEEKS,
    today: LocalDate = LocalDate.now(),
): ReplanResult {
    val effectiveStartDate = clampStartDateToToday(startDate, today)
    val movedSlots = activeMovedSlots(currentEntries, selectedPlanId, effectiveStartDate)
    val movedTargets = movedSlots.mapTo(HashSet()) { it.to }
    val ownIdsToDelete = currentEntries
        .filter {
            it.planId == selectedPlanId && it.kind == ScheduleEntryKind.PLANNED &&
                it.date >= effectiveStartDate && it.date !in movedTargets
        }
        .map { it.id }
    val occupied = currentEntries
        .filter {
            it.kind == ScheduleEntryKind.DONE ||
                (it.kind == ScheduleEntryKind.PLANNED && it.planId != selectedPlanId && !it.archived)
        }
        .map { it.date }
        .toSet() + movedSlots.map { it.from } + movedTargets
    val slots = generatePlannedSlots(assignments, effectiveStartDate, weeks, occupied)
    val newSlotDates = slots.mapTo(HashSet()) { it.date }
    val deadIdsToDelete = currentEntries
        .filter { it.kind == ScheduleEntryKind.PLANNED && it.archived && it.date in newSlotDates }
        .map { it.id }
    return ReplanResult(ownIdsToDelete + deadIdsToDelete, slots)
}

// ---------- walidacja kolizji planów ----------

/**
 * Buduje wejście do [conflictingOtherPlanEntry] z [refs]: DONE zajmuje zawsze
 * (historia treningu — archiwizacja jej nie kasuje, patrz KDoc
 * [ScheduleEntryRef.archived]), PLANNED zajmuje TYLKO gdy jego plan NIE jest
 * zarchiwizowany. PLANNED zarchiwizowanego planu to martwy wpis (plan
 * zarchiwizowany, wpis w `schedule` przetrwał) — traktowany jak wolny dzień,
 * ten sam podział źródła prawdy co w [planReplacement]. [planNameOf] dociąga
 * nazwę planu po id (może zniknąć — wołający decyduje o fallbacku).
 */
fun buildOccupiedEntries(refs: List<ScheduleEntryRef>, planNameOf: (String) -> String): List<OccupiedEntry> =
    refs
        .filter { it.kind == ScheduleEntryKind.DONE || (it.kind == ScheduleEntryKind.PLANNED && !it.archived) }
        .map { OccupiedEntry(it.date, it.planId, planNameOf(it.planId)) }

/**
 * Pierwszy (najwcześniejszy) wpis INNEGO planu w oknie [startDate, startDate +
 * [weeks]) — sygnał blokujący CTA w [AssignPlanDialog]. Cel: na jeden okres tylko
 * jeden plan, więc sprawdzamy CAŁE okno, nie tylko konkretne dni przypisania.
 *
 * `null` = okno wolne od kolizji z innym planem (może być częściowo zajęte przez
 * TEN SAM plan — to nie blokuje CTA, patrz [ScheduleViewModel.onAssignPlan]).
 * Wejście [occupied] typowo z [buildOccupiedEntries] — martwe wpisy
 * zarchiwizowanych planów są tam już odfiltrowane, więc ta funkcja o
 * archiwizacji nic nie musi wiedzieć.
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
