package com.stronk.progression

import com.stronk.data.GoalDefaults
import com.stronk.data.TrainingGoal

/**
 * Kalibracja ciężarów startowych z treningu testowego — czyste funkcje, zero
 * Androida i zero Firestore.
 *
 * Flow: user robi jedną serię testową (np. 40 kg × 10) → estymujemy 1RM wzorem
 * Epleya → bierzemy z niego procent zależny od celu (`GoalParams.calibrationPercent`)
 * → wychodzi CIĘŻAR ROBOCZY, który ląduje w planie jako
 * `PlanExercise.startWeightKg`. Dalej ćwiczenie prowadzi [ProgressionEngine]
 * dokładnie tak jak dotąd (m.in. ramp-up po przerwie startuje od 55 % tego
 * ciężaru roboczego, nie od surowego ciężaru testu).
 *
 * Estymacja Epleya jest wiarygodna tylko dla sensownego zakresu powtórzeń
 * ([RELIABLE_REPS]) — poza nim UI ostrzega, ale nie blokuje.
 */
object Calibration {

    /** Zakres powtórzeń, w którym estymacja Epleya jest wiarygodna (poza nim UI ostrzega, nie blokuje). */
    val RELIABLE_REPS: IntRange = 2..12

    /** Dzielnik wzoru Epleya: e1RM = w · (1 + r/30). */
    private const val EPLEY_DIVISOR = 30.0

    /**
     * Estymowane 1RM wzorem Epleya: `e1RM = w · (1 + r/30)`. Dla r = 1 zwraca
     * dokładnie [weightKg] (seria pojedyncza JEST maksem).
     *
     * @param weightKg ciężar serii testowej, musi być dodatni
     * @param reps liczba powtórzeń serii testowej, minimum 1
     */
    fun estimateOneRepMax(weightKg: Double, reps: Int): Double {
        require(weightKg > 0) { "Ciężar testowy musi być dodatni, było: $weightKg" }
        require(reps >= 1) { "Liczba powtórzeń musi być >= 1, było: $reps" }
        // Seria pojedyncza JEST maksem — wzór Epleya podbiłby ją o 1/30, co nie ma sensu.
        if (reps == 1) return weightKg
        return weightKg * (1 + reps / EPLEY_DIVISOR)
    }

    /**
     * Ciężar roboczy z serii testowej: estymowane 1RM × procent celu
     * (`null` = cel niewybrany → [GoalDefaults.FALLBACK]), zaokrąglony do 2,5 kg
     * tak samo jak propozycje silnika progresji.
     *
     * Wynik jest tym, co zapisujemy jako `PlanExercise.startWeightKg`.
     */
    fun workingWeightKg(testWeightKg: Double, testReps: Int, goal: TrainingGoal?): Double {
        val oneRepMax = estimateOneRepMax(testWeightKg, testReps)
        return ProgressionEngine.roundWeight(oneRepMax * GoalDefaults.calibrationPercentFor(goal))
    }

    /** Czy liczba powtórzeń testu daje wiarygodną estymację (podpowiedź w UI, nie walidacja). */
    fun isReliable(testReps: Int): Boolean = testReps in RELIABLE_REPS
}
