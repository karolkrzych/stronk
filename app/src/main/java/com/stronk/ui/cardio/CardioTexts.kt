package com.stronk.ui.cardio

import com.stronk.data.CardioType
import kotlin.math.roundToInt

/**
 * Teksty, formatowanie i WALIDACJA formularza cardio — czyste funkcje, zero
 * Androida, więc dają się przetestować jednostkowo.
 *
 * TWARDA ZASADA (zasady Karola, docs/design-system.md pkt 3): tu NIE POWSTAJE
 * fraza „42 min · 14,2 km". Liczba i jednostka wracają OSOBNO ([minutesValue] +
 * [UNIT_MINUTES], [distanceValue] + [UNIT_KILOMETERS]) i ekran renderuje z nich
 * dwa stat-bloki z własnymi kapitalikami.
 */
internal object CardioTexts {

    // --- nazwy wartości i sekcji (jedno miejsce zmian) ---

    const val SECTION_CARDIO = "Cardio"
    const val LABEL_TYPE = "Typ"
    const val LABEL_TIME = "Czas"
    const val LABEL_DISTANCE = "Dystans"
    const val UNIT_MINUTES = "min"
    const val UNIT_KILOMETERS = "km"
    const val SHEET_TITLE_ADD = "Dodaj cardio"
    const val SHEET_TITLE_EDIT = "Zmień cardio"
    const val SAVE = "Zapisz"
    const val DELETE = "Usuń"
    const val OPTIONAL = "opcjonalnie"
    const val DISTANCE_PLACEHOLDER = "np. 14,2"
    const val MINUTES_PLACEHOLDER = "0"

    /** Najdłuższe sensowne cardio jednego dnia — 24 h. Chroni przed literówką. */
    const val MAX_MINUTES = 24 * 60

    /** Ponad maraton wagonem — dalej to już błąd wpisu, nie wynik. */
    const val MAX_DISTANCE_KM = 999.0

    /** Polska etykieta typu („Rower", „Bieg", …) — słownik żyje przy enumie. */
    fun typeLabel(type: CardioType): String = type.labelPl

    // ------------------------------------------------------------ formatowanie

    /** „42" — sama liczba minut; jednostka jedzie osobno jako [UNIT_MINUTES]. */
    fun minutesValue(durationMin: Int): String = durationMin.toString()

    /** „14,2" / „8" — sam dystans, przecinek dziesiętny, bez zbędnych zer. */
    fun distanceValue(distanceKm: Double): String = decimal(distanceKm)

    /** „60" / „62,5" — liczba bez zbędnych zer, z przecinkiem dziesiętnym. */
    private fun decimal(value: Double): String {
        val rounded = (value * 100).roundToInt() / 100.0
        return if (rounded % 1.0 == 0.0) {
            rounded.toInt().toString()
        } else {
            rounded.toString().replace('.', ',')
        }
    }

    // ------------------------------------------------------------- wpisywanie

    /**
     * Filtr pola minut: same cyfry, bez wiodących zer, maks 4 znaki. Klawiatura
     * numeryczna i tak wpuszcza śmieci (schowek, klawiatury sprzętowe).
     */
    fun sanitizeMinutes(raw: String): String {
        val digits = raw.filter { it.isDigit() }.take(4)
        val trimmed = digits.trimStart('0')
        return if (trimmed.isEmpty() && digits.isNotEmpty()) "0" else trimmed
    }

    /**
     * Filtr pola dystansu: cyfry i JEDEN separator dziesiętny (kropka z
     * klawiatury zamienia się w przecinek), maks 2 miejsca po przecinku.
     */
    fun sanitizeDistance(raw: String): String {
        val normalized = raw.replace('.', ',').filter { it.isDigit() || it == ',' }
        val separator = normalized.indexOf(',')
        if (separator < 0) return normalized.take(4)
        val whole = normalized.substring(0, separator).take(4)
        // Drugi separator jest ignorowany razem z ogonem — „14,2,5" to „14,2",
        // a nie sklejone „14,25" (wpisanie przecinka drugi raz to literówka).
        val fraction = normalized.substring(separator + 1).takeWhile { it.isDigit() }.take(2)
        return "$whole,$fraction"
    }

    // -------------------------------------------------------------- walidacja

    /** Minuty z pola: null gdy puste, zerowe albo poza [MAX_MINUTES]. */
    fun parseMinutes(text: String): Int? =
        text.trim().toIntOrNull()?.takeIf { it > 0 && it <= MAX_MINUTES }

    /**
     * Dystans z pola: null gdy pusty (to pole OPCJONALNE — brak wartości jest
     * poprawnym stanem) albo gdy wartość nie ma sensu.
     */
    fun parseDistance(text: String): Double? = text.trim()
        .replace(',', '.')
        .removeSuffix(".")
        .toDoubleOrNull()
        ?.takeIf { it > 0.0 && it <= MAX_DISTANCE_KM }

    /**
     * Czy „Zapisz" jest aktywne. Warunek jest JEDEN: minuty muszą być sensowne.
     * Dystans wpisany błędnie po prostu nie zapisze się jako dystans — nie
     * blokuje wpisu, bo jest opcjonalny.
     */
    fun canSave(minutesText: String): Boolean = parseMinutes(minutesText) != null

    /** Wartość pola minut przy edycji istniejącego wpisu. */
    fun minutesInput(durationMin: Int): String = durationMin.toString()

    /** Wartość pola dystansu przy edycji istniejącego wpisu („" gdy brak). */
    fun distanceInput(distanceKm: Double?): String =
        distanceKm?.let { distanceValue(it) }.orEmpty()
}
