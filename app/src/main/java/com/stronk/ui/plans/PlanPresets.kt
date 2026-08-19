package com.stronk.ui.plans

import com.stronk.data.StressLevel

/**
 * Wszystkie progi i wartości domyślne modułu planów w jednym miejscu —
 * zero magic numbers w logice i UI (zasada jak [com.stronk.progression.ProgressionConstants]).
 */
object PlanDefaults {

    /** Domyślna liczba serii nowo dodanego ćwiczenia. */
    const val DEFAULT_SETS = 3

    /** Domyślny cel powtórzeń (WEIGHT_REPS / REPS). */
    const val DEFAULT_REPS = 10

    /** Domyślny cel czasu w sekundach (TIME). */
    const val DEFAULT_TIME_SECONDS = 30

    /** Domyślny cel dystansu w metrach (DISTANCE_TIME). */
    const val DEFAULT_DISTANCE_METERS = 1000.0

    /** Domyślny cel czasu dla dystansu (DISTANCE_TIME) — spokojne ~6 min/km. */
    const val DEFAULT_DISTANCE_SECONDS = 360

    /** Zakres konfigurowalnej długości bloku — tygodnie PRACY (ADR-004). */
    const val BLOCK_WEEKS_MIN = 2
    const val BLOCK_WEEKS_MAX = 12

    /** Granice liczby serii w edytorze. */
    const val SETS_MIN = 1
    const val SETS_MAX = 10

    /** Ile propozycji zamienników pokazujemy w arkuszu. */
    const val SUBSTITUTE_LIMIT = 8

    /**
     * Limit ustawiany chipem w kroku „Twoje ograniczenia” — chip jest binarny,
     * więc bierzemy łagodniejszy z dwóch poziomów profilu (odpadają ćwiczenia
     * mocno obciążające). Dokładny poziom user dostroi w profilu.
     */
    val WIZARD_CONSTRAINT_LEVEL = StressLevel.MEDIUM
}

/**
 * Slot presetu: rola w planie + kandydaci z bundlowanej bazy w kolejności
 * preferencji. Parametryzacja profilem dzieje się w [generatePresetDays]:
 * kandydat odpada, gdy sprzęt niedostępny albo narusza ograniczenia — wtedy
 * wchodzi następny kandydat / zamiennik z [com.stronk.data.findSubstitutes].
 */
data class PresetSlot(
    /** Rola slotu po polsku, np. "Klatka — wyciskanie". */
    val label: String,
    /** exerciseId z bundlowanej bazy, od najbardziej preferowanego. */
    val candidateIds: List<String>,
    /**
     * Autorska liczba serii — od [generatePresetDays] NIE trafia już wprost do
     * planu (realne serie pochodzą z [com.stronk.data.GoalDefaults] wg celu
     * z profilu); zostaje jako walidowana wartość startowa i dokumentacja intencji.
     */
    val sets: Int,
    /**
     * Autorska liczba powtórzeń — jak [sets], realny cel liczy [generatePresetDays]
     * z [com.stronk.data.GoalDefaults]. Wartość ≥12 tutaj oznacza slot akcesoryjny/
     * izolowany (dostaje `accessoryReps` zamiast `defaultReps` — patrz `isAccessory()`
     * w `PresetGenerator.kt`); typy czas/dystans i tak dostają wartości domyślne
     * z [PlanDefaults].
     */
    val reps: Int,
)

/** Dzień presetu (np. "Push", "Full body A"). */
data class PresetDay(
    val name: String,
    val slots: List<PresetSlot>,
)

/** Gotowy szablon planu — czyste DANE, bez logiki. */
data class PlanPreset(
    val id: String,
    val name: String,
    /** Krótki opis pokazywany przy wyborze presetu. */
    val description: String,
    val days: List<PresetDay>,
) {
    val slotCount: Int get() = days.sumOf { it.slots.size }
}

