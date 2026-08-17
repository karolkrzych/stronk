package com.stronk.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stronk.StronkApplication
import com.stronk.data.Exercise
import com.stronk.data.ExerciseFilters
import com.stronk.data.ExerciseRepository
import com.stronk.data.filterExercises
import com.stronk.ui.PlLabels
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Stan ekranu listy ćwiczeń. */
data class ExerciseListUiState(
    val loading: Boolean = true,
    val query: String = "",
    val filters: ExerciseFilters = ExerciseFilters(),
    /** Ćwiczenia po zastosowaniu wyszukiwania i filtrów. */
    val exercises: List<Exercise> = emptyList(),
    val totalCount: Int = 0,
    /** Opcje filtrów wyliczone z datasetu, posortowane po polskich etykietach. */
    val muscleOptions: List<String> = emptyList(),
    val equipmentOptions: List<String> = emptyList(),
    val levelOptions: List<String> = emptyList(),
    val categoryOptions: List<String> = emptyList(),
)

class ExerciseListViewModel(private val repository: ExerciseRepository) : ViewModel() {

    private val allExercises = MutableStateFlow<List<Exercise>?>(null)
    private val query = MutableStateFlow("")
    private val filters = MutableStateFlow(ExerciseFilters())

    val uiState: StateFlow<ExerciseListUiState> =
        combine(allExercises, query, filters) { all, currentQuery, currentFilters ->
            if (all == null) {
                ExerciseListUiState(loading = true, query = currentQuery, filters = currentFilters)
            } else {
                ExerciseListUiState(
                    loading = false,
                    query = currentQuery,
                    filters = currentFilters,
                    exercises = filterExercises(all, currentQuery, currentFilters),
                    totalCount = all.size,
                    muscleOptions = all.flatMap { it.primaryMuscles }.distinct()
                        .sortedBy { PlLabels.muscle(it) },
                    equipmentOptions = all.mapNotNull { it.equipment }.distinct()
                        .sortedBy { PlLabels.equipment(it) },
                    levelOptions = all.map { it.level }.distinct()
                        .sortedBy { levelOrder.indexOf(it) },
                    categoryOptions = all.map { it.category }.distinct()
                        .sortedBy { PlLabels.category(it) },
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseListUiState())

    init {
        viewModelScope.launch { allExercises.value = repository.getAll() }
    }

    fun onQueryChange(newQuery: String) {
        query.value = newQuery
    }

    fun onFiltersChange(newFilters: ExerciseFilters) {
        filters.value = newFilters
    }

    companion object {
        private val levelOrder = listOf("beginner", "intermediate", "expert")

        /** Ręczna kompozycja: repozytorium z [StronkApplication]. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as StronkApplication
                ExerciseListViewModel(app.exerciseRepository)
            }
        }
    }
}
