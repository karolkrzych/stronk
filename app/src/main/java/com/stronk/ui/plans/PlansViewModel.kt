package com.stronk.ui.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stronk.StronkApplication
import com.stronk.data.Plan
import com.stronk.data.PlanRepository
import com.stronk.data.ScheduleEntry
import com.stronk.data.ScheduleRepository
import com.stronk.data.ScheduleStatus
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Plan przygotowany pod kartę listy: nazwa + trzy mini-staty. */
data class PlanCardUi(
    val id: String,
    val name: String,
    /** Liczba dni w planie — stat DNI. */
    val days: Int,
    /**
     * Stat TYGODNIE: pełna długość bloku razem z tygodniem lekkim (ADR-004)
     * albo „∞" — plan bez bloku biegnie bez końca.
     */
    val weeks: String,
    /** Suma ćwiczeń we wszystkich dniach — stat ĆWICZENIA. */
    val exercises: Int,
    /** Plan, po którym akurat trenujemy — limonkowy pasek, obwódka i pigułka. */
    val active: Boolean,
)

/** Stan listy planów. */
data class PlansUiState(
    val loading: Boolean = true,
    /** Aktywne plany, najnowsze pierwsze; plan „w użyciu" ma `active = true`. */
    val plans: List<PlanCardUi> = emptyList(),
    /** Zarchiwizowane plany, najnowsze pierwsze — schowane pod linkiem „Archiwum". */
    val archived: List<PlanCardUi> = emptyList(),
)

class PlansViewModel(
    planRepository: PlanRepository,
    scheduleRepository: ScheduleRepository,
) : ViewModel() {

    val uiState: StateFlow<PlansUiState> = combine(
        planRepository.observePlans(),
        scheduleRepository.observeSchedule(),
    ) { plans, schedule ->
        val active = plans.filterNot { it.archived }.sortedByDescending { it.createdAt }
        val activeId = activePlanId(active, schedule)
        PlansUiState(
            loading = false,
            plans = active.map { it.toCard(active = it.id == activeId) },
            archived = plans.filter { it.archived }
                .sortedByDescending { it.createdAt }
                .map { it.toCard(active = false) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlansUiState())

    companion object {

        private fun Plan.toCard(active: Boolean) = PlanCardUi(
            id = id,
            name = name,
            days = days.size,
            weeks = PlanTexts.blockWeeksStat(PlanTexts.fullBlockWeeksOrNull(this)),
            exercises = PlanTexts.exerciseCount(this),
            active = active,
        )

        /**
         * „Aktywny" = plan, po którym realnie trenujemy: ten z najbliższego
         * zaplanowanego wpisu harmonogramu. Bez harmonogramu spada na najnowszy
         * plan (ten, który user właśnie złożył).
         */
        internal fun activePlanId(activePlans: List<Plan>, schedule: List<ScheduleEntry>): String? {
            if (activePlans.isEmpty()) return null
            val ids = activePlans.map { it.id }.toSet()
            val todayKey = LocalDate.now().toString()
            // observeSchedule sortuje chronologicznie: pierwszy pasujący = najbliższy.
            val upcoming = schedule.firstOrNull {
                it.status == ScheduleStatus.PLANNED && it.date >= todayKey && it.planId in ids
            }
            val recent = schedule.lastOrNull { it.date <= todayKey && it.planId in ids }
            return upcoming?.planId ?: recent?.planId ?: activePlans.first().id
        }

        /** Ręczna kompozycja: repozytoria z [StronkApplication]. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as StronkApplication
                PlansViewModel(app.planRepository, app.scheduleRepository)
            }
        }
    }
}
