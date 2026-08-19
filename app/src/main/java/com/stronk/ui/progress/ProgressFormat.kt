package com.stronk.ui.progress

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Formatowanie wartości progresu po polsku (jedno miejsce zmian dla pakietów
 * progress / detail). Etykiety wartości datasetu są w [com.stronk.ui.PlLabels].
 *
 * **Zasada twarda (Karol):** liczba i jednostka to OSOBNE byty. Dlatego tu nie
 * ma i nie będzie funkcji sklejającej „40 kg × 10" — są pary
 * [decimal]/[weightUnit], [timeValue]/[timeUnit], [distanceValue]/[distanceUnit],
 * a UI składa z nich stat-blok albo kolumnę z nagłówkiem raz na sekcję.
 */
internal object ProgressFormat {

    private val polish = Locale.forLanguageTag("pl")
    private val shortDateFormatter = DateTimeFormatter.ofPattern("dd.MM", polish)

    /** Krótka data sesji, np. "05.08" (mock: lewa szyna tabeli sesji). */
    fun shortDate(epochMillis: Long): String = shortDateFormatter.format(toLocalDate(epochMillis))

    /** Sama liczba, bez jednostki: "40", "37,5" (bez zbędnego ",0"). */
    fun decimal(value: Double): String {
        val rounded = (value * 10).roundToLong() / 10.0
        return if (rounded % 1.0 == 0.0) {
            rounded.toLong().toString()
        } else {
            "%.1f".format(polish, rounded)
        }
    }

    /** Liczba ze znakiem — przyrost na nagłówku wykresu, np. "+7,5" albo "−2,5". */
    fun signedDecimal(value: Double): String = when {
        value > 0 -> "+${decimal(value)}"
        value < 0 -> "−${decimal(-value)}"
        else -> "0"
    }

    /** Jednostka ciężaru — osobno od liczby. */
    const val weightUnit: String = "kg"

    /** Sama liczba czasu: "45" (sekundy) albo "5" (pełne minuty). */
    fun timeValue(seconds: Int): String =
        if (seconds >= 60 && seconds % 60 == 0) (seconds / 60).toString() else seconds.toString()

    /** Jednostka pasująca do [timeValue]. */
    fun timeUnit(seconds: Int): String =
        if (seconds >= 60 && seconds % 60 == 0) "min" else "s"

    /** Sama liczba dystansu: "800" (metry) albo "2" (pełne kilometry). */
    fun distanceValue(meters: Double): String {
        val rounded = meters.roundToInt()
        return if (rounded >= 1000 && rounded % 1000 == 0) (rounded / 1000).toString() else rounded.toString()
    }

    /** Jednostka pasująca do [distanceValue]. */
    fun distanceUnit(meters: Double): String {
        val rounded = meters.roundToInt()
        return if (rounded >= 1000 && rounded % 1000 == 0) "km" else "m"
    }

    /** Nazwa metryki wykresu, np. "Ciężar roboczy" (kapitalik nad wykresem). */
    fun metricLabel(metric: ChartMetric): String = when (metric) {
        ChartMetric.WEIGHT -> "Ciężar roboczy"
        ChartMetric.VOLUME -> "Objętość"
        ChartMetric.REPS -> "Powtórzenia"
        ChartMetric.TIME -> "Czas"
        ChartMetric.DISTANCE -> "Dystans"
    }

    /** Liczba sesji z odmianą: "1 sesja" / "3 sesje" / "8 sesji". */
    fun sessionsCount(count: Int): String =
        "$count ${plural(count, "sesja", "sesje", "sesji")}"

    /** Polska odmiana rzeczownika po liczebniku (1 sesja / 2 sesje / 5 sesji). */
    private fun plural(count: Int, one: String, few: String, many: String): String = when {
        count == 1 -> one
        count % 10 in 2..4 && count % 100 !in 12..14 -> few
        else -> many
    }

    private fun toLocalDate(epochMillis: Long): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
}
