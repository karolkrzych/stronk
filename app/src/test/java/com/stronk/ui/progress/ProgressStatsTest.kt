package com.stronk.ui.progress

import com.stronk.data.SetLog
import com.stronk.data.Workout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Testy czystych funkcji progresu: objętość, rekordy osobiste, punkty wykresu. */
class ProgressStatsTest {

    private fun weight(
        exerciseId: String = "squat",
        kg: Double,
        reps: Int,
        setNumber: Int = 1,
        warmup: Boolean = false,
    ) = SetLog.WeightReps(
        exerciseId = exerciseId, workoutId = "w", setNumber = setNumber,
        isWarmup = warmup, timestamp = 0L, kg = kg, reps = reps,
    )

    private fun reps(exerciseId: String = "pushup", count: Int, warmup: Boolean = false) =
        SetLog.Reps(
            exerciseId = exerciseId, workoutId = "w", setNumber = 1,
            isWarmup = warmup, timestamp = 0L, reps = count,
        )

    private fun time(exerciseId: String = "plank", seconds: Int) = SetLog.Time(
        exerciseId = exerciseId, workoutId = "w", setNumber = 1,
        isWarmup = false, timestamp = 0L, seconds = seconds,
    )

    private fun distance(exerciseId: String = "run", meters: Double, seconds: Int) =
        SetLog.DistanceTime(
            exerciseId = exerciseId, workoutId = "w", setNumber = 1,
            isWarmup = false, timestamp = 0L, meters = meters, seconds = seconds,
        )

    private fun workout(id: String, startedAt: Long, sets: List<SetLog>) =
        Workout(id = id, startedAt = startedAt, sets = sets)

    private fun record(records: List<PersonalRecord>, exerciseId: String, kind: PrKind) =
        records.firstOrNull { it.exerciseId == exerciseId && it.kind == kind }

    // --- workoutVolume ---

    @Test
    fun `workoutVolume liczy tylko serie robocze WEIGHT_REPS`() {
        val w = workout(
            "w1", 1_000L,
            listOf(
                weight(kg = 60.0, reps = 5),
                weight(kg = 60.0, reps = 5, setNumber = 2),
                weight(kg = 40.0, reps = 8, warmup = true), // rozgrzewka nie wchodzi
                reps(count = 12), // typ bez ciężaru nie wchodzi
            ),
        )

        assertEquals(600.0, workoutVolume(w), 1e-9)
    }

    // --- computePersonalRecords ---

    @Test
    fun `rekordy - najcięższa seria i najlepsza objętość z całej historii`() {
        val w1 = workout(
            "w1", 1_000L,
            listOf(weight(kg = 60.0, reps = 5), weight(kg = 70.0, reps = 3, setNumber = 2)),
        )
        // Lżejszy trening, ale większa objętość.
        val w2 = workout(
            "w2", 2_000L,
            listOf(weight(kg = 65.0, reps = 8), weight(kg = 65.0, reps = 8, setNumber = 2)),
        )

        val records = computePersonalRecords(listOf(w1, w2))

        val maxWeight = record(records, "squat", PrKind.MAX_WEIGHT)!!
        assertEquals(70.0, maxWeight.value, 1e-9)
        assertEquals("w1", maxWeight.workoutId)
        val volume = record(records, "squat", PrKind.SESSION_VOLUME)!!
        assertEquals(1040.0, volume.value, 1e-9)
        assertEquals("w2", volume.workoutId)
    }

    @Test
    fun `rekordy - kolejność wejściowa treningów nie ma znaczenia`() {
        val w1 = workout("w1", 1_000L, listOf(weight(kg = 60.0, reps = 5)))
        val w2 = workout("w2", 2_000L, listOf(weight(kg = 80.0, reps = 5)))

        val records = computePersonalRecords(listOf(w2, w1)) // odwrotnie niż chronologia

        assertEquals("w2", record(records, "squat", PrKind.MAX_WEIGHT)!!.workoutId)
        assertEquals(2_000L, record(records, "squat", PrKind.MAX_WEIGHT)!!.achievedAt)
    }

    @Test
    fun `wyrównanie rekordu nie przejmuje autorstwa`() {
        val sets = listOf(weight(kg = 80.0, reps = 5))
        val w1 = workout("w1", 1_000L, sets)
        val w2 = workout("w2", 2_000L, sets) // identyczny wynik później

        val records = computePersonalRecords(listOf(w1, w2))

        // Autorem obu rekordów (ciężar i objętość) zostaje pierwszy trening…
        assertTrue(records.isNotEmpty())
        assertTrue(records.all { it.workoutId == "w1" })
        // …więc najnowszy trening nie celebruje niczego.
        assertTrue(newRecordsInLatestWorkout(listOf(w1, w2)).isEmpty())
    }

    @Test
    fun `nowy rekord w najnowszym treningu jest wykrywany`() {
        val w1 = workout("w1", 1_000L, listOf(weight(kg = 60.0, reps = 5)))
        val w2 = workout("w2", 2_000L, listOf(weight(kg = 62.5, reps = 5)))

        val newRecords = newRecordsInLatestWorkout(listOf(w1, w2))

        assertEquals(
            setOf(PrKind.MAX_WEIGHT, PrKind.SESSION_VOLUME),
            newRecords.map { it.kind }.toSet(),
        )
        assertTrue(newRecords.all { it.workoutId == "w2" })
    }

