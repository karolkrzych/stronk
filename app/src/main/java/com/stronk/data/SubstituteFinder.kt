package com.stronk.data

/**
 * Zamienniki ćwiczeń po tagach — czyste funkcje (bez Androida) współdzielone
 * przez kreator planów i tryb treningu (CONCEPT moduł 3 i 5, ADR-005 pkt 6).
 */

/** Naruszenie limitu obciążenia stawu z profilu; klucz stawu jak w [JointStress.all]. */
data class ConstraintViolation(
    val joint: String,
    val exerciseStress: StressLevel,
    val maxAccepted: StressLevel,
)

/** Wynik sprawdzenia ćwiczenia pod profil — czytelne pola do flagowania w UI. */
data class ComplianceResult(
    val constraintViolations: List<ConstraintViolation>,
    val equipmentAvailable: Boolean,
) {
    val isFullyCompliant: Boolean
        get() = constraintViolations.isEmpty() && equipmentAvailable
}

/** Pozycja rankingu zamienników; [warnings] = naruszenia limitów stawów do oflagowania. */
data class SubstituteMatch(
    val exercise: Exercise,
    val score: Double,
    val warnings: List<ConstraintViolation>,
)

/** Wszystkie wagi i progi rankingu w jednym miejscu — zero magic numbers w logice. */
object SubstituteScoring {
    /** Waga pokrycia partii głównych (mnożona przez ułamek pokrycia 0..1). */
    const val PRIMARY_MUSCLE_WEIGHT = 100.0
    const val SAME_MECHANIC_BONUS = 15.0
    const val SAME_CATEGORY_BONUS = 10.0
    const val SAME_LEVEL_BONUS = 5.0

    /**
     * Kara za każde naruszenie limitu stawu. Celowo większa niż suma wszystkich
     * bonusów, więc każdy zgodny zamiennik wyprzedza każdy naruszający.
     */
    const val CONSTRAINT_VIOLATION_PENALTY = 1000.0

    /** Sprzęt uznawany za zawsze dostępny, nawet gdy nie ma go w profilu. */
    val ALWAYS_AVAILABLE_EQUIPMENT = setOf("body only")

    const val DEFAULT_LIMIT = 10

    /** Przekazywane do [findSubstitutes] jako `limit`, gdy chcemy PEŁną listę kandydatów. */
    const val NO_LIMIT = Int.MAX_VALUE
}

/** Porządek dotkliwości — jawny, bo ordinal enuma [StressLevel] idzie od HIGH do NONE. */
private val StressLevel.severity: Int
    get() = when (this) {
        StressLevel.NONE -> 0
        StressLevel.LOW -> 1
        StressLevel.MEDIUM -> 2
        StressLevel.HIGH -> 3
    }

/**
 * Sprawdza ćwiczenie pod limity stawów i sprzęt z profilu.
 *
 * Naruszenie = obciążenie stawu w ćwiczeniu przekracza maksymalny akceptowany
 * poziom z `profile.constraints`. Pusta lista sprzętu w profilu oznacza
 * "sprzęt nieskonfigurowany" → wszystko dostępne; ćwiczenia bez sprzętu
 * (null / "body only") są dostępne zawsze.
 */
fun isCompliant(exercise: Exercise, profile: ProfileDetails): ComplianceResult {
    val violations = profile.constraints.mapNotNull { (joint, maxAccepted) ->
        val stress = exercise.jointStress.all[joint] ?: return@mapNotNull null
        if (stress.severity > maxAccepted.severity) {
            ConstraintViolation(joint = joint, exerciseStress = stress, maxAccepted = maxAccepted)
        } else {
            null
        }
    }
    val equipmentAvailable = exercise.equipment == null ||
        exercise.equipment in SubstituteScoring.ALWAYS_AVAILABLE_EQUIPMENT ||
        profile.equipment.isEmpty() ||
        exercise.equipment in profile.equipment
    return ComplianceResult(violations, equipmentAvailable)
}

