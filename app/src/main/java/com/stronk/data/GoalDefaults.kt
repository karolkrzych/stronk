package com.stronk.data

/**
 * Parametry treningu wynikające z celu ([TrainingGoal]) — jedno źródło prawdy dla
 * presetów planów, sugestii w edytorze i domyślnej długości przerwy w treningu.
 *
 * Czyste stałe, zero zależności od Androida: silnik progresji i UI mogą to czytać
 * bez ceregieli. Wartości są celowo konserwatywne — apka ma pomagać wrócić do
 * formy, nie budować rekordów świata.
 */
data class GoalParams(
    /** Zakres powtórzeń roboczych dla ćwiczeń złożonych. */
    val repRange: IntRange,
    /** Domyślna liczba powtórzeń (ćwiczenie złożone) — środek [repRange]. */
    val defaultReps: Int,
    /** Domyślna liczba powtórzeń dla ćwiczeń izolowanych/akcesoryjnych. */
    val accessoryReps: Int,
    /** Domyślna liczba serii roboczych. */
    val defaultSets: Int,
    /** Domyślna przerwa między seriami w sekundach. */
    val restSeconds: Int,
)

/**
 * Domyślne parametry per cel treningowy.
 *
 * | cel               | powtórzenia | serie | przerwa |
 * |-------------------|-------------|-------|---------|
 * | SIŁA              | 4–6         | 4     | 180 s   |
 * | MASA              | 8–12        | 3     | 90 s    |
 * | POWRÓT DO FORMY   | 10–15       | 3     | 75 s    |
 */
object GoalDefaults {

    /** Siła: ciężko, mało powtórzeń, długa przerwa. */
    val STRENGTH = GoalParams(
        repRange = 4..6,
        defaultReps = 5,
        accessoryReps = 8,
        defaultSets = 4,
        restSeconds = 180,
    )

    /** Masa: klasyczna hipertrofia. */
    val MASS = GoalParams(
        repRange = 8..12,
        defaultReps = 10,
        accessoryReps = 12,
        defaultSets = 3,
        restSeconds = 90,
    )

    /** Powrót do formy: lekko, dużo powtórzeń, krótka przerwa — technika przed ciężarem. */
    val RETURN_TO_FORM = GoalParams(
        repRange = 10..15,
        defaultReps = 12,
        accessoryReps = 15,
        defaultSets = 3,
        restSeconds = 75,
    )

    /** Parametry używane, gdy profil nie ma jeszcze wybranego celu. */
    val FALLBACK = MASS

    /** Parametry dla celu; `null` (cel niewybrany) → [FALLBACK]. */
    fun forGoal(goal: TrainingGoal?): GoalParams = when (goal) {
        TrainingGoal.STRENGTH -> STRENGTH
        TrainingGoal.MASS -> MASS
        TrainingGoal.RETURN_TO_FORM -> RETURN_TO_FORM
        null -> FALLBACK
    }

    /** Parametry wprost z profilu użytkownika. */
    fun forProfile(profile: UserProfile?): GoalParams = forGoal(profile?.profile?.goal)

    /** Domyślne powtórzenia; [accessory] = ćwiczenie izolowane/akcesoryjne. */
    fun repsFor(goal: TrainingGoal?, accessory: Boolean = false): Int =
        forGoal(goal).let { if (accessory) it.accessoryReps else it.defaultReps }

    /** Domyślna liczba serii roboczych. */
    fun setsFor(goal: TrainingGoal?): Int = forGoal(goal).defaultSets

    /** Domyślna przerwa między seriami w sekundach. */
    fun restSecondsFor(goal: TrainingGoal?): Int = forGoal(goal).restSeconds

    /** Wciąga liczbę powtórzeń w zakres celu — do sugestii, nie do twardej walidacji. */
    fun clampReps(goal: TrainingGoal?, reps: Int): Int = reps.coerceIn(forGoal(goal).repRange)

    /** Czy liczba powtórzeń mieści się w zakresie celu (podpowiedź w edytorze). */
    fun repsInRange(goal: TrainingGoal?, reps: Int): Boolean = reps in forGoal(goal).repRange

    /** Zakres powtórzeń do pokazania użytkownikowi, np. "4–6" (półpauza, nie myślnik). */
    fun repRangeLabel(goal: TrainingGoal?): String =
        forGoal(goal).repRange.let { "${it.first}–${it.last}" }

    /** Polska nazwa celu — jedno miejsce, żeby ekrany nie wymyślały własnych. */
    fun label(goal: TrainingGoal): String = when (goal) {
        TrainingGoal.STRENGTH -> "Siła"
        TrainingGoal.MASS -> "Masa"
        TrainingGoal.RETURN_TO_FORM -> "Powrót do formy"
    }

    /** Jednozdaniowy opis celu pod nazwą (wybór celu w profilu, kreator planu). */
    fun description(goal: TrainingGoal): String = when (goal) {
        TrainingGoal.STRENGTH -> "Mało powtórzeń, długie przerwy, duży ciężar"
        TrainingGoal.MASS -> "Średnie powtórzenia, umiarkowane przerwy"
        TrainingGoal.RETURN_TO_FORM -> "Lekko i technicznie, krótkie przerwy"
    }
}
