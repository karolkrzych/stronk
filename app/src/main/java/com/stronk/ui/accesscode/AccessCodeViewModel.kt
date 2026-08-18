package com.stronk.ui.accesscode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stronk.StronkApplication
import com.stronk.data.AccessCodeGenerator
import com.stronk.data.AccessCodeStore
import com.stronk.data.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Krok ekranu kodu dostępu. */
sealed interface AccessCodeStep {
    /** Wybór ścieżki: nowy kod albo istniejący. */
    data object Choice : AccessCodeStep

    /** Wygenerowany kod czeka na potwierdzenie "zapisałem". */
    data class Generated(val code: String) : AccessCodeStep

    /** Wpisywanie istniejącego kodu (np. po zmianie telefonu). */
    data class EnterExisting(val input: String = "", val error: String? = null) : AccessCodeStep
}

/** Stan ekranu kodu dostępu. */
data class AccessCodeUiState(
    val step: AccessCodeStep = AccessCodeStep.Choice,
    /** true po zapisaniu kodu — sygnał do nawigacji do właściwej apki. */
    val finished: Boolean = false,
)

class AccessCodeViewModel(
    private val accessCodeStore: AccessCodeStore,
    private val userProfileRepository: UserProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccessCodeUiState())
    val uiState: StateFlow<AccessCodeUiState> = _uiState

    fun onGenerateNew() {
        _uiState.value =
            AccessCodeUiState(step = AccessCodeStep.Generated(AccessCodeGenerator.generate()))
    }

    fun onHaveCode() {
        _uiState.value = AccessCodeUiState(step = AccessCodeStep.EnterExisting())
    }

    fun onBackToChoice() {
        _uiState.value = AccessCodeUiState(step = AccessCodeStep.Choice)
    }

    fun onInputChange(raw: String) {
        val normalized = AccessCodeGenerator.normalize(raw).take(AccessCodeGenerator.CODE_LENGTH)
        _uiState.value = AccessCodeUiState(step = AccessCodeStep.EnterExisting(input = normalized))
    }

    fun onConfirmGenerated() {
        val step = _uiState.value.step as? AccessCodeStep.Generated ?: return
        commit(step.code)
    }

    fun onSubmitEntered() {
        val step = _uiState.value.step as? AccessCodeStep.EnterExisting ?: return
        if (!AccessCodeGenerator.isValid(step.input)) {
            _uiState.value = AccessCodeUiState(
                step = step.copy(
                    error = "Kod ma ${AccessCodeGenerator.CODE_LENGTH} znaków: " +
                        "wielkie litery i cyfry 2–9, bez O, I oraz L.",
                ),
            )
            return
        }
        commit(step.input)
    }

    /**
     * Zapis kodu lokalnie + utworzenie/scalenie profilu `users/{code}`
     * (merge, fire-and-forget — offline-first, bez czekania na serwer).
     */
    private fun commit(code: String) {
        accessCodeStore.saveCode(code)
        userProfileRepository.ensureProfileDocument()
        _uiState.value = _uiState.value.copy(finished = true)
    }

    companion object {
        /** Ręczna kompozycja: zależności z [StronkApplication]. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as StronkApplication
                AccessCodeViewModel(app.accessCodeStore, app.userProfileRepository)
            }
        }
    }
}
