package com.stronk.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Liczniki i nagłówki ekranu „Dziś" — polska odmiana rzeczowników (CTA „6
 * ćwiczeń", belka „6 ćwiczeń · 18 serii") i rozbicie nazwy planu na tytuł
 * arkusza + kapitalik.
 */
class HomeTextsTest {

    // ---------- licznik ćwiczeń (CTA i belka ukończenia) ----------

    @Test
    fun `jedno cwiczenie w liczbie pojedynczej`() {
        assertEquals("1 ćwiczenie", HomeTexts.exercisesCount(1))
    }

    @Test
    fun `dwa do czterech cwiczen konczy sie na -a`() {
        assertEquals("2 ćwiczenia", HomeTexts.exercisesCount(2))
        assertEquals("3 ćwiczenia", HomeTexts.exercisesCount(3))
        assertEquals("4 ćwiczenia", HomeTexts.exercisesCount(4))
        assertEquals("22 ćwiczenia", HomeTexts.exercisesCount(22))
    }

    @Test
    fun `piec i wiecej cwiczen w dopelniaczu`() {
        assertEquals("0 ćwiczeń", HomeTexts.exercisesCount(0))
        assertEquals("5 ćwiczeń", HomeTexts.exercisesCount(5))
        assertEquals("6 ćwiczeń", HomeTexts.exercisesCount(6))
    }

    @Test
    fun `nastolatki lapia dopelniacz mimo koncowki`() {
        assertEquals("12 ćwiczeń", HomeTexts.exercisesCount(12))
        assertEquals("13 ćwiczeń", HomeTexts.exercisesCount(13))
        assertEquals("14 ćwiczeń", HomeTexts.exercisesCount(14))
    }

    // ---------- podsumowanie ukończonego treningu ----------

    @Test
    fun `belka ukonczenia laczy oba liczniki`() {
        assertEquals("6 ćwiczeń · 18 serii", HomeTexts.workoutSummary(6, 18))
        assertEquals("1 ćwiczenie · 1 seria", HomeTexts.workoutSummary(1, 1))
        assertEquals("3 ćwiczenia · 3 serie", HomeTexts.workoutSummary(3, 3))
    }

    @Test
    fun `licznik serii uzywa tego samego slownika co lista cwiczen`() {
        assertEquals("1 seria", HomeTexts.setsCount(1))
        assertEquals("3 serie", HomeTexts.setsCount(3))
        assertEquals("18 serii", HomeTexts.setsCount(18))
    }

    // ---------- nagłówek arkusza planu ----------

    @Test
    fun `nazwa planu z dopiskiem rozpada sie na tytul i kapitalik`() {
        val name = "Full Body 3×/tydz. (powrót po przerwie)"
        assertEquals("Full Body 3×/tydz.", HomeTexts.planTitle(name))
        assertEquals("powrót po przerwie", HomeTexts.planSubtitle(name))
    }

    @Test
    fun `nazwa bez nawiasu zostaje w calosci`() {
        assertEquals("Push Pull Nogi", HomeTexts.planTitle("Push Pull Nogi"))
        assertNull(HomeTexts.planSubtitle("Push Pull Nogi"))
    }

    @Test
    fun `pusty albo samotny nawias nie okalecza tytulu`() {
        assertEquals("(powrót)", HomeTexts.planTitle("(powrót)"))
        assertNull(HomeTexts.planSubtitle("(powrót)"))
        assertEquals("Plan ()", HomeTexts.planTitle("Plan ()"))
        assertNull(HomeTexts.planSubtitle("Plan ()"))
    }

    // ---------- dzień wolny ----------

    @Test
    fun `zapowiedz nastepnego treningu to dzien tygodnia i nazwa dnia planu`() {
        assertEquals(
            "poniedziałek · Full body A",
            HomeTexts.nextWorkout("poniedziałek", "Full body A"),
        )
    }

    @Test
    fun `teksty dnia wolnego maja polskie diakrytyki i zero obietnicy startu`() {
        assertEquals("Dzień wolny", HomeTexts.FREE_DAY)
        assertEquals("NASTĘPNY TRENING", HomeTexts.NEXT_WORKOUT.uppercase())
        assertTrue(HomeTexts.FREE_DAY_HINT.contains("cardio"))
    }

    // ---------- podgląd dnia w arkuszu planu ----------

    @Test
    fun `licznik ukrytych miniatur ma plusa`() {
        assertEquals("+3", HomeTexts.moreLabel(3))
    }
}
