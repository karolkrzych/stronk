package com.stronk.ui.home

import com.stronk.data.Exercise
import com.stronk.data.Plan
import com.stronk.data.PlanDay
import com.stronk.data.ScheduleEntry
import com.stronk.data.ScheduleStatus
import com.stronk.ui.plans.PlanTexts

/**
 * Mapowanie danych (plan + harmonogram) na model ekranu „Dziś" — czyste funkcje
 * bez Androida i bez repozytoriów, żeby dały się przetestować jednostkowo.
 * [HomeViewModel] jest tu tylko sklejaczem strumieni.
 */
internal object HomeMapping {

    /**
     * Które wpisy harmonogramu obsługuje ekran. Trzy pytania w kolejności:
     * czy dziś COŚ jest do zrobienia, czy dziś COŚ zostało zrobione, co dalej.
     */
    data class Entries(
        /** Dzisiejszy wpis PLANNED — dominanta ekranu (CTA „Zacznij trening"). */
        val today: ScheduleEntry?,
        /** Dzisiejszy wpis DONE — ekran pokazuje belkę „Trening ukończony". */
        val todayDone: ScheduleEntry?,
        /** Najbliższy PLANNED po dziś — gdy dziś nie ma nic zaplanowanego. */
        val upcoming: ScheduleEntry?,
    )

    /**
     * Wybór wpisów na dziś/najbliżej. [schedule] przychodzi posortowany
     * chronologicznie (repozytorium), więc „first" = najwcześniejszy.
     */
    fun selectEntries(schedule: List<ScheduleEntry>, todayKey: String): Entries {
        val planned = schedule.filter { it.status == ScheduleStatus.PLANNED }
        return Entries(
            today = planned.firstOrNull { it.date == todayKey },
            todayDone = schedule.firstOrNull {
                it.date == todayKey && it.status == ScheduleStatus.DONE
            },
            upcoming = planned.firstOrNull { it.date > todayKey },
        )
    }

    /** Wiersze ćwiczeń jednego dnia planu — bez ciężarów, z chipem liczby serii. */
    fun exerciseRows(day: PlanDay, exercises: Map<String, Exercise>): List<HomeExerciseRow> =
        day.exercises.map { planExercise ->
            val exercise = exercises[planExercise.exerciseId]
            HomeExerciseRow(
                exerciseId = planExercise.exerciseId,
                name = exercise?.namePl ?: planExercise.exerciseId,
                muscleKey = exercise?.primaryMuscles?.firstOrNull(),
                setsChip = PlanTexts.setsChip(planExercise.sets),
            )
        }

    /** Suma serii roboczych dnia — druga liczba belki „Trening ukończony". */
    fun setCount(day: PlanDay): Int = day.exercises.sumOf { it.sets }

    /**
     * Cały plan pod sheet „Szczegóły planu" (wariant S2): każdy dzień to karta
     * z podglądem ćwiczeń, więc stan UI musi wieźć WSZYSTKIE dni, nie tylko dziś.
     *
     * @param currentDayIndex dzień, który apka pokazuje na ekranie („dziś") —
     *        dostaje krechę limeDeep; null, gdy żaden dzień planu nie jest bieżący
     */
    fun planOverview(
        plan: Plan,
        currentDayIndex: Int?,
        exercises: Map<String, Exercise>,
    ): PlanOverviewUi = PlanOverviewUi(
        planId = plan.id,
        name = plan.name,
        days = plan.days.mapIndexed { index, day ->
            PlanDayUi(
                dayIndex = index,
                name = day.name,
                exercises = exerciseRows(day, exercises),
                current = index == currentDayIndex,
            )
        },
    )

    /** Ile ćwiczeń dnia NIE mieści się w podglądzie karty (kwadracik „+N"). */
    fun hiddenCount(exerciseCount: Int): Int =
        (exerciseCount - HomeTexts.DAY_PREVIEW_THUMBS).coerceAtLeast(0)
}
