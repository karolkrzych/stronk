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
        /**
         * Najbliższy PLANNED po dziś. To TYLKO zapowiedź („następny trening"),
         * nigdy treść ekranu: dzień wolny zostaje dniem wolnym, a treningu
         * z wyprzedzeniem nie da się z ekranu „Dziś" wystartować.
         */
        val upcoming: ScheduleEntry?,
    )

    /**
     * Co ekran „Dziś" ma dziś pokazać. Kolejność pytań jest ta sama co na
     * ekranie, a stan [FREE_DAY] istnieje po to, żeby ekran mówił PRAWDĘ:
     * przy planie 3×/tydz. wolna niedziela ma wyglądać jak wolna niedziela,
     * a nie jak jutrzejszy trening podstawiony pod dzisiejszą datę.
     */
    enum class DayState {
        /** Dziś jest zaplanowany trening — pełna strefa treningu z CTA. */
        WORKOUT,

        /** Dzisiejszy trening zaliczony — belka „Trening ukończony". */
        DONE,

        /** Dziś nic nie zaplanowano, ale plan biegnie dalej — „Dzień wolny". */
        FREE_DAY,

        /** Jest plan, ale harmonogram nic nie wie o przyszłości. */
        NO_SCHEDULE,

        /** Zero aktywnych planów — zachęta do stworzenia pierwszego. */
        NO_PLANS,
    }

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

    /**
     * Wariant ekranu na dziś — czysta decyzja z [selectEntries] i jednej flagi.
     *
     * „Dzień wolny" wymaga zapowiedzi ([Entries.upcoming]): plan, który się
     * skończył (same przeszłe wpisy), to nie odpoczynek tylko pusty
     * harmonogram — tam ekran ma prosić o zaplanowanie kolejnych dni.
     *
     * @param hasActivePlan czy user ma choć jeden nie zarchiwizowany plan
     */
    fun dayState(entries: Entries, hasActivePlan: Boolean): DayState = when {
        entries.today != null -> DayState.WORKOUT
        entries.todayDone != null -> DayState.DONE
        !hasActivePlan -> DayState.NO_PLANS
        entries.upcoming != null -> DayState.FREE_DAY
        else -> DayState.NO_SCHEDULE
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
