package com.stronk.ui.progress

import com.stronk.data.SetLog
import com.stronk.data.Workout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testy warstwy „jak pokazać historię ćwiczenia": rekord jako DWIE osobne
 * liczby, tabela sesji w kolumnach i słupki wykresu. Zasada twarda: nigdzie
 * nie powstaje fraza typu „40 kg × 8" — liczba i jednostka są osobno.
 */
class ExerciseHistoryTest {

    private fun weight(
        kg: Double,
        reps: Int,
        setNumber: Int = 1,
        exerciseId: String = "squat",
        warmup: Boolean = false,
    ) = SetLog.WeightReps(
        exerciseId = exerciseId, workoutId = "w", setNumber = setNumber,
        isWarmup = warmup, timestamp = 0L, kg = kg, reps = reps,
    )

    private fun workout(id: String, startedAt: Long, sets: List<SetLog>) =
        Workout(id = id, startedAt = startedAt, sets = sets)

    /** Trzy sesje: 32,5 → 35 → 40 kg, ostatnia z rekordem. */
    private fun history() = listOf(
        workout(
            "w1", 1_000L,
            listOf(weight(kg = 32.5, reps = 12), weight(kg = 32.5, reps = 11, setNumber = 2)),
        ),
        workout(
            "w2", 2_000L,
            listOf(weight(kg = 35.0, reps = 10), weight(kg = 35.0, reps = 9, setNumber = 2)),
        ),
        workout(
            "w3", 3_000L,
            listOf(weight(kg = 40.0, reps = 8), weight(kg = 37.5, reps = 8, setNumber = 2)),
        ),
    )

    // --- rekord ---

    @Test
    fun `rekord to dwie osobne liczby - ciezar i powtorzenia rekordowej serii`() {
        val record = exerciseRecord(history(), "squat")!!

        assertEquals("Ciężar", record.primary.label)
        assertEquals("40", record.primary.value)
        assertEquals("kg", record.primary.unit)
        assertEquals("Powtórzenia", record.secondary?.label)
        assertEquals("8", record.secondary?.value)
        assertNull(record.secondary?.unit)
        assertEquals("w3", record.workoutId)
    }

    @Test
    fun `rekord ulamkowy uzywa polskiego przecinka`() {
        val workouts = listOf(workout("w1", 1_000L, listOf(weight(kg = 37.5, reps = 6))))

        assertEquals("37,5", exerciseRecord(workouts, "squat")!!.primary.value)
    }

    @Test
    fun `bez historii nie ma rekordu`() {
        assertNull(exerciseRecord(emptyList(), "squat"))
        assertNull(historyMetric(emptyList(), "squat"))
    }

    @Test
    fun `cwiczenie na powtorzenia ma rekord w powtorzeniach`() {
        val set = SetLog.Reps(
            exerciseId = "pushup", workoutId = "w", setNumber = 1,
            isWarmup = false, timestamp = 0L, reps = 21,
        )
        val workouts = listOf(workout("w1", 1_000L, listOf(set)))

        assertEquals(ChartMetric.REPS, historyMetric(workouts, "pushup"))
        val record = exerciseRecord(workouts, "pushup")!!
        assertEquals("Powtórzenia", record.primary.label)
        assertEquals("21", record.primary.value)
        assertNull(record.secondary)
    }

    // --- tabela sesji ---

    @Test
    fun `sesje ida od najnowszej i maja kolumny serii`() {
        val sessions = exerciseSessions(history(), "squat")

        assertEquals(3, sessions.size)
        assertEquals("w3", sessions.first().workoutId)
        assertEquals(2, sessions.first().cells.size)
        assertEquals("40", sessions.first().cells[0].main)
        assertEquals("8", sessions.first().cells[0].sub)
        assertEquals("37,5", sessions.first().cells[1].main)
    }

    @Test
    fun `wiersz z rekordem jest oznaczony`() {
        val sessions = exerciseSessions(history(), "squat")

        assertTrue(sessions.first { it.workoutId == "w3" }.hasPr)
        assertFalse(sessions.first { it.workoutId == "w1" }.hasPr)
    }

    @Test
    fun `krotsza sesja dostaje puste komorki zamiast krotszego wiersza`() {
        val workouts = listOf(
            workout("w1", 1_000L, listOf(weight(kg = 30.0, reps = 10))),
            workout(
                "w2", 2_000L,
                listOf(
                    weight(kg = 30.0, reps = 10),
                    weight(kg = 30.0, reps = 10, setNumber = 2),
                    weight(kg = 30.0, reps = 9, setNumber = 3),
                ),
            ),
        )

        val sessions = exerciseSessions(workouts, "squat")
        assertEquals(3, sessions.first().cells.size)
        val shorter = sessions.first { it.workoutId == "w1" }
        assertEquals(3, shorter.cells.size)
        assertNull(shorter.cells[1].main)
    }

    @Test
    fun `liczba kolumn nie przekracza limitu`() {
        val sets = (1..6).map { weight(kg = 30.0, reps = 10, setNumber = it) }
        val sessions = exerciseSessions(listOf(workout("w1", 1_000L, sets)), "squat", maxColumns = 4)

        assertEquals(4, sessions.single().cells.size)
    }

    @Test
    fun `naglowki lewej szyny podaja jednostki raz`() {
        assertEquals(SessionRailLabels("Kg", "Powt."), sessionRailLabels(ChartMetric.WEIGHT))
        assertEquals(SessionRailLabels("Powt.", null), sessionRailLabels(ChartMetric.REPS))
    }

    // --- wykres i trend ---

    @Test
    fun `slupki maja liczbe tylko na pierwszym i ostatnim, rekord podswietlony`() {
        val bars = chartBars(history(), "squat")

        assertEquals(3, bars.size)
        assertEquals("32,5", bars.first().label)
        assertEquals("40", bars.last().label)
        assertNull(bars[1].label)
        assertTrue(bars.last().isRecord)
        assertFalse(bars.first().isRecord)
    }

    @Test
    fun `przyrost liczy sie miedzy pierwsza a ostatnia sesja`() {
        val delta = trendDelta(history(), "squat")!!

        assertEquals("Przyrost", delta.label)
        assertEquals("+7,5", delta.value)
        assertEquals("kg", delta.unit)
    }

    @Test
    fun `bez zmiany ciezaru nie ma przyrostu`() {
        val workouts = listOf(
            workout("w1", 1_000L, listOf(weight(kg = 30.0, reps = 10))),
            workout("w2", 2_000L, listOf(weight(kg = 30.0, reps = 10))),
        )

        assertNull(trendDelta(workouts, "squat"))
    }

    @Test
    fun `mini-trend bierze ostatnie sesje w kolejnosci chronologicznej`() {
        val trend = trendValues(history(), "squat", limit = 2)

        assertEquals(listOf(35f, 40f), trend)
    }
}
