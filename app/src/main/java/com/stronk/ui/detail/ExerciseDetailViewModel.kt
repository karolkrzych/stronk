package com.stronk.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stronk.StronkApplication
import com.stronk.data.Exercise
import com.stronk.data.ExerciseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Stan ekranu szczegółów ćwiczenia. */
data class ExerciseDetailUiState(
    val loading: Boolean = true,
    val exercise: Exercise? = null,
)

class ExerciseDetailViewModel(
    repository: ExerciseRepository,
    exerciseId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExerciseDetailUiState())
    val uiState: StateFlow<ExerciseDetailUiState> = _uiState

    init {
        viewModelScope.launch {
            _uiState.value = ExerciseDetailUiState(
                loading = false,
                exercise = repository.getById(exerciseId),
            )
        }
    }

    companion object {
        /** Fabryka z parametrem id — ręczna kompozycja z [StronkApplication]. */
        fun factory(exerciseId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as StronkApplication
                ExerciseDetailViewModel(app.exerciseRepository, exerciseId)
            }
        }
    }
}
