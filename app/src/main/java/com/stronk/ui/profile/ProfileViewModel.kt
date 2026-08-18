package com.stronk.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stronk.StronkApplication
import com.stronk.data.AccessCodeStore
import com.stronk.data.ExerciseRepository
import com.stronk.data.StressLevel
import com.stronk.data.TrainingGoal
import com.stronk.data.UserProfileRepository
import com.stronk.ui.PlLabels
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Stan ekranu profilu. */
data class ProfileUiState(
    val loading: Boolean = true,
    val displayName: String = "",
    val equipment: Set<String> = emptySet(),
    /** Tylko realne limity (LOW/MEDIUM); brak wpisu = brak ograniczenia. */
    val constraints: Map<String, StressLevel> = emptyMap(),
    val goal: TrainingGoal? = null,
    val returningFromBreak: Boolean = false,
    /** Wartości sprzętu z datasetu, posortowane po polskich etykietach. */
    val equipmentOptions: List<String> = emptyList(),
    /** Kod dostępu do podglądu (klucz do danych przy zmianie telefonu). */
    val accessCode: String? = null,
)

/**
 * Profil: odczyt z snapshot listenera, zapis fire-and-forget przy każdej
 * zmianie (autosave, zero friction — bez przycisku "Zapisz"). Po pierwszej
 * edycji formularz jest lokalną prawdą — zdalne emisje (echa własnych zapisów)
 * już go nie nadpisują, więc szybkie klikanie nie migocze.
 */
class ProfileViewModel(
    private val userProfileRepository: UserProfileRepository,
    exerciseRepository: ExerciseRepository,
    accessCodeStore: AccessCodeStore,
) : ViewModel() {

    /** null do pierwszej emisji profilu (spinner) — potem zawsze aktualny formularz. */
    private val form = MutableStateFlow<ProfileFormState?>(null)
    private val equipmentOptions = MutableStateFlow<List<String>>(emptyList())
    private val accessCode: String? = accessCodeStore.getCode()

    /** createdAt z Firestore — zachowywany przy zapisie; brak dokumentu → "teraz". */
    private var createdAt: Long? = null

    /** true po pierwszej edycji — od tej pory ignorujemy zdalne emisje. */
    private var dirty = false

    /** Odroczony zapis imienia (debounce) — reszta pól zapisuje się od razu. */
    private var nameSaveJob: Job? = null

    /**
     * true, gdy debounce imienia jeszcze nie zapisał. Osobna flaga zamiast
     * `nameSaveJob.isActive`, bo viewModelScope jest anulowany PRZED
     * [onCleared] — job byłby już martwy i flush by nie zadziałał.
     */
    private var namePersistPending = false

    val uiState: StateFlow<ProfileUiState> = combine(form, equipmentOptions) { current, options ->
        if (current == null) {
            ProfileUiState(loading = true, accessCode = accessCode)
        } else {
            ProfileUiState(
                loading = false,
                displayName = current.displayName,
                equipment = current.equipment,
                constraints = current.constraints,
                goal = current.goal,
                returningFromBreak = current.returningFromBreak,
                // Wybrane wartości spoza datasetu doklejamy, żeby dały się odznaczyć.
                equipmentOptions = options + current.equipment.filterNot { it in options },
                accessCode = accessCode,
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ProfileUiState(accessCode = accessCode),
    )

    init {
        viewModelScope.launch {
            userProfileRepository.observeProfile().collect { profile ->
                if (profile != null) createdAt = profile.createdAt
                if (!dirty) form.value = profileFormFrom(profile)
            }
        }
        viewModelScope.launch {
            equipmentOptions.value = exerciseRepository.getAll()
                .mapNotNull { it.equipment }
                .distinct()
                .sortedBy { PlLabels.equipment(it) }
        }
    }

    fun onDisplayNameChange(value: String) {
        dirty = true
        form.value = currentForm().copy(displayName = value)
        namePersistPending = true
        nameSaveJob?.cancel()
        nameSaveJob = viewModelScope.launch {
            delay(ProfileDefaults.NAME_SAVE_DEBOUNCE_MS)
            persist()
        }
    }

    fun onEquipmentToggle(value: String) = update { current ->
        current.copy(
            equipment = if (value in current.equipment) {
                current.equipment - value
            } else {
                current.equipment + value
            },
        )
    }

    /** [maxAccepted] null = brak ograniczenia dla stawu. */
    fun onConstraintChange(joint: String, maxAccepted: StressLevel?) = update { current ->
        current.copy(
            constraints = if (maxAccepted == null) {
                current.constraints - joint
            } else {
                current.constraints + (joint to maxAccepted)
            },
        )
    }

    fun onGoalChange(goal: TrainingGoal) = update { it.copy(goal = goal) }

    fun onReturningFromBreakChange(value: Boolean) = update { it.copy(returningFromBreak = value) }

    private fun currentForm(): ProfileFormState = form.value ?: ProfileFormState()

    private fun update(transform: (ProfileFormState) -> ProfileFormState) {
        dirty = true
        form.value = transform(currentForm())
        // Natychmiastowy zapis niesie też ewentualną świeżą zmianę imienia.
        nameSaveJob?.cancel()
        persist()
    }

    /** Zapis całego formularza (merge, fire-and-forget). */
    private fun persist() {
        namePersistPending = false
        val current = form.value ?: return
        val timestamp = createdAt ?: System.currentTimeMillis().also { createdAt = it }
        userProfileRepository.save(current.toUserProfile(createdAt = timestamp))
    }

    override fun onCleared() {
        // Niedomknięty debounce imienia — dopisz od razu, zanim ViewModel zniknie.
        if (namePersistPending) persist()
        super.onCleared()
    }

    companion object {
        /** Ręczna kompozycja: zależności z [StronkApplication]. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as StronkApplication
                ProfileViewModel(
                    userProfileRepository = app.userProfileRepository,
                    exerciseRepository = app.exerciseRepository,
                    accessCodeStore = app.accessCodeStore,
                )
            }
        }
    }
}
