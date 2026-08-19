package com.stronk.ui.schedule

/**
 * Znacznik kwadratu dnia w siatce — to, co widać, a nie to, co jest w bazie.
 * Rozdzielone od [ScheduleDayStatus], bo cardio nie jest stanem harmonogramu:
 * to osobny fakt, który nakłada się na dzień.
 */
enum class DayMarker {
    /** Trening siłowy zrobiony — kwadrat WYPEŁNIONY `--lime-deep`. */
    DONE,

    /** Trening + cardio — wypełniony i z wewnętrznym ringiem (mock `.day.both`). */
    DONE_WITH_CARDIO,

    /** Samo cardio — sam OBRYS `--lime-deep` (mock `.day.cardio`). */
    CARDIO,

    /** Trening zaplanowany (albo przeszły niezaliczony) — obrys `--line`. */
    PLANNED,

    /** Dzień bez niczego — ledwie widoczna powierzchnia. */
    FREE,
}

/**
 * Reguły markerów kalendarza — CZYSTA funkcja, żeby dało się je przetestować
 * bez Compose'a i żeby ekran nie wymyślał ich po swojemu.
 *
 * Kolejność ważności jest jedna i twarda:
 * 1. zrobiony trening wygrywa nad wszystkim (wypełnienie = „trening zrobiony"),
 *    cardio dokłada wtedy tylko wewnętrzny ring,
 * 2. samo cardio ma własny znacznik — obrys limonką przygaszoną,
 * 3. plan (także przeszły niezaliczony) to obrys neutralny,
 * 4. reszta to dzień pusty.
 */
object CalendarMarkers {

    fun marker(status: ScheduleDayStatus, hasCardio: Boolean): DayMarker = when {
        status == ScheduleDayStatus.DONE && hasCardio -> DayMarker.DONE_WITH_CARDIO
        status == ScheduleDayStatus.DONE -> DayMarker.DONE
        // Cardio to FAKT, plan dopiero zamiar — fakt wygrywa o obrys.
        hasCardio -> DayMarker.CARDIO
        status == ScheduleDayStatus.PLANNED || status == ScheduleDayStatus.MISSED -> DayMarker.PLANNED
        else -> DayMarker.FREE
    }

    /** Czy w siatce jest cokolwiek cardio — decyduje o trzeciej pozycji legendy. */
    fun anyCardio(markers: List<DayMarker>): Boolean =
        markers.any { it == DayMarker.CARDIO || it == DayMarker.DONE_WITH_CARDIO }
}
