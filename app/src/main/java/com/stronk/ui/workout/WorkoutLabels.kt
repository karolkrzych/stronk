package com.stronk.ui.workout

import com.stronk.data.ExerciseState
import com.stronk.data.SetLog
import com.stronk.data.SetTarget
import com.stronk.progression.ExerciseProposal
import kotlin.math.roundToInt

/**
 * Formatowanie wartości treningowych do UI i powiadomienia (po polsku,
 * przecinek dziesiętny). Czyste funkcje — testowalne bez Androida.
 */
object WorkoutLabels {

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

    /** Wartość serii/prefillu: "60 kg × 8", "12 powt.", "45 s", "1 km · 5:00". */
    fun setValue(set: SetLog): String = when (set) {
        is SetLog.WeightReps -> "${kg(set.kg)} kg × ${set.reps}"
        is SetLog.Reps ->
            if (set.extraKg != null) "${set.reps} powt. (+${kg(set.extraKg!!)} kg)"
            else "${set.reps} powt."
        is SetLog.Time -> seconds(set.seconds)
        is SetLog.DistanceTime -> "${meters(set.meters)} · ${countdown(set.seconds)}"
    }

    /** Cel ćwiczenia w wierszu listy: "3×8 · 60 kg", "3×12", "3×45 s", "1 km · 5:00". */
    fun proposalTarget(proposal: ExerciseProposal): String = when (val t = proposal.target) {
        is SetTarget.WeightReps -> buildString {
            append("${proposal.sets}×${t.reps}")
            proposal.weightKg?.let { append(" · ${kg(it)} kg") }
        }
        is SetTarget.Reps -> "${proposal.sets}×${t.reps}"
        is SetTarget.Time -> "${proposal.sets}×${seconds(t.seconds)}"
        is SetTarget.DistanceTime -> {
            val base = "${meters(t.meters)} · ${countdown(t.seconds)}"
            if (proposal.sets > 1) "${proposal.sets}× $base" else base
        }
    }

    /** "Ostatnio: 60 kg × 8, 8, 7" z serii roboczych ostatniego treningu; null bez historii. */
    fun lastTime(state: ExerciseState?): String? {
        val working = state?.lastSets.orEmpty().filterNot { it.isWarmup }
        if (working.isEmpty()) return null
        val summary = when (working.first()) {
            is SetLog.WeightReps -> {
                val sets = working.filterIsInstance<SetLog.WeightReps>()
                "${kg(sets.maxOf { it.kg })} kg × ${sets.joinToString(", ") { "${it.reps}" }}"
            }
            is SetLog.Reps ->
                working.filterIsInstance<SetLog.Reps>().joinToString(", ") { "${it.reps}" } + " powt."
            is SetLog.Time ->
                working.filterIsInstance<SetLog.Time>().joinToString(", ") { seconds(it.seconds) }
            is SetLog.DistanceTime -> setValue(working.first())
        }
        return "Ostatnio: $summary"
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
}
