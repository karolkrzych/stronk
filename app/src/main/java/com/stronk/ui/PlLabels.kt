package com.stronk.ui

/**
 * Słownik etykiet PL dla wartości z datasetu (jeden plik — jedno miejsce zmian).
 * Wartości nieznane wracają bez zmian, żeby ewolucja datasetu niczego nie ukryła.
 */
object PlLabels {

    private val muscles = mapOf(
        "abdominals" to "brzuch",
        "abductors" to "odwodziciele",
        "adductors" to "przywodziciele",
        "biceps" to "biceps",
        "calves" to "łydki",
        "chest" to "klatka piersiowa",
        "forearms" to "przedramiona",
        "glutes" to "pośladki",
        "hamstrings" to "dwugłowe uda",
        "lats" to "najszersze grzbietu",
        "lower back" to "dolny odcinek pleców",
        "middle back" to "środek pleców",
        "neck" to "szyja",
        "quadriceps" to "czworogłowe uda",
        "shoulders" to "barki",
        "traps" to "kaptury",
        "triceps" to "triceps",
    )

    private val equipment = mapOf(
        "body only" to "masa ciała",
        "machine" to "maszyna",
        "smith machine" to "suwnica Smitha",
        "leverage machine" to "maszyna dźwigniowa",
        "leg machine" to "maszyna do nóg",
        "cardio machine" to "sprzęt cardio",
        "other" to "inne",
        "foam roll" to "wałek piankowy",
        "kettlebells" to "kettlebell",
        "dumbbell" to "hantle",
        "cable" to "wyciąg",
        "barbell" to "sztanga",
        "bands" to "gumy oporowe",
        "medicine ball" to "piłka lekarska",
        "exercise ball" to "piłka gimnastyczna",
        "e-z curl bar" to "gryf łamany",
    )

    private val levels = mapOf(
        "beginner" to "początkujący",
        "intermediate" to "średniozaawansowany",
        "expert" to "zaawansowany",
    )

    private val categories = mapOf(
        "strength" to "siła",
        "stretching" to "rozciąganie",
        "plyometrics" to "plyometria",
        "strongman" to "strongman",
        "powerlifting" to "trójbój siłowy",
        "cardio" to "cardio",
        "olympic weightlifting" to "dwubój olimpijski",
    )

    private val mechanics = mapOf(
        "compound" to "wielostawowe",
        "isolation" to "izolowane",
    )

    private val forces = mapOf(
        "push" to "pchanie",
        "pull" to "przyciąganie",
        "static" to "statyczne",
    )

    private val joints = mapOf(
        "lowBack" to "dolny odcinek kręgosłupa",
        "knee" to "kolano",
        "shoulder" to "bark",
        "hip" to "biodro",
        "elbow" to "łokieć",
        "wrist" to "nadgarstek",
        "neck" to "szyja/kark",
    )

    fun muscle(value: String): String = muscles[value] ?: value

    fun equipment(value: String?): String = value?.let { equipment[it] ?: it } ?: "bez sprzętu"

    fun level(value: String): String = levels[value] ?: value

    fun category(value: String): String = categories[value] ?: value

    fun mechanic(value: String?): String? = value?.let { mechanics[it] ?: it }

    fun force(value: String?): String? = value?.let { forces[it] ?: it }

    fun joint(value: String): String = joints[value] ?: value
}
