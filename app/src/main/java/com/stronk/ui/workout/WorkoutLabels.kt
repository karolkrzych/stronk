package com.stronk.ui.workout

import com.stronk.data.ExerciseState
import com.stronk.data.SetLog
import com.stronk.data.SetTarget
import com.stronk.progression.Calibration
import com.stronk.progression.ExerciseProposal
import kotlin.math.roundToInt

/**
 * Jedna wartość treningowa jako OSOBNY byt (twarda zasada Karola): KAPITALIK
 * z nazwą, pod nim sama liczba, jednostka jako mały sufiks.
 *
 * W apce NIE MA fraz typu „32,5 kg × 12 powt." ani „3×8" — para wartości to
 * zawsze dwa stat-bloki obok siebie (`StronkStatBlock` + `StronkStatDivider`)
 * albo dwie kolumny z nagłówkiem raz na sekcję.
 *
 * @param label nazwa wartości podana normalnie; wersaliki robi komponent
 * @param value sama liczba, np. "32,5" albo "8, 7" (lista powtórzeń serii)
 * @param unit sufiks jednostki, np. "kg"; null = liczba bez jednostki
 */
data class SetStat(
    val label: String,
    val value: String,
    val unit: String? = null,
)

/**
 * Formatowanie wartości treningowych do UI i powiadomienia (po polsku,
 * przecinek dziesiętny). Czyste funkcje — testowalne bez Androida.
 *
 * Ekrany biorą stąd [SetStat]-y i renderują je jako stat-bloki. Jedyny wyjątek
 * to [setValue]: jednolinijkowa etykieta systemowa (powiadomienie), gdzie
 * stat-bloki nie istnieją — dlatego zwraca JEDNĄ wartość wiodącą z jednostką,
 * a nie sklejoną parę.
 */
object WorkoutLabels {

    // --- nazwy wartości (jedno miejsce zmian, spójne z mockami) ---

    const val LABEL_WEIGHT = "Ciężar"
    const val LABEL_REPS = "Powtórzenia"

    /** Skrócona wersja dla ciasnych kontekstów (pola dialogu edycji). */
    const val LABEL_REPS_SHORT = "Powt."
    const val LABEL_TIME = "Czas"
    const val LABEL_DISTANCE = "Dystans"
    const val LABEL_EXTRA_WEIGHT = "Dociążenie"
    const val LABEL_SETS = "Serie"

    /** "60" / "62,5" — liczba bez zbędnych zer, z przecinkiem dziesiętnym. */
    private fun decimal(value: Double): String {
        val rounded = (value * 100).roundToInt() / 100.0
        return if (rounded % 1.0 == 0.0) rounded.toInt().toString()
        else rounded.toString().replace('.', ',')
    }

    fun kg(value: Double): String = decimal(value)

    /** "800 m" / "2 km" / "2,5 km". */
    fun meters(value: Double): String {
        val m = value.roundToInt()
        if (m < 1000) return "$m m"
        return "${decimal(m / 1000.0)} km"
    }

    /** "1:30" — zegar minut:sekund (odliczanie przerwy, czasy cardio). */
    fun countdown(totalSeconds: Int): String {
        val clamped = totalSeconds.coerceAtLeast(0)
        return "%d:%02d".format(clamped / 60, clamped % 60)
    }

    /** "45 s" dla krótkich czasów, "1:30" od minuty w górę. */
    fun seconds(value: Int): String = if (value < 60) "$value s" else countdown(value)

    // ------------------------------------------------------------ stat-bloki

    /** Dystans rozbity na liczbę i jednostkę ("800" + "m" / "2,5" + "km"). */
    private fun distanceStat(value: Double): SetStat {
        val m = value.roundToInt()
        return if (m < 1000) SetStat(LABEL_DISTANCE, "$m", "m")
        else SetStat(LABEL_DISTANCE, decimal(m / 1000.0), "km")
    }

    /** Czas rozbity na liczbę i jednostkę ("45" + "s" / "1:30" bez jednostki). */
    private fun timeStat(value: Int): SetStat =
        if (value < 60) SetStat(LABEL_TIME, "$value", "s")
        else SetStat(LABEL_TIME, countdown(value), null)

