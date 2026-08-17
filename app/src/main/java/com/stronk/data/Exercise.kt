package com.stronk.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Ćwiczenie z bundlowanego datasetu (assets/exercises.json).
 * Schemat 1:1 z wynikiem scripts/build-dataset.mjs.
 */
@Serializable
data class Exercise(
    val id: String,
    /** Nazwa oryginalna (angielska) — pokazywana drobnym drukiem. */
    val name: String,
    val namePl: String,
    val instructionsPl: List<String>,
    val primaryMuscles: List<String>,
    val secondaryMuscles: List<String>,
    val equipment: String? = null,
    val level: String,
    val category: String,
    val mechanic: String? = null,
    val force: String? = null,
    /** Ścieżki względne w assets/exercise-images/, np. "3_4_Sit-Up/0.jpg". */
    val images: List<String>,
    val jointStress: JointStress,
    val cautionNotes: String? = null,
    val measurementType: MeasurementType,
) {
    /** Czy którykolwiek staw ma obciążenie HIGH — wtedy pokazujemy badge ostrzegawczy. */
    val hasHighJointStress: Boolean
        get() = jointStress.all.any { (_, level) -> level == StressLevel.HIGH }
}

/** Sposób mierzenia serii danego ćwiczenia (heurystyka ze skryptu build-dataset). */
@Serializable
enum class MeasurementType {
    WEIGHT_REPS,
    REPS,
    TIME,
    DISTANCE_TIME,
}

/** Poziom obciążenia stawu. */
@Serializable
enum class StressLevel {
    @SerialName("high") HIGH,
    @SerialName("medium") MEDIUM,
    @SerialName("low") LOW,
    @SerialName("none") NONE,
}

/** Obciążenie siedmiu stawów/okolic dla ćwiczenia. */
@Serializable
data class JointStress(
    val lowBack: StressLevel,
    val knee: StressLevel,
    val shoulder: StressLevel,
    val hip: StressLevel,
    val elbow: StressLevel,
    val wrist: StressLevel,
    val neck: StressLevel,
) {
    /** Mapa staw → poziom; klucze zgodne z nazwami pól JSON. */
    val all: Map<String, StressLevel>
        get() = mapOf(
            "lowBack" to lowBack,
            "knee" to knee,
            "shoulder" to shoulder,
            "hip" to hip,
            "elbow" to elbow,
            "wrist" to wrist,
            "neck" to neck,
        )
}
