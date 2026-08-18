package com.stronk.ui.workout

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Jedno źródło prawdy o trwającym treningu — singleton procesu, żeby stan
 * przeżył nawigację i wygaszenie ekranu (telefon w kieszeni, ADR-005) i był
 * współdzielony między [WorkoutViewModel]
 * a [com.stronk.service.RestTimerService] (akcja "✓ seria" z powiadomienia).
 *
 * Sesja żyje najwyżej tyle co proces — do Firestore trafia dopiero przy
 * zakończeniu treningu; porzucenie = [clear] bez śladu.
 */
object WorkoutSessionManager {

    private val _session = MutableStateFlow<WorkoutSession?>(null)
    val session: StateFlow<WorkoutSession?> = _session

    /** Czy trwa sesja dla tego wejścia trasy (powrót do ekranu = ta sama sesja). */
    fun isActiveFor(planId: String, dayIndex: Int): Boolean {
        val s = _session.value
        return s != null && s.planId == planId && s.dayIndex == dayIndex
    }

    fun start(session: WorkoutSession) {
        _session.value = session
    }

    /** Atomowa modyfikacja sesji czystą funkcją przejścia; no-op, gdy sesji nie ma. */
    fun mutate(transform: (WorkoutSession) -> WorkoutSession) {
        _session.update { it?.let(transform) }
    }

    /** ✓ z wielkiego przycisku i z powiadomienia na lock screenie. */
    fun completeCurrentSet(nowMillis: Long = System.currentTimeMillis()) {
        mutate { it.completeCurrentSet(nowMillis) }
    }

    /** Koniec sesji (trening zapisany albo porzucony). */
    fun clear() {
        _session.value = null
    }
}