/**
 * Ranking zamienników dla [exercise] spośród [allExercises]:
 * - kandydat musi dzielić co najmniej jedną partię główną, inaczej odpada,
 * - samo [exercise] jest wykluczone,
 * - niedostępny sprzęt DYSKWALIFIKUJE (zamiennik ma być wykonalny od ręki),
 * - naruszenia limitów stawów nie dyskwalifikują, ale kara
 *   [SubstituteScoring.CONSTRAINT_VIOLATION_PENALTY] spycha je za wszystkie
 *   zgodne propozycje; naruszenia wracają w [SubstituteMatch.warnings],
 * - bonusy za ten sam mechanic (tylko gdy znany), kategorię i poziom.
 *
 * Wynik posortowany malejąco po score, remisy po namePl (deterministycznie).
 */
fun findSubstitutes(
    exercise: Exercise,
    allExercises: List<Exercise>,
    profile: ProfileDetails,
    limit: Int = SubstituteScoring.DEFAULT_LIMIT,
): List<SubstituteMatch> {
    val originalPrimaries = exercise.primaryMuscles.toSet()
    if (originalPrimaries.isEmpty() || limit <= 0) return emptyList()

    return allExercises.asSequence()
        .filter { it.id != exercise.id }
        .mapNotNull { candidate ->
            val sharedPrimaries = candidate.primaryMuscles.toSet().count { it in originalPrimaries }
            if (sharedPrimaries == 0) return@mapNotNull null
            val compliance = isCompliant(candidate, profile)
            if (!compliance.equipmentAvailable) return@mapNotNull null

            var score =
                SubstituteScoring.PRIMARY_MUSCLE_WEIGHT * sharedPrimaries / originalPrimaries.size
            if (candidate.mechanic != null && candidate.mechanic == exercise.mechanic) {
                score += SubstituteScoring.SAME_MECHANIC_BONUS
            }
            if (candidate.category == exercise.category) {
                score += SubstituteScoring.SAME_CATEGORY_BONUS
            }
            if (candidate.level == exercise.level) {
                score += SubstituteScoring.SAME_LEVEL_BONUS
            }
            score -= SubstituteScoring.CONSTRAINT_VIOLATION_PENALTY *
                compliance.constraintViolations.size

            SubstituteMatch(candidate, score, compliance.constraintViolations)
        }
        .sortedWith(compareByDescending<SubstituteMatch> { it.score }.thenBy { it.exercise.namePl })
        .take(limit)
        .toList()
}

/**
 * Filtr po grupie sprzętu ([com.stronk.ui.profile.ProfileEquipment]) w arkuszu
 * zamienników — działa na PEŁNEJ liście kandydatów (wołaj [findSubstitutes] ze
 * `limit = SubstituteScoring.NO_LIMIT`), `displayLimit` stosujemy DOPIERO PO
 * filtrze. Bez tego zawężenie do jednej grupy sprzętu potrafiło pokazać pustkę,
 * mimo że pasujące zamienniki istniały, tylko zostały ucięte limitem WCZEŚNIEJ.
 *
 * Generic i bez zależności od typu UI — współdzielona przez arkusz zamienników
 * treningu ([com.stronk.ui.workout.SubstituteUi]) i edytora planu
 * ([com.stronk.data.SubstituteMatch]); `groupIdOf` wyciąga grupę z elementu.
 * Nic nie zaznaczone ([selectedGroups] puste) = pokazujemy wszystko (jak dziś).
 */
fun <T> filterSubstitutesByGroup(
    items: List<T>,
    groupIdOf: (T) -> String,
    selectedGroups: Set<String>,
    displayLimit: Int,
): List<T> {
    val filtered = if (selectedGroups.isEmpty()) {
        items
    } else {
        items.filter { groupIdOf(it) in selectedGroups }
    }
    return filtered.take(displayLimit)
}
