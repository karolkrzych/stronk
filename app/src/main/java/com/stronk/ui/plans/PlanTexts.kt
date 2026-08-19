package com.stronk.ui.plans

import com.stronk.data.ComplianceResult
import com.stronk.data.Plan
import com.stronk.data.PlanExercise
import com.stronk.data.SetTarget
import com.stronk.data.StressLevel
import com.stronk.progression.ProgressionConstants
import com.stronk.ui.PlLabels
import kotlin.math.roundToInt

/**
 * Etykiety i formatowanie tekstów modułu planów. PlLabels jest read-only —
 * brakujące tam etykiety (poziomy obciążenia stawów) żyją lokalnie tutaj.
 */
internal object PlanTexts {

    fun stressLevel(level: StressLevel): String = when (level) {
        StressLevel.HIGH -> "wysokie"
        StressLevel.MEDIUM -> "średnie"
        StressLevel.LOW -> "niskie"
        StressLevel.NONE -> "brak"
    }

    /** Lista problemów zgodności z profilem do pokazania pod ćwiczeniem. */
    fun complianceIssues(compliance: ComplianceResult): List<String> = buildList {
        compliance.constraintViolations.forEach { violation ->
            add("obciąża: ${PlLabels.joint(violation.joint)} (${stressLevel(violation.exerciseStress)})")
        }
        if (!compliance.equipmentAvailable) {
            add("brak sprzętu w profilu")
        }
    }

    /**
     * SAMA liczba serii — do kolumny pod kapitalikiem „SERIE".
     *
     * Zakaz Karola: nigdy nie sklejaj frazy „3×10" ani „40 kg × 10 powt.".
     * Serie i cel to dwie osobne wartości; nagłówki kolumn stoją RAZ nad sekcją.
     */
    fun setsValue(exercise: PlanExercise): String = exercise.sets.toString()

    /**
     * SAMA wartość celu jednej serii — do kolumny pod kapitalikiem „CEL":
     * powtórzenia („10"), czas („60 s") albo dystans z czasem („1 km · 6:00").
     */
    fun targetValue(exercise: PlanExercise): String = when (val target = exercise.target) {
        is SetTarget.WeightReps -> target.reps.toString()
        is SetTarget.Reps -> target.reps.toString()
        is SetTarget.Time -> "${target.seconds} s"
        is SetTarget.DistanceTime ->
            "${metersLabel(target.meters)} · ${minutesLabel(target.seconds)}"
    }

    /** Nagłówek kolumny celu — zależy od typu pomiaru, więc liczba pod nim jest naga. */
    fun targetColumnLabel(exercise: PlanExercise): String = when (exercise.target) {
        is SetTarget.WeightReps, is SetTarget.Reps -> "Powt."
        is SetTarget.Time -> "Czas"
        is SetTarget.DistanceTime -> "Dystans"
    }

    /**
     * Chip liczby serii na liście ćwiczeń dnia (mock: „3 serie"). To ETYKIETA
     * z odmienionym rzeczownikiem, nie fraza „wartość × wartość".
     */
    fun setsChip(sets: Int): String = when {
        sets == 1 -> "1 seria"
        sets % 10 in 2..4 && sets % 100 !in 12..14 -> "$sets serie"
        else -> "$sets serii"
    }

    /** Pełna długość bloku razem z tygodniem lekkim (ADR-004) — do statu TYGODNIE. */
    fun fullBlockWeeks(plan: Plan): Int =
        plan.blockLengthWeeks + ProgressionConstants.BLOCK_LIGHT_WEEKS

    /** Liczba ćwiczeń we wszystkich dniach planu — do statu ĆWICZENIA. */
    fun exerciseCount(plan: Plan): Int = plan.days.sumOf { it.exercises.size }

    fun metersLabel(meters: Double): String {
        val rounded = meters.roundToInt()
        return if (rounded >= 1000 && rounded % 1000 == 0) "${rounded / 1000} km" else "$rounded m"
    }

    fun minutesLabel(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)
}