/**
 * Definicje presetów (CONCEPT moduł 3: "wizard dla laika" w wersji mechanicznej).
 *
 * Kolejność kandydatów w slotach jest celowa: w presecie powrotowym najpierw
 * warianty najłagodniejsze dla kolana i dolnego odcinka pleców (kryterium
 * sukcesu alfy), w pozostałych — klasyka danego wzorca ruchu, potem warianty
 * na inny sprzęt.
 */
object PlanPresets {

    /** Priorytet alfy: powrót po przerwie z ograniczeniami kolano + L5-S1. */
    val fullBodyReturn = PlanPreset(
        id = "full-body-return-3",
        name = "Full Body 3×/tydz. (powrót po przerwie)",
        description = "Trzy lekkie treningi całego ciała: maszyny i warianty " +
            "oszczędzające kolana oraz dolny odcinek pleców.",
        days = listOf(
            PresetDay(
                name = "Full body A",
                slots = listOf(
                    PresetSlot(
                        label = "Nogi — przód uda",
                        candidateIds = listOf(
                            "Leg_Press",
                            "Leg_Extensions",
                            "Goblet_Squat",
                            "Bodyweight_Walking_Lunge",
                        ),
                        sets = 3,
                        reps = 10,
                    ),
                    PresetSlot(
                        label = "Nogi — tył uda",
                        candidateIds = listOf(
                            "Seated_Leg_Curl",
                            "Lying_Leg_Curls",
                            "Glute_Ham_Raise",
                        ),
                        sets = 3,
                        reps = 12,
                    ),
                    PresetSlot(
                        label = "Klatka — wyciskanie",
                        candidateIds = listOf(
                            "Machine_Bench_Press",
                            "Dumbbell_Bench_Press",
                            "Pushups",
                            "Barbell_Bench_Press_-_Medium_Grip",
                        ),
                        sets = 3,
                        reps = 10,
                    ),
                    PresetSlot(
                        label = "Plecy — wiosłowanie",
                        candidateIds = listOf(
                            "Leverage_Iso_Row",
                            "Dumbbell_Incline_Row",
                            "Inverted_Row",
                            "Seated_Cable_Rows",
                        ),
                        sets = 3,
                        reps = 10,
                    ),
                    PresetSlot(
                        label = "Barki — wyciskanie",
                        candidateIds = listOf(
                            "Leverage_Shoulder_Press",
                            "Machine_Shoulder_Military_Press",
                            "Dumbbell_Shoulder_Press",
                            "Side_Lateral_Raise",
                        ),
                        sets = 3,
                        reps = 10,
                    ),
                    PresetSlot(
                        label = "Core — stabilizacja",
                        candidateIds = listOf(
                            "Dead_Bug",
                            "Pallof_Press",
                            "Plank",
                        ),
                        sets = 3,
                        reps = 10,
                    ),
                ),
            ),
            PresetDay(
                name = "Full body B",
                slots = listOf(
                    PresetSlot(
                        label = "Pośladki — mostek biodrowy",
                        candidateIds = listOf(
                            "Barbell_Glute_Bridge",
                            "Physioball_Hip_Bridge",
                            "Hip_Extension_with_Bands",
                            "Butt_Lift_Bridge",
                        ),
                        sets = 3,
                        reps = 12,
                    ),
                    PresetSlot(
                        label = "Nogi — tył uda",
                        candidateIds = listOf(
                            "Lying_Leg_Curls",
                            "Seated_Leg_Curl",
                            "Stiff-Legged_Dumbbell_Deadlift",
                        ),
                        sets = 3,
                        reps = 12,
                    ),
                    PresetSlot(
                        label = "Plecy — ściąganie",
                        candidateIds = listOf(
                            "Close-Grip_Front_Lat_Pulldown",
                            "Wide-Grip_Lat_Pulldown",
                            "Band_Assisted_Pull-Up",
                            "Chin-Up",
                        ),
                        sets = 3,
                        reps = 10,
                    ),
                    PresetSlot(
                        label = "Klatka — wyciskanie",
                        candidateIds = listOf(
                            "Dumbbell_Bench_Press",
                            "Machine_Bench_Press",
                            "Pushups",
                        ),
                        sets = 3,
                        reps = 10,
                    ),
                    PresetSlot(
                        label = "Łydki",
                        candidateIds = listOf(
                            "Seated_Calf_Raise",
                            "Calf_Press_On_The_Leg_Press_Machine",
                            "Standing_Dumbbell_Calf_Raise",
                        ),
                        sets = 3,
                        reps = 15,
                    ),
                    PresetSlot(
                        label = "Core — antyrotacja",
                        candidateIds = listOf(
                            "Pallof_Press",
                            "Dead_Bug",
                            "Side_Bridge",
                        ),
                        sets = 3,
                        reps = 10,
                    ),
                ),
            ),
            PresetDay(
                name = "Full body C",
                slots = listOf(
                    PresetSlot(
                        label = "Nogi — suwnica / przysiad",
                        candidateIds = listOf(
                            "Leg_Press",
                            "Goblet_Squat",
                            "Leg_Extensions",
                        ),
                        sets = 3,
                        reps = 10,
                    ),
                    PresetSlot(
                        label = "Pośladki",
                        candidateIds = listOf(
                            "Hip_Extension_with_Bands",
                            "Barbell_Glute_Bridge",
                            "Butt_Lift_Bridge",
                        ),
                        sets = 3,
                        reps = 12,
                    ),
                    PresetSlot(
                        label = "Plecy — wiosłowanie hantlem",
                        candidateIds = listOf(
                            "Dumbbell_Incline_Row",
                            "One-Arm_Dumbbell_Row",
                            "Leverage_High_Row",
                        ),
                        sets = 3,
                        reps = 10,
                    ),
                    PresetSlot(
                        label = "Barki — wznosy bokiem",
                        candidateIds = listOf(
                            "Side_Lateral_Raise",
                            "Seated_Side_Lateral_Raise",
                            "Cable_Seated_Lateral_Raise",
                        ),
                        sets = 3,
                        reps = 12,
                    ),
                    PresetSlot(
                        label = "Biceps",
                        candidateIds = listOf(
                            "Dumbbell_Bicep_Curl",
                            "Hammer_Curls",
                            "Barbell_Curl",
                        ),
                        sets = 3,
                        reps = 12,
                    ),
                    PresetSlot(
                        label = "Triceps",
                        candidateIds = listOf(
                            "Triceps_Pushdown",
                            "Standing_Dumbbell_Triceps_Extension",
                            "Bench_Dips",
                        ),
                        sets = 3,
                        reps = 12,
                    ),
                ),
            ),
        ),
    )

