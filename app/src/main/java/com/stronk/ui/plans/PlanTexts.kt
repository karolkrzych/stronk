package com.stronk.ui.plans

import com.stronk.data.ComplianceResult
import com.stronk.data.Plan
import com.stronk.data.PlanExercise
import com.stronk.data.SetTarget
import com.stronk.data.StressLevel
import com.stronk.progression.ProgressionConstants
import com.stronk.ui.PlLabels
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Etykiety i formatowanie tekstów modułu planów. PlLabels jest read-only —
 * brakujące tam etykiety (poziomy obciążenia stawów) żyją lokalnie tutaj.
 */
internal object PlanTexts {

    private val polish = Locale.forLanguageTag("pl")

    /**
     * Sama liczba kilogramów, bez jednostki — do dużych liczb w kafelkach statystyk,
     * gdzie „kg” jest osobnym elementem. Np. "80", "82,5" (bez zbędnego ",0").
     */
    fun kgValue(kg: Double): String {
        val rounded = (kg * 10).roundToLong() / 10.0
        return if (rounded % 1.0 == 0.0) {
            rounded.toLong().toString()
        } else {
            "%.1f".format(polish, rounded)
        }
    }

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

    /** Skrót celu ćwiczenia, np. "3×8", "3×60 s", "3× 1 km / 6:00". */
    fun targetLabel(exercise: PlanExercise): String = when (val target = exercise.target) {
        is SetTarget.WeightReps -> "${exercise.sets}×${target.reps}"
        is SetTarget.Reps -> "${exercise.sets}×${target.reps}"
        is SetTarget.Time -> "${exercise.sets}×${target.seconds} s"
        is SetTarget.DistanceTime ->
            "${exercise.sets}× ${metersLabel(target.meters)} / ${minutesLabel(target.seconds)}"
    }

    /** Podsumowanie planu na liście, np. "3 dni · 17 ćwiczeń · blok 5 tyg. + 1 lekki". */
    fun planSummary(plan: Plan): String {
        val exerciseCount = plan.days.sumOf { it.exercises.size }
        return "${plan.days.size} ${dayWord(plan.days.size)} · " +
            "$exerciseCount ${exerciseWord(exerciseCount)} · " +
            "blok ${plan.blockLengthWeeks} tyg. + ${ProgressionConstants.BLOCK_LIGHT_WEEKS} lekki"
    }

    fun metersLabel(meters: Double): String {
        val rounded = meters.roundToInt()
        return if (rounded >= 1000 && rounded % 1000 == 0) "${rounded / 1000} km" else "$rounded m"
    }

    fun minutesLabel(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

    private fun dayWord(count: Int): String = if (count == 1) "dzień" else "dni"

    private fun exerciseWord(count: Int): String = when {
        count == 1 -> "ćwiczenie"
        count % 10 in 2..4 && count % 100 !in 12..14 -> "ćwiczenia"
        else -> "ćwiczeń"
    }
}
