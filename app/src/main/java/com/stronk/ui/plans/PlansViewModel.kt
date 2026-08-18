package com.stronk.ui.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stronk.StronkApplication
import com.stronk.data.Plan
import com.stronk.data.PlanRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Stan listy planów. */
data class PlansUiState(
    val loading: Boolean = true,
    /** Aktywne plany, najnowsze pierwsze. */
    val activePlans: List<Plan> = emptyList(),
    /** Zarchiwizowane plany, najnowsze pierwsze. */
    val archivedPlans: List<Plan> = emptyList(),
)

class PlansViewModel(private val planRepository: PlanRepository) : ViewModel() {

    val uiState: StateFlow<PlansUiState> = planRepository.observePlans()
        .map { plans ->
            PlansUiState(
                loading = false,
                activePlans = plans.filterNot { it.archived }.sortedByDescending { it.createdAt },
                archivedPlans = plans.filter { it.archived }.sortedByDescending { it.createdAt },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlansUiState())

    /** Archiwizacja/przywrócenie — zapis całego dokumentu (fire-and-forget). */
    fun setArchived(plan: Plan, archived: Boolean) {
        planRepository.save(plan.copy(archived = archived))
    }

    companion object {
        /** Ręczna kompozycja: repozytorium z [StronkApplication]. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as StronkApplication
                PlansViewModel(app.planRepository)
            }
        }
    }
}