    val pushPullLegs = PlanPreset(
        id = "push-pull-legs",
        name = "Push / Pull / Legs",
        description = "Klasyczny trójpodział: pchanie, przyciąganie, nogi — " +
            "dla wracających do pełnych obciążeń.",
        days = listOf(
            PresetDay(
                name = "Push",
                slots = listOf(
                    PresetSlot(
                        label = "Klatka — wyciskanie poziome",
                        candidateIds = listOf(
                            "Barbell_Bench_Press_-_Medium_Grip",
                            "Dumbbell_Bench_Press",
                            "Machine_Bench_Press",
                        ),
                        sets = 4,
                        reps = 8,
                    ),
                    PresetSlot(
                        label = "Klatka — wyciskanie na skosie",
                        candidateIds = listOf(
                            "Barbell_Incline_Bench_Press_-_Medium_Grip",
                            "Hammer_Grip_Incline_DB_Bench_Press",
                            "Leverage_Incline_Chest_Press",
                        ),
                        sets = 3,
                        reps = 10,
                    ),
                    PresetSlot(
                        label = "Barki — wyciskanie",
                        candidateIds = listOf(
                            "Barbell_Shoulder_Press",
                            "Dumbbell_Shoulder_Press",
                            "Leverage_Shoulder_Press",
                        ),
                        sets = 3,
                        reps = 10,
                    ),
                    PresetSlot(
                        label = "Barki — wznosy bokiem",
                        candidateIds = listOf(
                            "Side_Lateral_Raise",
                            "Cable_Seated_Lateral_Raise",
                            "Lateral_Raise_-_With_Bands",
                        ),
                        sets = 3,
                        reps = 12,
                    ),
                    PresetSlot(
                        label = "Triceps",
                        candidateIds = listOf(
                            "Triceps_Pushdown",
                            "Standing_Dumbbell_Triceps_Extension",
                            "Bench_Dips",
                        ),
                        sets = 3,
                        reps = 12,
                    ),
                ),
            ),
            PresetDay(
                name = "Pull",
                slots = listOf(
                    PresetSlot(
                        label = "Plecy — wiosłowanie",
                        candidateIds = listOf(
                            "Bent_Over_Barbell_Row",
                            "One-Arm_Dumbbell_Row",
                            "Seated_Cable_Rows",
                            "Leverage_Iso_Row",
                        ),
                        sets = 4,
                        reps = 8,
                    ),
                    PresetSlot(
                        label = "Plecy — pion",
                        candidateIds = listOf(
                            "Pullups",
                            "Wide-Grip_Lat_Pulldown",
                            "Band_Assisted_Pull-Up",
                            "Chin-Up",
                        ),
                        sets = 3,
                        reps = 8,
                    ),
                    PresetSlot(
                        label = "Tył barków",
                        candidateIds = listOf(
                            "Face_Pull",
                            "Cable_Rope_Rear-Delt_Rows",
                            "Dumbbell_Lying_Rear_Lateral_Raise",
                        ),
                        sets = 3,
                        reps = 15,
                    ),
                    PresetSlot(
                        label = "Biceps",
                        candidateIds = listOf(
                            "Barbell_Curl",
                            "Dumbbell_Bicep_Curl",
                            "Hammer_Curls",
                        ),
                        sets = 3,
                        reps = 12,
                    ),
                    PresetSlot(
                        label = "Grzbiet — wyprosty",
                        candidateIds = listOf(
                            "Hyperextensions_Back_Extensions",
                            "Hyperextensions_With_No_Hyperextension_Bench",
                        ),
                        sets = 3,
                        reps = 12,
                    ),
                ),
            ),
            PresetDay(
                name = "Legs",
                slots = listOf(
                    PresetSlot(
                        label = "Przysiad",
                        candidateIds = listOf(
                            "Barbell_Squat",
                            "Goblet_Squat",
                            "Leg_Press",
                        ),
                        sets = 4,
                        reps = 8,
                    ),
                    PresetSlot(
                        label = "Zawias biodrowy",
                        candidateIds = listOf(
                            "Romanian_Deadlift",
                            "Stiff-Legged_Dumbbell_Deadlift",
                            "Barbell_Glute_Bridge",
                        ),
                        sets = 3,
                        reps = 10,
                    ),
                    PresetSlot(
                        label = "Nogi — przód uda",
                        candidateIds = listOf(
                            "Leg_Press",
                            "Dumbbell_Lunges",
                            "Leg_Extensions",
                        ),
                        sets = 3,
                        reps = 10,
                    ),
                    PresetSlot(
                        label = "Nogi — tył uda",
                        candidateIds = listOf(
                            "Lying_Leg_Curls",
                            "Seated_Leg_Curl",
                            "Glute_Ham_Raise",
                        ),
                        sets = 3,
                        reps = 12,
                    ),
                    PresetSlot(
                        label = "Łydki",
                        candidateIds = listOf(
                            "Standing_Calf_Raises",
                            "Seated_Calf_Raise",
                            "Standing_Dumbbell_Calf_Raise",
                        ),
                        sets = 4,
                        reps = 12,
                    ),
                    PresetSlot(
                        label = "Core",
                        candidateIds = listOf(
                            "Dead_Bug",
                            "Plank",
                            "Air_Bike",
                        ),
                        sets = 3,
                        reps = 10,
                    ),
                ),
            ),
        ),
    )

