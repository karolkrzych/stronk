package com.stronk.ui.progress

import com.stronk.data.SetLog
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Formatowanie wartości progresu po polsku (jedno miejsce zmian dla całego
 * pakietu progress). Etykiety wartości datasetu są w [com.stronk.ui.PlLabels] —
 * tu tylko liczby, daty i odmiana.
 */
internal object ProgressFormat {

    private val polish = Locale.forLanguageTag("pl")
    private val sameYearFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", polish)
    private val otherYearFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", polish)
    private val axisFormatter = DateTimeFormatter.ofPattern("d.MM", polish)

    /** Data treningu, np. "wtorek, 18 sierpnia" (z rokiem, gdy inny niż bieżący). */
    fun date(epochMillis: Long): String {
        val date = toLocalDate(epochMillis)
        val formatter =
            if (date.year == LocalDate.now().year) sameYearFormatter else otherYearFormatter
        return formatter.format(date)
    }

    /** Krótka data na oś wykresu, np. "18.08". */
    fun axisDate(epochMillis: Long): String = axisFormatter.format(toLocalDate(epochMillis))

    /** Ciężar, np. "80 kg" albo "82,5 kg" (bez zbędnego ",0"). */
    fun kg(value: Double): String {
        val rounded = (value * 10).roundToLong() / 10.0
        return if (rounded % 1.0 == 0.0) {
            "${rounded.toLong()} kg"
        } else {
            "${"%.1f".format(polish, rounded)} kg"
        }
    }

    /** Objętość, np. "3 250 kg" (grupowanie tysięcy po polsku). */
    fun volume(value: Double): String =
        NumberFormat.getIntegerInstance(polish).format(value.roundToLong()) + " kg"

    /** Czas serii, np. "45 s" albo "5 min" dla pełnych minut. */
    fun seconds(value: Int): String =
        if (value >= 60 && value % 60 == 0) "${value / 60} min" else "$value s"

    /** Dystans, np. "800 m" albo "2 km" dla pełnych kilometrów. */
    fun meters(value: Double): String {
        val rounded = value.roundToInt()
        return if (rounded >= 1000 && rounded % 1000 == 0) "${rounded / 1000} km" else "$rounded m"
    }

    /** Jedna zalogowana seria, np. "80 kg × 5", "12 powt. (+10 kg)", "1 km · 5 min". */
    fun setLabel(set: SetLog): String = when (set) {
        is SetLog.WeightReps -> "${kg(set.kg)} × ${set.reps}"
        is SetLog.Reps -> set.extraKg
            ?.takeIf { it > 0 }
            ?.let { "${set.reps} powt. (+${kg(it)})" }
            ?: "${set.reps} powt."

        is SetLog.Time -> seconds(set.seconds)
        is SetLog.DistanceTime -> "${meters(set.meters)} · ${seconds(set.seconds)}"
    }

    /** Nazwa rodzaju rekordu (małą literą — do składania w zdania). */
    fun prKindLabel(kind: PrKind): String = when (kind) {
        PrKind.MAX_WEIGHT -> "najcięższa seria"
        PrKind.SESSION_VOLUME -> "najlepsza objętość"
        PrKind.MAX_REPS -> "najwięcej powtórzeń"
        PrKind.MAX_TIME -> "najdłuższy czas"
        PrKind.MAX_DISTANCE -> "najdłuższy dystans"
    }

    /** Wartość rekordu we właściwej jednostce. */
    fun prValue(kind: PrKind, value: Double): String = when (kind) {
        PrKind.MAX_WEIGHT -> kg(value)
        PrKind.SESSION_VOLUME -> volume(value)
        PrKind.MAX_REPS -> "${value.roundToInt()} powt."
        PrKind.MAX_TIME -> seconds(value.roundToInt())
        PrKind.MAX_DISTANCE -> meters(value)
    }

    /** Etykieta metryki wykresu (chip przełącznika). */
    fun metricLabel(metric: ChartMetric): String = when (metric) {
        ChartMetric.WEIGHT -> "Ciężar"
        ChartMetric.VOLUME -> "Objętość"
        ChartMetric.REPS -> "Powtórzenia"
        ChartMetric.TIME -> "Czas"
        ChartMetric.DISTANCE -> "Dystans"
    }

    /** Wartość metryki na osi/etykiecie wykresu. */
    fun metricValue(metric: ChartMetric, value: Double): String = when (metric) {
        ChartMetric.WEIGHT -> kg(value)
        ChartMetric.VOLUME -> volume(value)
        ChartMetric.REPS -> value.roundToInt().toString()
        ChartMetric.TIME -> seconds(value.roundToInt())
        ChartMetric.DISTANCE -> meters(value)
    }

    fun exercisesCount(count: Int): String =
        "$count ${plural(count, "ćwiczenie", "ćwiczenia", "ćwiczeń")}"

    fun setsCount(count: Int): String = "$count ${plural(count, "seria", "serie", "serii")}"

    fun sessionsCount(count: Int): String =
        "$count ${plural(count, "trening", "treningi", "treningów")}"

    /** Polska odmiana rzeczownika po liczebniku (1 seria / 2 serie / 5 serii). */
    private fun plural(count: Int, one: String, few: String, many: String): String = when {
        count == 1 -> one
        count % 10 in 2..4 && count % 100 !in 12..14 -> few
        else -> many
    }

    private fun toLocalDate(epochMillis: Long): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
}
