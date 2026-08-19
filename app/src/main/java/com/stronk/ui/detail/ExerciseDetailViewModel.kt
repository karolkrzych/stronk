package com.stronk.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stronk.StronkApplication
import com.stronk.data.Exercise
import com.stronk.data.ExerciseRepository
import com.stronk.data.ProfileDetails
import com.stronk.data.StressLevel
import com.stronk.data.UserProfileRepository
import com.stronk.ui.PlLabels
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Stan ekranu szczegółów ćwiczenia (zakładka „Opis"). */
data class ExerciseDetailUiState(
    val loading: Boolean = true,
    val exercise: Exercise? = null,
    /**
     * Notka „Twoje stawy" — jedna–dwie linijki złożone z ograniczeń profilu
     * i tagów jointStress ćwiczenia. null, gdy nie ma czego powiedzieć.
     */
    val jointNote: String? = null,
)

class ExerciseDetailViewModel(
    exerciseRepository: ExerciseRepository,
    userProfileRepository: UserProfileRepository,
    exerciseId: String,
) : ViewModel() {

    /** null = dataset jeszcze się ładuje; wrapper, bo ćwiczenia może nie być. */
    private data class ExerciseLookup(val exercise: Exercise?)

    private val lookup = MutableStateFlow<ExerciseLookup?>(null)

    /**
     * Ograniczenia z profilu.
     * - `onStart(null)`: opis ćwiczenia pokazujemy OD RAZU, notka dochodzi
     *   po pierwszym snapshocie — ekran nigdy nie czeka na Firestore.
     * - `runCatching`: bez ustalonego kodu dostępu repozytorium rzuca; szczegół
     *   ćwiczenia ma wtedy działać dalej, tylko bez notki.
     */
    private val profileDetails: Flow<ProfileDetails?> =
        runCatching { userProfileRepository.observeProfile().map { it?.profile } }
            .getOrElse { emptyFlow() }
            .onStart { emit(null) }

    val uiState: StateFlow<ExerciseDetailUiState> = combine(
        lookup,
        profileDetails,
    ) { exerciseLookup, profile ->
        val exercise = exerciseLookup?.exercise
        ExerciseDetailUiState(
            loading = exerciseLookup == null,
            exercise = exercise,
            jointNote = exercise?.let { JointNote.text(it, profile ?: ProfileDetails()) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseDetailUiState())

    init {
        viewModelScope.launch {
            lookup.value = ExerciseLookup(exerciseRepository.getById(exerciseId))
        }
    }

    companion object {
        /** Fabryka z parametrem id — ręczna kompozycja z [StronkApplication]. */
        fun factory(exerciseId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as StronkApplication
                ExerciseDetailViewModel(
                    exerciseRepository = app.exerciseRepository,
                    userProfileRepository = app.userProfileRepository,
                    exerciseId = exerciseId,
                )
            }
        }
    }
}

/**
 * Notka „TWOJE STAWY" (mock `pack-progres-baza`, ekran 3) — JEDNO zdanie
 * o tym, co to ćwiczenie robi z Twoimi ograniczeniami, plus ewentualna uwaga
 * z datasetu. Kontuzje są pierwszorzędnym mechanizmem apki, więc notka mówi
 * o stawach Z PROFILU, a nie o wszystkich siedmiu.
 *
 * Bez profilu (brak ograniczeń) zostaje ostrzeżenie ogólne — i tylko wtedy,
 * gdy ćwiczenie realnie mocno coś obciąża.
 */
internal object JointNote {

    fun text(exercise: Exercise, profile: ProfileDetails): String? {
        val stress = exercise.jointStress.all
        val caution = exercise.cautionNotes?.trim()?.takeIf { it.isNotEmpty() }

        val mine = profile.constraints.keys
            .mapNotNull { joint -> stress[joint]?.let { joint to it } }
        val sentence = if (mine.isNotEmpty()) {
            constrainedSentence(mine, profile.constraints)
        } else {
            highStressSentence(stress)
        }
        return listOfNotNull(sentence, caution)
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" ")
    }

    /** Zdanie o stawach z profilu: najgorszy poziom + informacja o przekroczeniu limitu. */
    private fun constrainedSentence(
        mine: List<Pair<String, StressLevel>>,
        limits: Map<String, StressLevel>,
    ): String {
        val worst = mine.maxOf { rank(it.second) }
        val joints = mine.filter { rank(it.second) == worst }.map { it.first }
        val level = mine.first { rank(it.second) == worst }.second
        val exceeded = mine.any { (joint, value) ->
            limits[joint]?.let { rank(value) > rank(it) } == true
        }
        val head = jointNames(joints)
        return when {
            exceeded -> "$head: obciążenie ${levelWord(level)} — powyżej Twojego limitu."
            level == StressLevel.NONE -> "$head: bez obciążenia."
            else -> "$head: obciążenie ${levelWord(level)}."
        }
    }

    /** Bez profilu mówimy tylko o tym, co ćwiczenie obciąża mocno. */
    private fun highStressSentence(stress: Map<String, StressLevel>): String? {
        val high = stress.filterValues { it == StressLevel.HIGH }.keys
        if (high.isEmpty()) return null
        return "Mocno obciąża: ${jointNames(high.toList()).replaceFirstChar { it.lowercase() }}."
    }

    private fun jointNames(joints: List<String>): String {
        val names = joints.map { PlLabels.joint(it) }
        val joined = when (names.size) {
            1 -> names.first()
            2 -> "${names[0]} i ${names[1]}"
            else -> names.dropLast(1).joinToString(", ") + " i " + names.last()
        }
        return joined.replaceFirstChar { it.uppercase() }
    }

    private fun levelWord(level: StressLevel): String = when (level) {
        StressLevel.HIGH -> "wysokie"
        StressLevel.MEDIUM -> "średnie"
        StressLevel.LOW -> "niskie"
        StressLevel.NONE -> "żadne"
    }

    /** Porządek poziomów: NONE < LOW < MEDIUM < HIGH. */
    private fun rank(level: StressLevel): Int = when (level) {
        StressLevel.NONE -> 0
        StressLevel.LOW -> 1
        StressLevel.MEDIUM -> 2
        StressLevel.HIGH -> 3
    }
}