    val fullBodyTwice = PlanPreset(
        id = "full-body-2",
        name = "Full Body 2×/tydz.",
        description = "Dwa treningi całego ciała — minimum czasu, " +
            "podstawowe wzorce ruchu.",
        days = listOf(
            PresetDay(
                name = "Full body A",
                slots = listOf(
                    PresetSlot(
                        label = "Przysiad / suwnica",
                        candidateIds = listOf(
                            "Barbell_Squat",
                            "Leg_Press",
                            "Goblet_Squat",
                        ),
                        sets = 3,
                        reps = 8,
                    ),
                    PresetSlot(
                        label = "Klatka — wyciskanie",
                        candidateIds = listOf(
                            "Barbell_Bench_Press_-_Medium_Grip",
                            "Dumbbell_Bench_Press",
                            "Machine_Bench_Press",
                        ),
                        sets = 3,
                        reps = 8,
                    ),
                    PresetSlot(
                        label = "Plecy — wiosłowanie",
                        candidateIds = listOf(
                            "Bent_Over_Barbell_Row",
                            "Seated_Cable_Rows",
                            "One-Arm_Dumbbell_Row",
                        ),
                        sets = 3,
                        reps = 10,
                    ),
                    PresetSlot(
                        label = "Barki — wznosy bokiem",
                        candidateIds = listOf(
                            "Side_Lateral_Raise",
                            "Cable_Seated_Lateral_Raise",
                        ),
                        sets = 3,
                        reps = 12,
                    ),
                    PresetSlot(
                        label = "Core",
                        candidateIds = listOf(
                            "Dead_Bug",
                            "Plank",
                        ),
                        sets = 3,
                        reps = 10,
                    ),
                ),
            ),
            PresetDay(
                name = "Full body B",
                slots = listOf(
                    PresetSlot(
                        label = "Zawias biodrowy",
                        candidateIds = listOf(
                            "Romanian_Deadlift",
                            "Stiff-Legged_Dumbbell_Deadlift",
                            "Lying_Leg_Curls",
                        ),
                        sets = 3,
                        reps = 10,
                    ),
                    PresetSlot(
                        label = "Barki — wyciskanie",
                        candidateIds = listOf(
                            "Standing_Military_Press",
                            "Dumbbell_Shoulder_Press",
                            "Leverage_Shoulder_Press",
                        ),
                        sets = 3,
                        reps = 8,
                    ),
                    PresetSlot(
                        label = "Plecy — pion",
                        candidateIds = listOf(
                            "Wide-Grip_Lat_Pulldown",
                            "Pullups",
                            "Close-Grip_Front_Lat_Pulldown",
                        ),
                        sets = 3,
                        reps = 10,
                    ),
                    PresetSlot(
                        label = "Biceps",
                        candidateIds = listOf(
                            "Barbell_Curl",
                            "Dumbbell_Bicep_Curl",
                        ),
                        sets = 3,
                        reps = 12,
                    ),
                    PresetSlot(
                        label = "Triceps",
                        candidateIds = listOf(
                            "Triceps_Pushdown",
                            "Bench_Dips",
                        ),
                        sets = 3,
                        reps = 12,
                    ),
                ),
            ),
        ),
    )

    /** Wszystkie presety w kolejności pokazywania (powrotowy pierwszy — priorytet alfy). */
    val all: List<PlanPreset> = listOf(fullBodyReturn, pushPullLegs, fullBodyTwice)
}