    /**
     * Wartości serii/prefillu jako osobne staty — pierwsza pozycja to dominanta
     * ekranu (HERO), kolejne idą mniejsze. Dla WEIGHT_REPS: CIĘŻAR + POWTÓRZENIA.
     */
    fun setStats(set: SetLog): List<SetStat> = when (set) {
        is SetLog.WeightReps -> listOf(
            SetStat(LABEL_WEIGHT, kg(set.kg), "kg"),
            SetStat(LABEL_REPS, "${set.reps}"),
        )

        is SetLog.Reps -> buildList {
            add(SetStat(LABEL_REPS, "${set.reps}"))
            set.extraKg?.let { add(SetStat(LABEL_EXTRA_WEIGHT, kg(it), "kg")) }
        }

        is SetLog.Time -> listOf(timeStat(set.seconds))

        is SetLog.DistanceTime -> listOf(distanceStat(set.meters), timeStat(set.seconds))
    }

    /**
     * Cel ćwiczenia w szczegółach (arkusz za ikoną „i"): SERIE + POWTÓRZENIA
     * + ewentualnie CIĘŻAR. Nigdy jako fraza „3×8 · 60 kg".
     */
    fun targetStats(proposal: ExerciseProposal): List<SetStat> = buildList {
        add(SetStat(LABEL_SETS, "${proposal.sets}"))
        when (val t = proposal.target) {
            is SetTarget.WeightReps -> {
                add(SetStat(LABEL_REPS, "${t.reps}"))
                proposal.weightKg?.let { add(SetStat(LABEL_WEIGHT, kg(it), "kg")) }
            }

            is SetTarget.Reps -> add(SetStat(LABEL_REPS, "${t.reps}"))

            is SetTarget.Time -> add(timeStat(t.seconds))

            is SetTarget.DistanceTime -> {
                add(distanceStat(t.meters))
                add(timeStat(t.seconds))
            }
        }
    }

    /**
     * Ostatni trening z tym ćwiczeniem jako staty: CIĘŻAR (maks z serii
     * roboczych) i POWTÓRZENIA kolejnych serii ("8, 7"). Pusta lista = brak
     * historii. Serie rozgrzewkowe nigdy się nie liczą.
     */
    fun lastStats(state: ExerciseState?): List<SetStat> {
        val working = state?.lastSets.orEmpty().filterNot { it.isWarmup }
        if (working.isEmpty()) return emptyList()
        return when (working.first()) {
            is SetLog.WeightReps -> {
                val sets = working.filterIsInstance<SetLog.WeightReps>()
                listOf(
                    SetStat(LABEL_WEIGHT, kg(sets.maxOf { it.kg }), "kg"),
                    SetStat(LABEL_REPS, sets.joinToString(", ") { "${it.reps}" }),
                )
            }

            is SetLog.Reps -> listOf(
                SetStat(
                    LABEL_REPS,
                    working.filterIsInstance<SetLog.Reps>().joinToString(", ") { "${it.reps}" },
                ),
            )

            is SetLog.Time -> listOf(
                SetStat(
                    LABEL_TIME,
                    working.filterIsInstance<SetLog.Time>().joinToString(", ") { seconds(it.seconds) },
                ),
            )

            is SetLog.DistanceTime -> setStats(working.first())
        }
    }

    /**
     * WARTOŚĆ WIODĄCA serii — jedna liczba z jednostką ("60 kg", "10 powt.",
     * "45 s", "1 km").
     *
     * WYŁĄCZNIE do jednolinijkowych etykiet systemowych (akcja „✓ Zalicz serię"
     * w powiadomieniu), gdzie nie da się postawić stat-bloków. W UI używaj
     * [setStats] — sklejanie pary wartości w jedną frazę jest zakazane.
     */
    fun setValue(set: SetLog): String = when (set) {
        is SetLog.WeightReps -> "${kg(set.kg)} kg"
        is SetLog.Reps -> "${set.reps} powt."
        is SetLog.Time -> seconds(set.seconds)
        is SetLog.DistanceTime -> meters(set.meters)
    }

