package com.stronk.ui.home

import com.stronk.ui.plans.PlanTexts

/**
 * Teksty ekranu „Dziś" — czyste funkcje, zero Androida, więc dają się
 * przetestować jednostkowo (jak [com.stronk.ui.cardio.CardioTexts]).
 *
 * Liczniki są ETYKIETAMI z odmienionym rzeczownikiem („6 ćwiczeń"), nie frazami
 * typu „6 × 3". Jedyne sklejenie, jakie tu powstaje, to podsumowanie ukończonego
 * treningu z mocka rundy 5 („6 ćwiczeń · 18 serii") — dwie policzalne etykiety
 * w JEDNEJ wygaszonej linijce pod „Trening ukończony", nie stat do czytania.
 */
internal object HomeTexts {

    const val TITLE = "Dziś"

    /** CTA, gdy trening wypada DZIŚ. */
    const val CTA_TODAY = "Zacznij trening"

    /** CTA, gdy najbliższy trening jest w innym dniu. */
    const val CTA_UPCOMING = "Zacznij teraz"

    const val DONE_BAR = "Trening ukończony"
    const val STATUS_DONE = "Dzisiejszy trening zaliczony."
    const val SECTION_EXERCISES = "Ćwiczenia"
    const val SECTION_CARDIO = "Cardio"
    const val ADD_CARDIO = "Dodaj cardio"
    const val EDIT_PLAN = "Edytuj plan"

    /** Ile miniatur ćwiczeń pokazuje karta dnia w sheecie „Szczegóły planu". */
    const val DAY_PREVIEW_THUMBS = 3

    /** „1 ćwiczenie" / „3 ćwiczenia" / „6 ćwiczeń" — polska odmiana licznika. */
    fun exercisesCount(count: Int): String = "$count ${exercisesNoun(count)}"

    /** Sam rzeczownik w odmianie pasującej do liczby. */
    fun exercisesNoun(count: Int): String = when {
        count == 1 -> "ćwiczenie"
        count % 10 in 2..4 && count % 100 !in 12..14 -> "ćwiczenia"
        else -> "ćwiczeń"
    }

    /** „1 seria" / „3 serie" / „18 serii" — ten sam słownik co lista ćwiczeń dnia. */
    fun setsCount(count: Int): String = PlanTexts.setsChip(count)

    /** Druga linijka belki „Trening ukończony": „6 ćwiczeń · 18 serii". */
    fun workoutSummary(exercises: Int, sets: Int): String =
        "${exercisesCount(exercises)} · ${setsCount(sets)}"

    /** Licznik ukrytych miniatur na karcie dnia („+3"); 0 = kwadracika nie ma. */
    fun moreLabel(hidden: Int): String = "+$hidden"

    /**
     * Nagłówek sheetu planu — nazwa BEZ dopisku w nawiasie: presety nazywają się
     * „Full Body 3×/tydz. (powrót po przerwie)", a w tytule 27 taka nazwa łamie
     * się na trzy linie. Dopisek wraca jako [planSubtitle] w kapitaliku.
     */
    fun planTitle(name: String): String =
        parenthetical(name)?.first ?: name.trim()

    /** KAPITALIK pod nazwą planu — treść nawiasu albo null, gdy nazwa go nie ma. */
    fun planSubtitle(name: String): String? = parenthetical(name)?.second

    /** Nazwa rozbita na „człon główny" + „dopisek z nawiasu"; null = brak nawiasu. */
    private fun parenthetical(name: String): Pair<String, String>? {
        val match = PARENTHETICAL.matchEntire(name.trim()) ?: return null
        val head = match.groupValues[1].trim()
        val tail = match.groupValues[2].trim()
        if (head.isEmpty() || tail.isEmpty()) return null
        return head to tail
    }

    /** „Nazwa (dopisek)" — nawias TYLKO na końcu i bez zagnieżdżeń. */
    private val PARENTHETICAL = Regex("""(.*?)\s*\(([^()]*)\)""")
}