    @Test
    fun `serie rozgrzewkowe nie liczą się do rekordów`() {
        val w = workout(
            "w1", 1_000L,
            listOf(weight(kg = 100.0, reps = 3, warmup = true), weight(kg = 60.0, reps = 5)),
        )

        val records = computePersonalRecords(listOf(w))

        assertEquals(60.0, record(records, "squat", PrKind.MAX_WEIGHT)!!.value, 1e-9)
    }

    @Test
    fun `rekordy dla typów bez ciężaru - powtórzenia czas dystans`() {
        val w1 = workout(
            "w1", 1_000L,
            listOf(reps(count = 10), time(seconds = 45), distance(meters = 800.0, seconds = 300)),
        )
        val w2 = workout(
            "w2", 2_000L,
            listOf(reps(count = 12), time(seconds = 40), distance(meters = 1000.0, seconds = 360)),
        )

        val records = computePersonalRecords(listOf(w1, w2))

        assertEquals(12.0, record(records, "pushup", PrKind.MAX_REPS)!!.value, 1e-9)
        assertEquals("w2", record(records, "pushup", PrKind.MAX_REPS)!!.workoutId)
        assertEquals(45.0, record(records, "plank", PrKind.MAX_TIME)!!.value, 1e-9)
        assertEquals("w1", record(records, "plank", PrKind.MAX_TIME)!!.workoutId)
        assertEquals(1000.0, record(records, "run", PrKind.MAX_DISTANCE)!!.value, 1e-9)
        // Typy bez ciężaru nie generują rekordów wagowych.
        assertNull(record(records, "pushup", PrKind.MAX_WEIGHT))
        assertNull(record(records, "plank", PrKind.SESSION_VOLUME))
    }

    @Test
    fun `rekordy są liczone per ćwiczenie`() {
        val w = workout(
            "w1", 1_000L,
            listOf(weight(exerciseId = "squat", kg = 80.0, reps = 5), weight(exerciseId = "bench", kg = 60.0, reps = 5)),
        )

        val records = computePersonalRecords(listOf(w))

        assertEquals(80.0, record(records, "squat", PrKind.MAX_WEIGHT)!!.value, 1e-9)
        assertEquals(60.0, record(records, "bench", PrKind.MAX_WEIGHT)!!.value, 1e-9)
    }

    @Test
    fun `pusta historia nie daje rekordów`() {
        assertTrue(computePersonalRecords(emptyList()).isEmpty())
        assertTrue(newRecordsInLatestWorkout(emptyList()).isEmpty())
    }

    // --- chartPoints ---

    @Test
    fun `chartPoints są chronologiczne i pomijają treningi bez ćwiczenia`() {
        val w1 = workout("w1", 1_000L, listOf(weight(kg = 60.0, reps = 5)))
        val w2 = workout("w2", 2_000L, listOf(weight(exerciseId = "bench", kg = 40.0, reps = 8)))
        val w3 = workout("w3", 3_000L, listOf(weight(kg = 62.5, reps = 5), weight(kg = 65.0, reps = 3, setNumber = 2)))

        // Wejście w kolejności "z repozytorium" (malejąco po startedAt).
        val points = chartPoints(listOf(w3, w2, w1), "squat", ChartMetric.WEIGHT)

        assertEquals(listOf(1_000L, 3_000L), points.map { it.startedAt })
        assertEquals(listOf(60.0, 65.0), points.map { it.value })
    }

    @Test
    fun `chartPoints VOLUME sumuje robocze serie sesji`() {
        val w = workout(
            "w1", 1_000L,
            listOf(
                weight(kg = 60.0, reps = 5),
                weight(kg = 60.0, reps = 5, setNumber = 2),
                weight(kg = 40.0, reps = 8, warmup = true),
            ),
        )

        val points = chartPoints(listOf(w), "squat", ChartMetric.VOLUME)

        assertEquals(1, points.size)
        assertEquals(600.0, points.single().value, 1e-9)
    }

    @Test
    fun `chartPoints pomija trening z samymi rozgrzewkami`() {
        val w = workout("w1", 1_000L, listOf(weight(kg = 40.0, reps = 8, warmup = true)))

        assertTrue(chartPoints(listOf(w), "squat", ChartMetric.WEIGHT).isEmpty())
    }

    @Test
    fun `chartPoints dla czasu i dystansu biorą najlepszą serię sesji`() {
        val w = workout(
            "w1", 1_000L,
            listOf(time(seconds = 30), time(seconds = 45), distance(meters = 800.0, seconds = 240)),
        )

        assertEquals(45.0, chartPoints(listOf(w), "plank", ChartMetric.TIME).single().value, 1e-9)
        assertEquals(800.0, chartPoints(listOf(w), "run", ChartMetric.DISTANCE).single().value, 1e-9)
        // Metryka nie pasująca do typu serii nie daje punktów.
        assertTrue(chartPoints(listOf(w), "plank", ChartMetric.WEIGHT).isEmpty())
    }
}
