package com.stronk.progression

import com.stronk.data.SetTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Propozycje dla typów bez prowadzonego ciężaru: REPS, TIME, DISTANCE_TIME. */
class ProposeBodyweightTimeDistanceTest {

    // ------------------------------------------------------------- REPS

    private val pullups = planReps(reps = 10, sets = 3)

    @Test
    fun `REPS brak stanu - cel z planu, bez ciężaru`() {
        val p = propose(pullups)
        assertEquals(SetTarget.Reps(10), p.target)
        assertNull(p.weightKg)
    }

    @Test
    fun `REPS brak stanu po przerwie - start od okolic 55 procent powtórzeń`() {
        val p = propose(pullups, returningFromBreak = true)
        // 10 * 0.55 = 5.5 → 6
        assertEquals(SetTarget.Reps(6), p.target)
        assertTrue(p.isRampUp)
    }

    @Test
    fun `REPS zaliczony trening - plus jedno powtórzenie od minimum z serii`() {
        val state = stateOf(lastSets = listOf(repsSet(12, 1), repsSet(11, 2), repsSet(10, 3)))
        val p = propose(pullups, state)
        assertEquals(SetTarget.Reps(11), p.target)
    }

    @Test
    fun `REPS jedna porażka - powtórka próbowanego celu, plan jako podłoga`() {
        val state = stateOf(lastSets = listOf(repsSet(9, 1), repsSet(8, 2), repsSet(7, 3)), failStreak = 1)
        val p = propose(pullups, state)
        assertEquals(SetTarget.Reps(10), p.target)
    }

    @Test
    fun `REPS jedna porażka powyżej planu - powtórka najlepszej serii`() {
        val state = stateOf(lastSets = listOf(repsSet(15, 1), repsSet(13, 2), repsSet(12, 3)), failStreak = 1)
        val p = propose(pullups, state)
        assertEquals(SetTarget.Reps(15), p.target)
    }

    @Test
    fun `REPS dwie porażki - deload minus 10 procent powtórzeń`() {
        val state = stateOf(lastSets = listOf(repsSet(9, 1), repsSet(8, 2), repsSet(8, 3)), failStreak = 2)
        val p = propose(pullups, state)
        // 9 * 0.9 = 8.1 → 8
        assertEquals(SetTarget.Reps(8), p.target)
        assertTrue(p.isReactiveDeload)
    }

    @Test
    fun `REPS tydzień lekki - minus 40 procent powtórzeń`() {
        val state = stateOf(lastSets = listOf(repsSet(10, 1), repsSet(10, 2), repsSet(10, 3)))
        val p = propose(pullups, state, weekIndexInBlock = 5, blockLengthWeeks = 6)
        // baza po progresji 11 → * 0.6 = 6.6 → 7
        assertEquals(SetTarget.Reps(7), p.target)
        assertTrue(p.isLightWeek)
    }

    @Test
    fun `REPS tydzień lekki nie schodzi poniżej jednego powtórzenia`() {
        val plan = planReps(reps = 1)
        val state = stateOf(lastSets = listOf(repsSet(1, 1), repsSet(1, 2), repsSet(1, 3)), failStreak = 1)
        val p = propose(plan, state, weekIndexInBlock = 5, blockLengthWeeks = 6)
        assertEquals(SetTarget.Reps(1), p.target)
    }

    @Test
    fun `REPS ramp-up - podwojony przyrost z kapem na planie`() {
        val state = stateOf(lastSets = listOf(repsSet(6, 1), repsSet(6, 2), repsSet(6, 3)))
        val p = propose(pullups, state, returningFromBreak = true)
        assertEquals(SetTarget.Reps(8), p.target)
        assertTrue(p.isRampUp)

        val nearLevel = stateOf(lastSets = listOf(repsSet(9, 1), repsSet(9, 2), repsSet(9, 3)))
        val capped = propose(pullups, nearLevel, returningFromBreak = true)
        assertEquals(SetTarget.Reps(10), capped.target)
    }

    @Test
    fun `REPS po dogonieniu poziomu - zwykły plus jeden`() {
        val state = stateOf(lastSets = listOf(repsSet(10, 1), repsSet(10, 2), repsSet(10, 3)))
        val p = propose(pullups, state, returningFromBreak = true)
        assertEquals(SetTarget.Reps(11), p.target)
        assertFalse(p.isRampUp)
    }

    // ------------------------------------------------------------- TIME

    private val plank = planTime(seconds = 60, sets = 3)

    @Test
    fun `TIME brak stanu - cel z planu`() {
        assertEquals(SetTarget.Time(60), propose(plank).target)
    }

    @Test
    fun `TIME brak stanu po przerwie - 55 procent zaokrąglone do 5 s`() {
        val p = propose(plank, returningFromBreak = true)
        // 60 * 0.55 = 33 → 35
        assertEquals(SetTarget.Time(35), p.target)
        assertTrue(p.isRampUp)
    }

