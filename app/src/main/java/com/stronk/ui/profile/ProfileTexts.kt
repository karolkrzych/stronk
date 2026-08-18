package com.stronk.ui.profile

import com.stronk.data.StressLevel
import com.stronk.ui.PlLabels

/**
 * Teksty ekranu profilu — czysta warstwa słów, bez Compose'a, żeby dało się je
 * przetestować i żeby żaden ekran nie wymyślał własnych wariantów nazw.
 */
object ProfileTexts {

    /** Poziomy ograniczenia oferowane użytkownikowi (od łagodnego do ostrego). */
    val SEVERITY_OPTIONS: List<StressLevel> = listOf(StressLevel.MEDIUM, StressLevel.LOW)

    /** Nazwa stawu jako tytuł wiersza — [PlLabels] pisze małą literą. */
    fun jointTitle(joint: String): String =
        PlLabels.joint(joint).replaceFirstChar { it.uppercase() }

    /** Nazwa poziomu ograniczenia (wybór w arkuszu). */
    fun severityTitle(level: StressLevel): String = when (level) {
        StressLevel.MEDIUM -> "Do średniego obciążenia"
        StressLevel.LOW, StressLevel.NONE -> "Tylko niskie obciążenie"
        StressLevel.HIGH -> "Bez ograniczeń"
    }

    /** Co ten poziom robi z doborem ćwiczeń — jedno zdanie pod nazwą. */
    fun severityDescription(level: StressLevel): String = when (level) {
        StressLevel.MEDIUM -> "Odpadają ćwiczenia mocno obciążające to miejsce."
        StressLevel.LOW, StressLevel.NONE -> "Zostają wyłącznie ćwiczenia delikatne dla tego miejsca."
        StressLevel.HIGH -> "Nie pomijamy niczego."
    }

    /** Podpis pod nazwą stawu na liście ograniczeń — mały, wygaszony. */
    fun severityRowText(level: StressLevel): String = when (level) {
        StressLevel.MEDIUM -> "unikaj wysokiego obciążenia"
        StressLevel.LOW, StressLevel.NONE -> "tylko niskie obciążenie"
        StressLevel.HIGH -> "bez ograniczeń"
    }

    /** Komunikat nad listą sprzętu — mówi, co zaznaczenie realnie zmienia. */
    fun equipmentHint(selectedCount: Int): String =
        if (selectedCount == 0) {
            "Nic nie zaznaczone — pokazujemy wszystkie ćwiczenia. Zaznacz, co masz pod ręką."
        } else {
            "Zaznaczone: $selectedCount. Pod ten sprzęt dobieramy ćwiczenia i zamienniki."
        }

    /** Liczba do kafelka przerwy: pełne minuty jako minuty, reszta jako sekundy. */
    fun restValue(seconds: Int): String =
        if (seconds >= 60 && seconds % 60 == 0) (seconds / 60).toString() else seconds.toString()

    /** Jednostka pasująca do [restValue]. */
    fun restUnit(seconds: Int): String =
        if (seconds >= 60 && seconds % 60 == 0) "min" else "s"

    /** Przerwa jako tekst chipa przy niewybranym celu, np. "90 s przerwy". */
    fun restChip(seconds: Int): String = "${restValue(seconds)} ${restUnit(seconds)} przerwy"

    /** Liczba serii jako tekst chipa — polska odmiana bez kombinowania. */
    fun setsChip(sets: Int): String = if (sets == 1) "1 seria" else "$sets serie"
}