    /** "1 seria" / "3 serie" / "5 serii" — polska odmiana liczby serii. */
    fun setCount(count: Int): String {
        val units = count % 10
        val teens = count % 100 in 12..14
        return when {
            count == 1 -> "1 seria"
            units in 2..4 && !teens -> "$count serie"
            else -> "$count serii"
        }
    }

    /** Plakietki modyfikatorów propozycji (silnik progresji, ADR-004). */
    fun proposalBadges(proposal: ExerciseProposal): List<String> = buildList {
        if (proposal.isLightWeek) add("tydzień lekki −40%")
        if (proposal.isReactiveDeload) add("deload −10%")
        if (proposal.isRampUp) add("ramp-up")
    }

    // ------------------------------------------------------------- kalibracja

    /** Etykieta serii testowej — pierwsza seria ćwiczenia o nieznanym ciężarze. */
    const val CALIBRATION_LABEL = "Seria testowa"

    /** Nagłówek karty z wynikiem kalibracji. */
    const val CALIBRATION_TITLE = "Kalibracja"

    /** KAPITALIK nad estymowanym maksem. */
    const val LABEL_ONE_REP_MAX = "Szac. 1RM"

    /** KAPITALIK nad ciężarem roboczym wyliczonym z serii testowej. */
    const val LABEL_WORKING_WEIGHT = "Ciężar roboczy"

    /**
     * Dolna granica PORADY treningowej do serii testowej — poniżej tylu powtórzeń
     * test robi się siłowy i mniej bezpieczny. Górną granicę bierzemy wprost z
     * [Calibration.RELIABLE_REPS], żeby copy nie rozjechało się ze stałą.
     */
    private const val CALIBRATION_ADVICE_MIN_REPS = 5

    /** Jedno zdanie instrukcji do serii testowej, np. "Dobierz ciężar na maks. 5–12 powtórzeń". */
    val CALIBRATION_HINT: String =
        "Dobierz ciężar na maks. $CALIBRATION_ADVICE_MIN_REPS–${Calibration.RELIABLE_REPS.last} powtórzeń"

    /** Zakres wiarygodności estymacji, np. "2–12" (półpauza, nie myślnik). */
    fun reliableRepsLabel(): String =
        "${Calibration.RELIABLE_REPS.first}–${Calibration.RELIABLE_REPS.last}"

    /** Delikatne ostrzeżenie przy wpisywaniu testu; null = wszystko gra. */
    fun calibrationRepsWarning(reps: Int): String? =
        if (reps in Calibration.RELIABLE_REPS) null
        else "Najpewniejszy szacunek wychodzi z ${reliableRepsLabel()} powtórzeń — " +
            "policzę go tak czy siak."

    /** Ten sam fakt przy gotowym wyniku; null = test był w wiarygodnym zakresie. */
    fun calibrationRepsNote(reps: Int): String? =
        if (reps in Calibration.RELIABLE_REPS) null
        else "Test poza zakresem ${reliableRepsLabel()} powtórzeń — szacunek orientacyjny."

    /**
     * Wynik kalibracji jako OSOBNE staty (zakaz fraz typu „53 kg → 32,5 kg"):
     * SZAC. 1RM i CIĘŻAR ROBOCZY stoją obok siebie jak każda inna para wartości.
     */
    fun calibrationStats(c: CalibrationResult): List<SetStat> = listOf(
        SetStat(LABEL_ONE_REP_MAX, kg(c.estimatedOneRepMaxKg), "kg"),
        SetStat(LABEL_WORKING_WEIGHT, kg(c.workingWeightKg), "kg"),
    )

    /** Plakietki ćwiczenia po kalibracji (widoczne do końca treningu). */
    fun calibrationBadges(c: CalibrationResult?): List<String> = buildList {
        if (c == null) return@buildList
        add("kalibracja")
        if (c.isRampUp) add("ramp-up")
    }
}