    @Test
    fun `TIME zaliczony trening - plus 10 procent zaokrąglone do 5 s`() {
        val state = stateOf(lastSets = listOf(timeSet(60, 1), timeSet(60, 2), timeSet(60, 3)))
        val p = propose(plank, state)
        // 60 * 1.1 = 66 → 65
        assertEquals(SetTarget.Time(65), p.target)
    }

    @Test
    fun `TIME krótkie czasy - minimalny przyrost 5 s`() {
        val plan = planTime(seconds = 20)
        val state = stateOf(lastSets = listOf(timeSet(20, 1), timeSet(20, 2), timeSet(20, 3)))
        val p = propose(plan, state)
        // 20 * 1.1 = 22 → zaokrąglenie dałoby 20, więc wymuszone +5
        assertEquals(SetTarget.Time(25), p.target)
    }

    @Test
    fun `TIME dwie porażki - deload minus 10 procent`() {
        val state = stateOf(lastSets = listOf(timeSet(50, 1), timeSet(45, 2), timeSet(40, 3)), failStreak = 2)
        val p = propose(plank, state)
        // 50 * 0.9 = 45
        assertEquals(SetTarget.Time(45), p.target)
        assertTrue(p.isReactiveDeload)
    }

    @Test
    fun `TIME tydzień lekki - minus 40 procent`() {
        val state = stateOf(lastSets = listOf(timeSet(60, 1), timeSet(60, 2), timeSet(60, 3)))
        val p = propose(plank, state, weekIndexInBlock = 5, blockLengthWeeks = 6)
        // baza po progresji 65 → * 0.6 = 39 → 40
        assertEquals(SetTarget.Time(40), p.target)
        assertTrue(p.isLightWeek)
    }

    @Test
    fun `TIME ramp-up - przyspieszony przyrost z kapem na planie`() {
        val state = stateOf(lastSets = listOf(timeSet(35, 1), timeSet(35, 2), timeSet(35, 3)))
        val p = propose(plank, state, returningFromBreak = true)
        // 35*1.2 = 42 → 40; minimum 35+10 = 45 → 45
        assertEquals(SetTarget.Time(45), p.target)
        assertTrue(p.isRampUp)

        val nearLevel = stateOf(lastSets = listOf(timeSet(58, 1), timeSet(58, 2), timeSet(58, 3)))
        assertEquals(SetTarget.Time(60), propose(plank, nearLevel, returningFromBreak = true).target)
    }

    // ---------------------------------------------------- DISTANCE_TIME

    private val run = planDistanceTime(meters = 1000.0, seconds = 300, sets = 1)

    @Test
    fun `DISTANCE_TIME brak stanu - cel z planu`() {
        assertEquals(SetTarget.DistanceTime(1000.0, 300), propose(run).target)
    }

    @Test
    fun `DISTANCE_TIME brak stanu po przerwie - 55 procent dystansu i czasu`() {
        val p = propose(run, returningFromBreak = true)
        // 550 m (krok 50 m), 165 s (krok 5 s)
        assertEquals(SetTarget.DistanceTime(550.0, 165), p.target)
        assertTrue(p.isRampUp)
    }

    @Test
    fun `DISTANCE_TIME zaliczony trening - plus 10 procent przy zachowanym tempie`() {
        val state = stateOf(lastSets = listOf(distSet(1000.0, 290)))
        val p = propose(run, state)
        // 1100 m, 290*1.1 = 319 → 320 s
        assertEquals(SetTarget.DistanceTime(1100.0, 320), p.target)
    }

    @Test
    fun `DISTANCE_TIME jedna porażka poniżej planu - powtórka celu z planu`() {
        val state = stateOf(lastSets = listOf(distSet(900.0, 300)), failStreak = 1)
        assertEquals(SetTarget.DistanceTime(1000.0, 300), propose(run, state).target)
    }

    @Test
    fun `DISTANCE_TIME dwie porażki - deload minus 10 procent`() {
        val state = stateOf(lastSets = listOf(distSet(900.0, 300)), failStreak = 2)
        val p = propose(run, state)
        // 810 → 800 m; 270 s
        assertEquals(SetTarget.DistanceTime(800.0, 270), p.target)
        assertTrue(p.isReactiveDeload)
    }

    @Test
    fun `DISTANCE_TIME tydzień lekki - minus 40 procent`() {
        val state = stateOf(lastSets = listOf(distSet(1000.0, 300)))
        val p = propose(run, state, weekIndexInBlock = 5, blockLengthWeeks = 6)
        // baza po progresji 1100/330 → 660 → 650 m; 198 → 200 s
        assertEquals(SetTarget.DistanceTime(650.0, 200), p.target)
        assertTrue(p.isLightWeek)
    }

    @Test
    fun `DISTANCE_TIME ramp-up dogania poziom - kap na celu z planu`() {
        val state = stateOf(lastSets = listOf(distSet(900.0, 270)))
        val p = propose(run, state, returningFromBreak = true)
        // 900*1.2 = 1080 >= 1000 → cel z planu
        assertEquals(SetTarget.DistanceTime(1000.0, 300), p.target)
        assertTrue(p.isRampUp)
    }
}
